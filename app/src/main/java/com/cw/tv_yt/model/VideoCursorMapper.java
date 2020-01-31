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

package com.cw.tv_yt.model;

import android.database.Cursor;
import androidx.leanback.database.CursorMapper;

import com.cw.tv_yt.data.VideoContract;

/**
 * VideoCursorMapper maps a database Cursor to a Video object.
 */
public final class VideoCursorMapper extends CursorMapper {

    private static int idIndex;
    private static int linkTitleIndex;
    private static int linkUrlIndex;
    private static int cardImageUrlIndex;
    private static int rowTitleIndex;

    @Override
    protected void bindColumns(Cursor cursor) {
        idIndex = cursor.getColumnIndex(VideoContract.VideoEntry._ID);
        linkTitleIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_LINK_TITLE);
        linkUrlIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_LINK_URL);
        rowTitleIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_ROW_TITLE);
        cardImageUrlIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_THUMB_URL);
    }

    @Override
    protected Object bind(Cursor cursor) {

        // Get the values of the video.
        long id = cursor.getLong(idIndex);
        String rowTitle = cursor.getString(rowTitleIndex);
        String linkTitle = cursor.getString(linkTitleIndex);
        String linkUrl = cursor.getString(linkUrlIndex);
        String bgImageUrl = "android.resource://com.cw.tv_yt/drawable/image";
        String cardImageUrl = cursor.getString(cardImageUrlIndex);

        // Build a Video object to be processed.
        return new Video.VideoBuilder()
                .id(id)
                .rowTitle(rowTitle)
                .linkUrl(linkUrl)
                .linkTitle(linkTitle)
                .bgImageUrl(bgImageUrl)
                .cardImageUrl(cardImageUrl)
                .build();
    }
}
