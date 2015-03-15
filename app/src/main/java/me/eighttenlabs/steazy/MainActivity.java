package me.eighttenlabs.steazy;

import android.content.Context;
import android.content.Intent;
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
import android.widget.TextView;

import com.soundcloud.api.ApiWrapper;
import com.spotify.sdk.android.Spotify;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.playback.Config;
import com.spotify.sdk.android.playback.Player;

import java.util.ArrayList;


public class MainActivity extends ActionBarActivity {

    public static final String SPOTIFY_CLIENT_ID = "e0fd082de90e4cd7b60bf6047f5033f0";
    public static final String SOUNDCLOUD_CLIENT_ID = "81ca87317b91e4051f6d8797e5cce358";
    public static final String SOUNDCLOUD_PRIVATE_ID = "b65b6b45d93eca0442dd9851b7c4b01d";
    private static final String SPOTIFY_CALLBACK = "spotify-sub://callback";
    private static final int REQUEST_CODE = 1337;

    ArrayList<Song> songs;
    private MusicController controller;
    private ArrayList<Song> savedSearch;
    private boolean paused = false;

    private ApiWrapper wrapper;


    private ImageButton playPauseButton;
    private TextView songName;
    private TextView songArtist;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ImageButton skipButton;
        ImageButton previousButton;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(SPOTIFY_CLIENT_ID, AuthenticationResponse.Type.TOKEN, SPOTIFY_CALLBACK);
        builder.setScopes(new String[]{"streaming"});
        AuthenticationRequest request = builder.build();
        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);

        wrapper = new ApiWrapper(SOUNDCLOUD_CLIENT_ID, SOUNDCLOUD_PRIVATE_ID, null, null);

        songs = new ArrayList<>();

        skipButton = (ImageButton) findViewById(R.id.next_button);
        previousButton = (ImageButton) findViewById(R.id.previous_button);
        playPauseButton = (ImageButton) findViewById(R.id.play_pause_button);
        songName = (TextView) findViewById(R.id.song_title);
        songArtist = (TextView) findViewById(R.id.song_artist);

        playPauseButton.setImageResource(R.drawable.ic_action_play);
        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (controller.isPlaying()) {
                    controller.pause();
                    playPauseButton.setImageResource(R.drawable.ic_action_play);
                } else {
                    controller.start();
                    playPauseButton.setImageResource(R.drawable.ic_action_pause);
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
        Song song = songs.get(Integer.parseInt(view.getTag().toString()));
        controller.play(song);
        songName.setText(song.name);
        songArtist.setText(song.artists[0]);
        playPauseButton.setImageResource(R.drawable.ic_action_pause);

    }

    public void setSongList() {
        ListView songList = (ListView) findViewById(R.id.songs);
        songList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                songPicked(view);
            }
        });
        Adapter adapter = new Adapter(getApplicationContext(), songs);
        songList.setAdapter(adapter);
        registerForContextMenu(songList);
    }

    public void searchCallback(ArrayList<Song> searchedSongs) {
        if (songs.isEmpty()) {
            songs = searchedSongs;
        } else {
            ArrayList<Song> returnList = new ArrayList<>();
            while ((!songs.isEmpty() || !searchedSongs.isEmpty()) && returnList.size() < 40) {
                if (!songs.isEmpty()) {
                    returnList.add(songs.get(0));
                    songs.remove(0);
                }
                if (!searchedSongs.isEmpty()) {
                    returnList.add(searchedSongs.get(0));
                    searchedSongs.remove(0);
                }
            }
            songs = returnList;
        }
        setSongList();
    }

    private void searchSongs(String query) {
        try {
            me.eighttenlabs.steazy.Spotify.Search spotifySearch = new me.eighttenlabs.steazy.Spotify.Search(this);
            SoundCloud.SoundCloudSearch soundCloudSearch = new SoundCloud.SoundCloudSearch(wrapper, this);
            spotifySearch.execute(query);
            soundCloudSearch.execute(query);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int responseCode, Intent intent) {
        super.onActivityResult(requestCode, responseCode, intent);
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(responseCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                Config playerConfig = new Config(this, response.getAccessToken(), SPOTIFY_CLIENT_ID);
                Player player = Spotify.getPlayer(playerConfig, controller, controller);
                controller.onInitialized(player);
            }
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
        super.onDestroy();
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
            case R.id.action_show_queue:
                if (savedSearch == null) {
                    savedSearch = songs;
                    songs = controller.getQueue();
                } else {
                    songs = savedSearch;
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
        Song object = songs.get(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position);
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
}
