package me.eighttenlabs.steazy;

/**
 * Created by Ian on 1/21/2015.
 */
public class SpotifyWebObject implements Comparable<SpotifyWebObject> {

    public String tag;
    private float popularity;

    public SpotifyWebObject(float prob, String tag) {
        this.popularity = prob;
        this.tag = tag;
    }

    @Override
    public int compareTo(SpotifyWebObject another) {
        return (int) (popularity * 100) - (int) (another.popularity * 100);
    }

    public float getPopularity() {
        return popularity;
    }

    public String getTag() {
        return tag;
    }
}
