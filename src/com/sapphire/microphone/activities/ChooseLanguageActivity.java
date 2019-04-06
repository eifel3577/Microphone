package com.sapphire.microphone.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import com.sapphire.microphone.C;
import com.sapphire.microphone.R;
import com.sapphire.microphone.Util;
import com.sapphire.microphone.util.PrefUtil;

public class ChooseLanguageActivity extends Activity implements
		View.OnClickListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Util.changeLanguage(PrefUtil.getLanguage(getResources()
				.getConfiguration().locale.getLanguage()), getResources());
		setContentView(R.layout.choose_lang_fragment_layout);
		if (PrefUtil.isRegistered() && isMainAction()) {
			final Intent i = new Intent(getApplicationContext(),
					MainFragmentActivity.class);
			startActivity(i);
			finish();
		}
	}

	@Override
	public void onClick(View view) {
		String lang = "en";
		switch (view.getId()) {
		case R.id.english:
			lang = "en";
			break;
		case R.id.russian:
			lang = "ru";
			break;
		case R.id.chinese:
			lang = "zh";
			break;
		case R.id.japanese:
			lang = "ja";
			break;
		case R.id.korean:
			lang = "ko";
		}
		saveLanguage(lang);
		if (isMainAction()) {
			final Intent i = new Intent(getApplicationContext(),
					CheckHardwareActivity.class);
			Util.changeLanguage(lang, getResources());
			startActivity(i);
		}
		final Intent i = new Intent();
		i.putExtra(C.DATA, lang);
		setResult(RESULT_OK, i);
		finish();
	}

	private boolean isMainAction() {
		return !C.ACTION_CHANGE_LANGUAGE.equals(getIntent().getAction());
	}

	private void saveLanguage(final String lang) {
		PrefUtil.saveLanguage(lang);
	}

	@Override
	public void onBackPressed() {
		setResult(RESULT_CANCELED);
		super.onBackPressed();
	}
}
