package me.eighttenlabs.steazy.playbacklisteners;

import android.app.AlertDialog;
import android.content.DialogInterface;

import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;
import com.spotify.sdk.android.player.PlayerStateCallback;

import me.eighttenlabs.steazy.MusicService;
import me.eighttenlabs.steazy.R;

/**
 * Handles Spotify callbacks
 * <p/>
 * Created by Ian on 6/20/2015.
 */
public class SpotifyListener implements PlayerNotificationCallback, PlayerStateCallback, ConnectionStateCallback {

    private MusicService service;
    private PlayerState state;

    public SpotifyListener(MusicService service) {
        this.service = service;
    }

    @Override
    public void onPlaybackEvent(EventType eventType, PlayerState playerState) {
        state = playerState;
        if (eventType == EventType.END_OF_CONTEXT) {
            service.playNext();
        }
        if (eventType == EventType.LOST_PERMISSION) {
            service.pause();
            new AlertDialog.Builder(service.activity).
                    setTitle(R.string.lost_spotify_title).setMessage(R.string.lost_spotify_content).
                    setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            service.activity.buildSpotify();
                        }
                    }).setNegativeButton(R.string.no, null).show();
        }
    }

    @Override
    public void onPlaybackError(ErrorType errorType, String s) {
    }

    @Override
    public void onPlayerState(PlayerState playerState) {
        state = playerState;
    }

    @Override
    public void onLoggedIn() {
    }

    @Override
    public void onLoggedOut() {
    }

    @Override
    public void onLoginFailed(Throwable throwable) {
    }

    @Override
    public void onTemporaryError() {
    }

    @Override
    public void onConnectionMessage(String s) {
    }

    public int getPosition() {
        return state.positionInMs;
    }

    public int getDuration() {
        return state.durationInMs;
    }

    public boolean isPlaying() {
        return state.playing;
    }
}
