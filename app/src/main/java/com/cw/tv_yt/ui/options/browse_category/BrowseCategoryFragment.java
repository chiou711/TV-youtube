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

package com.cw.tv_yt.ui.options.browse_category;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import androidx.leanback.app.VerticalGridSupportFragment;
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
import android.view.View;
import android.widget.Toast;

import com.cw.tv_yt.R;
import com.cw.tv_yt.Utils;
import com.cw.tv_yt.data.VideoContract;
import com.cw.tv_yt.model.Video;
import com.cw.tv_yt.model.VideoCursorMapper;
import com.cw.tv_yt.presenter.CardPresenter;
import com.cw.tv_yt.ui.PlaybackActivity;
import com.cw.tv_yt.ui.VideoDetailsActivity;
import com.google.android.youtube.player.YouTubeIntents;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.cw.tv_yt.Utils.getYoutubeId;


/*
 * BrowseCategoryFragment shows a grid of videos that can be scrolled vertically.
 */
public class BrowseCategoryFragment extends VerticalGridSupportFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int NUM_COLUMNS = 5;
    private CursorObjectAdapter mVideoCursorAdapter;
    private static final int ALL_VIDEOS_LOADER = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // parameter -1 is used for hiding row number in card view
        mVideoCursorAdapter = new CursorObjectAdapter(new CardPresenter(getActivity(),-1));

        mVideoCursorAdapter.setMapper(new VideoCursorMapper());
        setAdapter(mVideoCursorAdapter);

        setTitle(getString(R.string.category_grid_view_title));

        if (savedInstanceState == null) {
            prepareEntranceTransition();
        }
        setupFragment();
    }

    private void setupFragment() {
        VerticalGridPresenter gridPresenter = new VerticalGridPresenter();
        gridPresenter.setNumberOfColumns(NUM_COLUMNS);
        setGridPresenter(gridPresenter);

        getLoaderManager().initLoader(ALL_VIDEOS_LOADER, null, this);

        // After 500ms, start the animation to transition the cards into view.
        new Handler().postDelayed(new Runnable() {
            public void run() {
                startEntranceTransition();
            }
        }, 500);

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

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(
                getActivity(),
                VideoContract.VideoEntry.CONTENT_URI,
                null, // projection
                null, // selection
                null, // selection clause
                null  // sort order
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (loader.getId() == ALL_VIDEOS_LOADER && cursor != null && cursor.moveToFirst()) {
            mVideoCursorAdapter.changeCursor(cursor);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mVideoCursorAdapter.changeCursor(null);
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof Video) {
                System.out.println("VerticalGridFragment /  _onItemClicked");
                Video video = (Video) item;

                String path;
                String urlStr = video.videoUrl;
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
                                getActivity().runOnUiThread(new Runnable() {
                                    public void run() {

                                        if(((Video) item).videoUrl.contains("playlist"))
                                        {
                                            String playListIdStr = Utils.getYoutubePlaylistId(((Video) item).videoUrl);
                                            Intent intent = YouTubeIntents.createPlayPlaylistIntent(getActivity(), playListIdStr);
                                            intent.putExtra("force_fullscreen", true);
                                            intent.putExtra("finish_on_ended", true);
                                            startActivity(intent);
                                        } else {
                                            // for open directly
                                            String idStr = getYoutubeId(((Video) item).videoUrl);
                                            Intent intent = YouTubeIntents.createPlayVideoIntent(getActivity(), idStr);
                                            intent.putExtra("force_fullscreen", true);
                                            intent.putExtra("finish_on_ended", true);
                                            startActivity(intent);
                                        }
                                    }
                                });
                            } else {
                                // https://drive.google.com/uc?export=view&id=ID
                                if(urlStr.contains("https://drive.google.com/uc?export=view") ||
                                   urlStr.contains("https://storage.googleapis.com/android-tv") ){
                                    // play video
                                    Intent intent = new Intent(getActivity(), PlaybackActivity.class);
                                    intent.putExtra(VideoDetailsActivity.VIDEO, ((Video) item));
                                    startActivity(intent);
                                } else {
                                    // play HTML
                                    String link = ((Video) item).videoUrl;
                                    Uri uriStr = Uri.parse(link);
                                    Intent intent = new Intent(Intent.ACTION_VIEW, uriStr);
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
            }
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                RowPresenter.ViewHolder rowViewHolder, Row row) {
        }
    }
}
