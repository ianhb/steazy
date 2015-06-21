package me.eighttenlabs.steazy;

import android.content.Context;
import android.media.MediaPlayer;
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

    private static final String URL = "http://steazy-dev.elasticbeanstalk.com";
    private static NetworkQueue QUEUE;
    private static String TOKEN = "";

    public static void setQueue(Context context) {
        QUEUE = NetworkQueue.getInstance(context);
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

        public Login(final String username, final String password) {
            JSONObject json = new JSONObject();
            try {
                json.put("username", username);
                json.put("password", password);
            } catch (JSONException e) {
                Log.d("JSON Exception", e.toString());
            }
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    URL + "/login/",
                    json,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                TOKEN = response.getString("token");
                                Log.d("Login", TOKEN);
                            } catch (JSONException e) {
                                Log.d("Login Failed", e.getMessage());
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d("Request Error", error.toString());
                }
            }
            );
            QUEUE.addtToRequestQueue(request);
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
                    URL + path,
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
                    URL + path,
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

    public static class SoundcloudRedirect {
        public SoundcloudRedirect(Song song, final MediaPlayer player) {
            Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        if (response.getString("status").equals("302 - Found")) {
                            String url = response.getString("location");
                            player.reset();
                            player.setDataSource(url);
                            player.prepareAsync();
                        }
                    } catch (JSONException e) {
                        Log.d("JSONError", e.getMessage());
                    } catch (IOException e) {
                        Log.d("IOException", e.getMessage());
                    }
                }
            };
            new JsonObjectRequest("api.soundcloud.com/tracks/" + song.tag + "/stream/?client_id=" + MainActivity.SOUNDCLOUD_CLIENT_ID,
                    null, listener, null);
        }
    }

}
