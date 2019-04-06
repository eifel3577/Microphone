package com.sapphire.microphone.fragments;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.*;
import android.content.res.Configuration;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.sapphire.microphone.C;
import com.sapphire.microphone.L;
import com.sapphire.microphone.R;
import com.sapphire.microphone.Util;
import com.sapphire.microphone.activities.MainFragmentActivity;
import com.sapphire.microphone.core.MicService;
import com.sapphire.microphone.session.Session;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class DevicesFragmentMic extends Fragment implements WifiP2pManager.ConnectionInfoListener, View.OnClickListener {
    private TextView deviceName, caption;
    private ProgressDialog progressDialog;
    private BluetoothServerSocket serverSocket;
    private BluetoothDevice device;
    private final AtomicBoolean isListeningBT = new AtomicBoolean(false);
    public static final int REQUEST_DISCOVERABLE = 1000;
    private static final int DISCOVERY_DURATION = 360;
    private boolean isDiscoverable = false;

    public DevicesFragmentMic() {
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.devices_fragment_layout_mic, container, false);
        deviceName = (TextView) v.findViewById(R.id.device_name);
        caption = (TextView) v.findViewById(R.id.connected_caption);
        caption.setClickable(true);
        deviceName.setClickable(true);
        caption.setOnClickListener(this);
        deviceName.setOnClickListener(this);
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage(getActivity().getString(R.string.CONNECTING));
        progressDialog.setIndeterminate(true);
        if (!Session.current().isConnected()) {
            deviceName.setText(getString(R.string.WAITING_FOR_CONNECTION));
            discoverPeers();
        } else {
            connectionSuccess();
        }
        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        registerWifiReceiver();
        enableDiscoverable();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getActivity().unregisterReceiver(receiver);
        if (isListeningBT.get()) {
            if (serverSocket != null) {
                closeSocket(serverSocket);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (BluetoothAdapter.getDefaultAdapter().isEnabled() && !Session.current().isConnected() && !isListeningBT.get()) {
            startListeningConnections();
        }
    }

    private void registerWifiReceiver() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        intentFilter.addAction(C.CONNECTION_ESTABLISHED);
        intentFilter.addAction(C.CONNECTION_ERROR);
        intentFilter.addAction(C.ACTION_ABORT_CONNECTION);
        getActivity().registerReceiver(receiver, intentFilter);
    }

    private void discoverPeers() {
        Session.current().getManager().discoverPeers(Session.current().getChannel(), new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int reasonCode) {

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
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    if (getParentActivity().getCurrentPageNumber() == 0)
                        discoverPeers();
                } else {
                    disconnect();
                }
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                if (Session.current().isConnected() && Session.current().isBluetoothConnection())
                    return;
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                L.e("is connected: " + networkInfo.isConnected());
                if (networkInfo.isConnected()) {
                    manager.requestConnectionInfo(channel, DevicesFragmentMic.this);
                } else {
                    progressDialog.dismiss();
                    disconnect();
                }
            } else if (C.CONNECTION_ESTABLISHED.equals(action)) {
                Util.toast(getString(R.string.CONNECTION_ESTABLISHED), getActivity());
                progressDialog.dismiss();
                final boolean isBT = intent.getStringExtra(C.CONNECTION_INFO).equals(C.ACTION_CONNECT_BT);
                if (!isBT)
                    connectionSuccess();
                else
                    connectionBTSuccess();
                getParentActivity().showRecordButton();
            } else if (C.CONNECTION_ERROR.equals(action)) {
                progressDialog.dismiss();
                disconnect();
                if (!isListeningBT.get())
                    startListeningConnections();
                getParentActivity().hideRecordButton();
            } else if (C.ACTION_ABORT_CONNECTION.equals(action)) {
                disconnect();
                getParentActivity().hideRecordButton();
                if (!isListeningBT.get())
                    startListeningConnections();
            } else if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, -1);
                final int prevState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE, -1);
                if (state == BluetoothAdapter.SCAN_MODE_NONE && prevState == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                    isDiscoverable = false;
                    if (!Session.current().isConnected()) {
                        enableDiscoverable();
                    }
                } else if (state == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                    isDiscoverable = true;
                }
            }
        }
    };

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        L.e("on config");
        caption.setText(getString(R.string.CONNECTED_WITH_DEVICE));
        if (!Session.current().isConnected()) {
            deviceName.setText(getString(R.string.WAITING_FOR_CONNECTION));
        }
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
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
        }
    }

    public void connectionSuccess() {
        if (Session.current().isConnected() && !Session.current().getRemoteDeviceName().isEmpty()) {
            deviceName.setText(Session.current().getRemoteDeviceName());
            return;
        }
        Session.current().getManager().requestGroupInfo(Session.current().getChannel(), new WifiP2pManager.GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(WifiP2pGroup group) {
                if (group == null) {
                    Util.toast(getString(R.string.CONNECTION_IS_INTERRUPTED), getActivity());
                    return;
                }
                if (!group.isGroupOwner()) {
                    String name = group.getOwner().deviceName;
                    if (name == null || name.isEmpty())
                        name = group.getOwner().deviceAddress;
                    deviceName.setText(name);
                    Session.current().setRemoteDeviceName(name);
                } else {
                    for (WifiP2pDevice device : group.getClientList()) {
                        if (!device.equals(group.getOwner())) {
                            deviceName.setText(device.deviceAddress);
                            Session.current().setRemoteDeviceName(device.deviceAddress);
                            return;
                        }
                    }
                }
            }
        });
    }

    public void connectionBTSuccess() {
        if (device != null) {
            deviceName.setText(device.getName());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        L.e("destroy");
        disconnect();
    }

    public void disconnect() {
        final Intent i = new Intent(getActivity(), MicService.class);
        getActivity().stopService(i);
        Session.current().getManager().removeGroup(Session.current().getChannel(), null);
        Session.current().clear();
        deviceName.setText(getString(R.string.WAITING_FOR_CONNECTION));
        discoverPeers();
    }

    @Override
    public void onClick(View v) {
        if (Session.current().isConnected() && !deviceName.getText().toString().equals(getString(R.string.WAITING_FOR_CONNECTION))) {
            createDisconnectDialog();
        }
    }

    private void createDisconnectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setMessage(getString(R.string.CANCEL_CONNECTION))
                .setTitle(getString(R.string.CONNECTION))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.INTERRUPT), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        disconnect();
                    }
                })
                .setNegativeButton(getString(R.string.CANCEL), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builder.create().show();
    }

    private MainFragmentActivity getParentActivity() {
        return (MainFragmentActivity)getActivity();
    }

    private void enableDiscoverable() {
        if (!isDiscoverable) {
            final Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERY_DURATION);
            startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_DISCOVERABLE && resultCode == DISCOVERY_DURATION) {
            isDiscoverable = true;
        }
    }

    private void startListeningConnections() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                L.e("server thread launched");
                try {
                    isListeningBT.set(true);
                    if(Looper.myLooper() == null) {
                        Looper.prepare();
                    }
                    serverSocket = BluetoothAdapter.getDefaultAdapter().listenUsingInsecureRfcommWithServiceRecord(getString(R.string.app_name), C.SERVICE_UUID);
                    final BluetoothSocket socket = serverSocket.accept();
                    L.e("accepted socket");
                    closeSocket(serverSocket);
                    Util.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            device = socket.getRemoteDevice();
                            final Intent i = new Intent(deviceName.getContext(), MicService.class);
                            Session.current().setBluetoothSocket(socket);
                            i.setAction(C.ACTION_CONNECT_BT);
                            getActivity().startService(i);
                        }
                    });
                } catch (Exception e) {
                    closeSocket(serverSocket);
                    e.printStackTrace();
                }
                isListeningBT.set(false);
            }
        }).start();
    }

    private void closeSocket(final BluetoothServerSocket serverSocket) {
        if (serverSocket == null)
            return;
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
