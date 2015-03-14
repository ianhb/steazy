package me.eighttenlabs.steazy;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.view.View;
import android.widget.MediaController;

import com.spotify.sdk.android.Spotify;
import com.spotify.sdk.android.playback.ConnectionStateCallback;
import com.spotify.sdk.android.playback.Player;
import com.spotify.sdk.android.playback.PlayerNotificationCallback;
import com.spotify.sdk.android.playback.PlayerState;

import java.util.ArrayList;

/**
 * Created by Ian on 1/19/2015.
 */
public class MusicController extends MediaController implements PlayerNotificationCallback, ConnectionStateCallback, Player.InitializationObserver, MediaController.MediaPlayerControl {

    private MusicService musicService;
    private boolean musicBound;
    private ArrayList<Song> queue;
    private Intent playIntent;

    public MusicController(Context context) {
        super(context);
        queue = new ArrayList<>();
        setEnabled(true);
        ServiceConnection musicConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
                musicService = binder.getService();
                musicService.setSongs(queue);
                setMusicService(musicService);
                setMusicBound(true);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                setMusicService(null);
                setMusicBound(false);
            }
        };
        playIntent = new Intent(context, MusicService.class);
        context.bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
        context.startService(playIntent);

    }

    public void setController() {
        setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                next();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                previous();
            }
        });
        setMediaPlayer(this);
        setEnabled(true);
    }

    @Override
    public void hide() {

    }

    public void queue(Song song) {
        queue.add(song);
        musicService.setSongs(queue);
    }

    public ArrayList<Song> getQueue() {
        return queue;
    }

    public boolean isPlaying() {
        return (musicService != null && musicBound && musicService.isPlaying());
    }

    public void play(Song song) {
        queue = new ArrayList<>();
        queue.add(song);
        musicService.setSongs(queue);
        musicService.setQueuePosition(0);
        musicService.playSong();

    }

    public void setMusicService(MusicService service) {
        musicService = service;
    }

    public void setMusicBound(boolean bound) {
        musicBound = bound;
    }

    public void onDestroy() {
        playIntent = null;
        musicService.onDestroy();
        musicService = null;
        Spotify.destroyPlayer(this);
    }

    public void next() {
        musicService.playNext();
        setController();
    }

    public void previous() {
        musicService.playPrevious();
        setController();
    }

    public void pause() {
        musicService.pause();
    }

    public void start() {
        musicService.start();
        setController();
    }

    @Override
    public int getDuration() {
        if (musicService != null && musicBound) {
            return musicService.getDuration();
        }
        return 0;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    @Override
    public int getCurrentPosition() {
        if (musicService != null && musicBound) {
            return musicService.getPosition();
        }
        return 0;
    }

    @Override
    public void seekTo(int pos) {
        musicService.seek(pos);
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
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
    public void onInitialized(Player player) {
        musicService.setSpotify(player);
    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onPlaybackError(ErrorType errorType, String s) {

    }

    @Override
    public void onPlaybackEvent(EventType eventType, PlayerState playerState) {

    }

    @Override
    public void onConnectionMessage(String s) {

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
}

