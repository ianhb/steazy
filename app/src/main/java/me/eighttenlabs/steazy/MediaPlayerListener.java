package me.eighttenlabs.steazy;

import android.media.MediaPlayer;

/**
 * Handles MediaPlayer callbacks
 * <p/>
 * Created by Ian on 6/20/2015.
 */
public class MediaPlayerListener implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    private MusicService service;

    public MediaPlayerListener(MusicService service) {
        this.service = service;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (mp.getCurrentPosition() > 0) {
            mp.reset();
            service.playNext();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (service.isPlaying()) {
            service.stop();
        }
        mp.start();
        service.makeNotification();
    }
}
