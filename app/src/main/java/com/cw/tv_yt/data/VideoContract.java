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

import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * VideoContract represents the contract for storing videos in the SQLite database.
 */
public final class VideoContract {

    // The name for the entire content provider.
    public static final String CONTENT_AUTHORITY = "com.cw.tv_yt";

    // Base of all URIs that will be used to contact the content provider.
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // The content paths.
    public static final String PATH_VIDEO = "video";

    public static final class VideoEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_VIDEO).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "." + PATH_VIDEO;

        // Name of the video table.
        public static final String TABLE_NAME = "video";

        // Column with the foreign key into the category table.
        public static final String COLUMN_ROW_TITLE = "row_title";

        // Name of the video.
        public static final String COLUMN_LINK_TITLE = SearchManager.SUGGEST_COLUMN_TEXT_1;

        // The url to the video content.
        public static final String COLUMN_LINK_URL = "link_url";

        // The card image for the video.
        public static final String COLUMN_THUMB_URL = SearchManager.SUGGEST_COLUMN_RESULT_CARD_IMAGE;


        // The action intent for the result.
        public static final String COLUMN_ACTION = SearchManager.SUGGEST_COLUMN_INTENT_ACTION;

        // Returns the Uri referencing a video with the specified id.
        public static Uri buildVideoUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }

    // The content paths.
    public static final String PATH_CATEGORY = "category";
//    public static final String PATH_CATEGORY = "video";

    public static final class CategoryEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_CATEGORY).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "." + PATH_CATEGORY;

        // Name of the video table.
        public static final String TABLE_NAME = "category";

        // Column with the foreign key into the category table.
        public static final String COLUMN_CATEGORY_NAME = "category_name";
        public static final String COLUMN_VIDEO_TABLE_ID = "video_table_id";

        // Returns the Uri referencing a video with the specified id.
        public static Uri buildCategoryUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }


}
