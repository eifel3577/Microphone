package com.sapphire.microphone.fragments;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.sapphire.microphone.C;
import com.sapphire.microphone.R;
import com.sapphire.microphone.Util;
import com.sapphire.microphone.activities.MainFragmentActivity;
import com.sapphire.microphone.core.MicService;
import com.sapphire.microphone.renderer.MovingGraphRenderer;
import com.sapphire.microphone.renderer.Renderer;
import com.sapphire.microphone.session.Session;
import com.sapphire.microphone.view.VisualizerView;

public class MicRecordFragment extends Fragment implements View.OnClickListener {
	private TextView waitingForRecord;
	private TextView recordTime;
	private ImageView rec, stop, preview;
	private volatile int seconds = 0;
	private Timer timer;
	private VisualizerView visualizerView;
	private long lastClickTime = System.currentTimeMillis();

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.mic_record_fragment_layout,
				container, false);
		getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		waitingForRecord = (TextView) v.findViewById(R.id.waiting_text);
		recordTime = (TextView) v.findViewById(R.id.record_time);
		rec = (ImageButton) v.findViewById(R.id.record);
		stop = (ImageButton) v.findViewById(R.id.stop);
		preview = (ImageView) v.findViewById(R.id.adaptive_preview);
		final DisplayMetrics dm = getResources().getDisplayMetrics();
		preview.setMinimumWidth(dm.widthPixels);
		preview.setMinimumHeight(dm.widthPixels);
		preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
		rec.setOnClickListener(this);
		stop.setOnClickListener(this);
		visualizerView = (VisualizerView) v.findViewById(R.id.visualizer);
		setupVisualizer();
		if (Session.current().isConnected()) {
			if (savedInstanceState == null)
				return v;
			seconds = savedInstanceState.getInt(C.DATA, 0);
			final boolean isRec = savedInstanceState.getBoolean(C.UNKNOWN,
					false);
			if (isRec) {
				showRecordButton();
				startRecording();
			}
		}
		return v;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		waitingForRecord.setText(getString(R.string.WAITING_FOR_RECORD));
	}

	private void setupVisualizer() {
		Paint paint = new Paint();
		paint.setStrokeWidth(Util.convertDpToPixel(1, getResources()));
		paint.setAntiAlias(true);
		paint.setDither(false);
		paint.setColor(getResources().getColor(R.color.action_bar_red));
		final Renderer lineGraphRenderer = new MovingGraphRenderer(paint);
		visualizerView.clearRenderers();
		visualizerView.setRenderer(lineGraphRenderer);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(C.ACTION_PAUSE_RECORDING);
		intentFilter.addAction(C.ACTION_STOP_RECORDING);
		intentFilter.addAction(C.ACTION_START_RECORDING);
		intentFilter.addAction(C.ACTION_AUDIO_DATA);
		intentFilter.addAction(C.ACTION_SEND_PREVIEW);
		getActivity().registerReceiver(actionsReceiver, intentFilter);
	}

	@Override
	public void onDetach() {
		super.onDetach();
		getActivity().unregisterReceiver(actionsReceiver);
		stopTimer();
	}

	public void showRecordButton() {
		try {
			getView().findViewById(R.id.button_group).setVisibility(
					View.VISIBLE);
			waitingForRecord.setVisibility(View.INVISIBLE);
			rec.setImageResource(R.drawable.record);
			rec.setTag("0");
			clearRecordTime();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void hideRecordButton() {
		getView().findViewById(R.id.button_group).setVisibility(View.GONE);
		waitingForRecord.setVisibility(View.VISIBLE);
		stopTimer();
		clearRecordTime();
		getMainActivity().unlock();
		clearVisualizer();
		preview.setImageDrawable(new ColorDrawable(Color.TRANSPARENT));
		preview.invalidate();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(C.DATA, seconds);
		outState.putBoolean(C.UNKNOWN, isRecording());
	}

	@Override
	public void onClick(View v) {
		if (!Session.current().isConnected()) {
			Util.toast(getString(R.string.CONNECTION_IS_NOT_EXISTING),
					getActivity());
			return;
		}
		if (System.currentTimeMillis() - lastClickTime <= 3000) {
			return;
		}
		lastClickTime = System.currentTimeMillis();
		if (v == stop) {
			stop();
		} else if (v == rec) {
			final String state = rec.getTag().toString();
			if (state.equals("0")) {
				record();
				rec.setTag("1");
			} else {
				pause();
				rec.setTag("0");
			}
		}
	}

	public void record() {
		final Intent i = new Intent(getActivity(), MicService.class);
		i.setAction(C.ACTION_START_RECORDING);
		getActivity().startService(i);
		rec.setImageResource(R.drawable.pause);
		rec.setTag("1");
		stopTimer();
		timer = new Timer(true);
		timer.scheduleAtFixedRate(new CounterTask(), 2000, 1000);
		getMainActivity().lockPage();
		hidePreview();
		visualizerView.setVisibility(View.VISIBLE);
	}

	public void stop() {
		final Intent i = new Intent(getActivity(), MicService.class);
		i.setAction(C.ACTION_STOP_RECORDING);
		getActivity().startService(i);
		rec.setImageResource(R.drawable.record);
		rec.setTag("0");
		stopTimer();
		clearRecordTime();
		seconds = 0;
		getMainActivity().unlock();
		clearVisualizer();
		showPreview();
	}

	public void pause() {
		final Intent i = new Intent(getActivity(), MicService.class);
		i.setAction(C.ACTION_PAUSE_RECORDING);
		getActivity().startService(i);
		rec.setImageResource(R.drawable.record);
		rec.setTag("1");
		stopTimer();
	}

	public void startRecording() {
		rec.setImageResource(R.drawable.pause);
		rec.setTag("1");
		stopTimer();
		timer = new Timer(true);
		timer.scheduleAtFixedRate(new CounterTask(), 1000, 1000);
		getMainActivity().lockPage();
		hidePreview();
		visualizerView.setVisibility(View.VISIBLE);
	}

	public void stopRecording() {
		rec.setImageResource(R.drawable.record);
		rec.setTag("0");
		stopTimer();
		seconds = 0;
		clearRecordTime();
		getMainActivity().unlock();
		clearVisualizer();
		showPreview();
	}

	public void pauseRecording() {
		rec.setImageResource(R.drawable.record);
		rec.setTag("0");
		stopTimer();
	}

	private void stopTimer() {
		if (timer != null) {
			timer.cancel();
			timer.purge();
			timer = null;
		}
	}

	private boolean isRecording() {
		return rec.getTag().toString().equals("1");
	}

	private void clearRecordTime() {
		recordTime.setText("0:00");
		seconds = 0;
	}

	private class CounterTask extends TimerTask {
		@Override
		public void run() {
			seconds++;
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					final int minutes = seconds / 60;
					final int seconds = MicRecordFragment.this.seconds % 60;
					recordTime.setText(minutes + ":"
							+ ((seconds < 10) ? "0" + seconds : seconds));
				}
			});
		}
	}

	private final BroadcastReceiver actionsReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (C.ACTION_START_RECORDING.equals(action)) {
				startRecording();
			} else if (C.ACTION_PAUSE_RECORDING.equals(action)) {
				pauseRecording();
			} else if (C.ACTION_STOP_RECORDING.equals(action)) {
				stopRecording();
			} else if (C.ACTION_AUDIO_DATA.equals(action)) {
				final short[] data = intent.getShortArrayExtra(C.DATA);
				if (visualizerView.getVisibility() == View.VISIBLE)
					visualizerView.updateVisualizer(data);
			} else if (C.ACTION_SEND_PREVIEW.equals(action)) {
				final byte[] data = intent.getByteArrayExtra(C.DATA);
				final Bitmap bm = BitmapFactory.decodeByteArray(data, 0,
						data.length);
				preview.setImageBitmap(bm);
			}
		}
	};

	private void clearVisualizer() {
		visualizerView.setVisibility(View.INVISIBLE);
		visualizerView.clearRenderers();
		visualizerView.clear();
	}

	private MainFragmentActivity getMainActivity() {
		return (MainFragmentActivity) getActivity();
	}

	private void showPreview() {
		preview.setVisibility(View.VISIBLE);
		preview.setImageDrawable(new ColorDrawable(Color.TRANSPARENT));
		preview.invalidate();
	}

	private void hidePreview() {
		preview.setImageDrawable(new ColorDrawable(Color.TRANSPARENT));
		preview.invalidate();
		preview.setVisibility(View.INVISIBLE);
	}

}
