package com.niemisami.wedu.utils;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.niemisami.wedu.R;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.content.ContentValues.TAG;

/**
 * Created by Sami on 13.3.2017.
 */

public class MessageFetchTask extends AsyncTask<String, Void, Void> {

    private Context mContext;
    private WeduNetworkCallbacks mNetworkCallbacks;
    private OkHttpClient mClient;

    public MessageFetchTask(Context context, WeduNetworkCallbacks networkCallbacks) {
        mContext = context;
        mNetworkCallbacks = networkCallbacks;
        mClient = new OkHttpClient();

    }

    private boolean isDataRequired() {
        return mNetworkCallbacks != null;
    }

    @Override
    protected Void doInBackground(String... ids) {
        if (ids.length == 1 && isDataRequired()) {
            mNetworkCallbacks.fetchBegin();
            getWebservice(ids[0]);
        }
        return null;
    }

    private void getWebservice(final String requestUrl) {

        String url = mContext.getString(R.string.server_end_point_heroku) + "/message/" + requestUrl;
        final Request request = new Request.Builder().url(url).build();
        mClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                if (isDataRequired())
                    mNetworkCallbacks.fetchFailed(e);
            }

            @Override
            public void onResponse(Call call, final Response response) {
                if (isDataRequired()) {
                    String data = null;
                    try {

                        data = response.body().string();
                    } catch (IOException e) {
                        mNetworkCallbacks.fetchFailed(e);
                        Log.e(TAG, "onResponse: ", e);
                    } catch (NullPointerException e) {
                        mNetworkCallbacks.fetchFailed(e);
                        Log.e(TAG, "onResponse: ", e);
                    }
                    mNetworkCallbacks.fetchComplete(data);

                }
            }
        });
    }
}