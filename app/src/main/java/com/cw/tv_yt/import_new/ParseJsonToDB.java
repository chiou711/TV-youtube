/*
 * Copyright (C) 2019 CW Chiu
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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.cw.tv_yt.R;
import com.cw.tv_yt.Utils;
import com.cw.tv_yt.data.DbHelper;
import com.cw.tv_yt.data.VideoContract;
import com.cw.tv_yt.data.VideoProvider;
import com.cw.tv_yt.ui.MainActivity;
import com.cw.tv_yt.ui.MainFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.cw.tv_yt.data.VideoDbBuilder.TAG_LINK_PAGE;
import static com.cw.tv_yt.data.VideoDbBuilder.TAG_MEDIA;
import static com.cw.tv_yt.data.VideoDbBuilder.TAG_TITLE;

public class ParseJsonToDB {

    private Context mContext;
    public static boolean isParsing;
    public String fileBody = "";
    String filePath;
    String content;

    ParseJsonToDB(String filePath, Context context)
    {
        mContext = context;
        this.filePath = filePath;
        isParsing = true;
    }

    ParseJsonToDB(Context context, String _content)
    {
        mContext = context;
        isParsing = true;
        content = _content;
    }

    public ParseJsonToDB(Context context)
    {
        mContext = context;
        isParsing = true;
    }

    //
    // parse JSON file and insert content to DB tables
    //
    private void parseJsonFileAndInsertDB(String filePath) throws JSONException
    {
        final String jsonString = getJsonStringByFile(filePath);
        System.out.println("ParseJsonToDB / _parseJsonFileAndInsertDB / filePath = " + filePath);
        System.out.println("ParseJsonToDB / _parseJsonFileAndInsertDB / jsonString = " + jsonString);
        JSONObject jsonObj = new JSONObject(jsonString);
        parseJsonAndInsertDB(jsonObj);
    }

    //
    // parse JSON string and insert content to DB tables
    //
    private void parseJsonStringAndInsertDB(String content) throws JSONException
    {
        if(content != null) {
            content = content.replaceAll("(?m)^[ \t]*\r?\n", "");
        }

        final String jsonString = content;//getJsonStringByFile(filePath);

//        System.out.println("ParseJsonToDB / _parseJsonFileAndInsertDB / filePath = " + filePath);
//        System.out.println("ParseJsonToDB / _parseJsonFileAndInsertDB / jsonString = " + jsonString);
        JSONObject jsonObj = new JSONObject(jsonString);
        parseJsonAndInsertDB(jsonObj);
    }

    //
    // parse JSON object and insert content to DB tables
    //
    public void parseJsonAndInsertDB(JSONObject jsonObj) throws JSONException
    {
        System.out.println("ParseJsonToDB / _parseJsonAndInsertDB / jsonObj string = " + jsonObj.toString());

        // 1) delete database
//        try {
//            System.out.println("ParseJsonToDB / _parseJsonAndInsertDB / will delete DB");
//            Objects.requireNonNull(mContext).deleteDatabase(DbHelper.DATABASE_NAME);
//
//            ContentResolver resolver = mContext.getContentResolver();
//            ContentProviderClient client = resolver.acquireContentProviderClient(VideoContract.CONTENT_AUTHORITY);
//            assert client != null;
//            VideoProvider provider = (VideoProvider) client.getLocalContentProvider();
//
//            assert provider != null;
//            provider.mContentResolver = resolver;
//            provider.mOpenHelper.close();
//
//            provider.mOpenHelper = new DbHelper(mContext);
//            provider.mOpenHelper.setWriteAheadLoggingEnabled(false);
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
//                client.close();
//            else
//                client.release();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        // 2) import jsonObj: get category list
        JSONArray contentArray_cat = jsonObj.getJSONArray("content");
        List<ContentValues> videosToInsert_cat = new ArrayList<>();

        for (int h = 0; h < contentArray_cat.length(); h++) {

            JSONObject contentObj = contentArray_cat.getJSONObject(h);

            // category name
            String category_name = contentObj.getString("category");

            // add suffix for duplicated category name
            int duplicatedTimes = getCategoryNameDuplicatedTimes(category_name);
            if(duplicatedTimes > 0)
                category_name = category_name.concat(String.valueOf(duplicatedTimes));

            // save category names
            ContentValues categoryValues = new ContentValues();
            categoryValues.put("category_name", category_name);
            videosToInsert_cat.add(categoryValues);
        }

        // 3) import jsonObj: add category list
        List<ContentValues> contentValuesList = videosToInsert_cat;

        ContentValues[] downloadedVideoContentValues =
                contentValuesList.toArray(new ContentValues[contentValuesList.size()]);

        ContentResolver contentResolver = mContext.getApplicationContext().getContentResolver();

        // get current video* tables count
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

        // 4) import jsonObj: get video list
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

        // 5) import jsonObj: add video list
        List<List<ContentValues>> contentValuesList_video = contentList;

        for(int i=0;i<contentValuesList_video.size();i++) {

            ContentValues[] downloadedVideoContentValues_video =
                    contentValuesList_video.get(i).toArray(new ContentValues[contentValuesList_video.get(i).size()]);

            ContentResolver contentResolver_video = mContext.getApplicationContext().getContentResolver();

            VideoProvider.tableId = String.valueOf(currentVideoTablesCount + i + 1);
            contentResolver_video.bulkInsert(VideoContract.VideoEntry.CONTENT_URI, downloadedVideoContentValues_video);
        }

        // 6) start new MainActivity
        Intent new_intent = new Intent(mContext, MainActivity.class);
        new_intent.addFlags(FLAG_ACTIVITY_CLEAR_TASK);
        new_intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        Objects.requireNonNull(mContext).startActivity(new_intent);

        isParsing = false;
    }


    private String getJsonStringByFile(String filePath) {
//        System.out.println("ParseJsonToDB / _getJsonString / filePath = " + filePath);

        File file = new File(filePath);

        FileInputStream fileInputStream = null;
        try
        {
            fileInputStream = new FileInputStream(file);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }


        StringBuilder total = null;
        try
        {
            BufferedReader r = new BufferedReader(new InputStreamReader(fileInputStream));
            total = new StringBuilder();

            for (String line; (line = r.readLine()) != null; )
            {
                total.append(line).append('\n');
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }

        final String jsonString = total.toString();
        System.out.println("ParseJsonToDB / _getJsonString / jsonString = " + jsonString);

        return jsonString;
    }

    void handleParseJsonFileAndInsertDB()
    {
        Thread thread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                   parseJsonFileAndInsertDB(filePath);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    void handleParseJsonStringAndInsertDB(String intent)
    {
        Thread thread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    parseJsonStringAndInsertDB(intent);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    void handleViewJson()
    {
        Thread thread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    fileBody = getJsonStringByFile(filePath);
                    isParsing = false;
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    // get duplicated times of same category name
    // i.e.
    // 1. JSON file is the same
    // 2. category names in DB are different
    int getCategoryNameDuplicatedTimes(String categoryName){
        int size = MainFragment.mCategoryNames.size();
        int duplicatedTimes = 0;

        for(int i=0;i<size;i++) {
            if (MainFragment.mCategoryNames.get(i).contains(categoryName))
                duplicatedTimes++;
        }
        return duplicatedTimes;
    }
}