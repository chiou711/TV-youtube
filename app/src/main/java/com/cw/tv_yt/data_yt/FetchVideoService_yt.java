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
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.util.Log;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

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
	    String session = workIntent.getStringExtra("Session");
	    System.out.println("FetchVideoService_yt / _onHandleIntent / serviceUrl = " + serviceUrl);
	    System.out.println("FetchVideoService_yt / _onHandleIntent / session = " + session);
        VideoDbBuilder_yt builder = new VideoDbBuilder_yt(getApplicationContext());

        try {
//	        List<ContentValues> contentValuesList =
			        List<List<ContentValues>> contentValuesList =
//                    builder.fetch(getResources().getString(R.string.catalog_url));
                    builder.fetch(serviceUrl);

			for(int i=0;i<contentValuesList.size();i++) {

				ContentValues[] downloadedVideoContentValues =
//						contentValuesList.toArray(new ContentValues[contentValuesList.size()]);
						contentValuesList.get(i).toArray(new ContentValues[contentValuesList.get(i).size()]);

				ContentResolver contentResolver = getApplicationContext().getContentResolver();
				System.out.println("FetchVideoService_yt / _onHandleIntent / contentResolver = " + contentResolver.toString());

//            getApplicationContext().getContentResolver().bulkInsert(VideoContract_yt.VideoEntry.CONTENT_URI,
//                    downloadedVideoContentValues);
				VideoProvider_yt.tableId = String.valueOf(i+1);
				contentResolver.bulkInsert(VideoContract_yt.VideoEntry.CONTENT_URI, downloadedVideoContentValues);
			}

        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error occurred in downloading videos");
            e.printStackTrace();
        }

        // Puts the status into the Intent
        String status = "FetchVideoServiceIsDone"; // any data that you want to send back to receivers

	    Intent localIntent = null;
	    if(session.equalsIgnoreCase("install")) {
		    localIntent = new Intent(Constants.BROADCAST_ACTION);
		    localIntent.putExtra(Constants.EXTENDED_DATA_STATUS, status);
	    }else if(session.equalsIgnoreCase("renew"))
	    {
		    localIntent = new Intent(Constants.SELECT_LINKS_BROADCAST_ACTION);
		    localIntent.putExtra(Constants.SELECT_LINKS_EXTENDED_DATA_STATUS, status);
	    }

        // Broadcasts the Intent to receivers in this app.
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
        System.out.println("FetchVideoService_yt / _onHandleIntent / sendBroadcast");
    }

    public final class Constants {
        // Defines a custom Intent action
        public static final String BROADCAST_ACTION =
		        "com.cw.tv_yt.BROADCAST";
	    // Defines the key for the status "extra" in an Intent
	    public static final String EXTENDED_DATA_STATUS =
			    "com.cw.tv_yt.STATUS";
	    public static final String SELECT_LINKS_BROADCAST_ACTION =
			    "com.cw.tv_yt.SELECT_LINKS_BROADCAST";
	    // Defines the key for the status "extra" in an Intent
	    public static final String SELECT_LINKS_EXTENDED_DATA_STATUS =
			    "com.cw.tv_yt.SELECT_LINKS_STATUS";
    }
}