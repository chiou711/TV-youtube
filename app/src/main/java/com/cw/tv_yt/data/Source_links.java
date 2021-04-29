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

	public static List<Pair<String, String>> getFileIdList(Activity act) {

		// in res/raw
//        Context context = getActivity().getApplicationContext();
//        InputStream istream = context.getResources().openRawResource(R.raw.db_src_links);

		// in assets
		AssetManager assetManager = act.getAssets();
		InputStream in_stream = null;
		List<Pair<String, String>> file_id_list = new ArrayList<Pair<String, String>>();

		try {
			in_stream = assetManager.open("db_src_links.xml");
		} catch (
				IOException e) {
			e.printStackTrace();
		}

		int eventType = -1;

		try {
			XmlPullParser xpp = XmlPullParserFactory.newInstance().newPullParser();
			assert in_stream != null;
			xpp.setInput(new InputStreamReader(in_stream));

			eventType = xpp.getEventType();

			Pair<String, String> pair = new Pair<>("","");
			String tag = null;
			do {
				switch (eventType) {
					case XmlPullParser.START_TAG:
						tag = xpp.getName();
						//System.out.println("---- tag = " + tag);
						if ("id".equals(tag)) {
						}
						break;

					case XmlPullParser.TEXT:
						String text = xpp.getText();
						text = text.trim();
						//System.out.println("---- text  = " + text);
						if (!text.isEmpty()) {
							if ("title".equals(tag))
								pair.setFirst(text);
							else if ("id".equals(tag))
								pair.setSecond(text);
						}
						break;
				}

//				System.out.println("---- pair.getFirst()  = " + pair.getFirst());
//				System.out.println("---- pair.getSecond()  = " + pair.getSecond());
				if( !(pair.getFirst()).isEmpty() &&
					!(pair.getSecond()).isEmpty() ) {
					file_id_list.add(pair);
					pair = new Pair<>("","");
				}

			} while ((eventType = xpp.next()) != XmlPullParser.END_DOCUMENT);

		} catch (
				XmlPullParserException | IOException e) {
			e.printStackTrace();
		}

		return  file_id_list;
	}

}
