package com.example.course.musicplayer.ui;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.support.v4.media.MediaBrowserCompat;
import android.os.Bundle;
import android.text.TextUtils;

import com.example.course.musicplayer.R;
import com.example.course.musicplayer.ui.MediaBrowserFragment.MediaFragmentListener;
import com.example.course.musicplayer.utils.LogHelper;

public class MusicPlayerActivity extends ExtendBaseActivity
        implements MediaFragmentListener {

    private static final String TAG = LogHelper.makeLogTag(MusicPlayerActivity.class);
    private static final String SAVED_MEDIA_ID="com.example.course.musicplayer.MEDIA_ID";
    private static final String FRAGMENT_TAG = "mp_list_container";

    public static final String EXTRA_START_FULLSCREEN =
            "com.example.course.musicplayer.EXTRA_START_FULLSCREEN";


    public static final String EXTRA_CURRENT_MEDIA_DESCRIPTION =
            "com.example.course.musicplayer.CURRENT_MEDIA_DESCRIPTION";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeToolbar();

        initializeFromParams(savedInstanceState, getIntent());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        String mediaId = getMediaId();
        if (mediaId != null) {
            outState.putString(SAVED_MEDIA_ID, mediaId);
        }
        super.onSaveInstanceState(outState);
    }

    public String getMediaId() {
        MediaBrowserFragment fragment = getBrowseFragment();
        if (fragment == null) {
            return null;
        }
        return fragment.getMediaId();
    }

    private void initializeFromParams(Bundle savedInstanceState, Intent intent) {
        String mediaId = null;
        if (savedInstanceState != null) {
            // If there is a saved media ID, use it
            mediaId = savedInstanceState.getString(SAVED_MEDIA_ID);
        }
        navigateToBrowser(mediaId);
    }

    private void navigateToBrowser(String mediaId) {
        LogHelper.d(TAG, "navigateToBrowser, mediaId=" + mediaId);
        MediaBrowserFragment fragment = getBrowseFragment();

        if (fragment == null || !TextUtils.equals(fragment.getMediaId(), mediaId)) {
            fragment = new MediaBrowserFragment();
            fragment.setMediaId(mediaId);
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.setCustomAnimations(
                    R.animator.slide_in_from_right, R.animator.slide_out_to_left,
                    R.animator.slide_in_from_left, R.animator.slide_out_to_right);
            transaction.replace(R.id.container, fragment, FRAGMENT_TAG);
            // If this is not the top level media (root), we add it to the fragment back stack,
            // so that actionbar toggle and Back will work appropriately:
            if (mediaId != null) {
                transaction.addToBackStack(null);
            }
            transaction.commit();
        }
    }

    private MediaBrowserFragment getBrowseFragment() {
        return (MediaBrowserFragment) getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
    }

    @Override
    protected void onMediaControllerConnected() {
        getBrowseFragment().onConnected();
    }

    @Override
    public void onMediaItemSelected(MediaBrowserCompat.MediaItem item) {
        LogHelper.d(TAG, "onMediaItemSelected, mediaId=" + item.getMediaId());
        if (item.isPlayable()) {
//            getSupportMediaController().getTransportControls()
//                    .playFromMediaId(item.getMediaId(), null);
        } else if (item.isBrowsable()) {
            navigateToBrowser(item.getMediaId());
        } else {
            LogHelper.w(TAG, "Ignoring MediaItem that is neither browsable nor playable: ",
                    "mediaId=", item.getMediaId());
        }
    }

    @Override
    public void setToolbarTitle(CharSequence title) {
        LogHelper.d(TAG, "Setting toolbar title to ", title);
        if (title == null) {
            title = getString(R.string.app_name);
        }
        setTitle(title);
    }
}
