package me.eighttenlabs.steazy.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Response;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.Spotify;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.eighttenlabs.steazy.BuildConfig;
import me.eighttenlabs.steazy.MusicService;
import me.eighttenlabs.steazy.R;
import me.eighttenlabs.steazy.networking.Requests;
import me.eighttenlabs.steazy.ui.PlaylistAdapter;
import me.eighttenlabs.steazy.ui.SongAdapter;
import me.eighttenlabs.steazy.wrappers.Playlist;
import me.eighttenlabs.steazy.wrappers.Song;

import static me.eighttenlabs.steazy.wrappers.Song.songFromJSON;


public class MainActivity extends AppCompatActivity {

    public static final String SPOTIFY_CLIENT_ID = "e0fd082de90e4cd7b60bf6047f5033f0";
    public static final String SOUNDCLOUD_CLIENT_ID = "81ca87317b91e4051f6d8797e5cce358";

    SharedPreferences preferences;

    ArrayList<Song> searchedSongs;
    private List<Song> playlistSongs;
    private MusicService musicService;
    private boolean musicBound;
    private Intent playIntent;

    private Playlist displayingPlaylist;

    private ImageButton playPauseButton;
    private TextView songName;
    private TextView songArtist;
    private ListView listView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        preferences = getSharedPreferences(getString(R.string.login_prefs), MODE_PRIVATE);

        // UI setup
        ImageButton skipButton;
        ImageButton previousButton;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        skipButton = (ImageButton) findViewById(R.id.next_button);
        previousButton = (ImageButton) findViewById(R.id.previous_button);
        playPauseButton = (ImageButton) findViewById(R.id.play_pause_button);
        songName = (TextView) findViewById(R.id.song_title);
        songArtist = (TextView) findViewById(R.id.song_artist);
        listView = (ListView) findViewById(R.id.songs);

        playPauseButton.setImageResource(R.drawable.ic_action_play);
        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (musicService.getQueue() == null || musicService.getQueue().size() == 0){
                    return;
                }
                if (musicService.isPlaying()) {
                    musicService.pause();
                } else {
                    musicService.start();
                }
            }
        });
        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                musicService.playNext();
            }
        });
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                musicService.playPrevious();
            }
        });

        // Send auth request to Spotify for playback

        buildSpotify();


        // Holds songs to display
        searchedSongs = new ArrayList<>();

        notShowingPlaylist();

        // Sets up the playback service
        setupService();
    }

    private void setupService() {
        ServiceConnection musicConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
                musicService = binder.getService();
                setMusicBound(true);
                musicService.setActivity(MainActivity.this);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                musicService = null;
                setMusicBound(false);
            }
        };
        playIntent = new Intent(getApplicationContext(), MusicService.class);
        getApplicationContext().bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
        getApplicationContext().startService(playIntent);
    }

    public void buildSpotify() {
        Log.i("Spotify Approval", "Refresh");
        Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (jsonObject.has(getString(R.string.spotify_access_token))) {
                    try {
                        Config playerConfig = new Config(getApplicationContext(),
                                jsonObject.getString(getString(R.string.spotify_access_token)), SPOTIFY_CLIENT_ID);
                        Spotify.getPlayer(playerConfig, musicService, new Player.InitializationObserver() {
                            @Override
                            public void onInitialized(Player player) {
                                musicService.setPlayers(player);
                            }

                            @Override
                            public void onError(Throwable throwable) {
                            }
                        });
                    } catch (JSONException e) {
                        Log.d("JSON Exception", e.toString());
                    }
                } else {
                    if (BuildConfig.DEBUG) {
                        throw new AssertionError("Spotify Response Doesn't Have Token");
                    }
                }
            }
        };
        Requests.getAuthToken(listener);
    }

    private void setSongList() {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                songPicked(view);
            }
        });
        SongAdapter songAdapter;
        if (playlistSongs != null) {
            songAdapter = new SongAdapter(getApplicationContext(), playlistSongs);
        } else {
            songAdapter = new SongAdapter(getApplicationContext(), searchedSongs);
        }
        listView.setAdapter(songAdapter);
        registerForContextMenu(listView);
        invalidateOptionsMenu();
    }

    private void setPlaylistList() {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Playlist playlist = Playlist.getPlaylist().get(Integer.parseInt(view.getTag().toString()));
                Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        try {
                            Playlist playlist1 = Playlist.PlaylistFromJSON(jsonObject);
                            playlistSongs = playlist1.getSongs();
                            setSongList();
                            displayingPlaylist = playlist1;
                        } catch (JSONException e) {
                            Log.d("JSON Exception", e.toString());
                        }
                    }
                };
                Requests.getPlaylist(playlist.getId(), listener);
            }
        });
        PlaylistAdapter playlistAdapter = new PlaylistAdapter(getApplicationContext(), Playlist.getPlaylist());
        listView.setAdapter(playlistAdapter);
        registerForContextMenu(listView);
        invalidateOptionsMenu();
        notShowingPlaylist();
    }

    public ImageButton getPlayPauseButton() {
        return playPauseButton;
    }

    public void songPicked(View view) {
        play(Integer.parseInt(view.getTag().toString()));
    }

    public void onSongChanged(Song song) {
        songName.setText(song.name);
        songArtist.setText(song.artists[0]);
    }

    @Override
    public void onBackPressed() {
        if (displayingPlaylist != null) {
            notShowingPlaylist();
            setPlaylistList();
        } else if (listView.getAdapter() instanceof PlaylistAdapter) {
            notShowingPlaylist();
            setSongList();
        } else {
            this.moveTaskToBack(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        new UserLoginTask().execute((Void) null);
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        playIntent = null;
        super.onDestroy();
        if (musicBound) {
            musicService.onDestroy();
            setMusicBound(false);
            musicService = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_song, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (listView.getAdapter() instanceof SongAdapter) {
            menu.clear();
            getMenuInflater().inflate(R.menu.menu_main_song, menu);
        } else if (listView.getAdapter() instanceof PlaylistAdapter) {
            menu.clear();
            getMenuInflater().inflate(R.menu.menu_main_playlist, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_search_songs:
                if (android.os.Build.VERSION.SDK_INT < 11) {
                    supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
                }
                if (getSupportActionBar() != null && getSupportActionBar().getCustomView() != null) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(
                            Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(getSupportActionBar().getCustomView().findViewById(R.id.search_box).getWindowToken(), 0);
                    getSupportActionBar().setCustomView(null);
                    getSupportActionBar().setDisplayShowCustomEnabled(false);
                } else if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayShowCustomEnabled(true);
                    getSupportActionBar().setCustomView(R.layout.action_bar_search);
                    final EditText box = ((EditText) getSupportActionBar().getCustomView().findViewById(R.id.search_box));
                    box.requestFocus();
                    box.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
                    final InputMethodManager imm = (InputMethodManager) getSystemService(
                            Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(box, 0);
                    box.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                                try {
                                    searchSongs(box.getText().toString());
                                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    return false;
                                }
                            }
                            return true;
                        }
                    });
                    box.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {}

                        @Override
                        public void afterTextChanged(Editable s) {
                            fastSearch(s.toString());
                        }
                    });

                }
                break;

            case R.id.action_show_queue:
                getUserPlaylists();
                setPlaylistList();
                break;

            case R.id.action_new_playlist:
                newPlaylistDialog();
                break;

            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        if (listView.getAdapter() instanceof PlaylistAdapter) {
            inflater.inflate(R.menu.context_menu_main_playlist, menu);
        } else if (displayingPlaylist != null) {
            inflater.inflate(R.menu.context_menu_main_playlist_songs, menu);
        } else {
            inflater.inflate(R.menu.context_menu_main_search_songs, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int position = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position;
        Song song = null;
        Playlist playlist = null;
        if (playlistSongs != null) {
            song = playlistSongs.get(position);
        } else if (listView.getAdapter() instanceof SongAdapter) {
            song = searchedSongs.get(position);
        } else if (listView.getAdapter() instanceof PlaylistAdapter) {
            playlist = Playlist.getPlaylist().get(position);
        }
        switch (item.getItemId()) {
            case R.id.play_song:
                if (song == null) {
                    throw new AssertionError("Should only be called on song");
                }
                play(position);
                return true;
            case R.id.queue_song:
                if (song == null) {
                    throw new AssertionError("Should only be called on song");
                }
                queue(song);
                return true;
            case R.id.add_song_to_playlist:
                if (song == null) {
                    throw new AssertionError("Should only be called on song");
                }
                addSongDialog(song);
                return true;
            case R.id.remove_song_from_playlist:
                if (song == null) {
                    throw new AssertionError("Should only be called on song");
                }
                displayingPlaylist.deleteSong(song);
                return true;
            case R.id.context_menu_playlist_rename:
                if (playlist == null) {
                    throw new AssertionError("Should only be called on playlist");
                }
                renamePlaylistDialog(playlist);
                return true;
            case R.id.context_menu_playlist_delete:
                if (playlist == null) {
                    throw new AssertionError("Should only be called on playlist");
                }
                deletePlaylistDialog(playlist);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    public void getUserPlaylists() {
        Requests.getPlaylists(new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray jsonArray) {
                try {
                    Playlist.setPlaylist(Playlist.PlaylistListFromJSON(jsonArray));
                    setPlaylistList();
                } catch (JSONException e) {
                    Log.d("JSON Exception", e.toString());
                }

            }
        });
    }

    public void searchSongs(String query) {
        final ProgressDialog dialog = new ProgressDialog(MainActivity.this);
        dialog.setTitle(R.string.searching_progress_title);
        dialog.show();
        Requests.search(query, Requests.SEARCH_ALL, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                ArrayList<Song> songs1 = new ArrayList<>();
                try {
                    for (int i = 0; i < response.length(); i++) {
                        songs1.add(songFromJSON(response.getJSONObject(i)));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                MainActivity.this.searchedSongs = songs1;
                setSongList();
                notShowingPlaylist();
                dialog.dismiss();
            }
        });
    }

    public void fastSearch(String query) {
        Requests.search(query, Requests.SEARCH_DATABASE, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                ArrayList<Song> songs1 = new ArrayList<>();
                try {
                    for (int i = 0; i < response.length(); i++) {
                        songs1.add(songFromJSON(response.getJSONObject(i)));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                MainActivity.this.searchedSongs = songs1;
                setSongList();
                notShowingPlaylist();
            }
        });
    }

    public void setMusicBound(boolean musicBound) {
        this.musicBound = musicBound;
    }

    private void play(int pos) {
        if (playlistSongs != null) {
            musicService.setSongs(new ArrayList<>(playlistSongs));
        } else {
            musicService.setSongs(searchedSongs);
        }
        musicService.setQueuePosition(pos);
        musicService.playSong();
    }

    private void queue(Song song) {
        if (musicService.getQueue().size() == 0) {
            musicService.getQueue().add(song);
        } else {
            musicService.getQueue().add(musicService.getQueuePosition() + 1, song);
        }
    }

    private void addSongDialog(final Song song){
        if (Playlist.getPlaylist() == null) {
            getUserPlaylists();
            return;
        }
        final Playlist[] playlists = Playlist.getPlaylist().toArray(
                new Playlist[Playlist.getPlaylist().size()]);
        String[] playlistNames = new String[playlists.length];
        for (int i = 0; i < playlists.length; i++) {
            playlistNames[i] = playlists[i].getName();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select a Playlist").setItems(playlistNames, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Requests.addSongToPlaylist(song.getId(), playlists[which].getId());
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void newPlaylistDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.create_playlist_title));
        View layout = getLayoutInflater().inflate(R.layout.text_dialog, null);
        builder.setView(layout);
        final EditText nameBox = (EditText) layout.findViewById(R.id.dialog_box);
        Button cancelButton = (Button) layout.findViewById(R.id.dialog_cancel_button);
        Button createButton = (Button) layout.findViewById(R.id.dialog_confirm_button);
        final AlertDialog dialog = builder.create();
        nameBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Requests.postPlaylist(v.getText().toString(), new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        getUserPlaylists();
                        setPlaylistList();
                    }
                });
                dialog.dismiss();
                return true;
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Requests.postPlaylist(nameBox.getText().toString(), new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        getUserPlaylists();
                        setPlaylistList();
                    }
                });
                dialog.dismiss();
            }
        });
        createButton.setText(getString(R.string.create_playlist_create));
        dialog.show();
        nameBox.requestFocus();
    }

    private void renamePlaylistDialog(final Playlist playlist) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.rename_playlist_title));
        View layout = getLayoutInflater().inflate(R.layout.text_dialog, null);
        builder.setView(layout);
        final EditText nameBox = (EditText) layout.findViewById(R.id.dialog_box);
        Button cancelButton = (Button) layout.findViewById(R.id.dialog_cancel_button);
        Button renameButton = (Button) layout.findViewById(R.id.dialog_confirm_button);
        final AlertDialog dialog = builder.create();
        nameBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Requests.renamePlaylist(playlist.getId(), nameBox.getText().toString(), new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        getUserPlaylists();
                        setPlaylistList();
                    }
                });
                dialog.dismiss();
                return true;
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        renameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Requests.renamePlaylist(playlist.getId(), nameBox.getText().toString(), new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        getUserPlaylists();
                        setPlaylistList();
                    }
                });
                dialog.dismiss();
            }
        });
        renameButton.setText(getString(R.string.rename_playlist_rename));
        dialog.show();
        nameBox.requestFocus();
    }

    public void deletePlaylistDialog(final Playlist playlist) {
        new AlertDialog.Builder(this).setTitle(getString(R.string.delete_playlist_title))
                .setMessage(getString(R.string.delete_playlist_delete)).
                setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Requests.deletePlaylist(playlist.getId(), null);
                        Playlist.removePlaylist(playlist.getId());
                        setPlaylistList();
                    }
                }).setNegativeButton(R.string.no, null).show();
    }

    private void notShowingPlaylist() {
        displayingPlaylist = null;
        playlistSongs = null;
    }

    private class UserLoginTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected void onPostExecute(Boolean success) {
            if (!success) {
                preferences.edit().remove(getString(R.string.login_username)).
                        remove(getString(R.string.login_password)).apply();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                MainActivity.this.finish();
            }
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            String mUsername = preferences.getString(getString(R.string.login_username), null);
            String mPassword = preferences.getString(getString(R.string.login_password), null);
            Map<String, String> data = new HashMap<>();
            data.put("username", mUsername);
            data.put("password", mPassword);

            String response = Requests.genericPostRequest(data, Requests.BASEURL + "login/");

            try {
                JSONObject responseJson = new JSONObject(response);
                if (responseJson.has("token")) {
                    Requests.setToken(responseJson.getString("token"));
                    Log.d("Login", "Token Received");
                    return true;
                } else {
                    return false;
                }
            } catch (JSONException e) {
                return false;
            }
        }
    }
}
