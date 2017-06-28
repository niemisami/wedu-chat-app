package com.niemisami.wedu.chat;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.niemisami.wedu.R;
import com.niemisami.wedu.question.Question;
import com.niemisami.wedu.settings.SettingsActivity;
import com.niemisami.wedu.utils.MessageApiService;

import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class ChatActivity extends AppCompatActivity {

    public static final String TAG = ChatActivity.class.getSimpleName();

    private String mQuestionId;
    private int mQuestionBackgroundColor;

    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mQuestionId = getIntent().getExtras().getString(Question.EXTRA_QUESTION_ID);
        mQuestionBackgroundColor = getIntent().getExtras().getInt(Question.EXTRA_QUESTION_COLOR);
        inflateFragment();

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
        }
        return true;
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if (fragment instanceof ChatFragment) {
            final ChatFragment chatFragment = (ChatFragment) fragment;
            // TODO: combine results. Zip perhaps?
            // TODO: ensure garbage collection
            fetchQuestionInfo(chatFragment);
            fetchQuestionThread(chatFragment);
        }
    }

    private void fetchQuestionInfo(final ChatFragment chatFragment) {
        MessageApiService.getQuestionInfo(mQuestionId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableSingleObserver<Question>() {
                    @Override
                    public void onSuccess(Question question) {
                        chatFragment.onQuestionInfoLoaded(question);
                    }

                    @Override
                    public void onError(Throwable e) {
                        chatFragment.fetchFailed(new Exception("Failed to fetch question information"));
                    }
                });
    }

    private void fetchQuestionThread(final ChatFragment chatFragment) {
        MessageApiService.getQuestionThread(mQuestionId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableSingleObserver<List<Question>>() {
                    @Override
                    public void onSuccess(List<Question> messages) {
                        chatFragment.onMessagesLoaded(messages);
                    }

                    @Override
                    public void onError(Throwable e) {
                        chatFragment.fetchFailed(new Exception("Failed to fetch question thread"));
                    }
                });
    }


    private void inflateFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, new ChatFragment());
        fragmentTransaction.commit();
    }


    public int getQuestionBackgroundColor() {
        return mQuestionBackgroundColor;
    }

    public void matchToolbarColorWithQuestion() {
        mToolbar.setBackgroundColor(mQuestionBackgroundColor);
    }
}


