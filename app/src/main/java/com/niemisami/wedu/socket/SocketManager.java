package com.niemisami.wedu.socket;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.niemisami.wedu.utils.MessageApiService;

import org.json.JSONObject;

import java.net.URISyntaxException;

import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.CompletableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Cancellable;
import io.reactivex.internal.operators.completable.CompletableCreate;

/**
 * Created by sakrnie on 22.6.2017.
 */

public class SocketManager {

    private static final String TAG = SocketManager.class.getSimpleName();
    private static SocketManager mInstance;
    private Socket mSocket;

    private static final String EVENT_USER_LOGIN = "login";
    private static final String EVENT_ADD_USER = "add user";
    private static final String EVENT_USER_LEFT = "user left";
    private static final String EVENT_USER_JOINED = "user joined";

    private static final String EVENT_TYPING = "typing";
    private static final String EVENT_STOP_TYPING = "stop typing";
    private static final String EVENT_NEW_MESSAGE = "new message";

    private static final String EVENT_SELECT_ROOM = "select room";

    private static final String EVENT_VOTED = "voted";
    private static final String EVENT_UPVOTE = "upvote";
    private static final String EVENT_DOWNVOTE = "downvote";

    private SocketManager() {
        try {
            mSocket = IO.socket(MessageApiService.SERVER_API_URL);

        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Socket singleton
     */
    public static SocketManager getSocketManager() {
        if (mInstance == null) {
            mInstance = new SocketManager();
        }
        return mInstance;
    }

    public void connect() {
        mSocket.connect();
    }

    public void destroy() {
        mSocket.disconnect();
        mSocket = null;
        mInstance = null;
    }

//    mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
//    mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);

//


    public Completable createConnectionListener() {
        return CompletableCreate.create(new CompletableOnSubscribe() {
            @Override
            public void subscribe(final CompletableEmitter emitter) throws Exception {
                final Emitter.Listener onConnected = new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        emitter.onComplete();
                    }
                };
                mSocket.on(Socket.EVENT_CONNECT, onConnected);

                emitter.setDisposable(new Disposable() {
                    @Override
                    public void dispose() {
                        mSocket.off(Socket.EVENT_CONNECT, onConnected);
                    }

                    @Override
                    public boolean isDisposed() {
                        return false;
                    }
                });
            }
        });
    }

    public Completable createDisconnectionListener() {
        return CompletableCreate.create(new CompletableOnSubscribe() {
            @Override
            public void subscribe(final CompletableEmitter emitter) throws Exception {
                final Emitter.Listener onDisconnected = new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        emitter.onComplete();
                    }
                };
                mSocket.on(Socket.EVENT_DISCONNECT, onDisconnected);

                emitter.setCancellable(new Cancellable() {
                    @Override
                    public void cancel() throws Exception {
                        mSocket.off(Socket.EVENT_DISCONNECT, onDisconnected);
                    }
                });
            }
        });
    }

    public Observable<JSONObject> createLoggedUsersCountListener() {
        return Observable.create(new ObservableOnSubscribe<JSONObject>() {
            @Override
            public void subscribe(final ObservableEmitter<JSONObject> emitter) throws Exception {
                final Emitter.Listener onUserCountChanged = new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        emitter.onNext((JSONObject) args[0]);
                    }
                };
                mSocket.on(EVENT_USER_LOGIN, onUserCountChanged);

                emitter.setCancellable(new Cancellable() {
                    @Override
                    public void cancel() throws Exception {
                        mSocket.off(EVENT_USER_LOGIN, onUserCountChanged);
                    }
                });
            }
        });
    }

    public Observable<JSONObject> createMessageListener() {
        return Observable.create(new ObservableOnSubscribe<JSONObject>() {
            @Override
            public void subscribe(final ObservableEmitter<JSONObject> emitter) throws Exception {
                final Emitter.Listener onMessageReceived = new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        emitter.onNext((JSONObject) args[0]);
                    }
                };
                mSocket.on(EVENT_NEW_MESSAGE, onMessageReceived);

                emitter.setCancellable(new Cancellable() {
                    @Override
                    public void cancel() throws Exception {
                        mSocket.off(EVENT_NEW_MESSAGE, onMessageReceived);
                    }
                });
            }
        });
    }

    public Observable<JSONObject> createUserJoinedListener() {
        return Observable.create(new ObservableOnSubscribe<JSONObject>() {
            @Override
            public void subscribe(final ObservableEmitter<JSONObject> emitter) throws Exception {
                final Emitter.Listener onUserJoined = new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        emitter.onNext((JSONObject) args[0]);
                    }
                };
                mSocket.on(EVENT_USER_JOINED, onUserJoined);

                emitter.setCancellable(new Cancellable() {
                    @Override
                    public void cancel() throws Exception {
                        mSocket.off(EVENT_NEW_MESSAGE, onUserJoined);
                    }
                });
            }
        });
    }

    public Observable<JSONObject> createUserLeftListener() {
        return createSocketAction(EVENT_USER_LEFT);
    }

    /**
     * Each listener follows the same structure.
     * Method creates a new observable which registers listener of the provided event into the Socket.
     * Server response is always JSONObject
     */
    public Observable<JSONObject> createSocketAction(final String socketEvent) {
        return Observable.create(new ObservableOnSubscribe<JSONObject>() {
            @Override
            public void subscribe(final ObservableEmitter<JSONObject> emitter) throws Exception {
                final Emitter.Listener listener = new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        emitter.onNext((JSONObject) args[0]);
                    }
                };
                mSocket.on(socketEvent, listener);

                emitter.setCancellable(new Cancellable() {
                    @Override
                    public void cancel() throws Exception {
                        mSocket.off(socketEvent, listener);
                    }
                });
            }
        });
    }

//    mSocket.on("user joined", onUserJoined);
//    mSocket.on("user left", onUserLeft);
//    mSocket.on("voted", onVoted);

    /**
     * Send typing message to the server
     */
    public void startTyping() {
        mSocket.emit(EVENT_TYPING);
    }

    /**
     * Send stop typing message to the server
     */
    public void stopTyping() {
        mSocket.emit(EVENT_STOP_TYPING);
    }

    /**
     * <pre>messageData: {
     *     message: String,
     *     type: int,
     *     course: String,
     *     questionId: String
     * }</pre>
     */
    public void sendMessage(JSONObject messageData) {
        mSocket.emit(EVENT_NEW_MESSAGE, messageData);
    }

//    mSocket.emit("new message", obj);

    /**
     * <pre>userData: {
     *     user: String
     *  }</pre>
     */
    public void addUser(JSONObject userData) {
        mSocket.emit(EVENT_ADD_USER, userData);
    }

    /**
     * <pre>userData: {
     *     room: String
     *  }</pre>
     */
    public void selectRoom(JSONObject roomData) {
        mSocket.emit(EVENT_SELECT_ROOM, roomData);
    }

    /**
     * <pre>userData: {
     *     messageId: String
     *  }</pre>
     */
    public void downvote(JSONObject voteData) {
        mSocket.emit(EVENT_DOWNVOTE, voteData);
    }

    /**
     * <pre>: {
     *     messageId: String
     *  }</pre>
     */
    public void upvote(JSONObject voteData) {
        mSocket.emit(EVENT_UPVOTE, voteData);
    }


}
