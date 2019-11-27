package com.cw.tv_yt;

import android.os.AsyncTask;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;

public class JsonAsync extends AsyncTask<URL, Void, String> //Generic: Params, Progress, Result
{
	String title="";

    @Override
    protected void onPreExecute(){
    }

    @Override
    protected String doInBackground(URL... arg0) {
    	try {
			title = new JSONObject(IOUtils.toString(arg0[0])).getString("title");
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return title;
    }

    @Override
    protected void onPostExecute(String result) {
//    	System.out.println("JsonAsync / _onPostExecute / result (title)= " + result);
		if(!this.isCancelled())
		{
			this.cancel(true);
		}
    }
}