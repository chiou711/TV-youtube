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

import android.content.Context;

import com.cw.tv_yt.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class ParseJsonToDB {

    private Context mContext;
    public static boolean isParsing;
    public String fileBody = "";
    String filePath;

    ParseJsonToDB(String filePath, Context context)
    {
        mContext = context;
        this.filePath = filePath;
        isParsing = true;
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
        Utils.parseJsonAndInsertDB(mContext,jsonObj);
        isParsing = false;
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
        Utils.parseJsonAndInsertDB(mContext,jsonObj);
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

}