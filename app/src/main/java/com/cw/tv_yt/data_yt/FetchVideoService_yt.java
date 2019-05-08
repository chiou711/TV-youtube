/*
 * Copyright (c) 2016 The Android Open Source Project
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

package com.cw.tv_yt.data_yt;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.util.Log;

import com.cw.tv_yt.R;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;

/**
 * FetchVideoService is responsible for fetching the videos from the Internet and inserting the
 * results into a local SQLite database.
 */
public class FetchVideoService_yt extends IntentService {
    private static final String TAG = "FetchVideoService_yt";
    public static String serviceUrl;

    /**
     * Creates an IntentService with a default name for the worker thread.
     */
    public FetchVideoService_yt() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        serviceUrl = workIntent.getStringExtra("FetchUrl");
        System.out.println("FetchVideoService_yt / _onHandleIntent / serviceUrl = " + serviceUrl);
        VideoDbBuilder_yt builder = new VideoDbBuilder_yt(getApplicationContext());

        try {
            List<ContentValues> contentValuesList =
//                    builder.fetch(getResources().getString(R.string.catalog_url));
                    builder.fetch(serviceUrl);

            ContentValues[] downloadedVideoContentValues =
                    contentValuesList.toArray(new ContentValues[contentValuesList.size()]);

            getApplicationContext().getContentResolver().bulkInsert(VideoContract_yt.VideoEntry.CONTENT_URI,
                    downloadedVideoContentValues);

        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error occurred in downloading videos");
            e.printStackTrace();
        }
    }
}
