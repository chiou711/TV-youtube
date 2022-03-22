/*
 * Copyright (c) 2015 The Android Open Source Project
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

package com.cw.tv_yt;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.widget.Toast;

import com.cw.tv_yt.data.DbHelper;
import com.cw.tv_yt.data.VideoContract;
import com.cw.tv_yt.data.VideoProvider;
import com.cw.tv_yt.define.Define;
import com.cw.tv_yt.ui.MainActivity;
import com.cw.tv_yt.ui.MainFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.fragment.app.FragmentActivity;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.cw.tv_yt.data.DbBuilder_video.TAG_LINK_PAGE;
import static com.cw.tv_yt.data.DbBuilder_video.TAG_MEDIA;
import static com.cw.tv_yt.data.DbBuilder_video.TAG_TITLE;

/**
 * A collection of utility methods, all static.
 */
public class Utils {

    public interface MediaDimensions {
        double MEDIA_HEIGHT = 0.95;
        double MEDIA_WIDTH = 0.95;
        double MEDIA_TOP_MARGIN = 0.025;
        double MEDIA_RIGHT_MARGIN = 0.025;
        double MEDIA_BOTTOM_MARGIN = 0.025;
        double MEDIA_LEFT_MARGIN = 0.025;
    }

    /*
     * Making sure public utility methods remain static
     */
    private Utils() {
    }

    public static String getYoutubeId(String url) {

        String videoId = "";

        if (url != null && url.trim().length() > 0 && url.startsWith("http")) {
            String expression = "^.*((youtu.be\\/)|(v\\/)|(\\/u\\/w\\/)|(embed\\/)|(watch\\?))\\??(v=)?([^#\\&\\?]*).*";
            CharSequence input = url;
            Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);//??? some Urls are NG
            Matcher matcher = pattern.matcher(input);
            if (matcher.matches()) {
                String groupIndex1 = matcher.group(8);
                if (groupIndex1 != null && groupIndex1.length() == 11)
                    videoId = groupIndex1;
            }
        }
        return videoId;
    }

    // get preference video table ID
    public static int getPref_video_table_id(Context context)
    {
        String catName = getPref_category_name(context);
        return getVideoTableId_byCategoryName(context,catName);
    }

    // set link source number
    public static void setPref_link_source_number(Context context, int linkSrcNumber ){
        SharedPreferences pref = context.getSharedPreferences("link_src", 0);
        String keyName = "link_source_number";
        pref.edit().putInt(keyName, linkSrcNumber).apply();
    }

    // get link source number
    // Note:  after new installation, link source number
    // 1 dedicated for Default: apply this
    // 2 dedicated for Local: not ready
    public static int getPref_link_source_number (Context context) {
        SharedPreferences pref = context.getSharedPreferences("link_src", 0);
        String keyName = "link_source_number";
        return pref.getInt(keyName, Define.INIT_SOURCE_LINK_NUMBER);
    }

    // set preference category name
    public static void setPref_category_name(Context context, String name ){
        SharedPreferences pref = context.getSharedPreferences("category", 0);
        String keyName = "category_name";
        pref.edit().putString(keyName, name).apply();
    }

    // get preference category name
    public static String getPref_category_name(Context context )
    {
        SharedPreferences pref = context.getSharedPreferences("category", 0);
        String keyName = "category_name";
        return pref.getString(keyName, "no name"); // folder table Id: default is 1
    }

    // remove key of preference category name
    public static void removePref_category_name(Context context){
        SharedPreferences pref = context.getSharedPreferences("category", 0);
        String keyName = "category_name";
        pref.edit().remove(keyName).apply();
    }

    // get video tables count
    public static int getVideoTablesCount(Context context){
        // get video tables count
        DbHelper mOpenHelper = new DbHelper(context);
        mOpenHelper.setWriteAheadLoggingEnabled(false);
        SQLiteDatabase sqlDb = mOpenHelper.getReadableDatabase();

        String SQL_GET_ALL_TABLES = "SELECT * FROM sqlite_master WHERE name like 'video%'";
        Cursor cursor = sqlDb.rawQuery(SQL_GET_ALL_TABLES, null);
        int countVideoTables = cursor.getCount();
        cursor.close();
        sqlDb.close();
        return countVideoTables;
    }

    // Get YouTube playlist Id
    public static String getYoutubePlaylistId(String url) {

        String videoId = "";

        if (url != null && url.trim().length() > 0 && url.startsWith("http")) {
            String expression = "^.*((youtu.be/)|(v/)|(/u/w/)|(embed/)|(playlist\\?))\\??v?=?([^#&?]*).*list?=?([^#&?]*).*";
            CharSequence input = url;
            Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(input);
            if (matcher.matches()) {
                String groupIndex1 = matcher.group(8);
                if (groupIndex1 != null )
                    videoId = groupIndex1;
            }
        }
        System.out.println("Util / _getYoutubePlaylistId / playlist_id = " + videoId);
        return videoId;
    }

    // get App default storage directory name
    static public String getStorageDirName(Context context)
    {
        Resources currentResources = context.getResources();
        Configuration conf = new Configuration(currentResources.getConfiguration());
        conf.locale = Locale.ENGLISH; // apply English to avoid reading directory error
        Resources newResources = new Resources(context.getAssets(),
                currentResources.getDisplayMetrics(),
                conf);
        String dirName = newResources.getString(R.string.dir_name);

        // restore locale
        new Resources(context.getAssets(),
                currentResources.getDisplayMetrics(),
                currentResources.getConfiguration());

        System.out.println("Utils / _getStorageDirName / dirName = " + dirName);
        return dirName;
    }

    // is Empty string
    public static boolean isEmptyString(String str)
    {
        boolean empty = true;
        if( str != null )
        {
            if(str.length() > 0 )
                empty = false;
        }
        return empty;
    }

    //
    // parse JSON object and insert content to DB tables
    //
    public static void parseJsonAndInsertDB(Context mContext, JSONObject jsonObj) throws JSONException
    {
        System.out.println("ParseJsonToDB / _parseJsonAndInsertDB / jsonObj string = " + jsonObj.toString());

        // 1) get current video* tables count
        ContentResolver contentResolver = mContext.getApplicationContext().getContentResolver();
        String[] projection = new String[]{"_id", "category_name", "video_table_id"};
        String selection = null;
        String[] selectionArgs = null;
        String sortOrder = null;
        Cursor query = contentResolver.query(VideoContract.CategoryEntry.CONTENT_URI,projection,selection,selectionArgs,sortOrder);

        // get unique video table ID (base on current biggest one)
        int biggestVideoTableId = 0;
        if (query.moveToFirst()) {
            do {
                String columnStr = VideoContract.CategoryEntry.COLUMN_VIDEO_TABLE_ID;
                int index = query.getColumnIndex(columnStr);
                int pointedVideoTableId = query.getInt(index);
                if(pointedVideoTableId >= biggestVideoTableId)
                    biggestVideoTableId = pointedVideoTableId;

            } while (query.moveToNext());
        }

        query.close();

        // 2) import jsonObj: get category list
        JSONArray contentArray_cat = jsonObj.getJSONArray("content");
        List<ContentValues> videosToInsert_cat = new ArrayList<>();

        for (int h = 0; h < contentArray_cat.length(); h++) {

            JSONObject contentObj = contentArray_cat.getJSONObject(h);

            // category name
            String category_name = contentObj.getString("category");

            // video table Id
            int video_table_id = biggestVideoTableId + h + 1;

            // add suffix for duplicated category name
            int duplicatedTimes = MainFragment.getCategoryNameDuplicatedTimes(category_name);
            if(duplicatedTimes > 0) {
                category_name = category_name.concat(String.valueOf(duplicatedTimes));
//                video_table_id += duplicatedTimes;
            }

            // save category names
            ContentValues categoryValues = new ContentValues();
            categoryValues.put("category_name", category_name);
            categoryValues.put("video_table_id", video_table_id);
            videosToInsert_cat.add(categoryValues);
        }

        // 3) import jsonObj: add category list
        List<ContentValues> contentValuesList = videosToInsert_cat;

        ContentValues[] downloadedVideoContentValues =
                contentValuesList.toArray(new ContentValues[contentValuesList.size()]);

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
            String newVideoTableId = String.valueOf(biggestVideoTableId + h+1); //Id starts from 1

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

            VideoProvider.tableId = String.valueOf(biggestVideoTableId + i + 1);
            contentResolver_video.bulkInsert(VideoContract.VideoEntry.CONTENT_URI, downloadedVideoContentValues_video);
        }

        // 6) start new MainActivity
        Intent new_intent = new Intent(mContext, MainActivity.class);
        new_intent.addFlags(FLAG_ACTIVITY_CLEAR_TASK);
        new_intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        Objects.requireNonNull(mContext).startActivity(new_intent);
    }

    // Get video_table_id by category name
    public static int getVideoTableId_byCategoryName(Context act,String categoryName){
        int videoTableId = 0;

        System.out.println("Utils / _getVideoTableId_byCategoryName /  categoryName =　" + categoryName);

        // initial video table ID
        if(categoryName.equalsIgnoreCase("no name"))
            return Define.INIT_CATEGORY_NUMBER;

        DbHelper mOpenHelper = new DbHelper(act);
        mOpenHelper.setWriteAheadLoggingEnabled(false);

        SQLiteDatabase sqlDb;
        sqlDb = mOpenHelper.getReadableDatabase();

        Cursor cursor = sqlDb.query(
                "category",
                new String[]{"video_table_id"},
                "category_name="+"\'"+ categoryName+"\'",
                null,
                null,
                null,
                null);

        cursor.moveToFirst();

        try {
            int columnIndex = cursor.getColumnIndex(VideoContract.CategoryEntry.COLUMN_VIDEO_TABLE_ID);
            videoTableId = cursor.getInt(columnIndex);
        }catch (Exception e){
            e.printStackTrace();
            System.out.println(" cursor get int : error !");
        }

        cursor.close();
        sqlDb.close();
        mOpenHelper.close();

        return videoTableId;
    }

    // delete selected category
    public static void deleteSelectedCategory(FragmentActivity act, List<String> mCategoryNames, String item){
        DbHelper mOpenHelper = new DbHelper(act);
        mOpenHelper.setWriteAheadLoggingEnabled(false);
        SQLiteDatabase sqlDb = mOpenHelper.getWritableDatabase();

        // get video table ID
        int videoTableId = Utils.getVideoTableId_byCategoryName(act.getApplicationContext(),(String)item);
        System.out.println("Utils / _deleteSelectedCategory / videoTableId = " + videoTableId);

        // Drop video table
        final String SQL_DROP_VIDEO_TABLE = "DROP TABLE IF EXISTS " +
                VideoContract.VideoEntry.TABLE_NAME.concat(String.valueOf(videoTableId));

//        System.out.println(" SQL_DROP_VIDEO_TABLE = " + SQL_DROP_VIDEO_TABLE);

        // Do the creating of the databases.
        sqlDb.execSQL(SQL_DROP_VIDEO_TABLE);
        sqlDb.close();
        mOpenHelper.close();

        // delete current row in category table after drop its video table
        ContentResolver contentResolver = act.getApplicationContext().getContentResolver();
        contentResolver.delete(VideoContract.CategoryEntry.CONTENT_URI,
                "category_name=" + "\'"+(String)item+"\'" ,
                null);

        Intent returnIntent = new Intent();
        returnIntent.putExtra("KEY_DELETE", Pref.ACTION_DELETE);
        act.setResult( Activity.RESULT_OK, returnIntent);

        // show toast
        act.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(act, act.getString(R.string.database_delete_item), Toast.LENGTH_SHORT).show();
            }
        });

        // update category names, in order get next available category name
        for(int i=0;i< mCategoryNames.size();i++){
            if(mCategoryNames.get(i).equalsIgnoreCase((String)item))
                mCategoryNames.remove(i);
        }

        // update focus with first category name
        Utils.setPref_category_name(act,mCategoryNames.get(0));

        // start new MainActivity
        Intent new_intent = new Intent(act, MainActivity.class);
        new_intent.addFlags(FLAG_ACTIVITY_CLEAR_TASK);
        new_intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        act.startActivity(new_intent);
        act.finish();
    }
}
