package com.sapphire.microphone.connection;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pInfo;
import android.view.View;
import com.sapphire.microphone.C;
import com.sapphire.microphone.L;
import com.sapphire.microphone.R;
import com.sapphire.microphone.Util;
import com.sapphire.microphone.dialogs.AcceptConnectionDialog;
import com.sapphire.microphone.exceptions.CancelException;
import com.sapphire.microphone.exceptions.SameRoleException;
import com.sapphire.microphone.interfaces.SocketActionListener;
import com.sapphire.microphone.model.RoleRequest;
import com.sapphire.microphone.util.PrefUtil;
import com.sapphire.microphone.util.SocketWrapper;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ClientThread extends Thread implements View.OnClickListener {
    private ObjectOutputStream os = null;
    private ObjectInputStream is = null;
    private Socket socket = null;
    private final WifiP2pInfo info;
    private final SocketActionListener listener;
    private final Context context;

    public ClientThread(WifiP2pInfo info, SocketActionListener listener, final Context context) {
        this.info = info;
        this.listener = listener;
        this.context = context;
    }

    @Override
    public void run() {
        try {
            connect();
            if (PrefUtil.isMic())
                showAcceptDialog();
            else
                dialogDismissed(true);
        } catch (Exception e) {
            e.printStackTrace();
            listener.onConnectionError(false);
            closeSocket();
        }
    }

    private void showAcceptDialog() {
        if (PrefUtil.isMic()) {
            Util.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AcceptConnectionDialog acceptConnectionDialog = new AcceptConnectionDialog(context);
                    acceptConnectionDialog.setOnClickListener(ClientThread.this);
                    acceptConnectionDialog.setCancelable(false);
                    acceptConnectionDialog.show();
                }
            });
        }
    }

    private void closeSocket() {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void connect() throws Exception {
        socket = new Socket();
        socket.bind(null);
        socket.setTcpNoDelay(true);
        for (int port : C.CONNECTION_PORTS) {
            try {
                L.e("trying to connect to port: " + port);
                socket.connect((new InetSocketAddress(info.groupOwnerAddress.getHostAddress(), port)), 5000);
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (!socket.isConnected()) {
            throw new Exception("not connected");
        }
    }

    private void sendInitMessage(boolean canConnect) throws Exception {
        os = new ObjectOutputStream(socket.getOutputStream());
        if (canConnect) {
            os.writeUTF(C.INIT_CONNECTION);
            L.e("sent message " + C.INIT_CONNECTION);
        }
        else {
            os.writeUTF(C.CONNECTION_CANCELED);
            L.e("sent message " + C.CONNECTION_CANCELED);
        }
        os.flush();
        is = new ObjectInputStream(socket.getInputStream());
        final String response = is.readUTF();
        L.e("received response: " + response);
        if (response.equals(C.RESPONSE_OK) && canConnect)
            return;
        if (response.equals(C.RESPONSE_OK) && !canConnect)
            throw new CancelException();
        if (response.equals(C.RESPONSE_CANCEL) || !canConnect) {
            throw new CancelException();
        }
        throw new IllegalStateException("connection init error");
    }

    private void sendRoleMessage() throws Exception {
        final String localRole = PrefUtil.getRole();
        os.writeUTF(localRole);
        os.flush();
        final String remoteRole = is.readUTF();
        final RoleRequest roleRequest = new RoleRequest();
        roleRequest.setLocalRole(localRole);
        roleRequest.setRemoteRole(remoteRole);
        if (roleRequest.isEqualRoles()) {
            throw new SameRoleException(roleRequest);
        }
    }

    private void sendPreperaMessage() throws Exception {
        if (PrefUtil.getRole().equals(C.MIC)) {
            os.writeUTF(C.PREPARE_RECEIVING_AUDIO_STREAM);
            os.flush();
            final String response = is.readUTF();
            if (response.equals(C.RESPONSE_OK))
                return;
        } else {
            final String message = is.readUTF();
            if (message.equals(C.PREPARE_RECEIVING_AUDIO_STREAM)) {
                os.writeUTF(C.RESPONSE_OK);
                os.flush();
                return;
            }
        }
        throw new Exception("wrong prepare message response");
    }

    @Override
    public void onClick(View v) {
        final boolean canConnect = v.getId() == R.id.ok;
        L.e("dialog click canconnect=" + canConnect);
        dialogDismissed(canConnect);
    }

    private void dialogDismissed(final boolean canConnect) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sendInitMessage(canConnect);
                    sendRoleMessage();
                    sendPreperaMessage();
                } catch (SameRoleException e) {
                    e.printStackTrace();
                    listener.onSameRole(e.getRoleRequest());
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                    listener.onConnectionError(e instanceof CancelException);
                    closeSocket();
                    return;
                }
                listener.onConnectionSuccess(new SocketWrapper(socket));
            }
        }).start();
    }
}
