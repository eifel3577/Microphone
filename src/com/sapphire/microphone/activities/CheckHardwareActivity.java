package com.sapphire.microphone.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import com.sapphire.microphone.R;
import com.sapphire.microphone.Util;
import com.sapphire.microphone.util.PrefUtil;

public class CheckHardwareActivity extends Activity implements
		View.OnClickListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Util.changeLanguage(PrefUtil.getLanguage(getResources()
				.getConfiguration().locale.getLanguage()), getResources());
		super.onCreate(savedInstanceState);
		setContentView(R.layout.check_hardware_layout);
		final ImageView camera = (ImageView) findViewById(R.id.camera);
		if (!Util.hasCamera(this)) {
			camera.setImageResource(R.drawable.ic_action_cancel);
		}

		final ImageView microphone = (ImageView) findViewById(R.id.microphone);
		if (!Util.hasMic(this)) {
			microphone.setImageResource(R.drawable.ic_action_cancel);
		}

		final ImageView wifi = (ImageView) findViewById(R.id.wifi);
		if (!Util.hasWifiDirect(this)) {
			wifi.setImageResource(R.drawable.ic_action_cancel);
		}

		final ImageView bluetooth = (ImageView) findViewById(R.id.bluetooth);
		if (!Util.hasBluetooth(this)) {
			bluetooth.setImageResource(R.drawable.ic_action_cancel);
		}
	}

	@Override
	public void onClick(View v) {
		final Intent i = new Intent(getApplicationContext(),
				RegistrationActivity.class);
		startActivity(i);
		finish();
	}
}
