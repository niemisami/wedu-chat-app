package com.niemisami.wedu.question;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
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
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.niemisami.wedu.MainActivity;
import com.niemisami.wedu.R;
import com.niemisami.wedu.chat.ChatActivity;
import com.niemisami.wedu.chat.Message;
import com.niemisami.wedu.course.QuestionsAdapter;
import com.niemisami.wedu.login.LoginActivity;
import com.niemisami.wedu.settings.SettingsActivity;
import com.niemisami.wedu.settings.WeduPreferenceHelper;
import com.niemisami.wedu.socket.SocketManager;
import com.niemisami.wedu.utils.FabUpdater;
import com.niemisami.wedu.utils.MessageApiService;
import com.niemisami.wedu.utils.MessageJsonParser;
import com.niemisami.wedu.utils.ToolbarUpdater;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

import static android.content.ContentValues.TAG;
import static android.content.Context.INPUT_METHOD_SERVICE;
import static com.niemisami.wedu.R.id.questions;

/**
 * A simple {@link Fragment} subclass.
 */
public class QuestionsFragment extends Fragment implements QuestionsAdapter.QuestionOnClickHandler, MainActivity.OnFabClickListener {

    private static final int REQUEST_LOGIN = 0;

    private SocketManager mSocketManager;
    private CompositeDisposable mListenersDisposable;

    private ToolbarUpdater mToolbarUpdater;
    private FabUpdater mFabUpdater;

    private String mUsername;

    private RecyclerView mQuestionsView;
    private RelativeLayout mQuestionInputContainer;
    private EditText mQuestionInputField;
    private ImageButton mSendQuestionButton;
    private TextView mWelcomeText;
    private List<Question> mQuestions;
    private QuestionsAdapter mAdapter;
    private Toast mToast;


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

        // TODO: ensure garbage collection
        MessageApiService.getQuestions()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableSingleObserver<List<Question>>() {
                    @Override
                    public void onSuccess(List<Question> questions) {
                        onQuestionsLoaded(questions);
                    }

                    @Override
                    public void onError(Throwable e) {
                        fetchFailed(new Exception("Failed to fetch questions"));
                    }
                });

        mSocketManager = SocketManager.getSocketManager();
        mListenersDisposable = new CompositeDisposable();

    }

    public void onQuestionsLoaded(List<Question> questions) {
        try {

            if (questions.size() < 10) {
                for (Question q : questions) {
                    addQuestion(q);
                }
            } else {
                mQuestions = questions;
                mAdapter.setQuestions(mQuestions);
                // Hide welcome text
                if (mWelcomeText.getVisibility() == View.VISIBLE) {
                    mWelcomeText.setVisibility(View.GONE);
                }
            }
            if (mQuestions.size() > 0)
                mToolbarUpdater.setTitle(mQuestions.get(0).getCourseId());

        } catch (NullPointerException e) {
            Log.e(TAG, "onResponse: ", e);
        }
    }

    //TODO: Display errors etc
    public void fetchFailed(Exception e) {
        Log.e(TAG, "fetchFailed: ", e);
        getActivity().finish();

    }


    @Override
    public void onResume() {
        super.onResume();

        mUsername = WeduPreferenceHelper.getUsername(getActivity());
        if (mUsername == null || mUsername.length() == 0) {
            startSignIn();
        }

        createSocketListeners();
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

                        obj = new JSONObject();
                        try {
                            obj.put("room", "test-course");
                            mSocketManager.selectRoom(obj);
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
        mListenersDisposable.add(mSocketManager.createMessageVotedListener()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<JSONObject>() {
                    @Override
                    public void onNext(JSONObject messageData) {
                        String messageId = "";
                        int gradedQuestionPosition = -1;
                        int upVotes;
                        try {
                            messageId = messageData.getString("messageId");

                            JSONArray upVotedUsers = messageData.getJSONObject("grade").getJSONArray("upvotes");
                            JSONArray downVotedUsers = messageData.getJSONObject("grade").getJSONArray("downvotes");

                            upVotes = upVotedUsers.length() - downVotedUsers.length();

                            gradedQuestionPosition = findQuestionPositionWithId(messageId);

                        } catch (JSONException e) {
                            Log.e(TAG, "onVoted. Couldn't parse message json with id: " + messageId);
                            return;
                        } catch (Resources.NotFoundException e) {
                            Log.e(TAG, "onVoted. Didn't find question with id: " + messageId);
                            return;
                        }

                        Question gradedQuestion = mQuestions.get(gradedQuestionPosition);

                        final Question votedQuestion = gradedQuestion.toBuilder().upvotes(upVotes).build();
                        mQuestions.remove(gradedQuestionPosition);
                        mQuestions.add(gradedQuestionPosition, votedQuestion);
                        mAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                })
        );
        // onNewQuestionListener
        mListenersDisposable.add(mSocketManager.createMessageListener()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<JSONObject>() {
                    @Override
                    public void onNext(JSONObject questionData) {
                        Question question;
                        if (MessageJsonParser.parseMessageType(questionData) == Question.TYPE_MESSAGE_QUESTION) {
                            question = MessageJsonParser.parseQuestion(questionData);
                        } else {
                            return;
                        }
                        addQuestion(question);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                })
        );

        // Combine both user left and join observables into a one disposable
        Observable<JSONObject> userJoined = mSocketManager.createUserJoinedListener()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
        Observable<JSONObject> userLeft = mSocketManager.createUserLeftListener()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());

        mListenersDisposable.add(Observable.merge(userJoined, userLeft)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<JSONObject>() {
                    @Override
                    public void onNext(JSONObject data) {
                        int numUsers;
                        try {
                            numUsers = data.getInt("numUsers");
                        } catch (JSONException e) {
                            return;
                        }
                        addParticipantsLog(numUsers);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                })
        );


        // Ensure that connection is established
        mSocketManager.connect();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_questions, container, false);

        mWelcomeText = (TextView) view.findViewById(R.id.welcome_text);
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        mListenersDisposable.dispose();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSocketManager.disconnect();
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mQuestions = new ArrayList<>();

        mQuestionsView = (RecyclerView) view.findViewById(questions);
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
                sendQuestion();
                mQuestionInputContainer.setVisibility(View.GONE);
                mFabUpdater.showFab();
            }
        });

        mQuestionInputField = (EditText) view.findViewById(R.id.message_input);
        mQuestionInputField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int id, KeyEvent event) {
                if (id == R.id.send || id == EditorInfo.IME_NULL) {
                    sendQuestion();
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
        mListenersDisposable.dispose();
        mSocketManager.disconnect();
        WeduPreferenceHelper.clearUsername(getActivity());
        startSignIn();
    }

    private void startSignIn() {
        mUsername = null;
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        startActivityForResult(intent, REQUEST_LOGIN);
    }


    private void sendQuestion() {
        if (null == mUsername) return;

        String message = mQuestionInputField.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            mQuestionInputField.requestFocus();
            return;
        }
        mQuestionInputField.setText("");
        // perform the sending message attempt.

        JSONObject obj = new JSONObject();
        try {
            obj.put("message", message.trim());
            obj.put("type", Message.TYPE_MESSAGE_QUESTION);
            obj.put("course", Question.DEFAULT_COURSE);
            mSocketManager.sendMessage(obj);

        } catch (JSONException e) {
            Log.e(TAG, "sendQuestion: ", e);
        }
    }

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
                mSocketManager.upvote(obj);

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
                mSocketManager.downvote(obj);

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
        }
    }

    public static void hideKeyboard(Activity activity, View requestingImeView) {

        if (activity != null && requestingImeView != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(requestingImeView.getWindowToken(), 0);
        }
    }


    private void showToast(int stringResourceId) {
        if (mToast != null) {
            mToast.cancel();
        }
        if (getActivity() != null) {
            mToast = Toast.makeText(getActivity().getApplicationContext(), stringResourceId, Toast.LENGTH_LONG);
            mToast.show();
        }
    }


}
