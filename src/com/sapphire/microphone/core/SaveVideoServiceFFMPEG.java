package com.sapphire.microphone.core;

import android.app.IntentService;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.PowerManager;
import android.os.Process;
import android.view.WindowManager;
import com.sapphire.microphone.C;
import com.sapphire.microphone.L;
import com.sapphire.microphone.R;
import com.sapphire.microphone.Util;
import com.sapphire.microphone.dialogs.SaveVideoDialog;
import com.sapphire.microphone.dialogs.SaveVideoProgressDialog;
import com.sapphire.microphone.ffmpeg.Ffmpeg;
import com.yandex.metrica.Counter;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

public class SaveVideoServiceFFMPEG extends IntentService {
	private Timer timer;
	private SaveVideoProgressDialog progressDialog = null;
	private PowerManager.WakeLock wakeLock = null;

	public SaveVideoServiceFFMPEG() {
		super(SaveVideoServiceFFMPEG.class.getName());
	}
	

	@Override
	public void onCreate() {
		super.onCreate();
		PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
				SaveVideoServiceFFMPEG.class.getName() + "_lock");
	}

	@Override
	protected void onHandleIntent(final Intent intent) {
		final String action = intent.getAction();
		if (C.ACTION_MERGE_VIDEO_AUDIO.equals(action)) {
			final String video = intent.getStringExtra(C.VIDEO);
			final String audio = intent.getStringExtra(C.AUDIO);
			final int rotation = intent.getIntExtra(C.DATA, 0);
			final int duration = intent.getIntExtra(C.DURATION, 100) / 3 * 2;
			final int shift = intent.getIntExtra(C.DELAY, 0);
			acquireLock();
			sendBroadcast(new Intent(C.ACTION_START_MERGING));
			mergeVideoAndAudio(video, audio, rotation, shift, duration);
			sendBroadcast(new Intent(C.ACTION_STOP_MERGING));
			releaseLock();
		}
	}

	private void showProgressDialog(final int max) {
		Util.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (progressDialog == null) {
					progressDialog = new SaveVideoProgressDialog(
							getApplicationContext());
				}
				if (progressDialog.isShowing()) {
					progressDialog.dismiss();
				}
				progressDialog.setProgress(0);
				progressDialog.setMax(max);
				progressDialog.getWindow().setType(
						WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
				progressDialog.setCancelable(false);
				progressDialog.adjustOrientation(false);
				progressDialog.show();
			}
		});
	}

	private void incProgress(final int value) {
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.inc(value);
		}
	}

	private boolean mergeVideoAndAudio(final String videoFileName,
			final String aacFileName, final int rotation, final int shift,
			final int duration) {
		android.os.Process
				.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
		boolean result = false;
		try {
			showProgressDialog(duration);
			final File merged = new File(videoFileName.replaceAll(
					C.MP4_FILE_EXTENSION, ".va" + C.MP4_FILE_EXTENSION));
			setProgressPercentage(0);
			startTimer();
			final Ffmpeg ffmpeg = new Ffmpeg();
			if (ffmpeg.combineAudioAndVideo(videoFileName, aacFileName,
					rotation, shift, merged.getAbsolutePath())) {
				setProgressPercentage(duration);
				Util.notifyChange(getApplicationContext(),
						merged.getAbsolutePath());
				stopTimer();
				showVideoMergedDialog();
				result = true;
			} else {
				L.e("merge error");
				showVideoMergedDialog(getString(R.string.VIDEO_MERGE_ERROR));
				stopTimer();
				result = false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			Counter.sharedInstance().reportError(e.getMessage(), e);
			Util.toast(getString(R.string.VIDEO_MERGE_ERROR), this);
			Util.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					progressDialog.dismiss();
				}
			});
			stopTimer();
		} finally {
			Util.deleteFiles(videoFileName, aacFileName);
		}
		return result;
	}

	private void shutdownProcess() {
		Process.killProcess(Process.myPid());
	}

	private void stopTimer() {
		if (timer != null) {
			timer.cancel();
			timer.purge();
			timer = null;
		}
	}

	private void startTimer() {
		stopTimer();
		timer = new Timer(true);
		timer.scheduleAtFixedRate(new PercentageSendTask(), 1000, 1000);
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
				d.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						shutdownProcess();
					}
				});
				d.show();
			}
		});
	}

	private void showVideoMergedDialog(final String text) {
		Util.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (progressDialog != null) {
					progressDialog.dismiss();
				}
				final SaveVideoDialog d = new SaveVideoDialog(
						getApplicationContext());
				d.setMessage(text);
				d.getWindow().setType(
						WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
				d.adjustOrientation(true);
				d.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						shutdownProcess();
					}
				});
				d.show();
			}
		});
	}

	private void setProgressPercentage(final int percentage) {
		Util.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				progressDialog.setProgress(percentage);
			}
		});
	}

	private class PercentageSendTask extends TimerTask {

		@Override
		public void run() {
			Util.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					incProgress(1);
				}
			});
		}
	}

	private void acquireLock() {
		wakeLock.acquire();
	}

	private void releaseLock() {
		wakeLock.release();
	}
}
