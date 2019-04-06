package com.sapphire.microphone.fragments;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.sapphire.microphone.C;
import com.sapphire.microphone.R;
import com.sapphire.microphone.activities.*;
import com.sapphire.microphone.adapters.OtherSettingsAdapter;
import com.sapphire.microphone.model.SettingsListItem;
import com.sapphire.microphone.util.PrefUtil;

public class OtherSettingsFragment extends Fragment implements
		View.OnClickListener, AdapterView.OnItemClickListener {
	private ListView list;
	private ImageView role;
	private TextView changeRoleCaption;
	public static final int REQUEST_CHANGE_LOCALE = 10;

	public OtherSettingsFragment() {
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final View v = inflater.inflate(
				R.layout.other_settings_fragment_layout, container, false);
		list = (ListView) v.findViewById(R.id.list);
		list.setOnItemClickListener(this);
		role = (ImageView) v.findViewById(R.id.role);
		if (PrefUtil.isMic())
			role.setImageResource(R.drawable.camera_selector);
		else
			role.setImageResource(R.drawable.mic_selector);
		role.setOnClickListener(this);
		changeRoleCaption = (TextView) v.findViewById(R.id.change_role);
		return v;
	}

	@Override
	public void onClick(View v) {
		String role = PrefUtil.getRole();
		if (role.equals(C.CAMERA))
			PrefUtil.saveRole(C.MIC);
		else
			PrefUtil.saveRole(C.CAMERA);
		PrefUtil.saveLastCheckedTabIndex(0);
		getActivity().finish();
		final Intent i = new Intent(getActivity().getApplicationContext(),
				MainFragmentActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		getActivity().startActivity(i);
	}

	@Override
	public void onResume() {
		super.onResume();
		list.setAdapter(new OtherSettingsAdapter(getActivity()));
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
		final SettingsListItem item = (SettingsListItem) view.getTag();
		final String title = item.getTitle();
		if (getString(R.string.CHANGE_APP_LANGUAGE).equals(item.getTitle())) {
			final Intent intent = new Intent(getActivity(),
					ChooseLanguageActivity.class);
			intent.setAction(C.ACTION_CHANGE_LANGUAGE);
			getActivity().startActivityForResult(intent, REQUEST_CHANGE_LOCALE);
		} else if (getString(R.string.VIEW_OPPORTUNITIES).equals(title)) {
			final Intent intent = new Intent(getActivity(),
					RegistrationActivity.class);
			intent.setAction(Intent.ACTION_VIEW);
			startActivity(intent);
		} else if (getString(R.string.CHANGE_ROLE).equals(title)) {
			changeRole();
		} else if (getString(R.string.RECORD_QUALITY).equals(title)) {
			final Intent intent = new Intent(getActivity(),
					RecordQualityActivity.class);
			startActivity(intent);
		} else if ("FTP".equals(title)) {
			final Intent intent = new Intent(getActivity(), FTPActivity.class);
			startActivity(intent);
		} else if (getString(R.string.CAMERA_ROLE).equals(title)) {
			final Intent intent = new Intent(getActivity(),
					CameraRoleActivity.class);
			startActivity(intent);
		}
	}

	public void changeRole() {
		setRoleSettingsVisibility(View.VISIBLE);
	}

	private void setRoleSettingsVisibility(final int visibility) {
		changeRoleCaption.setVisibility(visibility);
		role.setVisibility(visibility);
		final int viceVersa = visibility == View.GONE ? View.VISIBLE
				: View.GONE;
		list.setVisibility(viceVersa);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		changeRoleCaption.setText(getString(R.string.CHANGE_ROLE));
	}
}
