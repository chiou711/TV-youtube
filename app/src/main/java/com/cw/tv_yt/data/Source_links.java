package com.cw.tv_yt.data;

import android.app.Activity;
import android.content.res.AssetManager;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

// cf: https://stackoverflow.com/questions/2586528/counting-xml-elements-in-file-on-android/2586621

public class Source_links {

	public static List<String> getFileIdList(Activity act) {

		// in res/raw
//        Context context = getActivity().getApplicationContext();
//        InputStream istream = context.getResources().openRawResource(R.raw.db_src_links);

		// in assets
		AssetManager assetManager = act.getAssets();
		InputStream in_stream = null;
		List<String> file_id_list = new ArrayList<>();

		try {
			in_stream = assetManager.open("db_src_links.xml");
		} catch (
				IOException e) {
			e.printStackTrace();
		}

		int eventType = -1;

		try {
			XmlPullParser xpp = XmlPullParserFactory.newInstance().newPullParser();
			xpp.setInput(new InputStreamReader(in_stream));

			eventType = xpp.getEventType();

			do {
				switch (eventType) {

					case XmlPullParser.START_TAG:
						final String tag = xpp.getName();
						if ("string".equals(tag)) {
						}
						break;

					case XmlPullParser.TEXT:
						String file_id = xpp.getText();
						file_id = file_id.trim();
						if (!file_id.isEmpty()) {
							System.out.println("--- file ID = " + file_id);
							file_id_list.add(file_id);
						}
						break;
				}

			} while ((eventType = xpp.next()) != XmlPullParser.END_DOCUMENT);

		} catch (
				XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return  file_id_list;
	}
}
