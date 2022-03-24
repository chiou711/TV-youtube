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

package com.cw.tv_yt.ui.options.select_category;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cw.tv_yt.R;
import com.cw.tv_yt.Utils;
import com.cw.tv_yt.data.VideoContract;
import com.cw.tv_yt.ui.MainActivity;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.app.VerticalGridSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.VerticalGridPresenter;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

/*
 * VerticalGridFragment shows a grid of videos that can be scrolled vertically.
 */
public class SelectCategoryFragment extends VerticalGridSupportFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "SelectCategoryFragment";
    private static final int NUM_COLUMNS = 5;

    private static class Adapter extends ArrayObjectAdapter {
        Adapter(StringPresenter presenter) {
            super(presenter);
        }
        void callNotifyChanged() {
            super.notifyChanged();
        }
    }

    private Adapter mAdapter;
	private List<String> mCategoryNames = new ArrayList<>();

    // Maps a Loader Id to its CursorObjectAdapter.
    private LoaderManager mLoaderManager;
	private static final int CATEGORY_LOADER = 246; // Unique ID for Category Loader.
    private FragmentActivity act;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

	    // Start loading the categories from the database.
	    mLoaderManager = LoaderManager.getInstance(this);
	    mLoaderManager.initLoader(CATEGORY_LOADER, null, this);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        act = getActivity();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        System.out.println("SelectCategoryFragment / _onCreate");

        super.onCreate(savedInstanceState);

        mAdapter = new Adapter(new StringPresenter());
        setAdapter(mAdapter);

        setTitle(getString(R.string.select_category_title));

        if (savedInstanceState == null) {
            prepareEntranceTransition();
        }

        setupFragment();
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {

        System.out.println("SelectCategoryFragment / _onCreateLoader / id == CATEGORY_LOADER / CategoryContract.CategoryEntry.CONTENT_URI =" + VideoContract.CategoryEntry.CONTENT_URI);

        // id = CATEGORY_LOADER
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

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {

        System.out.println("SelectCategoryFragment / _onLoadFinished" );

        if (data != null && data.moveToFirst()) {
            final int loaderId = loader.getId();
            System.out.println("SelectCategoryFragment / _onLoadFinished / loaderId = " + loaderId);

            if (loaderId == CATEGORY_LOADER) {

                mCategoryNames = new ArrayList<>();
                // Iterate through each category entry and add it to the ArrayAdapter.
                while (!data.isAfterLast()) {

                    int categoryIndex = data.getColumnIndex(VideoContract.CategoryEntry.COLUMN_CATEGORY_NAME);
                    String category_name = data.getString(categoryIndex);
                    System.out.println("SelectCategoryFragment / _onLoadFinished / category_name = " + category_name);
                    mCategoryNames.add(category_name);
                    data.moveToNext();
                }

                startEntranceTransition();
            }
        } else
            System.out.println("SelectCategoryFragment / onLoadFinished / data is null");
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
                setSelectedPosition(Utils.getPref_video_table_id(getActivity())-1);
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

                Utils.setPref_category_name(getActivity(),(String)item);

                if(getActivity() != null)
                    getActivity().finish();

                Intent new_intent = new Intent(getActivity(), MainActivity.class);
                new_intent.addFlags(FLAG_ACTIVITY_CLEAR_TASK);
                new_intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                getActivity().startActivity(new_intent);
            }
        });
    }

    private void loadData() {
        System.out.println("SelectCategoryFragment / _loadData");

        for(int i = 0; i< mCategoryNames.size(); i++) {
            String categoryName = mCategoryNames.get(i);
            mAdapter.add(categoryName);
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

            ((TextView) viewHolder.view).setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Utils.confirmDeleteCategory(act,mCategoryNames,(String)item);
                    return true;
                }
            });
        }

        public  void onUnbindViewHolder(ViewHolder viewHolder) {
            Log.d(TAG, "onUnbindViewHolder");
        }
    }

}