package com.sapphire.microphone.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.*;
import android.content.res.Configuration;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.*;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import com.sapphire.microphone.C;
import com.sapphire.microphone.L;
import com.sapphire.microphone.R;
import com.sapphire.microphone.Util;
import com.sapphire.microphone.activities.MainFragmentActivity;
import com.sapphire.microphone.adapters.SearchDevicesAdapter;
import com.sapphire.microphone.adapters.WifiDevicesListAdapter;
import com.sapphire.microphone.interfaces.OnActionListener;
import com.sapphire.microphone.session.Session;

import java.util.ArrayList;
import java.util.List;

public class DevicesFragmentCameraWIFI extends Fragment implements WifiP2pManager.PeerListListener,
        WifiP2pManager.ConnectionInfoListener, AdapterView.OnItemClickListener, View.OnClickListener {
    private ListView list = null;
    private WifiP2pDevice device = null;
    private ProgressDialog progressDialog;
    private boolean isDiscovering = false;
    private TextView caption;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.devices_fragment_layout_camera_wifi, container, false);
        list = (ListView) v.findViewById(R.id.list);
        list.setOnItemClickListener(this);
        caption = (TextView) v.findViewById(R.id.caption);
        Util.enableWifiDirect();
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage(getActivity().getString(R.string.CONNECTING));
        progressDialog.setIndeterminate(true);
        v.findViewById(R.id.select_bt).setOnClickListener(this);
        if (Session.current().isConnected() && !Session.current().isBluetoothConnection()) {
            connectionSuccess();
        }
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        list.invalidateViews();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        registerWifiReceiver();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getActivity().unregisterReceiver(receiver);
    }

    private void registerWifiReceiver() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(C.CONNECTION_ESTABLISHED);
        intentFilter.addAction(C.CONNECTION_ERROR);
        intentFilter.addAction(C.ACTION_ABORT_CONNECTION);
        getActivity().registerReceiver(receiver, intentFilter);
    }

    private void discoverPeers() {
        if (isDiscovering)
            return;
        Session.current().getManager().discoverPeers(Session.current().getChannel(), new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                L.e("discovery success");
                list.setAdapter(new SearchDevicesAdapter(getView().getContext()));
                isDiscovering = true;
            }

            @Override
            public void onFailure(int reasonCode) {
                L.e("discovery failure" + reasonCode);
            }
        });
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            final WifiP2pManager manager = Session.current().getManager();
            final WifiP2pManager.Channel channel = Session.current().getChannel();
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state != WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    disconnect();
                    L.e("wifi p2p enabled = false");
                    return;
                }
                L.e("wifi p2p enabled = true");
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                if (manager != null) {
                    manager.requestPeers(channel, DevicesFragmentCameraWIFI.this);
                }
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                L.e("network is connected: " + networkInfo.isConnected());
                if (networkInfo.isConnected()) {
                    manager.requestConnectionInfo(channel, DevicesFragmentCameraWIFI.this);
                } else {
                    progressDialog.dismiss();
                    disconnect();
                    discoverPeers();
                }
            } else if (C.CONNECTION_ESTABLISHED.equals(action)) {
                Util.toast(getString(R.string.CONNECTION_ESTABLISHED), getActivity());
                progressDialog.dismiss();
                connectionSuccess();
            } else if (C.CONNECTION_ERROR.equals(action)) {
                progressDialog.dismiss();
                disconnect();
                discoverPeers();
            } else if (C.ACTION_ABORT_CONNECTION.equals(action)) {
                disconnect();
                discoverPeers();
            }
        }
    };

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        L.e("info available = " + info);
        if (Session.current().isConnected())
            return;
        if (info.groupFormed) {
            progressDialog.dismiss();
            final Intent i = new Intent(getActivity(), Util.getServiceClass());
            i.setAction(C.ACTION_CONNECT_WIFI);
            i.putExtra(C.CONNECTION_INFO, info);
            getActivity().startService(i);
            progressDialog.setMessage(getString(R.string.CONNECTING));
            progressDialog.show();
            isDiscovering = false;
        }
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peers) {
        if (!peers.getDeviceList().isEmpty()) {
            list.setAdapter(new WifiDevicesListAdapter(getActivity(), peers.getDeviceList()));
            list.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (list.getAdapter() instanceof WifiDevicesListAdapter) {
            final WifiP2pDevice device = (WifiP2pDevice) view.getTag();
            if (!Session.current().isConnected()) {
                connect(device);
                progressDialog.show();
            } else {
                if (device.equals(this.device)) {
                    createDisconnectDialog(null);
                } else {
                    createDisconnectDialog(new OnActionListener() {
                        @Override
                        public void onSuccess() {
                            connect(device);
                            progressDialog.show();
                        }

                        @Override
                        public void onError(Exception e) {

                        }
                    });
                }
            }
        }
    }

    private void connect(final WifiP2pDevice device) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.groupOwnerIntent = 15;
        config.wps.setup = WpsInfo.PBC;

        Session.current().getManager().connect(Session.current().getChannel(), config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                L.e("connection success");
                DevicesFragmentCameraWIFI.this.device = device;
                isDiscovering = false;
            }

            @Override
            public void onFailure(int reason) {
                L.e("connection fail + " + reason);
                progressDialog.dismiss();
            }
        });
    }

    public void connectionSuccess() {
        L.e("device = " + device);
        Session.current().setRemoteDeviceName(device.deviceName);
        if (list.getAdapter() == null) {
            final List<WifiP2pDevice> devices = new ArrayList<WifiP2pDevice>();
            devices.add(device);
            WifiDevicesListAdapter adapter = new WifiDevicesListAdapter(getActivity(), devices);
            list.setAdapter(adapter);
        }
        ((WifiDevicesListAdapter)list.getAdapter()).setDeviceConnected(device);
        list.invalidateViews();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnect();
    }


    public void disconnect() {
        Session.current().getManager().removeGroup(Session.current().getChannel(), null);
        isDiscovering = false;
        if (list.getAdapter() != null) {
            list.setAdapter(new SearchDevicesAdapter(getActivity()));
            list.invalidateViews();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.select_bt && !Session.current().isConnected()) {
            switchToBT();
            return;
        }
        if (Session.current().isConnected()) {
            createDisconnectDialog(null);
        }
    }

    private void switchToBT() {
        ((MainFragmentActivity)getActivity()).switchToBT();
    }

    private void createDisconnectDialog(final OnActionListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setMessage(getString(R.string.CANCEL_CONNECTION))
                .setTitle(getString(R.string.CONNECTION))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.INTERRUPT), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        disconnect();
                        if (listener != null)
                            listener.onSuccess();
                    }
                })
                .setNegativeButton(getString(R.string.CANCEL), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (listener != null)
                            listener.onError(new Exception());
                    }
                });
        builder.create().show();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (list != null && list.getAdapter() != null && list.getAdapter() instanceof SearchDevicesAdapter) {
            list.setAdapter(new SearchDevicesAdapter(getActivity()));
        }
        caption.setText(getString(R.string.DEVICES));
    }
}
