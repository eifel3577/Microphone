package com.sapphire.microphone.activities;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.sapphire.microphone.C;
import com.sapphire.microphone.L;
import com.sapphire.microphone.R;
import com.sapphire.microphone.Util;
import com.sapphire.microphone.core.SaveVideoServiceFFMPEG;
import com.sapphire.microphone.dialogs.ApproveDialog;
import com.sapphire.microphone.session.Session;

public class MuxActivity extends Activity implements View.OnClickListener {
	private VideoView videoView;
	private MediaPlayer mediaPlayer;
	private ProgressBar loading;
	private SeekBar seekBar;
	private Timer timer;
	private TextView duration, currentPosition, shiftIndicator;
	private ImageButton play;
	private int lastProgress = 0;
	private int shift = 0;
	ApproveDialog approveDialog;
	private static final int SHIFT_AMOUNT = 100;
	private int rotation = 0;
	private String video, audio;
	private int videoHeight = 0, videoWidth = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mux_layout);
		approveDialog = new ApproveDialog(MuxActivity.this);
		seekBar = (SeekBar) findViewById(R.id.seek_bar);
		seekBar.setOnSeekBarChangeListener(seekBarListener);
		duration = (TextView) findViewById(R.id.duration);
		currentPosition = (TextView) findViewById(R.id.time);
		shiftIndicator = (TextView) findViewById(R.id.shift_indicator);
		shiftIndicator.setText(String.valueOf(shift));
		play = (ImageButton) findViewById(R.id.play);
		// restoreState(savedInstanceState);
		loading = (ProgressBar) findViewById(R.id.loading);
		loading.setVisibility(View.VISIBLE);
		video = getIntent().getStringExtra(C.VIDEO);
		audio = getIntent().getStringExtra(C.AUDIO);
		initAudio(audio);
		rotation = getIntent().getIntExtra(C.DATA, 0);
		videoView = (VideoView) findViewById(R.id.vvMedia);
		videoView.setVideoPath(video);

		videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
			@Override
			public void onPrepared(MediaPlayer mp) {
				L.e("prepared");
				L.e("video duration = " + videoView.getDuration());
				L.e("audio duration = " + mediaPlayer.getDuration());
				videoHeight = mp.getVideoHeight();
				videoWidth = mp.getVideoWidth();
				setVideoViewFullscreen();
				seekBar.setMax(videoView.getDuration());
				if (shift == 0) {
					final int s = videoView.getDuration()
							- mediaPlayer.getDuration();
					if (Math.abs(s) >= 100) {
						shift = s - s % 100;
						updateShiftIndicator();
					}
				}
				loading.setVisibility(View.GONE);
				videoView.seekTo(lastProgress);
				videoView.start();
				final int audioDuration = mediaPlayer.getDuration();
				Util.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						final int videoPosition = videoView
								.getCurrentPosition();
						int i = videoPosition - shift;

						System.out.println("Rest of te time or escaped time"
								+ i);
						L.e("progress(" + videoPosition + ") - shift(" + shift
								+ ") = " + i);
						if (i > audioDuration)
							i = audioDuration;
						if (i < 0)
							i = 0;
						mediaPlayer.seekTo(i);
						mediaPlayer.start();
						startTimer();
						duration.setText(Util.formatDuration(videoView
								.getDuration()));
					}
				}, 300);
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
						play.setImageResource(R.drawable.play);
						restart();
					}
				});
		videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				L.e("media error: " + what);
				Util.toast(getString(R.string.VIDEO_PLAYBACK_ERROR),
						getApplicationContext());

				finish();
				return false;
			}
		});

		Util.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				setVideoViewFullscreen();
			}
		}, 300);
	}

	private void initAudio(final String audio) {
		mediaPlayer = new MediaPlayer();
		try {
			mediaPlayer.setDataSource(audio);
			mediaPlayer.prepare();

		} catch (Exception e) {
			Util.toast(getString(R.string.AUDIO_PLAY_ERROR), this);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Util.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				setVideoViewFullscreen();
			}
		}, 300);
	}

	private void purgeTimer() {
		if (timer != null) {
			timer.cancel();
			timer.purge();
		}
	}

	private void startTimer() {
		timer = new Timer(true);
		timer.scheduleAtFixedRate(new ProgressTask(), 1000, 1000);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.save) {
			save();
			return;
		} else if (v.getId() == R.id.cancel) {
			createQuitDialog();
			return;
		}
		final ImageButton button = (ImageButton) v;
		if (v.getId() == R.id.play) {
			if (videoView.isPlaying()) {
				lastProgress = videoView.getCurrentPosition();
				videoView.pause();
				mediaPlayer.pause();
				purgeTimer();
				button.setImageResource(R.drawable.play);
			} else {
				try {
					if (videoView.getCurrentPosition() != 0) {
						videoView.start();
						mediaPlayer.start();
					} else {
						videoView.start();
						if (shift == 0) {
							mediaPlayer.start();
						} else if (shift > 0) {
							Util.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									mediaPlayer.start();
								}
							}, shift);
						} else if (shift < 0) {
							mediaPlayer.start();
							mediaPlayer.seekTo(Math.abs(shift));
						}
					}
					startTimer();
					button.setImageResource(R.drawable.pause);

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} else if (v.getId() == R.id.shift_to_left) {
			shift -= SHIFT_AMOUNT;
			updateShiftIndicator();
			restart();
		} else if (v.getId() == R.id.shift_to_rightt) {
			shift += SHIFT_AMOUNT;
			updateShiftIndicator();
			restart();
		}
	}

	private void restart() {
		videoView.pause();
		videoView.seekTo(0);
		mediaPlayer.pause();
		mediaPlayer.seekTo(0);
		lastProgress = 0;
		purgeTimer();
		seekBar.setProgress(0);
		((ImageButton) findViewById(R.id.play))
				.setImageResource(R.drawable.play);
	}

	private void updateShiftIndicator() {
		shiftIndicator.setText(String.format(Locale.ENGLISH, "%.2f s",
				shift / 1000f));
	}

	private class ProgressTask extends TimerTask {
		@Override
		public void run() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					seekBar.setProgress(videoView.getCurrentPosition());
					updateTime(videoView.getCurrentPosition());
				}
			});
		}
	}

	private SeekBar.OnSeekBarChangeListener seekBarListener = new SeekBar.OnSeekBarChangeListener() {
		private int progress = 0;

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			if (fromUser) {
				this.progress = progress;
				if (progress < 2000)
					this.progress = 0;
				updateTime(progress);
			}
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			purgeTimer();
			videoView.pause();
			mediaPlayer.pause();
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			updateTime(progress);
			L.e("video progress = " + videoView.getCurrentPosition());
			L.e("audio progress = " + mediaPlayer.getCurrentPosition());
			final int audioDuration = mediaPlayer.getDuration();
			videoView.seekTo(progress);
			videoView.start();
			Util.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					final int videoPosition = videoView.getCurrentPosition();
					int i = videoPosition - shift;
					L.e("progress(" + videoPosition + ") - shift(" + shift
							+ ") = " + i);
					if (i > audioDuration)
						i = audioDuration;
					if (i < 0)
						i = 0;
					mediaPlayer.seekTo(i);
					mediaPlayer.start();
					L.e("video progress after start = "
							+ videoView.getCurrentPosition());
					L.e("audio progress after start = "
							+ mediaPlayer.getCurrentPosition());
					L.e("-----------------------------------------------------------");
					startTimer();
				}
			}, 200);
		}
	};

	private void updateTime(final int time) {
		currentPosition.setText(Util.formatDuration(time));
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			videoView.suspend();
		} catch (Exception ignored) {
		}
		try {
			mediaPlayer.release();
		} catch (Exception ignored) {
		}
	}

	private void save() {
		final Intent i = new Intent(getApplicationContext(),
				SaveVideoServiceFFMPEG.class);
		i.setAction(C.ACTION_MERGE_VIDEO_AUDIO);
		i.putExtra(C.VIDEO, video);
		i.putExtra(C.AUDIO, audio);
		i.putExtra(C.DATA, rotation);
		i.putExtra(C.DELAY, shift);
		i.putExtra(
				C.DURATION,
				Math.max(mediaPlayer.getDuration(), videoView.getDuration()) / 1000);
		videoView.suspend();
		mediaPlayer.release();
		startService(i);
		finish();
	}

	private void createQuitDialog() {

		approveDialog.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (v.getId() == R.id.save) {
					save();
				} else if (v.getId() == R.id.delete) {
					videoView.suspend();
					mediaPlayer.release();
					Util.deleteFiles(video, audio);
					finish();
					if (Session.current().isConnected()) {
						final Intent i = new Intent(getApplicationContext(),
								CameraActivity.class);
						i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(i);
					}
				}
			}
		});
		approveDialog.show();
	}

	@Override
	public void onBackPressed() {
		createQuitDialog();
	}

	private void setVideoViewFullscreen() {
		final DisplayMetrics metrics = getResources().getDisplayMetrics();
		final int w = videoWidth;
		final int h = videoHeight;
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
