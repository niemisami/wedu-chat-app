package com.niemisami.wedu.utils;

import android.content.Context;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Sami on 9.3.2017.
 */

public class NetworkUtils {

//    private NetworkUtils mInstance;
//    private OkHttpClient mClient;
//
//    private NetworkUtils() {
//        mInstance = new NetworkUtils();
//        mClient = new OkHttpClient();
//    }
//
//    public NetworkUtils getInstance() {
//        if (mInstance == null) {
//            mInstance = new NetworkUtils();
//        }
//        return mInstance;
//    }
//
//    public static void getWebservice(Context context) {
//
//
//        final Request request = new Request.Builder().url("http://www.ssaurel.com/tmp/todos").build();
//        mclient.newCall(request).enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        result.setText("Failure !");
//                    }
//                });
//            }
//
//            @Override
//            public void onResponse(Call call, final Response response) {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        try {
//                            result.setText(response.body().string());
//                        } catch (IOException ioe) {
//                            result.setText("Error during get body");
//                        }
//                    }
//                });
//            }
//        });
//    }


}
