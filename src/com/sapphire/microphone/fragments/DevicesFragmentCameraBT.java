package com.sapphire.microphone.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.*;
import android.content.*;
import android.content.res.Configuration;
import android.media.AudioManager;
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
import com.sapphire.microphone.adapters.BluetoothDevicesListAdapter;
import com.sapphire.microphone.adapters.SearchDevicesAdapter;
import com.sapphire.microphone.core.CameraService;
import com.sapphire.microphone.interfaces.OnActionListener;
import com.sapphire.microphone.interfaces.SCOEventListener;
import com.sapphire.microphone.session.Session;
import com.sapphire.microphone.util.BluetoothHeadsetUtils;

import java.util.ArrayList;
import java.util.List;

public class DevicesFragmentCameraBT extends Fragment implements AdapterView.OnItemClickListener, View.OnClickListener, SCOEventListener {
    private ListView list;
    private BluetoothDevicesListAdapter adapter;
    private ProgressDialog progressDialog;
    private BluetoothDevice device;
    private BluetoothHeadsetUtils bluetoothHeadsetUtils;
    private TextView caption;

    private static final int REQUEST_ENABLE_BT = 10;

    public DevicesFragmentCameraBT() {
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        final IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(C.CONNECTION_ESTABLISHED);
        intentFilter.addAction(C.CONNECTION_ERROR);
        intentFilter.addAction(C.ACTION_ABORT_CONNECTION);
        intentFilter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        intentFilter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
        activity.registerReceiver(bluetoothDevicesReceiver, intentFilter);
        if (BluetoothAdapter.getDefaultAdapter() == null) {
            switchToWifi();
            return;
        }
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.devices_fragment_layout_camera_bt, container, false);
        caption = (TextView) v.findViewById(R.id.caption);
        list = (ListView) v.findViewById(R.id.list);
        list.setOnItemClickListener(this);
        final TextView selectWifi = (TextView) v.findViewById(R.id.select_wifi);
        selectWifi.setOnClickListener(this);
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage(getString(R.string.CONNECTING));
        progressDialog.setIndeterminate(true);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        discoverPeers();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (list.getAdapter() instanceof BluetoothDevicesListAdapter) {
            final BluetoothDevice device = (BluetoothDevice) view.getTag();
            if (!Session.current().isConnected()) {
                connect(device);
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

    private void connect(final BluetoothDevice device) {
        this.device = device;
        progressDialog.show();
        if (device.getBluetoothClass() != null && device.getBluetoothClass().hasService(BluetoothClass.Service.AUDIO)) {
            connectSCO(device);
            return;
        }
        final Intent i = new Intent(getActivity(), CameraService.class);
        i.setAction(C.ACTION_CONNECT_BT);
        i.putExtra(C.DATA, device);
        getActivity().startService(i);
    }

    private void connectSCO(final BluetoothDevice device) {
        if (device.getBondState() != BluetoothDevice.BOND_BONDED)
            L.e("bonded = " + Util.pairDevice(device));
        else {
            startHeadsetUtils();
        }
    }

    private void discoverPeers() {
        L.e("discovering peers BT");
        if (!Session.current().isConnected()) {
            final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (!adapter.isDiscovering() || list.getAdapter() == null) {
                L.e("isDiscovering");
                adapter.cancelDiscovery();
                adapter.startDiscovery();
                list.setAdapter(new SearchDevicesAdapter(getActivity()));
            }
        }
        BluetoothAdapter.getDefaultAdapter().getProfileProxy(getActivity(), new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                final List<BluetoothDevice> connectedDevices = proxy.getConnectedDevices();
                if (!connectedDevices.isEmpty()) {
                    device = proxy.getConnectedDevices().get(0);
                    if (list.getAdapter() instanceof SearchDevicesAdapter || adapter == null) {
                        adapter = new BluetoothDevicesListAdapter(getActivity(), new ArrayList<BluetoothDevice>());
                        list.setAdapter(adapter);
                    }
                    adapter.addDevice(device);
                    final AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
                    if (audioManager.isBluetoothScoOn()) {
                        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                        onScoAudioConnected();
                    } else {
                        audioManager.setBluetoothScoOn(true);
                        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                        startBluetoothSCO(audioManager);
                    }
                    BluetoothHeadset headset = (BluetoothHeadset) proxy;
                    headset.startVoiceRecognition(device);
                }
                BluetoothAdapter.getDefaultAdapter().closeProfileProxy(BluetoothProfile.HEADSET, proxy);
            }

            @Override
            public void onServiceDisconnected(int profile) {

            }
        }, BluetoothProfile.HEADSET);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getActivity().unregisterReceiver(bluetoothDevicesReceiver);
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
    }

    private final BroadcastReceiver bluetoothDevicesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                if (list.getAdapter() instanceof SearchDevicesAdapter) {
                    adapter = new BluetoothDevicesListAdapter(getActivity(), new ArrayList<BluetoothDevice>());
                    list.setAdapter(adapter);
                }
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getName() == null || device.getName().isEmpty())
                    return;
                adapter.addDevice(device);
            } else if (C.CONNECTION_ESTABLISHED.equals(action)) {
                Util.toast(getString(R.string.CONNECTION_ESTABLISHED), getActivity());
                progressDialog.dismiss();
                connectionSuccess();
            } else if (C.CONNECTION_ERROR.equals(action)) {
                progressDialog.dismiss();
                disconnect();
            } else if (C.ACTION_ABORT_CONNECTION.equals(action)) {
                disconnect();
            } else if (AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED.equals(action)) {
                L.e(action);
                int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
                if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED || state == AudioManager.SCO_AUDIO_STATE_CONNECTING) {
                    onScoAudioConnected();
                }
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);
                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                    startHeadsetUtils();
                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED && device != null){
                    disconnect();
                }
            } else if (BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_AUDIO_DISCONNECTED);
                if (state == BluetoothHeadset.STATE_AUDIO_CONNECTED) {
                    onScoAudioConnected();
                } else if (state == BluetoothHeadset.STATE_AUDIO_DISCONNECTED) {
                    onScoAudioDisconnected();
                }
            } else if (BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                progressDialog.dismiss();
                final int state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_DISCONNECTED);
                if (state == BluetoothHeadset.STATE_CONNECTED) {
                    L.e("headset state connected");
                    final AudioManager audioManager = (AudioManager)getActivity().getSystemService(Context.AUDIO_SERVICE);
                    if (audioManager.isBluetoothScoOn()) {
                        L.e("sco on");
                        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                        onScoAudioConnected();
                    } else {
                        L.e("sco off");
                        audioManager.setBluetoothScoOn(true);
                        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                        startBluetoothSCO(audioManager);
                    }
                } else if (state == BluetoothHeadset.STATE_DISCONNECTED || state == BluetoothHeadset.STATE_DISCONNECTING) {
                    L.e("headset state disconnected");
                    if (Session.current().isSCO() && Session.current().isConnected()) {
                        BluetoothAdapter.getDefaultAdapter().getProfileProxy(getActivity(), new BluetoothProfile.ServiceListener() {
                            @Override
                            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                                final BluetoothHeadset headset = (BluetoothHeadset) proxy;
                                final int s = headset.getConnectionState(device);
                                if (s == BluetoothHeadset.STATE_DISCONNECTED || s == BluetoothHeadset.STATE_DISCONNECTING) {
                                    list.setAdapter(new SearchDevicesAdapter(getActivity()));
                                    Session.current().clear();
                                    device = null;
                                    discoverPeers();
                                }
                                BluetoothAdapter.getDefaultAdapter().closeProfileProxy(BluetoothProfile.HEADSET, proxy);
                            }

                            @Override
                            public void onServiceDisconnected(int profile) {

                            }
                        }, BluetoothProfile.HEADSET);
                    }
                }
            }
        }
    };

    private void startBluetoothSCO(final AudioManager audioManager) {
        L.e("start bl sco");
        try {
            audioManager.startBluetoothSco();
        } catch (Exception e) {
            try {
                Util.delay(100);
                audioManager.startBluetoothSco();
            } catch (Exception e1) {
                try {
                    Util.delay(100);
                    audioManager.startBluetoothSco();
                } catch (Exception e2) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void startHeadsetUtils() {
        if (bluetoothHeadsetUtils == null) {
            bluetoothHeadsetUtils = new BluetoothHeadsetUtils(DevicesFragmentCameraBT.this, device, getActivity());
            bluetoothHeadsetUtils.start();
        }
    }

    @Override
    public void onClick(View v) {
        if (!Session.current().isConnected())
            switchToWifi();
    }

    private void switchToWifi() {
        ((MainFragmentActivity)getActivity()).switchToWifi();
    }

    public void connectionSuccess() {
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
        Session.current().setRemoteDeviceName(device.getName());
        if (list.getAdapter() instanceof BluetoothDevicesListAdapter) {
            ((BluetoothDevicesListAdapter)list.getAdapter()).setDeviceConnected(device);
        } else {
            final List<BluetoothDevice> devices = new ArrayList<BluetoothDevice>();
            devices.add(device);
            adapter = new BluetoothDevicesListAdapter(getActivity(), devices);
            list.setAdapter(adapter);
            adapter.setDeviceConnected(device);
        }
        list.invalidateViews();
    }

    public void disconnect() {
        final Intent i = new Intent(getActivity(), CameraService.class);
        getActivity().stopService(i);
        if (Session.current().isSCO() && Session.current().isConnected()) {
            final AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
            audioManager.stopBluetoothSco();
            audioManager.setMode(AudioManager.MODE_NORMAL);
        }
        discoverPeers();
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode != Activity.RESULT_OK) {
            switchToWifi();
        } else {
            discoverPeers();
        }
    }

    @Override
    public void onHeadsetDisconnected() {
        L.e("on headset disconnected");
    }

    @Override
    public void onHeadsetConnected() {
        L.e("on headset connected");
    }

    @Override
    public void onScoAudioDisconnected() {
        disconnect();
    }

    @Override
    public void onScoAudioConnected() {
        L.e("on sco connected");
        Session.current().setSCO(true);
        Session.current().setConnected(true);
        if (device == null) {
            BluetoothAdapter.getDefaultAdapter().getProfileProxy(getActivity(),new BluetoothProfile.ServiceListener() {
                @Override
                public void onServiceConnected(int profile, BluetoothProfile proxy) {
                    if (proxy.getConnectedDevices().isEmpty())
                        return;
                    device = proxy.getConnectedDevices().get(0);
                    connectionSuccess();
                    final BluetoothHeadset headset = (BluetoothHeadset) proxy;
                    headset.startVoiceRecognition(device);
                    BluetoothAdapter.getDefaultAdapter().closeProfileProxy(profile, proxy);
                }

                @Override
                public void onServiceDisconnected(int profile) {

                }
            }, BluetoothProfile.HEADSET);
        } else {
            connectionSuccess();
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bluetoothHeadsetUtils != null)
            bluetoothHeadsetUtils.stop();
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (list != null && list.getAdapter() != null && list.getAdapter() instanceof SearchDevicesAdapter) {
            list.setAdapter(new SearchDevicesAdapter(getActivity()));
        }
        caption.setText(getString(R.string.DEVICES));
    }
}
