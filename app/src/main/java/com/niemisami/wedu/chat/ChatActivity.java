package com.niemisami.wedu.chat;

import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;

import com.niemisami.wedu.R;
import com.niemisami.wedu.question.Question;
import com.niemisami.wedu.utils.MessageFetchTask;
import com.niemisami.wedu.utils.MessageJsonParser;
import com.niemisami.wedu.utils.NetworkUtils;
import com.niemisami.wedu.utils.WeduNetworkCallbacks;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.R.attr.fragment;

public class ChatActivity extends AppCompatActivity {

    public static final String TAG = ChatActivity.class.getSimpleName();

    private OkHttpClient mClient;
    private String mQuestionId;
    private int mQuestionBackgroundColor;

    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mClient = new OkHttpClient();
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
        }
        return true;
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if (fragment instanceof WeduNetworkCallbacks) {
            String requestUrl = "getMessage/" + mQuestionId;
            new MessageFetchTask(this, (WeduNetworkCallbacks) fragment).execute(requestUrl);
        }
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


