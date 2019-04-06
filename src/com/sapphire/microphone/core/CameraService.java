package com.sapphire.microphone.core;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.*;
import android.os.Process;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.sapphire.microphone.*;
import com.sapphire.microphone.connection.ClientBTThread;
import com.sapphire.microphone.connection.ServerConnectionHandler;
import com.sapphire.microphone.interfaces.SocketActionListener;
import com.sapphire.microphone.model.Dump;
import com.sapphire.microphone.model.RoleRequest;
import com.sapphire.microphone.model.TCPMessage;
import com.sapphire.microphone.wave.PcmAudioHelper;
import com.sapphire.microphone.wave.RiffHeaderData;
import com.sapphire.microphone.wave.WavAudioFormat;
import com.sapphire.microphone.session.Session;
import com.sapphire.microphone.speex.FrequencyBand;
import com.sapphire.microphone.speex.SpeexDecoder;
import com.sapphire.microphone.speex.SpeexEncoder;
import com.sapphire.microphone.util.SocketWrapper;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class CameraService extends Service implements SocketActionListener {
    private SocketWrapper socket = null;
    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    private final AtomicBoolean shouldRecord = new AtomicBoolean(true);
    private final AtomicBoolean recordThreadStarted = new AtomicBoolean(false);
    private final AtomicBoolean prepareToStart = new AtomicBoolean(false);
    private final AtomicInteger threadsStarted = new AtomicInteger(0);
    private final AtomicBoolean canWriteWave = new AtomicBoolean(false);
    private ServerSocket serverSocket;
    private final Executor pool = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            final Thread t = new Thread(r, "ActionsThreadPool-Thread");
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        }
    });
    private static final WavAudioFormat WAV_AUDIO_FORMAT = WavAudioFormat.mono16Bit(Util.SAMPLERATE);

    private PowerManager.WakeLock wakeLock = null;

    public CameraService() {
        super();
        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(12345));
            startListeningForIncomingConnections();
        }  catch (IOException e) {
                e.printStackTrace();
        }
    }

    private void startListeningForIncomingConnections() {
        final Thread t = new Thread("Wifi P2P server socket thread") {
            @Override
            public void run() {
                try {
                    while (true) {
                        final Socket socket = serverSocket.accept();
                        if (Session.current().isConnected()) {
                            socket.close();
                            continue;
                        }
                        socket.setTcpNoDelay(true);
                        new ServerConnectionHandler(CameraService.this, socket).start();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        final IntentFilter intentFilter = new IntentFilter(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        registerReceiver(wifiStateReceiver, intentFilter);
        acquireWakeLock();
        SpeexEncoder.loadLibrary();
    }

    private void acquireWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, MicService.class.getName());
        wakeLock.acquire();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        pool.execute(new Runnable() {
            @Override
            public void run() {
                handleCommand(intent);
            }
        });
        return START_NOT_STICKY;
    }

    private void handleCommand(final Intent intent) {
        final String action = intent.getAction();
        if (C.ACTION_CONNECT_WIFI.equals(action)) {
            return;
        }
        if (C.ACTION_CONNECT_BT.equals(action)) {
            final BluetoothDevice device = intent.getParcelableExtra(C.DATA);
            L.e(device.toString());
            createBTConnection(device);
            return;
        }
        if (socket == null) {
            return;
        }
        if (C.ACTION_START_RECORDING.equals(action)) {
            prepareToStart.set(true);
            start();
            sendTCPCommand(action);
        } else if (C.ACTION_PAUSE_RECORDING.equals(action)) {
            pause();
            sendTCPCommand(action);
        } else if (C.ACTION_STOP_RECORDING.equals(action)) {
            stop();
            sendTCPCommand(action);
        } else if (C.ACTION_SEND_PREVIEW.equals(action)) {
            final Bitmap bm = intent.getParcelableExtra(C.DATA);
            sendPreview(bm);
        }
    }

    private void start() {
        shouldRecord.set(true);
        isPaused.set(false);
        if (!recordThreadStarted.get())
            startListeningAudio();
    }

    private void pause() {
        if (recordThreadStarted.get())
            isPaused.set(true);
    }

    private void stop() {
        if (recordThreadStarted.get())
            shouldRecord.set(false);
    }

    private void createBTConnection(final BluetoothDevice device) {
        new ClientBTThread(device, this, this).start();
    }

    private void sendTCPCommand(final String action) {
        try {
            final OutputStream out = socket.getOutputStream();
            TCPMessage.tcp_message message = TCPMessage.tcp_message.newBuilder().setCommand(action).build();
            message.writeDelimitedTo(out);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendPreview(final Bitmap bm) {
        if (bm == null)
            return;
        try {
            final OutputStream out = socket.getOutputStream();
            TCPMessage.tcp_message message = TCPMessage.tcp_message.newBuilder()
                    .setData(ByteString.copyFrom(Util.bitmapToByteArray(bm)))
                    .setCommand(C.ACTION_SEND_PREVIEW)
                    .build();
            message.writeDelimitedTo(out);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSameRole(final RoleRequest roleRequest) {
        L.e("same role");
        if (roleRequest.getLocalRole().equals(C.UNKNOWN)) {
            Util.toast(getString(R.string.ERROR_REMOTE_ROLE_CHECK), this);
            return;
        }
        Util.toast(getString(R.string.SAME_ROLE, roleRequest.getRemoteRoleString(getApplicationContext())), this);
        stopSelf();
    }

    @Override
    public void onConnectionError(final boolean isCanceled) {
        L.e("connection error");
        if (!isCanceled)
            Util.toast(getString(R.string.CONNECTION_ERROR_RETRY_AGAIN), this);
        else {
            Util.toast(getString(R.string.CONNECTION_CANCELED_BY_USER), this);
        }
        sendConnectionErrorBroadcast();
        stopSelf();
        Session.current().clear();
    }

    @Override
    public void onConnectionSuccess(final SocketWrapper socket) {
        L.e("connection success");
        this.socket = socket;
        sendConnectionSuccessBroadcast();
        Session.current().setConnected(true);
        Session.current().setSCO(false);
        if (socket.isBTSocket()) {
            Session.current().setConnectionType(C.TYPE_BLUETOOTH);
        } else {
            Session.current().setConnectionType(C.TYPE_WIFI);
        }
        startListeningAudio();
    }

    private void onConnectionInterrupted() {
        L.e("connection interrupted");
        sendConnectionInterruptionBroadcast();
        stopSelf();
        Session.current().clear();
    }

    private void sendConnectionSuccessBroadcast() {
        final Intent i = new Intent(C.CONNECTION_ESTABLISHED);
        sendBroadcast(i);
    }

    private void sendConnectionInterruptionBroadcast() {
        final Intent i = new Intent(C.ACTION_ABORT_CONNECTION);
        sendBroadcast(i);
    }

    private void sendConnectionErrorBroadcast() {
        final Intent i = new Intent(C.CONNECTION_ERROR);
        sendBroadcast(i);
    }

    private void startListeningAudio() {
        final Thread t = new Thread("Camera service listenting thread") {
            @Override
            public void run() {
                L.e("start listening audio");
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
                threadsStarted.incrementAndGet();
                recordThreadStarted.set(true);
                final File f = new File(MicrofonApp.getAppDir(), Util.createFileName(C.WAV_FILE_EXTENSION));
                final SpeexDecoder decoder = new SpeexDecoder(FrequencyBand.ULTRA_WIDE_BAND);
                OutputStream fileOutputStream = null;
                int pcmSize = 0;
                try {
                    final CodedInputStream in = CodedInputStream.newInstance(socket.getInputStream());
                    fileOutputStream = new BufferedOutputStream(new FileOutputStream(f));
                    fileOutputStream.write(new RiffHeaderData(WAV_AUDIO_FORMAT, 0).asByteArray());
                    while (shouldRecord.get()) {
                        int limit = in.pushLimit(in.readRawVarint32());
                        TCPMessage.tcp_message message = TCPMessage.tcp_message.newBuilder().mergeFrom(in).build();
                        in.popLimit(limit);
                        in.resetSizeCounter();
                        if (message.hasCommand()) {
                            final String action = message.getCommand();
                            L.e(action);
                            if (C.ACTION_PAUSE_RECORDING.equals(action)) {
                                isPaused.set(true);
                                sendBroadcast(new Intent(action));
                                continue;
                            } else if (C.ACTION_STOP_RECORDING.equals(action)) {
                                break;
                            } else if (C.ACTION_START_RECORDING.equals(action)) {
                                isPaused.set(false);
                                prepareToStart.set(true);
                                continue;
                            }
                        }
                        if (!isPaused.get()) {
                            final short[] decoded = decoder.decode(message.getData().toByteArray());
                            if (decoded.length == 0)
                                L.e("zero length data!");
                            if (prepareToStart.get()) {
                                if (!f.exists()) {
                                    Util.quietClose(fileOutputStream);
                                    fileOutputStream = new BufferedOutputStream(new FileOutputStream(f));
                                    fileOutputStream.write(new RiffHeaderData(WAV_AUDIO_FORMAT, 0).asByteArray());
                                }
                                canWriteWave.set(false);
                                final Intent intent = new Intent(C.ACTION_START_RECORDING);
                                Messenger messenger = new Messenger(new MyHandler());
                                intent.putExtra(C.DATA, messenger);
                                sendBroadcast(intent);
                                prepareToStart.set(false);
                            } else if (canWriteWave.get()) {
                                final byte[] asBytes = Util.shortToByte(decoded);
                                pcmSize += asBytes.length;
                                fileOutputStream.write(asBytes);
                            }
                        } else {
                            L.e("paused");
                        }
                    }
                    L.e("stop");
                } catch (Exception e) {
                    e.printStackTrace();
                    Dump.createDump(socket.isBTSocket() ? "Bluetooth" : "WIFI", e.getMessage(), "CAMERA");
                    onConnectionInterrupted();
                    L.e("exit listening thread");
                    return;
                } finally {
                    decoder.close();
                    recordThreadStarted.set(false);
                    threadsStarted.decrementAndGet();
                    PcmAudioHelper.modifyRiffSizeData(f, pcmSize);
                    Util.quietClose(fileOutputStream);
                    sendPCMFileBroadcast(f.getAbsolutePath());
                    L.e("exit listening thread");
                }
                shouldRecord.set(true);
                isPaused.set(true);
                startListeningAudio();
            }
        };

        t.setPriority(Thread.MAX_PRIORITY);
        t.start();
    }



    private class MyHandler extends Handler {
        public MyHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            canWriteWave.set(true);
        }
    }

    private void sendPCMFileBroadcast(final String filePath) {
        final Intent i = new Intent(C.ACTION_PCM_CREATED);
        i.putExtra(C.DATA, filePath);
        sendBroadcast(i);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        L.e("DESTROY CAMERA SERVICE");
        unregisterReceiver(wifiStateReceiver);
        new Thread(threadsCloseWaiter).start();
        if (wakeLock != null)
            wakeLock.release();
    }

    private final BroadcastReceiver wifiStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            L.e(action);
            if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                if (!networkInfo.isConnected() && (socket != null && !socket.isBTSocket())) {
                    Session.current().clear();
                    stopSelf();
                }
            }
        }
    };

    private void closeSockets() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private final Runnable threadsCloseWaiter = new Runnable() {
        @Override
        public void run() {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            shouldRecord.set(false);
            if (socket != null) {
                try {
                    socket.getInputStream().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            closeSockets();
        }
    };
}
