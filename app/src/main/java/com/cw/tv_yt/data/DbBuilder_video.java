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
package com.cw.tv_yt.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.cw.tv_yt.R;
import com.cw.tv_yt.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import androidx.annotation.NonNull;

/**
 * The VideoDbBuilder is used to grab a JSON file from a server and parse the data
 * to be placed into a local database
 */
public class DbBuilder_video {
    public static final String TAG_LINK_PAGE = "link_page";//"googlevideos";
    public static final String TAG_MEDIA = "links";
    public static final String TAG_TITLE = "title";

    private static final String TAG = "VideoDbBuilder";

    private Context mContext;

    /**
     * Default constructor that can be used for tests
     */

    public DbBuilder_video(Context mContext) {
        this.mContext = mContext;
    }

    /**
     * Fetches JSON data representing videos from a server and populates that in a database
     * @param url The location of the video list
     */
    public @NonNull List<List<ContentValues>> fetch(String url)
            throws IOException, JSONException {
        JSONObject videoData = fetchJSON(url);
        System.out.println("VideoDbBuilder / _fetch / videoData length = " + videoData.length()) ;
        return buildMedia(videoData);
    }

    /**
     * Takes the contents of a JSON object and populates the database
     * @param jsonObj The JSON object of videos
     * @throws JSONException if the JSON object is invalid
     */
    public List<List<ContentValues>> buildMedia(JSONObject jsonObj) throws JSONException {

        System.out.println("VideoDbBuilder / _buildMedia / jsonObj.toString = " + jsonObj.toString());

        JSONArray contentArray = jsonObj.getJSONArray("content");
        List<List<ContentValues>> contentList = new ArrayList<>();
        List<ContentValues> videosToInsert = null;
        System.out.println("VideoDbBuilder / _buildMedia / contentArray.length() = " + contentArray.length());

        for (int h = 0; h < contentArray.length(); h++) {

            JSONObject contentObj = contentArray.getJSONObject(h);

            JSONArray pageArray = contentObj.getJSONArray(TAG_LINK_PAGE);

            videosToInsert = new ArrayList<>();

            for (int i = 0; i < pageArray.length(); i++) {

                JSONArray linksArray;

                JSONObject page = pageArray.getJSONObject(i);
                String rowTitle = page.getString(TAG_TITLE);

                System.out.println("DbBuilder_video / _buildMedia / pageTitle = " + rowTitle);

                linksArray = page.getJSONArray(TAG_MEDIA);

                // links
                for (int j = 0; j < linksArray.length(); j++) {
                    JSONObject link = linksArray.getJSONObject(j);

                    String linkTitle = link.optString("note_title");
                    System.out.println("DbBuilder_video / _buildMedia / linkTitle = " + linkTitle);

                    String linkUrl = (String) link.opt("note_link_uri"); // Get the first link only.

                    // card image Url: YouTube or HTML
                    String cardImageUrl;
                    cardImageUrl = (String) link.opt("note_image_uri");
                    System.out.println("DbBuilder_video / _buildMedia / cardImageUrl = " + cardImageUrl);

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

                    videosToInsert.add(videoValues);
                }

            }

            DbHelper mOpenHelper = new DbHelper(mContext);
            mOpenHelper.setWriteAheadLoggingEnabled(false);

            // Will call DbHelper.onCreate()first time when WritableDatabase is not created yet
            SQLiteDatabase sqlDb;
            sqlDb = mOpenHelper.getWritableDatabase();
            String tableId = String.valueOf(h+1); //Id starts from 1
            // Create a table to hold videos.
            final String SQL_CREATE_VIDEO_TABLE = "CREATE TABLE IF NOT EXISTS " + VideoContract.VideoEntry.TABLE_NAME.concat(tableId) + " (" +
                    VideoContract.VideoEntry._ID + " INTEGER PRIMARY KEY," +
                    VideoContract.VideoEntry.COLUMN_ROW_TITLE + " TEXT NOT NULL, " +
                    VideoContract.VideoEntry.COLUMN_LINK_URL + " TEXT NOT NULL, " + // TEXT UNIQUE NOT NULL will make the URL unique.
                    VideoContract.VideoEntry.COLUMN_LINK_TITLE + " TEXT NOT NULL, " +
                    VideoContract.VideoEntry.COLUMN_THUMB_URL + " TEXT, " +
                    VideoContract.VideoEntry.COLUMN_ACTION + " TEXT NOT NULL " +
                    " );";

            // Do the creating of the databases.
            sqlDb.execSQL(SQL_CREATE_VIDEO_TABLE);

            contentList.add(videosToInsert);
        }
        return contentList;
    }

    /**
     * Fetch JSON object from a given URL.
     *
     * @return the JSONObject representation of the response
     * @throws JSONException
     * @throws IOException
     */
    private JSONObject fetchJSON(String urlString) throws JSONException, IOException {
        System.out.println("DbBuilder_video / fetchJSON / urlString = " + urlString);

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