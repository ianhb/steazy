package me.eighttenlabs.steazy;

import android.util.Log;
import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Class to wrap a playlist
 * <p/>
 * Created by Ian on 6/22/2015.
 */
public class Playlist {

    private static ArrayList<Playlist> playlist = new ArrayList<>();
    private int id;
    private String name;
    private String owner;
    private int ownerId;
    private ArrayList<Pair<Song, Integer>> songPairs;
    private String createDate;

    public Playlist(int id, String name, String owner, int ownerId, ArrayList<Pair<Song, Integer>> songs,
                    String createDate) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.ownerId = ownerId;
        this.songPairs = songs;
        this.createDate = createDate;
    }

    public static Playlist PlaylistFromJSON(JSONObject playlistJSON) {
        try {
            int id = playlistJSON.getInt("id");
            String name = playlistJSON.getString("name");
            String owner = playlistJSON.getString("owner_name");
            int ownerId = playlistJSON.getInt("owner");
            String createDate = playlistJSON.getString("date_created");
            JSONArray songs = playlistJSON.getJSONArray("songs");
            ArrayList<Pair<Song, Integer>> songList = new ArrayList<>();
            for (int i = 0; i < songs.length(); i++) {
                songList.add(new Pair<>(Song.songFromJSON(songs.getJSONObject(i).getJSONObject("song_data")),
                        songs.getJSONObject(i).getInt("id")));
            }
            return new Playlist(id, name, owner, ownerId, songList, createDate);
        } catch (JSONException e) {
            Log.e("JSON Error", e.toString());
            return null;
        }
    }

    public static ArrayList<Playlist> getPlaylist() {
        return playlist;
    }

    public static void setPlaylist(ArrayList<Playlist> playlist) {
        Playlist.playlist = playlist;
    }

    public static void removePlaylist(int id) {
        for (Playlist pL : playlist) {
            if (pL.getId() == id) {
                playlist.remove(pL);
                return;
            }
        }
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public void deleteSong(Song song) {
        for (Pair<Song, Integer> s : songPairs) {
            if (song == s.first) {
                Requests.removeSongFromPlaylist(s.second);
            }
        }
    }

    public ArrayList<Song> getSongs() {
        ArrayList<Song> songList = new ArrayList<>();
        for (Pair<Song, Integer> pair : songPairs) {
            songList.add(pair.first);
        }
        return songList;
    }

    public String getCreateDate() {
        return createDate;
    }

    @Override
    public String toString() {
        return this.name + " by " + this.getOwner();
    }
}
