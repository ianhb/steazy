package me.eighttenlabs.steazy;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Class to hold all functions related to Spotify
 * <p/>
 * Created by Ian on 1/19/2015.
 */
public class Spotify {

    public static class Search extends AsyncTask<String, String, ArrayList<Song>> {

        private static final String HEADER = "http://ws.spotify.com/search/1/";
        private MainActivity activity;

        public Search(MainActivity activity) {
            this.activity = activity;
        }

        @Override
        protected ArrayList<Song> doInBackground(String... params) {
            ArrayList<Song> objects = new ArrayList<>();
            String searchWords = params[0].replaceAll(" ", "%20");
            try {
                String searchString = HEADER + "track.json?q=" + searchWords;
                URL searchResults = new URL(searchString);

                HttpURLConnection con = (HttpURLConnection) searchResults.openConnection();

                BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));

                objects = parseSongJsonToArray(reader.readLine());

                reader.close();

                con.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return objects;
        }

        @Override
        protected void onPostExecute(ArrayList<Song> songs) {
            activity.searchCallback(songs);
        }

        private ArrayList<Song> parseSongJsonToArray(String data) {
            ArrayList<Song> songs = new ArrayList<>();
            try {
                JSONObject json = new JSONObject(data);
                JSONArray entries = json.getJSONArray("tracks");
                for (int i = 0; i < entries.length(); i++) {
                    JSONObject entry = entries.getJSONObject(i);
                    JSONArray artists = entry.getJSONArray("artists");
                    ArrayList<String> artistList = new ArrayList<>();
                    for (int j = 0; j < artists.length(); j++) {
                        artistList.add(artists.getJSONObject(j).getString("name"));
                    }
                    String[] artistArray = artistList.toArray(new String[artistList.size()]);
                    JSONObject album = entry.getJSONObject("album");
                    songs.add(new Song(entry.getString("name"), artistArray, album.getString("name"),
                            entry.getString("href"), Float.parseFloat(entry.getString("popularity")), MusicService.SPOTIFY));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            insertionSort(songs);
            return songs;
        }


        private void insertionSort(ArrayList<Song> list) {
            for (int i = 0; i < list.size(); i++) {
                int j = i;
                while (j > 0 && list.get(j - 1).popularity < list.get(j).popularity) {
                    Song a = list.get(j);
                    list.set(j, list.get(j - 1));
                    list.set(j - 1, a);
                    j--;
                }
            }
        }
    }
}
