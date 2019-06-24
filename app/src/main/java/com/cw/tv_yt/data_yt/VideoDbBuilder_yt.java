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

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.media.Rating;
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
public class VideoDbBuilder_yt {
    public static final String TAG_LINK_PAGE = "link_page";//"googlevideos";
    public static final String TAG_MEDIA = "links";
    public static final String TAG_TITLE = "title";

    private static final String TAG = "VideoDbBuilder";

    private Context mContext;

    /**
     * Default constructor that can be used for tests
     */

    public VideoDbBuilder_yt(Context mContext) {
        this.mContext = mContext;
    }

    /**
     * Fetches JSON data representing videos from a server and populates that in a database
     * @param url The location of the video list
     */
    public @NonNull List<List<ContentValues>> fetch(String url)
            throws IOException, JSONException {
        JSONObject videoData = fetchJSON(url);
        System.out.println("VideoDbBuilder_yt / _fetch / videoData length = " + videoData.length()) ;
        return buildMedia(videoData);
    }

    /**
     * Takes the contents of a JSON object and populates the database
     * @param jsonObj The JSON object of videos
     * @throws JSONException if the JSON object is invalid
     */
    public List<List<ContentValues>> buildMedia(JSONObject jsonObj) throws JSONException {

        System.out.println("VideoDbBuilder_yt / _buildMedia / jsonObj.toString = " + jsonObj.toString());

        JSONArray contentArray = jsonObj.getJSONArray("content");
        List<List<ContentValues>> contentList = new ArrayList<>();
        List<ContentValues> videosToInsert = null;
        System.out.println("VideoDbBuilder_yt / _buildMedia / contentArray.length() = " + contentArray.length());

        for (int h = 0; h < contentArray.length(); h++) {

            JSONObject contentObj = contentArray.getJSONObject(h);

            JSONArray categoryArray = contentObj.getJSONArray(TAG_LINK_PAGE);

            videosToInsert = new ArrayList<>();

            for (int i = 0; i < categoryArray.length(); i++) {

                JSONArray videoArray;

                JSONObject category = categoryArray.getJSONObject(i);
                String titleName = category.getString(TAG_TITLE);
                videoArray = category.getJSONArray(TAG_MEDIA);

                ///
                // links
                for (int j = 0; j < videoArray.length(); j++) {
                    JSONObject video = videoArray.getJSONObject(j);

                    String title = video.optString("note_title");

                    String description = "DESCRIPTION";

                    String videoUrl = (String) video.opt("note_link_uri"); // Get the first video only.

                    Uri myURI = Uri.parse("android.resource://com.cw.tv_yt/" + R.drawable.image);
                    String bgImageUrl = myURI.toString();

                    String cardImageUrl = "http://img.youtube.com/vi/" + Utils.getYoutubeId(videoUrl) + "/0.jpg";

                    String studio = "STUDIO";

                    ContentValues videoValues = new ContentValues();
                    videoValues.put(VideoContract_yt.VideoEntry.COLUMN_TITLE, titleName);
                    videoValues.put(VideoContract_yt.VideoEntry.COLUMN_NAME, title);
                    videoValues.put(VideoContract_yt.VideoEntry.COLUMN_DESC, description);
                    videoValues.put(VideoContract_yt.VideoEntry.COLUMN_VIDEO_URL, videoUrl);
                    videoValues.put(VideoContract_yt.VideoEntry.COLUMN_CARD_IMG, cardImageUrl);
                    videoValues.put(VideoContract_yt.VideoEntry.COLUMN_BG_IMAGE_URL, bgImageUrl);
                    videoValues.put(VideoContract_yt.VideoEntry.COLUMN_STUDIO, studio);

                    // Fixed defaults.
                    videoValues.put(VideoContract_yt.VideoEntry.COLUMN_CONTENT_TYPE, "video/mp4");
                    videoValues.put(VideoContract_yt.VideoEntry.COLUMN_IS_LIVE, false);
                    videoValues.put(VideoContract_yt.VideoEntry.COLUMN_AUDIO_CHANNEL_CONFIG, "2.0");
                    videoValues.put(VideoContract_yt.VideoEntry.COLUMN_PRODUCTION_YEAR, 2014);
                    videoValues.put(VideoContract_yt.VideoEntry.COLUMN_DURATION, 0);
                    videoValues.put(VideoContract_yt.VideoEntry.COLUMN_RATING_STYLE, Rating.RATING_5_STARS);
                    videoValues.put(VideoContract_yt.VideoEntry.COLUMN_RATING_SCORE, 3.5f);
                    if (mContext != null) {
                        videoValues.put(VideoContract_yt.VideoEntry.COLUMN_PURCHASE_PRICE,
                                mContext.getResources().getString(R.string.buy_2));
                        videoValues.put(VideoContract_yt.VideoEntry.COLUMN_RENTAL_PRICE,
                                mContext.getResources().getString(R.string.rent_2));
                        videoValues.put(VideoContract_yt.VideoEntry.COLUMN_ACTION,
                                mContext.getResources().getString(R.string.global_search));
                    }

                    // TODO: Get these dimensions.
                    videoValues.put(VideoContract_yt.VideoEntry.COLUMN_VIDEO_WIDTH, 1280);
                    videoValues.put(VideoContract_yt.VideoEntry.COLUMN_VIDEO_HEIGHT, 720);

                    videosToInsert.add(videoValues);
                }

            }

            VideoDbHelper_yt mOpenHelper = new VideoDbHelper_yt(mContext);

            // Will call VideoDbHelper_yt.onCreate()first time when WritableDatabase is not created yet
            SQLiteDatabase sqlDb;
            sqlDb = mOpenHelper.getWritableDatabase();
            String tableId = String.valueOf(h+1); //Id starts from 1
            // Create a table to hold videos.
            final String SQL_CREATE_VIDEO_TABLE = "CREATE TABLE IF NOT EXISTS " + VideoContract_yt.VideoEntry.TABLE_NAME.concat(tableId) + " (" +
                    VideoContract_yt.VideoEntry._ID + " INTEGER PRIMARY KEY," +
                    VideoContract_yt.VideoEntry.COLUMN_TITLE + " TEXT NOT NULL, " +
                    VideoContract_yt.VideoEntry.COLUMN_VIDEO_URL + " TEXT UNIQUE NOT NULL, " + // Make the URL unique.
                    VideoContract_yt.VideoEntry.COLUMN_NAME + " TEXT NOT NULL, " +
                    VideoContract_yt.VideoEntry.COLUMN_DESC + " TEXT NOT NULL, " +
                    VideoContract_yt.VideoEntry.COLUMN_BG_IMAGE_URL + " TEXT NOT NULL, " +
                    VideoContract_yt.VideoEntry.COLUMN_STUDIO + " TEXT NOT NULL, " +
                    VideoContract_yt.VideoEntry.COLUMN_CARD_IMG + " TEXT NOT NULL, " +
                    VideoContract_yt.VideoEntry.COLUMN_CONTENT_TYPE + " TEXT NOT NULL, " +
                    VideoContract_yt.VideoEntry.COLUMN_IS_LIVE + " INTEGER DEFAULT 0, " +
                    VideoContract_yt.VideoEntry.COLUMN_VIDEO_WIDTH + " INTEGER NOT NULL, " +
                    VideoContract_yt.VideoEntry.COLUMN_VIDEO_HEIGHT + " INTEGER NOT NULL, " +
                    VideoContract_yt.VideoEntry.COLUMN_AUDIO_CHANNEL_CONFIG + " TEXT NOT NULL, " +
                    VideoContract_yt.VideoEntry.COLUMN_PURCHASE_PRICE + " TEXT NOT NULL, " +
                    VideoContract_yt.VideoEntry.COLUMN_RENTAL_PRICE + " TEXT NOT NULL, " +
                    VideoContract_yt.VideoEntry.COLUMN_RATING_STYLE + " TEXT NOT NULL, " +
                    VideoContract_yt.VideoEntry.COLUMN_RATING_SCORE + " TEXT NOT NULL, " +
                    VideoContract_yt.VideoEntry.COLUMN_PRODUCTION_YEAR + " TEXT NOT NULL, " +
                    VideoContract_yt.VideoEntry.COLUMN_DURATION + " TEXT NOT NULL, " +
                    VideoContract_yt.VideoEntry.COLUMN_ACTION + " TEXT NOT NULL " +
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
        System.out.println("VideoDbBuilder_yt / fetchJSON / urlString = " + urlString);

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