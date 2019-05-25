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

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cw.tv_yt.R;
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


    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        mAdapter = new Adapter(new StringPresenter());
        setAdapter(mAdapter);

        setTitle(getString(R.string.select_links_title));

        if (savedInstanceState == null) {
            prepareEntranceTransition();
        }
        setupFragment();
    }

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
                Log.i(TAG, "onItemClicked: " + item + " row " + row);
                mAdapter.callNotifyChanged();

                String pre_str = "catalog_url_";
                int res_id = getActivity().getResources().getIdentifier(pre_str.concat(item.toString()),"string",getActivity().getPackageName());
                startFetchService(getString(res_id) );
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


    private void loadData() {

        int count=1;
        for(int i=1;i<=NUM_MAX;i++) {
            String pre_str = "catalog_url_";
            int id = getActivity().getResources().getIdentifier(pre_str.concat(String.valueOf(i)),
                                                            "string",
                                                                    getActivity().getPackageName());
            if(id !=0 )
                count = i;
        }

        System.out.println("SelectLinksFragment / max = " + count);

        for (int i = 1; i <= count; i++) {
            mAdapter.add(Integer.toString(i));
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
