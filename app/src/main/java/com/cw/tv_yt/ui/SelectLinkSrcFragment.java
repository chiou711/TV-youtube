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
import android.view.ViewGroup;
import android.widget.TextView;

import com.cw.tv_yt.R;
import com.cw.tv_yt.Utils;
import com.cw.tv_yt.data.DbHelper;
import com.cw.tv_yt.data.Source_links;
import com.cw.tv_yt.data.VideoContract;
import com.cw.tv_yt.data.VideoProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.leanback.app.VerticalGridSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.VerticalGridPresenter;
import androidx.loader.app.LoaderManager;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

/*
 * VerticalGridFragment shows a grid of videos that can be scrolled vertically.
 */
public class SelectLinkSrcFragment extends VerticalGridSupportFragment  {
    private static final String TAG = "Select link source";
    private static final int NUM_COLUMNS = 5;

    private static class Adapter extends ArrayObjectAdapter {
        Adapter(SrcStringPresenter presenter) {
            super(presenter);
        }
        void callNotifyChanged() {
            super.notifyChanged();
        }
    }

    private Adapter mAdapter;
	private List<String> mLinkSrcNames = new ArrayList<>();

    // Maps a Loader Id to its CursorObjectAdapter.
    private LoaderManager mLoaderManager;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        System.out.println("SelectLinkSrcFragment / _onCreate");

        super.onCreate(savedInstanceState);

        mAdapter = new Adapter(new SrcStringPresenter());
        setAdapter(mAdapter);

        setTitle(getString(R.string.select_link_src_title));

        if (savedInstanceState == null) {
            prepareEntranceTransition();
        }

        setupFragment();
    }

    @NonNull


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
                for(int i = 1; i<= mLinkSrcNames.size(); i++) {
                    if (mLinkSrcNames.get(i-1).equalsIgnoreCase(item.toString()))
                    {
                        clickedPos = i;
                        System.out.println("------------------clickedPos = " + clickedPos);
                    }
                }

                Utils.setPref_link_source_number(getActivity(),clickedPos);

                startRenewFetchService();

                // remove reference keys
                Utils.removePref_focus_category_number(getActivity());

                int countVideoTables = Utils.getVideoTablesCount(getActivity());

                // remove category name key
                for(int i = 1; i<= countVideoTables; i++)
                    Utils.removePref_category_name(getActivity(),i);
            }
        });
    }


    // start fetch service by URL string
    private void startRenewFetchService() {
        System.out.println("SelectLinkSrcFragment / _startFetchService");
        // delete database
        try {
            System.out.println("SelectLinkSrcFragment / _startFetchService / will delete DB");
            Objects.requireNonNull(getActivity()).deleteDatabase(DbHelper.DATABASE_NAME);

            ContentResolver resolver = getActivity().getContentResolver();
            ContentProviderClient client = resolver.acquireContentProviderClient(VideoContract.CONTENT_AUTHORITY);
            assert client != null;
            VideoProvider provider = (VideoProvider) client.getLocalContentProvider();

            assert provider != null;
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
        Objects.requireNonNull(getActivity()).startActivity(new_intent);
    }


    private void loadData() {
        System.out.println("SelectLinkSrcFragment / _loadData");

        // add source names
        List<String> src_links = Source_links.getFileIdList(Objects.requireNonNull(getActivity()));
        for(int i=1;i<=src_links.size();i++) {
            mLinkSrcNames.add("Source "+ i);
        }

        for(int i = 0; i< mLinkSrcNames.size(); i++) {
            String categoryName = mLinkSrcNames.get(i);
            mAdapter.add(categoryName);
        }
    }
}

class SrcStringPresenter extends Presenter {
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