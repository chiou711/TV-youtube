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
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
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
import android.util.SparseArray;
import android.view.View;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.cw.tv_yt.R;
import com.cw.tv_yt.Utils;
import com.cw.tv_yt.data_yt.DbHelper_yt;
import com.cw.tv_yt.data_yt.FetchCategoryService_yt;
import com.cw.tv_yt.data_yt.FetchVideoService_yt;
import com.cw.tv_yt.data_yt.VideoContract_yt;
import com.cw.tv_yt.data_yt.VideoProvider_yt;
import com.cw.tv_yt.model.Video;
import com.cw.tv_yt.presenter.CardPresenter;
import com.cw.tv_yt.model.VideoCursorMapper;
import com.cw.tv_yt.presenter.GridItemPresenter;
import com.cw.tv_yt.presenter.IconHeaderItemPresenter;
import com.cw.tv_yt.recommendation.UpdateRecommendationsService;

import java.util.ArrayList;
import java.util.List;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.cw.tv_yt.Utils.getPref_focus_category_number;
import static com.cw.tv_yt.ui.MovieList.getYoutubeId;
import com.google.android.youtube.player.YouTubeIntents;

/*
 * Main class to show BrowseFragment with header and rows of videos
 */
public class MainFragment extends BrowseSupportFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int BACKGROUND_UPDATE_DELAY = 300;
    private final Handler mHandler = new Handler();
    private ArrayObjectAdapter mTitleRowAdapter;
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;
    private Runnable mBackgroundTask;
    private Uri mBackgroundURI;
    private BackgroundManager mBackgroundManager;
    private LoaderManager mLoaderManager;
    private static final int TITLE_LOADER = 123; // Unique ID for Title Loader.
	private static final int CATEGORY_LOADER = 246; // Unique ID for Category Loader.
	private List<String> mCategoryNames = new ArrayList<>();
    private final int INIT_NUMBER = 1;

    // Maps a Loader Id to its CursorObjectAdapter.
    private SparseArray<CursorObjectAdapter> mVideoCursorAdapters;

    // workaround for keeping 1. cursor position 2. correct rows after Refresh
    private int rowsLoadedCount;
    private FetchServiceResponseReceiver responseReceiver;
    LocalBroadcastManager localBroadcastMgr;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        System.out.println("MainFragment / _onAttach");

        // Create a list to contain all the CursorObjectAdapters.
        // Each adapter is used to render a specific row of videos in the MainFragment.
        mVideoCursorAdapters = new SparseArray<CursorObjectAdapter>();//new HashMap<>();

        // Start loading the titles from the database.
        mLoaderManager = LoaderManager.getInstance(this);
        mLoaderManager.initLoader(CATEGORY_LOADER, null, this);
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

        // receiver for fetch video service
        IntentFilter statusIntentFilter = new IntentFilter(FetchVideoService_yt.Constants.BROADCAST_ACTION);
        responseReceiver = new FetchServiceResponseReceiver();

        // Registers the FetchServiceResponseReceiver and its intent filters
        localBroadcastMgr = LocalBroadcastManager.getInstance(getActivity());
        localBroadcastMgr.registerReceiver(responseReceiver, statusIntentFilter );
    }

    @Override
    public void onPause() {
        super.onPause();
        System.out.println("MainFragment / _onPause");
    }

    @Override
    public void onDestroy() {
	    System.out.println("MainFragment / _onDestroy");

	    mHandler.removeCallbacks(mBackgroundTask);
        mBackgroundManager = null;

		// unregister receiver
        localBroadcastMgr.unregisterReceiver(responseReceiver);
        responseReceiver = null;
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
        int focusNumber = getPref_focus_category_number(getActivity());
        String categoryName = Utils.getPref_category_name(getActivity(),focusNumber);

        //setTitle(getString(R.string.browse_title)); // Badge, when set, takes precedent over title
        if(!categoryName.equalsIgnoreCase(String.valueOf(focusNumber)))
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
        if (id == CATEGORY_LOADER)
            System.out.println("MainFragment / _onCreateLoader / id = CATEGORY_LOADER");
        else if(id == TITLE_LOADER)
            System.out.println("MainFragment / _onCreateLoader / id = TITLE_LOADER");
        else
            System.out.println("MainFragment / _onCreateLoader / id = "+ id);

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
        System.out.println("MainFragment / _onLoadFinished");
        // return when load is OK
        if( (rowsLoadedCount!=0 ) && (rowsLoadedCount >= mVideoCursorAdapters.size()) ) {
            return;
        }

        if (data != null && data.moveToFirst()) {
            final int loaderId = loader.getId();

            if (loaderId == CATEGORY_LOADER)
	            System.out.println("MainFragment / _onLoadFinished / loaderId = CATEGORY_LOADER");
            else if(loaderId == TITLE_LOADER)
                System.out.println("MainFragment / _onLoadFinished / loaderId = TITLE_LOADER");
            else
                System.out.println("MainFragment / _onLoadFinished / loaderId = " + loaderId);

            if (loaderId == CATEGORY_LOADER) {
                mCategoryNames = new ArrayList<>();
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

	            // Create a row for category selections at top
	            HeaderItem gridHeaderCategory = new HeaderItem("Categories");
	            GridItemPresenter gridPresenterCategory = new GridItemPresenter(this,mCategoryNames);
	            ArrayObjectAdapter gridRowAdapterCategory = new ArrayObjectAdapter(gridPresenterCategory);

                for(int i=1;i<= mCategoryNames.size();i++)
                    gridRowAdapterCategory.add(mCategoryNames.get(i-1));

	            ListRow listRowCategory = new ListRow(gridHeaderCategory, gridRowAdapterCategory);
	            mTitleRowAdapter.add(listRowCategory);

	            // row id count start
	            int row_id = 0;
                listRowCategory.setId(row_id);

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
                    row_id++;
                    if (existingAdapter == null) {

                        // Map video results from the database to Video objects.
                        CursorObjectAdapter videoCursorAdapter = new CursorObjectAdapter(new CardPresenter());
                        videoCursorAdapter.setMapper(new VideoCursorMapper());
                        mVideoCursorAdapters.put(videoLoaderId, videoCursorAdapter);

                        ListRow row = new ListRow(header, videoCursorAdapter);
                        mTitleRowAdapter.add(row);
                        row.setId(row_id);
	                    System.out.println("MainFragment / _onLoadFinished / existingAdapter is null  / will initLoader / videoLoaderId = " + videoLoaderId);

                        // Start loading the videos from the database for a particular category.
                        Bundle args = new Bundle();
                        args.putString(VideoContract_yt.VideoEntry.COLUMN_TITLE, title);
                        mLoaderManager.initLoader(videoLoaderId, args, this);
                    } else {
                        //System.out.println("MainFragment / _onLoadFinished / existingAdapter is not null ");
                        ListRow row = new ListRow(header, existingAdapter);
                        row.setId(row_id);
                        mTitleRowAdapter.add(row);
                    }

                    //System.out.println("MainFragment / _onLoadFinished / loaderId == TITLE_LOADER / rowsLoadedCount = " + rowsLoadedCount);
                    data.moveToNext();
                }

                // Create a row for this special case with more samples.
                HeaderItem gridHeader = new HeaderItem(getString(R.string.more_samples));
                GridItemPresenter gridPresenter = new GridItemPresenter(this);
                ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(gridPresenter);
                gridRowAdapter.add(getString(R.string.renew));
                gridRowAdapter.add(getString(R.string.select_links));
	            gridRowAdapter.add(getString(R.string.grid_view));
                gridRowAdapter.add(getString(R.string.guidedstep_first_title));
                gridRowAdapter.add(getString(R.string.error_fragment));
                gridRowAdapter.add(getString(R.string.personal_settings));
                ListRow row = new ListRow(gridHeader, gridRowAdapter);
                row_id++;
                row.setId(row_id);
                mTitleRowAdapter.add(row);

                startEntranceTransition(); // TODO: Move startEntranceTransition to after all
                // cursors have loaded.

                // set focus category item position
                int cate_number = Utils.getPref_focus_category_number(MainFragment.this.getActivity());
                setSelectedPosition(0);
                setSelectedPosition(0, true, new ListRowPresenter.SelectItemViewHolderTask(cate_number-1));

            } else {
                // The CursorAdapter contains a Cursor pointing to all videos.
                mVideoCursorAdapters.get(loaderId).changeCursor(data);

                // one row added
                rowsLoadedCount++;
                System.out.println("MainFragment / _onLoadFinished / rowsLoadedCount = "+ rowsLoadedCount);
            }
        } else {

            // data base is not created yet, call service for the first time
                // Start an Intent to fetch the categories
            if ((loader.getId() == CATEGORY_LOADER) && (mCategoryNames.size() == 0)) {
                Utils.setPref_focus_category_number(getActivity(), INIT_NUMBER);

                System.out.println("MainFragment / onLoadFinished / start Fetch category service =================================");
                Intent serviceIntent = new Intent(getActivity(), FetchCategoryService_yt.class);
                serviceIntent.putExtra("FetchUrl", getDefaultUrl());
                getActivity().startService(serviceIntent);
            }
            // Start an Intent to fetch the videos
            else if ((loader.getId() == TITLE_LOADER) && (rowsLoadedCount == 0)) {
                System.out.println("MainFragment / onLoadFinished / start Fetch video service =================================");

                Intent serviceIntent = new Intent(getActivity(), FetchVideoService_yt.class);
                serviceIntent.putExtra("FetchUrl", getDefaultUrl());
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
    private String getDefaultUrl()
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

    // switch Data base
    private void switchDB(int clickedPos)
    {
        try {
            Utils.setPref_focus_category_number(getContext(), clickedPos);
            Utils.setPref_category_name(getContext(), clickedPos, mCategoryNames.get(clickedPos - 1));
            mLoaderManager.destroyLoader(TITLE_LOADER);

            getActivity().recreate();
        }
        catch (Exception e)
        {
            e.printStackTrace();
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

                // category selection click
                for(int i=1;i<= mCategoryNames.size();i++) {
                      if (((String) item).equalsIgnoreCase(mCategoryNames.get(i-1))) {
                          // After delay, start switch DB
                          new Handler().postDelayed(new Runnable() {
                              public void run() {
                                  switchDB(currentNavPosition+1);
                              }
                          }, 100);
                    }
                }

                if (((String) item).contains(getString(R.string.renew))) {

                    // renew DB
                    int res_id = getResourceIdentifier(String.valueOf(1));

                    startRenewFetchService(getString(res_id));

                    // remove reference keys
                    Utils.removePref_focus_category_number(getActivity());

                    // get video tables count (same as categories count)
                    DbHelper_yt mOpenHelper = new DbHelper_yt(getActivity());
                    SQLiteDatabase sqlDb = mOpenHelper.getReadableDatabase();

                    String SQL_GET_ALL_TABLES = "SELECT * FROM sqlite_master WHERE name like 'video%'";
                    Cursor cursor = sqlDb.rawQuery(SQL_GET_ALL_TABLES, null);
                    int countVideoTables = cursor.getCount();
                    cursor.close();
                    sqlDb.close();

                    // remove category name key
                    for(int i = 1; i<= countVideoTables; i++)
                        Utils.removePref_category_name(getActivity(),i);

                } else if (((String) item).contains(getString(R.string.select_links))) {

                    localBroadcastMgr.unregisterReceiver(responseReceiver);
                    responseReceiver = null;

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
                    //Toast.makeText(getActivity(), ((String) item), Toast.LENGTH_SHORT).show();
                }

            }
        }
    }

    // get resource Identifier
    private int getResourceIdentifier(String bodyStr)
    {
        String pre_str = "catalog_url_";
        int res_id = getActivity().getResources().getIdentifier(pre_str.concat(bodyStr),
                "string",
                getActivity().getPackageName());
        System.out.println("MainFragment / _getResourceIdentifier / res_id = " + res_id);
        return res_id;
    }


    // start fetch service by URL string
    private void startRenewFetchService(String url) {
        System.out.println("MainFragment / _startFetchService");
        // delete database
        try {
            System.out.println("MainFragment / _startFetchService / will delete DB");
            getActivity().deleteDatabase(DbHelper_yt.DATABASE_NAME);

            ContentResolver resolver = getActivity().getContentResolver();
            ContentProviderClient client = resolver.acquireContentProviderClient(VideoContract_yt.CONTENT_AUTHORITY);
            VideoProvider_yt provider = (VideoProvider_yt) client.getLocalContentProvider();

            provider.mContentResolver = resolver;
            provider.mOpenHelper.close();

            provider.mOpenHelper = new DbHelper_yt(getActivity());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                client.close();
            else
                client.release();

        } catch (Exception e) {
            e.printStackTrace();
        }

        // start new MainActivity
        Intent new_intent = new Intent(getActivity(), MainActivity.class);
        new_intent.addFlags(FLAG_ACTIVITY_CLEAR_TASK);
        new_intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        getActivity().startActivity(new_intent);
    }


    private static int currentNavPosition;

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof Video) {
                mBackgroundURI = Uri.parse(((Video) item).bgImageUrl);
                startBackgroundTimer();
            }
            else if (item instanceof String) {
                // category selection header
//                int cate_number = Utils.getPref_focus_category_number(MainFragment.this.getActivity());
//                System.out.println("---------- focus cate_number = " + cate_number);
//                String cate_name = Utils.getPref_category_name(MainFragment.this.getActivity(), cate_number);
//                System.out.println("---------- focus cate_name = " + cate_name);
//                System.out.println("---------- itemViewHolder.view.getId() = " + itemViewHolder.view.getId());

                for(int i=0;i<mCategoryNames.size();i++)
                {
                    if(item.toString().equalsIgnoreCase(mCategoryNames.get(i)))
                    {
                        currentNavPosition = i;
                        System.out.println("---------- current navigation position = " + currentNavPosition);
                    }
                }
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
            String statusStr = intent.getExtras().getString(FetchCategoryService_yt.Constants.EXTENDED_DATA_STATUS);
            System.out.println("MainFragment / _FetchServiceResponseReceiver / _onReceive / statusStr = " + statusStr);

            // for fetch category
            if((statusStr != null) && statusStr.equalsIgnoreCase("FetchCategoryServiceIsDone"))
            {
                if (context != null) {
                }
            }

            // for fetch video
            if((statusStr != null) && statusStr.equalsIgnoreCase("FetchVideoServiceIsDone"))
            {
                if (context != null) {
                    // unregister receiver
                    LocalBroadcastManager.getInstance(context).unregisterReceiver(responseReceiver);

                    System.out.println("MainFragment / _FetchServiceResponseReceiver / will start new main activity");
                    Intent new_intent = new Intent(context, MainActivity.class);
                    new_intent.addFlags(FLAG_ACTIVITY_CLEAR_TASK);
                    new_intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(new_intent);
                }
            }
        }
    }

}