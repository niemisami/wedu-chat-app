package com.niemisami.wedu.question;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;
import com.niemisami.wedu.MainActivity;
import com.niemisami.wedu.R;
import com.niemisami.wedu.WeduApplication;
import com.niemisami.wedu.chat.ChatActivity;
import com.niemisami.wedu.chat.Message;
import com.niemisami.wedu.course.QuestionsAdapter;
import com.niemisami.wedu.login.LoginActivity;
import com.niemisami.wedu.settings.SettingsActivity;
import com.niemisami.wedu.settings.WeduPreferenceHelper;
import com.niemisami.wedu.utils.AnimationHelper;
import com.niemisami.wedu.utils.FabUpdater;
import com.niemisami.wedu.utils.MessageFetchTask;
import com.niemisami.wedu.utils.MessageJsonParser;
import com.niemisami.wedu.utils.NetworkUtils;
import com.niemisami.wedu.utils.ToolbarUpdater;
import com.niemisami.wedu.utils.WeduNetworkCallbacks;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static android.R.attr.type;
import static android.content.ContentValues.TAG;
import static android.content.Context.INPUT_METHOD_SERVICE;
import static com.niemisami.wedu.R.id.linearLayout;

/**
 * A simple {@link Fragment} subclass.
 */
public class QuestionsFragment extends Fragment implements QuestionsAdapter.QuestionOnClickHandler, MainActivity.OnFabClickListener, WeduNetworkCallbacks {

    private static final int REQUEST_LOGIN = 0;

    private ToolbarUpdater mToolbarUpdater;
    private FabUpdater mFabUpdater;
    private RecyclerView mQuestionsView;
    private RelativeLayout mQuestionInputContainer;
    private EditText mQuestionInputField;
    private ImageButton mSendQuestionButton;
    private TextView mWelcomeText;
    private List<Question> mQuestions;
    private QuestionsAdapter mAdapter;
    private String mUsername;
    private Socket mSocket;

    private Boolean isConnected = false;


    public QuestionsFragment() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ToolbarUpdater)
            mToolbarUpdater = (ToolbarUpdater) context;
        if (context instanceof FabUpdater) {
            mFabUpdater = (FabUpdater) context;
        }
        mAdapter = new QuestionsAdapter(context, this);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        WeduApplication app = (WeduApplication) getActivity().getApplication();
        mSocket = app.getSocket();
        mSocket.on(Socket.EVENT_CONNECT, onConnect);
        mSocket.on(Socket.EVENT_DISCONNECT, onDisconnect);
        mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.on("new message", onNewQuestion);
        mSocket.on("user joined", onUserJoined);
        mSocket.on("user left", onUserLeft);
        mSocket.on("voted", onVoted);

        mUsername = WeduPreferenceHelper.getUsername(getActivity());

        if (mUsername == null || mUsername.length() == 0) {
            startSignIn();
        }
        mSocket.connect();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_questions, container, false);

        mWelcomeText = (TextView) view.findViewById(R.id.welcome_text);
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
        mSocket.disconnect();

        mSocket.off(Socket.EVENT_CONNECT, onConnect);
        mSocket.off(Socket.EVENT_DISCONNECT, onDisconnect);
        mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.off(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.off("new message", onNewQuestion);
        mSocket.off("user joined", onUserJoined);
        mSocket.off("user left", onUserLeft);
        mSocket.off("voted", onVoted);
        isConnected = false;
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mQuestions = new ArrayList<>();

        mQuestionsView = (RecyclerView) view.findViewById(R.id.questions);
        mQuestionsView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mQuestionsView.setAdapter(mAdapter);
        mAdapter.setQuestions(mQuestions);

        mQuestionsView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    mFabUpdater.hideFab();
                } else if (dy < 0) {
                    mFabUpdater.showFab();
                }
            }
        });

        mQuestionInputContainer = (RelativeLayout) view.findViewById(R.id.message_input_layout);
//        mQuestionInputContainer.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
        mSendQuestionButton = (ImageButton) view.findViewById(R.id.send_button);
        mSendQuestionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptSend();
                //TODO better place for these
                mQuestionInputContainer.setVisibility(View.GONE);
                mFabUpdater.showFab();
            }
        });

        mQuestionInputField = (EditText) view.findViewById(R.id.message_input);
        mQuestionInputField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int id, KeyEvent event) {
                if (id == R.id.send || id == EditorInfo.IME_NULL) {
                    attemptSend();
                    //TODO better place for these
                    mQuestionInputContainer.setVisibility(View.GONE);
                    mFabUpdater.showFab();
                    return true;
                }
                return false;
            }
        });
        mQuestionInputField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    showKeyboard(getActivity(), mQuestionInputField);
                } else {
                    hideKeyboard(getActivity(), mQuestionInputField);
                }
            }
        });


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Activity.RESULT_OK != resultCode) {
            getActivity().finish();
            return;
        }

        mUsername = data.getStringExtra("user");
        int numUsers = data.getIntExtra("numUsers", 1);

        addParticipantsLog(numUsers);
    }

    private void addParticipantsLog(int numUsers) {

        // TODO: come up with better place to check welcome text. It is hidden when new question is added in attemptSend()
        if (mAdapter.getItemCount() == 0) {
            mWelcomeText.setVisibility(View.VISIBLE);
        } else {
            mWelcomeText.setVisibility(View.GONE);
        }

        if (mToolbarUpdater != null)
            mToolbarUpdater.setSubtitle(getResources().getQuantityString(R.plurals.message_participants, numUsers, numUsers));
    }

    private void addQuestion(Question question) {
        if (mWelcomeText.getVisibility() == View.VISIBLE) {
            mWelcomeText.setVisibility(View.GONE);
        }

        mQuestions.add(question);
        mAdapter.notifyItemInserted(mQuestions.size() - 1);
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_chat, menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                getActivity().onBackPressed();
                break;
            case R.id.action_settings:
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                break;
            case R.id.action_leave:
                leave();
                break;
        }
        return true;
    }

    private void leave() {
        mUsername = null;
        mSocket.disconnect();
        WeduPreferenceHelper.clearUsername(getActivity());
        startSignIn();
    }

    private void startSignIn() {
        mUsername = null;
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        startActivityForResult(intent, REQUEST_LOGIN);
    }

    private void scrollToBottom() {
        mQuestionsView.scrollToPosition(mAdapter.getItemCount() - 1);
    }


    private void attemptSend() {
        if (null == mUsername) return;
        if (!mSocket.connected()) return;

        String message = mQuestionInputField.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            mQuestionInputField.requestFocus();
            return;
        }

        mQuestionInputField.setText("");
//        addMessage(mUsername, message);

        // perform the sending message attempt.

        JSONObject obj = new JSONObject();
        try {
            obj.put("message", message.trim());
            obj.put("type", Message.TYPE_MESSAGE_QUESTION);
            obj.put("course", Question.DEFAULT_COURSE);
            mSocket.emit("new message", obj);

        } catch (JSONException e) {
            Log.e(TAG, "attemptSend: ", e);
        }
    }


    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!isConnected) {
                        if (null != mUsername) {

                            JSONObject obj = new JSONObject();
                            try {
                                obj.put("user", mUsername);
                                mSocket.emit("add user", obj);
                            } catch (JSONException e) {
                                Log.e(TAG, "attemptSend: ", e);
                            }

                        } else {
                            getActivity().finish();
                        }
                        showToast(R.string.connect);
                        isConnected = true;
                    }
                }
            });
        }
    };

    private Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    isConnected = false;
                    showToast(R.string.disconnect);
                }
            });
        }
    };

    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showToast(R.string.error_connect);
                }
            });
        }
    };

    private Emitter.Listener onNewQuestion = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];

                    Question question = null;
                    try {
                        question = MessageJsonParser.parseQuestion(data);
                    } catch (NullPointerException e) {
                        Log.e(TAG, "run: ", e);
                    }

                    addQuestion(question);
                }
            });
        }
    };

    private Emitter.Listener onUserJoined = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    int numUsers;
                    try {
                        username = data.getString("user");
                        numUsers = data.getInt("numUsers");
                    } catch (JSONException e) {
                        return;
                    }

                    addParticipantsLog(numUsers);
                }
            });
        }
    };

    private Emitter.Listener onUserLeft = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    int numUsers;
                    try {
                        username = data.getString("user");
                        numUsers = data.getInt("numUsers");
                    } catch (JSONException e) {
                        return;
                    }

                    addParticipantsLog(numUsers);
                }
            });
        }
    };

    private Emitter.Listener onVoted = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String messageId = "";
                    int gradedQuestionPosition = -1;
                    int upvotes;
                    try {
                        messageId = data.getString("messageId");

                        JSONArray upvotedUsers = data.getJSONObject("grade").getJSONArray("upvotes");
                        JSONArray downvotedUsers = data.getJSONObject("grade").getJSONArray("downvotes");

                        upvotes = upvotedUsers.length() - downvotedUsers.length();

                        gradedQuestionPosition = findQuestionPositionWithId(messageId);

                    } catch (JSONException e) {
                        Log.e(TAG, "onVoted. Couldn't parse message json with id: " + messageId);
                        return;
                    } catch (Resources.NotFoundException e) {
                        Log.e(TAG, "onVoted. Didn't find question with id: " + messageId);
                        return;
                    }


                    Question gradedQuestion = mQuestions.get(gradedQuestionPosition);

                    final Question downvotedQuestion = gradedQuestion.toBuilder().upvotes(upvotes).build();
                    mQuestions.remove(gradedQuestionPosition);
                    mQuestions.add(gradedQuestionPosition, downvotedQuestion);
                    mAdapter.notifyDataSetChanged();
                }
            });
        }
    };


    /**
     * Find message with given ID and return the position in Questions array
     */
    private int findQuestionPositionWithId(String messageId) throws Resources.NotFoundException {
        if (mQuestions.size() >= 0) {
            for (int i = 0; i < mQuestions.size(); i++) {
                if (mQuestions.get(i).getId().equals(messageId)) {
                    return i;
                }
            }
        }
        throw new Resources.NotFoundException();

    }

    @Override
    public void onUpvoteClick(int itemPosition) {
        if (itemPosition >= 0) {

            Question votedQuestion = mQuestions.get(itemPosition);

            JSONObject obj = new JSONObject();
            try {
                obj.put("messageId", votedQuestion.getId());
                mSocket.emit("upvote", obj);

            } catch (JSONException e) {
                Log.e(TAG, "onUpvoteClick: ", e);
            }

        } else {
            Log.w(TAG, "onUpvoteClick: tried to upvote negative index");
        }

    }

    @Override
    public void onDownvoteClick(int itemPosition) {
        if (itemPosition >= 0) {

            Question votedQuestion = mQuestions.get(itemPosition);

            JSONObject obj = new JSONObject();
            try {
                obj.put("messageId", votedQuestion.getId());
                mSocket.emit("downvote", obj);

            } catch (JSONException e) {
                Log.e(TAG, "onDownvoteClick: ", e);
            }

        } else {
            Log.w(TAG, "onDownvoteClick: tried to downvote negative index");
        }

    }

    @Override
    public void onQuestionClick(int itemPosition) {
        Intent launchQuestionDetails = new Intent(getActivity(), ChatActivity.class);
        launchQuestionDetails.putExtra(Question.EXTRA_QUESTION_ID, mQuestions.get(itemPosition).getId());
        launchQuestionDetails.putExtra(Question.EXTRA_QUESTION_COLOR, mAdapter.getItemBackgroundColorId(itemPosition));
        startActivity(launchQuestionDetails);
    }

    @Override
    public void onFabClicked() {
        displaQuestionInputField();
    }

    private void displaQuestionInputField() {
        mQuestionInputContainer.setVisibility(View.VISIBLE);
        mQuestionInputField.requestFocus();
    }

    public static void showKeyboard(Activity activity, View requestingImeView) {
        if (activity != null && requestingImeView != null) {
            InputMethodManager inputMethodManager =
                    (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.toggleSoftInputFromWindow(
                    requestingImeView.getApplicationWindowToken(),
                    InputMethodManager.SHOW_FORCED, 0);

//            activity.getWindow()
//                    .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    public static void hideKeyboard(Activity activity, View requestingImeView) {

        if (activity != null && requestingImeView != null) {
//            activity.getWindow()
//                    .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

            InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(requestingImeView.getWindowToken(), 0);
        }
    }

    private Toast mToast;

    private void showToast(int stringResourceId) {
        if (mToast != null) {
            mToast.cancel();
        }
        if (getActivity() != null) {
            mToast = Toast.makeText(getActivity().getApplicationContext(), stringResourceId, Toast.LENGTH_LONG);
            mToast.show();
        }
    }


    @Override
    public void fetchBegin() {
        //TODO: displayProgressBar();
    }

    @Override
    public void fetchFailed(Exception e) {
        //TODO: hideProgressBar();

    }

    @Override
    public void fetchComplete(String data) {
        //TODO: hideProgressBar();
        try {
            final List<Question> questions = MessageJsonParser.parseQuestionList(data);
            if (getActivity() != null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (questions.size() < 10) {
                            for (Question q : questions) {
                                addQuestion(q);
                            }
                        } else {
                            mQuestions = questions;
                            mAdapter.setQuestions(mQuestions);
                        }
                    }
                });
            }

        } catch (NullPointerException e) {
            Log.e(TAG, "onResponse: ", e);
        }

    }
}
