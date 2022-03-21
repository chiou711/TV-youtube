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
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import java.util.HashMap;

import androidx.annotation.NonNull;

/**
 * VideoProvider is a ContentProvider that provides videos for the rest of applications.
 */
public class CategoryProvider extends ContentProvider {
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    public DbHelper mOpenHelper;

    // These codes are returned from sUriMatcher#match when the respective Uri matches.
    private static final int VIDEO = 1;
    private static final int CATEGORY = 2;
    private static final int VIDEO_WITH_CATEGORY = 3;
    private static final int SEARCH_SUGGEST = 4;
    private static final int REFRESH_SHORTCUT = 5;

    private static SQLiteQueryBuilder sVideosContainingQueryBuilder;
    private static String[] sVideosContainingQueryColumns;
    private static final HashMap<String, String> sColumnMap = buildColumnMap();
    public ContentResolver mContentResolver;
    Context context;

    @Override
    public boolean onCreate() {
        System.out.println("VideoProvider / _onCreate");
        context = getContext();
        mContentResolver = context.getContentResolver();

        mOpenHelper = new DbHelper(context);
        mOpenHelper.setWriteAheadLoggingEnabled(false);

        updateQueryBuilder();
        return true;
    }

    static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = VideoContract.CONTENT_AUTHORITY;

        // For each type of URI to add, create a corresponding code.
        matcher.addURI(authority, VideoContract.PATH_VIDEO, VIDEO);
        matcher.addURI(authority, VideoContract.PATH_VIDEO + "/*", VIDEO_WITH_CATEGORY);

        // For each type of URI to add, create a corresponding code.
        matcher.addURI(authority, VideoContract.PATH_CATEGORY, CATEGORY);
        matcher.addURI(authority, VideoContract.PATH_CATEGORY + "/*", VIDEO_WITH_CATEGORY);

        // Search related URIs.
        matcher.addURI(authority, "search/" + SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
        matcher.addURI(authority, "search/" + SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST);
        return matcher;
    }

    private Cursor getSuggestions(String query) {
        query = query.toLowerCase();
        return sVideosContainingQueryBuilder.query(
                mOpenHelper.getReadableDatabase(),
                sVideosContainingQueryColumns,
                VideoContract.VideoEntry.COLUMN_LINK_TITLE + " LIKE ? ",
                new String[]{"%" + query + "%"},
                null,
                null,
                null
        );
    }

    private static HashMap<String, String> buildColumnMap() {
        HashMap<String, String> map = new HashMap<>();
        map.put(VideoContract.CategoryEntry._ID, VideoContract.CategoryEntry._ID);
        map.put(VideoContract.CategoryEntry.COLUMN_CATEGORY_NAME, VideoContract.CategoryEntry.COLUMN_CATEGORY_NAME);
        map.put(VideoContract.CategoryEntry.COLUMN_VIDEO_TABLE_ID, VideoContract.CategoryEntry.COLUMN_VIDEO_TABLE_ID);
        return map;
    }

    void updateQueryBuilder()
    {
        sVideosContainingQueryBuilder = new SQLiteQueryBuilder();
        sVideosContainingQueryBuilder.setTables(VideoContract.CategoryEntry.TABLE_NAME);
        sVideosContainingQueryBuilder.setProjectionMap(sColumnMap);
        sVideosContainingQueryColumns = new String[]{
                VideoContract.CategoryEntry._ID,
                VideoContract.CategoryEntry.COLUMN_CATEGORY_NAME,
                VideoContract.CategoryEntry.COLUMN_VIDEO_TABLE_ID,
        };
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        System.out.println("VideoProvider / _query/ uri =  " + uri.toString());

        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            case CATEGORY: {
                System.out.println("VideoProvider / _query/ case CATEGORY  " );
                retCursor = mOpenHelper.getReadableDatabase().query(
                        VideoContract.CategoryEntry.TABLE_NAME,//todo temp
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }

        retCursor.setNotificationUri(mContentResolver, uri);
        return retCursor;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        switch (sUriMatcher.match(uri)) {
            // The application is querying the db for its own contents.
            case VIDEO_WITH_CATEGORY:
                return VideoContract.VideoEntry.CONTENT_TYPE;
            case VIDEO:
                return VideoContract.VideoEntry.CONTENT_TYPE;
            case CATEGORY:
                return VideoContract.CategoryEntry.CONTENT_TYPE;
            // The Android TV global search is querying our app for relevant content.
            case SEARCH_SUGGEST:
                return SearchManager.SUGGEST_MIME_TYPE;
            case REFRESH_SHORTCUT:
                return SearchManager.SHORTCUT_MIME_TYPE;

            // We aren't sure what is being asked of us.
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        System.out.println("VideoProvider / _insert/ uri = " + uri.toString());
        final Uri returnUri;
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case VIDEO: {
                long _id = mOpenHelper.getWritableDatabase().insert(
                        VideoContract.VideoEntry.TABLE_NAME, null, values);
                if (_id > 0) {
                    returnUri = VideoContract.VideoEntry.buildVideoUri(_id);
                } else {
                    throw new SQLException("Failed to insert row into " + uri);
                }
                break;
            }
            case CATEGORY: {
                long _id = mOpenHelper.getWritableDatabase().insert(
                        VideoContract.CategoryEntry.TABLE_NAME, null, values);
                if (_id > 0) {
                    returnUri = VideoContract.CategoryEntry.buildCategoryUri(_id);
                } else {
                    throw new SQLException("Failed to insert row into " + uri);
                }
                break;
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }

        mContentResolver.notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        final int rowsDeleted;

        if (selection == null) {
            throw new UnsupportedOperationException("Cannot delete without selection specified.");
        }

        switch (sUriMatcher.match(uri)) {
            case VIDEO: {
                rowsDeleted = mOpenHelper.getWritableDatabase().delete(
                        VideoContract.VideoEntry.TABLE_NAME.concat(tableId), selection, selectionArgs);
                break;
            }
            case CATEGORY: {
                rowsDeleted = mOpenHelper.getWritableDatabase().delete(
                        VideoContract.CategoryEntry.TABLE_NAME, selection, selectionArgs);
                break;
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }

        if (rowsDeleted != 0)
            mContentResolver.notifyChange(uri, null);

        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        final int rowsUpdated;

        switch (sUriMatcher.match(uri)) {
            case VIDEO: {
                rowsUpdated = mOpenHelper.getWritableDatabase().update(
                        VideoContract.VideoEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            }
            case CATEGORY: {
                rowsUpdated = mOpenHelper.getWritableDatabase().update(
                        VideoContract.CategoryEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
        }

        if (rowsUpdated != 0) {
            mContentResolver.notifyChange(uri, null);
        }

        return rowsUpdated;
    }

    public static String tableId;

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        System.out.println("VideoProvider / _bulkInsert / uri = " + uri.toString());
        switch (sUriMatcher.match(uri)) {
            case VIDEO: {
                final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                int returnCount = 0;

                db.beginTransaction();
                try {
                    for (ContentValues value : values) {
                        long _id = db.insertWithOnConflict(VideoContract.VideoEntry.TABLE_NAME.concat(tableId),
                                null, value, SQLiteDatabase.CONFLICT_REPLACE);

                        System.out.println("VideoProvider / _bulkInsert (case VIDEO) / _id = " + _id);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }

                mContentResolver.notifyChange(uri, null);
                return returnCount;
            }
            case CATEGORY: {
                final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                int returnCount = 0;

                db.beginTransaction();
                try {
                    for (ContentValues value : values) {
                        long _id = db.insertWithOnConflict(VideoContract.CategoryEntry.TABLE_NAME,
                                null, value, SQLiteDatabase.CONFLICT_REPLACE);

                        System.out.println("VideoProvider / _bulkInsert (case CATEGORY) / _id = " + _id);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }

                mContentResolver.notifyChange(uri, null);
                return returnCount;
            }
            default: {
                return super.bulkInsert(uri, values);
            }
        }
    }
}
