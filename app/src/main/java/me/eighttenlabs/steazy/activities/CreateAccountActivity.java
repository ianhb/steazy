package me.eighttenlabs.steazy.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import me.eighttenlabs.steazy.R;
import me.eighttenlabs.steazy.networking.Requests;


public class CreateAccountActivity extends Activity {

    /**
     * Keep track of creation task to ensure it can be cancelled.
     */
    private UserCreateTask mAuthTask = null;

    // UI references
    private EditText mUsernameView;
    private EditText mPasswordView;
    private EditText mEmailView;
    private View mProgressView;
    private View mCreateFormView;

    /**
     * Used as a check to ensure username is valid.
     * Does not check if the username is in use.
     *
     * @param username string to check
     * @return whether provided username is valid
     */
    protected static boolean isUsernameValid(String username) {
        //TODO: add logic
        return username.length() > 4;
    }

    /**
     * Used as a check to ensure email is valid.
     * Does not check if the email is in use.
     *
     * @param email string to check
     * @return whether provided email is valid
     */
    private static boolean isEmailValid(String email) {
        //TODO: add logic
        return email.contains("@");
    }

    /**
     * Used as a check to ensure password is valid.
     *
     * @param password string to check
     * @return whether provided password is valid
     */
    protected static boolean isPasswordValid(String password) {
        //TODO: add logic
        return password.length() > 4;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        // Set up form
        mUsernameView = (EditText) findViewById(R.id.create_username);
        mEmailView = (EditText) findViewById(R.id.create_email);
        mPasswordView = (EditText) findViewById(R.id.create_password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == R.id.create_account || actionId == EditorInfo.IME_ACTION_NONE) {
                    attemptCreate();
                    return true;
                }
                return false;
            }
        });

        Button mCreateButton = (Button) findViewById(R.id.button_create_account);
        mCreateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptCreate();
            }
        });

        mProgressView = findViewById(R.id.create_account_progress);
        mCreateFormView = findViewById(R.id.create_account_Form);

        SharedPreferences preferences = getSharedPreferences(getString(R.string.login_prefs), MODE_PRIVATE);
        preferences.edit().clear().apply();
    }

    /**
     * Attempts to to create an account specified by the creation form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and the creation is canceled.
     */
    public void attemptCreate() {
        if (mAuthTask != null) {
            return;
        }

        mUsernameView.setError(null);
        mEmailView.setError(null);
        mPasswordView.setError(null);

        String username = mUsernameView.getText().toString();
        String email = mEmailView.getText().toString();
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
        else if (!isUsernameValid(username)) {
            mUsernameView.setError(getString(R.string.error_invalid_username));
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
        else if (!isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }
        // Check if user entered an email
        else if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        }
        // Check if email passes basic validity test
        else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }
        if (cancel) {
            // There was an error; don't attempt creation and focus on
            // the form field with an error.
            focusView.requestFocus();
        } else {
            // Show progress spinner and start background task to attempt to
            // create an account.
            showProgress(true);
            mAuthTask = new UserCreateTask(username, password, email);
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

            mCreateFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mCreateFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mCreateFormView.setVisibility(show ? View.GONE : View.VISIBLE);
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
            mCreateFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Saves credentials to shared preferences and starts main activity
     */
    @Override
    public void finish() {
        SharedPreferences preferences = getSharedPreferences(getString(R.string.login_prefs), MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(getString(R.string.login_username), mUsernameView.getText().toString());
        editor.putString(getString(R.string.login_password), mPasswordView.getText().toString());
        editor.apply();
        Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                try {
                    String state = jsonObject.getString("state");
                    String url = "https://accounts.spotify.com/authorize?client_id=" + MainActivity.SPOTIFY_CLIENT_ID +
                            "&response_type=code&scope=playlist-read-private%20playlist-read-collaborative%20streaming&redirect_uri=" + LoginActivity.SPOTIFY_CALLBACK +
                            "&state=" + state;
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                } catch (JSONException e) {
                    Log.d("JSON Exception", e.toString());
                }
            }
        };
        Requests.setState(listener);
        super.finish();
    }

    /**
     * Represents an asynchronous account creation task
     */
    public class UserCreateTask extends AsyncTask<Void, Void, Boolean> {

        private final String mUsername;
        private final String mPassword;
        private final String mEmail;

        public UserCreateTask(String username, String password, String email) {
            mUsername = username;
            mEmail = email;
            mPassword = password;

        }

        @Override
        protected Boolean doInBackground(Void... params) {
            Map<String, String> data = new HashMap<>();
            data.put("username", mUsername);
            data.put("email", mEmail);
            data.put("password", mPassword);

            String response = Requests.genericPostRequest(data, Requests.BASEURL + "users/create/");

            try {
                JSONObject responseJson = new JSONObject(response);
                if (responseJson.has("key")) {
                    Requests.setToken(responseJson.getString("key"));
                    return true;
                } else if (responseJson.has("username")) {
                    mUsernameView.setError(getString(R.string.error_inuse_username));
                    return false;
                } else if (responseJson.has("email")) {
                    mEmailView.setError(getString(R.string.error_inuse_email));
                    return false;
                } else {
                    Log.e("Request Error", responseJson.toString());
                }
            } catch (JSONException e) {
                Log.e("JSON Error", response);
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean sucess) {
            mAuthTask = null;
            showProgress(false);
            if (sucess) {
                finish();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }

}
