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
 * Created by Ian on 1/19/2015.
 */
public class SpotifySearch extends AsyncTask<String, String, ArrayList<Song>> {

    private static final String HEADER = "http://ws.spotify.com/search/1/";

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
/**
 searchString = HEADER + "artist.json?q=" + searchWords;
 searchResults = new URL(searchString);

 con = (HttpURLConnection) searchResults.openConnection();

 reader = new BufferedReader(new InputStreamReader(con.getInputStream()));

 objects = shuffle(objects, parseArtistJsonToArray(reader.readLine()));

 reader.close();
 con.disconnect();
 **/
        } catch (IOException e) {
            e.printStackTrace();
        }

        return objects;
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
                        entry.getString("href"), Float.parseFloat(entry.getString("popularity")), "Spotify"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        insertionSort(songs);
        return songs;
    }

    /**
     * private ArrayList<SpotifyWebObject> parseArtistJsonToArray(String data) {
     * ArrayList<SpotifyWebObject> artists = new ArrayList<>();
     * try {
     * JSONObject json = new JSONObject(data);
     * JSONArray entries = json.getJSONArray("artists");
     * for (int i = 0; i < entries.length(); i++) {
     * JSONObject entry = entries.getJSONObject(i);
     * String tag = entry.getString("href");
     * String name = entry.getString("name");
     * float pop = Float.parseFloat(entry.getString("popularity"));
     * artists.add(new Artist(name, tag, pop));
     * }
     * } catch (JSONException e) {
     * e.printStackTrace();
     * }
     * insertionSort(artists);
     * return artists;
     * }
     */

    private ArrayList<WebObject> shuffle(ArrayList<WebObject> songs, ArrayList<WebObject> artists) {
        ArrayList<WebObject> returnList = new ArrayList<>();
        while (!songs.isEmpty() && !artists.isEmpty()) {
            if (songs.get(0).getPopularity() > artists.get(0).getPopularity()) {
                returnList.add(songs.get(0));
                songs.remove(0);
            } else {
                returnList.add(artists.get(0));
                artists.remove(0);
            }
        }
        return returnList;
    }

    private void insertionSort(ArrayList<Song> list) {
        for (int i = 0; i < list.size(); i++) {
            int j = i;
            while (j > 0 && list.get(j - 1).getPopularity() < list.get(j).getPopularity()) {
                Song a = list.get(j);
                list.set(j, list.get(j - 1));
                list.set(j - 1, a);
                j--;
            }
        }
    }

    public static class ArtistSongs extends AsyncTask<String, String, ArrayList<WebObject>> {

        @Override
        protected ArrayList<WebObject> doInBackground(String... params) {
            ArrayList<WebObject> objects = new ArrayList<>();
            try {
                String searchString = "https://api.spotify.com/v1/artists/" + params[0].substring(15) + "/top-tracks?country=US";
                URL searchResults = new URL(searchString);

                HttpURLConnection con = (HttpURLConnection) searchResults.openConnection();

                BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));

                String line;
                String json = "";
                while ((line = reader.readLine()) != null) {
                    json += line;
                }

                objects = parseArtistTopSongs(json);

                reader.close();

                con.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return objects;
        }

        private ArrayList<WebObject> parseArtistTopSongs(String line) {
            ArrayList<WebObject> songs = new ArrayList<>();
            try {
                JSONObject json = new JSONObject(line);
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
                            entry.getString("uri"), Float.parseFloat(entry.getString("popularity")), "Spotify"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return songs;
        }
    }

}
