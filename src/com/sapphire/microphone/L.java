package com.sapphire.microphone;

import android.util.Log;

public final class L {
	private static final String TAG = "TAG";

	public static void e(final String text) {
		try {
			final String className = new Exception().getStackTrace()[1]
					.getFileName().replace(".java", "");
			Log.e(className, text != null ? text : "null");
		} catch (Exception e) {
			Log.e(TAG, text != null ? text : "null");
		}
	}
}
