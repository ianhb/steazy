package me.eighttenlabs.steazy;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class to wrap a song
 *
 * Created by Ian on 1/19/2015.
 */
public class Song {

    public String name;
    public String[] artists;
    public String album;
    public String source;
    public String tag;
    public float popularity;
    private int id;

    public Song(String name, String[] artists, String album, String tag, float popularity, String source, int id) {
        this.tag = tag;
        this.popularity = popularity;
        this.name = name;
        this.artists = artists;
        this.album = album;
        this.source = source;
        this.id = id;
    }

    public static Song songFromJSON(JSONObject song) {
        try {
            String name = song.getString("name");
            String album = song.getString("album");
            String source = song.getString("source");
            String tag = song.getString("tag");
            float popularity = (float) song.getDouble("inherited_popularity");
            String artistString = song.getString("artist");
            String[] artists = artistString.split(", ");
            int id = song.getInt("id");
            return new Song(name, artists, album, tag, popularity, source, id);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public int getId() {
        return id;
    }
}
