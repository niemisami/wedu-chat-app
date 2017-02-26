package com.niemisami.wedu.question;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
import com.niemisami.wedu.course.QuestionsAdapter;
import com.niemisami.wedu.login.LoginActivity;
import com.niemisami.wedu.utils.FabUpdater;
import com.niemisami.wedu.utils.ToolbarUpdater;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static android.R.attr.type;
import static android.content.ContentValues.TAG;
import static com.niemisami.wedu.R.id.username;

/**
 * A simple {@link Fragment} subclass.
 */
public class QuestionsFragment extends Fragment implements QuestionsAdapter.QuestionOnClickHandler, MainActivity.OnFabClickListener {

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

    private Boolean isConnected = true;


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

        WeduApplication app = (WeduApplication) getActivity().getApplication();
        mSocket = app.getSocket();
        mSocket.on(Socket.EVENT_CONNECT, onConnect);
        mSocket.on(Socket.EVENT_DISCONNECT, onDisconnect);
        mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.on("new message", onNewQuestion);
        mSocket.on("user joined", onUserJoined);
        mSocket.on("user left", onUserLeft);
        mSocket.connect();

        startSignIn();
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

        mSocket.disconnect();

        mSocket.off(Socket.EVENT_CONNECT, onConnect);
        mSocket.off(Socket.EVENT_DISCONNECT, onDisconnect);
        mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.off(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.off("new message", onNewQuestion);
        mSocket.off("user joined", onUserJoined);
        mSocket.off("user left", onUserLeft);
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


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Activity.RESULT_OK != resultCode) {
            getActivity().finish();
            return;
        }

        mUsername = data.getStringExtra("username");
        int numUsers = data.getIntExtra("numUsers", 1);


//        addQuestion("Sami", "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod");
//        addQuestion("Pyry", "Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt. Neque porro quisquam est, qui dolorem ipsum");
//        addQuestion("Anna", "quaerat voluptatem. Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid ex ea com");
//        addQuestion("Anna", "quaerat voluptatem. Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid ex ea com");
//        addQuestion("Anna", "quaerat voluptatem. Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid ex ea com");
//        addQuestion("Anna", "quaerat voluptatem. Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid ex ea com");

        addParticipantsLog(numUsers);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_chat, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_leave) {
            leave();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void addParticipantsLog(int numUsers) {

        // TODO: come up with better place to check welcome text. It is hidden when new question is added in attemptSend()
        if (mAdapter.getItemCount() == 0) {
            mWelcomeText.setVisibility(View.VISIBLE);
        }

        if (mToolbarUpdater != null)
            mToolbarUpdater.setSubtitle(getResources().getQuantityString(R.plurals.message_participants, numUsers, numUsers));
    }

    private void addQuestion(Question question) {
        if(mWelcomeText.getVisibility() == View.VISIBLE){
            mWelcomeText.setVisibility(View.GONE);
        }

        mQuestions.add(question);
        mAdapter.notifyItemInserted(mQuestions.size() - 1);
    }

    private void leave() {
        mUsername = null;
        mSocket.disconnect();
        mSocket.connect();
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
                                obj.put("username", mUsername);
                                mSocket.emit("add user", obj);
                            } catch (JSONException e) {
                                Log.e(TAG, "attemptSend: ", e);
                            }

                        } else {
                            getActivity().finish();
                        }
                    }
                    Toast.makeText(getActivity().getApplicationContext(),
                            R.string.connect, Toast.LENGTH_LONG).show();
                    isConnected = true;
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
                    Toast.makeText(getActivity().getApplicationContext(),
                            R.string.disconnect, Toast.LENGTH_LONG).show();
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
                    Toast.makeText(getActivity().getApplicationContext(),
                            R.string.error_connect, Toast.LENGTH_LONG).show();
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
                    int type;
                    String username;
                    String message;
                    String courseId;
                    long created;
                    int upvotes;
                    boolean isSolved;
                    try {

                        type = Question.TYPE_MESSAGE_QUESTION;
                        username = data.getString("username");
                        message = data.getString("message");
                        created = System.currentTimeMillis();
                        courseId = "Koodikerho";
                        upvotes = new Random().nextInt(10);
                        isSolved = upvotes % 2 == 0;

                    } catch (JSONException e) {
                        return;
                    }

                    Question question = new Question.Builder(type)
                            .message(message)
                            .username(username)
                            .courseId(courseId)
                            .solved(isSolved)
                            .created(created)
                            .upvotes(upvotes)
                            .build();

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
                        username = data.getString("username");
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
                        username = data.getString("username");
                        numUsers = data.getInt("numUsers");
                    } catch (JSONException e) {
                        return;
                    }

                    addParticipantsLog(numUsers);
                }
            });
        }
    };

    @Override
    public void onUpvoteClick(int itemPosition) {
        if (itemPosition >= 0) {
            Question votedQuestion = mQuestions.get(itemPosition);
            int currentUpvotes = votedQuestion.getUpvotes();
            final Question upvotedQuestion = votedQuestion.toBuilder().upvotes(++currentUpvotes).build();
            mQuestions.remove(itemPosition);
            mQuestions.add(itemPosition, upvotedQuestion);
            mAdapter.notifyDataSetChanged();
        } else {
            Log.w(TAG, "onDownvoteClick: tried to upvote negative index");
        }

    }

    @Override
    public void onDownvoteClick(int itemPosition) {
        if (itemPosition >= 0) {
            Question votedQuestion = mQuestions.get(itemPosition);
            int currentUpvotes = votedQuestion.getUpvotes();
            final Question downvotedQuestion = votedQuestion.toBuilder().upvotes(--currentUpvotes).build();
            mQuestions.remove(itemPosition);
            mQuestions.add(itemPosition, downvotedQuestion);
            mAdapter.notifyDataSetChanged();
        } else {
            Log.w(TAG, "onDownvoteClick: tried to downvote negative index");
        }

    }

    @Override
    public void onQuestionClick(int itemPosition) {

    }

    @Override
    public void onFabClicked() {
        displaQuestionInputField();
    }

    private void displaQuestionInputField() {
        mQuestionInputContainer.setVisibility(View.VISIBLE);
        mQuestionInputField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    showKeyboard(getActivity());
                } else {
                    hideKeyboard(getActivity());
                }
            }
        });
        mQuestionInputField.postDelayed(new Runnable() {
            @Override
            public void run() {
                mQuestionInputField.requestFocus();
            }
        }, 1000);
        mQuestionInputField.requestFocus();
    }

    public static void showKeyboard(Activity activity) {
        if (activity != null) {
            activity.getWindow()
                    .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    public static void hideKeyboard(Activity activity) {
        if (activity != null) {
            activity.getWindow()
                    .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }
    }
}
