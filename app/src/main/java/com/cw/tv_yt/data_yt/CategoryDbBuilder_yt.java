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

import android.content.Context;
import android.util.Log;

import com.cw.tv_yt.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.net.ssl.HttpsURLConnection;

import androidx.annotation.NonNull;

/**
 * The VideoDbBuilder is used to grab a JSON file from a server and parse the data
 * to be placed into a local database
 */
public class CategoryDbBuilder_yt {
    public static final String TAG_CATEGORY = "category";
    private Context mContext;

    /**
     * Default constructor that can be used for tests
     */

    public CategoryDbBuilder_yt(Context mContext) {
        this.mContext = mContext;
    }

    /**
     * Fetches JSON data representing videos from a server and populates that in a database
     * @param url The location of the video list
     */
    public @NonNull void fetch(String url,int index)
            throws IOException, JSONException {
        JSONObject videoData = fetchJSON(url);
        buildMedia(videoData,index);
    }

    /**
     * Takes the contents of a JSON object and populates the database
     * @param jsonObj The JSON object of videos
     * @throws JSONException if the JSON object is invalid
     */
      public void buildMedia(JSONObject jsonObj,int index) throws JSONException {

        System.out.println("CategoryDbBuilder / _buildMedia / jsonObj.toString = " + jsonObj.toString());

        String categoryName = jsonObj.getString(TAG_CATEGORY);
        System.out.println("CategoryDbBuilder / categoryName = " + categoryName);

        // save preference
        Utils.setPref_category_name(mContext,index,categoryName);
    }

    /**
     * Fetch JSON object from a given URL.
     *
     * @return the JSONObject representation of the response
     * @throws JSONException
     * @throws IOException
     */
    private JSONObject fetchJSON(String urlString) throws JSONException, IOException {
        System.out.println("CategoryDbBuilder_yt / fetchJSON / urlString = " + urlString);

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
                    Log.e(TAG_CATEGORY, "JSON feed closed", e);
                }
            }
        }
    }
}