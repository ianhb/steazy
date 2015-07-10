package me.eighttenlabs.steazy;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import me.eighttenlabs.steazy.wrappers.Song;

/**
 * SongAdapter to show songs in ListView
 *
 * Created by Ian on 1/18/2015.
 */
public class SongAdapter extends BaseAdapter {

    private ArrayList<Song> songs;
    private LayoutInflater songInf;

    public SongAdapter(Context c, ArrayList<Song> songs) {
        this.songs = songs;
        songInf = LayoutInflater.from(c);
    }

    @Override
    public Object getItem(int position) {
        return songs.get(position);
    }

    @Override
    public int getCount() {
        return songs.size();
    }

    @Override
    public long getItemId(int position) {
        return songs.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LinearLayout layout = (LinearLayout) songInf.inflate(R.layout.song, parent, false);
        Song curSong = songs.get(position);
        TextView artist = (TextView) layout.findViewById(R.id.song_layout_artist);
        TextView name = (TextView) layout.findViewById(R.id.song_layout_name);
        TextView source = (TextView) layout.findViewById(R.id.song_layout_source);
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
}
