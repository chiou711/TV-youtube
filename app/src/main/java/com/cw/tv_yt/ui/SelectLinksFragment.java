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
import android.os.Build;
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
import com.cw.tv_yt.data_yt.VideoProvider_yt;

import androidx.core.content.res.ResourcesCompat;
import androidx.leanback.app.VerticalGridSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.VerticalGridPresenter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

/*
 * VerticalGridFragment shows a grid of videos that can be scrolled vertically.
 */
public class SelectLinksFragment extends VerticalGridSupportFragment {
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
                int clickedPos = 0;
                for(int i = 1; i<= countCategory; i++) {

                    String categoryName = Utils.getPref_category_name(getActivity(), i);
                    if (categoryName.equalsIgnoreCase(item.toString())) {
                        clickedPos = i;
                    }
                }

                int res_id = getResourceIdentifier(String.valueOf(clickedPos));
                if(res_id == 0)
                    res_id = getResourceIdentifier(item.toString());



                // receiver for fetch video service
                IntentFilter statusIntentFilter = new IntentFilter(
                        FetchVideoService_yt.Constants.BROADCAST_ACTION);
                responseReceiver = new FetchServiceResponseReceiver();

                // Registers the FetchServiceResponseReceiver and its intent filters
                LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                        responseReceiver, statusIntentFilter );

                startFetchService(getString(res_id));
            }

        });
        setOnSearchClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SearchActivity.class);
                startActivity(intent);
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

    private int countCategory;
    private void loadData() {

        countCategory =1;
        for(int i=1;i<=NUM_MAX;i++) {
            String pre_str = "catalog_url_";
            int id = getActivity().getResources().getIdentifier(pre_str.concat(String.valueOf(i)),
                                                            "string",
                                                                    getActivity().getPackageName());
            if(id !=0 )
                countCategory = i;
        }

        System.out.println("SelectLinksFragment / max = " + countCategory);

        String[] urlArray = new String[countCategory];
        for(int i = 0; i< countCategory; i++) {
            int id = getResourceIdentifier(String.valueOf(i+1));
            urlArray[i] = getActivity().getResources().getString(id);

            String categoryName = Utils.getPref_category_name(getActivity(),i+1);
            System.out.println("SelectLinksFragment / _loadData / categoryName = " + categoryName);

            if( categoryName.equalsIgnoreCase(String.valueOf(i+1))) {
                mAdapter.add(categoryName);

	            // receiver for fetch category service
	            IntentFilter statusIntentFilter = new IntentFilter(FetchCategoryService_yt.Constants.BROADCAST_ACTION);
	            responseReceiver = new FetchServiceResponseReceiver();

	            // Registers the FetchCategoryResponseReceiver and its intent filters
	            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(responseReceiver, statusIntentFilter );

                // start new fetch category service
                Intent serviceIntent = new Intent(getActivity(), FetchCategoryService_yt.class);
                serviceIntent.putExtra("FetchCategoryIndex", i+1);
                serviceIntent.putExtra("FetchCategoryUrl", urlArray[i]);
                getActivity().startService(serviceIntent);
                break;
            }
            else
                mAdapter.add(categoryName);
        }
    }

    // start fetch service by URL string
    private void startFetchService(String url) {
        // delete database
        try {
            getActivity().deleteDatabase(VideoDbHelper_yt.DATABASE_NAME);

            ContentResolver resolver = getActivity().getContentResolver();
            ContentProviderClient client = resolver.acquireContentProviderClient(VideoContract_yt.CONTENT_AUTHORITY);
            VideoProvider_yt provider = (VideoProvider_yt) client.getLocalContentProvider();

            provider.mContentResolver = resolver;
            provider.mOpenHelper.close();
            provider.mOpenHelper = new VideoDbHelper_yt(getActivity());

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
        getActivity().startService(serviceIntent);
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
            String statusStr = intent.getExtras().getString(FetchCategoryService_yt.Constants.EXTENDED_DATA_STATUS);
            System.out.println("SelectLinksFragment / _FetchServiceResponseReceiver / _onReceive / statusStr = " + statusStr);

            if((statusStr != null) && statusStr.equalsIgnoreCase("FetchCategoryServiceIsDone"))
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
            statusStr = intent.getExtras().getString(FetchVideoService_yt.Constants.EXTENDED_DATA_STATUS);
            System.out.println("SelectLinksFragment / _FetchServiceResponseReceiver / _onReceive / statusStr = " + statusStr);
            if((statusStr != null ) && statusStr.equalsIgnoreCase("FetchVideoServiceIsDone"))
            {
                if (context != null) {

                    LocalBroadcastManager.getInstance(context)
                            .unregisterReceiver(responseReceiver);

                    if(getActivity() != null)
                        getActivity().finish();

                    Intent new_intent = new Intent(context, MainActivity.class);
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