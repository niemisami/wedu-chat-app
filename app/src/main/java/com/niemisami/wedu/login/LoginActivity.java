package com.niemisami.wedu.login;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;
import com.niemisami.wedu.R;
import com.niemisami.wedu.WeduApplication;
import com.niemisami.wedu.settings.WeduPreferenceHelper;
import com.niemisami.wedu.utils.AnimationHelper;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = LoginActivity.class.getSimpleName();

    private EditText mUsernameView;
    private String mUsername;

    private Socket mSocket;
    private boolean mTryingToLogin;

    private ProgressBar mLoginProgressBar;
    private Handler mLoginTimeoutHandler;
    private static final int LOGIN_TIMEOUT = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        WeduApplication app = (WeduApplication) getApplication();
        mSocket = app.getSocket();

        mLoginTimeoutHandler = new Handler();

        TextView appLabel = (TextView) findViewById(R.id.label_app_title);
        Typeface custom_font = Typeface.createFromAsset(getAssets(), "fonts/Lobster-Regular.ttf");
        appLabel.setTypeface(custom_font);

        // Set up the login form.
        mUsernameView = (EditText) findViewById(R.id.username_input);
        mUsernameView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        mLoginProgressBar = (ProgressBar) findViewById(R.id.login_progress_bar);
        FrameLayout signInButton = (FrameLayout) findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mSocket.on("login", onLogin);

        mSocket.connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mLoginTimeoutHandler.removeCallbacksAndMessages(null);
        mSocket.off("login", onLogin);
    }

    /**
     * Attempts to sign in the account specified by the login form.
     * If there are form errors (invalid username, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        // Reset errors.
        if(!mSocket.connected()) {
            mSocket.connect();
        }
        if (!mTryingToLogin) {
            mTryingToLogin = true;
            AnimationHelper.alphaIn(mLoginProgressBar);
            mUsernameView.setError(null);

            // Store values at the time of the login attempt.
            String username = mUsernameView.getText().toString().trim();

            // Check for a valid username.
            if (TextUtils.isEmpty(username)) {
                // There was an error; don't attempt login and focus the first
                // form field with an error.
                mUsernameView.setError(getString(R.string.error_field_required));
                mUsernameView.requestFocus();
                mTryingToLogin = false;
                AnimationHelper.alphaOut(mLoginProgressBar);
                return;
            }
            mUsername = username;

            JSONObject obj = new JSONObject();
            try {
                obj.put("user", mUsername);
            } catch (JSONException e) {
                Log.e(TAG, "attemptLogin: ", e);
            }

            // perform the user login attempt.
            mSocket.emit("add user", obj);

            mLoginTimeoutHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mTryingToLogin = false;
                    AnimationHelper.alphaOut(mLoginProgressBar);
                    displayToast(getString(R.string.error_connect));
                    mSocket.connect();
                }
            }, LOGIN_TIMEOUT);
        }
    }

    private Emitter.Listener onLogin = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];

            int numUsers;
            try {
                numUsers = data.getInt("numUsers");
            } catch (JSONException e) {
                return;
            }

            storeUsername();

            Intent intent = new Intent();
            intent.putExtra("user", mUsername);
            intent.putExtra("numUsers", numUsers);
            setResult(RESULT_OK, intent);
            finish();
        }
    };

    private void storeUsername() {
        WeduPreferenceHelper.storeUsername(this, mUsername);
    }

    private void displayToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
