package com.sapphire.microphone.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import com.sapphire.microphone.C;

public class PrefUtil {
	private static SharedPreferences pref;

	public static void init(final Context context) {
		pref = PreferenceManager.getDefaultSharedPreferences(context);
	}

	public static void saveRole(final String role) {
		pref.edit().putString(C.ROLE, role).apply();
	}

	public static String getRole() {
		return pref.getString(C.ROLE, C.CAMERA);
	}

	public static boolean isMic() {
		return pref.getString(C.ROLE, C.CAMERA).equals(C.MIC);
	}

	public static void saveLastConnectionType(final int type) {
		pref.edit().putInt(C.LAST_CONNECTION_TYPE, type).apply();
	}

	public static int getLastConnectionType() {
		return pref.getInt(C.LAST_CONNECTION_TYPE, C.TYPE_BLUETOOTH);
	}

	public static void saveRegistered() {
		pref.edit().putBoolean(C.IS_REGISTERED, true).apply();
	}

	public static boolean isRegistered() {
		return pref.getBoolean(C.IS_REGISTERED, false);
	}

	public static void saveLastCheckedTabIndex(final int index) {
		pref.edit().putInt(C.CHECKED_TAB_INDEX, index).apply();
	}

	public static int getLastCheckedTabIndex() {
		return pref.getInt(C.CHECKED_TAB_INDEX, 0);
	}

	public static void saveLanguage(final String language) {
		pref.edit().putString(C.LANGUAGE, language).apply();
	}

	public static String getLanguage(final String defaultLang) {
		return pref.getString(C.LANGUAGE, defaultLang);
	}

	public static void saveVideoQuality(final int quality) {
		pref.edit().putInt(C.VIDEO_QUALITY, quality).apply();
	}

	public static int getVideoQuality() {
		return pref.getInt(C.VIDEO_QUALITY, C.VIDEO_QUALITY_NORMAL);
	}

	public static void saveAudioQuality(final int quality) {
		pref.edit().putInt(C.AUDIO_QUALITY, quality).apply();
	}

	public static int getAudioQuality() {
		return pref.getInt(C.AUDIO_QUALITY, C.AUDIO_QUALITY_NORMAL);
	}

	public static void saveOrientation(final int orientation) {
		pref.edit().putInt(C.ORIENTATION, orientation).apply();
	}

	public static int getOrientation() {
		return pref.getInt(C.ORIENTATION, Configuration.ORIENTATION_PORTRAIT);
	}

}
