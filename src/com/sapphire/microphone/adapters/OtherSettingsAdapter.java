package com.sapphire.microphone.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.sapphire.microphone.R;
import com.sapphire.microphone.model.SettingsListItem;

import java.util.ArrayList;
import java.util.List;

public class OtherSettingsAdapter extends BaseAdapter {
	private final LayoutInflater inflater;
	private final List<SettingsListItem> items = new ArrayList<SettingsListItem>();

	public OtherSettingsAdapter(final Context context) {
		inflater = LayoutInflater.from(context);
		SettingsListItem item = new SettingsListItem();
		item.setTitle(context.getString(R.string.CHANGE_ROLE));
		item.setDrawableResource(R.drawable.switch_role);
		items.add(item);
		item = new SettingsListItem();
		item.setTitle(context.getString(R.string.CHANGE_APP_LANGUAGE));
		item.setDrawableResource(R.drawable.flag_icon);
		items.add(item);
		item = new SettingsListItem();
		item.setTitle(context.getString(R.string.VIEW_OPPORTUNITIES));
		item.setDrawableResource(R.drawable.book);
		items.add(item);
		item = new SettingsListItem();
		item.setTitle(context.getString(R.string.RECORD_QUALITY));
		item.setDrawableResource(R.drawable.video);
		items.add(item);
		item = new SettingsListItem();
		item.setTitle("FTP");
		item.setDrawableResource(R.drawable.ftp);
		items.add(item);
		item = new SettingsListItem();
		item.setTitle(context.getString(R.string.CAMERA_ROLE));
		item.setDrawableResource(R.drawable.video);
		items.add(item);
	}

	@Override
	public int getCount() {
		return items.size();
	}

	@Override
	public SettingsListItem getItem(int i) {
		return items.get(i);
	}

	@Override
	public long getItemId(int i) {
		return i;
	}

	@Override
	public View getView(int i, View view, ViewGroup viewGroup) {
		if (view == null)
			view = inflater.inflate(R.layout.settings_list_item_layout, null);
		final TextView title = (TextView) view.findViewById(R.id.setting_title);
		final ImageView image = (ImageView) view.findViewById(R.id.image);
		final SettingsListItem item = getItem(i);
		title.setText(item.getTitle());
		image.setImageResource(item.getDrawableResource());
		view.setTag(item);
		return view;
	}

}
