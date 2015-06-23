package me.eighttenlabs.steazy;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Encompasses the network requests to the servers
 *
 * Created by Ian on 6/18/2015.
 */
public class Requests {

    public static final int GET = JsonObjectRequest.Method.GET;
    public static final int PUT = JsonObjectRequest.Method.PUT;
    public static final int POST = JsonObjectRequest.Method.POST;
    public static final int DELETE = JsonObjectRequest.Method.DELETE;

    private static String BASEURL;
    private static NetworkQueue QUEUE;
    private static String TOKEN = "";

    public static void setQueue(Context context) {
        QUEUE = NetworkQueue.getInstance(context);
        BASEURL = context.getString(R.string.baseUrl);
    }

    private static void checkAuthStatus() throws Exception {
        if (QUEUE == null) {
            throw new Exception("Queue not initialized");
        }
        if (TOKEN == null) {
            throw new Exception("TOKEN not set");
        }
    }

    public static class Login {

        public Login(final String username, final String password, Response.Listener<JSONObject> listener) {
            JSONObject json = new JSONObject();
            try {
                json.put("username", username);
                json.put("password", password);
            } catch (JSONException e) {
                Log.d("JSON Exception", e.toString());
            }
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    BASEURL + "/login/",
                    json,
                    listener
                    , new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d("Request Error", error.toString());
                }
            }
            );
            QUEUE.addtToRequestQueue(request);
        }

        public Login(final String username, final String password) {
            this(username, password, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        TOKEN = response.getString("token");
                        Log.d("Login", TOKEN);
                    } catch (JSONException e) {
                        Log.d("Login Failed", e.getMessage());
                    }
                }
            });
        }
    }

    private static class TokenRequest {

        private JsonObjectRequest request;

        public TokenRequest(String path, final JSONObject data, int method, Response.Listener<JSONObject> listener, final Map<String, String> params) {
            try {
                checkAuthStatus();
            } catch (Exception e) {
                return;
            }
            request = new JsonObjectRequest(
                    method,
                    BASEURL + path,
                    data,
                    listener,
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d("Request Error", error.toString());
                        }
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    HashMap<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Token " + TOKEN);
                    return headers;
                }

                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    return params;
                }
            };
            QUEUE.addtToRequestQueue(request);
        }
    }

    private static class TokenArrayRequest {
        private JsonArrayRequest request;
        public TokenArrayRequest(String path, JSONArray data, int method, Response.Listener<JSONArray> listener, final Map<String, String> params) {
            try {
                checkAuthStatus();
            } catch (Exception e) {
                return;
            }
            request = new JsonArrayRequest(
                    method,
                    BASEURL + path,
                    data,
                    listener,
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d("Request Error", error.toString());
                        }
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> params = new HashMap<>();
                    params.put("Authorization", "Token " + TOKEN);
                    return params;
                }

                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    if (params != null) {
                        return params;
                    } else {
                        return super.getParams();
                    }
                }
            };
            QUEUE.addtToRequestQueue(request);
        }
    }

    public static class Search {
        public Search(String query, Response.Listener<JSONArray> listener) {
            query = query.replaceAll(" ", "%20");
            new TokenArrayRequest("/songs/?query=" + query,
                    null, Requests.GET, listener, null);
        }
    }

    public static class GetPlaylists {
        public GetPlaylists(Response.Listener<JSONArray> listener) {
            new TokenArrayRequest("/playlists/", null, Requests.GET, listener, null);
        }
    }

    public static class AddSongToPlaylist {
        public AddSongToPlaylist(final int songId, final int playlistId) {
            try {
                JSONObject data = new JSONObject();
                data.put("playlist", playlistId);
                data.put("song", songId);
                Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (BuildConfig.DEBUG && (response.getInt("song") != songId || response.getInt("playlist") != playlistId)) {
                                throw new AssertionError("Response doesn't match send");
                            }
                        } catch (JSONException e) {
                            Log.d("JSON Error", "Unable to decode response");
                        }
                    }
                };
                new TokenRequest("/add/", data, Requests.POST, listener, null);
            } catch (JSONException e) {
                Log.d("JSON Error", "Unable to add song");
            }

        }
    }

    public static class Play {
        public Play(final Song song) {
            String obj = "{\"song\":" + song.getId() + "}";
            Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        if (BuildConfig.DEBUG && (response.getInt("id") != song.getId())) {
                            throw new AssertionError("Song Doesn't match");
                        }
                    } catch (JSONException e) {
                        Log.d("Json Error", e.toString());
                    }
                }
            };
            try {
                final JSONObject object = new JSONObject(obj);
                new TokenRequest("/play/", object, Requests.POST, listener, null);
            } catch (JSONException e) {
                Log.d("Json Error", e.toString());
            }
        }
    }

    public static class CreateAccount {
        /**
         * Sends a network request to create an account on the backend
         * If connection is successful, returns to listener a JSON object of {'key':token for new account}
         * If username or email is taken, returns a JSON object of
         * {'username'/'email': ["This field must be unique"}
         * If connection fails, calls eListener.
         *
         * @param username  username for new account
         * @param password  password for new account
         * @param email     email for new account
         * @param listener  listener to process feedback from server
         * @param eListener listener to process errors in connection
         */
        public CreateAccount(String username, String password, String email,
                             Response.Listener<JSONObject> listener,
                             Response.ErrorListener eListener) {
            try {
                JSONObject accountData = new JSONObject();
                accountData.put("email", email);
                accountData.put("username", username);
                accountData.put("password", password);
                JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST,
                        BASEURL + "/users/create/",
                        accountData,
                        listener,
                        eListener
                );
                QUEUE.addtToRequestQueue(request);
            } catch (JSONException e) {
                Log.d("Json Error", e.toString());
            }
        }
    }

    public static class SoundcloudRedirect extends AsyncTask<Song, Void, String> {
        MediaPlayer player;

        public SoundcloudRedirect(final MediaPlayer player) {
            this.player = player;
        }

        @Override
        protected String doInBackground(Song... params) {
            try {
                URL url = new URL("https://api.soundcloud.com/tracks/" + params[0].tag +
                        "/stream?client_id=" + MainActivity.SOUNDCLOUD_CLIENT_ID);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setInstanceFollowRedirects(false);
                connection.connect();
                String location = connection.getHeaderField("Location");
                Log.i("Location", connection.getHeaderField("Location"));
                connection.disconnect();
                return location;
            } catch (IOException e) {
                Log.d("Bad Url", e.toString());
            }
            return "";
        }

        @Override
        protected void onPostExecute(String s) {
            try {
                player.reset();
                player.setDataSource(s);
                player.prepareAsync();
            } catch (IOException e) {
                Log.d("IO Error", e.toString());
            }
        }
    }

}
