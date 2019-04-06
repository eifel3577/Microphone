package com.sapphire.microphone.interfaces;


public interface SCOEventListener {

    public void onHeadsetDisconnected();

    public void onHeadsetConnected();

    public void onScoAudioDisconnected();

    public void onScoAudioConnected();

}
