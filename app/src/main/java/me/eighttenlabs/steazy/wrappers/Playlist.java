package me.eighttenlabs.steazy.wrappers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import me.eighttenlabs.steazy.networking.Requests;

/**
 * Class to wrap a playlist
 * <p/>
 * Created by Ian on 6/22/2015.
 */
public class Playlist {

    // Stores the user's playlists
    private static List<Playlist> playlist = new ArrayList<>();


    private int id;
    private String name;
    private String ownerName;
    private int owner;
    private String createDate;

    private int length;
    private ArrayList<Song> songs;

    public Playlist(int id, String name, String ownerName, int owner, ArrayList<Song> songs,
                    String createDate) {
        this.id = id;
        this.name = name;
        this.ownerName = ownerName;
        this.owner = owner;
        this.songs = songs;
        this.createDate = createDate;
    }

    public Playlist(int id, String name, String ownerName, int owner, String createDate, int length) {
        this.id = id;
        this.name = name;
        this.ownerName = ownerName;
        this.owner = owner;
        this.createDate = createDate;
        this.length = length;
    }

    public static List<Playlist> PlaylistListFromJSON(JSONArray array) throws JSONException {
        List<Playlist> playlists = new LinkedList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject playlist = array.getJSONObject(i);
            int id = playlist.getInt("id");
            String name = playlist.getString("name");
            String ownerName = playlist.getString("owner_name");
            int owner = playlist.getInt("owner");
            String create_date = playlist.getString("date_created");
            int length = playlist.getInt("length");
            Playlist play = new Playlist(id, name, ownerName, owner, create_date, length);
            playlists.add(play);
        }
        return playlists;
    }

    public static Playlist PlaylistFromJSON(JSONObject playlistDetail) throws JSONException {
        int id = playlistDetail.getInt("id");
        String name = playlistDetail.getString("name");
        String ownerName = playlistDetail.getString("owner_name");
        int owner = playlistDetail.getInt("owner");
        String createDate = playlistDetail.getString("date_created");
        JSONArray songs = playlistDetail.getJSONArray("song_details");
        ArrayList<Song> songList = new ArrayList<>();
        for (int i = 0; i < songs.length(); i++) {
            songList.add(Song.songFromJSON(songs.getJSONObject(i)));
        }
        return new Playlist(id, name, ownerName, owner, songList, createDate);
    }

    public static List<Playlist> getPlaylist() {
        return playlist;
    }

    public static void setPlaylist(List<Playlist> playlist) {
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

    public String getOwnerName() {
        return ownerName;
    }

    public void deleteSong(Song song) {
        Requests.removeSongFromPlaylist(song.getId(), this.id);
    }

    public ArrayList<Song> getSongs() {
        return songs;
    }

    public String getCreateDate() {
        return createDate;
    }

    public int getLength() {
        return length;
    }

    @Override
    public String toString() {
        return this.name + " by " + this.getOwnerName();
    }
}
