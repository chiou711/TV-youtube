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

package com.cw.tv_yt.import_new;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.cw.tv_yt.R;
import com.cw.tv_yt.Utils;
import com.cw.tv_yt.data.DbHelper;
import com.cw.tv_yt.data.VideoContract;
import com.cw.tv_yt.data.VideoProvider;
import com.cw.tv_yt.ui.MainActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.cw.tv_yt.data.VideoDbBuilder.TAG_LINK_PAGE;
import static com.cw.tv_yt.data.VideoDbBuilder.TAG_MEDIA;
import static com.cw.tv_yt.data.VideoDbBuilder.TAG_TITLE;

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

	    try {
		    JSONObject videoData = fetchJSON(serviceUrl);

		    // parse JSON and insert DB
		    try {
			    parseJsonAndInsertDB(videoData);
		    } catch (JSONException e) {
			    e.printStackTrace();
		    }
	    } catch (JSONException e) {
		    e.printStackTrace();
	    } catch (IOException e) {
		    e.printStackTrace();
	    }
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

	// parse JSON and insert data to DB
	public void parseJsonAndInsertDB(JSONObject jsonObj) throws JSONException
	{
		System.out.println("FetchLinkSrcService / _parseJsonAndInsertDB / jsonObj string = " + jsonObj.toString());
		Context mContext = this;

		// 1) import jsonObj: get category list
		JSONArray contentArray_cat = jsonObj.getJSONArray("content");
		List<ContentValues> videosToInsert_cat = new ArrayList<>();

		for (int h = 0; h < contentArray_cat.length(); h++) {

			JSONObject contentObj = contentArray_cat.getJSONObject(h);

			// category name
			String category_name = contentObj.getString("category");

			// save category names
			ContentValues categoryValues = new ContentValues();
			categoryValues.put("category_name", category_name);
			videosToInsert_cat.add(categoryValues);
		}

		// 2) import jsonObj: add category list
		List<ContentValues> contentValuesList = videosToInsert_cat;

		ContentValues[] downloadedVideoContentValues =
				contentValuesList.toArray(new ContentValues[contentValuesList.size()]);

		ContentResolver contentResolver = mContext.getApplicationContext().getContentResolver();

		// get current video tables count
		String[] projection = new String[]{"_id", "category_name"};
		String selection = null;
		String[] selectionArgs = null;
		String sortOrder = null;
		Cursor query = contentResolver.query(VideoContract.CategoryEntry.CONTENT_URI,projection,selection,selectionArgs,sortOrder);

//        int index = query.getColumnIndex(VideoContract.CategoryEntry.COLUMN_CATEGORY_NAME);
		int currentVideoTablesCount = 0;
		if (query.moveToFirst()) {
			do {
//                String string = query.getString(index);
				currentVideoTablesCount++;
			} while (query.moveToNext());
		}

		contentResolver.bulkInsert(VideoContract.CategoryEntry.CONTENT_URI, downloadedVideoContentValues);

		// 3) import jsonObj: get video list
		JSONArray contentArray_video = jsonObj.getJSONArray("content");
		List<List<ContentValues>> contentList = new ArrayList<>();
		List<ContentValues> videosToInsert_video;

		for (int h = 0; h < contentArray_video.length(); h++) {

			JSONObject contentObj = contentArray_video.getJSONObject(h);

			JSONArray pageArray = contentObj.getJSONArray(TAG_LINK_PAGE);

			videosToInsert_video = new ArrayList<>();

			for (int i = 0; i < pageArray.length(); i++) {

				JSONArray linksArray;

				JSONObject page = pageArray.getJSONObject(i);
				String rowTitle = page.getString(TAG_TITLE);
				linksArray = page.getJSONArray(TAG_MEDIA);

				// links
				for (int j = 0; j < linksArray.length(); j++) {
					JSONObject link = linksArray.getJSONObject(j);

					String linkTitle = link.optString("note_title");

					String linkUrl = (String) link.opt("note_link_uri"); // Get the first link only.

					// card image Url: YouTube or HTML
					String cardImageUrl;
					cardImageUrl = (String) link.opt("note_image_uri");

					// for YouTube link
					if(( !linkUrl.contains("playlist") && (linkUrl.contains("youtube") || linkUrl.contains("youtu.be")) ) ) {
						cardImageUrl = "https://img.youtube.com/vi/" + Utils.getYoutubeId(linkUrl) + "/0.jpg";
					}
					// for HTML link
					else if(cardImageUrl == null)
					{
						Uri uri = Uri.parse("android.resource://" + mContext.getResources().getDrawable(R.drawable.movie, null));
						cardImageUrl = uri.getPath();
					}

					ContentValues videoValues = new ContentValues();
					videoValues.put(VideoContract.VideoEntry.COLUMN_ROW_TITLE, rowTitle);
					videoValues.put(VideoContract.VideoEntry.COLUMN_LINK_TITLE, linkTitle);
					videoValues.put(VideoContract.VideoEntry.COLUMN_LINK_URL, linkUrl);
					videoValues.put(VideoContract.VideoEntry.COLUMN_THUMB_URL, cardImageUrl);

					if (mContext != null) {
						videoValues.put(VideoContract.VideoEntry.COLUMN_ACTION,
								mContext.getResources().getString(R.string.global_search));
					}

					videosToInsert_video.add(videoValues);
				}
			}

			DbHelper mOpenHelper = new DbHelper(mContext);
			mOpenHelper.setWriteAheadLoggingEnabled(false);

			// Will call DbHelper.onCreate()first time when WritableDatabase is not created yet
			SQLiteDatabase sqlDb;
			sqlDb = mOpenHelper.getWritableDatabase();

			// set new video table Id
			String newVideoTableId = String.valueOf(currentVideoTablesCount + h+1); //Id starts from 1

			// Create new video table to hold videos.
			final String SQL_CREATE_VIDEO_TABLE = "CREATE TABLE IF NOT EXISTS " + VideoContract.VideoEntry.TABLE_NAME.concat(newVideoTableId) + " (" +
					VideoContract.VideoEntry._ID + " INTEGER PRIMARY KEY," +
					VideoContract.VideoEntry.COLUMN_ROW_TITLE + " TEXT NOT NULL, " +
					VideoContract.VideoEntry.COLUMN_LINK_URL + " TEXT NOT NULL, " + // TEXT UNIQUE NOT NULL will make the URL unique.
					VideoContract.VideoEntry.COLUMN_LINK_TITLE + " TEXT NOT NULL, " +
					VideoContract.VideoEntry.COLUMN_THUMB_URL + " TEXT, " +
					VideoContract.VideoEntry.COLUMN_ACTION + " TEXT NOT NULL " +
					" );";

			// Do the creating of the databases.
			sqlDb.execSQL(SQL_CREATE_VIDEO_TABLE);

			contentList.add(videosToInsert_video);
		}

		// 4) import jsonObj: add video list
		List<List<ContentValues>> contentValuesList_video = contentList;

		for(int i=0;i<contentValuesList_video.size();i++) {

			ContentValues[] downloadedVideoContentValues_video =
					contentValuesList_video.get(i).toArray(new ContentValues[contentValuesList_video.get(i).size()]);

			ContentResolver contentResolver_video = mContext.getApplicationContext().getContentResolver();

			VideoProvider.tableId = String.valueOf(currentVideoTablesCount + i + 1);
			contentResolver_video.bulkInsert(VideoContract.VideoEntry.CONTENT_URI, downloadedVideoContentValues_video);
		}

		// 5) start new MainActivity
		Intent new_intent = new Intent(mContext, MainActivity.class);
		new_intent.addFlags(FLAG_ACTIVITY_CLEAR_TASK);
		new_intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
		Objects.requireNonNull(mContext).startActivity(new_intent);
	}

}