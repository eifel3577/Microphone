package com.sapphire.microphone.activities;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Face;
import android.hardware.Camera.FaceDetectionListener;
import android.hardware.Camera.Parameters;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.Settings.System;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.ZoomControls;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.sapphire.microphone.C;
import com.sapphire.microphone.L;
import com.sapphire.microphone.MicrofonApp;
import com.sapphire.microphone.R;
import com.sapphire.microphone.Util;
import com.sapphire.microphone.core.CameraService;
import com.sapphire.microphone.core.ConcatVideoService;
import com.sapphire.microphone.dialogs.LoadingDialog;
import com.sapphire.microphone.dialogs.SaveVideoDialog;
import com.sapphire.microphone.model.LastPreviewSize;
import com.sapphire.microphone.recorder.AudioRecorder;
import com.sapphire.microphone.session.Session;
import com.sapphire.microphone.view.AdaptiveSurfaceView;

/**
 * 
 * @author Deepak Kumar Chauhan
 * 
 */
public class CameraActivity extends Activity implements View.OnClickListener,
		Camera.PreviewCallback, MediaRecorder.OnErrorListener,
		SurfaceHolder.Callback {
	private Camera camera;
	private AdaptiveSurfaceView preview;
	private AudioRecorder recorder;
	private ImageButton[] rec, stop;
	private ImageButton currentStop;
	private TextView[] time;
	public int count_camera_parameters_exception = 0;
	private ProgressDialog progressDialog;
	private static boolean running = false;
	private int orientation = -1;
	private ScheduledExecutorService myScheduledExecutorService;
	private OrientationEventListener orientationEventListener;
	private int lastDegree = -1;
	private Timer freeSpaceCheckTimer;
	private int seconds = 0;
	private Timer recordTimeCounter;
	int max_exposure = +12;
	int min_exposure = +7;
	private static final AudioRecorder.MediaRecorderConfig config = new AudioRecorder.MediaRecorderConfig();
	private LastPreviewSize lastPreviewSize = null;
	private long lastClickTime = 0;
	private boolean isDefaultLandscape = false;
	private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
	private static byte[] previewBuffer = null;
	private LoadingDialog loadingDialog = null;
	SharedPreferences sharedPreferences, sharedPreferences1;
	boolean previewing = false;
	private SeekBar brightbar, brightBar2;

	// Variable to store brightness value
	private int brightness;
	// Content resolver used as a handle to the system's settings
	private ContentResolver cResolver;
	// Window object, that will store a reference to the current window
	private Window window;
	Face[] detectedFaces;
	SurfaceHolder surfaceHolder;
	DrawingView drawingView;
	private LinearLayout layout;
	private Handler m_handler;
	ImageButton imageButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		isDefaultLandscape = Util.getDeviceDefaultOrientation() == Configuration.ORIENTATION_LANDSCAPE;
		sharedPreferences = getSharedPreferences("CAMERAROLE", MODE_PRIVATE);
		sharedPreferences1 = getSharedPreferences("CAMERAROLE1", MODE_PRIVATE);
		String findCameraRole = sharedPreferences.getString("CameraRole", "");

		if (findCameraRole.equalsIgnoreCase("Square")) {
			CameraActivity.this
					.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			setContentView(R.layout.camera_preview_layout);
		} else {
			setContentView(R.layout.camera_normal_preview);
		}

		CameraActivity.this
				.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		brightbar = (SeekBar) findViewById(R.id.seek_bar_for_camera);

		imageButton = (ImageButton) findViewById(R.id.stop_15);
		layout = (LinearLayout) findViewById(R.id.layout);

		time = new TextView[4];
		time[0] = (TextView) findViewById(R.id.time_0);
		time[1] = (TextView) findViewById(R.id.time_1);
		time[2] = (TextView) findViewById(R.id.time_2);
		time[3] = (TextView) findViewById(R.id.time_3);
		rec = new ImageButton[4];
		stop = new ImageButton[4];
		rec[0] = (ImageButton) findViewById(R.id.record_0);
		rec[1] = (ImageButton) findViewById(R.id.record_1);
		rec[2] = (ImageButton) findViewById(R.id.record_2);
		rec[3] = (ImageButton) findViewById(R.id.record_3);
		stop[0] = (ImageButton) findViewById(R.id.stop_0);
		stop[1] = (ImageButton) findViewById(R.id.stop_1);
		stop[2] = (ImageButton) findViewById(R.id.stop_2);
		stop[3] = (ImageButton) findViewById(R.id.stop_3);
		currentStop = stop[0];
		preview = (AdaptiveSurfaceView) findViewById(R.id.adaptive_preview);
		preview.setKeepScreenOn(true);
		surfaceHolder = preview.getHolder();
		preview.getHolder().setSizeFromLayout();
		drawingView = new DrawingView(this);

		LayoutParams layoutParamsDrawing = new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		this.addContentView(drawingView, layoutParamsDrawing);

		preview.getHolder().addCallback(new SurfaceHolder.Callback() {
			@Override
			public void surfaceCreated(SurfaceHolder holder) {

				initRecorder();
				handleCommand();

			}

			@Override
			public void surfaceChanged(SurfaceHolder holder, int format,
					int width, int height) {

				if (previewing) {
					camera.stopFaceDetection();
					camera.stopPreview();
					previewing = false;
				}
				if (camera != null) {
					previewing = true;
					camera.startPreview();
					camera.startFaceDetection();
					lastPreviewSize = new LastPreviewSize(width, height);
					try {
						camera.setDisplayOrientation(90);
						final Camera.Parameters parameters = camera
								.getParameters();
						min_exposure = parameters.getMinExposureCompensation();
						max_exposure = parameters.getMaxExposureCompensation();

						final Camera.Size size = Util.getOptimalPreviewSize(
								parameters.getSupportedPreviewSizes(), width,
								height);
						preview.setPreviewSize(size);
						camera.setPreviewDisplay(holder);
						parameters.setPreviewSize(size.width, size.height);
						if (parameters.getSupportedFocusModes() != null
								&& parameters
										.getSupportedFocusModes()
										.contains(
												Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
							parameters
									.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
						}
						camera.setParameters(parameters);
					} catch (Exception e) {
						e.printStackTrace();
					}
					camera.startPreview();

				}
			}

			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
				L.e("destroyed surface");
				try {
					camera.stopFaceDetection();
					camera.stopPreview();
					camera.release();
					camera = null;
					previewing = false;
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		});

		registerReceiver();
		progressDialog = new ProgressDialog(this);
		progressDialog.setMessage(getString(R.string.VIDEO_IS_SAVING_WAIT));
		progressDialog.setTitle(getString(R.string.PROCESSING));
		progressDialog.setIndeterminate(true);
		progressDialog.setCancelable(false);
		for (ImageButton b : stop) {
			b.setVisibility(View.GONE);
		}
		running = true;
		orientationEventListener = new OrientationEventListener(
				CameraActivity.this) {

			@Override
			public void onOrientationChanged(int orientation) {
				for (final int o : C.ORIENTATIONS) {
					if (o == CameraActivity.this.orientation)
						continue;
					if (orientation <= o + 30 && orientation >= o - 30) {
						L.e("new orientation=" + o);
						if (o == 180) {
							break;
						}
						CameraActivity.this.orientation = o;
						changeButtonPositions(o);
						return;
					}
				}
			}
		};

		orientationEventListener.enable();
	}

	public void setWhiteBalance(String wb) {
		Camera.Parameters parameters;
		parameters = camera.getParameters();
		parameters.setWhiteBalance(wb);
		camera.setParameters(parameters);// actually set the parameters
	}

	private void changeButtonPositions(final int orientationDegree) {
		if (lastDegree == orientationDegree)
			return;
		int index = Arrays.binarySearch(C.ORIENTATIONS, orientationDegree);
		for (int i = 0; i < 4; i++) {
			rec[i].setVisibility(View.INVISIBLE);
			if (stop[i].getVisibility() != View.GONE) {
				stop[i].setVisibility(View.INVISIBLE);
			}
			time[i].setVisibility(View.INVISIBLE);
		}
		if (isDefaultLandscape) {
			switch (orientationDegree) {
			case 0:
				index = 3;
				break;
			case 90:
				index = 0;
				break;
			case 180:
				index = 1;
				break;
			default:
				index = 2;
			}
		}
		rec[index].setVisibility(View.VISIBLE);
		if (stop[index].getVisibility() != View.GONE) {
			stop[index].setVisibility(View.VISIBLE);
		}
		if (index == 2) {
			brightbar.setVisibility(View.INVISIBLE);
			brightBar2.setVisibility(View.VISIBLE);

		}
		time[index].setVisibility(View.VISIBLE);

		currentStop = stop[index];

		lastDegree = orientationDegree;
	}

	private void handleCommand() {
		if (C.ACTION_START_RECORDING.equals(getIntent().getAction())) {
			startRecording();
		}
	}

	private void registerReceiver() {
		final IntentFilter intentFilter = new IntentFilter(C.ACTION_PCM_CREATED);
		intentFilter.addAction(C.ACTION_START_RECORDING);
		intentFilter.addAction(C.ACTION_PAUSE_RECORDING);
		intentFilter.addAction(C.ACTION_ABORT_CONNECTION);
		intentFilter.addAction(C.ACTION_START_MERGING);
		intentFilter.addAction(C.ACTION_STOP_MERGING);
		intentFilter.addAction(C.ACTION_CONCAT_START);
		intentFilter.addAction(C.ACTION_CONCAT_END);
		registerReceiver(serviceActionsReceiver, intentFilter);
	}

	private boolean safeCameraOpen() {
		try {
			releaseCameraAndPreview();
			camera = Camera.open();
			if (camera == null)
				camera = Camera.open(0);
			camera.setDisplayOrientation(90);
			final Camera.Parameters params = camera.getParameters();
			camera.setPreviewDisplay(preview.getHolder());
			if (lastPreviewSize != null) {
				try {
					final Camera.Size size = Util.getOptimalPreviewSize(camera
							.getParameters().getSupportedPreviewSizes(),
							lastPreviewSize.getWidth(), lastPreviewSize
									.getHeight());
					preview.setPreviewSize(size);
					params.setPreviewSize(size.width, size.height);
					if (params.getSupportedFocusModes() != null
							&& params
									.getSupportedFocusModes()
									.contains(
											Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
						params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
					}
				} catch (Exception ignored) {

				}
				camera.setParameters(params);
				setCameraPreviewCallback();
			}
			camera.startPreview();
			return (camera != null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private void setCameraPreviewCallback() {
		if (!Session.current().isSCO()) {
			if (previewBuffer == null) {
				final Camera.Size size = Util
						.getOptimalPreviewSize(camera.getParameters()
								.getSupportedPreviewSizes(), lastPreviewSize
								.getWidth(), lastPreviewSize.getHeight());
				int yStride = (int) Math.ceil(size.width / 16.0) * 16;
				int uvStride = (int) Math.ceil((yStride / 2) / 16.0) * 16;
				int ySize = yStride * size.height;
				int uvSize = uvStride * size.height / 2;
				int s = ySize + uvSize * 2;
				previewBuffer = new byte[s];
			}
			camera.addCallbackBuffer(previewBuffer);
			camera.setPreviewCallbackWithBuffer(this);
		}
	}

	private void releaseCameraAndPreview() {
		if (camera != null) {
			camera.setPreviewCallback(null);
			camera.stopPreview();
			camera.release();
			camera = null;
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		android.provider.Settings.System.putInt(this.getContentResolver(),
				android.provider.Settings.System.SCREEN_BRIGHTNESS, 200);
	}

	@Override
	protected void onStop() {
		super.onStop();
		android.provider.Settings.System.putInt(this.getContentResolver(),
				android.provider.Settings.System.SCREEN_BRIGHTNESS, 200);
		if (recorder != null && recorder.isRecording()) {
			pauseRecording();

		}
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		if (recorder != null && recorder.isPaused()) {
			startRecording();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (recorder != null && recorder.isPaused()) {
			startRecording();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		L.e("on destroy");
		releaseCameraAndPreview();
		unregisterReceiver(serviceActionsReceiver);
		stopCheckTimer();
		stopRecordTimeCounter();
		running = false;
		orientationEventListener.disable();
	}

	private void initRecorder() {
		safeCameraOpen();
		config.setCamera(camera);
		config.setOrientation(orientation);
		config.setOnErrorListener(this);
		final String fileName = Util.createFileName("");
		final String filePath = new File(MicrofonApp.getAppDir(), fileName
				+ C.MP4_FILE_EXTENSION).getAbsolutePath();
		Session.current().setRecordFileName(fileName);
		recorder = AudioRecorder.build(filePath, config);
		recorder.lockCamera();
	}

	public static boolean isRunning() {
		return running;
	}

	public void clickedExposure(View view) {

		SeekBar seek_bar = ((SeekBar) findViewById(R.id.seek_bar_for_camera));
		int visibility = seek_bar.getVisibility();
		seek_bar.setVisibility(View.VISIBLE);
		imageButton.setVisibility(View.INVISIBLE);
		final int min_exposure = getMinimumExposure();
		seek_bar.setVisibility(View.VISIBLE);
		setSeekBarExposure();
		seek_bar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {

				setExposure(min_exposure + progress, false);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});

	}

	protected void setExposure(int new_exposure, boolean update_seek_bar) {

		if (camera != null && (min_exposure != 0 || max_exposure != 0)) {

			Camera.Parameters parameters = camera.getParameters();
			int current_exposure = parameters.getExposureCompensation();
			if (new_exposure < min_exposure)
				new_exposure = min_exposure;
			if (new_exposure > max_exposure)
				new_exposure = max_exposure;
			if (new_exposure != current_exposure) {

				parameters.setExposureCompensation(new_exposure);
				setCameraParameters(parameters);
				// now save

				if (update_seek_bar) {
					CameraActivity main_activity = (CameraActivity) this
							.getApplicationContext();
					main_activity.setSeekBarExposure();
				}
			}
		}
	}

	private void setCameraParameters(Camera.Parameters parameters) {

		if (camera == null) {

			return;
		}
		try {
			camera.setParameters(parameters);

		} catch (RuntimeException e) {
			// just in case something has gone wrong

			e.printStackTrace();
			count_camera_parameters_exception++;
		}
	}

	void setSeekBarExposure() {
		SeekBar seek_bar = ((SeekBar) findViewById(R.id.seek_bar_for_camera));
		final int min_exposure = getMinimumExposure();
		seek_bar.setMax(getMaximumExposure() - min_exposure);
		seek_bar.setProgress(getCurrentExposure() - min_exposure);
	}

	int getCurrentExposure() {

		if (camera == null) {

			return 0;
		}
		Camera.Parameters parameters = camera.getParameters();
		int current_exposure = parameters.getExposureCompensation();
		return current_exposure;
	}

	private int getMaximumExposure() {
		// TODO Auto-generated method stub
		return this.max_exposure;
	}

	int getMinimumExposure() {

		return this.min_exposure;
	}

	private void startRecording() {
		if (!Session.current().isSCO()) {
			Util.sendStartCommand(this);
			progressDialog.setMessage(getString(R.string.START_RECORDING));
			progressDialog.show();
		} else {
			progressDialog.dismiss();
			for (ImageButton s : stop) {
				if (s.getVisibility() == View.GONE)
					s.setVisibility(View.INVISIBLE);
			}
			currentStop.setVisibility(View.VISIBLE);
			if (recorder != null) {
				recorder.setRotation(orientation);
				try {
					recorder.start();
					stop[0].setTag(recorder.getRecordFileName());
					for (ImageButton r : rec)
						r.setImageResource(R.drawable.pause);
					rec[0].setTag("1");
					startCheckTimer();
					startRecordTimeCounter();
				} catch (Exception e) {
					L.e("start exception: " + e.getMessage());
					e.printStackTrace();
					recorder.lockCamera();
				}
			}
		}
	}

	private void pauseRecording() {
		if (recorder != null)
			try {
				recorder.pause();
				if (!Session.current().isSCO()) {
					Util.sendPauseCommand(CameraActivity.this);
				}
				stopRecordTimeCounter();
				L.e("paused");
			} catch (Exception e) {
				L.e("pause exception: " + e.getMessage());
				e.printStackTrace();
				recorder.lockCamera();
			}
	}

	private void stopRecording() {
		if (!Session.current().isSCO()) {
			final Intent i = new Intent(getApplicationContext(),
					CameraService.class);
			i.setAction(C.ACTION_STOP_RECORDING);
			startService(i);
		} else {
			if (recorder != null)
				stopRecordTimeCounter();
			clearTime();
			try {
				recorder.stop();
				final File f = new File(recorder.getRecordFileName());
				final String newName = f.getAbsolutePath().replaceAll(
						C.MP4_FILE_EXTENSION, ".va" + C.MP4_FILE_EXTENSION);
				final Intent i = new Intent(getApplicationContext(),
						ConcatVideoService.class);
				i.setAction(C.ACTION_RENAME_VIDEO);
				i.putExtra(C.VIDEO, f.getAbsolutePath());
				i.putExtra(C.DATA, newName);
				startService(i);
				stopCheckTimer();
				initRecorder();
			} catch (Exception e) {
				e.printStackTrace();
				stopCheckTimer();
				recorder.lockCamera();
			}
		}
	}

	@Override
	public void onClick(View v) {
		if (!Session.current().isConnected()) {
			Util.toast(getString(R.string.CONNECTION_NOT_ESTABLISHED), this);
			return;
		}
		if (java.lang.System.currentTimeMillis() - lastClickTime <= 3000) {
			return;
		}
		lastClickTime = java.lang.System.currentTimeMillis();
		switch (v.getId()) {
		case R.id.record_0:
		case R.id.record_2:
		case R.id.record_3:
		case R.id.record_1:
			final String state = rec[0].getTag().toString();
			if (state.equals("0")) {
				for (ImageButton r : rec)
					r.setImageResource(R.drawable.pause);
				startRecording();
				rec[0].setTag("1");
			} else {
				for (ImageButton r : rec)
					r.setImageResource(R.drawable.record);
				pauseRecording();
				rec[0].setTag("0");
			}
			return;
		case R.id.stop_0:
		case R.id.stop_1:
		case R.id.stop_2:
		case R.id.stop_3:
			stopRecording();
			for (ImageButton r : rec)
				r.setImageResource(R.drawable.record);
			rec[0].setTag("0");
		}
	}

	private final BroadcastReceiver serviceActionsReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final String action = intent.getAction();
			if (C.ACTION_PCM_CREATED.equals(action)) {
				stopCheckTimer();
				if (recorder != null) {
					// final int rotation = recorder.getRotation();
					stopRecordTimeCounter();
					clearTime();
					if (recorder.isRecording()) {
						try {
							final String audioFileName = intent
									.getStringExtra(C.DATA);
							final String videoFileName = stop[0].getTag()
									.toString();
							registerConcatReceiver(videoFileName, audioFileName);
							recorder.stop();
							initRecorder();
							for (ImageButton r : rec)
								r.setImageResource(R.drawable.record);
							rec[0].setTag("0");
							// mergeVideoAndAudio(videoFileName, audioFileName,
							// rotation);
						} catch (Exception e) {
							L.e("stop exception: " + e.getMessage());
							e.printStackTrace();
							progressDialog.dismiss();
							recorder.lockCamera();
						}
					} else if (recorder.isPaused()) {
						try {
							final String audioFileName = intent
									.getStringExtra(C.DATA);
							final String videoFileName = stop[0].getTag()
									.toString();
							registerConcatReceiver(videoFileName, audioFileName);
							recorder.stop();
							initRecorder();
						} catch (Exception e) {
							e.printStackTrace();
							initRecorder();
						}
						for (ImageButton r : rec)
							r.setImageResource(R.drawable.record);
						rec[0].setTag("0");
						// mergeVideoAndAudio(videoFileName, audioFileName,
						// rotation);
					}
				}
			} else if (C.ACTION_START_RECORDING.equals(action)) {
				final Messenger messenger = intent.getParcelableExtra(C.DATA);
				progressDialog.dismiss();
				for (ImageButton s : stop) {
					if (s.getVisibility() == View.GONE)
						s.setVisibility(View.INVISIBLE);
				}
				currentStop.setVisibility(View.VISIBLE);
				if (recorder != null) {
					recorder.setRotation(orientation);
					try {
						recorder.start();
						if (messenger != null) {
							try {
								messenger.send(new Message());
							} catch (RemoteException e) {
								e.printStackTrace();
							}
						}
						stop[0].setTag(recorder.getRecordFileName());
						for (ImageButton r : rec)
							r.setImageResource(R.drawable.pause);
						rec[0].setTag("1");
						startCheckTimer();
						startRecordTimeCounter();
						camera.setPreviewCallbackWithBuffer(null);
					} catch (Exception e) {
						L.e("start exception: " + e.getMessage());
						e.printStackTrace();
						recorder.lockCamera();
					}
				}
			} else if (C.ACTION_PAUSE_RECORDING.equals(action)) {
				try {
					recorder.pause();
				} catch (Exception e) {
					e.printStackTrace();
				}
				for (ImageButton r : rec)
					r.setImageResource(R.drawable.record);
				rec[0].setTag("0");
				stopRecordTimeCounter();
			} else if (C.ACTION_ABORT_CONNECTION.equals(action)) {
				progressDialog.dismiss();
				final SaveVideoDialog d = new SaveVideoDialog(
						CameraActivity.this);
				d.setMessage(getString(R.string.CONNECTION_IS_INTERRUPTED));
				d.show();
				if (recorder != null)
					try {
						recorder.stop();
					} catch (Exception e) {
						e.printStackTrace();
					}
				stopRecordTimeCounter();
				clearTime();
			} else if (C.ACTION_START_MERGING.equals(action)
					|| C.ACTION_CONCAT_START.equals(action)) {
				camera.setPreviewCallbackWithBuffer(null);
			} else if (C.ACTION_STOP_MERGING.equals(action)
					|| C.ACTION_CONCAT_END.equals(action)) {
				setCameraPreviewCallback();
			}

		}
	};

	private void registerConcatReceiver(final String video, final String audio) {
		final IntentFilter intentFilter = new IntentFilter(C.ACTION_CONCAT_END);
		registerReceiver(new ConcatReceiver(video, audio), intentFilter);
		if (loadingDialog == null) {
			loadingDialog = new LoadingDialog(this);
		}
		if (loadingDialog.isShowing()) {
			loadingDialog.dismiss();
		}
		loadingDialog.adjustOrientation(true);
		loadingDialog.setCancelable(false);
		loadingDialog.show();
	}

	@Override
	public void onError(MediaRecorder mr, int what, int extra) {
		if (what == MediaRecorder.MEDIA_ERROR_SERVER_DIED) {
			L.e("camera server died");
		}
	}

	private class ConcatReceiver extends BroadcastReceiver {
		private final String video, audio;
		private int rotation = 0;

		public ConcatReceiver(final String video, final String audio) {
			this.video = video;
			this.audio = audio;
			if (recorder != null)
				rotation = recorder.getRotation();
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (C.ACTION_CONCAT_END.equals(action)) {
				loadingDialog.dismiss();
				unregisterReceiver(this);
				mergeVideoAndAudio(video, audio, rotation);
			}
		}
	}

	private void mergeVideoAndAudio(final String videoFileName,
			final String audioFileName, final int rotation) {
		final Intent i = new Intent(getApplicationContext(), MuxActivity.class);
		i.putExtra(C.VIDEO, videoFileName);
		i.putExtra(C.AUDIO, audioFileName);
		i.putExtra(C.DATA, rotation);
		startActivity(i);
		finish();
	}

	@Override
	public void onBackPressed() {
		if (recorder != null && recorder.isRecording()) {
			return;
		}
		super.onBackPressed();
	}

	private void startCheckTimer() {
		stopCheckTimer();
		freeSpaceCheckTimer = new Timer(true);
		freeSpaceCheckTimer.scheduleAtFixedRate(new FreeSpaceCheckTask(), 0,
				5000);
	}

	private void stopCheckTimer() {
		if (freeSpaceCheckTimer != null) {
			freeSpaceCheckTimer.cancel();
			freeSpaceCheckTimer.purge();
			freeSpaceCheckTimer = null;
		}
	}

	public static int calculateInSampleSize(
			final BitmapFactory.Options options, final int reqWidth,
			final int reqHeight) {
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {
			final int heightRatio = Math.round((float) height
					/ (float) reqHeight);
			final int widthRatio = Math.round((float) width / (float) reqWidth);
			inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
		}
		return inSampleSize;
	}

	@Override
	public void onPreviewFrame(final byte[] yuvData, final Camera camera) {
		if (yuvData == null) {

			camera.addCallbackBuffer(previewBuffer);
			return;
		}
		if (recorder != null && recorder.isRecording()) {
			camera.setPreviewCallbackWithBuffer(null);
			return;
		}
		final byte[] data = createBitmapFromPreview(yuvData);
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		options.inPreferQualityOverSpeed = false;
		options.inPreferredConfig = Bitmap.Config.RGB_565;
		BitmapFactory.decodeByteArray(data, 0, data.length, options);
		options.inSampleSize = calculateInSampleSize(options,
				C.PREVIEW_BITMAP_SIZE.x, C.PREVIEW_BITMAP_SIZE.y);
		options.inJustDecodeBounds = false;
		final Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length,
				options);
		if (bm == null) {
			L.e("preview bitmap is null");
			camera.addCallbackBuffer(previewBuffer);
			return;
		}
		final Bitmap rotatedBitmap;
		if (orientation != 270 && !isDefaultLandscape) {
			Matrix matrix = new Matrix();
			matrix.postRotate(orientation + 90);
			rotatedBitmap = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(),
					bm.getHeight(), matrix, true);
			bm.recycle();
		} else if (isDefaultLandscape && orientation > 0) {
			Matrix matrix = new Matrix();
			matrix.postRotate(orientation);
			rotatedBitmap = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(),
					bm.getHeight(), matrix, true);
			bm.recycle();
		} else {
			rotatedBitmap = bm;
		}
		if (rotatedBitmap != null && !rotatedBitmap.isRecycled()) {
			final Intent i = new Intent(getApplicationContext(),
					CameraService.class);
			i.setAction(C.ACTION_SEND_PREVIEW);
			i.putExtra(C.DATA, rotatedBitmap);
			startService(i);
		}
		preview.postDelayed(new Runnable() {
			@Override
			public void run() {
				camera.addCallbackBuffer(previewBuffer);
			}
		}, 1500);
	}

	private byte[] createBitmapFromPreview(final byte[] data) {
		Camera.Size size = preview.size;
		YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, size.width,
				size.height, null);
		final Rect rect = new Rect(0, 0, size.width, size.height);
		baos.reset();
		yuvImage.compressToJpeg(rect, 50, baos);
		drawingView.setHaveTouch(true, rect);
		drawingView.invalidate();
		return baos.toByteArray();
	}

	private class FreeSpaceCheckTask extends TimerTask {
		private volatile long startFreeSpace = -1;
		private volatile boolean isNotified = false;

		@Override
		public void run() {
			if (Session.current().isSCO()) {
				checkSCO();
			} else {
				checkRemote();
			}
		}

		private void checkSCO() {
			if (recorder == null)
				return;
			final long freeSpace = Environment.getExternalStorageDirectory()
					.getFreeSpace();
			if (startFreeSpace == -1) {
				startFreeSpace = freeSpace;
				return;
			}
			if (freeSpace <= 10 * 1024 * 1024) {
				stopCheckTimer();
				Util.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (recorder != null && recorder.isRecording()) {
							for (ImageButton s : stop) {
								if (s.getVisibility() == View.VISIBLE)
									s.performClick();
							}
							final SaveVideoDialog d = new SaveVideoDialog(
									CameraActivity.this);
							d.setMessage(getString(R.string.RECORD_STOPPED_NOT_ENOUGH_FREE_SPACE));
							d.show();
						}
					}
				});
			}
			if (freeSpace <= 20 * 1024 * 1024) {
				Util.toast(getString(R.string.FREE_SPACE_ENDS),
						CameraActivity.this);
				isNotified = true;
			}
		}

		private void checkRemote() {
			if (recorder == null)
				return;
			final long freeSpace = Environment.getExternalStorageDirectory()
					.getFreeSpace();
			if (startFreeSpace == -1) {
				startFreeSpace = freeSpace;
				return;
			}
			final long fileSize = startFreeSpace - freeSpace;
			if (freeSpace <= fileSize * 1.5
					|| fileSize >= Integer.MAX_VALUE * 0.95f) {
				stopCheckTimer();
				Util.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (recorder != null && recorder.isRecording()) {
							for (ImageButton s : stop) {
								if (s.getVisibility() == View.VISIBLE)
									s.performClick();
							}
							final SaveVideoDialog d = new SaveVideoDialog(
									CameraActivity.this);
							if (fileSize >= Integer.MAX_VALUE * 0.95f)
								d.setMessage(getString(R.string.RECORD_STOPPED_DUE_TO_MAX_FILE_LENGTH));
							else
								d.setMessage(getString(R.string.RECORD_STOPPED_NOT_ENOUGH_FREE_SPACE));
							d.show();
						}
					}
				});
			}
			if (isNotified)
				return;
			if (freeSpace <= fileSize * 2
					|| fileSize >= Integer.MAX_VALUE * 0.9f) {
				if (fileSize >= Integer.MAX_VALUE * 0.9f)
					Util.toast(
							getString(R.string.FILE_IS_RUNNING_OUT_OF_MAX_SIZE),
							CameraActivity.this);
				else
					Util.toast(getString(R.string.FREE_SPACE_ENDS),
							CameraActivity.this);
				isNotified = true;
			}
		}
	}

	private void clearTime() {
		seconds = 0;
		for (TextView t : time)
			t.setText("0:00");
	}

	private void startRecordTimeCounter() {
		stopRecordTimeCounter();
		recordTimeCounter = new Timer("Microphone record time counter", true);
		recordTimeCounter.scheduleAtFixedRate(new CounterTask(), 1000, 1000);
	}

	private void stopRecordTimeCounter() {
		if (recordTimeCounter != null) {
			recordTimeCounter.cancel();
			recordTimeCounter.purge();
			recordTimeCounter = null;
		}
	}

	private class CounterTask extends TimerTask {
		@Override
		public void run() {
			seconds++;
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					final int minutes = seconds / 60;
					final int seconds = CameraActivity.this.seconds % 60;
					for (TextView t : time)
						t.setText(minutes + ":"
								+ ((seconds < 10) ? "0" + seconds : seconds));
				}
			});
		}
	}

	public void touchFocus(final Rect tfocusRect) {
		try {

			/* buttonTakePicture.setEnabled(false); */

			camera.stopFaceDetection();

			// Convert from View's width and height to +/- 1000
			final Rect targetFocusRect = new Rect(tfocusRect.left * 2000
					/ drawingView.getWidth() - 1000, tfocusRect.top * 2000
					/ drawingView.getHeight() - 1000, tfocusRect.right * 2000
					/ drawingView.getWidth() - 1000, tfocusRect.bottom * 2000
					/ drawingView.getHeight() - 1000);

			final List<Camera.Area> focusList = new ArrayList<Camera.Area>();
			Camera.Area focusArea = new Camera.Area(targetFocusRect, 1000);
			focusList.add(focusArea);

			Parameters para = camera.getParameters();
			para.setFocusAreas(focusList);
			para.setMeteringAreas(focusList);
			camera.setParameters(para);

			camera.autoFocus(myAutoFocusCallback);

			drawingView.setHaveTouch(true, tfocusRect);
			drawingView.invalidate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	FaceDetectionListener faceDetectionListener = new FaceDetectionListener() {

		@Override
		public void onFaceDetection(Face[] faces, Camera tcamera) {

			if (faces.length == 0) {
				// prompt.setText(" No Face Detected! ");
				drawingView.setHaveFace(false);
			} else {
				// prompt.setText(String.valueOf(faces.length) +
				// " Face Detected :) ");
				drawingView.setHaveFace(true);
				detectedFaces = faces;

				// Set the FocusAreas using the first detected face
				List<Camera.Area> focusList = new ArrayList<Camera.Area>();
				Camera.Area firstFace = new Camera.Area(faces[0].rect, 1000);
				focusList.add(firstFace);

				Parameters para = camera.getParameters();

				if (para.getMaxNumFocusAreas() > 0) {
					para.setFocusAreas(focusList);
				}

				if (para.getMaxNumMeteringAreas() > 0) {
					para.setMeteringAreas(focusList);
				}

				/* buttonTakePicture.setEnabled(false); */

				// Stop further Face Detection
				camera.stopFaceDetection();

				/* buttonTakePicture.setEnabled(false); */

				/*
				 * Allways throw java.lang.RuntimeException: autoFocus failed if
				 * I call autoFocus(myAutoFocusCallback) here!
				 * 
				 * camera.autoFocus(myAutoFocusCallback);
				 */

				// Delay call autoFocus(myAutoFocusCallback)
				myScheduledExecutorService = Executors
						.newScheduledThreadPool(1);
				myScheduledExecutorService.schedule(new Runnable() {
					@SuppressWarnings("deprecation")
					public void run() {
						camera.autoFocus(myAutoFocusCallback);
					}
				}, 5000, TimeUnit.MILLISECONDS);

			}

			drawingView.invalidate();

		}
	};
	/*
	 * AutoFocusCallback myAutoFocusCallback = new AutoFocusCallback() {
	 * 
	 * @Override public void onAutoFocus(boolean arg0, Camera arg1) { // TODO
	 * Auto-generated method stub if (arg0) {
	 * 
	 * // camera.cancelAutoFocus(); } camera.autoFocus(myAutoFocusCallback);
	 * float focusDistances[] = new float[3];
	 * arg1.getParameters().getFocusDistances(focusDistances);
	 * 
	 * }
	 * 
	 * };
	 */
	AutoFocusCallback myAutoFocusCallback = new AutoFocusCallback() {

		@Override
		public void onAutoFocus(boolean arg0, Camera arg1) {
			if (arg0) {
				camera.cancelAutoFocus();
				// Bitmap bg = Bitmap.createBitmap(0, 0, Bitmap.Config.ALPHA_8);
				layout.setVisibility(View.GONE);
			} else {
				camera.autoFocus(myAutoFocusCallback);
			}
		}
	};

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub
		if (previewing) {
			camera.stopPreview();
			previewing = false;
		}

		if (camera != null) {
			try {
				camera.setPreviewDisplay(surfaceHolder);
				camera.startPreview();
				previewing = true;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {

		camera = Camera.open();
		try {
			camera.setPreviewDisplay(holder);

		} catch (IOException exception) {
			camera.release();
			camera = null;
			// TODO: add more exception handling logic here
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		camera.stopPreview();
		camera.release();
		camera = null;
		previewing = false;
	}
}
