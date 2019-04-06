package com.sapphire.microphone.interfaces;


import com.sapphire.microphone.model.RoleRequest;
import com.sapphire.microphone.util.SocketWrapper;

public interface SocketActionListener {

    public void onSameRole(final RoleRequest roleRequest);
    public void onConnectionError(final boolean isCanceled);
    public void onConnectionSuccess(final SocketWrapper socket);
}
