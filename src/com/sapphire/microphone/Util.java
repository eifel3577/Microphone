package com.sapphire.microphone;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.media.*;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;
import com.sapphire.microphone.core.CameraService;
import com.sapphire.microphone.core.MicService;
import com.sapphire.microphone.session.Session;
import com.sapphire.microphone.util.PrefUtil;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class Util {
	private static final Handler handler = new Handler(Looper.getMainLooper());

	public static final int SAMPLERATE = 44100;
	public static final int RECORD_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
	private static final int PLAY_CHANNELS = AudioFormat.CHANNEL_OUT_MONO;
	private static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	private static final ByteArrayOutputStream baos = new ByteArrayOutputStream(
			C.PREVIEW_BITMAP_SIZE.x * C.PREVIEW_BITMAP_SIZE.y * 4);

	public static void toast(final String text, final Context context) {
		if (isUIThread()) {
			Toast.makeText(context, text, Toast.LENGTH_LONG).show();
		} else {
			handler.post(new Runnable() {
				@Override
				public void run() {
					toast(text, context);
				}
			});
		}
	}

	public static boolean isUIThread() {
		return Thread.currentThread()
				.equals(Looper.getMainLooper().getThread());
	}

	public static boolean hasCamera(final Context context) {
		return context.getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_CAMERA);
	}

	public static boolean hasMic(final Context context) {

		return context.getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_MICROPHONE);
	}

	public static boolean hasBluetooth(final Context context) {
		return context.getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_BLUETOOTH);
	}

	public static boolean hasWifiDirect(final Context context) {
		return context.getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_WIFI_DIRECT);
	}

	public static int convertDpToPixel(final float dp, final Resources resources) {
		DisplayMetrics metrics = resources.getDisplayMetrics();
		return Math.round(dp * (metrics.densityDpi / 160f));
	}

	public static float convertPixelsToDp(final float px,
			final Resources resources) {
		DisplayMetrics metrics = resources.getDisplayMetrics();
		return px / (metrics.densityDpi / 160f);
	}

	public static String createFileName(final String extension) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		Calendar cal = Calendar.getInstance();
		return dateFormat.format(cal.getTime()) + extension;
	}

	public static AudioRecord createAudioRecord() {
		L.e("record buffer = " + getRecordBuffer());
		L.e("track buffer = " + getTrackBuffer());
		return new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,
				SAMPLERATE, RECORD_CHANNELS, AUDIO_ENCODING, getRecordBuffer());
	}

	public static int getRecordBuffer() {
		return AudioRecord.getMinBufferSize(SAMPLERATE, RECORD_CHANNELS,
				AUDIO_ENCODING);
	}

	public static int getTrackBuffer() {
		return AudioTrack.getMinBufferSize(SAMPLERATE, PLAY_CHANNELS,
				AUDIO_ENCODING);
	}

	public static AudioTrack createAudioTrack() {
		L.e("record buffer = " + getRecordBuffer());
		L.e("track buffer = " + getTrackBuffer());
		return new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLERATE,
				PLAY_CHANNELS, AUDIO_ENCODING, getTrackBuffer(),
				AudioTrack.MODE_STREAM);
	}

	public static void sendStartCommand(final Context context) {
		final Intent i = new Intent(context, getServiceClass());
		i.setAction(C.ACTION_START_RECORDING);
		context.startService(i);
	}

	public static void sendPauseCommand(final Context context) {
		final Intent i = new Intent(context, getServiceClass());
		i.setAction(C.ACTION_PAUSE_RECORDING);
		context.startService(i);
	}

	public static void sendStopCommand(final Context context) {
		final Intent i = new Intent(context, getServiceClass());
		i.setAction(C.ACTION_STOP_RECORDING);
		context.startService(i);
	}

	public static Class getServiceClass() {
		if (PrefUtil.isMic())
			return MicService.class;
		return CameraService.class;
	}

	public static File backupFile(final File f) throws IOException {
		final File backup = new File(f.getAbsolutePath() + ".bak");
		backup.createNewFile();
		copyFile(f.getAbsolutePath(), backup.getAbsolutePath());
		return backup;
	}

	public static void copyFile(final String from, final String destination)
			throws IOException {
		FileChannel source = null;
		FileChannel dest = null;
		try {
			source = new FileInputStream(from).getChannel();
			dest = new FileOutputStream(destination).getChannel();
			final long size = source.size();
			source.transferTo(0, size, dest);
		} finally {
			if (source != null)
				source.close();
			if (dest != null)
				dest.close();
		}
	}

	public static String formatDuration(final int duration) {
		final int totalSeconds = duration / 1000;
		final int minutes = totalSeconds / 60;
		final int seconds = totalSeconds % 60;
		return minutes + ":" + ((seconds < 10) ? "0" + seconds : seconds);
	}

	/*
	 * numSamples: number of samples numBytes: size of data in bytes
	 * numChannels: number of channels e.g. 1 for mono, 2 for stereo
	 * numBytesPerSample: number of bytes per sample e.g. 1 for PCM 8-bit, 2 for
	 * PCM 16-bit sampleRate: the sample rate of the PCM
	 * 
	 * numSamples = (numBytes / numChannels) / numBytesPerSample
	 * 
	 * Duration (milliseconds) = (numSamples * 1000) / sampleRate
	 */

	public static long calcPCMDuration(final long byteSize) {
		return (long) (byteSize / 88.2f);
		/*
		 * double numSamples = byteSize / 2; return (long)((numSamples * 1000) /
		 * SAMPLERATE);
		 */
	}

	public static void sleep(final long millis) {
		try {
			TimeUnit.MILLISECONDS.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static long avgTime(final List<Long> times) {
		long l = 0;
		for (final long time : times) {
			l += time;
		}
		return l / times.size();
	}

	public static int deleteFiles(String... fileNames) {
		int numDeleted = 0;
		File f;
		for (final String fileName : fileNames) {
			f = new File(fileName);
			if (f.delete())
				numDeleted++;
		}
		return numDeleted;
	}

	public static boolean enableWifiDirect() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			return true;
		}
		try {
			Method method1 = Session.current().getManager().getClass()
					.getMethod("enableP2p", WifiP2pManager.Channel.class);
			method1.invoke(Session.current().getManager(), Session.current()
					.getChannel());
			return true;
		} catch (Exception ignored) {

		}
		return false;
	}

	public static void disableWifiDirect() {
		try {
			Method method1 = Session.current().getManager().getClass()
					.getMethod("disableP2p", WifiP2pManager.Channel.class);
			method1.invoke(Session.current().getManager(), Session.current()
					.getChannel());
		} catch (Exception ignored) {

		}
	}

	public static void runOnUiThread(final Runnable runnable) {
		handler.post(runnable);
	}

	public static void runOnUiThread(final Runnable runnable, final long millis) {
		handler.postDelayed(runnable, millis);
	}

	public static void changeLanguage(final String lang, final Resources res) {
		DisplayMetrics dm = res.getDisplayMetrics();
		android.content.res.Configuration conf = res.getConfiguration();
		conf.locale = new Locale(lang);
		res.updateConfiguration(conf, dm);
		PrefUtil.saveLanguage(lang);
	}

	public static boolean quietClose(final OutputStream out) {
		if (out == null)
			return true;
		try {
			out.flush();
			out.close();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static void quietClose(final Channel channel) {
		if (channel == null)
			return;
		try {
			channel.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static boolean pairDevice(BluetoothDevice device) {
		try {
			return device.createBond();
		} catch (Exception e) {
			e.printStackTrace();
			try {
				Method method = device.getClass().getMethod("createBond",
						(Class[]) null);
				return (Boolean) method.invoke(device, (Object[]) null);
			} catch (Exception err) {
				err.printStackTrace();
			}
		}
		return false;
	}

	public static int getVideoBitrate() {
		return C.VIDEO_QUALITY_MAP.get(PrefUtil.getVideoQuality());
	}

	public static int getAudioBitrate() {
		return C.AUDIO_QUALITY_MAP.get(PrefUtil.getAudioQuality());
	}

	public static void delay(final long millis) {
		try {
			TimeUnit.MILLISECONDS.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static byte[] shortToByte(final short[] input) {
		baos.reset();
		for (final short s : input) {
			baos.write((byte) (s & 0x00FF));
			baos.write((byte) ((s & 0xFF00) >> 8));
		}
		return baos.toByteArray();
	}

	public static double[] toFFT(final byte[] buffer) {
		final int size = buffer.length;
		double[] micBufferData = new double[size];
		final int bytesPerSample = AUDIO_ENCODING;
		final double amplification = 100.0;
		for (int index = 0, floatIndex = 0; index < size - bytesPerSample + 1; index += bytesPerSample, floatIndex++) {
			double sample = 0;
			for (int b = 0; b < bytesPerSample; b++) {
				int v = buffer[index + b];
				if (b < bytesPerSample - 1) {
					v &= 0xFF;
				}
				sample += v << (b * 8);
			}
			double sample32 = amplification * (sample / 32768.0);
			micBufferData[floatIndex] = sample32;
		}
		return micBufferData;
	}

	public static Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes,
			int w, int h) {
		final double ASPECT_TOLERANCE = 0.1;
		double targetRatio = (double) h / w;

		if (sizes == null)
			return null;

		Camera.Size optimalSize = null;
		double minDiff = Double.MAX_VALUE;

		for (Camera.Size size : sizes) {
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
				continue;
			if (Math.abs(size.height - h) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(size.height - h);
			}
		}

		if (optimalSize == null) {
			minDiff = Double.MAX_VALUE;
			for (Camera.Size size : sizes) {
				if (Math.abs(size.height - h) < minDiff) {
					optimalSize = size;
					minDiff = Math.abs(size.height - h);
				}
			}
		}
		return optimalSize;
	}

	public static synchronized byte[] bitmapToByteArray(final Bitmap bm) {
		baos.reset();
		bm.compress(Bitmap.CompressFormat.JPEG, 50, baos);
		return baos.toByteArray();
	}

	public static void notifyChange(final Context context, final String fileName) {
		context.getContentResolver().notifyChange(
				Uri.fromFile(MicrofonApp.getAppDir()), null);
		MediaScannerConnection.scanFile(context, new String[] { fileName },
				null, null);
	}

	public static int getDeviceDefaultOrientation() {
		WindowManager windowManager = (WindowManager) MicrofonApp.getContext()
				.getSystemService(Activity.WINDOW_SERVICE);
		Configuration config = MicrofonApp.getContext().getResources()
				.getConfiguration();
		int rotation = windowManager.getDefaultDisplay().getRotation();
		if (((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) && config.orientation == Configuration.ORIENTATION_LANDSCAPE)
				|| ((rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) && config.orientation == Configuration.ORIENTATION_PORTRAIT)) {
			return Configuration.ORIENTATION_LANDSCAPE;
		} else {
			return Configuration.ORIENTATION_PORTRAIT;
		}
	}
}
