/*
 * Copyright (c) 2022 The Android Open Source Project
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
import android.os.Bundle;
import android.os.Handler;

import androidx.fragment.app.FragmentActivity;
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
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.widget.Button;
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
import com.cw.tv_yt.data.Pair;
import com.cw.tv_yt.data.Source_links;
import com.cw.tv_yt.data.VideoContract;
import com.cw.tv_yt.define.Define;
import com.cw.tv_yt.model.Video;
import com.cw.tv_yt.presenter.CardPresenter;
import com.cw.tv_yt.model.VideoCursorMapper;
import com.cw.tv_yt.presenter.GridItemPresenter;
import com.cw.tv_yt.presenter.IconHeaderItemPresenter;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.cw.tv_yt.Utils.getYoutubeId;
import static com.cw.tv_yt.define.Define.INIT_CATEGORY_NUMBER;

import com.cw.tv_yt.ui.options.select_category.SelectCategoryActivity;
import com.cw.tv_yt.ui.add_category.AddCategoryActivity;
import com.cw.tv_yt.ui.options.setting.SettingsActivity;
import com.cw.tv_yt.ui.options.browse_category.BrowseCategoryActivity;
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
    private static final int CATEGORY_LOADER = 100; // Unique ID for Category Loader.
    private static final int TITLE_LOADER = 101; // Unique ID for Title Loader.
	public static List<String> mCategoryNames = new ArrayList<>();
    private final static int YOUTUBE_LINK_INTENT = 98;
    public final static int VIDEO_DETAILS_INTENT = 99;
    // Maps a Loader Id to its CursorObjectAdapter.
    private SparseArray<CursorObjectAdapter> mVideoCursorAdapters;

    private FetchServiceResponseReceiver responseReceiver;
    private LocalBroadcastManager localBroadcastMgr;

    // loaded rows after Refresh
    private int rowsLoadedCount;

    private FragmentActivity act;
    public static List<RowLength> rowLengthList;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        System.out.println("MainFragment / _onAttach");

        // Create a list to contain all the CursorObjectAdapters.
        // Each adapter is used to render a specific row of videos in the MainFragment.
        mVideoCursorAdapters = new SparseArray<>();//new HashMap<>();

        // Start loading the titles from the database.
        mLoaderManager = LoaderManager.getInstance(this);
        mLoaderManager.initLoader(CATEGORY_LOADER, null, this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // Final initialization, modifying UI elements.
        super.onActivityCreated(savedInstanceState);

        act = getActivity();

        System.out.println("MainFragment / _onActivityCreated");
        // Prepare the manager that maintains the same background image between activities.
        prepareBackgroundManager();

        setupUIElements();

        setupEventListeners();
        prepareEntranceTransition();

        // Map title results from the database to ListRow objects.
        // This Adapter is used to render the MainFragment sidebar labels.

//        updateRecommendations();

    }

    AlertDialog.Builder builder;
    private AlertDialog alertDlg;
    private Handler handler;
    private int count;
    private String countStr;
    private String nextLinkTitle;

    public void onResume() {
        super.onResume();

        System.out.println("MainFragment / _onResume");

        // receiver for fetch video service
        IntentFilter statusIntentFilter = new IntentFilter(FetchVideoService.Constants.BROADCAST_ACTION);
        responseReceiver = new FetchServiceResponseReceiver();

        // Registers the FetchServiceResponseReceiver and its intent filters
        localBroadcastMgr = LocalBroadcastManager.getInstance(act);
        localBroadcastMgr.registerReceiver(responseReceiver, statusIntentFilter );
    }

    @Override
    public void onPause() {
        super.onPause();
        System.out.println("MainFragment / _onPause");

        if(alertDlg != null)
            alertDlg.dismiss();

        cancelYouTubeHandler(); //todo ??? Why not stop when power off
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
        mBackgroundManager = BackgroundManager.getInstance(act);
        mBackgroundManager.attach(act.getWindow());
        mDefaultBackground = getResources().getDrawable(R.drawable.default_background, null);
        mBackgroundTask = new UpdateBackgroundTask();
        mMetrics = new DisplayMetrics();
        act.getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void setupUIElements() {

        // option: drawable
        setBadgeDrawable(act.getResources().getDrawable(R.drawable.tt, null));

        // Badge, when set, takes precedent over title
        // option: title
//        int focusNumber = getPref_focus_category_number(act);
//        String categoryName = Utils.getPref_category_name(act,focusNumber);
        //setTitle(getString(R.string.browse_title));
//        if(!categoryName.equalsIgnoreCase(String.valueOf(focusNumber)))
//            setTitle(categoryName);

        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true); //true: focus will return to header, false: will close App

        // Set fastLane (or headers) background color
        setBrandColor(ContextCompat.getColor(act, R.color.fastlane_background));

        // Set search icon color.
        setSearchAffordanceColor(ContextCompat.getColor(act, R.color.default_background));

        setHeaderPresenterSelector(new PresenterSelector() {
            @Override
            public Presenter getPresenter(Object o) {
                return new IconHeaderItemPresenter(act);
            }
        });
    }

    private void setupEventListeners() {
        setOnSearchClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(act, AddCategoryActivity.class);
                startActivity(intent);
            }
        });

        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new ItemViewSelectedListener());
    }

    // public static int currentNavPosition;
    // selected is navigated here
    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {

            if(itemViewHolder!= null && itemViewHolder.view != null)
                itemViewHolder.view.setBackgroundColor(getResources().getColor(R.color.selected_background));

            if (item instanceof Video) {
//                System.out.println("---------- onItemSelected / video");
                mBackgroundURI = Uri.parse(((Video) item).bgImageUrl);
                startBackgroundTimer();
            }
            else if (item instanceof String) {
                System.out.println("---------- onItemSelected / category / item = " + item);
//                for(int i=0;i<mCategoryNames.size();i++)
//                {
//                    if(item.toString().equalsIgnoreCase(mCategoryNames.get(i)))
//                    {
//                        currentNavPosition = i;
//                        System.out.println("---------- current navigation position = " + currentNavPosition);
//                    }
//                }

//                int currentNavPosition =  gridRowAdapterCategory.indexOf(item);
//                System.out.println("----------  currentNavPosition = " + currentNavPosition);

                // switch category by onItemViewSelected
//                String cate_name = Utils.getPref_category_name(act);
//                if( !cate_name.equalsIgnoreCase((String)item) && isOkToChangeCategory)
//                    switchCategory(item);
            }
        }
    }

    boolean isOkToChangeCategory;
    // Switch category by category name
    void switchCategory(Object catName) {

        // renew list for Show row number and link number
        rowLengthList = new ArrayList<>();

        String categoryName =  (String) catName;
        // After delay, start switch DB
        new Handler().postDelayed(new Runnable() {
            public void run() {
                isOkToChangeCategory = false;
                if(isCategoryRow(categoryName)) {
                    try {
                        // switch DB
                        Utils.setPref_category_name(getContext(), categoryName );
                        mLoaderManager.destroyLoader(TITLE_LOADER);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }, 100);
    }

    int currentRowPos;
    int currentRow1stId;
    int currentRowSize;
    int currentRowLastId;
    static boolean isLongClicked;
    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            // item is video
            if (item instanceof Video) {
                openVideoItem(item);
            } else if (item instanceof String) {
                System.out.println("MainFragment / onItemClicked / item = "+ item);
                // item is Select category
                if (((String) item).contains(getString(R.string.select_category))) {

                    localBroadcastMgr.unregisterReceiver(responseReceiver);
                    responseReceiver = null;

                    Intent intent = new Intent(act, SelectCategoryActivity.class);
                    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(act).toBundle();
                    startActivity(intent, bundle);

                // item is Browse category
                } else if (((String) item).contains(getString(R.string.category_grid_view_title))) {
                    Intent intent = new Intent(act, BrowseCategoryActivity.class);
                    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(act).toBundle();
                    startActivity(intent, bundle);

                // item is Setting
                } else if(((String) item).contains(getString(R.string.personal_settings))) {
                    Intent intent = new Intent(act, SettingsActivity.class);
                    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(act).toBundle();
                    startActivity(intent, bundle);

                // item is Category
                } else {
                    if(isLongClicked) {
                        System.out.println("--- is Long clicked");
                        isLongClicked = false;
                        return;
                    } else {
                        // switch category by onItemClicked
                        switchCategory(item);
                    }
                }
//                } else if (((String) item).contains(getString(R.string.guidedstep_first_title))) {
//                    Intent intent = new Intent(act, GuidedStepActivity.class);
//                    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(act).toBundle();
//                    startActivity(intent, bundle);
//                } else if (((String) item).contains(getString(R.string.error_fragment))) {
//                    BrowseErrorFragment errorFragment = new BrowseErrorFragment();
//                    getFragmentManager().beginTransaction().replace(R.id.main_frame, errorFragment).addToBackStack(null).commit();
//                } else {
                    //Toast.makeText(act, ((String) item), Toast.LENGTH_SHORT).show();

            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == YOUTUBE_LINK_INTENT) {
            count = Define.DEFAULT_COUNT_DOWN_TIME_TO_PLAY_NEXT; // countdown time to play next
            builder = new AlertDialog.Builder(getContext());

            // prepare next Id
            int nextId_auto = getNextCursorPositionId_auto(getPlayId());
            setNextId_auto(nextId_auto);

            // set new play Id
            setPlayId(nextId_auto);

            nextLinkTitle =  getYouTubeTitle();

            countStr = act.getString(R.string.play_countdown)+
                              " " + count + " " +
                              act.getString(R.string.play_time_unit);
            countStr = countStr.replaceFirst("[0-9]",String.valueOf(count));

            builder.setTitle(act.getString(R.string.play_next))
                    .setMessage(act.getString(R.string.play_4_spaces)+ nextLinkTitle +"\n\n" + countStr)
                    .setPositiveButton(act.getString(R.string.play_stop), new DialogInterface.OnClickListener()
                    {
                        // stop
                        @Override
                        public void onClick(DialogInterface dialog1, int which1)
                        {
                            alertDlg.dismiss();
                            cancelYouTubeHandler();
                        }
                    })
                    .setNegativeButton(act.getString(R.string.guidedstep_continue), new DialogInterface.OnClickListener()
                    {
                        // continue
                        @Override
                        public void onClick(DialogInterface dialog1, int which1)
                        {
                            alertDlg.dismiss();
                            cancelYouTubeHandler();

                            // launch next intent
                            alertDlg.dismiss();
                            cancelYouTubeHandler();
                            launchYouTubeIntent();
                        }
                    }).
                    setOnCancelListener(new DialogInterface.OnCancelListener(){
                        // cancel
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            alertDlg.dismiss();
                            cancelYouTubeHandler();
                        }
                    } );
            alertDlg = builder.create();

            // set listener for selection
            alertDlg.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dlgInterface) {
                    handler = new Handler();
                    handler.postDelayed(runCountDown,1000);

                    // focus
                    Button negative = alertDlg.getButton(AlertDialog.BUTTON_NEGATIVE);
                    negative.setFocusable(true);
                    negative.setFocusableInTouchMode(true);
                    negative.requestFocus();
                }
            });
            alertDlg.show();
        }
        else if(requestCode == VIDEO_DETAILS_INTENT) {
            if(data != null) {
                int action = data.getIntExtra("KEY_DELETE",0);
                if (action == Pref.ACTION_DELETE)
                {
                    act.finish();
                    // start new MainActivity
                    Intent new_intent = new Intent(act, MainActivity.class);
                    new_intent.addFlags(FLAG_ACTIVITY_CLEAR_TASK);
                    new_intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                    act.startActivity(new_intent);
                }
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == CATEGORY_LOADER)
            System.out.println("MainFragment / _onCreateLoader / id = CATEGORY_LOADER");
        else if(id == TITLE_LOADER)
            System.out.println("MainFragment / _onCreateLoader / id = TITLE_LOADER");
        else
            System.out.println("MainFragment / _onCreateLoader / id = "+ id);

        // init loaded rows count
        rowsLoadedCount = 0;

        // init playlists
        mPlayLists = new ArrayList<>();

        // list for Show row number and link number
        rowLengthList = new ArrayList<>();


        if (id == CATEGORY_LOADER) {
            return new CursorLoader(
                    Objects.requireNonNull(getContext()),
                    VideoContract.CategoryEntry.CONTENT_URI, // Table to query
                    // not show duplicated category name
                    new String[]{"DISTINCT " + VideoContract.CategoryEntry.COLUMN_CATEGORY_NAME,
                            VideoContract.CategoryEntry.COLUMN_VIDEO_TABLE_ID},
                    // show duplicated category name
//                    new String[]{VideoContract.CategoryEntry.COLUMN_CATEGORY_NAME},
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

    private List<List> mPlayLists;

    int row_id;


    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
//        System.out.println("MainFragment / _onLoadFinished /  start rowsLoadedCount = " + rowsLoadedCount);
//        System.out.println("MainFragment / _onLoadFinished /  mVideoCursorAdapters.size() = " + mVideoCursorAdapters.size());

        // return when load is OK
        if( (rowsLoadedCount!=0 ) && (rowsLoadedCount >= mVideoCursorAdapters.size()) ) {
            System.out.println("MainFragment / _onLoadFinished / return");//??? not needed this?
            return;
        }

        // cursor data is not null
        if (data != null && data.moveToFirst()) {
            final int loaderId = loader.getId();

            if (loaderId == CATEGORY_LOADER)
	            System.out.println("MainFragment / _onLoadFinished / loaderId = CATEGORY_LOADER");
            else if(loaderId == TITLE_LOADER)
                System.out.println("MainFragment / _onLoadFinished / loaderId = TITLE_LOADER");
            else
                System.out.println("MainFragment / _onLoadFinished / loaderId (video) = " + loaderId);

            if (loaderId == CATEGORY_LOADER) {
                mCategoryNames = new ArrayList<>();
                // Iterate through each category entry and add it to the ArrayAdapter.
                while (!data.isAfterLast()) {
                    int categoryIndex = data.getColumnIndex(VideoContract.CategoryEntry.COLUMN_CATEGORY_NAME);
                    String category_name = data.getString(categoryIndex);
                    System.out.println("MainFragment / _onLoadFinished / category_name = " + category_name);
                    mCategoryNames.add(category_name);

                    // check only
//                    int video_table_id_index = data.getColumnIndex(VideoContract.CategoryEntry.COLUMN_VIDEO_TABLE_ID);
//                    int video_table_id = data.getInt(video_table_id_index);
//                    System.out.println("MainFragment / _onLoadFinished / video_table_id = " + video_table_id);

                    data.moveToNext();
                }

                //start loading video
                mLoaderManager.initLoader(TITLE_LOADER, null, this);

            } else if (loaderId == TITLE_LOADER) {

                // create category list row
                createListRow_category();

                // create video list rows
                row_id = createListRows_video(data);

                // create option list row
                createListRow_option(row_id);

                // init row position and focus item
                setSelectedPosition(0);
                int pos = getFocusItemPosition_categoryRow();
                setSelectedPosition(0, true, new ListRowPresenter.SelectItemViewHolderTask(pos));

                startEntranceTransition(); //Move startEntranceTransition to after all

                /*
                 *  end of loading category
                 */
//                System.out.println("MainFragment / _onLoadFinished / -----------------------------------------");
//                System.out.println("MainFragment / _onLoadFinished / end of onLoadFinished category");
//                System.out.println("MainFragment / _onLoadFinished / -----------------------------------------");

            } else {
                // The CursorAdapter(mVideoCursorAdapters)
                // contains a Cursor pointing to all videos.
                if((mVideoCursorAdapters!= null) &&
                   (mVideoCursorAdapters.get(loaderId)!= null))
                    mVideoCursorAdapters.get(loaderId).changeCursor(data);

                int columnIndex = data.getColumnIndex(VideoContract.VideoEntry._ID);
                int video_id = data.getInt(columnIndex);
//                System.out.println("MainFragment / _onLoadFinished / 1st video_id of row = " + video_id);
                int sizeOfRowLinks = data.getCount();
//                System.out.println("MainFragment / _onLoadFinished / sizeOfLinks= " + sizeOfRowLinks);

                // start number and links count of a row
                rowLengthList.add(new RowLength(video_id,sizeOfRowLinks));

                List<Integer> playlist = new ArrayList<>();
                for(int i=video_id;i<(video_id+sizeOfRowLinks);i++)
                    playlist.add(i);

                mPlayLists.add(playlist);

                // one row added
                rowsLoadedCount++;
//                System.out.println("MainFragment / _onLoadFinished / rowsLoadedCount = "+ rowsLoadedCount);

                /**
                 *  end of loading video
                 * */
                if(rowsLoadedCount == mVideoCursorAdapters.size() )
                {
                    setPlaylistsCount(rowsLoadedCount);
                    isOkToChangeCategory = true;

                    System.out.println("MainFragment / _onLoadFinished / -------------------------------------");
                    System.out.println("MainFragment / _onLoadFinished / end of onLoadFinished video");
                    System.out.println("MainFragment / _onLoadFinished / -------------------------------------");
                }
            }
        } else { // cursor data is null after App installation
            /***
             *  call fetch service to load or update data base
             */

            // Start an Intent to fetch the categories
            if ((loader.getId() == CATEGORY_LOADER) && (mCategoryNames == null)) {
                System.out.println("MainFragment / onLoadFinished / start Fetch category service =================================");

                // show toast
                Toast.makeText(act,getString(R.string.database_update),Toast.LENGTH_LONG).show();

                // data base is not created yet, call service for the first time
                Intent serviceIntent = new Intent(act, FetchCategoryService.class);
                int linkSrcNum = Utils.getPref_link_source_number(act);
                serviceIntent.putExtra("FetchUrl", getDefaultUrl(linkSrcNum) );
                act.startService(serviceIntent);
            }
            // Start an Intent to fetch the videos
            else if ((loader.getId() == TITLE_LOADER) && (rowsLoadedCount == 0)) {
                System.out.println("MainFragment / onLoadFinished / start Fetch video service =================================");

                Intent serviceIntent = new Intent(act, FetchVideoService.class);
                int linkSrcNum = Utils.getPref_link_source_number(act);
                serviceIntent.putExtra("FetchUrl", getDefaultUrl(linkSrcNum));
                act.startService(serviceIntent);
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
            mVideoCursorAdapters.clear();

            rowsLoadedCount = 0;
            mLoaderManager.restartLoader(CATEGORY_LOADER, null, this);
        }
    }

    //
    // create Category presenter
    //
    void createListRow_category(){
        // set focus category
        int focusPos = getFocusItemPosition_categoryRow();
        CategoryListRowPresenter cate_listRowPresenter = new CategoryListRowPresenter(act,focusPos);
        cate_listRowPresenter.setRowHeight(400);
        mTitleRowAdapter = new ArrayObjectAdapter(cate_listRowPresenter);
        setAdapter(mTitleRowAdapter);

        // category UI
        // Create a row for category selections at top
        String cate_name = Utils.getPref_category_name(act);
        String curr_cate_message;

        // initial category name
        if(cate_name.equalsIgnoreCase("no category name")){
            // get first available category name
            cate_name = mCategoryNames.get(INIT_CATEGORY_NUMBER - 1);
            Utils.setPref_category_name(getActivity(),cate_name);
        }

        // current category message
        curr_cate_message = act.getResources().
                getString(R.string.current_category_title).
                concat(" : ").
                concat(cate_name);

        // category header
        HeaderItem cate_gridHeader = new HeaderItem(curr_cate_message);

        // Category item presenter
        CategoryGridItemPresenter cate_gridItemPresenter= new CategoryGridItemPresenter(this,mCategoryNames);
        ArrayObjectAdapter cate_gridRowAdapter = new ArrayObjectAdapter(cate_gridItemPresenter);

        // show category name
        for(int i=1;i<= mCategoryNames.size();i++)
            cate_gridRowAdapter.add(mCategoryNames.get(i-1));

        // category list row
        ListRow cate_listRow = new ListRow(cate_gridHeader, cate_gridRowAdapter);

        // add category row
        mTitleRowAdapter.add(cate_listRow);
    }

    //
    // create Video presenter
    //
    int createListRows_video(Cursor data){
        // row id count start
        int row_id = 0;
//                listRowCategory.setId(row_id);

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
            System.out.println("MainFragment / _onLoadFinished / header.getName() = " + header.getName());

            // if (getHeadersSupportFragment() != null){
                // on selected
//                getHeadersSupportFragment().setOnHeaderViewSelectedListener(new HeadersSupportFragment.OnHeaderViewSelectedListener() {
//                    @Override
//                    public void onHeaderSelected(RowHeaderPresenter.ViewHolder viewHolder, Row row) {
//                        System.out.println("MainFragment / _onLoadFinished / setOnHeaderViewSelectedListener /" +
//                                "  = " + row.getId() + " / "
//                                + row.getHeaderItem().getName());
//                    }
//                });

                // on clicked
//                getHeadersSupportFragment().setOnHeaderClickedListener(new HeadersSupportFragment.OnHeaderClickedListener() {
//                    @Override
//                    public void onHeaderClicked(RowHeaderPresenter.ViewHolder viewHolder, Row row) {
//                        System.out.println("MainFragment / _onLoadFinished / setOnHeaderClickedListener /" +
//                                " row ID = " + row.getId() + " / header name = "
//                                + row.getHeaderItem().getName());
//                    }
//                });
//            }

            int videoLoaderId = title.hashCode(); // Create unique int from title.
            CursorObjectAdapter existingAdapter = mVideoCursorAdapters.get(videoLoaderId);
            row_id++;
            if (existingAdapter == null) {

                // Map video results from the database to Video objects.
                CursorObjectAdapter videoCursorAdapter = new CursorObjectAdapter(new CardPresenter(act,row_id));
                videoCursorAdapter.setMapper(new VideoCursorMapper());
                mVideoCursorAdapters.put(videoLoaderId, videoCursorAdapter);

                ListRow row = new ListRow(header, videoCursorAdapter);
                mTitleRowAdapter.add(row);
                row.setId(row_id);
	            // System.out.println("MainFragment / _onLoadFinished / existingAdapter is null  / will initLoader / videoLoaderId = " + videoLoaderId);

                // Start loading the videos from the database for a particular category.
                Bundle args = new Bundle();
                args.putString(VideoContract.VideoEntry.COLUMN_ROW_TITLE, title);

                // init loader for video items
                mLoaderManager.initLoader(videoLoaderId, args, this);
            } else {
                // System.out.println("MainFragment / _onLoadFinished / existingAdapter is not null ");
                ListRow row = new ListRow(header, existingAdapter);
                row.setId(row_id);
                mTitleRowAdapter.add(row);
            }

            //System.out.println("MainFragment / _onLoadFinished / loaderId == TITLE_LOADER / rowsLoadedCount = " + rowsLoadedCount);
            data.moveToNext();
        }
        return row_id;
    }

    //
    // create Option presenter
    //
    void createListRow_option(int _row_id){

        row_id = _row_id;

        // Create a row for this special case with more samples.
        HeaderItem gridHeader = new HeaderItem(getString(R.string.options));
        GridItemPresenter gridPresenter = new GridItemPresenter(this);
        ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(gridPresenter);
        gridRowAdapter.add(getString(R.string.select_category));
        gridRowAdapter.add(getString(R.string.category_grid_view_title));
//                gridRowAdapter.add(getString(R.string.guidedstep_first_title));
//                gridRowAdapter.add(getString(R.string.error_fragment));
        gridRowAdapter.add(getString(R.string.personal_settings));
        ListRow row = new ListRow(gridHeader, gridRowAdapter);

        row_id++;
        row.setId(row_id);
        mTitleRowAdapter.add(row);
    }

    /**
     * runnable for counting down
     */
    private Runnable runCountDown = new Runnable() {
        public void run() {
            // show count down
            TextView messageView = (TextView) alertDlg.findViewById(android.R.id.message);
            count--;
            countStr = act.getString(R.string.play_countdown)+
                                " " + count + " " +
                              act.getString(R.string.play_time_unit);
            countStr = countStr.replaceFirst("[0-9]",String.valueOf(count));
            messageView.setText( act.getString(R.string.play_4_spaces)+ nextLinkTitle +"\n\n" +countStr);

            if(count>0)
                handler.postDelayed(runCountDown,1000);
            else
            {
                // launch next intent
                alertDlg.dismiss();
                cancelYouTubeHandler();
                launchYouTubeIntent();
            }
        }
    };

    int delay100ms = 100;
    /**
     *  launch next YouTube intent
     */
    private void launchYouTubeIntent()
    {
//        if(MainFragment.currLinkId >= MainFragment.getCurrLinksLength())
        //refer: https://developer.android.com/reference/android/view/KeyEvent.html#KEYCODE_DPAD_DOWN_RIGHT

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
                Thread.sleep(delay100ms *10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            BaseInputConnection mInputConnection = new BaseInputConnection(act.findViewById(R.id.main_frame), true);
            mInputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT));
            mInputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT));

            // delay
            try {
                Thread.sleep(delay100ms * 10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // method 1: by intent
            String video_url = getYouTubeLink();
            startYouTubeIntentForResult(video_url);

            // method 2 : by UI
//            mInputConnection = new BaseInputConnection(act.findViewById(R.id.main_frame), true);
//            mInputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER ));
//            mInputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER));

        }
    }

    // start YouTube intent
    private void startYouTubeIntentForResult(String url)
    {
        String idStr = getYoutubeId(url);
        //Intent intent = YouTubeIntents.createPlayVideoIntentWithOptions(act, idStr, true/*fullscreen*/, true/*finishOnEnd*/);
        Intent intent  = YouTubeIntents.createPlayVideoIntent(act, idStr);
        intent.putExtra("force_fullscreen", true);
        intent.putExtra("finish_on_ended", true);
        startActivityForResult(intent, YOUTUBE_LINK_INTENT);
    }

    private void cancelYouTubeHandler()
    {
        if(handler != null) {
            handler.removeCallbacks(runCountDown);
            handler = null;
        }
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
            BaseInputConnection mInputConnection = new BaseInputConnection(act.findViewById(R.id.main_frame), true);

            // point to first item of current row
            for(int i=0;i<dPadSteps;i++)
            {
                System.out.println("MainFragment / DPadAsyncTask / back i = " + i);
                mInputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT));
                mInputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT));
                try {
                    Thread.sleep(delay100ms*2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // for auto play by category only
            if(Pref.isAutoPlayByCategory(act)) {
                // add delay to make sure key event works
                try {
                    Thread.sleep(delay100ms * 2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // TBD: change to other row, problem: item selected is not reset to 1st position
                // point to first row if meets the end of last row
                if (getPlayId() == 1) {
                    for (int i = (mPlayLists.size() - 1); i >= 1; i--) {
                        mInputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP));
                        mInputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_UP));
                        try {
                            Thread.sleep(delay100ms * 2);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    // point to next row
                    BaseInputConnection connection = new BaseInputConnection(act.findViewById(R.id.main_frame), true);
                    connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN));
                    connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN));
                }

                // add delay for viewer
                try {
                    Thread.sleep(delay100ms * 10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            String video_url = getYouTubeLink();
            startYouTubeIntentForResult(video_url);
        }
    }

    private int backSteps;

    // check if Row End
    private boolean isRowEnd()
    {
        boolean isEnd = false;
        System.out.println("isRowEnd / getNextId_auto() = " + getNextId_auto());
        backSteps = 0;

        // for auto play by category only
        if(Pref.isAutoPlayByCategory(act)) {
            for (int i = 0; i < mPlayLists.size(); i++) {
                List<Integer> playlist = mPlayLists.get(i);
                int firstIdOfRow = playlist.get(0);
                if (firstIdOfRow == getNextId_auto()) {
                    isEnd = true;
                    if (getNextId_auto() == 1) {
                        // back steps for last row or 1st row
                        backSteps = mPlayLists.get(mPlayLists.size() - 1).size() - 1;
                    } else if (i != 0) {
                        // back steps for other row
                        backSteps = mPlayLists.get(i - 1).size() - 1;
                    }
                    break;
                }
            }
        }

        // for auto play by list only
        if(Pref.isAutoPlayByList(act)) {
            if (getNextId_auto() == currentRow1stId) {
                backSteps = currentRowSize - 1;
                isEnd = true;
            }
        }

        System.out.println("isRowEnd / isEnd = " + isEnd);
        System.out.println("isRowEnd / backSteps = " + backSteps);

        return isEnd;
    }

    private void updateBackground(String uri) {
        int width = mMetrics.widthPixels;
        int height = mMetrics.heightPixels;

        RequestOptions options = new RequestOptions()
                .centerCrop()
                .error(mDefaultBackground);

        Glide.with(act)
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

//    private void updateRecommendations() {
//        Intent recommendationIntent = new Intent(act, UpdateRecommendationsService.class);
//        act.startService(recommendationIntent);
//    }

    // get default URL
    private String getDefaultUrl(int init_number)
    {
        // in res/values
//        String name = "db_source_id_".concat(String.valueOf(init_number));
//        int res_id = Objects.requireNonNull(act)
//                .getResources().getIdentifier(name,"string",act.getPackageName());
//        return "https://drive.google.com/uc?export=download&id=" +  getString(res_id);

        // in assets
        List<Pair<String, String>> src_links = Source_links.getFileIdList(Objects.requireNonNull(act));
        int index = init_number -1; // starts from 1
//        // note: AND sign expression
//        //  in XML: &amp;
//        //  in Java: &
        return "https://drive.google.com/uc?export=download&id=" + src_links.get(index).getSecond();
    }

    private class UpdateBackgroundTask implements Runnable {

        @Override
        public void run() {
            if (mBackgroundURI != null) {
                updateBackground(mBackgroundURI.toString());
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

    // set next id for Auto
    private int next_id;
    private void setNextId_auto(int id) {
        next_id = id;
    }

    // get next ID for Auto
    private int getNextId_auto() {
        return next_id;
    }

    // Broadcast receiver for receiving status updates from the IntentService
    public class FetchServiceResponseReceiver extends BroadcastReceiver {
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

    // get YouTube link
    private String getYouTubeLink()
    {
        int focusCatNum = Utils.getPref_video_table_id(act);
        String table = VideoContract.VideoEntry.TABLE_NAME.concat(String.valueOf(focusCatNum));
        String columnName = VideoContract.VideoEntry.COLUMN_LINK_URL;
        int pos = getCursorPositionById(getPlayId());
        return getDB_link_data(table,columnName,pos);
    }

    // get YouTube title
    private String getYouTubeTitle()
    {
        int focusCatNum = Utils.getPref_video_table_id(act);
        String table = VideoContract.VideoEntry.TABLE_NAME.concat(String.valueOf(focusCatNum));
        String columnName = VideoContract.VideoEntry.COLUMN_LINK_TITLE;
        int pos = getCursorPositionById(getPlayId());
        return getDB_link_data(table,columnName,pos);
    }

    // get cursor position by ID
    int getCursorPositionById(int id){
        int focusCatNum = Utils.getPref_video_table_id(act);
        String table = VideoContract.VideoEntry.TABLE_NAME.concat(String.valueOf(focusCatNum));

        int pos = 0;
        DbHelper mOpenHelper = new DbHelper(act);
        mOpenHelper.setWriteAheadLoggingEnabled(false);
        SQLiteDatabase sqlDb = mOpenHelper.getReadableDatabase();
        Cursor cursor = sqlDb.query(
                table,
                null,//projection,
                null,//selection,
                null,//selectionArgs,
                null,
                null,
                null//sortOrder
        );

        int index_id = cursor.getColumnIndex("_id");
        for(int position=0;position<cursor.getCount();position++){
            cursor.moveToPosition((int) position);
            if(id == cursor.getInt(index_id)) {
                pos = position;
                break;
            }
        }
        cursor.close();
        sqlDb.close();

        return  pos;
    }

    // get next cursor position ID for Auto play
    int getNextCursorPositionId_auto(int currentPlayId){
        int focusCatNum = Utils.getPref_video_table_id(act);
        String table = VideoContract.VideoEntry.TABLE_NAME.concat(String.valueOf(focusCatNum));

        int cursorPos = 0;
        DbHelper mOpenHelper = new DbHelper(act);
        mOpenHelper.setWriteAheadLoggingEnabled(false);
        SQLiteDatabase sqlDb = mOpenHelper.getReadableDatabase();
        Cursor cursor = sqlDb.query(
                table,
                null,//projection,
                null,//selection,
                null,//selectionArgs,
                null,
                null,
                null//sortOrder
        );

        int index_id = cursor.getColumnIndex("_id");
        for(int position=0;position<cursor.getCount();position++){
            cursor.moveToPosition((int) position);
            if(currentPlayId == cursor.getInt(index_id)) {
                cursorPos = position;
                break;
            }
        }

        // check row position
        for (int i = 0; i < mPlayLists.size(); i++) {
            if (currentPlayId == (int) mPlayLists.get(i).get(0)) {
                currentRowPos = i;
                break;
            }
        }

        // video ID starts with 1
        currentRow1stId = (int) mPlayLists.get(currentRowPos).get(0);
        currentRowSize = mPlayLists.get(currentRowPos).size();
        currentRowLastId = currentRow1stId + currentRowSize - 1;

//        System.out.println("??? cursorPos = " + cursorPos);
//        System.out.println("??? currentPlayId = " + currentPlayId);
//        System.out.println("??? currentRowPos = " + currentRowPos);
//        System.out.println("??? currentRow1stId = " + currentRow1stId);
//        System.out.println("??? currentRowLastId = " + currentRowLastId);
//        System.out.println("??? currentRowSize = " + currentRowSize);

        int nextId = 0;

        // at last video item of category
        try{
            cursor.moveToPosition(cursorPos+1);
            cursor.getInt(index_id);
        } catch(Exception e){
            // set next ID
            if(Pref.isAutoPlayByList(act))
                nextId = currentRow1stId;
            else if (Pref.isAutoPlayByCategory(act))
                nextId = 1;

            cursor.close();
            sqlDb.close();
//            System.out.println("??? nextId (return / exception) = " + nextId);
            return nextId;
        }

        // move cursor to next position
        cursor.moveToPosition(cursorPos+1);
//        System.out.println("??? cursor.getInt(index_id) = " + cursor.getInt(index_id));

        // at row end
        if(cursor.getInt(index_id)  > currentRowLastId  ) {

            // set next ID
            if(Pref.isAutoPlayByList(act))
                nextId = currentRow1stId;
            else if(Pref.isAutoPlayByCategory(act))
                nextId = currentRowLastId + 1;

//            System.out.println("??? nextId 1 = " + nextId);
        }
        else {
            cursor.moveToPosition((int) cursorPos + 1);
            nextId = cursor.getInt(index_id);
//            System.out.println("??? nextId 2 = " + nextId);
        }

//        System.out.println("??? nextId (return)= " + nextId);

        cursor.close();
        sqlDb.close();

        return  nextId;
    }

    // get DB link data
    private String getDB_link_data(String table, String columnName, int pos)
    {
        DbHelper mOpenHelper = new DbHelper(act);
        mOpenHelper.setWriteAheadLoggingEnabled(false);
        SQLiteDatabase sqlDb = mOpenHelper.getReadableDatabase();
        Cursor cursor = sqlDb.query(
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

    private int getTotalLinksCount()
    {
        int focusCatNum = Utils.getPref_video_table_id(act);

        String table = VideoContract.VideoEntry.TABLE_NAME.concat(String.valueOf(focusCatNum));
        DbHelper mOpenHelper = new DbHelper(act);
        mOpenHelper.setWriteAheadLoggingEnabled(false);
        SQLiteDatabase sqlDb = mOpenHelper.getReadableDatabase();

        Cursor cursor = sqlDb.query(
                table,
                null,//projection,
                null,//selection,
                null,//selectionArgs,
                null,
                null,
                null//sortOrder
        );

        int totalLinksCount = cursor.getCount();
        cursor.close();

        return totalLinksCount;
    }


    // get duplicated times of same category name
    // i.e.
    // 1. JSON file is the same
    // 2. category names in DB are different
    public static int getCategoryNameDuplicatedTimes(String categoryName){
        int size = mCategoryNames.size();
        int duplicatedTimes = 0;

        for(int i=0;i<size;i++) {
            if (mCategoryNames.get(i).contains(categoryName))
                duplicatedTimes++;
        }
        return duplicatedTimes;
    }

    static int rowsCount;
    // set playlists count
    public void setPlaylistsCount(int count){
        rowsCount = count;
    }

    // get playlists count
    public static int getPlaylistsCount(){
        return rowsCount;
    }

    // get focus position of category row
    int getFocusItemPosition_categoryRow(){
        // get current video* tables
        int prefVideoTableId = Utils.getPref_video_table_id(MainFragment.this.act);
        ContentResolver contentResolver = act.getApplicationContext().getContentResolver();
        String[] projection = new String[]{"_id", "category_name", "video_table_id"};
        String selection = null;
        String[] selectionArgs = null;
        String sortOrder = null;
        Cursor query = contentResolver.query(VideoContract.CategoryEntry.CONTENT_URI,projection,selection,selectionArgs,sortOrder);

        // get new position by video table ID
        int new_position=0;
        if (query.moveToFirst()) {
            do {
                String columnStr = VideoContract.CategoryEntry.COLUMN_VIDEO_TABLE_ID;
                int index = query.getColumnIndex(columnStr);
                int pointedVideoTableId = query.getInt(index);
                if(pointedVideoTableId == prefVideoTableId)
                    break;
                else
                    new_position++;

            } while (query.moveToNext());
        }
        query.close();
        return new_position;
    }

    // differentiate category row with other rows
    boolean isCategoryRow(String catName){
        for(int i=0;i<mCategoryNames.size();i++) {
            if(catName.equalsIgnoreCase(mCategoryNames.get(i)))
                return true;
        }
        return false;
    }

    // open video item
    void openVideoItem(Object item){
        Video video = (Video) item;
        System.out.println("MainFragment / _openVideoItem / item = "+ item );

        // for auto play by list only
        if(!Pref.isAutoPlayByCategory(act)) {
            // check row position
            for (int i = 0; i < mPlayLists.size(); i++) {
                if (video.id >= (int) mPlayLists.get(i).get(0))
                    currentRowPos = i;
            }
        }

        // video ID starts with 1
        currentRow1stId = (int) mPlayLists.get(currentRowPos).get(0);
        currentRowSize = mPlayLists.get(currentRowPos).size();
        currentRowLastId = currentRow1stId + currentRowSize - 1;

        String urlStr = ((Video) item).videoUrl;

        String path;
        // YouTube or HTML
        if(!urlStr.contains("playlist") && ( urlStr.contains("youtube") || urlStr.contains("youtu.be") ))
            path = "https://img.youtube.com/vi/"+getYoutubeId(urlStr)+"/0.jpg";
        else
            path = urlStr;

        System.out.println("MainFragment / _openVideoItem / path= "+ path);
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
                    System.out.println("MainFragment / _openVideoItem / responseCode  OK = " + responseCode);
                }
                catch (IOException e)
                {
                    System.out.println("MainFragment / _openVideoItem / responseCode NG = "+ responseCode);
                    e.printStackTrace();
                    return;
                }
                urlConnection.disconnect();

                /**
                 *  normal response: launch VideoDetailsActivity
                 */
                // YouTube  or video or HTML
                if(responseCode == 200) {
                    // play YouTube
                    if(urlStr.contains("youtube") || urlStr.contains("youtu.be"))
                    {
                        // auto play
                        if (Pref.isAutoPlayByList(act) ||
                            Pref.isAutoPlayByCategory(act)) {
                            setPlayId((int) ((Video) (item)).id);
                            startYouTubeIntentForResult(((Video) item).videoUrl);
                        } else {
                            // manual play
                            act.runOnUiThread(new Runnable() {
                                public void run() {
                                    // for VideoDetailsActivity
//                                            Intent intent = new Intent(act, VideoDetailsActivity.class);
//                                            intent.putExtra(VideoDetailsActivity.VIDEO, video);
//                                            if (urlStr.contains("youtube") || urlStr.contains("youtu.be")) {
//                                                // play YouTube
//                                                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
//                                                        act,
//                                                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
//                                                        VideoDetailsActivity.SHARED_ELEMENT_NAME).toBundle();
//                                                        startActivityForResult(intent, VIDEO_DETAILS_INTENT, bundle);
//                                            }

                                    if(((Video) item).videoUrl.contains("playlist"))
                                    {
                                        String playListIdStr = Utils.getYoutubePlaylistId(((Video) item).videoUrl);
                                        Intent intent = YouTubeIntents.createPlayPlaylistIntent(act, playListIdStr);
                                        intent.putExtra("force_fullscreen", true);
                                        intent.putExtra("finish_on_ended", true);
                                        startActivity(intent);
                                    }
                                    else {
                                        // for open directly
                                        setPlayId((int) ((Video) (item)).id);
                                        String idStr = getYoutubeId(((Video) item).videoUrl);
                                        Intent intent = YouTubeIntents.createPlayVideoIntent(act, idStr);
                                        intent.putExtra("force_fullscreen", true);
                                        intent.putExtra("finish_on_ended", true);
                                        startActivity(intent);
                                    }

                                    // for testing NewPipe
//                                            Uri linkUri = Uri.parse(((Video) item).videoUrl);
//                                            Intent appIntent = new Intent(Intent.ACTION_VIEW, linkUri);

                                    // case: w/o chooser
//                                            startActivity(appIntent);

                                    // case: w/ chooser
//                                            String title = "Select an APP";
//                                            Intent chooser = Intent.createChooser(appIntent, title);
//                                            if (appIntent.resolveActivity(act.getPackageManager()) != null) {
//                                                startActivity(chooser);
//                                            }

                                }
                            });
                        }
                    }
                    else {
                        // play video
                        // https://drive.google.com/uc?export=view&id=ID
                        if(urlStr.contains("https://drive.google.com/uc?export=view") ||
                                urlStr.contains("https://storage.googleapis.com/android-tv") ){
                            Intent intent = new Intent(act, PlaybackActivity.class);
                            intent.putExtra(VideoDetailsActivity.VIDEO, ((Video) item));
                            startActivity(intent);
                        }
                        // play HTML
                        else {
                            String link = ((Video) item).videoUrl;
                            Uri uriStr = Uri.parse(link);
                            Intent intent = new Intent(Intent.ACTION_VIEW, uriStr);
                            startActivity(intent);
                        }
                    }
                } else {
                    /*
                     *  show connection error toast
                     */
                    act.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(act, getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();

    }

    // row length class
    public static class RowLength {
        public long start_id;
        public int row_length;
        RowLength(long start_id, int row_length) {
            this.start_id = start_id;
            this.row_length = row_length;
        }
    }

    // get row length by video id
    public static RowLength getRowLengthByVideoId(long videoId){
        // set position info text view : get link id & current row length
        // Sorting pair : original row length pair below could be not ordered

        RowLength rowLen1, rowLen2;
        List<RowLength> rowLenList = rowLengthList;

        int rows_count = rowLenList.size();
        long link_id = 0;
        int current_row_len = 0;

        long start = 0,start1,start2;
        long end = 0,end1,end2;
        for(int i=0;i<rows_count;i++) {
            rowLen1 = rowLenList.get(i);
            start1 = rowLen1.start_id;
            end1 = start1 + rowLen1.row_length -1;

            for(int j=0;j<rows_count;j++) {

                rowLen2 = rowLenList.get(j);
                start2 = rowLen2.start_id;
                end2 = start2 + rowLen2.row_length -1;

                if(start2 < start1) {
                    start = start2;
                    end = end2;
                    current_row_len = rowLen2.row_length;
                } else {
                    start = start1;
                    end = end1;
                    current_row_len = rowLen1.row_length;
                }
            }

            if ((start <= videoId) && (videoId <= end)) {
                link_id = videoId - start + 1;
                break;
            }
        }
        return new RowLength(link_id,current_row_len);
    }

}