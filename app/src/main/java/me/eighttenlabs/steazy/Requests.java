package me.eighttenlabs.steazy;

import android.content.Context;
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

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ian on 6/18/2015.
 */
public class Requests {

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

        public Login(String username, String password) {
            final Map<String, String> headers = new HashMap<>();
            headers.put("username", username);
            headers.put("password", password);
            JsonObjectRequest loginRequest = new JsonObjectRequest(
                    Request.Method.PUT,
                    URL + "/login/",
                    null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                TOKEN = response.getString("token");
                            } catch (JSONException e) {
                                Log.d("Login Failed", e.getMessage());
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d("Request Error", error.getMessage());
                }
            }
            ) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    return headers;
                }
            };
        }
    }

    private static class TokenRequest {

        public static final int GET = JsonObjectRequest.Method.GET;
        public static final int PUT = JsonObjectRequest.Method.PUT;
        public static final int POST = JsonObjectRequest.Method.POST;
        public static final int DELETE = JsonObjectRequest.Method.DELETE;

        private JsonObjectRequest request;

        public TokenRequest(String path, JSONObject data, int method, Response.Listener listener, final Map<String, String> params) {
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
                            Log.d("Request Error", error.getMessage());
                        }
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> params = new HashMap<>();
                    params.put("Authorization", "Token " + TOKEN);
                    params.put("Content-Type", "application/json");
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

    private static class TokenArrayRequest {

        public static final int GET = JsonObjectRequest.Method.GET;
        public static final int PUT = JsonObjectRequest.Method.PUT;
        public static final int POST = JsonObjectRequest.Method.POST;
        public static final int DELETE = JsonObjectRequest.Method.DELETE;

        private JsonArrayRequest request;

        public TokenArrayRequest(String path, JSONArray data, int method, Response.Listener listener, final Map<String, String> params) {
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
                            Log.d("Request Error", error.getMessage());
                        }
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> params = new HashMap<>();
                    params.put("Authorization", "Token " + TOKEN);
                    params.put("Content-Type", "application/json");
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

        public Search(String query, Response.Listener listener) {
            Map<String, String> params = new HashMap<>();
            params.put("query", query);
            TokenArrayRequest request = new TokenArrayRequest("/search/", null, TokenRequest.GET, listener, params);
        }

    }

}
