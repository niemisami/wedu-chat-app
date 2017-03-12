package com.niemisami.wedu;

import android.os.AsyncTask;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.niemisami.wedu.question.Question;
import com.niemisami.wedu.question.QuestionsFragment;
import com.niemisami.wedu.utils.FabUpdater;
import com.niemisami.wedu.utils.MessageJsonParser;
import com.niemisami.wedu.utils.ToolbarUpdater;
import com.niemisami.wedu.utils.WeduNetworkCallbacks;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements ToolbarUpdater, FabUpdater {

    private static final String TAG = MainActivity.class.getSimpleName();

    private Toolbar mToolbar;
    private OnFabClickListener mOnFabClickListener;
    private FloatingActionButton mFab;
    private FabUpdater mFabUpdater;


    private OkHttpClient mClient;
    private WeduNetworkCallbacks mNetworkCallbacks;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inflateFragment();

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        mToolbar.setTitle(getString(R.string.app_name));

        mFab = (FloatingActionButton) findViewById(R.id.add_question_fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideFab();
                mOnFabClickListener.onFabClicked();
            }
        });
        mClient = new OkHttpClient();
    }

    private void inflateFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, new QuestionsFragment());
        fragmentTransaction.commit();
    }


    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if (fragment instanceof OnFabClickListener) {
            mFabUpdater = this;
            mOnFabClickListener = (OnFabClickListener) fragment;
        }
        if (fragment instanceof WeduNetworkCallbacks) {
            mNetworkCallbacks = (WeduNetworkCallbacks) fragment;
            new QuestionsFetchTask().execute();
        }
    }

    @Override
    public void setSubtitle(String subtitle) {
        if (mToolbar != null) {
            mToolbar.setSubtitle(subtitle);
        }
    }

    @Override
    public void clearSubtitle() {
        if (mToolbar != null) {
            mToolbar.setSubtitle(null);
        }
    }

    @Override
    public void showFab() {
        mFab.show();

    }

    @Override
    public void hideFab() {
        mFab.hide();

    }

    public interface OnFabClickListener {
        void onFabClicked();
    }

    private class QuestionsFetchTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... ids) {
            mNetworkCallbacks.fetchBegin();
            getWebservice();
            return null;
        }
    }

    private void getWebservice() {

        String requestUrl = getString(R.string.server_end_point_local) + "/message/getQuestions";
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
                                String data = "";
                                try {
                                    data = response.body().string();
                                } catch (IOException e) {
                                    Log.e(TAG, "onResponse: ", e);
                                } catch (NullPointerException e) {
                                    Log.e(TAG, "onResponse: ", e);
                                }
                                mNetworkCallbacks.fetchComplete(data);
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

}
