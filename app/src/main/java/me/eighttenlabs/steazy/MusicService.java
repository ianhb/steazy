package me.eighttenlabs.steazy;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.soundcloud.api.ApiWrapper;
import com.spotify.sdk.android.Spotify;
import com.spotify.sdk.android.playback.Player;

import java.util.ArrayList;

/**
 * Created by Ian on 2/28/2015.
 */
public class MusicService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    private final IBinder musicBind = new MusicBinder();
    private MediaPlayer aPlayer;
    private Player sPlayer;
    private ArrayList<Song> queue;
    private int queuePosition;
    private Song currentSong;

    public void playSong() {
        Song playSong = queue.get(queuePosition);
        currentSong = playSong;
        switch (playSong.source) {
            case "spotify":

                break;
            case "SoundCloud":
                aPlayer.reset();
                try {
                    String redirectSource = new SoundCloudSearch.SoundCloudRedirect(new ApiWrapper(MainActivity.SOUNDCLOUD_CLIENT_ID,
                            MainActivity.SOUNDCLOUD_PRIVATE_ID, null, null)).execute(currentSong.tag).get() + "?client_id=" + MainActivity.SOUNDCLOUD_CLIENT_ID;
                    aPlayer.setDataSource(redirectSource);
                    Log.d("Stream", redirectSource);
                    currentSong = playSong;
                    aPlayer.prepareAsync();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        super.onDestroy();
    }

    public void setQueuePosition(int pos) {
        queuePosition = pos;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("Service", "created");
        queuePosition = 0;
        initMusicPlayers();
    }

    public void setSongs(ArrayList<Song> queue) {
        this.queue = queue;
    }

    public void initMusicPlayers() {
        aPlayer = new MediaPlayer();
        aPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        aPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        aPlayer.setOnPreparedListener(this);
        aPlayer.setOnErrorListener(this);
        aPlayer.setOnCompletionListener(this);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();
        Log.d("what", String.valueOf(what));
        Log.d("extra", String.valueOf(extra));
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (mp.getCurrentPosition() > 0) {
            mp.reset();
            playNext();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
        if (Build.VERSION.SDK_INT > 15) {
            Intent notIntent = new Intent(this, MainActivity.class);
            notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            Notification.Builder builder = new Notification.Builder(this);
            builder.setContentIntent(pendingIntent).setSmallIcon(R.drawable.ic_action_play).setTicker(currentSong.name).setOngoing(true).setContentTitle("Playing").setContentText(currentSong.name);
            Notification not = builder.build();
            startForeground(1, not);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("Service", "bound");
        return musicBind;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        aPlayer.stop();
        aPlayer.release();
        Spotify.destroyPlayer(this);
        return false;
    }

    public int getPosition() {
        switch (currentSong.source) {
            case "SoundCloud":
                return aPlayer.getCurrentPosition();
            default:
                return 0;
        }
    }

    public int getDuration() {
        switch (currentSong.source) {
            case "SoundCloud":
                return aPlayer.getDuration();
            default:
                return 0;
        }
    }

    public boolean isPlaying() {
        switch (currentSong.source) {
            case "SoundCloud":
                return aPlayer.isPlaying();
            default:
                return false;
        }
    }

    public void pause() {
        switch (currentSong.source) {
            case "SoundCloud":
                aPlayer.pause();
        }
    }

    public void seek(int pos) {
        switch (currentSong.source) {
            case "SoundCloud":
                aPlayer.seekTo(pos);
        }
    }

    public void start() {
        switch (currentSong.source) {
            case "SoundCloud":
                aPlayer.start();
                break;
        }
    }

    public void playPrevious() {
        queuePosition--;
        if (queuePosition < 0) {
            queuePosition = queue.size() - 1;
        }
        playSong();
    }

    public void playNext() {
        queuePosition++;
        if (queuePosition >= queue.size()) {
            queuePosition = 0;
        }
        playSong();
    }

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }


}
