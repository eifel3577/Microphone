package com.sapphire.microphone.adapters;


import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.sapphire.microphone.R;

import java.util.List;

public class BluetoothDevicesListAdapter extends BaseAdapter {
    private final LayoutInflater inflater;
    private final List<BluetoothDevice> devices;
    private BluetoothDevice deviceConnected = null;

    public BluetoothDevicesListAdapter(final Context context, final List<BluetoothDevice> devices) {
        this.devices = devices;
        inflater = LayoutInflater.from(context);
    }

    public void addDevice(final BluetoothDevice device) {
        if (!devices.contains(device)) {
            devices.add(device);
            notifyDataSetChanged();
        }
    }

    public void setDeviceConnected(final BluetoothDevice deviceConnected) {
        this.deviceConnected = deviceConnected;
    }

    @Override
    public int getCount() {
        return devices.size();
    }

    @Override
    public BluetoothDevice getItem(int position) {
        return devices.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = inflater.inflate(R.layout.bluetooth_devices_list_item, null);
        final TextView name = (TextView) convertView.findViewById(R.id.name);
        final BluetoothDevice device = getItem(position);
        final ImageView connectionStatus = (ImageView) convertView.findViewById(R.id.connection_status);
        name.setText(device.getName());
        if (device.equals(deviceConnected)) {
            connectionStatus.setImageResource(R.drawable.connected);
        } else {
            connectionStatus.setImageResource(R.drawable.not_connected);
        }
        convertView.setTag(device);
        return convertView;
    }
}
