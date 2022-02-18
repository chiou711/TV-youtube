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

package com.cw.tv_yt.ui.link_src;

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
import com.cw.tv_yt.data.Pair;
import com.cw.tv_yt.data.Source_links;
import com.cw.tv_yt.import_new.FetchLinkSrcService;
import com.cw.tv_yt.import_new.Import_fileListAct;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.leanback.app.VerticalGridSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.VerticalGridPresenter;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

/*
 * VerticalGridFragment shows a grid of videos that can be scrolled vertically.
 */
public class SelectLinkSrcFragment extends VerticalGridSupportFragment  {
    private static final String TAG = "Select_link_source";
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
                Log.i(TAG, "onItemSelected: item = " + item + " row = " + row);
            }
        });

        setOnItemViewClickedListener(new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                      RowPresenter.ViewHolder rowViewHolder, Row row) {
                Log.i(TAG, "onItemClicked: item = " + item + " row = " + row );
                mAdapter.callNotifyChanged();

                int clickedSrcLinkNum = 1;
                for(int i = 1; i<= mLinkSrcNames.size(); i++) {
                    if (mLinkSrcNames.get(i-1).equalsIgnoreCase(item.toString()))
                        clickedSrcLinkNum = i;
                }

                // position number 1: is dedicated for local link source
                // others: for link sources
                if(clickedSrcLinkNum == 1){ // local link source
                    Intent intent = new Intent(getActivity(), Import_fileListAct.class);
                    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity()).toBundle();
                    bundle.putInt("link_source_number",clickedSrcLinkNum);
                    intent.putExtras(bundle);
                    startActivity(intent, bundle);
                } else { // add new data from selected link source
                    Utils.setPref_link_source_number(getActivity(), clickedSrcLinkNum);

                    // get URL string
                    String urlString;
                    List<Pair<String, String>> src_links = Source_links.getFileIdList(Objects.requireNonNull(getActivity()));
                    int index = clickedSrcLinkNum -1;
                    urlString =  "https://drive.google.com/uc?export=download&id=" + src_links.get(index).getSecond();

                    // start Fetch service to import DB data
                    Intent serviceIntent = new Intent(getActivity(), FetchLinkSrcService.class);
                    serviceIntent.putExtra("FetchUrl", urlString );
                    getActivity().startService(serviceIntent);
                }
            }
        });
    }

    private void loadData() {
        System.out.println("SelectLinkSrcFragment / _loadData");

        // add source names
        List<Pair<String, String>> src_links = Source_links.getFileIdList(Objects.requireNonNull(getActivity()));

        System.out.println(" SelectLinkSrcFragment / _loadData / src_links.size() = " + src_links.size());
            for(int pos=0;pos<src_links.size();pos++) {
                String title = src_links.get(pos).getFirst();
                System.out.println(" SelectLinkSrcFragment / _loadData / title = " + title);

                // set current usage marking *
                int linkSrcNum = Utils.getPref_link_source_number(getActivity());

                // position 0 and 1 are dedicated
                if((pos>=2) && (pos+1) == linkSrcNum) // link src number starts from 1
                    mLinkSrcNames.add(src_links.get(pos).getFirst().concat("*"));
                else {
                    // add marking after localized string at onBindViewHolder stage, not here
                    mLinkSrcNames.add(src_links.get(pos).getFirst());
                }
        }

        for(int i = 0; i< mLinkSrcNames.size(); i++) {
            String linkSrcName = mLinkSrcNames.get(i);
            mAdapter.add(linkSrcName);
        }
    }
}

class SrcStringPresenter extends Presenter {
    private static final String TAG = "StringPresenter";

    String localLinkSrc;
    String defaultLinkSrc;
    int linkSrcNum;

    public  ViewHolder onCreateViewHolder(ViewGroup parent) {
        Log.d(TAG, "onCreateViewHolder");
        final Context context = parent.getContext();
        TextView tv = new TextView(context);
        tv.setFocusable(true);
        tv.setFocusableInTouchMode(true);
        tv.setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.text_bg,
                context.getTheme()));

        // get localized string
        localLinkSrc = context.getResources().getString(R.string.local_link_src);
        defaultLinkSrc =  context.getResources().getString(R.string.default_link_src);

        linkSrcNum = Utils.getPref_link_source_number(context);
        return new ViewHolder(tv);
    }

    public  void onBindViewHolder(ViewHolder viewHolder, Object item) {
        Log.d(TAG, "onBindViewHolder for " + item.toString());

        // replace title with with localized string for position 0 and 1
        if(item.toString().equalsIgnoreCase("Local_TV-youtube")) {
            if(linkSrcNum == 1)
                localLinkSrc = localLinkSrc.concat("*");
            ((TextView) viewHolder.view).setText(localLinkSrc);
        } else if(item.toString().equalsIgnoreCase("Default_TV-youtube")) {
            if(linkSrcNum == 2)
                defaultLinkSrc = defaultLinkSrc.concat("*");
            ((TextView) viewHolder.view).setText(defaultLinkSrc);
        } else
            ((TextView) viewHolder.view).setText(item.toString());
    }

    public  void onUnbindViewHolder(ViewHolder viewHolder) {
        Log.d(TAG, "onUnbindViewHolder");
    }
}