package com.sapphire.microphone.session;


import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Looper;
import com.sapphire.microphone.C;

//синглтон
public final class Session {
    private static volatile Session INSTANCE = null;

    private boolean isConnected = false;
    private String remoteDeviceName = "";
    private volatile String recordFileName = "";
    /*Этот класс предоставляет API для управления подключением Wi-Fi. Это позволяет приложению обнаруживать
    доступные подключенные к WIFI устройства, настраивать соединение с ними.
    */
    private WifiP2pManager manager;
    //Канал, который соединяет приложение с платформой Wifi p2p.
    private WifiP2pManager.Channel channel;
    
    private BluetoothSocket bluetoothSocket;
    private boolean isSCO = false;
    //по дефолту соединение через bluetooth
    private int connectionType = C.TYPE_BLUETOOTH;

    private Session() {
    }

    public static Session current() {
        if (INSTANCE == null)
            INSTANCE = new Session();
        return INSTANCE;
    }

    //сброс соединения
    public void clear() {
        isConnected = false;
        remoteDeviceName = "";
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean isConnected) {
        this.isConnected = isConnected;
    }

    public String getRemoteDeviceName() {
        return remoteDeviceName;
    }

    public void setRemoteDeviceName(String remoteDeviceName) {
        this.remoteDeviceName = remoteDeviceName;
    }

    
    public WifiP2pManager getManager() {
        return manager;
    }

    
    public void setManager(WifiP2pManager manager, final Context context) {
        this.manager = manager;
        //initialize Регистрирует приложение с помощью инфраструктуры Wi-Fi. Эта функция должна вызываться первой 
        //перед выполнением любых операций p2p.В Looper идут колбеки
        channel = manager.initialize(context, Looper.getMainLooper(), null);
    }

    public WifiP2pManager.Channel getChannel() {
        return channel;
    }

    public String getRecordFileName() {
        return recordFileName;
    }

    public void setRecordFileName(String recordFileName) {
        this.recordFileName = recordFileName;
    }

    public BluetoothSocket getBluetoothSocket() {
        return bluetoothSocket;
    }

    public void setBluetoothSocket(BluetoothSocket bluetoothSocket) {
        this.bluetoothSocket = bluetoothSocket;
    }

    public boolean isSCO() {
        return isSCO;
    }

    public void setSCO(boolean isSCO) {
        this.isSCO = isSCO;
    }

    public void setConnectionType(int connectionType) {
        this.connectionType = connectionType;
    }

    public boolean isBluetoothConnection() {
        return connectionType == C.TYPE_BLUETOOTH;
    }
}
