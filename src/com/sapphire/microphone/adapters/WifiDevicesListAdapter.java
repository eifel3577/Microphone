package com.sapphire.microphone.adapters;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.sapphire.microphone.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class WifiDevicesListAdapter extends BaseAdapter {
    private final List<WifiP2pDevice> devices;
    private final LayoutInflater inflater;
    private WifiP2pDevice connectedDevice = null;

    public WifiDevicesListAdapter(final Context context, final Collection<WifiP2pDevice> devices) {
        this.devices = new ArrayList<WifiP2pDevice>(devices);
        inflater = LayoutInflater.from(context);
    }

    public void setDeviceConnected(final WifiP2pDevice connectedDevice) {
        this.connectedDevice = connectedDevice;
    }

    @Override
    public int getCount() {
        return devices.size();
    }

    @Override
    public WifiP2pDevice getItem(int i) {
        return devices.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null)
            view = inflater.inflate(R.layout.wifi_devices_list_item, null);
        final WifiP2pDevice device = getItem(i);
        final TextView name = (TextView) view.findViewById(R.id.name);
        final ImageView connectionStatus = (ImageView) view.findViewById(R.id.connection_status);
        name.setText(device.deviceName);
        if (device.equals(connectedDevice)) {
            connectionStatus.setImageResource(R.drawable.connected);
        } else {
            connectionStatus.setImageResource(R.drawable.not_connected);
        }
        view.setTag(device);
        return view;
    }
}
