package com.sapphire.microphone.speex;

public class SpeexEncoder {
    private final int slot;

    public SpeexEncoder(FrequencyBand band, int quality) {
        slot = allocate(band.code, quality);
    }

    @Override
    protected void finalize() throws Throwable {
        deallocate(slot);
        super.finalize();
    }

    public synchronized int getFrameSize() {
        return getFrameSize(slot);
    }

    public synchronized byte[] encode(short[] samples) {
        return encode(slot, samples);
    }

    private native static byte[] encode(int slot, short[] samples);

    private native static int getFrameSize(int slot);

    protected native static int allocate(int band_code, int quality);

    protected native static void deallocate(int slot);

    public static boolean loadLibrary() {
        try {
            System.loadLibrary("speex");
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    /*public static void main(String[] argv) {
        short[] bogus = new short[666];

        byte[] frame = new SpeexEncoder(FrequencyBand.WIDE_BAND, 9).encode(bogus);

        System.out.println(frame.length);
    }*/
}