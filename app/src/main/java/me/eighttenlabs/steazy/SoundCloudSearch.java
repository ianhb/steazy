package me.eighttenlabs.steazy;

import android.os.AsyncTask;
import android.util.Log;

import com.soundcloud.api.ApiWrapper;
import com.soundcloud.api.Request;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Ian on 2/11/2015.
 */
public class SoundCloudSearch extends AsyncTask<String, String, ArrayList<Song>> {

    private ApiWrapper wrapper;

    public SoundCloudSearch(ApiWrapper wrapper) {
        this.wrapper = wrapper;
    }

    @Override
    protected ArrayList<Song> doInBackground(String... params) {
        ArrayList<Song> soundCloudTracks = new ArrayList<>();
        try {
            Log.d("request", params[0]);
            Request request = Request.to("/tracks");
            request.add("q", params[0]);
            request.add("order", "hotness");
            HttpResponse trackResp = wrapper.get(request);
            String jsonString = EntityUtils.toString(trackResp.getEntity());
            JSONArray tracks = new JSONArray(jsonString);
            for (int i = 0; i < tracks.length(); i++) {
                JSONObject song = tracks.getJSONObject(i);
                if (song.getBoolean("streamable") && (song.getInt("playback_count") > 5)) {
                    String source = song.getString("stream_url");
                    String title = song.getString("title");
                    String[] artist = {song.getString("label_name")};
                    String album = song.getString("description");
                    Log.d("Title", title);
                    soundCloudTracks.add(new Song(title, artist, album, source, (tracks.length() - i) / tracks.length(), "SoundCloud"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return soundCloudTracks;
    }
}
