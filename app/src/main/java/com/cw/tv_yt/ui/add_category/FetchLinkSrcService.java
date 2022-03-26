/*
 * Copyright (c) 2022 The Android Open Source Project
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

package com.cw.tv_yt.ui.add_category;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.cw.tv_yt.R;
import com.cw.tv_yt.Utils;
import com.cw.tv_yt.data.FetchVideoService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.net.ssl.HttpsURLConnection;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * FetchLinkSrcService is responsible for fetching the videos from the Internet and inserting the
 * results into a local SQLite database.
 */
public class FetchLinkSrcService extends IntentService {
    private static final String TAG = "FetchLinkSrcService";
    public static String serviceUrl;

    /**
     * Creates an IntentService with a default name for the worker thread.
     */
    public FetchLinkSrcService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        serviceUrl = workIntent.getStringExtra("FetchUrl");
        System.out.println("FetchLinkSrcService / _onHandleIntent / serviceUrl = " + serviceUrl);
	    Toast.makeText(this,R.string.please_wait,Toast.LENGTH_LONG).show();

	    try {
		    JSONObject videoData = fetchJSON(serviceUrl);

		    // parse JSON and insert DB
		    try {
			    Utils.parseJsonAndInsertDB(this,videoData);
		    } catch (JSONException e) {
			    e.printStackTrace();
		    }
	    } catch (JSONException e) {
		    e.printStackTrace();
	    } catch (IOException e) {
		    e.printStackTrace();
	    }

	    // Puts the status into the Intent
	    String status = "FetchVideoServiceIsDone"; // any data that you want to send back to receivers

	    Intent localIntent = new Intent(FetchVideoService.Constants.BROADCAST_ACTION);
	    localIntent.putExtra(FetchVideoService.Constants.EXTENDED_DATA_STATUS, status);

	    // Broadcasts the Intent to receivers in this app.
	    LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
	    System.out.println("FetchLinkSrcService / _onHandleIntent / sendBroadcast");
    }

    // fetch JSON by Url string
	private JSONObject fetchJSON(String urlString) throws JSONException, IOException {
		System.out.println("FetchLinkSrcService / fetchJSON / urlString = " + urlString);

		BufferedReader reader = null;
		java.net.URL url = new java.net.URL(urlString);
		HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
		try {
			reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(),
					"utf-8"));
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
			String json = sb.toString();
			return new JSONObject(json);
		} finally {
			urlConnection.disconnect();
			if (null != reader) {
				try {
					reader.close();
				} catch (IOException e) {
					Log.e(TAG, "JSON feed closed", e);
				}
			}
		}
	}

}