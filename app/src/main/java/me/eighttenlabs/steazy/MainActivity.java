package me.eighttenlabs.steazy;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.Spotify;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

import static me.eighttenlabs.steazy.Song.songFromJSON;


public class MainActivity extends ActionBarActivity {

    public static final String SPOTIFY_CLIENT_ID = "e0fd082de90e4cd7b60bf6047f5033f0";
    public static final String SOUNDCLOUD_CLIENT_ID = "81ca87317b91e4051f6d8797e5cce358";
    private static final String SPOTIFY_CALLBACK = "steazy://callback";
    private static final int REQUEST_CODE = 1337;

    private static final String URL = "http://steazy-dev.elasticbeanstalk.com";

    RequestQueue volleyQueue;

    ArrayList<Song> searchedSongs;
    ArrayList<Song> queue;
    private boolean paused = false;
    private MusicService musicService;
    private boolean musicBound;
    private Intent playIntent;

    private SharedPreferences preferences;

    private ImageButton playPauseButton;
    private TextView songName;
    private TextView songArtist;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // View setup
        ImageButton skipButton;
        ImageButton previousButton;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        skipButton = (ImageButton) findViewById(R.id.next_button);
        previousButton = (ImageButton) findViewById(R.id.previous_button);
        playPauseButton = (ImageButton) findViewById(R.id.play_pause_button);
        songName = (TextView) findViewById(R.id.song_title);
        songArtist = (TextView) findViewById(R.id.song_artist);

        playPauseButton.setImageResource(R.drawable.ic_action_play);
        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (musicService.isPlaying()) {
                    musicService.pause();
                    playPauseButton.setImageResource(R.drawable.ic_action_play);
                } else {
                    musicService.start();
                    playPauseButton.setImageResource(R.drawable.ic_action_pause);
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
        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(SPOTIFY_CLIENT_ID, AuthenticationResponse.Type.TOKEN, SPOTIFY_CALLBACK);
        builder.setScopes(new String[]{"streaming"});
        AuthenticationRequest request = builder.build();
        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);

        // Holds songs to display
        searchedSongs = new ArrayList<>();
        queue = new ArrayList<>();

        setupService();
        volleyQueue = Volley.newRequestQueue(getApplicationContext());
    }

    private void setupService() {
        ServiceConnection musicConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
                musicService = binder.getService();
                setMusicBound(true);
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

    private void setSongList() {
        ListView songList = (ListView) findViewById(R.id.songs);
        songList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                songPicked(view);
            }
        });
        Adapter adapter = new Adapter(getApplicationContext(), searchedSongs);
        songList.setAdapter(adapter);
        registerForContextMenu(songList);
    }

    public void songPicked(View view) {
        Song song = searchedSongs.get(Integer.parseInt(view.getTag().toString()));
        play(song);
    }

    public void searchCallback(ArrayList<Song> searchedSongs) {
        if (this.searchedSongs.isEmpty()) {
            this.searchedSongs = searchedSongs;
        } else {
            ArrayList<Song> returnList = new ArrayList<>();
            while ((!this.searchedSongs.isEmpty() || !searchedSongs.isEmpty()) && returnList.size() < 40) {
                if (!this.searchedSongs.isEmpty()) {
                    returnList.add(this.searchedSongs.get(0));
                    this.searchedSongs.remove(0);
                }
                if (!searchedSongs.isEmpty()) {
                    returnList.add(searchedSongs.get(0));
                    searchedSongs.remove(0);
                }
            }
            this.searchedSongs = returnList;
        }
        setSongList();
    }

    @Override
    protected void onActivityResult(int requestCode, int responseCode, Intent intent) {
        super.onActivityResult(requestCode, responseCode, intent);
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(responseCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                Config playerConfig = new Config(this, response.getAccessToken(), SPOTIFY_CLIENT_ID);
                Player player = Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {
                    @Override
                    public void onInitialized(Player player) {
                        musicService.setSpotify(player, MainActivity.this);
                        player.addPlayerNotificationCallback(musicService);
                    }

                    @Override
                    public void onError(Throwable throwable) {

                    }
                });
            }
        }
    }

    public void onSongChanged(Song song) {
        songName.setText(song.name);
        songArtist.setText(song.artists[0]);
        playPauseButton.setImageResource(R.drawable.ic_action_pause);
    }

    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (paused) {
            paused = false;
        }
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
        Spotify.destroyPlayer(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
                if (getSupportActionBar().getCustomView() != null) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(
                            Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(getSupportActionBar().getCustomView().findViewById(R.id.search_box).getWindowToken(), 0);
                    getSupportActionBar().setCustomView(null);
                    getSupportActionBar().setDisplayShowCustomEnabled(false);
                } else {
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

                }
                break;
            /*
            case R.id.action_show_queue:
                if (searchedSongs == null) {
                    searchedSongs = searchedSongs;
                    searchedSongs = musicService.getQueue();
                } else {
                    searchedSongs = savedSearch;
                    savedSearch = null;
                }
                setSongList();
                break;
            */
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
        inflater.inflate(R.menu.context_menu_main, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Song object = searchedSongs.get(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position);
        switch (item.getItemId()) {
            case R.id.play_song:
                play(object);
                return true;
            case R.id.queue_song:
                queue(object);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    public void searchSongs(String query) {
        query = query.replaceAll(" ", "%20");
        JsonArrayRequest arrayRequest = new JsonArrayRequest(Request.Method.GET, URL + "/songs/.json?query=" + query,
                null, new Response.Listener<JSONArray>() {
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
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("VolleyError", error.getMessage());
            }
        });
        volleyQueue.add(arrayRequest);
    }

    public void setMusicBound(boolean musicBound) {
        this.musicBound = musicBound;
    }

    private void play(Song song) {
        Log.d("Song name", song.name);
        ArrayList<Song> queue = new ArrayList<>();
        queue.add(song);
        musicService.setSongs(queue);
        musicService.setQueuePosition(0);
        musicService.playSong();
    }

    private void queue(Song song) {
        // TODO
    }
}
