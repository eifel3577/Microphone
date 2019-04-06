package com.sapphire.microphone.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.sapphire.microphone.R;

public class SearchDevicesAdapter extends BaseAdapter {
	private final LayoutInflater inflater;

	public SearchDevicesAdapter(final Context context) {
		inflater = LayoutInflater.from(context);
	}

	@Override
	public int getCount() {
		return 1;
	}

	@Override
	public Object getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return inflater.inflate(R.layout.searching_device_item, null);
	}
}
