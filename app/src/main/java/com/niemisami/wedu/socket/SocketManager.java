package com.niemisami.wedu.socket;

import android.content.Context;

import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.niemisami.wedu.R;

import java.net.URISyntaxException;

/**
 * Created by sakrnie on 22.6.2017.
 */

public class SocketManager {

    private static SocketManager mInstance;
    private static String CHAT_SERVER_URL;
    private Context mContext;
    private Socket mSocket;

    private SocketManager(Context context) {
        mContext = context;
        //        CHAT_SERVER_URL = getString(R.string.server_end_point_heroku);
        CHAT_SERVER_URL = mContext.getString(R.string.server_end_point_local);

        try {
            mSocket = IO.socket(CHAT_SERVER_URL);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Socket singleton
     */
    public static SocketManager getSocket(Context context) {
        if (mInstance == null) {
            mInstance = new SocketManager(context);
        }
        return mInstance;
    }


}
