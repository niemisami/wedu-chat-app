package com.niemisami.wedu.socket;

import android.content.Context;

import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.niemisami.wedu.utils.MessageApiService;

import java.net.URISyntaxException;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;

/**
 * Created by sakrnie on 22.6.2017.
 */

public class SocketManager {

    private static SocketManager mInstance;
    private Socket mSocket;

    private SocketManager() {
        try {
            mSocket = IO.socket(MessageApiService.SERVER_API_URL);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
    private void destroy() {
        mSocket.disconnect();
        mSocket = null;
        mInstance = null;
    }
    /**
     * Socket singleton
     */
    public static SocketManager getSocket() {
        if (mInstance == null) {
            mInstance = new SocketManager();
        }
        return mInstance;
    }


//      SOCKET ACTIONS TO IMPLEMENT
//    mSocket = app.getSocket();
//    mSocket.on(Socket.EVENT_CONNECT, onConnect);
//    mSocket.on(Socket.EVENT_DISCONNECT, onDisconnect);
//    mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
//    mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
//    mSocket.on("new message", onNewQuestion);
//    mSocket.on("user joined", onUserJoined);
//    mSocket.on("user left", onUserLeft);
//    mSocket.on("voted", onVoted);
//

    public static Observable<String> createConnectionListener(Socket socket) {
        return Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> e) throws Exception {


            }
        })
    }

}
