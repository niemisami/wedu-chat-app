package com.niemisami.wedu.chat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;
import com.niemisami.wedu.R;
import com.niemisami.wedu.WeduApplication;
import com.niemisami.wedu.login.LoginActivity;
import com.niemisami.wedu.question.Question;
import com.niemisami.wedu.utils.MessageJsonParser;
import com.niemisami.wedu.utils.ToolbarUpdater;
import com.niemisami.wedu.utils.WeduDateUtils;
import com.niemisami.wedu.utils.WeduNetworkCallbacks;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.R.attr.x;
import static android.content.ContentValues.TAG;
import static com.niemisami.wedu.utils.MessageJsonParser.parseQuestion;

public class ChatFragment extends Fragment implements WeduNetworkCallbacks {

    private static final int REQUEST_LOGIN = 0;

    private static final int TYPING_TIMER_LENGTH = 600;

//    private ToolbarUpdater mToolbarUpdater;
    private RecyclerView mMessagesView;
    private EditText mInputMessageView;
    private List<Message> mMessages = new ArrayList<>();
    private RecyclerView.Adapter mAdapter;
    private boolean mTyping = false;
    private Handler mTypingHandler = new Handler();
    private String mUsername;
    private int mQuestionBackgroundColor;
    private Socket mSocket;

    private Boolean isConnected = true;
    private Question mQuestion;


    private TextView mCreatedView, mQuestionView, mUpvotesView;
    private ImageView mSolvedIcon;
    private ImageButton mUpvoteButton, mDownvoteButton;


    public ChatFragment() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
//        if (context instanceof ToolbarUpdater)
//            mToolbarUpdater = (ToolbarUpdater) context;
        mAdapter = new MessageAdapter(context, mMessages);
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
        mSocket.on("new message", onNewMessage);
//        mSocket.on("user joined", onUserJoined);
//        mSocket.on("user left", onUserLeft);
        mSocket.on("typing", onTyping);
        mSocket.on("stop typing", onStopTyping);
        mSocket.connect();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        initViews(view);
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
        mSocket.off("new message", onNewMessage);
//        mSocket.off("user joined", onUserJoined);
//        mSocket.off("user left", onUserLeft);
        mSocket.off("typing", onTyping);
        mSocket.off("stop typing", onStopTyping);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mMessagesView = (RecyclerView) view.findViewById(R.id.messages);
        mMessagesView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mMessagesView.setAdapter(mAdapter);

        mInputMessageView = (EditText) view.findViewById(R.id.message_input);
        mInputMessageView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int id, KeyEvent event) {
                if (id == R.id.send || id == EditorInfo.IME_NULL) {
                    attemptSend();
                    return true;
                }
                return false;
            }
        });
        mInputMessageView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (null == mUsername) return;
                if (!mSocket.connected()) return;

                if (!mTyping) {
                    mTyping = true;
                    mSocket.emit("typing");
                }

                mTypingHandler.removeCallbacks(onTypingTimeout);
                mTypingHandler.postDelayed(onTypingTimeout, TYPING_TIMER_LENGTH);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        ImageButton sendButton = (ImageButton) view.findViewById(R.id.send_button);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptSend();
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

//        int numUsers = data.getIntExtra("numUsers", 1);

        addLog(getResources().getString(R.string.message_welcome));
//        addParticipantsLog(numUsers);
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

    private void addLog(String message) {
        mMessages.add(new Message.Builder("none", Message.TYPE_LOG)
                .message(message).build());
        mAdapter.notifyItemInserted(mMessages.size() - 1);
        scrollToBottom();
    }

//    private void addParticipantsLog(int numUsers) {
//        mToolbarUpdater.setSubtitle(getResources().getQuantityString(R.plurals.message_participants, numUsers, numUsers));
//    }

    private void addMessage(String username, String message) {
        if (username.equals(mUsername)) {
            mMessages.add(new Message.Builder("none", Message.TYPE_MESSAGE_OWN)
                    .username(username).message(message).build());
        } else {
            mMessages.add(new Message.Builder("none", Message.TYPE_MESSAGE_FRIEND)
                    .username(username).message(message).build());
        }
        mAdapter.notifyItemInserted(mMessages.size() - 1);
        scrollToBottom();
    }

    private void addTyping(String username) {
        mMessages.add(new Message.Builder("none", Message.TYPE_ACTION)
                .username(username).build());
        mAdapter.notifyItemInserted(mMessages.size() - 1);
        scrollToBottom();
    }

    private void removeTyping(String username) {
        for (int i = mMessages.size() - 1; i >= 0; i--) {
            Message message = mMessages.get(i);
            if (message.getType() == Message.TYPE_ACTION && message.getUsername().equals(username)) {
                mMessages.remove(i);
                mAdapter.notifyItemRemoved(i);
            }
        }
    }

    private void attemptSend() {
        if (null == mUsername) return;
        if (!mSocket.connected()) return;

        mTyping = false;

        String message = mInputMessageView.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            mInputMessageView.requestFocus();
            return;
        }

        mInputMessageView.setText("");
//        addMessage(mUsername, message);

        // perform the sending message attempt.

        JSONObject obj = new JSONObject();
        try {
            obj.put("message", message.trim());
            obj.put("type", Message.TYPE_MESSAGE_OWN);
            obj.put("course", Question.DEFAULT_COURSE);
            obj.put("questionId", mQuestion.getId());
            mSocket.emit("new message", obj);

        } catch (JSONException e) {
            Log.e(TAG, "attemptSend: ", e);
        }
    }

    private void startSignIn() {
        mUsername = null;
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        startActivityForResult(intent, REQUEST_LOGIN);
    }

    private void leave() {
        mUsername = null;
        mSocket.disconnect();
        mSocket.connect();
        startSignIn();
    }

    private void scrollToBottom() {
        mMessagesView.scrollToPosition(mAdapter.getItemCount() - 1);
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
                    }
                    showToast(R.string.connect);
                    isConnected = true;
                }
            });
        }
    };

    private Toast mToast;

    private void showToast(int stringResourceId) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(getActivity().getApplicationContext(), stringResourceId, Toast.LENGTH_LONG);
        mToast.show();
    }

    private Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    isConnected = false;
//                    showToast(R.string.disconnect);
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

    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    String message;
                    try {
                        username = data.getString("user");
                        message = data.getString("message");
                    } catch (JSONException e) {
                        Log.e(TAG, "run: ", e);
                        return;
                    }
                    Log.d(TAG, "run: " + data.toString());

                    removeTyping(username);
                    addMessage(username, message);
                }
            });
        }
    };

//    private Emitter.Listener onUserJoined = new Emitter.Listener() {
//        @Override
//        public void call(final Object... args) {
//            getActivity().runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    JSONObject data = (JSONObject) args[0];
//                    String username;
//                    int numUsers;
//                    try {
//                        username = data.getString("user");
//                        numUsers = data.getInt("numUsers");
//                    } catch (JSONException e) {
//                        return;
//                    }
//
//                    addLog(getResources().getString(R.string.message_user_joined, username));
//                    addParticipantsLog(numUsers);
//                }
//            });
//        }
//    };
//
//    private Emitter.Listener onUserLeft = new Emitter.Listener() {
//        @Override
//        public void call(final Object... args) {
//            getActivity().runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    JSONObject data = (JSONObject) args[0];
//                    String username;
//                    int numUsers;
//                    try {
//                        username = data.getString("user");
//                        numUsers = data.getInt("numUsers");
//                    } catch (JSONException e) {
//                        return;
//                    }
//
//                    addLog(getResources().getString(R.string.message_user_left, username));
//                    addParticipantsLog(numUsers);
//                    removeTyping(username);
//                }
//            });
//        }
//    };

    private Emitter.Listener onTyping = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    try {
                        username = data.getString("user");
                    } catch (JSONException e) {
                        return;
                    }
                    addTyping(username);
                }
            });
        }
    };

    private Emitter.Listener onStopTyping = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    try {
                        username = data.getString("user");
                    } catch (JSONException e) {
                        return;
                    }
                    removeTyping(username);
                }
            });
        }
    };

    private Runnable onTypingTimeout = new Runnable() {
        @Override
        public void run() {
            if (!mTyping) return;

            mTyping = false;

            JSONObject obj = new JSONObject();
            try {

                obj.put("user", mUsername);
                mSocket.emit("stop typing", obj);

            } catch (JSONException e) {
                Log.e(TAG, "attemptSend: ", e);
            }
        }
    };

    @Override
    public void fetchBegin() {
        Log.d(TAG, "fetchBegin: ");
    }

    @Override
    public void fetchFailed(Exception e) {
        Log.e(TAG, "fetchFailed: ", e);

    }

    @Override
    public void fetchComplete(String data) {
        Question question = null;
        try {
            question = MessageJsonParser.parseQuestion(new JSONObject(data));


        } catch (NullPointerException e) {
            Log.w(TAG, "fetchComplete: null json data", e);
            getActivity().finish();
        } catch (JSONException e) {
            Log.w(TAG, "fetchComplete: null json data", e);
            getActivity().finish();
        }

        if (question != null) {
            mQuestion = question;
            mUsername = "Sami";
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    inflateQuestionDetails();
                }
            });
        } else {
            Log.e(TAG, "fetchComplete: null message");
            getActivity().finish();
        }
    }

    private void inflateQuestionDetails() {
        setQuestionMessage(mQuestion.getMessage());
        setCreated(mQuestion.getCreated());
        displayAsSolved(mQuestion.isSolved());
        setUpvotes(mQuestion.getUpvotes());
        setQuestionBackgroundColor();
    }


    private void initViews(View view) {

        mCreatedView = (TextView) view.findViewById(R.id.question_created);
        mQuestionView = (TextView) view.findViewById(R.id.question_message);
        mUpvotesView = (TextView) view.findViewById(R.id.label_upvotes);
        mSolvedIcon = (ImageView) view.findViewById(R.id.icon_solved);
        mUpvoteButton = (ImageButton) view.findViewById(R.id.button_upvote_question);
        mDownvoteButton = (ImageButton) view.findViewById(R.id.button_downvote_question);

//        view.setOnClickListener(onQuestionClickListener);
//        mUpvoteButton.setOnClickListener(onUpvoteClickListener);
//        mDownvoteButton.setOnClickListener(onDownvoteClickListener);
    }

    public void displayAsSolved(boolean solved) {
        int displayIcon = (solved ? View.VISIBLE : View.INVISIBLE);
        mSolvedIcon.setVisibility(displayIcon);
    }

    public void setCreated(long dateMillis) {
        if (null == mCreatedView) return;

        long normalizedDate = WeduDateUtils.normalizeDate(dateMillis);
        String dateString = WeduDateUtils.getFriendlyDateString(getActivity(), normalizedDate, true);
        mCreatedView.setText(dateString);
    }

    public void setQuestionMessage(String message) {
        if (null == mQuestionView) return;
        mQuestionView.setText(message);
    }

    private void setQuestionBackgroundColor() {
        mQuestionBackgroundColor = ((ChatActivity) getActivity()).getQuestionBackgroundColor();
        ((ChatActivity) getActivity()).matchToolbarColorWithQuestion();
        View parent = (View) mCreatedView.getParent();
        parent.setBackgroundColor(mQuestionBackgroundColor);

    }

    public void setUpvotes(int upvotes) {
        mUpvotesView.setText(Integer.toString(upvotes));
    }

    /**
     * ClickListeners for question item
     */
//
//    private View.OnClickListener onQuestionClickListener = new View.OnClickListener() {
//        @Override
//        public void onClick(View view) {
//            mQuestionOnClickHandler.onQuestionClick(getAdapterPosition());
//        }
//    };
//    private View.OnClickListener onDownvoteClickListener = new View.OnClickListener() {
//        @Override
//        public void onClick(View view) {
//            mQuestionOnClickHandler.onDownvoteClick(getAdapterPosition());
//        }
//    };
//    private View.OnClickListener onUpvoteClickListener = new View.OnClickListener() {
//        @Override
//        public void onClick(View view) {
//            mQuestionOnClickHandler.onUpvoteClick(getAdapterPosition());
//        }
//    };
}