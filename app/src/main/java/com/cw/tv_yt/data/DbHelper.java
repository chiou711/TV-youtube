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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.cw.tv_yt.data.VideoContract.VideoEntry;

/**
 * VideoDbHelper manages the creation and upgrade of the database used in this sample.
 */
public class DbHelper extends SQLiteOpenHelper {

    // Change this when you change the database schema.
    private static final int DATABASE_VERSION = 4;

    // The name of our database.
    public static final String DATABASE_NAME = "tv_yt.db";

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        System.out.println("DbHelper / constructor");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        System.out.println("DbHelper / _onCreate (will create video1 table)");

        // Create a table to hold videos.
        final String SQL_CREATE_VIDEO_TABLE = "CREATE TABLE IF NOT EXISTS " + "video1" + " (" +
                VideoEntry._ID + " INTEGER PRIMARY KEY," +
                VideoEntry.COLUMN_ROW_TITLE + " TEXT NOT NULL, " +
                VideoEntry.COLUMN_LINK_URL + " TEXT NOT NULL, " + // TEXT UNIQUE NOT NULL will make the URL unique.
                VideoEntry.COLUMN_LINK_TITLE + " TEXT NOT NULL, " +
                VideoEntry.COLUMN_THUMB_URL + " TEXT, " +
                VideoEntry.COLUMN_ACTION + " TEXT NOT NULL" +
        " );";

        // Do the creating of the databases.
        db.execSQL(SQL_CREATE_VIDEO_TABLE);

        System.out.println("DbHelper / _onCreate (will create category table)");
        final String SQL_CREATE_CATEGORY_TABLE = "CREATE TABLE IF NOT EXISTS " + VideoContract.CategoryEntry.TABLE_NAME + " (" +
                VideoEntry._ID + " INTEGER PRIMARY KEY," +
                "category_name" + " TEXT NOT NULL " +
                " );";

        // Do the creating of the databases.
        db.execSQL(SQL_CREATE_CATEGORY_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Simply discard all old data and start over when upgrading.
        db.execSQL("DROP TABLE IF EXISTS " + VideoEntry.TABLE_NAME);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Do the same thing as upgrading...
        onUpgrade(db, oldVersion, newVersion);
    }
}