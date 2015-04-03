package me.eighttenlabs.steazy;

import android.app.ProgressDialog;
import android.os.AsyncTask;

import com.soundcloud.api.ApiWrapper;
import com.soundcloud.api.Request;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Class to hold all functions related to Soundcloud
 * <p/>
 * Created by Ian on 2/11/2015.
 */
public class SoundCloud {


    public static class SoundCloudSearch extends AsyncTask<String, String, ArrayList<Song>> {

        private ApiWrapper wrapper;
        private ProgressDialog dialog;
        private MainActivity activity;

        public SoundCloudSearch(ApiWrapper wrapper, MainActivity activity) {
            this.wrapper = wrapper;
            dialog = new ProgressDialog(activity);
            this.activity = activity;
        }

        @Override
        protected void onPreExecute() {
            dialog.setMessage("Searching the Cloud for Songs");
            dialog.show();
        }

        @Override
        protected ArrayList<Song> doInBackground(String... params) {
            ArrayList<Song> soundCloudTracks = new ArrayList<>();
            try {
                Request request = Request.to("/tracks");
                request.add("q", params[0]);
                request.add("order", "hotness");
                HttpResponse trackResp = wrapper.get(request);
                String jsonString = EntityUtils.toString(trackResp.getEntity());
                JSONArray tracks = new JSONArray(jsonString);
                for (int i = 0; i < tracks.length(); i++) {
                    JSONObject song = tracks.getJSONObject(i);
                    if (song.getBoolean("streamable")) {
                        String source = song.getString("stream_url");
                        String title = song.getString("title");
                        String[] artist = {song.getString("label_name")};
                        String album = song.getString("description");

                        soundCloudTracks.add(new Song(title, artist, album, source, (tracks.length() - i) / tracks.length(), MusicService.SOUNDCLOUD));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return soundCloudTracks;
        }

        @Override
        protected void onPostExecute(ArrayList<Song> songs) {
            activity.searchCallback(songs);
            dialog.dismiss();
        }
    }

    public static class Redirect extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                ApiWrapper wrapper = new ApiWrapper(MainActivity.SOUNDCLOUD_CLIENT_ID, MainActivity.SOUNDCLOUD_PRIVATE_ID, null, null);
                HttpResponse response = wrapper.get(Request.to(params[0]));
                JSONObject streamObj = new JSONObject(EntityUtils.toString(response.getEntity()));
                return streamObj.getString("location");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return params[0];
        }
    }

}
