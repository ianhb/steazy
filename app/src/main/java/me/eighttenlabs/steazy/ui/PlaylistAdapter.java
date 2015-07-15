package me.eighttenlabs.steazy.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import me.eighttenlabs.steazy.R;
import me.eighttenlabs.steazy.wrappers.Playlist;

/**
 * PlaylistAdapter to show playlists in ListView
 *
 * Created by Ian on 6/25/2015.
 */
public class PlaylistAdapter extends BaseAdapter {

    private List<Playlist> playlists;
    private LayoutInflater playlistInf;

    public PlaylistAdapter(Context c, List<Playlist> playlists) {
        playlistInf = LayoutInflater.from(c);
        this.playlists = playlists;
    }

    @Override
    public int getCount() {
        return playlists.size();
    }

    @Override
    public Object getItem(int position) {
        return playlists.get(position);
    }

    @Override
    public long getItemId(int position) {
        return playlists.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LinearLayout layout = (LinearLayout) playlistInf.inflate(R.layout.playlist, parent, false);
        Playlist playlist = playlists.get(position);
        TextView name = (TextView) layout.findViewById(R.id.playlist_layout_name);
        TextView owner = (TextView) layout.findViewById(R.id.playlist_layout_owner);

        name.setText(playlist.getName());
        owner.setText(playlist.getOwnerName());

        layout.setTag(position);
        return layout;
    }
}
