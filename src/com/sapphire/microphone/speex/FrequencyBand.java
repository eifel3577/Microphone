package com.sapphire.microphone.speex;

public enum FrequencyBand {
    /**
     * 8 KHz sample rate
     */
    NARROW_BAND(0),
    /**
     * 16 KHz sample rate
     */
    WIDE_BAND(1),
    /**
     * 32 KHz sample rate
     */
    ULTRA_WIDE_BAND(2);

    public final int code;

    FrequencyBand(int code) {
        this.code = code;
    }
}
