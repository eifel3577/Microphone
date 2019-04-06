package com.sapphire.microphone.exceptions;


public class CancelException extends Exception {

    public CancelException() {
        super("remote device canceled connection");
    }
}
