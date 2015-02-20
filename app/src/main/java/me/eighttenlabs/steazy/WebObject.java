package me.eighttenlabs.steazy;

/**
 * Created by Ian on 1/21/2015.
 */
public class WebObject implements Comparable<WebObject> {

    public String tag;
    private float popularity;

    public WebObject(float prob, String tag) {
        this.popularity = prob;
        this.tag = tag;
    }

    @Override
    public int compareTo(WebObject another) {
        return (int) (popularity * 100) - (int) (another.popularity * 100);
    }

    public float getPopularity() {
        return popularity;
    }

    public String getTag() {
        return tag;
    }
}
