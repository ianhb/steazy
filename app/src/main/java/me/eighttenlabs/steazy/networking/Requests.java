package me.eighttenlabs.steazy.networking;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import me.eighttenlabs.steazy.BuildConfig;
import me.eighttenlabs.steazy.R;
import me.eighttenlabs.steazy.activities.MainActivity;
import me.eighttenlabs.steazy.wrappers.Song;

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
    public static final String SEARCH_ALL = "";
    public static final String SEARCH_DATABASE = "fast/";
    public static String BASEURL;
    private static NetworkQueue QUEUE;
    private static String TOKEN = "";

    /**
     * Sets up the Requests class
     *
     * @param context context with which to get a NetworkQueue
     */
    public static void setQueue(Context context) {
        QUEUE = NetworkQueue.getInstance(context);
        BASEURL = context.getString(R.string.baseUrl);
    }

    public static void setToken(String token) {
        TOKEN = token;
    }

    /***
     * Checks to make sure that the Requests class has been setup to send requests. If not correctly
     * setup, throws an exception
     * @throws Exception error detailing the reason Requests has not been set up properly
     */
    private static void checkAuthStatus() throws Exception {
        if (QUEUE == null) {
            throw new Exception("QUEUE not initialized");
        }
        if (TOKEN == null) {
            throw new Exception("TOKEN not set");
        }
        if (BASEURL == null) {
            throw new Exception("BASEURL has not been set");
        }
    }

    /**
     * Anonymous POST request with given data to given url.
     *
     * @param data map of data to post
     * @param url  url to post to
     * @return string of server response to request
     */
    public static String genericPostRequest(Map<String, String> data, String url) {
        String response = "";

        try {
            URL connUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) connUrl.openConnection();
            connection.setRequestMethod("POST");
            connection.setReadTimeout(5000);
            connection.setConnectTimeout(5000);
            OutputStream os = connection.getOutputStream();
            BufferedWriter connWriter = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            connWriter.write(makePostData(data));
            connWriter.flush();
            connWriter.close();
            os.close();

            String line;
            InputStream is;
            try {
                is = connection.getInputStream();

            } catch (FileNotFoundException e) {
                is = connection.getErrorStream();
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                response += line;
            }
            br.close();
            is.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    /**
     * Converts a map to a HTTP post string
     * @param data map of data
     * @return string of data in UTF encoded HTTP format
     * @throws UnsupportedEncodingException
     */
    private static String makePostData(Map<String, String> data) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (first) {
                first = false;
            } else {
                result.append("&");
            }
            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }
        return result.toString();
    }

    /***
     * Requests a list of songs matching a query
     * @param query search request
     * @param listener listener to act on server response
     */
    public static void search(String query, String type, Response.Listener<JSONArray> listener) {
        try {
            query = URLEncoder.encode(query, "UTF-8");
            new TokenArrayRequest("/songs/" + type + "?query=" + query,
                    null, Requests.GET, listener, null);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            }
        }

    /**
     * Returns the users playlists
     * Playlists are returned as a JSONArray of Playlists
     * Playlists have format
     * {"id":table id of playlist, "name":name, "owner_name":owner's name, "owner":table id of owner, "date_created":string of create date+time
     * "songs":[array of songs]}
     *
     * @param listener listener to respond to server's response
     */
    public static void getPlaylists(Response.Listener<JSONArray> listener) {
        new TokenArrayRequest("/playlists/", null, GET, listener, null);
    }

    /**
     * Creates a new playlist
     * Returns the playlist's data
     *
     * @param name     name to give new playlist
     * @param listener listener to respond to response
         */
    public static void postPlaylist(String name, Response.Listener<JSONObject> listener) {
            try {
                JSONObject data = new JSONObject();
                data.put("name", name);
                new TokenRequest("/playlists/", data, POST, listener, null);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    public static void putPlaylist(int pk, String name, Response.Listener<JSONObject> listener) {
        try {
            JSONObject data = new JSONObject();
            data.put("name", name);
            new TokenRequest("/playlists/" + pk + "/", data, PUT, listener, null);
        } catch (JSONException e) {
            Log.d("Json Error", "Unable to encode data");
        }
    }

    public static void deletePlaylist(int pk, Response.Listener<JSONObject> listener) {
        new TokenRequest("playlists/" + pk + "/", null, DELETE, listener, null);
    }

    /***
         * Adds a song to a playlist
         * @param songId id of song to add
         * @param playlistId id of playlist to add to
         */
    public static void addSongToPlaylist(final int songId, final int playlistId) {
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
                            Log.d("JSON Error", "Unable to parse response");
                        }
                    }
                };
                new TokenRequest("/add/", data, Requests.POST, listener, null);
            } catch (JSONException e) {
                Log.d("JSON Error", "Unable to add song");
            }

        }

    /**
     * Removes a song from a playlist
         * @param linkId id of the song<-->playlist link
         */
    public static void removeSongFromPlaylist(final int linkId) {
            Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject jsonObject) {
                    if (BuildConfig.DEBUG) {
                        try {
                            JSONArray songs = jsonObject.getJSONArray("songs");
                            for (int i=0;i<songs.length();i++) {
                                if (songs.getJSONObject(i).getInt("id") == linkId) {
                                    throw new AssertionError("Song Not deleted");
                                }
                            }
                        } catch (JSONException e) {
                            Log.d("JSON Error", "Unable to parse response");
                        }
                    }
                }
            };
            new TokenRequest("add/" + linkId + "/", null, DELETE, listener, null);
        }

    public static void removeSongFromPlaylist(final int linkId, Response.Listener<JSONObject> listener) {
        new TokenRequest("add/" + linkId + "/", null, DELETE, listener, null);
    }

    /**
     * Sends a notice to the server that a user played a song
     *
     * @param song song played
     */
    public static void play(final Song song) {
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

    public static void setState(Response.Listener<JSONObject> listener) {
        new TokenRequest("/users/token", null, GET, listener, null);
    }

    /**
     * Get Spotify access code.
     * Precondition: Authorization has already been granted to steazy
     *
     * @param listener listener to act on response
     */
    public static void getAuthToken(Response.Listener<JSONObject> listener) {
        new TokenRequest("/users/spotifyaccess", null, GET, listener, null);
    }

    private static class TokenRequest {

        private JsonObjectRequest request;

        /**
         * A generic JSON request attributed to the user. Returns an object and takes an object as data. Automatically sends the token of the user.
         * Errors are logged. Allows for custom parameters to be sent along with the JSON data
         *
         * @param path     url extension from BASEURL
         * @param data     JSON Post data
         * @param method   HTTP Method (Requests.GET/POST/PUT/DELETE)
         * @param listener listener to act on response
         * @param params   additional parameters to send to server
         */
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

        /***
         * A generic JSONArray request attributed to the user. Returns an array and takes an array as data. Automatically sends the token of the user.
         * Errors are logged. Allows for custom parameters to be sent along with the JSON data
         * @param path url extension from BASEURL
         * @param data JSON Post data
         * @param method HTTP Method (Requests.GET/POST/PUT/DELETE)
         * @param listener listener to act on response
         * @param params additional parameters to send to server
         */
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

    /**
     * Used to find the stream for a Soundcloud song
     */
    public static class SoundcloudRedirect extends AsyncTask<Song, Void, String> {
        MediaPlayer player;

        /***
         * Initializes a redirect request to play a stream on player
         * @param player the player to start playing the stream on
         */
        public SoundcloudRedirect(final MediaPlayer player) {
            this.player = player;
        }

        /***
         * Gets the redirect url for the first song in params
         * @param params a length 1 array of a soundcloud song id string
         * @return url of the stream for song in params
         */
        @Override
        protected String doInBackground(Song... params) {
            try {
                URL url = new URL("https://api.soundcloud.com/tracks/" + params[0].tag +
                        "/stream?client_id=" + MainActivity.SOUNDCLOUD_CLIENT_ID);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setInstanceFollowRedirects(false);
                connection.connect();
                String location = connection.getHeaderField("Location");
                Log.i("Location", String.valueOf(location == null));
                connection.disconnect();
                return location;
            } catch (IOException e) {
                Log.d("Bad Url", e.toString());
            }
            return "";
        }

        /***
         * Sets starts preparing the song from the request
         * @param s url of the song's stream
         */
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
