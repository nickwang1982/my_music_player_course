package com.example.course.musicplayer.playback;


import android.support.v4.media.session.MediaSessionCompat;

public interface Playback {

    void start();

    void stop(boolean notifyListeners);

    void setState(int state);

    int getState();

    boolean isConnected();

    boolean isPlaying();

    int getCurrentStreamPosition();

    void setCurrentStreamPosition(int pos);

    void updateLastKnownStreamPosition();

    void play(MediaSessionCompat.QueueItem item);

    void pause();

    void seekTo(int position);

    void setCurrentMediaId(String mediaId);

    String getCurrentMediaId();

    interface Callback {
        /**
         * On current music completed.
         */
        void onCompletion();
        /**
         * on Playback status changed
         * Implementations can use this callback to update
         * playback state on the media sessions.
         */
        void onPlaybackStatusChanged(int state);

        /**
         * @param error to be added to the PlaybackState
         */
        void onError(String error);

        /**
         * @param mediaId being currently played
         */
        void setCurrentMediaId(String mediaId);
    }
    /**
     * @param callback to be called
     */
    void setCallback(Callback callback);
}
