package me.eighttenlabs.steazy;

/**
 * Created by Ian on 1/21/2015.
 */
public class Artist extends SpotifyWebObject {

    private String name;

    public Artist(String name, String tag, float popularity) {
        super(popularity, tag);
        this.name = name;
    }

    public String getName() {
        return name;
    }


}
