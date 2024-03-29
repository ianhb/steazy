package me.eighttenlabs.steazy;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.Spotify;

import java.util.ArrayList;

import me.eighttenlabs.steazy.activities.MainActivity;
import me.eighttenlabs.steazy.networking.Requests;
import me.eighttenlabs.steazy.playbacklisteners.MediaPlayerListener;
import me.eighttenlabs.steazy.playbacklisteners.SpotifyListener;
import me.eighttenlabs.steazy.wrappers.Song;

/**
 * Music playing service
 * <p/>
 * Created by Ian on 2/28/2015.
 */
public class MusicService extends Service {

    public static final String SPOTIFY = "Spotify";
    public static final String SOUNDCLOUD = "Soundcloud";

    private final IBinder musicBind = new MusicBinder();

    // Used to allow service to update UI
    public MainActivity activity;
    private ArrayList<Song> queue;
    private MediaPlayer aPlayer;
    private Player sPlayer;
    private int queuePosition;

    private MediaPlayerListener mediaPlayerListener;
    private SpotifyListener spotifyListener;

    private Song currentSong;
    private AudioManager am;
    private AudioManager.OnAudioFocusChangeListener af;
    private NoisyAudioStreamReceiver noisyAudioStreamReceiver;
    // Filters the intents
    private IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    @Override
    public void onCreate() {
        super.onCreate();
        queuePosition = 0;
        queue = new ArrayList<>();
        mediaPlayerListener = new MediaPlayerListener(this);
        spotifyListener = new SpotifyListener(this);
        aPlayer = new MediaPlayer();
        aPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        aPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        aPlayer.setOnPreparedListener(mediaPlayerListener);
        aPlayer.setOnErrorListener(mediaPlayerListener);
        aPlayer.setOnCompletionListener(mediaPlayerListener);

        noisyAudioStreamReceiver = new NoisyAudioStreamReceiver();
    }

    /**
     * Used to play song in queue at queueposition
     */
    public void playSong() {
        if (!isPlaying()){
            if (!pauseSystemPlayback()) {
                return;
            }
        }
        currentSong = queue.get(queuePosition);
        switch (currentSong.source) {
            case SPOTIFY:
                playSpotifySong();
                break;
            case SOUNDCLOUD:
                playSoundCloudSong();
                break;
        }
        activity.onSongChanged(currentSong);
        makeNotification();
        activity.getPlayPauseButton().setImageResource(R.drawable.ic_action_pause);
        Requests.play(currentSong);
    }

    /**
     * Pauses system sounds when playback starts
     */
    public boolean pauseSystemPlayback() {
        af = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    pause();
                } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                    am.abandonAudioFocus(af);
                    pause();
                }
            }
        };

        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int request = am.requestAudioFocus(af, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        registerReceiver(noisyAudioStreamReceiver, intentFilter);
        return (request == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
    }

    /**
     * Makes a notification for playback
     * TODO improve
     */
    public void makeNotification() {
        if (Build.VERSION.SDK_INT > 15) {
            Intent notIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            Notification.Builder builder = new Notification.Builder(this);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                builder.setContentIntent(pendingIntent).setSmallIcon(R.drawable.ic_launcher).setTicker(currentSong.name)
                        .setOngoing(true).setContentTitle("Playing").setContentText(currentSong.name);
            } else {
                // TODO: Add interactive notifications
                builder.setContentIntent(pendingIntent).setSmallIcon(R.drawable.lp_notification).setTicker(currentSong.name)
                        .setOngoing(true).setContentTitle("Playing").setContentText(currentSong.name).setStyle(new Notification.BigTextStyle().bigText(currentSong.name));
            }
            Notification notification = builder.build();
            startForeground(1, notification);
        }
    }

    /**
     * Used to play Soundcloud songs where url may redirct
     * Precondition: currentSong.source = SOUNDCLOUD
     */
    private void playSoundCloudSong() {
        new Requests.SoundcloudRedirect(aPlayer).execute(currentSong);
    }

    /**
     * Plays a Spotify song
     * Precondition: currentSong.source = SPOTIFY
     */
    private void playSpotifySong() {
        if (isPlaying()) {
            stop();
        }
        sPlayer.play("spotify:track:" + currentSong.tag);
        sPlayer.getPlayerState(spotifyListener);
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        if (am != null) {
            am.abandonAudioFocus(af);
        }
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }

    /**
     * Sets the songs in the queue
     *
     * @param queue songs to enqueue
     */
    public void setSongs(ArrayList<Song> queue) {
        this.queue = queue;
    }

    /**
     * Passes the Spotify player once it is initialized
     *
     * @param player   Spotify Player to used
     */
    public void setPlayers(Player player) {
        sPlayer = player;
        sPlayer.addConnectionStateCallback(spotifyListener);
        sPlayer.addPlayerNotificationCallback(spotifyListener);
        sPlayer.getPlayerState(spotifyListener);
    }

    public void setActivity(MainActivity activity) {
        this.activity = activity;
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

    /**
     * Returns position in current song
     * TODO add progres bar
     *
     * @return msec into song currently playing
     */
    public int getPosition() {
        switch (currentSong.source) {
            case SPOTIFY:
                return spotifyListener.getPosition();
            case SOUNDCLOUD:
                return aPlayer.getCurrentPosition();
            default:
                return 0;
        }
    }

    /**
     * Returns length of current song
     * TODO add progress bar
     * @return current song length in msec
     */
    public int getDuration() {
        switch (currentSong.source) {
            case SPOTIFY:
                return spotifyListener.getDuration();
            case SOUNDCLOUD:
                return aPlayer.getDuration();
            default:
                return 0;
        }
    }

    /***
     * Returns whether or not any of the players are currently playing
     * @return if any player is playing
     */
    public boolean isPlaying() {
        sPlayer.getPlayerState(spotifyListener);
        return spotifyListener.isPlaying() || aPlayer.isPlaying();
    }

    /***
     * Pauses playback
     */
    public void stop() {
        sPlayer.pause();
        if (aPlayer.isPlaying()) {
            aPlayer.pause();
        }
        stopForeground(true);
        activity.getPlayPauseButton().setImageResource(R.drawable.ic_action_play);
    }

    /***
     * Pauses playback and releases audio focus
     */
    public void pause() {
        stop();
        am.abandonAudioFocus(af);
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(1);
        unregisterReceiver(noisyAudioStreamReceiver);
    }

    /**
     * Skip to point in current song
     * TODO progress bar
     *
     * @param pos milliseconds to skip to
     */
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

    /**
     * Start playback of current song
     */
    public void start() {
        if (currentSong == null && queue != null && queue.size() > 0) {
            currentSong = queue.get(0);
        } else if (currentSong == null && (queue == null || queue.size() == 0)) {
            return;
        }
        switch (currentSong.source) {
            case SPOTIFY:
                sPlayer.resume();
                break;
            case SOUNDCLOUD:
                aPlayer.start();
                break;
        }
        pauseSystemPlayback();
        makeNotification();
        activity.getPlayPauseButton().setImageResource(R.drawable.ic_action_pause);
    }

    /***
     * Skips to the previous song in the queue and plays it
     */
    public void playPrevious() {
        queuePosition--;
        if (queuePosition < 0) {
            queuePosition = queue.size() - 1;
        }
        playSong();
    }

    /***
     * Skips to the next song in the queue and plays it
     */
    public void playNext() {
        queuePosition++;
        if (queuePosition >= queue.size()) {
            queuePosition = 0;
        }
        playSong();
    }

    /***
     * Returns the current songs in the queue
     * @return list of songs in queue
     */
    public ArrayList<Song> getQueue() {
        return queue;
    }

    /***
     * Returns the current queueposition
     * @return queue position
     */
    public int getQueuePosition() {
        return queuePosition;
    }

    /***
     * Sets the position in the queue to an integer
     * @param pos position in queue (Precondition: pos < queue.length())
     */
    public void setQueuePosition(int pos) {
        queuePosition = pos;
    }

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    /**
     * Used to pause playback if audio will play to speakers after headphone unplugged/disconnected
     */
    private class NoisyAudioStreamReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                pause();
            }
        }
    }

}
