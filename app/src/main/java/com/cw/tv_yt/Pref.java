package com.cw.tv_yt;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import static com.cw.tv_yt.define.Define.DEFAULT_AUTO_PLAY_BY_CATEGORY;
import static com.cw.tv_yt.define.Define.DEFAULT_AUTO_PLAY_BY_LIST;
import static com.cw.tv_yt.define.Define.DEFAULT_SHOW_YOUTUBE_DURATION;

public class Pref {
	public static int ACTION_DELETE = 99;

	public static boolean isAutoPlayByList(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getBoolean(context.getString(R.string.pref_key_auto_play_by_list), DEFAULT_AUTO_PLAY_BY_LIST);
	}

	public static boolean isAutoPlayByCategory(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getBoolean(context.getString(R.string.pref_key_auto_play_by_category), DEFAULT_AUTO_PLAY_BY_CATEGORY);
	}

	public static boolean isShowDuration(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getBoolean(context.getString(R.string.pref_key_show_duration), DEFAULT_SHOW_YOUTUBE_DURATION);
	}

}