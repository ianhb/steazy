package me.eighttenlabs.steazy;

import android.util.Log;

import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;
import com.spotify.sdk.android.player.PlayerStateCallback;

/**
 * Handles Spotify callbacks
 * <p/>
 * Created by Ian on 6/20/2015.
 */
public class SpotifyListener implements PlayerNotificationCallback, PlayerStateCallback {

    private MusicService service;
    private PlayerState state;

    public SpotifyListener(MusicService service) {
        this.service = service;
    }

    @Override
    public void onPlaybackEvent(EventType eventType, PlayerState playerState) {
        state = playerState;
        Log.i("SPOTIFY STATE", String.valueOf(state.durationInMs - state.positionInMs));
        if (eventType == EventType.END_OF_CONTEXT) {
            service.playNext();
        }
    }

    @Override
    public void onPlaybackError(ErrorType errorType, String s) {

    }

    @Override
    public void onPlayerState(PlayerState playerState) {
        state = playerState;
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
