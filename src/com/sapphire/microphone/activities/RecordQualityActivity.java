package com.sapphire.microphone.activities;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.sapphire.microphone.C;
import com.sapphire.microphone.R;
import com.sapphire.microphone.Util;
import com.sapphire.microphone.util.PrefUtil;

public class RecordQualityActivity extends Activity implements
		View.OnClickListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Util.changeLanguage(PrefUtil.getLanguage(getResources()
				.getConfiguration().locale.getLanguage()), getResources());
		super.onCreate(savedInstanceState);
		setContentView(R.layout.record_quality_layout);
		final int videoQuality = PrefUtil.getVideoQuality();
		final int audioQuality = PrefUtil.getAudioQuality();
		showQuality(videoQuality, audioQuality);
		if (PrefUtil.isMic()) {
			findViewById(R.id.message).setVisibility(View.VISIBLE);
			findViewById(R.id.quality_group).setVisibility(View.GONE);
			((Button) findViewById(R.id.save_button))
					.setText(getString(R.string.BACK));
		}
	}

	private void showQuality(final int videoQuality, final int audioQuality) {
		cancelAudioSelection();
		cancelVideoSelection();
		switch (videoQuality) {
		case C.VIDEO_QUALITY_HIGH:
			selectView(findViewById(R.id.video_high), true);
			break;
		case C.VIDEO_QUALITY_NORMAL:
			selectView(findViewById(R.id.video_normal), true);
			break;

		}

		switch (audioQuality) {
		case C.AUDIO_QUALITY_HIGH:
			selectView(findViewById(R.id.audio_high), true);
			return;
		case C.AUDIO_QUALITY_NORMAL:
			selectView(findViewById(R.id.audio_normal), true);
			return;
		case C.AUDIO_QUALITY_LOW:
			selectView(findViewById(R.id.audio_low), true);
		}
	}

	private void cancelVideoSelection() {
		selectView(findViewById(R.id.video_high), false);
		selectView(findViewById(R.id.video_normal), false);

	}

	private void cancelAudioSelection() {
		selectView(findViewById(R.id.audio_high), false);
		selectView(findViewById(R.id.audio_normal), false);
		selectView(findViewById(R.id.audio_low), false);
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.save_button:
			save();
			finish();
			return;
		case R.id.video_high:

		case R.id.video_normal:
			cancelVideoSelection();
			selectView(view, true);
			return;
		case R.id.audio_high:
		case R.id.audio_normal:
		case R.id.audio_low:
			cancelAudioSelection();
			selectView(view, true);
		}
	}

	private void selectView(final View v, final boolean select) {
		v.setSelected(select);
		if (select)
			((Button) v).setTextColor(Color.WHITE);
		else
			((Button) v).setTextColor(Color.BLACK);
	}

	private void save() {
		int videoQuality = C.VIDEO_QUALITY_NORMAL;
		if (findViewById(R.id.video_high).isSelected()) {
			videoQuality = C.VIDEO_QUALITY_HIGH;
		} else if (findViewById(R.id.video_normal).isSelected()) {
			videoQuality = C.VIDEO_QUALITY_NORMAL;
		}

		int audioQuality = C.AUDIO_QUALITY_NORMAL;
		if (findViewById(R.id.audio_high).isSelected()) {
			audioQuality = C.AUDIO_QUALITY_HIGH;
		} else if (findViewById(R.id.audio_normal).isSelected()) {
			audioQuality = C.AUDIO_QUALITY_NORMAL;
		} else if (findViewById(R.id.audio_low).isSelected()) {
			audioQuality = C.AUDIO_QUALITY_LOW;
		}
		PrefUtil.saveVideoQuality(videoQuality);
		PrefUtil.saveAudioQuality(audioQuality);
	}

}
