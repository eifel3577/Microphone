package com.sapphire.microphone;

import android.app.Application;
import android.content.Context;
import android.os.Environment;
import com.sapphire.microphone.util.PrefUtil;
import com.yandex.metrica.Counter;

import java.io.File;

public class MicrofonApp extends Application {
	private static File appDir;
	private static Context context;

	@Override
	public void onCreate() {
		super.onCreate();
		Counter.initialize(getApplicationContext());
		MicrofonApp.context = getApplicationContext();
		PrefUtil.init(getApplicationContext());
		MicrofonApp.appDir = createAppDirs();
		deleteTempFiles();
		Util.changeLanguage(PrefUtil.getLanguage(getResources()
				.getConfiguration().locale.getLanguage()), getResources());
		PrefUtil.saveAudioQuality(C.AUDIO_QUALITY_NORMAL);
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				Util.delay(100);
				deleteTempFiles();
			}
		}));
	}

	private File createAppDirs() {
		final File root = Environment.getExternalStorageDirectory();
		final File appDir = new File(root, getString(R.string.app_name));
		if (!appDir.exists())
			appDir.mkdir();
		return appDir;
	}

	private void deleteTempFiles() {
		final File[] files = appDir.listFiles();
		if (files != null)
			for (File f : files) {
				final String filename = f.getName();
				if (filename.contains(".tmp") || f.length() == 0
						|| filename.contains(".rotation")) {
					f.delete();
				}
			}
	}

	public static File getAppDir() {
		return appDir;
	}

	public static Context getContext() {
		return context;
	}

}
