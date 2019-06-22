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
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cw.tv_yt.R;
import com.cw.tv_yt.Utils;
import com.cw.tv_yt.data_yt.FetchCategoryService_yt;
import com.cw.tv_yt.data_yt.FetchVideoService_yt;
import com.cw.tv_yt.data_yt.VideoContract_yt;
import com.cw.tv_yt.data_yt.VideoDbHelper_yt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.leanback.app.VerticalGridSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.CursorObjectAdapter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.VerticalGridPresenter;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

/*
 * VerticalGridFragment shows a grid of videos that can be scrolled vertically.
 */
public class SelectLinksFragment extends VerticalGridSupportFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "Select links";
    private static final int NUM_COLUMNS = 5;
    private static final int NUM_MAX = 10;
    private static final int HEIGHT = 200;


    private static class Adapter extends ArrayObjectAdapter {
        public Adapter(StringPresenter presenter) {
            super(presenter);
        }

        public void callNotifyChanged() {
            super.notifyChanged();
        }
    }

    private Adapter mAdapter;
    FetchServiceResponseReceiver responseReceiver;
    private int countCategory;
    List<String> mCategoryNames = new ArrayList<>();

    // Maps a Loader Id to its CursorObjectAdapter.
    private Map<Integer, CursorObjectAdapter> mCategoryCursorAdapters;
    private LoaderManager mLoaderManager;
    private static final int CATEGORY_LOADER = 246; // Unique ID for Category Loader.

    @Override
    public void onAttach(Context context) {

        super.onAttach(context);

//        CategoryDbHelper mOpenHelper = new CategoryDbHelper(getActivity());
//        SQLiteDatabase sqlDb = mOpenHelper.getWritableDatabase();

        // Create a list to contain all the CursorObjectAdapters.
        // Each adapter is used to render a specific row of videos in the MainFragment.
        mCategoryCursorAdapters = new HashMap<>();

        // Start loading the categories from the database.
        mLoaderManager = LoaderManager.getInstance(this);
        mLoaderManager.initLoader(CATEGORY_LOADER, null, this);

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        System.out.println("SelectLinksFragment / _onCreate");

        super.onCreate(savedInstanceState);

        mAdapter = new Adapter(new StringPresenter());
        setAdapter(mAdapter);

        setTitle(getString(R.string.select_links_title));

        if (savedInstanceState == null) {
            prepareEntranceTransition();
        }

        setupFragment();

        // get categories count
        VideoDbHelper_yt mOpenHelper = new VideoDbHelper_yt(getActivity());
        SQLiteDatabase sqlDb = mOpenHelper.getReadableDatabase();

        String SQL_GET_ALL_TABLES = "SELECT * FROM sqlite_master WHERE name like 'video%'";
        Cursor cursor = sqlDb.rawQuery(SQL_GET_ALL_TABLES, null);
        countCategory = cursor.getCount();
        cursor.close();
        sqlDb.close();

        System.out.println("SelectLinksFragment / _onCreate / category tables count = " + countCategory);
    }


    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {

//        if (id == CATEGORY_LOADER)
        {
            System.out.println("SelectLinksFragment / _onCreateLoader / id == CATEGORY_LOADER / CategoryContract.CategoryEntry.CONTENT_URI =" + VideoContract_yt.CategoryEntry.CONTENT_URI);
//            System.out.println("MainFragment / _onCreateLoader / DISTINCT VideoContract.VideoEntry.COLUMN_TITLE = " + VideoContract_yt.VideoEntry.COLUMN_TITLE);

            return new CursorLoader(
                    getContext(),
//                    CategoryContract.CategoryEntry.CONTENT_URI, // Table to query
                    VideoContract_yt.CategoryEntry.CONTENT_URI, // Table to query
//                    new String[]{"DISTINCT " + CategoryContract.CategoryEntry.COLUMN_CATEGORY_NAME},
                    new String[]{"DISTINCT " + VideoContract_yt.CategoryEntry.COLUMN_CATEGORY_NAME},
                    // Only categories
                    null, // No selection clause
                    null, // No selection arguments
                    null  // Default sort order
            );
        }
//        else
//            return null;
    }


    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {

        System.out.println("SelectLinksFragment / _onLoadFinished" );

//        if(mVideoCursorAdapters != null)
//            System.out.println("MainFragment / _onLoadFinished / mVideoCursorAdapters.size() = " + mVideoCursorAdapters.size());

        // return when load is OK
//        if( (rowsLoadedCount!=0 ) && (rowsLoadedCount >= mVideoCursorAdapters.size()) ) {
//            return;
//        }

        if (data != null && data.moveToFirst()) {
            final int loaderId = loader.getId();
//            System.out.println("MainFragment / _onLoadFinished / loaderId = " + loaderId);

            if (loaderId == CATEGORY_LOADER) {
//                System.out.println("MainFragment / _onLoadFinished / loaderId == TITLE_LOADER");

                // clear for not adding duplicate rows
//                if(rowsLoadedCount != mVideoCursorAdapters.size())
//                {
//                    //System.out.println("MainFragment / _onLoadFinished /  mTitleRowAdapter.clear()");
//                    // Every time we have to re-get the category loader, we must re-create the sidebar.
//                    mTitleRowAdapter.clear();
//                }

                // Iterate through each category entry and add it to the ArrayAdapter.
                while (!data.isAfterLast()) {

                    int categoryIndex = data.getColumnIndex(VideoContract_yt.CategoryEntry.COLUMN_CATEGORY_NAME);
                    String category_name = data.getString(categoryIndex);
                    System.out.println("SelectLinksFragment / _onLoadFinished / category_name = " + category_name);
                    mCategoryNames.add(category_name);
                    // Create header for this category.
//                    HeaderItem header = new HeaderItem(title);
//                    System.out.println("MainFragment / _onLoadFinished / title = " + title);

//                    int videoLoaderId = title.hashCode(); // Create unique int from title.
//                    CursorObjectAdapter existingAdapter = mVideoCursorAdapters.get(videoLoaderId);
//                    if (existingAdapter == null) {
//
//                        // Map video results from the database to Video objects.
//                        CursorObjectAdapter videoCursorAdapter = new CursorObjectAdapter(new CardPresenter());
//                        videoCursorAdapter.setMapper(new VideoCursorMapper());
//                        mVideoCursorAdapters.put(videoLoaderId, videoCursorAdapter);
//
//                        ListRow row = new ListRow(header, videoCursorAdapter);
//                        mTitleRowAdapter.add(row);
//
//                        System.out.println("MainFragment / _onLoadFinished / existingAdapter is null  / will initLoader / videoLoaderId = " + videoLoaderId);
//
//                        // Start loading the videos from the database for a particular category.
//                        Bundle args = new Bundle();
//                        args.putString(VideoContract_yt.VideoEntry.COLUMN_TITLE, title);
//                        mLoaderManager.initLoader(videoLoaderId, args, this);
//                    } else {
//                        //System.out.println("MainFragment / _onLoadFinished / existingAdapter is not null ");
//                        ListRow row = new ListRow(header, existingAdapter);
//                        mTitleRowAdapter.add(row);
//                    }

                    //System.out.println("MainFragment / _onLoadFinished / loaderId == TITLE_LOADER / rowsLoadedCount = " + rowsLoadedCount);
                    data.moveToNext();
                }

                // Create a row for this special case with more samples.
//                HeaderItem gridHeader = new HeaderItem(getString(R.string.more_samples));
//                GridItemPresenter gridPresenter = new GridItemPresenter(this);
//                ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(gridPresenter);
//                gridRowAdapter.add(getString(R.string.select_links));
//                gridRowAdapter.add(getString(R.string.grid_view));
//                gridRowAdapter.add(getString(R.string.guidedstep_first_title));
//                gridRowAdapter.add(getString(R.string.error_fragment));
//                gridRowAdapter.add(getString(R.string.personal_settings));
//                ListRow row = new ListRow(gridHeader, gridRowAdapter);
//                mTitleRowAdapter.add(row);

                startEntranceTransition(); // TODO: Move startEntranceTransition to after all
                // cursors have loaded.

            } else {
//                System.out.println("MainFragment / _onLoadFinished / loaderId != TITLE_LOADER");
//                System.out.println("MainFragment / _onLoadFinished / loaderId = " + loaderId);
                // The CursorAdapter contains a Cursor pointing to all videos.
//                mVideoCursorAdapters.get(loaderId).changeCursor(data);
//
//                // one row added
//                rowsLoadedCount++;
            }
        } else {
//            System.out.println("MainFragment / _onLoadFinished / will do FetchVideoService / rowsLoadedCount = " + rowsLoadedCount);
            // Start an Intent to fetch the videos.

            // data base is not created yet, call service for the first time
//            if(rowsLoadedCount == 0)
            {
                Intent serviceIntent = new Intent(getActivity(), FetchCategoryService_yt.class);
                int initNumber = 1;
                Utils.setPref_focus_category_number(getActivity(),initNumber);
//
//                String categoryName = Utils.getPref_category_name(getActivity(),initNumber);
//                System.out.println("MainFragment / _onLoadFinished / categoryName = " + categoryName);
//
//                // todo use catalog_url_1 to be default URL
                String pre_str = "catalog_url_";
                int id = getActivity().getResources().getIdentifier(pre_str.concat(String.valueOf(initNumber)),
                        "string",
                        getActivity().getPackageName());
                String default_url = getString(id);
//
//                // when new installation
//                if( categoryName.equalsIgnoreCase(String.valueOf(initNumber))) {
//
//                    // receiver for fetch category service
//                    IntentFilter statusIntentFilter = new IntentFilter(FetchCategoryService_yt.Constants.BROADCAST_ACTION);
//                    responseReceiver = new FetchServiceResponseReceiver();
//
//                    // Registers the FetchCategoryResponseReceiver and its intent filters
//                    LocalBroadcastManager.getInstance(getActivity()).registerReceiver(responseReceiver, statusIntentFilter );
//
//                    // start new fetch category service
//                    serviceIntent = new Intent(getActivity(), FetchCategoryService_yt.class);
//                    serviceIntent.putExtra("FetchCategoryIndex", initNumber);
//                    serviceIntent.putExtra("FetchCategoryUrl", default_url);
//                    getActivity().startService(serviceIntent);
//
//                }
//                else
//                {
                System.out.println("SelectLinksFragment / onLoadFinished / start service =================================");
                serviceIntent.putExtra("FetchUrl", default_url);
                getActivity().startService(serviceIntent);

//                String categoryName = Utils.getPref_category_name(getActivity(),focusNumber);
//                }
            }
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {

    }

    // setup fragment
    private void setupFragment() {
        VerticalGridPresenter gridPresenter = new VerticalGridPresenter();
        gridPresenter.setNumberOfColumns(NUM_COLUMNS);
        setGridPresenter(gridPresenter);

        // After 500ms, start the animation to transition the cards into view.
        new Handler().postDelayed(new Runnable() {
            public void run() {
                loadData();
                setSelectedPosition(Utils.getPref_focus_category_number(getActivity())-1);
                startEntranceTransition();
            }
        }, 500);

        setOnItemViewSelectedListener(new OnItemViewSelectedListener() {
            @Override
            public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                       RowPresenter.ViewHolder rowViewHolder, Row row) {
                Log.i(TAG, "onItemSelected: " + item + " row " + row);
            }
        });

        setOnItemViewClickedListener(new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                      RowPresenter.ViewHolder rowViewHolder, Row row) {
                Log.i(TAG, "onItemClicked: item = " + item + " row = " + row );

                mAdapter.callNotifyChanged();

                // get clicked position
//                int clickedPos = 0;
//                for(int i = 1; i<= countCategory; i++) {
//
//                    String categoryName = Utils.getPref_category_name(getActivity(), i);
//                    System.out.println("------------------categoryName = " + categoryName);
//                    System.out.println("------------------i = " + i);
//                    if (categoryName.equalsIgnoreCase(item.toString())) {
//                        clickedPos = i;
//                        System.out.println("------------------clickedPos = " + clickedPos);
//                    }
//                }


                int clickedPos = 0;
                for(int i = 1; i<= countCategory; i++) {

//                    String categoryName = ;
//                    System.out.println("------------------categoryName = " + categoryName);
//                    System.out.println("------------------i = " + i);
                    if (mCategoryNames.get(i-1).equalsIgnoreCase(item.toString()))
                    {
                        clickedPos = i;
                        System.out.println("------------------clickedPos = " + clickedPos);
                    }
                }

//
//                int res_id = getResourceIdentifier(String.valueOf(clickedPos));
//                if(res_id == 0)
//                    res_id = getResourceIdentifier(item.toString());
//
//                // set focus category number
                Utils.setPref_focus_category_number(getActivity(),clickedPos);
//
//                // receiver for fetch video service
//                IntentFilter statusIntentFilter = new IntentFilter(
//                        FetchVideoService_yt.Constants.BROADCAST_ACTION);
//                responseReceiver = new FetchServiceResponseReceiver();
//
//                // Registers the FetchServiceResponseReceiver and its intent filters
//                LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
//                        responseReceiver, statusIntentFilter );
//
//                startFetchService(getString(res_id));

//                VideoProvider_yt.table_id =  String.valueOf(clickedPos);
//


                Intent new_intent = new Intent(getActivity(), MainActivity.class);
                new_intent.addFlags(FLAG_ACTIVITY_CLEAR_TASK);
                new_intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                getActivity().startActivity(new_intent);
                if(getActivity() != null)
                    getActivity().finish();

            }

        });
        //todo add a real refresh links button
        setOnSearchClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Intent intent = new Intent(getActivity(), SearchActivity.class);
//                startActivity(intent);

                ///
                int res_id = getResourceIdentifier(String.valueOf(1));
//                if(res_id == 0)
//                    res_id = getResourceIdentifier(item.toString());

                // receiver for fetch video service
                IntentFilter statusIntentFilter = new IntentFilter(
                        FetchVideoService_yt.Constants.BROADCAST_ACTION);
                responseReceiver = new FetchServiceResponseReceiver();

                // Registers the FetchServiceResponseReceiver and its intent filters
                LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                        responseReceiver, statusIntentFilter );

//                startFetchService(getString(res_id));
                ///

                // remove reference keys
                Utils.removePref_focus_category_number(getActivity());

                for(int i=1;i<=countCategory;i++)
                    Utils.removePref_category_name(getActivity(),i);

                getActivity().recreate();

            }
        });
    }

    // get resource Identifier
    int getResourceIdentifier(String bodyStr)
    {
	    String pre_str = "catalog_url_";
	    int res_id = getActivity().getResources().getIdentifier(pre_str.concat(bodyStr),
                                                            "string",
                                                                    getActivity().getPackageName());
	    System.out.print("SelectLinksFragment / _getResourceIdentifier / res_id = " + res_id);
	    return res_id;
    }

    private void loadData() {

//        countCategory =1;
//        for(int i=1;i<=NUM_MAX;i++) {
//            String pre_str = "catalog_url_";
//            int id = getActivity().getResources().getIdentifier(pre_str.concat(String.valueOf(i)),
//                                                            "string",
//                                                                    getActivity().getPackageName());
//            if(id !=0 )
//                countCategory = i;
//        }

        System.out.println("SelectLinksFragment / max = " + countCategory);

//        String[] urlArray = new String[countCategory];
        for(int i = 0; i< countCategory; i++) {
//            int id = getResourceIdentifier(String.valueOf(i+1));
//            urlArray[i] = getActivity().getResources().getString(id);
//
//            String categoryName = Utils.getPref_category_name(getActivity(),i+1);
//            System.out.println("SelectLinksFragment / _loadData / categoryName = " + categoryName);
//
//            if( categoryName.equalsIgnoreCase(String.valueOf(i+1))) {
//                mAdapter.add(categoryName);
//
//	            // receiver for fetch category service
//	            IntentFilter statusIntentFilter = new IntentFilter(FetchCategoryService_yt.Constants.BROADCAST_ACTION);
//	            responseReceiver = new FetchServiceResponseReceiver();
//
//	            // Registers the FetchCategoryResponseReceiver and its intent filters
//	            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(responseReceiver, statusIntentFilter );
//
//                // start new fetch category service
//                Intent serviceIntent = new Intent(getActivity(), FetchCategoryService_yt.class);
//                serviceIntent.putExtra("FetchCategoryIndex", i+1);
//                serviceIntent.putExtra("FetchCategoryUrl", urlArray[i]);
//                getActivity().startService(serviceIntent);
//                break;
//            }
//            else

            if((mCategoryNames != null) && (mCategoryNames.size() >0) ) {
                String categoryName = mCategoryNames.get(i);
//            ContentValues categoryValues = new ContentValues();
                //categoryValues.put("category_name", category_name);
//            long _id = openHelper.getWritableDatabase().insert("category", null, categoryValues);
                mAdapter.add(categoryName);
//              mAdapter.add(i+1);
            }
            else
                mAdapter.add(i+1);
        }
    }

    // start fetch service by URL string
//    private void startFetchService(String url) {
//        // delete database
//        try {
//            getActivity().deleteDatabase(VideoDbHelper_yt.DATABASE_NAME);
//
//            ContentResolver resolver = getActivity().getContentResolver();
//            ContentProviderClient client = resolver.acquireContentProviderClient(VideoContract_yt.CONTENT_AUTHORITY);
//            VideoProvider_yt provider = (VideoProvider_yt) client.getLocalContentProvider();
//
//            provider.mContentResolver = resolver;
//            provider.mOpenHelper.close();
//            provider.mOpenHelper = new VideoDbHelper_yt(getActivity());
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
//                client.close();
//            else
//                client.release();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//
//        // start new fetch video service
//        Intent serviceIntent = new Intent(getActivity(), FetchVideoService_yt.class);
//        serviceIntent.putExtra("FetchUrl", url);
//        getActivity().startService(serviceIntent);
//    }

    // Broadcast receiver for receiving status updates from the IntentService
    private class FetchServiceResponseReceiver extends BroadcastReceiver {
        // Called when the BroadcastReceiver gets an Intent it's registered to receive
        public void onReceive(Context context, Intent intent) {
            /*
             * You get notified here when your IntentService is done
             * obtaining data form the server!
             */

            // for fetch category
//            String statusStr = intent.getExtras().getString(FetchCategoryService_yt.Constants.EXTENDED_DATA_STATUS);
//            System.out.println("SelectLinksFragment / _FetchServiceResponseReceiver / _onReceive / statusStr = " + statusStr);

//            if((statusStr != null) && statusStr.equalsIgnoreCase("FetchCategoryServiceIsDone"))
//            {
//                if (context != null) {
//
//                    LocalBroadcastManager.getInstance(context).unregisterReceiver(responseReceiver);
//
//                    if(getActivity() != null)
//                        getActivity().finish();
//
//                    Intent new_intent;
//                    new_intent = new Intent(context, SelectLinksActivity.class);
//
//                    //new_intent = new Intent(context, MainActivity.class);
//
//                    new_intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
//                    context.startActivity(new_intent);
//                }
//            }

            // for fetch video service
            String statusStr = intent.getExtras().getString(FetchVideoService_yt.Constants.EXTENDED_DATA_STATUS);
            System.out.println("SelectLinksFragment / _FetchServiceResponseReceiver / _onReceive / statusStr = " + statusStr);
            if((statusStr != null ) && statusStr.equalsIgnoreCase("FetchCategoryServiceIsDone"))
            {
                if (context != null) {

                    LocalBroadcastManager.getInstance(context)
                            .unregisterReceiver(responseReceiver);

                    if(getActivity() != null)
                        getActivity().finish();

//                    Intent new_intent = new Intent(context, MainActivity.class);
                    Intent new_intent = new Intent(context, SelectLinksActivity.class);
                    new_intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(new_intent);

                }
            }
        }
    }

}

class StringPresenter extends Presenter {
    private static final String TAG = "StringPresenter";

    public  ViewHolder onCreateViewHolder(ViewGroup parent) {
        Log.d(TAG, "onCreateViewHolder");
        final Context context = parent.getContext();
        TextView tv = new TextView(context);
        tv.setFocusable(true);
        tv.setFocusableInTouchMode(true);
        tv.setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.text_bg,
                context.getTheme()));
        return new ViewHolder(tv);
    }

    public  void onBindViewHolder(ViewHolder viewHolder, Object item) {
        Log.d(TAG, "onBindViewHolder for " + item.toString());
        ((TextView) viewHolder.view).setText(item.toString());
    }

    public  void onUnbindViewHolder(ViewHolder viewHolder) {
        Log.d(TAG, "onUnbindViewHolder");
    }
}