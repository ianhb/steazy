package me.eighttenlabs.steazy;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Ian on 1/18/2015.
 */
public class Adapter extends BaseAdapter {

    private ArrayList<Song> list;
    private LayoutInflater songInf;

    public Adapter(Context c, ArrayList<Song> list) {
        this.list = list;
        songInf = LayoutInflater.from(c);
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        Song object = list.get(position);
        if (object instanceof Song) {
            LinearLayout layout = (LinearLayout) songInf.inflate(R.layout.song, parent, false);
            Song curSong = (Song) object;
            TextView artist = (TextView) layout.findViewById(R.id.artist);
            TextView name = (TextView) layout.findViewById(R.id.name);
            TextView source = (TextView) layout.findViewById(R.id.source);
            String finalArtistString = "";
            for (String art : curSong.artists) {
                finalArtistString += art + ", ";
            }
            finalArtistString = finalArtistString.substring(0, finalArtistString.length() - 2);
            artist.setText(finalArtistString);
            name.setText(curSong.name);
            source.setText(curSong.source);
            layout.setTag(position);
            return layout;
        }
        /**
        if (object instanceof Artist) {
            Artist artist = (Artist) object;
            LinearLayout layout = (LinearLayout) songInf.inflate(R.layout.artist, parent, false);
            ((TextView) layout.findViewById(R.id.artist_name)).setText(artist.getName());
            layout.setTag(position);
            return layout;
        }
         **/


        return convertView;
    }
}