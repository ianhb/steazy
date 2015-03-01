package me.eighttenlabs.steazy;

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

    public Song(String name, String[] artists, String album, String tag, float popularity, String source) {
        this.tag = tag;
        this.popularity = popularity;
        this.name = name;
        this.artists = artists;
        this.album = album;
        this.source = source;
    }
}
