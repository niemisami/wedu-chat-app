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

    public static final String CHAT_SERVER_URL = "http://wedudev.herokuapp.com";


    private Socket mSocket;

    {
        try {
            mSocket = IO.socket(CHAT_SERVER_URL);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public Socket getSocket() {
        return mSocket;
    }
}