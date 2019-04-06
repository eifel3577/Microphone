package com.sapphire.microphone.core;

import android.app.IntentService;
import android.content.Intent;
import android.os.PowerManager;
import android.view.WindowManager;
import com.sapphire.microphone.C;
import com.sapphire.microphone.L;
import com.sapphire.microphone.R;
import com.sapphire.microphone.Util;
import com.sapphire.microphone.dialogs.SaveVideoDialog;
import com.sapphire.microphone.dialogs.SaveVideoProgressDialog;
import com.sapphire.microphone.recorder.Mp4ParserWrapper;

import java.io.File;

public class ConcatVideoService extends IntentService {
	private SaveVideoProgressDialog progressDialog = null;
	private PowerManager.WakeLock wakeLock = null;

	public ConcatVideoService() {
		super(ConcatVideoService.class.getName());
	}

	@Override
	public void onCreate() {
		super.onCreate();
		PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
				ConcatVideoService.class.getName() + "_lock");
		registerComponentCallbacks(this);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		final String action = intent.getAction();
		if (C.ACTION_CONCAT_VIDEO.equals(action)) {
			final String data = intent.getStringExtra(C.DATA);
			final String data1 = intent.getStringExtra(C.VIDEO);
			acquireLock();
			Mp4ParserWrapper.append(data, data1);
			releaseLock();
		} else if (C.ACTION_CONCAT_VIDEO_LAST.equals(action)) {
			final String data = intent.getStringExtra(C.DATA);
			final String data1 = intent.getStringExtra(C.VIDEO);
			acquireLock();
			sendStartBroadcast();
			L.e("start concat");
			Mp4ParserWrapper.append(data, data1);
			L.e("end concat");
			sendEndBroadcast();
			releaseLock();
		} else if (C.ACTION_RENAME_VIDEO.equals(action)) {
			final String fileName = intent.getStringExtra(C.VIDEO);
			final String newName = intent.getStringExtra(C.DATA);
			File f = new File(fileName);
			if (!f.renameTo(new File(newName))) {
				L.e("couldn't rename file");
			} else {
				Util.notifyChange(this, newName);
				showVideoMergedDialog();
			}
		}
	}

	private void showVideoMergedDialog() {
		Util.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (progressDialog != null)
					progressDialog.dismiss();
				final SaveVideoDialog d = new SaveVideoDialog(
						getApplicationContext());
				d.setMessage(getString(R.string.VIDEO_HAS_BEEN_SAVED));
				d.getWindow().setType(
						WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
				d.adjustOrientation(true);
				d.show();
			}
		});
	}

	private void sendStartBroadcast() {
		sendBroadcast(new Intent(C.ACTION_CONCAT_START));
	}

	private void sendEndBroadcast() {
		sendBroadcast(new Intent(C.ACTION_CONCAT_END));
	}

	private void acquireLock() {
		wakeLock.acquire();
	}

	private void releaseLock() {
		wakeLock.release();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterComponentCallbacks(this);
	}
}
