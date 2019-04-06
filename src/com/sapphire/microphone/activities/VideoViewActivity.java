package com.sapphire.microphone.activities;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.*;

import com.sapphire.microphone.C;
import com.sapphire.microphone.R;
import com.sapphire.microphone.Util;

import java.util.Timer;
import java.util.TimerTask;

public class VideoViewActivity extends Activity implements
		View.OnClickListener, SeekBar.OnSeekBarChangeListener {
	private VideoView videoView;
	private ProgressBar loading;
	private SeekBar seekBar;
	private Timer timer;
	private TextView duration, currentPosition;
	private ImageButton play;
	SharedPreferences sharedPreferences;
	private static final int CHANGE_PROGRESS_HOLD_TIME = 200;
	private long seekBarLastTouched = System.currentTimeMillis();
	private int lastProgress = 0;
	private MediaPlayer mediaPlayer = null;
	String findCameraRole = "";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		sharedPreferences = getSharedPreferences("CAMERAROLE", MODE_PRIVATE);

		findCameraRole = sharedPreferences.getString("CameraRole", "");
		if (findCameraRole.equalsIgnoreCase("Square")) {

			setContentView(R.layout.square_format_video_view);

		} else {
			setContentView(R.layout.video_view_layout);
		}

		seekBar = (SeekBar) findViewById(R.id.seek_bar);
		seekBar.setOnSeekBarChangeListener(this);
		duration = (TextView) findViewById(R.id.duration);
		currentPosition = (TextView) findViewById(R.id.time);
		play = (ImageButton) findViewById(R.id.play);
		loading = (ProgressBar) findViewById(R.id.loading);
		loading.setVisibility(View.VISIBLE);
		Uri uri = getIntent().getParcelableExtra(C.DATA);
		videoView = (VideoView) findViewById(R.id.vvMedia);
		videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
			@Override
			public void onPrepared(MediaPlayer mp) {
				mediaPlayer = mp;
				loading.setVisibility(View.GONE);
				videoView.seekTo(lastProgress);
				videoView.start();
				timer = new Timer(true);
				timer.scheduleAtFixedRate(new ProgressTask(), 1000, 1000);

				duration.setText(Util.formatDuration(videoView.getDuration()));
			}
		});

		videoView
				.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
					@Override
					public void onCompletion(MediaPlayer mp) {
						if (timer != null) {
							timer.cancel();
							timer.purge();
						}
						updateTime(mp.getDuration());
						seekBar.setProgress(seekBar.getMax());
						play.setImageResource(R.drawable.play);
					}
				});
		videoView.setVideoURI(uri);
		Util.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				setVideoViewFullscreen();
			}
		}, 500);

	}

	@Override
	public void onClick(View v) {
		final ImageButton button = (ImageButton) v;
		if (videoView.isPlaying()) {
			lastProgress = videoView.getCurrentPosition();
			videoView.pause();
			timer.cancel();
			timer.purge();
			button.setImageResource(R.drawable.play);
		} else {
			videoView.resume();
			button.setImageResource(R.drawable.pause);
		}
	}

	private class ProgressTask extends TimerTask {
		@Override
		public void run() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					seekBar.setMax(videoView.getDuration());
					seekBar.setProgress(videoView.getCurrentPosition());
					updateTime(videoView.getCurrentPosition());
				}
			});
		}
	}

	@Override
	protected void onResume() {
		videoView.resume();
		super.onResume();
	}

	@Override
	protected void onPause() {
		videoView.suspend();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		videoView.stopPlayback();
		super.onDestroy();
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		if (fromUser) {
			updateTime(progress);
			if (System.currentTimeMillis() - seekBarLastTouched > CHANGE_PROGRESS_HOLD_TIME) {
				videoView.seekTo(progress);
				seekBarLastTouched = System.currentTimeMillis();
			}
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Util.runOnUiThread(new Runnable() {
			@Override
			public void run() {

				Toast.makeText(getApplicationContext(),
						"Configuration is called", Toast.LENGTH_SHORT).show();
				setVideoViewFullscreen();
			}
		}, 300);
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		seekBarLastTouched = 0;
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {

	}

	private void updateTime(final int time) {
		currentPosition.setText(Util.formatDuration(time));
	}

	private void setVideoViewFullscreen() {

		if (findCameraRole.equalsIgnoreCase("Square")) {

			VideoViewActivity.this
					.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

			if (mediaPlayer == null)
				return;
			final DisplayMetrics metrics = getResources().getDisplayMetrics();
			final int w = mediaPlayer.getVideoWidth();
			final int h = mediaPlayer.getVideoHeight() / 2;
			if (w > metrics.widthPixels) {
				videoView.setMinimumWidth(metrics.widthPixels);
				videoView.getLayoutParams().width = metrics.widthPixels;
			}
			if (h > metrics.heightPixels) {
				videoView.setMinimumHeight(metrics.heightPixels);
				videoView.getLayoutParams().height = metrics.heightPixels;
			}
			if (w < metrics.widthPixels) {
				videoView.setMinimumWidth(h);
				videoView.getLayoutParams().width = h;
			}
			if (h < metrics.heightPixels) {
				videoView.setMinimumHeight(w);
				videoView.getLayoutParams().height = w;
			}
		} else {

			if (mediaPlayer == null)
				return;
			final DisplayMetrics metrics = getResources().getDisplayMetrics();
			final int w = mediaPlayer.getVideoWidth();
			final int h = mediaPlayer.getVideoHeight();
			if (w > metrics.widthPixels / 2) {
				videoView.setMinimumWidth(metrics.widthPixels);
				videoView.getLayoutParams().width = metrics.widthPixels;
			}
			if (h > metrics.heightPixels / 2) {
				videoView.setMinimumHeight(metrics.heightPixels);
				videoView.getLayoutParams().height = metrics.heightPixels;
			}
			if (w < metrics.widthPixels / 2) {
				videoView.setMinimumWidth(h / 2);
				videoView.getLayoutParams().width = h / 2;
			}
			if (h < metrics.heightPixels / 2) {
				videoView.setMinimumHeight(w / 2);
				videoView.getLayoutParams().height = w / 2;
			}
		}
	}
}
