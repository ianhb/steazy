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

import com.spotify.sdk.android.Spotify;
import com.spotify.sdk.android.playback.Player;
import com.spotify.sdk.android.playback.PlayerState;
import com.spotify.sdk.android.playback.PlayerStateCallback;

import java.util.ArrayList;

/**
 * Music playing service
 * <p/>
 * Created by Ian on 2/28/2015.
 */
public class MusicService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, PlayerStateCallback {

    public static final String SPOTIFY = "spotify";
    public static final String SOUNDCLOUD = "soundcloud";
    public static final String PAUSEPLAY = "Pause_Play";
    public static final String NEXT = "Next";
    public static final String PREVIOUS = "Previous";

    private final IBinder musicBind = new MusicBinder();
    public boolean userSkip;
    public MainActivity activity;
    private MediaPlayer aPlayer;
    private Player sPlayer;
    private ArrayList<Song> queue;
    private int queuePosition;
    private Song currentSong;
    private PlayerState state;

    public void playSong() {
        Song playSong = queue.get(queuePosition);
        currentSong = playSong;
        switch (playSong.source) {
            case SPOTIFY:
                if (isPlaying()) {
                    pause();
                }
                sPlayer.play(currentSong.tag);
                sPlayer.getPlayerState(this);
                makeNotification();
                break;
            case SOUNDCLOUD:
                playSoundCloudSong();
                break;
        }
        activity.onSongChanged(currentSong);
    }

    private void makeNotification() {
        if (Build.VERSION.SDK_INT > 15) {
            Intent notIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            Intent pauseIntent = new Intent(this, MusicService.class);
            pauseIntent.setAction(PAUSEPLAY);
            PendingIntent piPause = PendingIntent.getService(this, 0, pauseIntent, 0);

            Intent nextIntent = new Intent(this, MusicService.class);
            pauseIntent.setAction(NEXT);
            PendingIntent piNext = PendingIntent.getService(this, 0, nextIntent, 0);

            Intent prevIntent = new Intent(this, MusicService.class);
            pauseIntent.setAction(PREVIOUS);
            PendingIntent piPrev = PendingIntent.getService(this, 0, prevIntent, 0);


            Notification.Builder builder = new Notification.Builder(this);
            builder.setContentIntent(pendingIntent).setSmallIcon(R.drawable.ic_launcher).setTicker(currentSong.name)
                    .setOngoing(true).setContentTitle("Playing").setContentText(currentSong.name).setStyle(new Notification.BigTextStyle().bigText(currentSong.name))
            //.addAction(R.drawable.ic_action_previous, "", piPrev)
            //.addAction(R.drawable.ic_action_pause, "", piPause)
            //.addAction(R.drawable.ic_action_next,"", piNext)
            ;
            Notification not = builder.build();
            startForeground(1, not);
        }
    }

    private void playSoundCloudSong() {
        aPlayer.reset();
        try {
            currentSong.tag = new SoundCloud.Redirect().execute(currentSong.tag).get();
            aPlayer.setDataSource(currentSong.tag);
            aPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
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
        queuePosition = 0;
        initMusicPlayers();
        userSkip = false;
    }

    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        switch (action) {
            case PAUSEPLAY:
                if (isPlaying()) {
                    pause();
                } else {
                    start();
                }
                break;
            case NEXT:
                playNext();
                break;
            case PREVIOUS:
                playPrevious();
                break;
        }
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

    public void setSpotify(Player player) {
        sPlayer = player;
    }

    @Override
    public void onPlayerState(PlayerState playerState) {
        state = playerState;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();
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
        if (isPlaying()) {
            pause();
        }
        mp.start();
        makeNotification();
    }

    @Override
    public IBinder onBind(Intent intent) {
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
            case SPOTIFY:
                sPlayer.getPlayerState(this);
                if (state != null) {
                    return state.positionInMs;
                } else {
                    return 0;
                }
            case SOUNDCLOUD:
                return aPlayer.getCurrentPosition();
            default:
                return 0;
        }
    }

    public int getDuration() {
        switch (currentSong.source) {
            case SPOTIFY:
                sPlayer.getPlayerState(this);
                if (state != null) {
                    return state.durationInMs;
                } else {
                    return 0;
                }
            case SOUNDCLOUD:
                return aPlayer.getDuration();
            default:
                return 0;
        }
    }

    public boolean isPlaying() {
        sPlayer.getPlayerState(this);
        return (state != null && state.playing) || aPlayer.isPlaying();
    }

    public void pause() {
        sPlayer.pause();
        if (aPlayer.isPlaying()) {
            aPlayer.pause();
        }
    }

    public void seek(int pos) {
        switch (currentSong.source) {
            case SPOTIFY:
                sPlayer.seekToPosition(pos);
                break;
            case SOUNDCLOUD:
                aPlayer.seekTo(pos);
                break;
        }
    }

    public void start() {
        switch (currentSong.source) {
            case SPOTIFY:
                sPlayer.resume();
                break;
            case SOUNDCLOUD:
                aPlayer.start();
                break;
        }
    }

    public void playPrevious() {
        queuePosition--;
        if (queuePosition < 0) {
            queuePosition = queue.size() - 1;
        }
        userSkip = true;
        playSong();
    }

    public void playNext() {
        queuePosition++;
        if (queuePosition >= queue.size()) {
            queuePosition = 0;
        }
        userSkip = true;
        playSong();
    }

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }


}
