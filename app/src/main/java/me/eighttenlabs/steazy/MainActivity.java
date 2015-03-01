package me.eighttenlabs.steazy;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
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
import android.widget.SeekBar;
import android.widget.TextView;

import com.soundcloud.api.ApiWrapper;
import com.spotify.sdk.android.Spotify;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.authentication.SpotifyAuthentication;
import com.spotify.sdk.android.playback.Config;

import java.util.ArrayList;


public class MainActivity extends ActionBarActivity {

    public static final String SPOTIFY_CLIENT_ID = "e0fd082de90e4cd7b60bf6047f5033f0";
    public static final String SOUNDCLOUD_CLIENT_ID = "81ca87317b91e4051f6d8797e5cce358";
    public static final String SOUNDCLOUD_PRIVATE_ID = "b65b6b45d93eca0442dd9851b7c4b01d";
    private static final String SPOTIFY_SUB_CALLBACK = "spotify-sub://callback";
    ArrayList<Song> webObjects;
    private MusicController controller;
    private ArrayList<Song> savedSearch;
    private boolean paused = false;

    private ApiWrapper wrapper;


    private ImageButton playPauseButton;
    private TextView songName;
    private TextView songArtist;
    private SeekBar seekBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ImageButton skipButton;
        ImageButton previousButton;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SpotifyAuthentication.openAuthWindow(SPOTIFY_CLIENT_ID, "token", SPOTIFY_SUB_CALLBACK, new String[]{"user-read-private", "streaming"}, null, this);

        wrapper = new ApiWrapper(SOUNDCLOUD_CLIENT_ID, SOUNDCLOUD_PRIVATE_ID, null, null);

        webObjects = new ArrayList<>();

        skipButton = (ImageButton) findViewById(R.id.next_button);
        previousButton = (ImageButton) findViewById(R.id.previous_button);
        playPauseButton = (ImageButton) findViewById(R.id.play_pause_button);
        songName = (TextView) findViewById(R.id.song_title);
        songArtist = (TextView) findViewById(R.id.song_artist);
        seekBar = (SeekBar) findViewById(R.id.seekBar);

        playPauseButton.setImageResource(R.drawable.ic_action_play);
        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (controller.isPlaying()) {
                    controller.pause();
                }
                if (!controller.isPlaying()) {
                    controller.start();
                }
            }
        });

        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                controller.next();
            }
        });

        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                controller.previous();
            }
        });

        setController();
    }

    private void setController() {
        controller = new MusicController(getApplicationContext());
        controller.setController();
        controller.setAnchorView(findViewById(R.id.songs));
    }

    public void songPicked(View view) {
        Song object = webObjects.get(Integer.parseInt(view.getTag().toString()));
        controller.play(object);
    }

    public void setSongList() {
        ListView songList = (ListView) findViewById(R.id.songs);
        songList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                songPicked(view);
            }
        });
        Adapter adapter = new Adapter(getApplicationContext(), webObjects);
        songList.setAdapter(adapter);
        registerForContextMenu(songList);
    }

    private ArrayList<Song> searchSongs(String query) {
        ArrayList<Song> spotify = new ArrayList<>();
        ArrayList<Song> soundcloud = new ArrayList<>();
        try {
            SpotifySearch spotifySearch = new SpotifySearch();
            SoundCloudSearch soundCloudSearch = new SoundCloudSearch(wrapper);
            spotifySearch.execute(query);
            soundCloudSearch.execute(query);
            spotify = spotifySearch.get();
            soundcloud = soundCloudSearch.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        ArrayList<Song> returnList = new ArrayList<>();
        while (!spotify.isEmpty() && !soundcloud.isEmpty()) {
            returnList.add(spotify.get(0));
            spotify.remove(0);
            returnList.add(soundcloud.get(0));
            soundcloud.remove(0);
        }
        return returnList;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Uri uri = intent.getData();
        if (uri != null) {
            AuthenticationResponse authenticationResponse = SpotifyAuthentication.parseOauthResponse(uri);
            Config config = new Config(this, authenticationResponse.getAccessToken(), SPOTIFY_CLIENT_ID);
            Spotify spotify = new Spotify();
            controller = new MusicController(this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
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
        controller.onDestroy();
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
                    InputMethodManager imm = (InputMethodManager) getSystemService(
                            Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(box, 0);
                    box.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                                try {
                                    webObjects = searchSongs(box.getText().toString());
                                    setSongList();
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
            case R.id.action_show_queue:
                if (savedSearch == null) {
                    savedSearch = webObjects;
                    webObjects = controller.getQueue();
                } else {
                    webObjects = savedSearch;
                    savedSearch = null;
                }
                setSongList();
                break;
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
        Song object = webObjects.get(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position);
        switch (item.getItemId()) {
            case R.id.play_song:
                controller.play(object);
                return true;
            case R.id.queue_song:
                controller.queue(object);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    public SeekBar getSeekBar() {
        return seekBar;
    }

    public ImageButton getPlayPauseButton() {
        return playPauseButton;
    }

    public TextView getSongName() {
        return songName;
    }

    public TextView getSongArtist() {
        return songArtist;
    }
}
