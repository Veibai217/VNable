package com.Veibai.VNable.media;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;

import java.io.File;

public final class AmbiencePlayer {

    private MediaPlayer player;
    private File currentFile;
    private boolean currentLoop;
    private boolean pausedByLifecycle;

    public void play(Context context, File file, boolean loop, boolean enabled) {
        if (!enabled || file == null) {
            stop();
            return;
        }
        if (pausedByLifecycle && file.equals(currentFile) && loop == currentLoop) {
            return;
        }
        if (file.equals(currentFile) && loop == currentLoop
                && player != null && isPlaying(player)) {
            return;
        }
        stop();
        MediaPlayer fresh = new MediaPlayer();
        try {
            fresh.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            fresh.setDataSource(file.getAbsolutePath());
            fresh.setLooping(loop);
            fresh.setOnPreparedListener(mp -> {
                if (mp != player) return;
                mp.start();
            });
            fresh.setOnErrorListener((mp, what, extra) -> {
                if (mp == player) releasePlayer();
                return true;
            });
            fresh.prepareAsync();
            player = fresh;
            currentFile = file;
            currentLoop = loop;
            pausedByLifecycle = false;
        } catch (Exception e) {
            fresh.release();
            releasePlayer();
        }
    }

    public void pause() {
        if (player == null) return;
        try {
            player.pause();
            pausedByLifecycle = true;
        } catch (Exception e) {
            releasePlayer();
        }
    }

    public void resume(boolean enabled) {
        boolean wasPaused = pausedByLifecycle;
        pausedByLifecycle = false;
        if (player == null || !wasPaused) return;
        if (!enabled) {
            releasePlayer();
            return;
        }
        try {
            player.start();
        } catch (Exception e) {
            releasePlayer();
        }
    }

    public void stop() {
        releasePlayer();
    }

    private static boolean isPlaying(MediaPlayer mp) {
        try {
            return mp.isPlaying();
        } catch (Exception e) {
            return false;
        }
    }

    private void releasePlayer() {
        if (player != null) {
            try {
                player.release();
            } catch (Exception ignored) {
            }
            player = null;
        }
        currentFile = null;
        pausedByLifecycle = false;
    }

    public void release() {
        releasePlayer();
    }
}
