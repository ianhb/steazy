package me.eighttenlabs.steazy;

/**
 * Created by Ian on 1/19/2015.
 */
public class Song extends SpotifyWebObject {

    public String name;
    public String[] artists;
    public String album;
    public int releaseYear;

    public Song(String name, String[] artists, String album, String tag, float popularity, int releaseYear) {
        super(popularity, tag);
        this.name = name;
        this.artists = artists;
        this.album = album;
        this.releaseYear = releaseYear;
    }
}
