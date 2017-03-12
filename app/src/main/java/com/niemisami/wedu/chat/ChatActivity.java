package com.niemisami.wedu.chat;

import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.niemisami.wedu.R;
import com.niemisami.wedu.question.Question;
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

public class ChatActivity extends AppCompatActivity {

    public static final String TAG = ChatActivity.class.getSimpleName();

    private OkHttpClient mClient;
    private String mQuestionId;
    private int mQuestionBackgroundColor;
    private WeduNetworkCallbacks mNetworkCallbacks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mClient = new OkHttpClient();
        mQuestionId = getIntent().getExtras().getString(Question.EXTRA_QUESTION_ID);
        mQuestionBackgroundColor = getIntent().getExtras().getInt(Question.EXTRA_QUESTION_COLOR);
        inflateFragment();
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if (fragment instanceof WeduNetworkCallbacks) {
            mNetworkCallbacks = (WeduNetworkCallbacks) fragment;
        }

        new MessageFetchTask().execute(mQuestionId);
    }

    private void inflateFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, new ChatFragment());
        fragmentTransaction.commit();
    }

    private class MessageFetchTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... ids) {
            if (ids.length == 1 && isDataRequired()) {
                mNetworkCallbacks.fetchBegin();
                getWebservice(ids[0]);
            }
            return null;
        }
    }

    private void getWebservice(final String requestId) {

        String requestUrl = getString(R.string.server_end_point_local) + "/message/message/" + requestId;
        final Request request = new Request.Builder().url(requestUrl).build();
        mClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isDataRequired())
                            mNetworkCallbacks.fetchFailed(e);
                        else
                            finish();
                    }
                });
            }

            @Override
            public void onResponse(Call call, final Response response) {
                if (isDataRequired()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isDataRequired()) {
                                Question question = null;
                                try {
                                    JSONObject data = new JSONObject(response.body().string());
                                    question = MessageJsonParser.parseQuestion(data);
                                } catch (JSONException e) {
                                    Log.e(TAG, "onResponse: ", e);
                                } catch (IOException e) {
                                    Log.e(TAG, "onResponse: ", e);
                                } catch (NullPointerException e) {
                                    Log.e(TAG, "onResponse: ", e);
                                }
                                mNetworkCallbacks.fetchComplete(question);
                            }
                        }
                    });
                }
            }
        });
    }

    private boolean isDataRequired() {
        return mNetworkCallbacks != null;
    }

    public int getQuestionBackgroundColor() {
        return mQuestionBackgroundColor;
    }
}


