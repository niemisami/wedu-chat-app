package com.niemisami.wedu;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.niemisami.wedu.socket.SocketManager;

import org.json.JSONObject;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.observers.DisposableObserver;

/**
 * Testing Socket Manager
 */
public class RxActivity extends AppCompatActivity {

    private static final String TAG = RxActivity.class.getSimpleName();
    private SocketManager mSocketManager;
    private CompositeDisposable mListenersDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rx);

        mSocketManager = SocketManager.getSocketManager();

        mListenersDisposable = new CompositeDisposable();
        mListenersDisposable.add(mSocketManager.createConnectionListener()
                .subscribeWith(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                        Log.d(TAG, "App connected with backend");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "App connection failed", e);
                    }
                })
        );
        mListenersDisposable.add(mSocketManager.createDisconnectionListener()
                .subscribeWith(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                        Log.d(TAG, "App disconnected");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "App disconnection failed", e);

                    }
                })
        );
        mListenersDisposable.add(mSocketManager.createLoggedUsersCountListener()
                .subscribeWith(new DisposableObserver<JSONObject>() {
                    @Override
                    public void onNext(JSONObject value) {
                        Log.d(TAG, "onNext: " + value);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                }));

        mSocketManager.connect();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mListenersDisposable.dispose();
        mSocketManager.destroy();
    }
}
