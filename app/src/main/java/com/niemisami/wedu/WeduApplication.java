package com.niemisami.wedu;

import android.app.Application;

import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.niemisami.wedu.utils.MessageApiService;

import java.net.URISyntaxException;

/**
 * Created by Sami on 21.2.2017.
 */

public class WeduApplication extends Application {

    private static final String TAG = WeduApplication.class.getSimpleName();

    private Socket mSocket;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            mSocket = IO.socket(MessageApiService.SERVER_API_URL);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public Socket getSocket() {
        return mSocket;
    }
}