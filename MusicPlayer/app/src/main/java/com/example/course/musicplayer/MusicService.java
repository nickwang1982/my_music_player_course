
package com.example.course.musicplayer;

 import android.app.PendingIntent;
 import android.content.Context;
 import android.content.Intent;
 import android.os.Bundle;
 import android.os.Handler;
 import android.os.Message;
 import android.os.RemoteException;
 import android.support.annotation.NonNull;
 import android.support.v4.media.MediaBrowserCompat.MediaItem;
 import android.support.v4.media.MediaBrowserServiceCompat;
 import android.support.v4.media.MediaMetadataCompat;
 import android.support.v4.media.session.MediaButtonReceiver;
 import android.support.v4.media.session.MediaSessionCompat;
 import android.support.v4.media.session.PlaybackStateCompat;

 import com.example.course.musicplayer.model.MusicProvider;
 import com.example.course.musicplayer.playback.LocalPlayback;
 import com.example.course.musicplayer.playback.PlaybackManager;
 import com.example.course.musicplayer.playback.QueueManager;
 import com.example.course.musicplayer.utils.LogHelper;


 import java.lang.ref.WeakReference;
 import java.util.List;

 import static com.example.course.musicplayer.utils.MediaIDHelper.MEDIA_ID_ROOT;
 import static com.example.course.musicplayer.utils.MediaIDHelper.MEDIA_ID_MUSICS_BY_ALBUM;

 /**
  * This class provides a MediaBrowser through a service. It exposes the media library to a browsing
  * client, through the onGetRoot and onLoadChildren methods. It also creates a MediaSession and
  * exposes it through its MediaSession.Token, which allows the client to create a MediaController
  * that connects to and send control commands to the MediaSession remotely. This is useful for
  * user interfaces that need to interact with your media session, like Android Auto. You can
  * (should) also use the same service from your app's UI, which gives a seamless playback
  * experience to the user.
  *
  * To implement a MediaBrowserService, you need to:
  *
  * <ul>
  *
  * <li> Extend {@link android.service.media.MediaBrowserService}, implementing the media browsing
  *      related methods {@link android.service.media.MediaBrowserService#onGetRoot} and
  *      {@link android.service.media.MediaBrowserService#onLoadChildren};
  * <li> In onCreate, start a new {@link android.media.session.MediaSession} and notify its parent
  *      with the session's token {@link android.service.media.MediaBrowserService#setSessionToken};
  *
  * <li> Set a callback on the
  *      {@link android.media.session.MediaSession#setCallback(android.media.session.MediaSession.Callback)}.
  *      The callback will receive all the user's actions, like play, pause, etc;
  *
  * <li> Handle all the actual music playing using any method your app prefers (for example,
  *      {@link android.media.MediaPlayer})
  *
  *
  * <li> Declare and export the service in AndroidManifest with an intent receiver for the action
  *      android.media.browse.MediaBrowserService

  */
 public class MusicService extends MediaBrowserServiceCompat implements PlaybackManager.PlaybackServiceCallback {

     private static final String TAG = LogHelper.makeLogTag(MusicService.class);

     private MusicProvider mMusicProvider;
     private PlaybackManager mPlaybackManager;

     private MediaSessionCompat mSession;

     /*
      * (non-Javadoc)
      * @see android.app.Service#onCreate()
      */
     @Override
     public void onCreate() {
         super.onCreate();
         LogHelper.d(TAG, "onCreate");

         mMusicProvider = new MusicProvider(getApplicationContext());

         // To make the app more responsive, fetch and cache catalog information now.
         // This can help improve the response time in the method
         // {@link #onLoadChildren(String, Result<List<MediaItem>>) onLoadChildren()}.
         mMusicProvider.retrieveMediaAsync(null /* Callback */);

         QueueManager queueManager = new QueueManager(mMusicProvider, getResources(),
                 new QueueManager.MetadataUpdateListener() {
                     @Override
                     public void onMetadataChanged(MediaMetadataCompat metadata) {
                         mSession.setMetadata(metadata);
                     }

                     @Override
                     public void onMetadataRetrieveError() {
                         // TODO：show Error message
                         mPlaybackManager.updatePlaybackState(
                                 getString(R.string.error_no_metadata));
                     }

                     @Override
                     public void onCurrentQueueIndexUpdated(int queueIndex) {
                         mPlaybackManager.handlePlayRequest();
                     }

                     @Override
                     public void onQueueUpdated(String title, List<MediaSessionCompat.QueueItem> newQueue) {
                         mSession.setQueue(newQueue);
                         mSession.setQueueTitle(title);
                     }
                 });


         LocalPlayback playback = new LocalPlayback(this, mMusicProvider);
         mPlaybackManager = new PlaybackManager(this, getResources(), queueManager, mMusicProvider, playback);
         // Start a new MediaSession
         mSession = new MediaSessionCompat(this, "MusicService");
         setSessionToken(mSession.getSessionToken());
         mSession.setCallback(mPlaybackManager.getMediaSessionCallback());
         mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                 MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
         mPlaybackManager.updatePlaybackState(null);
     }

     /**
      * (non-Javadoc)
      * @see android.app.Service#onStartCommand(Intent, int, int)
      */
     @Override
     public int onStartCommand(Intent startIntent, int flags, int startId) {
          return START_STICKY;
     }



     /**
      * (non-Javadoc)
      * @see android.app.Service#onDestroy()
      */
     @Override
     public void onDestroy() {
         LogHelper.d(TAG, "onDestroy");
         mPlaybackManager.handleStopRequest(null);
         mSession.release();
     }

     @Override
     public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid,
                                  Bundle rootHints) {
         LogHelper.d(TAG, "OnGetRoot: clientPackageName=" + clientPackageName,
                 "; clientUid=" + clientUid + " ; rootHints=", rootHints);
         return new BrowserRoot(MEDIA_ID_ROOT, null);
     }

     @Override
     public void onLoadChildren(@NonNull final String parentMediaId,
                                @NonNull final Result<List<MediaItem>> result) {
         LogHelper.d(TAG, "OnLoadChildren: parentMediaId=", parentMediaId);
         result.sendResult(mMusicProvider.getChildren(parentMediaId, getResources()));
     }

     @Override
     public void onPlaybackStart() {
         if (!mSession.isActive()) {
             mSession.setActive(true);
         }
         startService(new Intent(getApplicationContext(), MusicService.class));
     }

     @Override
     public void onNotificationRequired() {
        // TODO : start notification when it's ready.
     }

     @Override
     public void onPlaybackStop() {
        stopForeground(true);
     }

     @Override
     public void onPlaybackStateUpdated(PlaybackStateCompat newState) {
        mSession.setPlaybackState(newState);
     }
}
