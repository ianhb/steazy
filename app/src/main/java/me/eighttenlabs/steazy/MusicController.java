package me.eighttenlabs.steazy;

import android.widget.MediaController;
import android.widget.SeekBar;

import com.soundcloud.api.ApiWrapper;
import com.spotify.sdk.android.Spotify;
import com.spotify.sdk.android.playback.ConnectionStateCallback;
import com.spotify.sdk.android.playback.Player;
import com.spotify.sdk.android.playback.PlayerNotificationCallback;
import com.spotify.sdk.android.playback.PlayerState;
import com.spotify.sdk.android.playback.PlayerStateCallback;

import java.util.ArrayList;

/**
 * Created by Ian on 1/19/2015.
 */
public class MusicController extends MediaController implements PlayerNotificationCallback, ConnectionStateCallback, Player.InitializationObserver, PlayerStateCallback, MediaController.MediaPlayerControl {

    ApiWrapper wrapper;
    private Player mPlayer;
    private PlayerState mState;
    private ArrayList<Song> queue;
    private MainActivity activity;
    private int queuePosition;
    private boolean manualSongChange;

    public MusicController(MainActivity activity) {
        super(activity.getApplicationContext(), false);
        setEnabled(true);
        queue = new ArrayList<>();
        this.activity = activity;
        manualSongChange = false;
        activity.getSeekBar().setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mPlayer.seekToPosition(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    @Override
    public void hide() {

    }

    public void queue(Song song) {
        queue.add(song);
    }

    public ArrayList<Song> getQueue() {
        ArrayList<Song> displayList = new ArrayList<>();
        for (Song s : queue) {
            displayList.add(s);
        }
        return displayList;
    }

    public void setPlayers(Player player, ApiWrapper wrapper) {
        mPlayer = player;
        mPlayer.getPlayerState(this);
        this.wrapper = wrapper;
    }

    public void play(Song song) {
        queue = new ArrayList<>();
        queuePosition = 0;
        queue.add(song);
        manualSongChange = true;
        playNextSong();
    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onInitialized() {
        mPlayer.addConnectionStateCallback(this);
        mPlayer.addPlayerNotificationCallback(this);
    }

    @Override
    public void onPlaybackError(ErrorType errorType, String s) {

    }

    @Override
    public void onPlaybackEvent(EventType eventType, PlayerState playerState) {

        switch (eventType) {
            case TRACK_END:
                if (!manualSongChange) {
                    queuePosition++;
                    playNextSong();
                }
                manualSongChange = false;
                break;
        }


    }

    private void playNextSong() {
        mPlayer.play(queue.get(queuePosition).tag);
        mPlayer.getPlayerState(this);
    }

    @Override
    public void onConnectionMessage(String s) {

    }

    @Override
    public void onNewCredentials(String s) {

    }

    @Override
    public void onTemporaryError() {

    }

    @Override
    public void onLoginFailed(Throwable throwable) {

    }

    @Override
    public void onLoggedIn() {

    }

    @Override
    public void onLoggedOut() {

    }

    public void onDestroy() {
        Spotify.destroyPlayer(this);
    }

    public boolean isPlaying() {
        return mState != null && mState.playing;
    }

    public void next() {
        if (queuePosition < queue.size() - 1) {
            mPlayer.pause();
            queuePosition++;
            manualSongChange = true;
            playNextSong();
        }
    }

    public void previous() {
        mPlayer.pause();
        queuePosition--;
        manualSongChange = true;
        playNextSong();
    }

    public void pause() {
        mPlayer.pause();
        mPlayer.getPlayerState(this);
    }

    public void start() {
        mPlayer.resume();
        mPlayer.getPlayerState(this);
    }

    @Override
    public void onPlayerState(PlayerState state) {
        mState = state;
        activity.getSeekBar().setMax(state.durationInMs);
        if (state.playing) {
            activity.getPlayPauseButton().setImageResource(R.drawable.ic_action_pause);
            activity.getSongName().setText(queue.get(queuePosition).name);
            activity.getSongArtist().setText(queue.get(queuePosition).artists[0]);
        } else {
            activity.getPlayPauseButton().setImageResource(R.drawable.ic_action_play);
            activity.getSongName().setText("");
            activity.getSongArtist().setText("");
        }

    }

    @Override
    public boolean canSeekBackward() {
        return false;
    }

    @Override
    public boolean canSeekForward() {
        return false;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public int getCurrentPosition() {
        return 0;
    }

    @Override
    public int getDuration() {
        return 0;
    }

    @Override
    public void seekTo(int pos) {

    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }
}

