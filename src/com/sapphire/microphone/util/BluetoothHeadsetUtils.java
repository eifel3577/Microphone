package com.sapphire.microphone.util;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.CountDownTimer;
import com.sapphire.microphone.L;
import com.sapphire.microphone.interfaces.SCOEventListener;

import java.lang.reflect.Method;

public class BluetoothHeadsetUtils {
    private Context context;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothHeadset bluetoothHeadset;
    private BluetoothDevice connectedHeadset;

    private AudioManager audioManager;

    private boolean isCountDownOn;
    private boolean isHeadsetOn;
    private boolean isStarted;


    private final SCOEventListener listener;
    private final BluetoothDevice device;


    public BluetoothHeadsetUtils(SCOEventListener listener, final BluetoothDevice device, final Context context) {
        this.context = context;
        this.listener = listener;
        this.device = device;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        registerHeadsetReceiver(context);
    }

    private void registerHeadsetReceiver(final Context context) {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
        intentFilter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
        context.registerReceiver(headsetBroadcastReceiver, intentFilter);
    }

    /**
     * Call this to start BluetoothHeadsetUtils functionalities.
     *
     * @return The return value of startBluetooth() or startBluetooth()
     */
    public boolean start() {
        if (!isStarted) {
            isStarted = startBluetooth();
        }
        return isStarted;
    }

    /**
     * Should call this on onResume or onDestroy.
     * Unregister broadcast receivers and stop Sco audio connection
     * and cancel count down.
     */
    public void stop() {
        if (isStarted) {
            isStarted = false;
            stopBluetooth();
        }
    }

    /**
     * @return true if audio is connected through headset.
     */
    public boolean isOnHeadsetSco() {
        return isHeadsetOn;
    }


    /**
     * Register a headset profile listener
     *
     * @return false    if device does not support bluetooth or current platform does not supports
     * use of SCO for off call or error in getting profile proxy.
     */
    private boolean startBluetooth() {
        L.e("startBluetooth"); //$NON-NLS-1$

        // Device support bluetooth
        if (bluetoothAdapter != null) {
            if (audioManager.isBluetoothScoAvailableOffCall()) {
                // All the detection and audio connection are done in mHeadsetProfileListener
                if (bluetoothAdapter.getProfileProxy(context, mHeadsetProfileListener, BluetoothProfile.HEADSET)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * API >= 11
     * Unregister broadcast receivers and stop Sco audio connection
     * and cancel count down.
     */
    protected void stopBluetooth() {
        L.e("stopBluetooth"); //$NON-NLS-1$

        if (isCountDownOn) {
            isCountDownOn = false;
            countDownTimer.cancel();
        }

        if (bluetoothHeadset != null) {
            // Need to call stopVoiceRecognition here when the app
            // change orientation or close with headset still turns on.
            //bluetoothHeadset.stopVoiceRecognition(connectedHeadset);
            context.unregisterReceiver(headsetBroadcastReceiver);
            bluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, bluetoothHeadset);
            bluetoothHeadset = null;
        }
    }

    /**
     * API >= 11
     * Check for already connected headset and if so start audio connection.
     * Register for broadcast of headset and Sco audio connection states.
     */
    private BluetoothProfile.ServiceListener mHeadsetProfileListener = new BluetoothProfile.ServiceListener() {

        /**
         * This method is never called, even when we closeProfileProxy on onPause.
         * When or will it ever be called???
         */
        @Override
        public void onServiceDisconnected(int profile) {
            stopBluetooth();
        }

        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {

            // bluetoothHeadset is just a headset profile,
            // it does not represent a headset device.
            bluetoothHeadset = (BluetoothHeadset) proxy;

            // If a headset is connected before this application starts,
            // ACTION_CONNECTION_STATE_CHANGED will not be broadcast.
            // So we need to check for already connected headset.
            /*List<BluetoothDevice> devices = bluetoothHeadset.getConnectedDevices();
            if (devices.contains(device)) {
                // Only one headset can be connected at a time,
                // so the connected headset is at index 0.
                connectedHeadset = devices.get(0);

                listener.onHeadsetConnected();

                // Should not need count down timer, but just in case.
                // See comment below in headsetBroadcastReceiver onReceive()
                isCountDownOn = true;
                countDownTimer.start();

            } else {*/
                connectToHeadset();
            //}
        }
    };

    /**
     * API >= 11
     * Handle headset and Sco audio connection states.
     */
    private BroadcastReceiver headsetBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int state;
/*            if (action.equals(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)) {
                L.e("audio manager sco");
                state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
                L.e("audio manager sco state " + state);
                if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
                    listener.onScoAudioConnected();
                }
                return;
            }*/
            if (action.equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
                state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE,
                        BluetoothHeadset.STATE_DISCONNECTED);
                if (state == BluetoothHeadset.STATE_CONNECTED) {
                    connectedHeadset = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    // Calling startVoiceRecognition always returns false here,
                    // that why a count down timer is implemented to call
                    // startVoiceRecognition in the onTick.
                    isCountDownOn = true;
                    //countDownTimer.start();

                    audioManager.setBluetoothScoOn(true);
                    audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                    try {
                        audioManager.startBluetoothSco();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // override this if you want to do other thing when the device is connected.
                    listener.onHeadsetConnected();
                    //audioManager.startBluetoothSco();

                } else if (state == BluetoothHeadset.STATE_DISCONNECTED) {
                    // Calling stopVoiceRecognition always returns false here
                    // as it should since the headset is no longer connected.
                    if (isCountDownOn) {
                        isCountDownOn = false;
                        countDownTimer.cancel();
                    }
                    connectedHeadset = null;

                    // override this if you want to do other thing when the device is disconnected.
                    listener.onHeadsetDisconnected();
                }
            } else {
                state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_AUDIO_DISCONNECTED);
                if (state == BluetoothHeadset.STATE_AUDIO_CONNECTED) {

                    isHeadsetOn = true;

                    if (isCountDownOn) {
                        isCountDownOn = false;
                        countDownTimer.cancel();
                    }
                    //audioManager.startBluetoothSco();
                    // override this if you want to do other thing when headset audio is connected.
                    listener.onScoAudioConnected();
                } else if (state == BluetoothHeadset.STATE_AUDIO_DISCONNECTED) {
                    if (isHeadsetOn) {
                        isHeadsetOn = false;

                        // The headset audio is disconnected, but calling
                        // stopVoiceRecognition always returns true here.
                        bluetoothHeadset.stopVoiceRecognition(connectedHeadset);

                        // override this if you want to do other thing when headset audio is disconnected.
                        listener.onScoAudioDisconnected();
                    }
                }
            }
        }
    };

    /**
     * API >= 11
     * Try to connect to audio headset in onTick.
     */
    private CountDownTimer countDownTimer = new CountDownTimer(10000, 1000) {
        @Override
        public void onTick(long millisUntilFinished) {
            // First stick calls always returns false. The second stick
            // always returns true if the countDownInterval is set to 1000.
            // It is somewhere in between 500 to a 1000.
            bluetoothHeadset.startVoiceRecognition(device);
        }

        @Override
        public void onFinish() {
            // Calls to startVoiceRecognition in onStick are not successful.
            // Should implement something to inform user of this failure
            isCountDownOn = false;
        }
    };

    private void connectToHeadset() {
        try {
            Method m = bluetoothHeadset.getClass().getMethod("connect", BluetoothDevice.class);
            m.invoke(bluetoothHeadset, device);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
