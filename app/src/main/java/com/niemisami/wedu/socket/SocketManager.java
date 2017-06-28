package com.niemisami.wedu.socket;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.niemisami.wedu.utils.MessageApiService;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.CompletableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
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
     * @return SocketManager instance
     */
    public static SocketManager getSocketManager() {
        if (mInstance == null) {
            mInstance = new SocketManager();
        }
        return mInstance;
    }

    public void connect() {
        if (!mSocket.connected()) {
            mSocket.connect();
        }
    }

    public void disconnect() {
        mSocket.disconnect();
        mSocket = null;
        mInstance = null;
    }

    /**
     * Each listener follows the same structure.
     * Method creates a new observable which registers listener of the provided event into the Socket.
     * Server response is always JSONObject
     */
    private Observable<JSONObject> createObservableSocketAction(final String socketEvent) {
        return Observable.create(new ObservableOnSubscribe<JSONObject>() {
            @Override
            public void subscribe(final ObservableEmitter<JSONObject> emitter) throws Exception {
                final Emitter.Listener listener = new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        if (args.length > 0 && args[0] instanceof JSONObject) {
                            emitter.onNext((JSONObject) args[0]);
                        } else {
                            emitter.onError(new Exception("Failed to handle request"));
                        }
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

    private Completable createCompletableSocketAction(final String socketEvent) {
        return CompletableCreate.create(new CompletableOnSubscribe() {
            @Override
            public void subscribe(final CompletableEmitter emitter) throws Exception {
                final Emitter.Listener listener = new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        emitter.onComplete();
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


    //FIXME: Study if connection/disconnection Completables should also be converted to Observables
    // COMPLETABLES
    public Completable createConnectionListener() {
        return createCompletableSocketAction(Socket.EVENT_CONNECT);
    }

    public Completable createDisconnectionListener() {
        return createCompletableSocketAction(Socket.EVENT_DISCONNECT);
    }

    public Completable createConnectErrorListener() {
        return createCompletableSocketAction(Socket.EVENT_CONNECT_ERROR);
    }

    public Completable createConnectTimeoutListener() {
        return createCompletableSocketAction(Socket.EVENT_CONNECT_TIMEOUT);
    }

    // OBSERVABLES
    public Observable<JSONObject> createLoggedUsersCountListener() {
        return createObservableSocketAction(EVENT_USER_LOGIN);
    }

    public Observable<JSONObject> createMessageListener() {
        return createObservableSocketAction(EVENT_NEW_MESSAGE);
    }

    public Observable<JSONObject> createUserJoinedListener() {
        return createObservableSocketAction(EVENT_USER_JOINED);
    }

    public Observable<JSONObject> createUserLeftListener() {
        return createObservableSocketAction(EVENT_USER_LEFT);
    }

    public Observable<JSONObject> createMessageVotedListener() {
        return createObservableSocketAction(EVENT_VOTED);
    }

    // Pass an extra value "typing" along with received JSONObject which tells whether user x is typing or not
    public Observable<JSONObject> createTypingListener() {
        return Observable.create(new ObservableOnSubscribe<JSONObject>() {
            @Override
            public void subscribe(final ObservableEmitter<JSONObject> emitter) throws Exception {
                final Emitter.Listener startTypingListener = new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        if (args.length > 0 && args[0] instanceof JSONObject) {
                            JSONObject typingUser = (JSONObject) args[0];
                            try {
                                typingUser.put("typing", true);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            emitter.onNext(typingUser);
                        } else {
                            emitter.onError(new Exception("Failed to handle request"));
                        }
                    }
                };
                final Emitter.Listener stopTypingListener = new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        if (args.length > 0 && args[0] instanceof JSONObject) {
                            JSONObject typingUser = (JSONObject) args[0];
                            try {
                                typingUser.put("typing", false);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            emitter.onNext(typingUser);
                        } else {
                            emitter.onError(new Exception("Failed to handle request"));
                        }
                    }
                };
                mSocket.on(EVENT_TYPING, startTypingListener);
                mSocket.on(EVENT_STOP_TYPING, stopTypingListener);

                emitter.setCancellable(new Cancellable() {
                    @Override
                    public void cancel() throws Exception {
                        mSocket.off(EVENT_TYPING, startTypingListener);
                        mSocket.off(EVENT_STOP_TYPING, stopTypingListener);
                    }
                });
            }
        });
    }

    /**
     * Send typing action to the server
     */
    public void startTyping() {
        mSocket.emit(EVENT_TYPING);
    }

    /**
     * Send stop typing action to the server
     * <pre>userData: {
     *     user: String
     * }</pre>
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
     * <pre>: {
     *     messageId: String
     *  }</pre>
     */
    public void upvote(JSONObject voteData) {
        mSocket.emit(EVENT_UPVOTE, voteData);
    }

    /**
     * <pre>userData: {
     *     messageId: String
     *  }</pre>
     */
    public void downvote(JSONObject voteData) {
        mSocket.emit(EVENT_DOWNVOTE, voteData);
    }


}
