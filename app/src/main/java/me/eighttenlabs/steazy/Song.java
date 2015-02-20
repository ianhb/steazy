package me.eighttenlabs.steazy;

/**
 * Created by Ian on 1/19/2015.
 */
public class Song extends WebObject {

    public String name;
    public String[] artists;
    public String album;
    public String source;

    public Song(String name, String[] artists, String album, String tag, float popularity, String source) {
        super(popularity, tag);
        this.name = name;
        this.artists = artists;
        this.album = album;
        this.source = source;
    }
}
