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
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Point;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.VideoView;

import com.cw.tv_yt.data.DbHelper;

import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /**
     * Returns the screen/display size.
     */
    public static Point getDisplaySize(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        // You can get the height & width like such:
        // int width = size.x;
        // int height = size.y;
        return size;
    }

    public static int convertDpToPixel(Context ctx, int dp) {
        float density = ctx.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    /**
     * Example for handling resizing content for overscan.  Typically you won't need to resize when
     * using the Leanback support library.
     */
    public void overScan(Activity activity, VideoView videoView) {
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int w = (int) (metrics.widthPixels * MediaDimensions.MEDIA_WIDTH);
        int h = (int) (metrics.heightPixels * MediaDimensions.MEDIA_HEIGHT);
        int marginLeft = (int) (metrics.widthPixels * MediaDimensions.MEDIA_LEFT_MARGIN);
        int marginTop = (int) (metrics.heightPixels * MediaDimensions.MEDIA_TOP_MARGIN);
        int marginRight = (int) (metrics.widthPixels * MediaDimensions.MEDIA_RIGHT_MARGIN);
        int marginBottom = (int) (metrics.heightPixels * MediaDimensions.MEDIA_BOTTOM_MARGIN);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w, h);
        lp.setMargins(marginLeft, marginTop, marginRight, marginBottom);
        videoView.setLayoutParams(lp);
    }

    public static long getDuration(String videoUrl) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            mmr.setDataSource(videoUrl, new HashMap<>());
        } else {
            mmr.setDataSource(videoUrl);
        }
        return Long.parseLong(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
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

    // set category number
    public static void setPref_focus_category_number(Context context, int cateNumber )
    {
        SharedPreferences pref = context.getSharedPreferences("category", 0);
        String keyName = "current_category_number";
        pref.edit().putInt(keyName, cateNumber).apply();
    }

    // get category number
    public static int getPref_focus_category_number (Context context)
    {
        SharedPreferences pref = context.getSharedPreferences("category", 0);
        String keyName = "current_category_number";
        return pref.getInt(keyName, 1); // focus table Id: default is 1
    }

    // set link source number
    public static void setPref_link_source_number(Context context, int linkSrcNumber )
    {
        SharedPreferences pref = context.getSharedPreferences("link_src", 0);
        String keyName = "link_source_number";
        pref.edit().putInt(keyName, linkSrcNumber).apply();
    }

    // get link source number
    // Note:  after new installation, link source number
    // 1 dedicated for Default: apply this
    // 2 dedicated for Local: not ready
    public static int getPref_link_source_number (Context context)
    {
        SharedPreferences pref = context.getSharedPreferences("link_src", 0);
        String keyName = "link_source_number";
        return pref.getInt(keyName, 1);
    }

    // set category name
    public static void setPref_category_name(Context context, int cateNumber, String categoryStr )
    {
        System.out.println("Utils / _setPref_category_name / cateNumber = " + cateNumber);
        SharedPreferences pref = context.getSharedPreferences("category", 0);
        String keyName = "category_name_" + cateNumber;
        pref.edit().putString(keyName, categoryStr).apply();
    }

    // get category name
    public static String getPref_category_name(Context context,int cateNumber)
    {
        SharedPreferences pref = context.getSharedPreferences("category", 0);
        String keyName = "category_name_" + cateNumber;
        return pref.getString(keyName, String.valueOf(cateNumber)); // folder table Id: default is 1
    }

    // remove key of category name
    public static void removePref_category_name(Context context,int index)
    {
        SharedPreferences pref = context.getSharedPreferences("category", 0);
        String keyPrefix = "category_name_";
        String keyName = keyPrefix.concat(String.valueOf(index));
        pref.edit().remove(keyName).apply();
    }

    // remove key of category focus number
    public static void removePref_focus_category_number(Context context)
    {
        SharedPreferences pref = context.getSharedPreferences("category", 0);
        String keyName = "current_category_number";
        pref.edit().remove(keyName).apply();
    }

    // get video tables count
    public static int getVideoTablesCount(Context context)
    {
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
}
