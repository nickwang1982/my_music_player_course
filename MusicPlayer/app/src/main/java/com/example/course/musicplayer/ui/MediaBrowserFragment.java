package com.example.course.musicplayer.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.course.musicplayer.R;
import com.example.course.musicplayer.utils.LogHelper;
import com.example.course.musicplayer.utils.MediaIDHelper;

import java.util.ArrayList;
import java.util.List;

import static com.example.course.musicplayer.utils.MediaIDHelper.createMediaID;

public class MediaBrowserFragment extends Fragment {

    private static final String TAG = LogHelper.makeLogTag(MediaBrowserFragment.class);

    private static final String ARG_MEDIA_ID = "media_id";

    private BrowseAdapter mBrowserAdapter;
    private String mMediaId;
    private MediaFragmentListener mMediaFragmentListener;

    private final MediaBrowserCompat.SubscriptionCallback mSubscriptionCallback =
            new MediaBrowserCompat.SubscriptionCallback() {
                @Override
                public void onChildrenLoaded(@NonNull String parentId,
                                             @NonNull List<MediaBrowserCompat.MediaItem> children) {
                    try {
                        LogHelper.d(TAG, "fragment onChildrenLoaded, parentId=" + parentId +
                                "  count=" + children.size());
                        mBrowserAdapter.clear();
                        for (MediaBrowserCompat.MediaItem item : children) {
                            mBrowserAdapter.add(item);
                        }
                        mBrowserAdapter.notifyDataSetChanged();
                    } catch (Throwable t) {
                        LogHelper.e(TAG, "Error on childrenloaded", t);
                    }
                }

                @Override
                public void onError(@NonNull String id) {
                    LogHelper.e(TAG, "browse fragment subscription onError, id=" + id);
                    Toast.makeText(getActivity(), R.string.error_loading_media, Toast.LENGTH_LONG).show();
                }
            };

    public MediaBrowserFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LogHelper.d(TAG, "fragment.onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_list, container, false);

        mBrowserAdapter = new BrowseAdapter(getActivity());

        ListView listView = (ListView) rootView.findViewById(R.id.list_view);
        listView.setAdapter(mBrowserAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MediaBrowserCompat.MediaItem item = mBrowserAdapter.getItem(position);
                mMediaFragmentListener.onMediaItemSelected(item);
            }
        });
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // If used on an activity that doesn't implement MediaFragmentListener, it
        // will throw an exception as expected:
        mMediaFragmentListener = (MediaFragmentListener) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mMediaFragmentListener = null;
    }

    @Override
    public void onStart() {
        super.onStart();

        // fetch browsing information to fill the listview:
        MediaBrowserCompat mediaBrowser = mMediaFragmentListener.getMediaBrowser();

        Log.v("Nick", "fragment.onStart, mediaId=" + mMediaId +
                "  onConnected=" + mediaBrowser.isConnected());

        if (mediaBrowser.isConnected()) {
            onConnected();
        }

    }

    @Override
    public void onStop() {
        super.onStop();
        MediaBrowserCompat mediaBrowser = mMediaFragmentListener.getMediaBrowser();
        if (mediaBrowser != null && mediaBrowser.isConnected() && mMediaId != null) {
            mediaBrowser.unsubscribe(mMediaId);
        }
    }

    public String getMediaId() {
        Bundle args = getArguments();
        if(args != null) {
            return args.getString(ARG_MEDIA_ID);
        }
        return null;
    }

    public void setMediaId(String mediaId) {
        Bundle args = new Bundle(1);
        args.putString(MediaBrowserFragment.ARG_MEDIA_ID, mediaId);
        setArguments(args);
    }

    public void onConnected() {
        if (isDetached()) {
            return;
        }
        mMediaId = getMediaId();
        if (mMediaId == null) {
            mMediaId = mMediaFragmentListener.getMediaBrowser().getRoot();
        }
        updateTitle();

        // Unsubscribing before subscribing is required if this mediaId already has a subscriber
        // on this MediaBrowser instance. Subscribing to an already subscribed mediaId will replace
        // the callback, but won't trigger the initial callback.onChildrenLoaded.
        //
        // This is temporary: A bug is being fixed that will make subscribe
        // consistently call onChildrenLoaded initially, no matter if it is replacing an existing
        // subscriber or not. Currently this only happens if the mediaID has no previous
        // subscriber or if the media content changes on the service side, so we need to
        // unsubscribe first.
        mMediaFragmentListener.getMediaBrowser().unsubscribe(mMediaId);

        mMediaFragmentListener.getMediaBrowser().subscribe(mMediaId, mSubscriptionCallback);

        // Add MediaController callback so we can redraw the list when metadata changes:
//        MediaControllerCompat controller = ((FragmentActivity) getActivity())
//                .getSupportMediaController();
//        if (controller != null) {
//            controller.registerCallback(mMediaControllerCallback);
//        }
    }

    private void updateTitle() {
        if (MediaIDHelper.MEDIA_ID_ROOT.equals(mMediaId) &&
                MediaIDHelper.MEDIA_ID_MUSICS_BY_ALBUM.equals(mMediaId)) {
            mMediaFragmentListener.setToolbarTitle(null);
            return;
        }

        MediaBrowserCompat mediaBrowser = mMediaFragmentListener.getMediaBrowser();
        mediaBrowser.getItem(mMediaId, new MediaBrowserCompat.ItemCallback() {
            @Override
            public void onItemLoaded(MediaBrowserCompat.MediaItem item) {
                mMediaFragmentListener.setToolbarTitle(
                        item.getDescription().getTitle());
            }
        });
    }

    // An adapter for showing the list of browsed MediaItem's
    private static class BrowseAdapter extends ArrayAdapter<MediaBrowserCompat.MediaItem> {
        public BrowseAdapter(Context context) {
            super(context, R.layout.media_list_item, new ArrayList<MediaBrowserCompat.MediaItem>());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            MediaBrowserCompat.MediaItem item = getItem(position);
            int itemState = MediaItemViewHolder.STATE_NONE;

//            if (item.isPlayable()) {
//                itemState = MediaItemViewHolder.STATE_PLAYABLE;
//                MediaControllerCompat controller = ((FragmentActivity)getContext())
//                        .getSupportMediaController();
//                if (controller != null && controller.getMetadata() != null) {
//                    String currentPlaying = controller.getMetadata().getDescription().getMediaId();
//                    String musicId = MediaIDHelper.extractMusicIDFromMediaID(
//                            item.getDescription().getMediaId());
//                    if (currentPlaying != null && currentPlaying.equals(musicId)) {
//                        PlaybackStateCompat pbState = controller.getPlaybackState();
//                        if (pbState == null ||
//                                pbState.getState() == PlaybackStateCompat.STATE_ERROR) {
//                            itemState = MediaItemViewHolder.STATE_NONE;
//                        } else if (pbState.getState() == PlaybackStateCompat.STATE_PLAYING) {
//                            itemState = MediaItemViewHolder.STATE_PLAYING;
//                        } else {
//                            itemState = MediaItemViewHolder.STATE_PAUSED;
//                        }
//                    }
//                }
//            }
            return MediaItemViewHolder.setupView((Activity) getContext(), convertView, parent,
                    item.getDescription(), itemState);
        }
    }
    public interface MediaFragmentListener extends MediaBrowserProvider {
        void onMediaItemSelected(MediaBrowserCompat.MediaItem item);
        void setToolbarTitle(CharSequence title);
    }
}
