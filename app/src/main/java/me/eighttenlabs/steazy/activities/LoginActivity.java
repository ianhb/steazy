package me.eighttenlabs.steazy.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import me.eighttenlabs.steazy.R;
import me.eighttenlabs.steazy.networking.Requests;


/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends Activity {

    public static String SPOTIFY_CALLBACK;

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;
    private SharedPreferences prefs;

    // UI references.
    private EditText mUsernameView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        SPOTIFY_CALLBACK = getString(R.string.baseUrl) + "users/spotifycallback";

        super.onCreate(savedInstanceState);

        // Sets up network queue
        Requests.setQueue(getApplicationContext());

        setContentView(R.layout.activity_login);

        // Set up the login form.
        mUsernameView = (EditText) findViewById(R.id.username);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mSignInButton = (Button) findViewById(R.id.sign_in_button);
        mSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        Button createAccount = (Button) findViewById(R.id.button_create_activity);
        createAccount.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent createIntent = new Intent(LoginActivity.this, CreateAccountActivity.class);
                startActivity(createIntent);
            }
        });

        // Attempts to login if there are credentials saved on the device
        prefs = getSharedPreferences(getString(R.string.login_prefs), Context.MODE_PRIVATE);
        String username = prefs.getString(getString(R.string.login_username), null);
        String password = prefs.getString(getString(R.string.login_password), null);

        if (username != null && password != null) {
            mUsernameView.setText(username);
            mPasswordView.setText(password);
            attemptLogin();
        }
    }


    /**
     * Attempts to sign in the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mUsernameView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String username = mUsernameView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check if user entered a username
        if (TextUtils.isEmpty(username)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        }
        // Check if username passes basic validity test
        else if (!CreateAccountActivity.isUsernameValid(username)) {
            mUsernameView.setText(getString(R.string.error_invalid_username));
            focusView = mUsernameView;
            cancel = true;
        }
        // Check if user entered a password
        else if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        }
        // Check if password passes basic validity test
        else if (!CreateAccountActivity.isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(username, password);
            mAuthTask.execute((Void) null);
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Saves credentials to shared preferences and starts main activity
     */
    @Override
    public void finish() {
        if (!prefs.contains(getString(R.string.login_username)) ||
                !prefs.contains(getString(R.string.login_password))) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(getString(R.string.login_username), mUsernameView.getText().toString());
            editor.putString(getString(R.string.login_password), mPasswordView.getText().toString());
            editor.apply();
        }
        startActivity(new Intent(this, MainActivity.class));
        super.finish();
    }

    /**
     * Represents an asynchronous login task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mUsername;
        private final String mPassword;

        UserLoginTask(String username, String password) {
            mUsername = username;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            Map<String, String> data = new HashMap<>();
            data.put("username", mUsername);
            data.put("password", mPassword);

            String response = Requests.genericPostRequest(data, Requests.BASEURL + "login/");

            try {
                JSONObject responseJson = new JSONObject(response);
                if (responseJson.has("token")) {
                    Requests.setToken(responseJson.getString("token"));
                    return true;
                }
            } catch (JSONException e) {
                Log.e("JSON Error", response);
            }
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                finish();
            } else {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
                SharedPreferences preferences = getSharedPreferences(getString(R.string.login_prefs), MODE_PRIVATE);
                preferences.edit().clear().apply();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}

