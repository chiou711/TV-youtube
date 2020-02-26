/*
 * Copyright (c) 2019 The Android Open Source Project
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

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.cw.tv_yt.Pref;
import com.cw.tv_yt.R;
import com.cw.tv_yt.Utils;
import com.cw.tv_yt.data.DbHelper;
import com.cw.tv_yt.data.FetchCategoryService;
import com.cw.tv_yt.data.FetchVideoService;
import com.cw.tv_yt.data.VideoContract;
import com.cw.tv_yt.data.VideoProvider;
import com.cw.tv_yt.model.Video;
import com.cw.tv_yt.presenter.CardPresenter;
import com.cw.tv_yt.model.VideoCursorMapper;
import com.cw.tv_yt.presenter.GridItemPresenter;
import com.cw.tv_yt.presenter.IconHeaderItemPresenter;
import com.cw.tv_yt.recommendation.UpdateRecommendationsService;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
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
    final private static int YOUTUBE_LINK_INTENT = 99;
    public final static int VIDEO_DETAILS_INTENT = 98;
    // Maps a Loader Id to its CursorObjectAdapter.
    private SparseArray<CursorObjectAdapter> mVideoCursorAdapters;

    // workaround for keeping 1. cursor position 2. correct rows after Refresh
    private int rowsLoadedCount;
    private FetchServiceResponseReceiver responseReceiver;
    private LocalBroadcastManager localBroadcastMgr;

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
	    mPages = new ArrayList<>();

	    setTotalLinksCount();
    }

    AlertDialog.Builder builder;
    private AlertDialog alertDlg;
    private Handler handler;
    private int count;
    private String countStr;
    private String nextLinkTitle;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // check pages size, links size and video Id
//	    int pagesSize = mPages.size();
//	    System.out.println("pagesSize = " + pagesSize);
//	    for(int i=0;i<pagesSize;i++)
//	    {
//	    	List page = mPages.get(i);
//	    	int linksSize = page.size();
//		    System.out.println("linksSize = " +linksSize);
//	    	for(int j=0;j<linksSize;j++)
//		    {
//		    	System.out.println("video id = " + page.get(j));
//		    }
//	    }

        if(requestCode == YOUTUBE_LINK_INTENT) {
            count = 3; // countdown time to play next
            builder = new AlertDialog.Builder(getContext());

            setPlayId(getNewId());

            nextLinkTitle =  getYouTubeTitle();

            countStr = getActivity().getString(R.string.play_countdown)+
                              " " + count + " " +
                              getActivity().getString(R.string.play_time_unit);
            countStr = countStr.replaceFirst("[0-9]",String.valueOf(count));

            builder.setTitle(getActivity().getString(R.string.play_next))
                    .setMessage(getActivity().getString(R.string.play_4_spaces)+ nextLinkTitle +"\n\n" + countStr)
                    .setPositiveButton(getActivity().getString(R.string.play_stop), new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog1, int which1)
                        {
                            alertDlg.dismiss();
                            cancelYouTubeHandler();
                        }
                    });
            alertDlg = builder.create();

            // set listener for selection
            alertDlg.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dlgInterface) {
                    handler = new Handler();
                    handler.postDelayed(runCountDown,1000);
                }
            });
            alertDlg.show();
        }
        else if(requestCode == VIDEO_DETAILS_INTENT) {
            if(data != null) {
                int action = data.getIntExtra("KEY_DELETE",0);
                if (action == Pref.ACTION_DELETE)
                {
                    getActivity().finish();
                    // start new MainActivity
                    Intent new_intent = new Intent(getActivity(), MainActivity.class);
                    new_intent.addFlags(FLAG_ACTIVITY_CLEAR_TASK);
                    new_intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                    getActivity().startActivity(new_intent);
                }
            } else
            {
                //do nothing for non-action case
            }
        }
    }

    private String getYouTubeLink()
    {
        int focusCatNum = Utils.getPref_focus_category_number(getActivity());
        String table = VideoContract.VideoEntry.TABLE_NAME.concat(String.valueOf(focusCatNum));
        String columnName = VideoContract.VideoEntry.COLUMN_LINK_URL;
        int pos = getPlayId()-1;
        System.out.println("MainFragment / _getYouTubeLink / pos = " + pos);

        return getDB_data(table,columnName,pos);
    }

    private String getYouTubeTitle()
    {
        int focusCatNum = Utils.getPref_focus_category_number(getActivity());
        String table = VideoContract.VideoEntry.TABLE_NAME.concat(String.valueOf(focusCatNum));
        String columnName = VideoContract.VideoEntry.COLUMN_LINK_TITLE;
        int pos = getPlayId()-1;
        System.out.println("MainFragment / _getYouTubeTitle / pos = " + pos);

        return getDB_data(table,columnName,pos);
    }

    private String getDB_data(String table,String columnName,int pos)
    {
        DbHelper mOpenHelper = new DbHelper(getActivity());
        mOpenHelper.setWriteAheadLoggingEnabled(false);
        SQLiteDatabase sqlDb = mOpenHelper.getReadableDatabase();
        Cursor cursor = mOpenHelper.getReadableDatabase().query(
                table,
                null,//projection,
                null,//selection,
                null,//selectionArgs,
                null,
                null,
                null//sortOrder
        );

        int index = cursor.getColumnIndex(columnName);
        cursor.moveToPosition((int) pos);
        String retData = cursor.getString(index);
        cursor.close();
        sqlDb.close();

        return retData;
    }

    private int totalLinksCount;
    private void setTotalLinksCount()
    {
        int focusCatNum = Utils.getPref_focus_category_number(getActivity());
        String table = VideoContract.VideoEntry.TABLE_NAME.concat(String.valueOf(focusCatNum));
        DbHelper mOpenHelper = new DbHelper(getActivity());
        mOpenHelper.setWriteAheadLoggingEnabled(false);
        SQLiteDatabase sqlDb = mOpenHelper.getReadableDatabase();

        Cursor cursor = mOpenHelper.getReadableDatabase().query(
                table,
                null,//projection,
                null,//selection,
                null,//selectionArgs,
                null,
                null,
                null//sortOrder
        );

        totalLinksCount = cursor.getCount();
    }

    private int getTotalLinksCount()
    {
        return totalLinksCount;
    }

    /**
     * runnable for counting down
     */
    private Runnable runCountDown = new Runnable() {
        public void run() {
            // show count down
            TextView messageView = (TextView) alertDlg.findViewById(android.R.id.message);
            count--;
            countStr = getActivity().getString(R.string.play_countdown)+
                                " " + count + " " +
                              getActivity().getString(R.string.play_time_unit);
            countStr = countStr.replaceFirst("[0-9]",String.valueOf(count));
            messageView.setText( getActivity().getString(R.string.play_4_spaces)+ nextLinkTitle +"\n\n" +countStr);

            if(count>0)
                handler.postDelayed(runCountDown,1000);
            else
            {
                // launch next intent
                alertDlg.dismiss();
                cancelYouTubeHandler();
                launchYouTubeIntent();

                // prepare next Id
                setNewId(getPlayId() + 1);
            }
        }
    };

    private void cancelYouTubeHandler()
    {
        if(handler != null) {
            handler.removeCallbacks(runCountDown);
            handler = null;
        }
    }


    int delay = 10;
    /**
     *  launch next YouTube intent
     */
    private void launchYouTubeIntent()
    {
//        if(MainFragment.currLinkId >= MainFragment.getCurrLinksLength())
        //refer: https://developer.android.com/reference/android/view/KeyEvent.html#KEYCODE_DPAD_DOWN_RIGHT

        //todo temp mark, wait for adding page factor
        // check if at the end of row
        if(isRowEnd())
        {
            // from test result current capability is shift left 15 steps only
            DPadAsyncTask task = new DPadAsyncTask(backSteps);
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        else
        {
            // delay
            try {
                Thread.sleep(delay*100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            BaseInputConnection mInputConnection = new BaseInputConnection(getActivity().findViewById(R.id.main_frame), true);
            mInputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT));
            mInputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT));

            // delay
            try {
                Thread.sleep(delay * 100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            String video_url = getYouTubeLink();
            startYouTubeIntent(video_url);
        }
    }

    private void startYouTubeIntent(String url)
    {
        String idStr = getYoutubeId(url);
        //Intent intent = YouTubeIntents.createPlayVideoIntentWithOptions(getActivity(), idStr, true/*fullscreen*/, true/*finishOnEnd*/);
        Intent intent  = YouTubeIntents.createPlayVideoIntent(getActivity(), idStr);
        intent.putExtra("force_fullscreen", true);
        intent.putExtra("finish_on_ended", true);
        startActivityForResult(intent, YOUTUBE_LINK_INTENT);
    }

    private class DPadAsyncTask extends AsyncTask<Void, Integer, Void> {
        int dPadSteps;
        DPadAsyncTask(int dPadSteps)
        {
            this.dPadSteps = dPadSteps;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected Void doInBackground(Void... params)
        {
            BaseInputConnection mInputConnection = new BaseInputConnection(getActivity().findViewById(R.id.main_frame), true);

            // point to first item of current row
            for(int i=0;i<dPadSteps;i++)
            {
                System.out.println("MainFragment / DPadAsyncTask / back i = " + i);
                mInputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT));
                mInputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT));
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // add delay to make sure key event works
            try {
                Thread.sleep(delay * 100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // point to first row if meets the end of last row
            //todo temp mark, wait for adding page factor
            if(getPlayId() == 1)
            {
                  for (int i = (mPages.size()-1); i >= 1; i--) {
                    mInputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP));
                    mInputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_UP));
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            else
            {
                // point to next row
                BaseInputConnection connection = new BaseInputConnection(getActivity().findViewById(R.id.main_frame), true);
                connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN));
                connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN));
            }

            // add delay for viewer
            try {
                Thread.sleep(delay * 100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            String video_url = getYouTubeLink();
            startYouTubeIntent(video_url);
        }
    }

    private int backSteps;

    private boolean isRowEnd()
    {
        boolean isEnd = false;
        System.out.println("isRowEnd / getNewId() = " + getNewId());
        backSteps = 0;

        for(int i=0;i<mPages.size();i++) {
            List<Integer> page = mPages.get(i);
            int firstIdOfRow = page.get(0);
            System.out.println("isRowEnd / i = " + i);
            System.out.println("isRowEnd / firstIdOfRow = " + firstIdOfRow);
            System.out.println("isRowEnd / getNewId() = " + getNewId());

            if(firstIdOfRow == getNewId()) {
                isEnd = true;
                if(getNewId() == 1) {
                    // back steps for last row or 1st row
                    backSteps = mPages.get(mPages.size() - 1).size() - 1;
                }
                else if(i != 0) {
                    // back steps for other row
                    backSteps = mPages.get(i - 1).size() - 1;
                }

                break;
            }
        }
        System.out.println("isRowEnd / backSteps = " + backSteps);
        return isEnd;
    }


    @Override
    public void onResume() {
        super.onResume();

        System.out.println("MainFragment / _onResume");

        // receiver for fetch video service
        IntentFilter statusIntentFilter = new IntentFilter(FetchVideoService.Constants.BROADCAST_ACTION);
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
                    VideoContract.CategoryEntry.CONTENT_URI, // Table to query
                    new String[]{"DISTINCT " + VideoContract.CategoryEntry.COLUMN_CATEGORY_NAME},
                    // Only categories
                    null, // No selection clause
                    null, // No selection arguments
                    null  // Default sort order
            );

        }
        else if (id == TITLE_LOADER) {
            return new CursorLoader(
                    getContext(),
                    VideoContract.VideoEntry.CONTENT_URI, // Table to query
                    new String[]{"DISTINCT " + VideoContract.VideoEntry.COLUMN_ROW_TITLE},
                    // Only categories
                    null, // No selection clause
                    null, // No selection arguments
                    null  // Default sort order
            );

        } else {
            // Assume it is for a video.
            String title = args.getString(VideoContract.VideoEntry.COLUMN_ROW_TITLE);
            System.out.println("MainFragment / _onCreateLoader / title = "+ title);
            // This just creates a CursorLoader that gets all videos.
            return new CursorLoader(
                    getContext(),
                    VideoContract.VideoEntry.CONTENT_URI, // Table to query
                    null, // Projection to return - null means return all fields
                    VideoContract.VideoEntry.COLUMN_ROW_TITLE + " = ?", // Selection clause
                    new String[]{title},  // Select based on the rowTitle id.
                    null // Default sort order
            );
        }
    }

    private List<List> mPages;
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
                    int categoryIndex = data.getColumnIndex(VideoContract.CategoryEntry.COLUMN_CATEGORY_NAME);
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
                    int titleIndex = data.getColumnIndex(VideoContract.VideoEntry.COLUMN_ROW_TITLE);
                    String title = data.getString(titleIndex);
                    System.out.println("MainFragment / _onLoadFinished / title = " + title);

                    // Create header for this category.
                    HeaderItem header = new HeaderItem(title);

                    int videoLoaderId = title.hashCode(); // Create unique int from title.
                    CursorObjectAdapter existingAdapter = mVideoCursorAdapters.get(videoLoaderId);
                    row_id++;
                    if (existingAdapter == null) {

                        // Map video results from the database to Video objects.
                        CursorObjectAdapter videoCursorAdapter = new CursorObjectAdapter(new CardPresenter(getActivity()));
                        videoCursorAdapter.setMapper(new VideoCursorMapper());
                        mVideoCursorAdapters.put(videoLoaderId, videoCursorAdapter);

                        ListRow row = new ListRow(header, videoCursorAdapter);
                        mTitleRowAdapter.add(row);
                        row.setId(row_id);
	                    System.out.println("MainFragment / _onLoadFinished / existingAdapter is null  / will initLoader / videoLoaderId = " + videoLoaderId);

                        // Start loading the videos from the database for a particular category.
                        Bundle args = new Bundle();
                        args.putString(VideoContract.VideoEntry.COLUMN_ROW_TITLE, title);
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
                gridRowAdapter.add(getString(R.string.select_category));
	            gridRowAdapter.add(getString(R.string.grid_view));
//                gridRowAdapter.add(getString(R.string.guidedstep_first_title));
//                gridRowAdapter.add(getString(R.string.error_fragment));
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

                int columnIndex = data.getColumnIndex(VideoContract.VideoEntry._ID);
                int video_id = data.getInt(columnIndex);
                System.out.println("MainFragment / _onLoadFinished / 1st video_id of row = " + video_id);
                int sizeOfRowLinks = data.getCount();
	            System.out.println("MainFragment / _onLoadFinished / sizeOfLinks= " + sizeOfRowLinks);

	            List<Integer> page = new ArrayList<>();
	            for(int i=video_id;i<(video_id+sizeOfRowLinks);i++)
	                page.add(i);

	            mPages.add(page);

                // one row added
                rowsLoadedCount++;
                System.out.println("MainFragment / _onLoadFinished / rowsLoadedCount = "+ rowsLoadedCount);
            }
        } else {
            /***
             *  call fetch service to load or update data base
             */

            // show toast
            Toast.makeText(getActivity(),getString(R.string.database_update),Toast.LENGTH_LONG).show();

            // Start an Intent to fetch the categories
            if ((loader.getId() == CATEGORY_LOADER) && (mCategoryNames.size() == 0)) {
                Utils.setPref_focus_category_number(getActivity(), INIT_NUMBER);

                System.out.println("MainFragment / onLoadFinished / start Fetch category service =================================");
                Intent serviceIntent = new Intent(getActivity(), FetchCategoryService.class);
                serviceIntent.putExtra("FetchUrl", getDefaultUrl());
                getActivity().startService(serviceIntent);
            }
            // Start an Intent to fetch the videos
            else if ((loader.getId() == TITLE_LOADER) && (rowsLoadedCount == 0)) {
                System.out.println("MainFragment / onLoadFinished / start Fetch video service =================================");

                // avoid endless loop due to empty category selection
                Utils.setPref_focus_category_number(getContext(),1);

                Intent serviceIntent = new Intent(getActivity(), FetchVideoService.class);
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
                Video video = (Video) item;
                System.out.println("MainFragment / onItemClicked / id = "+ video.id );

                String urlStr = ((Video) item).videoUrl;

                String path;
                // YouTube or HTML
                if(urlStr.contains("youtube") || urlStr.contains("youtu.be"))
                    path = "https://img.youtube.com/vi/"+getYoutubeId(urlStr)+"/0.jpg";
                else
                    path = urlStr;

                System.out.println("MainFragment / onItemClicked / path= "+ path);
                new Thread(new Runnable(){
                    @Override
                    public void run() {
                        /**
                         *  check connection response
                         *  404: not found, 200: OK
                         */
                        int responseCode = -1;
                        HttpURLConnection urlConnection = null;
                        try {
                            URL url = new URL(path);
                            urlConnection = (HttpURLConnection) url.openConnection();
                            urlConnection.setRequestMethod("GET");
                            urlConnection.connect();
                            responseCode = urlConnection.getResponseCode();
                            System.out.println("MainFragment / _onItemClicked / responseCode  OK = " + responseCode);
                        }
                        catch (IOException e)
                        {
                            System.out.println("MainFragment / _onItemClicked / responseCode NG = "+ responseCode);
                            e.printStackTrace();
                            return;
                        }
                        urlConnection.disconnect();

                        /**
                         *  normal response: launch VideoDetailsActivity
                         */
                        // YouTube or HTML
                        if(responseCode == 200) {
                            // play YouTube
                            if(urlStr.contains("youtube") || urlStr.contains("youtu.be"))
                            {
                                    // auto play
                                if (Pref.isAutoPlay(getActivity())) {
                                    setPlayId((int) ((Video) (item)).id);
                                    startYouTubeIntent(((Video) item).videoUrl);
                                    setNewId(getPlayId() + 1);
                                } else {
                                    // manual play
                                    Intent intent = new Intent(getActivity(), VideoDetailsActivity.class);
                                    intent.putExtra(VideoDetailsActivity.VIDEO, video);

                                    getActivity().runOnUiThread(new Runnable() {
                                        public void run() {
                                            if (urlStr.contains("youtube") || urlStr.contains("youtu.be")) {
                                                // play YouTube
                                                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                                                        getActivity(),
                                                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
                                                        VideoDetailsActivity.SHARED_ELEMENT_NAME).toBundle();
                                                startActivityForResult(intent, VIDEO_DETAILS_INTENT, bundle);
                                            }
                                        }
                                    });
                                }
                            }
                            else {
                                // play HTML
                                if(urlStr.contains("8maple") || urlStr.contains("google")) {
                                    String link = ((Video) item).videoUrl;
                                    Uri uriStr = Uri.parse(link);
                                    Intent intent = new Intent(Intent.ACTION_VIEW, uriStr);
                                    startActivity(intent);
                                }
                                // play video
                                else {
                                    Intent intent = new Intent(getActivity(), PlaybackActivity.class);
                                    intent.putExtra(VideoDetailsActivity.VIDEO, ((Video) item));
                                    startActivity(intent);
                                }
                            }
                        } else {
                            /**
                             *  show connection error toast
                             */
                               getActivity().runOnUiThread(new Runnable() {
                               public void run() {
                                   Toast.makeText(getActivity(), getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
                               }
                            });
                        }
                    }
                }).start();

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
                    DbHelper mOpenHelper = new DbHelper(getActivity());
                    mOpenHelper.setWriteAheadLoggingEnabled(false);
                    SQLiteDatabase sqlDb = mOpenHelper.getReadableDatabase();

                    String SQL_GET_ALL_TABLES = "SELECT * FROM sqlite_master WHERE name like 'video%'";
                    Cursor cursor = sqlDb.rawQuery(SQL_GET_ALL_TABLES, null);
                    int countVideoTables = cursor.getCount();
                    cursor.close();
                    sqlDb.close();

                    // remove category name key
                    for(int i = 1; i<= countVideoTables; i++)
                        Utils.removePref_category_name(getActivity(),i);

                } else if (((String) item).contains(getString(R.string.select_category))) {

                    localBroadcastMgr.unregisterReceiver(responseReceiver);
                    responseReceiver = null;

                    Intent intent = new Intent(getActivity(), SelectCategoryActivity.class);
                    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity()).toBundle();
                    startActivity(intent, bundle);

                } else if (((String) item).contains(getString(R.string.grid_view))) {
			        Intent intent = new Intent(getActivity(), VerticalGridActivity.class);
			        Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity()).toBundle();
			        startActivity(intent, bundle);
//                } else if (((String) item).contains(getString(R.string.guidedstep_first_title))) {
//                    Intent intent = new Intent(getActivity(), GuidedStepActivity.class);
//                    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity()).toBundle();
//                    startActivity(intent, bundle);
//                } else if (((String) item).contains(getString(R.string.error_fragment))) {
//                    BrowseErrorFragment errorFragment = new BrowseErrorFragment();
//                    getFragmentManager().beginTransaction().replace(R.id.main_frame, errorFragment).addToBackStack(null).commit();
                } else if(((String) item).contains(getString(R.string.personal_settings))) {
                    Intent intent = new Intent(getActivity(), SettingsActivity.class);
                    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity()).toBundle();
                    startActivity(intent, bundle);
//                } else {
                    //Toast.makeText(getActivity(), ((String) item), Toast.LENGTH_SHORT).show();
                }

            }
        }
    }

    // play id
    private int play_id;
    private void setPlayId(int id)
    {
        play_id = id;
    }

    private int getPlayId()
    {
        return play_id;
    }


    // new id
    private int new_id;
    private void setNewId(int id)
    {
        if(id > getTotalLinksCount())
            new_id = 1;
        else
            new_id = id;
    }

    private int getNewId()
    {
        return new_id;
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
            getActivity().deleteDatabase(DbHelper.DATABASE_NAME);

            ContentResolver resolver = getActivity().getContentResolver();
            ContentProviderClient client = resolver.acquireContentProviderClient(VideoContract.CONTENT_AUTHORITY);
            VideoProvider provider = (VideoProvider) client.getLocalContentProvider();

            provider.mContentResolver = resolver;
            provider.mOpenHelper.close();

            provider.mOpenHelper = new DbHelper(getActivity());
            provider.mOpenHelper.setWriteAheadLoggingEnabled(false);

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
            if(itemViewHolder!= null && itemViewHolder.view != null)
                itemViewHolder.view.setBackgroundColor(getResources().getColor(R.color.selected_background));

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
            String statusStr = intent.getExtras().getString(FetchCategoryService.Constants.EXTENDED_DATA_STATUS);
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