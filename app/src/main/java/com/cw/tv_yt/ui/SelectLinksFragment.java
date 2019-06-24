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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cw.tv_yt.R;
import com.cw.tv_yt.Utils;
import com.cw.tv_yt.data_yt.CategoryDbHelper;
import com.cw.tv_yt.data_yt.FetchCategoryService_yt;
import com.cw.tv_yt.data_yt.FetchVideoService_yt;
import com.cw.tv_yt.data_yt.VideoContract_yt;
import com.cw.tv_yt.data_yt.VideoDbHelper_yt;
import com.cw.tv_yt.data_yt.VideoProvider_yt;

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
    private int countVideoTables;
    List<String> mCategoryNames = new ArrayList<>();

    // Maps a Loader Id to its CursorObjectAdapter.
    private Map<Integer, CursorObjectAdapter> mCategoryCursorAdapters;
    private LoaderManager mLoaderManager;
    private static final int CATEGORY_LOADER = 246; // Unique ID for Category Loader.

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        System.out.println("SelectLinksFragment / _onCreate");

        super.onCreate(savedInstanceState);

        // Create a list to contain all the CursorObjectAdapters.
        // Each adapter is used to render a specific row of videos in the MainFragment.
        mCategoryCursorAdapters = new HashMap<>();

        // Start loading the categories from the database.
        mLoaderManager = LoaderManager.getInstance(this);
        mLoaderManager.initLoader(CATEGORY_LOADER, null, this);

        // receiver for fetch category service
        IntentFilter statusIntentFilter = new IntentFilter(FetchCategoryService_yt.Constants.BROADCAST_ACTION);
        responseReceiver = new FetchServiceResponseReceiver();

        // Registers the FetchCategoryResponseReceiver and its intent filters
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(responseReceiver, statusIntentFilter );



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
        countVideoTables = cursor.getCount();
        cursor.close();
        sqlDb.close();

        System.out.println("SelectLinksFragment / _onCreate / video tables count = " + countVideoTables);
    }


    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {

        System.out.println("SelectLinksFragment / _onCreateLoader / id == CATEGORY_LOADER / CategoryContract.CategoryEntry.CONTENT_URI =" + VideoContract_yt.CategoryEntry.CONTENT_URI);

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


    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {

        System.out.println("SelectLinksFragment / _onLoadFinished" );

        if (data != null && data.moveToFirst()) {
            final int loaderId = loader.getId();
            System.out.println("SelectLinksFragment / _onLoadFinished / loaderId = " + loaderId);

            if (loaderId == CATEGORY_LOADER) {
                // Iterate through each category entry and add it to the ArrayAdapter.
                while (!data.isAfterLast()) {

                    int categoryIndex = data.getColumnIndex(VideoContract_yt.CategoryEntry.COLUMN_CATEGORY_NAME);
                    String category_name = data.getString(categoryIndex);
                    System.out.println("SelectLinksFragment / _onLoadFinished / category_name = " + category_name);
                    mCategoryNames.add(category_name);
                    data.moveToNext();
                }

                startEntranceTransition(); // TODO: Move startEntranceTransition to after all
                // cursors have loaded.

            }
        } else {
            Intent serviceIntent = new Intent(getActivity(), FetchCategoryService_yt.class);
            int initNumber = 1;
            Utils.setPref_focus_category_number(getActivity(),initNumber);
            // todo use catalog_url_1 to be default URL
            String pre_str = "catalog_url_";
            int id = getActivity().getResources().getIdentifier(pre_str.concat(String.valueOf(initNumber)),
                    "string",
                    getActivity().getPackageName());
            String default_url = getString(id);
            System.out.println("SelectLinksFragment / onLoadFinished / start service =================================");
            serviceIntent.putExtra("FetchUrl", default_url);
            getActivity().startService(serviceIntent);

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

                int clickedPos = 0;
                for(int i = 1; i<= mCategoryNames.size(); i++) {
                    if (mCategoryNames.get(i-1).equalsIgnoreCase(item.toString()))
                    {
                        clickedPos = i;
                        System.out.println("------------------clickedPos = " + clickedPos);
                    }
                }

                Utils.setPref_focus_category_number(getActivity(),clickedPos);

                if(getActivity() != null)
                    getActivity().finish();

                Intent new_intent = new Intent(getActivity(), MainActivity.class);
                new_intent.addFlags(FLAG_ACTIVITY_CLEAR_TASK);
                new_intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                getActivity().startActivity(new_intent);


            }

        });
        //todo add a real refresh links button
        setOnSearchClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int res_id = getResourceIdentifier(String.valueOf(1));

                // receiver for fetch video service
                IntentFilter statusIntentFilter = new IntentFilter(FetchVideoService_yt.Constants.SELECT_LINKS_BROADCAST_ACTION);
                responseReceiver = new FetchServiceResponseReceiver();

                // Registers the FetchServiceResponseReceiver and its intent filters
                LocalBroadcastManager.getInstance(getActivity()).registerReceiver(responseReceiver, statusIntentFilter );

                startFetchService(getString(res_id));

                // remove reference keys
                Utils.removePref_focus_category_number(getActivity());

                for(int i = 1; i<= countVideoTables; i++)
                    Utils.removePref_category_name(getActivity(),i);
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
	    System.out.println("SelectLinksFragment / _getResourceIdentifier / res_id = " + res_id);
	    return res_id;
    }

    private void loadData() {
        System.out.println("SelectLinksFragment / _loadData");

        for(int i = 0; i< mCategoryNames.size(); i++) {
            if((mCategoryNames != null) && (mCategoryNames.size() >0) ) {
                String categoryName = mCategoryNames.get(i);
                mAdapter.add(categoryName);
            }
            else
                mAdapter.add(i+1);
        }
    }

    // start fetch service by URL string
    private void startFetchService(String url) {
        System.out.println("SelectLinksFragment / _startFetchService");
        // delete database
        try {
            System.out.println("SelectLinksFragment / _startFetchService / will delete DB");
            getActivity().deleteDatabase(VideoDbHelper_yt.DATABASE_NAME);

            ContentResolver resolver = getActivity().getContentResolver();
            ContentProviderClient client = resolver.acquireContentProviderClient(VideoContract_yt.CONTENT_AUTHORITY);
            VideoProvider_yt provider = (VideoProvider_yt) client.getLocalContentProvider();

            provider.mContentResolver = resolver;
            provider.mOpenHelper.close();
            provider.mCategoryOpenHelper.close();

            provider.mOpenHelper = new VideoDbHelper_yt(getActivity());
            provider.mCategoryOpenHelper = new CategoryDbHelper(getActivity());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                client.close();
            else
                client.release();

        } catch (Exception e) {
            e.printStackTrace();
        }


        // start new fetch video service
        Intent serviceIntent = new Intent(getActivity(), FetchVideoService_yt.class);
        serviceIntent.putExtra("FetchUrl", url);
        serviceIntent.putExtra("Session", "renew");
        getActivity().startService(serviceIntent);
        System.out.println("SelectLinksFragment / _startFetchService / will start service");
        System.out.println("-----");
    }

    // Broadcast receiver for receiving status updates from the IntentService
    private class FetchServiceResponseReceiver extends BroadcastReceiver {
        // Called when the BroadcastReceiver gets an Intent it's registered to receive
        public void onReceive(Context context, Intent intent) {
            /*
             * You get notified here when your IntentService is done
             * obtaining data form the server!
             */

            // for fetch category
            String statusStr1 = intent.getExtras().getString(FetchCategoryService_yt.Constants.EXTENDED_DATA_STATUS);
            System.out.println("SelectLinksFragment / _FetchServiceResponseReceiver / _onReceive / statusStr1 = " + statusStr1);

            if((statusStr1 != null) && statusStr1.equalsIgnoreCase("FetchCategoryServiceIsDone"))
            {
                if (context != null) {

                    LocalBroadcastManager.getInstance(context).unregisterReceiver(responseReceiver);

                    if(getActivity() != null)
                        getActivity().finish();

                    Intent new_intent;
                    new_intent = new Intent(context, SelectLinksActivity.class);

                    //new_intent = new Intent(context, MainActivity.class);

                    new_intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(new_intent);
                }
            }

            // for fetch video service
            String statusStr2 = intent.getExtras().getString(FetchVideoService_yt.Constants.SELECT_LINKS_EXTENDED_DATA_STATUS);
            System.out.println("SelectLinksFragment / _FetchServiceResponseReceiver / _onReceive / statusStr2 = " + statusStr2);
            if((statusStr2 != null ) && statusStr2.equalsIgnoreCase("FetchVideoServiceIsDone"))
            {
                if (context != null) {

                    LocalBroadcastManager.getInstance(context)
                            .unregisterReceiver(responseReceiver);

                    if(getActivity() != null)
                        getActivity().finish();

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