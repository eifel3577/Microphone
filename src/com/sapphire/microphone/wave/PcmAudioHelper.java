package com.sapphire.microphone.wave;

import java.io.*;

import static org.jcaki.Bytes.toByteArray;

public class PcmAudioHelper {

    /**
     * Converts a pcm encoded raw audio stream to a wav file.
     *
     * @param af format
     * @param rawSource raw source file
     * @param wavTarget raw file target
     * @throws IOException thrown if an error occurs during file operations.
     */
    public static void convertRawToWav(WavAudioFormat af, File rawSource, File wavTarget) throws IOException {
        DataOutputStream dos = new DataOutputStream(new FileOutputStream(wavTarget));
        dos.write(new RiffHeaderData(af, 0).asByteArray());
        DataInputStream dis = new DataInputStream(new FileInputStream(rawSource));
        byte[] buffer = new byte[4096];
        int i;
        int total = 0;
        while ((i = dis.read(buffer)) != -1) {
            total += i;
            dos.write(buffer, 0, i);
        }
        dos.close();
        modifyRiffSizeData(wavTarget, total);
    }

    public static void modifyRiffSizeData(File wavFile, int size) {
        try {
            RandomAccessFile raf = new RandomAccessFile(wavFile, "rw");
            raf.seek(RiffHeaderData.RIFF_CHUNK_SIZE_INDEX);
            raf.write(toByteArray(size + 36, false));
            raf.seek(RiffHeaderData.RIFF_SUBCHUNK2_SIZE_INDEX);
            raf.write(toByteArray(size, false));
            raf.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
