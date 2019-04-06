package com.sapphire.microphone.core;

import android.app.Service;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioRecord;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Process;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.sapphire.microphone.C;
import com.sapphire.microphone.L;
import com.sapphire.microphone.R;
import com.sapphire.microphone.Util;
import com.sapphire.microphone.connection.ClientThread;
import com.sapphire.microphone.connection.ServerBTThread;
import com.sapphire.microphone.connection.ServerThread;
import com.sapphire.microphone.interfaces.SocketActionListener;
import com.sapphire.microphone.model.Dump;
import com.sapphire.microphone.model.RoleRequest;
import com.sapphire.microphone.model.TCPMessage;
import com.sapphire.microphone.session.Session;
import com.sapphire.microphone.speex.FrequencyBand;
import com.sapphire.microphone.speex.SpeexEncoder;
import com.sapphire.microphone.util.SocketWrapper;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MicService extends Service implements SocketActionListener {

	private SocketWrapper socket = null;
	private final AtomicBoolean isPaused = new AtomicBoolean(false);
	private final AtomicBoolean shouldRecord = new AtomicBoolean(false);
	private final AtomicBoolean shouldListenForCommands = new AtomicBoolean(
			true);
	private final AtomicBoolean recordThreadStarted = new AtomicBoolean(false);

	private final AtomicInteger threadsStarted = new AtomicInteger(0);
	private PowerManager.WakeLock wakeLock = null;
	private final Lock lock = new ReentrantLock();

	@Override
	public void onCreate() {
		super.onCreate();
		final IntentFilter intentFilter = new IntentFilter(
				WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
		registerReceiver(wifiStateReceiver, intentFilter);
		acquireWakeLock();
		if (!SpeexEncoder.loadLibrary()) {
			if (!SpeexEncoder.loadLibrary()) {
				L.e("couldn't load lib 'Speex'");
			}
		}
	}

	private void acquireWakeLock() {
		PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
				MicService.class.getName());
		wakeLock.acquire();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		final String action = intent.getAction();
		if (C.ACTION_CONNECT_WIFI.equals(action)) {
			L.e("service started");
			if (socket == null)
				createConnection((WifiP2pInfo) intent
						.getParcelableExtra(C.CONNECTION_INFO));
			return START_NOT_STICKY;
		}
		if (C.ACTION_CONNECT_BT.equals(action)) {
			final BluetoothSocket bluetoothSocket = Session.current()
					.getBluetoothSocket();
			createBTConnection(bluetoothSocket);
			Session.current().setBluetoothSocket(null);
			return START_NOT_STICKY;
		}
		if (socket == null) {
			Util.toast(getString(R.string.CONNECTION_NOT_ESTABLISHED),
					getApplicationContext());
			return START_NOT_STICKY;
		}
		if (C.ACTION_START_RECORDING.equals(action)) {
			sendTCPCommand(action);
			start();
		} else if (C.ACTION_PAUSE_RECORDING.equals(action)) {
			sendTCPCommand(action);
			pause();
		} else if (C.ACTION_STOP_RECORDING.equals(action)) {
			sendTCPCommand(action);
			stop();
		}
		return START_NOT_STICKY;
	}

	private void start() {
		shouldRecord.set(true);
		isPaused.set(false);
		L.e("record thread started: " + recordThreadStarted.get());
		if (!recordThreadStarted.get())
			startRecordingAudio();
	}

	private void pause() {
		if (recordThreadStarted.get())
			isPaused.set(true);
	}

	private void stop() {
		if (recordThreadStarted.get())
			shouldRecord.set(false);
	}

	private void createConnection(final WifiP2pInfo info) {
		if (info.isGroupOwner) {
			L.e("as server");
			new ServerThread(this, this).start();
		} else if (socket == null) {
			L.e("as client");
			new ClientThread(info, this, this).start();
		}
	}

	private void createBTConnection(final BluetoothSocket socket) {
		new ServerBTThread(this, socket, this).start();
	}

	private void sendTCPCommand(final String action) {
		if (socket == null)
			return;
		try {
			final OutputStream out = socket.getOutputStream();
			TCPMessage.tcp_message message = TCPMessage.tcp_message
					.newBuilder().setCommand(action).build();
			lock.lock();
			message.writeDelimitedTo(out);
			out.flush();
			lock.unlock();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onSameRole(final RoleRequest roleRequest) {
		L.e("on same role");
		if (roleRequest.getLocalRole().equals(C.UNKNOWN)) {
			Util.toast(getString(R.string.ERROR_REMOTE_ROLE_CHECK), this);
			return;
		}
		Util.toast(
				getString(R.string.SAME_ROLE, roleRequest
						.getRemoteRoleString(getApplicationContext())), this);
		stopSelf();
	}

	@Override
	public void onConnectionError(final boolean isCanceled) {
		L.e("on connection error");
		if (!isCanceled) {
			Util.toast(getString(R.string.CONNECTION_ERROR_RETRY_AGAIN), this);
			L.e("connection exception");
		} else {
			Util.toast(getString(R.string.CONNECTION_CANCELED_BY_USER), this);
			L.e("canceled by user");
		}
		sendConnectionErrorBroadcast();
		stopSelf();
		Session.current().clear();
	}

	@Override
	public void onConnectionSuccess(final SocketWrapper socket) {
		L.e("on connection success");
		this.socket = socket;
		startListeningCommands();
		sendConnectionSuccessBroadcast();
		Session.current().setConnected(true);
		Session.current().setSCO(false);
		if (socket.isBTSocket()) {
			Session.current().setConnectionType(C.TYPE_BLUETOOTH);
		} else {
			Session.current().setConnectionType(C.TYPE_WIFI);
		}
	}

	private void onConnectionInterrupted() {
		L.e("connection interrupted");
		sendConnectionInterruptionBroadcast();
		stopSelf();
		Session.current().clear();
	}

	private void sendConnectionSuccessBroadcast() {
		final Intent i = new Intent(C.CONNECTION_ESTABLISHED);
		if (socket.isBTSocket()) {
			i.putExtra(C.CONNECTION_INFO, C.ACTION_CONNECT_BT);
		} else {
			i.putExtra(C.CONNECTION_INFO, C.ACTION_CONNECT_WIFI);
		}
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

	private void startRecordingAudio() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				L.e("record thread started");
				Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
				threadsStarted.incrementAndGet();
				recordThreadStarted.set(true);
				final AudioRecord recorder = Util.createAudioRecord();
				recorder.startRecording();
				boolean isRecorderPaused = false;
				final SpeexEncoder encoder = new SpeexEncoder(
						FrequencyBand.ULTRA_WIDE_BAND, 10);
				final short[] buffer = new short[encoder.getFrameSize()];
				int i = 0;
				try {
					final OutputStream out = socket.getOutputStream();
					while (shouldRecord.get()) {
						if (!isPaused.get()) {
							if (isRecorderPaused) {
								recorder.startRecording();
								isRecorderPaused = false;
							}
							if ((recorder.read(buffer, 0, buffer.length)) != -1) {
								if (i % 10 == 0)
									sendAudioData(buffer);
								TCPMessage.tcp_message message = TCPMessage.tcp_message
										.newBuilder()
										.setData(
												ByteString.copyFrom(encoder
														.encode(buffer)))
										.build();
								lock.lock();
								message.writeDelimitedTo(out);
								lock.unlock();
							}
						} else {
							if (!isRecorderPaused) {
								recorder.stop();
								isRecorderPaused = true;
							}
						}
						i++;
					}
				} catch (Throwable e) {
					e.printStackTrace();
					Dump.createDump(socket.isBTSocket() ? "Bluetooth" : "WIFI",
							e.getMessage(), "MIC");
					onConnectionInterrupted();
				} finally {
					recorder.release();
					recordThreadStarted.set(false);
					L.e("exit recording thread");
					threadsStarted.decrementAndGet();
				}
			}
		}).start();
	}

	private void sendAudioData(final short[] shorts) {
		final Intent i = new Intent(C.ACTION_AUDIO_DATA);
		i.putExtra(C.DATA, shorts);
		sendBroadcast(i);
	}

	private void startListeningCommands() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				threadsStarted.incrementAndGet();
				try {
					final CodedInputStream in = CodedInputStream
							.newInstance(socket.getInputStream());
					while (shouldListenForCommands.get()) {
						int limit = in.pushLimit(in.readRawVarint32());
						TCPMessage.tcp_message message = TCPMessage.tcp_message
								.newBuilder().mergeFrom(in).build();
						in.popLimit(limit);
						in.resetSizeCounter();
						if (message == null)
							break;
						if (message.hasCommand()) {
							final String action = message.getCommand();
							final Intent i = new Intent(action);
							if (C.ACTION_STOP_RECORDING.equals(action)) {
								stop();
								sendBroadcast(i);
								while (true) {
									if (!recordThreadStarted.get())
										break;
								}
							} else if (C.ACTION_PAUSE_RECORDING.equals(action)) {
								pause();
								sendBroadcast(i);
							} else if (C.ACTION_START_RECORDING.equals(action)) {
								start();
								sendBroadcast(i);
							} else if (C.ACTION_SEND_PREVIEW.equals(action)) {
								final byte[] data = message.getData()
										.toByteArray();
								i.putExtra(C.DATA, data);
								sendBroadcast(i);
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					onConnectionInterrupted();
				}
				recordThreadStarted.set(false);
				L.e("exit listening thread");
				threadsStarted.decrementAndGet();
			}
		}).start();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		L.e("DESTROY MIC SERVICE");
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
			if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION
					.equals(action)) {
				NetworkInfo networkInfo = intent
						.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
				if (!networkInfo.isConnected()
						&& (socket != null && !socket.isBTSocket())) {
					Session.current().clear();
					stopSelf();
				}
			}
		}
	};

	private void closeSockets() {
		if (socket != null)
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	private final Runnable threadsCloseWaiter = new Runnable() {
		@Override
		public void run() {
			shouldListenForCommands.set(false);
			shouldRecord.set(false);
			if (socket != null) {
				try {
					socket.getInputStream().close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			while (threadsStarted.get() > 0) {
				Util.sleep(300);
			}
			closeSockets();
		}
	};
}
