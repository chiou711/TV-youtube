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

import android.media.MediaDescription;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Video is an immutable object that holds the various metadata associated with a single video.
 */
public final class Video implements Parcelable {
    public final long id;
    public final String rowTitle;
    public final String title;
    public final String bgImageUrl;
    public final String cardImageUrl;
    public final String videoUrl;

    private Video(
            final long id,
            final String rowTitle,
            final String title,
            final String videoUrl,
            final String bgImageUrl,
            final String cardImageUrl) {
        this.id = id;
        this.rowTitle = rowTitle;
        this.title = title;
        this.videoUrl = videoUrl;
        this.bgImageUrl = bgImageUrl;
        this.cardImageUrl = cardImageUrl;
    }

    protected Video(Parcel in) {
        id = in.readLong();
        rowTitle = in.readString();
        title = in.readString();
        bgImageUrl = in.readString();
        cardImageUrl = in.readString();
        videoUrl = in.readString();
    }

    public static final Creator<Video> CREATOR = new Creator<Video>() {
        @Override
        public Video createFromParcel(Parcel in) {
            return new Video(in);
        }

        @Override
        public Video[] newArray(int size) {
            return new Video[size];
        }
    };

    @Override
    public boolean equals(Object m) {
        return m instanceof Video && id == ((Video) m).id;
    }

    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(rowTitle);
        dest.writeString(title);
        dest.writeString(bgImageUrl);
        dest.writeString(cardImageUrl);
        dest.writeString(videoUrl);
    }

    @Override
    public String toString() {
        String s = "Video{";
        s += "id=" + id;
        s += ", rowTitle='" + rowTitle + "'";
        s += ", linkTitle='" + title + "'";
        s += ", linkUrl='" + videoUrl + "'";
        s += ", bgImageUrl='" + bgImageUrl + "'";
        s += ", cardImageUrl='" + cardImageUrl + "'";
        s += "}";
        return s;
    }

    // Builder for Video object.
    public static class VideoBuilder {
        private long id;
        private String rowTitle;
        private String title;
        private String bgImageUrl;
        private String cardImageUrl;
        private String videoUrl;

        public VideoBuilder id(long id) {
            this.id = id;
            return this;
        }

        public VideoBuilder rowTitle(String row_title) {
            this.rowTitle = row_title;
            return this;
        }

        public VideoBuilder linkTitle(String title) {
            this.title = title;
            return this;
        }


        public VideoBuilder linkUrl(String videoUrl) {
            this.videoUrl = videoUrl;
            return this;
        }

        public VideoBuilder bgImageUrl(String bgImageUrl) {
            this.bgImageUrl = bgImageUrl;
            return this;
        }

        public VideoBuilder cardImageUrl(String cardImageUrl) {
            this.cardImageUrl = cardImageUrl;
            return this;
        }

        public Video buildFromMediaDesc(MediaDescription desc) {
            return new Video(
                    Long.parseLong(desc.getMediaId()),
                    "", // Category - not provided by MediaDescription.
                    String.valueOf(desc.getTitle()),
                    "", // Background Image URI - not provided by MediaDescription.
                    String.valueOf(desc.getIconUri()),
                    String.valueOf(desc.getSubtitle())
            );
        }

        public Video build() {
            return new Video(
                    id,
                    rowTitle,
                    title,
                    videoUrl,
                    bgImageUrl,
                    cardImageUrl
            );
        }
    }
}
