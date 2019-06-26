/*
 * Copyright (c) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cw.tv_yt.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;

import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.CursorObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.PresenterSelector;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.core.app.ActivityOptionsCompat;
import androidx.loader.app.LoaderManager;
import androidx.core.content.ContextCompat;
import androidx.loader.content.Loader;
import androidx.loader.content.CursorLoader;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.cw.tv_yt.R;
import com.cw.tv_yt.Utils;
import com.cw.tv_yt.data_yt.FetchCategoryService_yt;
import com.cw.tv_yt.data_yt.FetchVideoService_yt;
import com.cw.tv_yt.data_yt.VideoContract_yt;
import com.cw.tv_yt.model.Video;
import com.cw.tv_yt.presenter.CardPresenter;
import com.cw.tv_yt.model.VideoCursorMapper;
import com.cw.tv_yt.presenter.GridItemPresenter;
import com.cw.tv_yt.presenter.IconHeaderItemPresenter;
import com.cw.tv_yt.recommendation.UpdateRecommendationsService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.cw.tv_yt.ui.MovieList.getYoutubeId;
import com.google.android.youtube.player.YouTubeIntents;

/*
 * Main class to show BrowseFragment with header and rows of videos
 */
public class MainFragment extends BrowseSupportFragment
        implements LoaderManager.LoaderCallbacks<Cursor> { //todo


    private static final int BACKGROUND_UPDATE_DELAY = 300;
    private final Handler mHandler = new Handler();
    private ArrayObjectAdapter mTitleRowAdapter;
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;
    private Runnable mBackgroundTask;
    private Uri mBackgroundURI;
    private BackgroundManager mBackgroundManager;
    private LoaderManager mLoaderManager;
    private static final int TITLE_LOADER = 123; // Unique ID for Category Loader.
	private static final int CATEGORY_LOADER = 246; // Unique ID for Category Loader.
	List<String> mCategoryNames = new ArrayList<>();
    private final int INIT_NUMBER = 1;

    // Maps a Loader Id to its CursorObjectAdapter.
    private Map<Integer, CursorObjectAdapter> mVideoCursorAdapters;

    // workaround for keeping 1. cursor position 2. correct rows after Refresh
    private int rowsLoadedCount;
    private FetchServiceResponseReceiver responseReceiver;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        System.out.println("MainFragment / _onAttach");

        // Create a list to contain all the CursorObjectAdapters.
        // Each adapter is used to render a specific row of videos in the MainFragment.
        mVideoCursorAdapters = new HashMap<>();

        // Start loading the titles from the database.
        mLoaderManager = LoaderManager.getInstance(this);
        mLoaderManager.initLoader(CATEGORY_LOADER, null, this);

        // receiver for fetch video service
        IntentFilter statusIntentFilter = new IntentFilter(FetchVideoService_yt.Constants.BROADCAST_ACTION);
        responseReceiver = new FetchServiceResponseReceiver();

        // Registers the FetchServiceResponseReceiver and its intent filters
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(responseReceiver, statusIntentFilter );
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // Final initialization, modifying UI elements.
        super.onActivityCreated(savedInstanceState);

        System.out.println("MainFragment / _onActivityCreated");
        // Prepare the manager that maintains the same background image between activities.
        prepareBackgroundManager();

        setupUIElements();

        setupEventListeners();
        prepareEntranceTransition();

        // Map title results from the database to ListRow objects.
        // This Adapter is used to render the MainFragment sidebar labels.

        mTitleRowAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setAdapter(mTitleRowAdapter);
        //todo temporary mark
//        updateRecommendations();

        rowsLoadedCount = 0;
    }

    @Override
    public void onResume() {
        super.onResume();
        System.out.println("MainFragment / _onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        System.out.println("MainFragment / _onPause");
    }

    @Override
    public void onDestroy() {
        mHandler.removeCallbacks(mBackgroundTask);
        mBackgroundManager = null;
        System.out.println("MainFragment / _onDestroy");
        super.onDestroy();
    }

    @Override
    public void onStop() {
        mBackgroundManager.release();
        super.onStop();
    }

    private void prepareBackgroundManager() {
        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());
        mDefaultBackground = getResources().getDrawable(R.drawable.default_background, null);
        mBackgroundTask = new UpdateBackgroundTask();
        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void setupUIElements() {

        // option: drawable
//        setBadgeDrawable(getActivity().getResources().getDrawable(R.drawable.tt, null));

        // option: title
        int focusNumber = Utils.getPref_focus_category_number(getActivity());
        String categoryName = Utils.getPref_category_name(getActivity(),focusNumber);

        //setTitle(getString(R.string.browse_title)); // Badge, when set, takes precedent over title
        if(categoryName != String.valueOf(focusNumber))
            setTitle(categoryName);

        setHeadersState(HEADERS_ENABLED);
	    setHeadersTransitionOnBackEnabled(true); //true: focus will return to header, false: will close App

        // Set fastLane (or headers) background color
        setBrandColor(ContextCompat.getColor(getActivity(), R.color.fastlane_background));

        // Set search icon color.
        setSearchAffordanceColor(ContextCompat.getColor(getActivity(), R.color.search_opaque));

        setHeaderPresenterSelector(new PresenterSelector() {
            @Override
            public Presenter getPresenter(Object o) {
                return new IconHeaderItemPresenter();
            }
        });
    }

    private void setupEventListeners() {
        setOnSearchClickedListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SearchActivity.class);
                startActivity(intent);
            }
        });

        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new ItemViewSelectedListener());
    }

    private void updateBackground(String uri) {
        int width = mMetrics.widthPixels;
        int height = mMetrics.heightPixels;

        RequestOptions options = new RequestOptions()
                .centerCrop()
                .error(mDefaultBackground);

        Glide.with(getActivity())
                .asBitmap()
                .load(uri)
                .apply(options)
                .into(new SimpleTarget<Bitmap>(width, height) {
                    @Override
                    public void onResourceReady(
                            Bitmap resource,
                            Transition<? super Bitmap> transition) {
                        mBackgroundManager.setBitmap(resource);
                    }
                });
    }

    private void startBackgroundTimer() {
        mHandler.removeCallbacks(mBackgroundTask);
        mHandler.postDelayed(mBackgroundTask, BACKGROUND_UPDATE_DELAY);
    }

    private void updateRecommendations() {
        Intent recommendationIntent = new Intent(getActivity(), UpdateRecommendationsService.class);
        getActivity().startService(recommendationIntent);
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        System.out.println("MainFragment / _onCreateLoader / id = " + id);

        if (id == CATEGORY_LOADER) {
            return new CursorLoader(
                    getContext(),
                    VideoContract_yt.CategoryEntry.CONTENT_URI, // Table to query
                    new String[]{"DISTINCT " + VideoContract_yt.CategoryEntry.COLUMN_CATEGORY_NAME},
                    // Only categories
                    null, // No selection clause
                    null, // No selection arguments
                    null  // Default sort order
            );

        }
        else if (id == TITLE_LOADER) {
            return new CursorLoader(
                    getContext(),
                    VideoContract_yt.VideoEntry.CONTENT_URI, // Table to query
                    new String[]{"DISTINCT " + VideoContract_yt.VideoEntry.COLUMN_TITLE},
                    // Only categories
                    null, // No selection clause
                    null, // No selection arguments
                    null  // Default sort order
            );

        } else {
            // Assume it is for a video.
            String title = args.getString(VideoContract_yt.VideoEntry.COLUMN_TITLE);

            // This just creates a CursorLoader that gets all videos.
            return new CursorLoader(
                    getContext(),
                    VideoContract_yt.VideoEntry.CONTENT_URI, // Table to query
                    null, // Projection to return - null means return all fields
                    VideoContract_yt.VideoEntry.COLUMN_TITLE + " = ?", // Selection clause
                    new String[]{title},  // Select based on the category id.
                    null // Default sort order
            );
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        // return when load is OK
        if( (rowsLoadedCount!=0 ) && (rowsLoadedCount >= mVideoCursorAdapters.size()) ) {
            return;
        }

        if (data != null && data.moveToFirst()) {
            final int loaderId = loader.getId();
	        System.out.println("MainFragment / _onLoadFinished / loaderId = " + loaderId);

            if (loaderId == CATEGORY_LOADER) {
                // Iterate through each category entry and add it to the ArrayAdapter.
                while (!data.isAfterLast()) {

                    int categoryIndex = data.getColumnIndex(VideoContract_yt.CategoryEntry.COLUMN_CATEGORY_NAME);
                    String category_name = data.getString(categoryIndex);
                    System.out.println("MainFragment / _onLoadFinished / category_name = " + category_name);
                    mCategoryNames.add(category_name);
                    data.moveToNext();
                }

                for(int i=1;i<= mCategoryNames.size();i++)
                    Utils.setPref_category_name(getActivity(),i,mCategoryNames.get(i-1));

                mLoaderManager.initLoader(TITLE_LOADER, null, this);

            } else if (loaderId == TITLE_LOADER) {

                // clear for not adding duplicate rows
                if(rowsLoadedCount != mVideoCursorAdapters.size())
                {
                    //System.out.println("MainFragment / _onLoadFinished /  mTitleRowAdapter.clear()");
                    // Every time we have to re-get the category loader, we must re-create the sidebar.
                    mTitleRowAdapter.clear();
                }

                // Iterate through each category entry and add it to the ArrayAdapter.
                while (!data.isAfterLast()) {

                    int titleIndex = data.getColumnIndex(VideoContract_yt.VideoEntry.COLUMN_TITLE);
                    String title = data.getString(titleIndex);

                    // Create header for this category.
                    HeaderItem header = new HeaderItem(title);
                    System.out.println("MainFragment / _onLoadFinished / title = " + title);

                    int videoLoaderId = title.hashCode(); // Create unique int from title.
                    CursorObjectAdapter existingAdapter = mVideoCursorAdapters.get(videoLoaderId);
                    if (existingAdapter == null) {

                        // Map video results from the database to Video objects.
                        CursorObjectAdapter videoCursorAdapter = new CursorObjectAdapter(new CardPresenter());
                        videoCursorAdapter.setMapper(new VideoCursorMapper());
                        mVideoCursorAdapters.put(videoLoaderId, videoCursorAdapter);

                        ListRow row = new ListRow(header, videoCursorAdapter);
                        mTitleRowAdapter.add(row);

	                    System.out.println("MainFragment / _onLoadFinished / existingAdapter is null  / will initLoader / videoLoaderId = " + videoLoaderId);

                        // Start loading the videos from the database for a particular category.
                        Bundle args = new Bundle();
                        args.putString(VideoContract_yt.VideoEntry.COLUMN_TITLE, title);
                        mLoaderManager.initLoader(videoLoaderId, args, this);
                    } else {
                        //System.out.println("MainFragment / _onLoadFinished / existingAdapter is not null ");
                        ListRow row = new ListRow(header, existingAdapter);
                        mTitleRowAdapter.add(row);
                    }

                    //System.out.println("MainFragment / _onLoadFinished / loaderId == TITLE_LOADER / rowsLoadedCount = " + rowsLoadedCount);
                    data.moveToNext();
                }

                // Create a row for this special case with more samples.
                HeaderItem gridHeader = new HeaderItem(getString(R.string.more_samples));
                GridItemPresenter gridPresenter = new GridItemPresenter(this);
                ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(gridPresenter);
                gridRowAdapter.add(getString(R.string.select_links));
	            gridRowAdapter.add(getString(R.string.grid_view));
                gridRowAdapter.add(getString(R.string.guidedstep_first_title));
                gridRowAdapter.add(getString(R.string.error_fragment));
                gridRowAdapter.add(getString(R.string.personal_settings));
                ListRow row = new ListRow(gridHeader, gridRowAdapter);
                mTitleRowAdapter.add(row);

                startEntranceTransition(); // TODO: Move startEntranceTransition to after all
                // cursors have loaded.

            } else {
                // The CursorAdapter contains a Cursor pointing to all videos.
                mVideoCursorAdapters.get(loaderId).changeCursor(data);

                // one row added
                rowsLoadedCount++;
            }
        } else {

            // data base is not created yet, call service for the first time

            // Start an Intent to fetch the categories
            if( (loader.getId() == CATEGORY_LOADER) && (mCategoryNames.size()==0) )
            {
                Utils.setPref_focus_category_number(getActivity(),INIT_NUMBER);

                System.out.println("MainFragment / onLoadFinished / start Fetch category service =================================");
                Intent serviceIntent = new Intent(getActivity(), FetchCategoryService_yt.class);
                serviceIntent.putExtra("FetchUrl", getDefaultUrl());
                getActivity().startService(serviceIntent);
            }
            // Start an Intent to fetch the videos
            else if(rowsLoadedCount == 0)
            {
                System.out.println("MainFragment / onLoadFinished / start Fetch video service =================================");

                Intent serviceIntent = new Intent(getActivity(), FetchVideoService_yt.class);
                serviceIntent.putExtra("FetchUrl", getDefaultUrl());
                serviceIntent.putExtra("Session", "install");
                getActivity().startService(serviceIntent);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        System.out.println("MainFragment / _onLoaderReset");

        int loaderId = loader.getId();
        if (loaderId != TITLE_LOADER) {
            mVideoCursorAdapters.get(loaderId).changeCursor(null);
        } else {
            mTitleRowAdapter.clear();
        }
    }

    // get default URL
    String getDefaultUrl()
    {
        // data base is not created yet, call service for the first time
        String urlName = "catalog_url_".concat(String.valueOf(INIT_NUMBER));
        int id = getActivity().getResources().getIdentifier(urlName,"string",getActivity().getPackageName());
        return getString(id);
    }


    private class UpdateBackgroundTask implements Runnable {

        @Override
        public void run() {
            if (mBackgroundURI != null) {
                updateBackground(mBackgroundURI.toString());
            }
        }
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof Video) {
             //todo case: with details
//                Video video = (Video) item;
//                Intent intent = new Intent(getActivity(), VideoDetailsActivity.class);
//                intent.putExtra(VideoDetailsActivity.VIDEO, video);
//
//                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
//                        getActivity(),
//                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
//                        VideoDetailsActivity.SHARED_ELEMENT_NAME).toBundle();
//                getActivity().startActivity(intent, bundle);

            //todo case: no details
                String idStr = getYoutubeId(((Video) item).videoUrl );
                Intent intent = YouTubeIntents.createPlayVideoIntentWithOptions(getActivity(), idStr, true/*fullscreen*/, true/*finishOnEnd*/);
                startActivity(intent);

            } else if (item instanceof String) {
	            if (((String) item).contains(getString(R.string.select_links))) {

                    Intent intent = new Intent(getActivity(), SelectLinksActivity.class);
                    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity()).toBundle();
                    startActivity(intent, bundle);

                } else if (((String) item).contains(getString(R.string.grid_view))) {
			        Intent intent = new Intent(getActivity(), VerticalGridActivity.class);
			        Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity()).toBundle();
			        startActivity(intent, bundle);
                } else if (((String) item).contains(getString(R.string.guidedstep_first_title))) {
                    Intent intent = new Intent(getActivity(), GuidedStepActivity.class);
                    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity()).toBundle();
                    startActivity(intent, bundle);
                } else if (((String) item).contains(getString(R.string.error_fragment))) {
                    BrowseErrorFragment errorFragment = new BrowseErrorFragment();
                    getFragmentManager().beginTransaction().replace(R.id.main_frame, errorFragment).addToBackStack(null).commit();
                } else if(((String) item).contains(getString(R.string.personal_settings))) {
                    Intent intent = new Intent(getActivity(), SettingsActivity.class);
                    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity()).toBundle();
                    startActivity(intent, bundle);
                } else {
                    Toast.makeText(getActivity(), ((String) item), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (item instanceof Video) {
                mBackgroundURI = Uri.parse(((Video) item).bgImageUrl);
                startBackgroundTimer();
            }

        }
    }


    // Broadcast receiver for receiving status updates from the IntentService
    class FetchServiceResponseReceiver extends BroadcastReceiver {
        // Called when the BroadcastReceiver gets an Intent it's registered to receive
        public void onReceive(Context context, Intent intent) {
            /*
             * You get notified here when your IntentService is done
             * obtaining data form the server!
             */
            System.out.println("MainFragment / _MyResponseReceiver / _onReceive");

            // for fetch category
            String statusStr1 = intent.getExtras().getString(FetchCategoryService_yt.Constants.EXTENDED_DATA_STATUS);
            System.out.println("MainFragment / _FetchServiceResponseReceiver / _onReceive / statusStr1 = " + statusStr1);

            if((statusStr1 != null) && statusStr1.equalsIgnoreCase("FetchCategoryServiceIsDone"))
            {
                if (context != null) {

                }
            }



            // for fetch video
            String statusStr = intent.getExtras().getString(FetchVideoService_yt.Constants.EXTENDED_DATA_STATUS);
            System.out.println("MainFragment / _FetchServiceResponseReceiver / _onReceive / statusStr = " + statusStr);
            if((statusStr != null) && statusStr.equalsIgnoreCase("FetchVideoServiceIsDone"))
            {
                if (context != null) {

                    LocalBroadcastManager.getInstance(context).unregisterReceiver(responseReceiver);

                    if(getActivity() != null)
                        getActivity().finish();
                    System.out.println("MainFragment / _FetchServiceResponseReceiver / will start new intent ");


                    Intent new_intent = new Intent(context, MainActivity.class);
                    new_intent.addFlags(FLAG_ACTIVITY_CLEAR_TASK);
                    new_intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(new_intent);


                }
            }

        }
    }

}