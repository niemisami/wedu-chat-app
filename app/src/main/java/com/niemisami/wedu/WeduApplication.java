package com.niemisami.wedu;

import android.app.Application;

import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import java.net.URISyntaxException;

/**
 * Created by Sami on 21.2.2017.
 */

public class WeduApplication extends Application {

    private static final String TAG = WeduApplication.class.getSimpleName();

    public static String CHAT_SERVER_URL;
    private Socket mSocket;

    @Override
    public void onCreate() {
        super.onCreate();
        CHAT_SERVER_URL = getString(R.string.server_end_point_heroku);
//        CHAT_SERVER_URL = getString(R.string.server_end_point_local);

        try {
            mSocket = IO.socket(CHAT_SERVER_URL);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public String getAPIEndPoint() {
        return CHAT_SERVER_URL;
    }

    public Socket getSocket() {
        return mSocket;
    }
}