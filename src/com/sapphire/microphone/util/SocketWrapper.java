package com.sapphire.microphone.util;


import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class SocketWrapper extends Socket {
    private Socket socket = null;
    private BluetoothSocket bluetoothSocket = null;

    public SocketWrapper(final Socket socket) {
        this.socket = socket;
    }

    public SocketWrapper(final BluetoothSocket bluetoothSocket) {
        this.bluetoothSocket = bluetoothSocket;
    }

    @Override
    public void close() throws IOException {
        if (socket != null && !socket.isClosed())
            socket.close();
        else if (bluetoothSocket != null)
            bluetoothSocket.close();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (socket != null)
            return socket.getInputStream();
        else
            return bluetoothSocket.getInputStream();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if (socket != null)
            return socket.getOutputStream();
        else
            return bluetoothSocket.getOutputStream();
    }

    public boolean isBTSocket() {
        return bluetoothSocket != null;
    }

    public InetAddress getAddress() {
        if (!isBTSocket())
            return socket.getInetAddress();
        else
            return null;
    }
}
