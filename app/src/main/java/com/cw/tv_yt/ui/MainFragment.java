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

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;

import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.CursorObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ImageCardView;
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

import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.cw.tv_yt.R;
import com.cw.tv_yt.data_yt.FetchVideoService_yt;
import com.cw.tv_yt.data_yt.VideoContract_yt;
import com.cw.tv_yt.model.Video;
import com.cw.tv_yt.presenter.CardPresenter;
import com.cw.tv_yt.model.VideoCursorMapper;
import com.cw.tv_yt.presenter.GridItemPresenter;
import com.cw.tv_yt.presenter.IconHeaderItemPresenter;
import com.cw.tv_yt.recommendation.UpdateRecommendationsService;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import static com.cw.tv_yt.ui.MovieList.getYoutubeId;
import com.google.android.youtube.player.YouTubeIntents;

/*
 * Main class to show BrowseFragment with header and rows of videos
 */
public class MainFragment extends BrowseSupportFragment
        implements LoaderManager.LoaderCallbacks<Cursor> { //todo


    private static final int BACKGROUND_UPDATE_DELAY = 300;
    private final Handler mHandler = new Handler();
    private ArrayObjectAdapter mCategoryRowAdapter;
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;
    private Runnable mBackgroundTask;
    private Uri mBackgroundURI;
    private BackgroundManager mBackgroundManager;
    private LoaderManager mLoaderManager;
    private static final int CATEGORY_LOADER = 123; // Unique ID for Category Loader.

    // Maps a Loader Id to its CursorObjectAdapter.
    private Map<Integer, CursorObjectAdapter> mVideoCursorAdapters;

    int rowsCount;

    // workaround for keeping cursor position
    private boolean isDataLoaded;
    private int rowsLoadedCount;


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        System.out.println("MainFragment / _onAttach");

        // Create a list to contain all the CursorObjectAdapters.
        // Each adapter is used to render a specific row of videos in the MainFragment.
        mVideoCursorAdapters = new HashMap<>();

        // Start loading the categories from the database.
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

        ///

        //todo
        // get total rows from server
//        GetRowsTask getRowsTask = new GetRowsTask();
//        getRowsTask.execute();
//        while (!getRowsTask.isGetReady)
//            SystemClock.sleep(1000);
//        rowsCount = getRowsTask.count;
//
//        loadRows();
        ///

        setupEventListeners();
        prepareEntranceTransition();

        // Map category results from the database to ListRow objects.
        // This Adapter is used to render the MainFragment sidebar labels.

        mCategoryRowAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setAdapter(mCategoryRowAdapter);
        //todo temporary mark
//        updateRecommendations();

        // workaround for keeping cursor position
        isDataLoaded = false;
        rowsLoadedCount = 0;
    }

    @Override
    public void onDestroy() {
        mHandler.removeCallbacks(mBackgroundTask);
        mBackgroundManager = null;
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
        setBadgeDrawable(
                getActivity().getResources().getDrawable(R.drawable.tt, null));
//        setTitle(getString(R.string.browse_title)); // Badge, when set, takes precedent over title
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
        System.out.println("MainFragment / _onCreateLoader");
        if (id == CATEGORY_LOADER) {
            System.out.println("MainFragment / _onCreateLoader / id == CATEGORY_LOADER / VideoContract.VideoEntry.CONTENT_URI =" +
                    VideoContract_yt.VideoEntry.CONTENT_URI);
            System.out.println("MainFragment / _onCreateLoader / DISTINCT VideoContract.VideoEntry.COLUMN_CATEGORY = " + VideoContract_yt.VideoEntry.COLUMN_CATEGORY);
            return new CursorLoader(
                    getContext(),
                    VideoContract_yt.VideoEntry.CONTENT_URI, // Table to query
                    new String[]{"DISTINCT " + VideoContract_yt.VideoEntry.COLUMN_CATEGORY},
                    // Only categories
                    null, // No selection clause
                    null, // No selection arguments
                    null  // Default sort order
            );
        } else {
            System.out.println("MainFragment / _onCreateLoader / id != CATEGORY_LOADER / VideoContract.VideoEntry.CONTENT_URI = " +
                    VideoContract_yt.VideoEntry.CONTENT_URI );
            System.out.println("MainFragment / _onCreateLoader / VideoContract.VideoEntry.COLUMN_CATEGORY = " + VideoContract_yt.VideoEntry.COLUMN_CATEGORY);
            // Assume it is for a video.
            String category = args.getString(VideoContract_yt.VideoEntry.COLUMN_CATEGORY);

            // This just creates a CursorLoader that gets all videos.
            return new CursorLoader(
                    getContext(),
                    VideoContract_yt.VideoEntry.CONTENT_URI, // Table to query
                    null, // Projection to return - null means return all fields
                    VideoContract_yt.VideoEntry.COLUMN_CATEGORY + " = ?", // Selection clause
                    new String[]{category},  // Select based on the category id.
                    null // Default sort order
            );
        }
    }


    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        // workaround for keeping cursor position
        if(isDataLoaded)
            return;

        if (data != null && data.moveToFirst()) {
            final int loaderId = loader.getId();

            if (loaderId == CATEGORY_LOADER) {
                System.out.println("MainFragment / _onLoadFinished / loaderId == CATEGORY_LOADER");

                // Every time we have to re-get the category loader, we must re-create the sidebar.
                mCategoryRowAdapter.clear();

                // Iterate through each category entry and add it to the ArrayAdapter.
                while (!data.isAfterLast()) {

                    int categoryIndex =
                            data.getColumnIndex(VideoContract_yt.VideoEntry.COLUMN_CATEGORY);
                    String category = data.getString(categoryIndex);

                    // Create header for this category.
                    HeaderItem header = new HeaderItem(category);

                    int videoLoaderId = category.hashCode(); // Create unique int from category.
                    CursorObjectAdapter existingAdapter = mVideoCursorAdapters.get(videoLoaderId);
                    if (existingAdapter == null) {

                        // Map video results from the database to Video objects.
                        CursorObjectAdapter videoCursorAdapter =
                                new CursorObjectAdapter(new CardPresenter());
                        videoCursorAdapter.setMapper(new VideoCursorMapper());
                        mVideoCursorAdapters.put(videoLoaderId, videoCursorAdapter);

                        ListRow row = new ListRow(header, videoCursorAdapter);
                        mCategoryRowAdapter.add(row);

                        // Start loading the videos from the database for a particular category.
                        Bundle args = new Bundle();
                        args.putString(VideoContract_yt.VideoEntry.COLUMN_CATEGORY, category);
                        mLoaderManager.initLoader(videoLoaderId, args, this);
                        System.out.println("MainFragment / _onLoadFinished / loaderId == CATEGORY_LOADER / 1 ");
                    } else {
                        ListRow row = new ListRow(header, existingAdapter);
                        mCategoryRowAdapter.add(row);
                        System.out.println("MainFragment / _onLoadFinished / loaderId == CATEGORY_LOADER / 2 ");
                    }

                    data.moveToNext();
                }
                System.out.println("MainFragment / _onLoadFinished / loaderId == CATEGORY_LOADER / rowsLoadedCount = " + rowsLoadedCount);
                // Create a row for this special case with more samples.
                HeaderItem gridHeader = new HeaderItem(getString(R.string.more_samples));
                GridItemPresenter gridPresenter = new GridItemPresenter(this);
                ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(gridPresenter);
                gridRowAdapter.add(getString(R.string.grid_view));
                gridRowAdapter.add(getString(R.string.guidedstep_first_title));
                gridRowAdapter.add(getString(R.string.error_fragment));
                gridRowAdapter.add(getString(R.string.personal_settings));
                ListRow row = new ListRow(gridHeader, gridRowAdapter);
                mCategoryRowAdapter.add(row);

                startEntranceTransition(); // TODO: Move startEntranceTransition to after all
                // cursors have loaded.

            } else {
                System.out.println("MainFragment / _onLoadFinished / loaderId != CATEGORY_LOADER");
                System.out.println("MainFragment / _onLoadFinished / loaderId = " + loaderId);
                System.out.println("MainFragment / _onLoadFinished / mVideoCursorAdapters.size() = " + mVideoCursorAdapters.size());
                // The CursorAdapter contains a Cursor pointing to all videos.
                mVideoCursorAdapters.get(loaderId).changeCursor(data);

                // workaround for keeping cursor position
                rowsLoadedCount++;
                if(mVideoCursorAdapters.size() == rowsLoadedCount)
                    isDataLoaded = true;
            }
        } else {
            System.out.println("MainFragment / _onLoadFinished / data == null or !data.moveToFirst()");
            // Start an Intent to fetch the videos.
            Intent serviceIntent = new Intent(getActivity(), FetchVideoService_yt.class);
            getActivity().startService(serviceIntent);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        System.out.println("MainFragment / _onLoaderReset");

        int loaderId = loader.getId();
        if (loaderId != CATEGORY_LOADER) {
            mVideoCursorAdapters.get(loaderId).changeCursor(null);
        } else {
            mCategoryRowAdapter.clear();
        }
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
             // case: with details
//                Video video = (Video) item;
//                Intent intent = new Intent(getActivity(), VideoDetailsActivity.class);
//                intent.putExtra(VideoDetailsActivity.VIDEO, video);
//
//                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
//                        getActivity(),
//                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
//                        VideoDetailsActivity.SHARED_ELEMENT_NAME).toBundle();
//                getActivity().startActivity(intent, bundle);

            //todo
            // case: no details
                String idStr = getYoutubeId(((Video) item).videoUrl );
                Intent intent = YouTubeIntents.createPlayVideoIntentWithOptions(getActivity(), idStr, true/*fullscreen*/, true/*finishOnEnd*/);
                startActivity(intent);

            } else if (item instanceof String) {
                if (((String) item).contains(getString(R.string.grid_view))) {
                    Intent intent = new Intent(getActivity(), VerticalGridActivity.class);
                    Bundle bundle =
                            ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity())
                                    .toBundle();
                    startActivity(intent, bundle);
                } else if (((String) item).contains(getString(R.string.guidedstep_first_title))) {
                    Intent intent = new Intent(getActivity(), GuidedStepActivity.class);
                    Bundle bundle =
                            ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity())
                                    .toBundle();
                    startActivity(intent, bundle);
                } else if (((String) item).contains(getString(R.string.error_fragment))) {
                    BrowseErrorFragment errorFragment = new BrowseErrorFragment();
                    getFragmentManager().beginTransaction().replace(R.id.main_frame, errorFragment)
                            .addToBackStack(null).commit();
                } else if(((String) item).contains(getString(R.string.personal_settings))) {
                    Intent intent = new Intent(getActivity(), SettingsActivity.class);
                    Bundle bundle =
                            ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity())
                                    .toBundle();
                    startActivity(intent, bundle);
                } else {
                    Toast.makeText(getActivity(), ((String) item), Toast.LENGTH_SHORT)
                            .show();
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


///
//private class GetRowsTask extends AsyncTask<Void, Void, Void> {
//    BrowseErrorActivity.SpinnerFragment mSpinnerFragment;
//    boolean isGetReady;
//    int count;
//
//    @Override
//    protected void onPreExecute() {
//        mSpinnerFragment = new BrowseErrorActivity.SpinnerFragment();
////        getFragmentManager().beginTransaction().add(R.id.main_browse_fragment, mSpinnerFragment).commit();
//        getFragmentManager().beginTransaction().add(R.id.main_frame,mSpinnerFragment).commit();
//    }
//
//    @Override
//    protected Void doInBackground(Void... params) {
//        // Do some background process here.
//        // It just waits 5 sec in this Tutorial
//        //SystemClock.sleep(5000);
//
//        String strResult = "";
//
//        // HTTPS POST
//        String project = "LiteNote";
//        String urlStr =  "https://" + project + ".ddns.net:8443/"+ project +"Web/client/viewTotalPages.jsp";
//
//        try {
//            URL url = new URL(urlStr);
//            MovieList.trustEveryone();
//            HttpsURLConnection urlConnection = ((HttpsURLConnection)url.openConnection());
//
//            // set Timeout and method
//            urlConnection.setReadTimeout(7000);
//            urlConnection.setConnectTimeout(7000);
//            urlConnection.setRequestMethod("POST");
//            urlConnection.setDoInput(true);
//            urlConnection.setDoOutput( true );
//            urlConnection.setInstanceFollowRedirects( false );
//            urlConnection.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded");
//            urlConnection.setRequestProperty( "charset", "utf-8");
//            urlConnection.setUseCaches( false );
//            try( DataOutputStream wr = new DataOutputStream( urlConnection.getOutputStream())) {
//                wr.close();
//                wr.flush();
//            }
//
//            // Add any data you wish to post here
//            urlConnection.connect();
//            InputStream in = urlConnection.getInputStream();
//
//            if(in != null) {
//                BufferedReader br = new BufferedReader(new InputStreamReader(in));
//                String inputLine;
//
//                while ((inputLine = br.readLine()) != null) {
//                    System.out.println("MainFragment / GetRowsTask / inputLine = " + inputLine);
//                    strResult += inputLine;
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        System.out.println("MainFragment / GetRowsTask / result final = " + strResult);
//
//        // JSON array
//        try {
//            JSONArray jsonArray = new JSONArray(strResult);
//            for (int i = 0; i < jsonArray.length(); i++)
//            {
//                JSONObject jsonObject = (JSONObject) jsonArray.get(i);
//                count = jsonObject.getInt("totalPagesCount");
//            }
//            isGetReady = true;
//        }
//        catch (Exception e)
//        {
//            e.printStackTrace();
//        }
//        return null;
//    }
//
//    @Override
//    protected void onPostExecute(Void aVoid) {
//        getFragmentManager().beginTransaction().remove(mSpinnerFragment).commit();
//    }
//}
//
//
//
//private void loadRows() {
//
//    ArrayObjectAdapter rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
//    CardPresenter cardPresenter = new CardPresenter();
//
//    int row;
////		for (row = 0; row < NUM_ROWS; row++) {
//    for (row = 0; row < rowsCount; row++) {
//
//        // prepare
//        MovieList.isDataReady = false;
//        MovieList.prepareList(row+1);//table name starts from 1
//        while (!MovieList.isDataReady)
//        {
//            System.out.println("MainFragment / waiting ...");
//            SystemClock.sleep(1000);
//        }
//
//        // setup list
//        List<Movie> list = MovieList.setupMovies();
//
//        //			if (row != 0) {
////				Collections.shuffle(list);
////			}
//        ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(cardPresenter);
////			for (int j = 0; j < NUM_COLS; j++) {
//
//        for (int col = 0; col < list.size(); col++) {
//            listRowAdapter.add(list.get(col));
//        }
//
////			HeaderItem header = new HeaderItem(row, MovieList.MOVIE_CATEGORY[row]);
//        HeaderItem header = new HeaderItem(row, "合集 "+(row+1));
//        rowsAdapter.add(new ListRow(header, listRowAdapter));
//    }
//
//    HeaderItem gridHeader = new HeaderItem(row, "PREFERENCES");
//
//    GridItemPresenter mGridPresenter = new GridItemPresenter(this);
//    ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(mGridPresenter);
//    gridRowAdapter.add(getResources().getString(R.string.grid_view));
//    gridRowAdapter.add(getString(R.string.error_fragment));
//    gridRowAdapter.add(getResources().getString(R.string.personal_settings));
//    ///
//    String GRID_STRING_SPINNER = "Spinner";
//    gridRowAdapter.add(GRID_STRING_SPINNER);
//    ///
//    rowsAdapter.add(new ListRow(gridHeader, gridRowAdapter));
//
//    setAdapter(rowsAdapter);
//}
///

}
