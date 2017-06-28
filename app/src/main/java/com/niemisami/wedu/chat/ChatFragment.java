package com.niemisami.wedu.chat;

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

import com.niemisami.wedu.R;
import com.niemisami.wedu.login.LoginActivity;
import com.niemisami.wedu.question.Question;
import com.niemisami.wedu.settings.WeduPreferenceHelper;
import com.niemisami.wedu.socket.SocketManager;
import com.niemisami.wedu.utils.ToolbarUpdater;
import com.niemisami.wedu.utils.WeduDateUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

import static android.content.ContentValues.TAG;

public class ChatFragment extends Fragment {

    private static final int REQUEST_LOGIN = 0;

    private static final int TYPING_TIMER_LENGTH = 600;

    private SocketManager mSocketManager;
    private CompositeDisposable mListenersDisposable;

    private List<Message> mMessages = new ArrayList<>();
    private boolean mTyping = false;
    private Handler mTypingHandler = new Handler();
    private String mUsername;

    private Question mQuestion;
    private Boolean isConnected = false;

    private ToolbarUpdater mToolbarUpdater;

    private RecyclerView mMessagesView;
    private EditText mInputMessageView;
    private RecyclerView.Adapter mAdapter;
    private View mQuestionDetailsContainer;
    private TextView mCreatedView, mQuestionView, mUpvotesView;
    private ImageView mSolvedIcon;
    private Toast mToast;


    public ChatFragment() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ToolbarUpdater)
            mToolbarUpdater = (ToolbarUpdater) context;
        mAdapter = new MessageAdapter(context, mMessages);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        mSocketManager = SocketManager.getSocketManager();
        mListenersDisposable = new CompositeDisposable();
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        initViews(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        createSocketListeners();
        mUsername = WeduPreferenceHelper.getUsername(getActivity());
    }

    @Override
    public void onPause() {
        super.onPause();
        mListenersDisposable.dispose();
        isConnected = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSocketManager.disconnect();
    }


    private void createSocketListeners() {
        //onConnect
        mListenersDisposable.add(mSocketManager.createConnectionListener()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                        JSONObject obj = new JSONObject();
                        try {
                            obj.put("user", mUsername);
                            mSocketManager.addUser(obj);
                        } catch (JSONException e) {
                            Log.e(TAG, "sendMessage: ", e);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "Chat connection failed", e);
                        showToast(R.string.error_connection_lost);
                        getActivity().finish();
                    }
                })
        );
        //onDisconnect
        mListenersDisposable.add(mSocketManager.createDisconnectionListener()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                        Log.d(TAG, "Chat disconnected");
                        showToast(R.string.error_connection_lost);
                        getActivity().finish();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "Chat disconnection failed", e);
                        getActivity().finish();

                    }
                })
        );
        //onConnectError
        mListenersDisposable.add(mSocketManager.createConnectErrorListener()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                        Log.d(TAG, "Chat connection error");
                        getActivity().finish();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "Chat connection error failed", e);

                    }
                })
        );
        //onConnectTimeout
        mListenersDisposable.add(mSocketManager.createConnectTimeoutListener()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                        Log.d(TAG, "Chat connection timeout");
                        getActivity().finish();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "Chat connection timeout failed", e);

                    }
                })
        );
        // onNewMessageListener
        mListenersDisposable.add(mSocketManager.createMessageListener()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<JSONObject>() {
                    @Override
                    public void onNext(JSONObject messageData) {
                        String username;
                        String message;
                        try {
                            username = messageData.getString("user");
                            message = messageData.getString("message");
                        } catch (JSONException e) {
                            Log.e(TAG, "run: ", e);
                            return;
                        }

                        hideTyping(username);
                        addMessage(username, message);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                })
        );
        //onTyping/onStopTyping
        mListenersDisposable.add(mSocketManager.createTypingListener()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<JSONObject>() {
                    @Override
                    public void onNext(JSONObject typingData) {
                        String username;
                        boolean isTyping;
                        try {
                            username = typingData.getString("user");
                            isTyping = typingData.getBoolean("typing");

                        } catch (JSONException e) {
                            return;
                        }
                        if (isTyping) {
                            hideTyping(username);
                        } else {
                            showTyping(username);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                }));

        // Ensure that connection is established
        mSocketManager.connect();
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
                    sendMessage();
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

                if (!mTyping) {
                    mTyping = true;
                    mSocketManager.startTyping();
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
                sendMessage();
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_chat, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_leave) {
            leave();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void addAllMessages(List<Question> messages) {
        for (Question message : messages) {
            addMessage(message.getUsername(), message.getMessage());
        }
    }

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

    private void showTyping(String username) {
        mMessages.add(new Message.Builder("none", Message.TYPE_ACTION)
                .username(username).build());
        mAdapter.notifyItemInserted(mMessages.size() - 1);
        scrollToBottom();
    }

    private void hideTyping(String username) {
        for (int i = mMessages.size() - 1; i >= 0; i--) {
            Message message = mMessages.get(i);
            if (message.getType() == Message.TYPE_ACTION && message.getUsername().equals(username)) {
                mMessages.remove(i);
                mAdapter.notifyItemRemoved(i);
            }
        }
    }

    private void sendMessage() {
        if (null == mUsername) return;

        mTyping = false;

        String message = mInputMessageView.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            mInputMessageView.requestFocus();
            return;
        }

        mInputMessageView.setText("");

        JSONObject obj = new JSONObject();
        try {
            obj.put("message", message.trim());
            obj.put("type", Message.TYPE_MESSAGE_OWN);
            obj.put("course", Question.DEFAULT_COURSE);
            obj.put("questionId", mQuestion.getId());
            mSocketManager.sendMessage(obj);

        } catch (JSONException e) {
            Log.e(TAG, "sendMessage: ", e);
        }
    }

    private void startSignIn() {
        mUsername = null;
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        startActivityForResult(intent, REQUEST_LOGIN);
    }

    private void leave() {
        mUsername = null;
        mListenersDisposable.dispose();
        mSocketManager.disconnect();
        startSignIn();
    }

    private void scrollToBottom() {
        mMessagesView.scrollToPosition(mAdapter.getItemCount() - 1);
    }

    private void showToast(int stringResourceId) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(getActivity().getApplicationContext(), stringResourceId, Toast.LENGTH_LONG);
        mToast.show();
    }

    private Runnable onTypingTimeout = new Runnable() {
        @Override
        public void run() {
            if (!mTyping) return;
            mTyping = false;
            mSocketManager.stopTyping();

        }
    };


    public void fetchFailed(Exception e) {
        Log.e(TAG, "fetchFailed: ", e);
        if(getActivity() != null) {
            getActivity().finish();
        }

    }

    public void onQuestionInfoLoaded(Question question) {
        mQuestion = question;

        inflateQuestionDetails();

        JSONObject obj = new JSONObject();
        try {
            obj.put("room", mQuestion.getId());
            mSocketManager.selectRoom(obj);
        } catch (JSONException e) {
            Log.e(TAG, "sendMessage: ", e);
        }
    }

    public void onMessagesLoaded(List<Question> messages) {
        addAllMessages(messages);
    }

    private void inflateQuestionDetails() {
        setQuestionMessage(mQuestion.getMessage());
        setCreated(mQuestion.getCreated());
        displayAsSolved(mQuestion.isSolved());
        setUpvotes(mQuestion.getUpvotes());
        setQuestionBackgroundColor();
        mQuestionDetailsContainer.setVisibility(View.VISIBLE);
    }


    private void initViews(View view) {

        mQuestionDetailsContainer = view.findViewById(R.id.question_details);
        mCreatedView = (TextView) view.findViewById(R.id.question_created);
        mQuestionView = (TextView) view.findViewById(R.id.question_message);
        mUpvotesView = (TextView) view.findViewById(R.id.label_upvotes);
        mSolvedIcon = (ImageView) view.findViewById(R.id.icon_solved);

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
        int questionBackgroundColor = ((ChatActivity) getActivity()).getQuestionBackgroundColor();
        ((ChatActivity) getActivity()).matchToolbarColorWithQuestion();
        View parent = (View) mCreatedView.getParent();
        parent.setBackgroundColor(questionBackgroundColor);
    }

    public void setUpvotes(int upvotes) {
        mUpvotesView.setText(Integer.toString(upvotes));
    }
}