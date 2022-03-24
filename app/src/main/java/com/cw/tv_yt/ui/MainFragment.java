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

    // workaround for keeping 1. cursor position 2. correct rows after Refresh
    private int rowsLoadedCount;
    private FetchServiceResponseReceiver responseReceiver;
    private LocalBroadcastManager localBroadcastMgr;

    FragmentActivity act;

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

        act = getActivity();

        System.out.println("MainFragment / _onActivityCreated");
        // Prepare the manager that maintains the same background image between activities.
        prepareBackgroundManager();

        setupUIElements();

        setupEventListeners();
        prepareEntranceTransition();

        // Map title results from the database to ListRow objects.
        // This Adapter is used to render the MainFragment sidebar labels.

        ListRowPresenter listRowPresenter = new ListRowPresenter();
        listRowPresenter.setRowHeight(400);
        mTitleRowAdapter = new ArrayObjectAdapter(listRowPresenter);
        setAdapter(mTitleRowAdapter);

        //todo temporary mark
//        updateRecommendations();

        rowsLoadedCount = 0;
	    mPages = new ArrayList<>();

	    // list for Show row number - link number
        links_count_of_row = new ArrayList<>();
        start_number_of_row = new ArrayList<>();
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

        cancelYouTubeHandler(); //todo Why not stop when power off
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
                return new IconHeaderItemPresenter(rowsLoadedCount);
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
                System.out.println("---------- onItemSelected / video");
                mBackgroundURI = Uri.parse(((Video) item).bgImageUrl);
                startBackgroundTimer();
            }
//            else if (item instanceof String) {
//                System.out.println("---------- onItemSelected / category");
//                // category selection header
//                for(int i=0;i<mCategoryNames.size();i++)
//                {
//                    if(item.toString().equalsIgnoreCase(mCategoryNames.get(i)))
//                    {
//                        currentNavPosition = i;
//                        System.out.println("---------- current navigation position = " + currentNavPosition);
//                    }
//                }
//            }
        }
    }

    int currentRowPos;
    int currentRow1stId;
    int currentRowSize;
    int currentRowLastId;
    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof Video) {
                Video video = (Video) item;
                System.out.println("MainFragment / onItemClicked / id = "+ video.id );

                // for auto play by list only
                if(!Pref.isAutoPlayByCategory(act)) {
                    // check row position
                    for (int i = 0; i < mPages.size(); i++) {
                        if (video.id >= (int) mPages.get(i).get(0))
                            currentRowPos = i;
                    }
                }

                // video ID starts with 1
                currentRow1stId = (int)mPages.get(currentRowPos).get(0);
                currentRowSize = mPages.get(currentRowPos).size();
                currentRowLastId = currentRow1stId + currentRowSize - 1; //todo ID count changed now

                String urlStr = ((Video) item).videoUrl;

                String path;
                // YouTube or HTML
                if(!urlStr.contains("playlist") && ( urlStr.contains("youtube") || urlStr.contains("youtu.be") ))
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
                        // YouTube  or video or HTML
                        if(responseCode == 200) {
                            // play YouTube
                            if(urlStr.contains("youtube") || urlStr.contains("youtu.be"))
                            {
                                // auto play
                                if (Pref.isAutoPlayByList(act) ||
                                        Pref.isAutoPlayByCategory(act)) {
                                    setPlayId((int) ((Video) (item)).id);
                                    startYouTubeIntent(((Video) item).videoUrl);
                                    setNewId(getPlayId() + 1);
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

            } else if (item instanceof String) {
                System.out.println("MainFragment / onItemClicked / item = "+ item);
                if (((String) item).contains(getString(R.string.select_category))) {

                    localBroadcastMgr.unregisterReceiver(responseReceiver);
                    responseReceiver = null;

                    Intent intent = new Intent(act, SelectCategoryActivity.class);
                    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(act).toBundle();
                    startActivity(intent, bundle);

                } else if (((String) item).contains(getString(R.string.category_grid_view_title))) {
                    Intent intent = new Intent(act, BrowseCategoryActivity.class);
                    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(act).toBundle();
                    startActivity(intent, bundle);
//                } else if (((String) item).contains(getString(R.string.guidedstep_first_title))) {
//                    Intent intent = new Intent(act, GuidedStepActivity.class);
//                    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(act).toBundle();
//                    startActivity(intent, bundle);
//                } else if (((String) item).contains(getString(R.string.error_fragment))) {
//                    BrowseErrorFragment errorFragment = new BrowseErrorFragment();
//                    getFragmentManager().beginTransaction().replace(R.id.main_frame, errorFragment).addToBackStack(null).commit();
                } else if(((String) item).contains(getString(R.string.personal_settings))) {
                    Intent intent = new Intent(act, SettingsActivity.class);
                    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(act).toBundle();
                    startActivity(intent, bundle);
//                } else {
                    //Toast.makeText(act, ((String) item), Toast.LENGTH_SHORT).show();
                } else {
                    if(isLongClicked) {
                        System.out.println("--- is Long clicked");
                        isLongClicked = false;
                        return;
                    } else {
                        String categoryName =  (String) item;//mCategoryNames.get(i-1);
                        // After delay, start switch DB
                        new Handler().postDelayed(new Runnable() {
                            public void run() {
                                switchDB(categoryName);
                            }
                        }, 100);
                    }
                }

            }
        }
    }

    boolean isLongClicked;
    // for item long clicked
    class CategoryItemPresenter extends GridItemPresenter{

        public CategoryItemPresenter(MainFragment mainFragment, List<String> category_names) {
            super(mainFragment, category_names);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object item) {
            viewHolder.view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    System.out.println("CategoryItemPresenter / onLongClick / category item = " + item);
                    isLongClicked = true;
                    Utils.confirmDeleteCategory(act,mCategoryNames,(String)item);
                    return false;
                }
            });
            super.onBindViewHolder(viewHolder, item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == YOUTUBE_LINK_INTENT) {
            count = Define.DEFAULT_COUNT_DOWN_TIME_TO_PLAY_NEXT; // countdown time to play next
            builder = new AlertDialog.Builder(getContext());

            setPlayId(getNewId());

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

                            // prepare next Id
                            setNewId(getPlayId() + 1);
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

    private List<List> mPages;
    public static List<Integer> links_count_of_row;
    public static List<Integer> start_number_of_row;

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
//        System.out.println("MainFragment / _onLoadFinished");
//        System.out.println("MainFragment / _onLoadFinished /  start rowsLoadedCount = " + rowsLoadedCount);
//        System.out.println("MainFragment / _onLoadFinished /  mVideoCursorAdapters.size() = " + mVideoCursorAdapters.size());

        // return when load is OK
        if( (rowsLoadedCount!=0 ) && (rowsLoadedCount >= mVideoCursorAdapters.size()) ) {
            System.out.println("MainFragment / _onLoadFinished / return");//??? not needed this?
            return;
        }

        if (data != null && data.moveToFirst()) {
            final int loaderId = loader.getId();

//            if (loaderId == CATEGORY_LOADER)
//	            System.out.println("MainFragment / _onLoadFinished / loaderId = CATEGORY_LOADER");
//            else if(loaderId == TITLE_LOADER)
//                System.out.println("MainFragment / _onLoadFinished / loaderId = TITLE_LOADER");
//            else
//                System.out.println("MainFragment / _onLoadFinished / loaderId (video) = " + loaderId);

            if (loaderId == CATEGORY_LOADER) {
                mCategoryNames = new ArrayList<>();
                // Iterate through each category entry and add it to the ArrayAdapter.
                while (!data.isAfterLast()) {
                    int categoryIndex = data.getColumnIndex(VideoContract.CategoryEntry.COLUMN_CATEGORY_NAME);
                    String category_name = data.getString(categoryIndex);
                    System.out.println("MainFragment / _onLoadFinished / category_name = " + category_name);
                    mCategoryNames.add(category_name);

                    int video_table_id_index = data.getColumnIndex(VideoContract.CategoryEntry.COLUMN_VIDEO_TABLE_ID);
                    int video_table_id = data.getInt(video_table_id_index);
                    System.out.println(" @ MainFragment / _onLoadFinished / video_table_id = " + video_table_id);

                    data.moveToNext();
                }

                mLoaderManager.initLoader(TITLE_LOADER, null, this);

            } else if (loaderId == TITLE_LOADER) {

                // Create a row for category selections at top
                String categoryName = Utils.getPref_category_name(act);
                String currCatMessage;

                if(categoryName.equalsIgnoreCase("no name")){// initial
                    // get first available category name
                    categoryName = mCategoryNames.get(INIT_CATEGORY_NUMBER - 1);
                    Utils.setPref_category_name(getActivity(),categoryName);
                }

                currCatMessage = act.getResources().
                        getString(R.string.current_category_title).
                        concat(" : ").
                        concat(categoryName);

                HeaderItem gridHeaderCategory = new HeaderItem(currCatMessage);

                //todo ??? how to change
//                GridItemPresenter gridPresenterCategory = new GridItemPresenter(this,mCategoryNames);
                GridItemPresenter gridPresenterCategory = new CategoryItemPresenter(this,mCategoryNames);
                ArrayObjectAdapter gridRowAdapterCategory = new ArrayObjectAdapter(gridPresenterCategory);

                // show category name
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
//                    System.out.println("MainFragment / _onLoadFinished / title = " + title);

                    // Create header for this category.
                    HeaderItem header = new HeaderItem(title);

                    int videoLoaderId = title.hashCode(); // Create unique int from title.
                    CursorObjectAdapter existingAdapter = mVideoCursorAdapters.get(videoLoaderId);
                    row_id++;
                    if (existingAdapter == null) {

                        // Map video results from the database to Video objects.
                        CursorObjectAdapter videoCursorAdapter = new CursorObjectAdapter(new CardPresenter(act));
                        videoCursorAdapter.setMapper(new VideoCursorMapper());
                        mVideoCursorAdapters.put(videoLoaderId, videoCursorAdapter);

                        ListRow row = new ListRow(header, videoCursorAdapter);
                        mTitleRowAdapter.add(row);
                        row.setId(row_id);
//	                    System.out.println("MainFragment / _onLoadFinished / existingAdapter is null  / will initLoader / videoLoaderId = " + videoLoaderId);

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

                startEntranceTransition(); // TODO: Move startEntranceTransition to after all

                // set focus category item position ??? why position error for 1st time launch (run App)
                int cate_number = Utils.getPref_video_table_id(MainFragment.this.act);
                setSelectedPosition(0);
                setSelectedPosition(0, true, new ListRowPresenter.SelectItemViewHolderTask(cate_number-1));

                /*
                 *  end of loading category
                 */
//                System.out.println("MainFragment / _onLoadFinished / -----------------------------------------");
//                System.out.println("MainFragment / _onLoadFinished / end of onLoadFinished category");
//                System.out.println("MainFragment / _onLoadFinished / -----------------------------------------");

            } else {
                // The CursorAdapter contains a Cursor pointing to all videos.
                mVideoCursorAdapters.get(loaderId).changeCursor(data);

                int columnIndex = data.getColumnIndex(VideoContract.VideoEntry._ID);
                int video_id = data.getInt(columnIndex);
//                System.out.println("MainFragment / _onLoadFinished / 1st video_id of row = " + video_id);
                int sizeOfRowLinks = data.getCount();
//                System.out.println("MainFragment / _onLoadFinished / sizeOfLinks= " + sizeOfRowLinks);

                // start number of a row
                start_number_of_row.add(video_id);

                // links count of a row
                links_count_of_row.add(sizeOfRowLinks);

                List<Integer> page = new ArrayList<>();
                for(int i=video_id;i<(video_id+sizeOfRowLinks);i++)
                    page.add(i);

                mPages.add(page);

                // one row added
                rowsLoadedCount++;
//                System.out.println("MainFragment / _onLoadFinished / rowsLoadedCount = "+ rowsLoadedCount);

                /**
                 *  end of loading video
                 * */
                if(rowsLoadedCount == mVideoCursorAdapters.size() )
                {
                    System.out.println("MainFragment / _onLoadFinished / -------------------------------------");
                    System.out.println("MainFragment / _onLoadFinished / end of onLoadFinished video");
                    System.out.println("MainFragment / _onLoadFinished / -------------------------------------");
                }
            }
        } else {
            /***
             *  call fetch service to load or update data base
             */

            // show toast
            Toast.makeText(act,getString(R.string.database_update),Toast.LENGTH_LONG).show();

            // Start an Intent to fetch the categories
            if ((loader.getId() == CATEGORY_LOADER) && (mCategoryNames == null)) {
//                Utils.setPref_focus_category_number(act, INIT_NUMBER);

                System.out.println("MainFragment / onLoadFinished / start Fetch category service =================================");

                // data base is not created yet, call service for the first time
                Intent serviceIntent = new Intent(act, FetchCategoryService.class);
                int linkSrcNum = Utils.getPref_link_source_number(act);
                serviceIntent.putExtra("FetchUrl", getDefaultUrl(linkSrcNum) );
                act.startService(serviceIntent);
            }
            // Start an Intent to fetch the videos
            else if ((loader.getId() == TITLE_LOADER) && (rowsLoadedCount == 0)) {
//                System.out.println("MainFragment / onLoadFinished / start Fetch video service =================================");

                //todo test? avoid endless loop due to empty category selection
//                Utils.setPref_focus_category_number(act,INIT_NUMBER);

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
        }
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

                // prepare next Id
                setNewId(getPlayId() + 1);
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
            startYouTubeIntent(video_url);

            // method 2 : by UI
//            mInputConnection = new BaseInputConnection(act.findViewById(R.id.main_frame), true);
//            mInputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER ));
//            mInputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER));

        }
    }

    private void startYouTubeIntent(String url)
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
                    for (int i = (mPages.size() - 1); i >= 1; i--) {
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
            startYouTubeIntent(video_url);
        }
    }

    private int backSteps;

    private boolean isRowEnd()
    {
        boolean isEnd = false;
        System.out.println("isRowEnd / getNewId() = " + getNewId());
        backSteps = 0;

        // for auto play by category only
        if(Pref.isAutoPlayByCategory(act)) {
            for (int i = 0; i < mPages.size(); i++) {
                List<Integer> page = mPages.get(i);
                int firstIdOfRow = page.get(0);
                if (firstIdOfRow == getNewId()) {
                    isEnd = true;
                    if (getNewId() == 1) {
                        // back steps for last row or 1st row
                        backSteps = mPages.get(mPages.size() - 1).size() - 1;
                    } else if (i != 0) {
                        // back steps for other row
                        backSteps = mPages.get(i - 1).size() - 1;
                    }
                    break;
                }
            }
        }

        // for auto play by list only
        if(!Pref.isAutoPlayByCategory(act)) {
            if (getNewId() == currentRow1stId) {
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

    // switch Data base
    private void switchDB(String categoryName)
    {
        System.out.println(" MainFragment / _switchDB / categoryName = " + categoryName);

        try {
            Utils.setPref_category_name(getContext(), categoryName );
            mLoaderManager.destroyLoader(TITLE_LOADER);

            // start new MainActivity to renew video provider
            act.finish();
            Intent new_intent = new Intent(act, MainActivity.class);
            new_intent.addFlags(FLAG_ACTIVITY_CLEAR_TASK);
            new_intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            startActivity(new_intent);
        }
        catch (Exception e)
        {
            e.printStackTrace();
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
        new_id = id;

        if(Pref.isAutoPlayByCategory(act)) {
            // for auto play by category
            if(id > getTotalLinksCount())
                new_id = 1;
        } else {
            // for auto play by list
            if (id > currentRowLastId)
                new_id = currentRow1stId;
        }

    }

    private int getNewId()
    {
        return new_id;
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

    private String getYouTubeLink()
    {
        int focusCatNum = Utils.getPref_video_table_id(act);
        String table = VideoContract.VideoEntry.TABLE_NAME.concat(String.valueOf(focusCatNum));
        String columnName = VideoContract.VideoEntry.COLUMN_LINK_URL;
        int pos = getPlayId()-1;
        System.out.println("MainFragment / _getYouTubeLink / pos = " + pos);

        return getDB_data(table,columnName,pos);
    }

    private String getYouTubeTitle()
    {
        int focusCatNum = Utils.getPref_video_table_id(act);
        String table = VideoContract.VideoEntry.TABLE_NAME.concat(String.valueOf(focusCatNum));
        String columnName = VideoContract.VideoEntry.COLUMN_LINK_TITLE;
        int pos = getPlayId()-1;
        System.out.println("MainFragment / _getYouTubeTitle / pos = " + pos);

        return getDB_data(table,columnName,pos);
    }

    private String getDB_data(String table,String columnName,int pos)
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

}