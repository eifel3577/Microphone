package com.sapphire.microphone.util;

import com.sapphire.microphone.C;

import java.io.ByteArrayOutputStream;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class CompressionUtils {
    private static final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(4096);


    public static byte[] compress(byte[] data, int offset, int size) throws Exception {
        final Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION, true);
        deflater.setInput(data, offset, size);
        outputStream.reset();
        deflater.finish();
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            outputStream.write(buffer, 0, count);
        }
        outputStream.close();
        byte[] output = outputStream.toByteArray();
        deflater.end();
        return output;
    }

    public static byte[] decompress(byte[] data, int offset, int size) throws Exception {
        final Inflater inflater = new Inflater(true);
        inflater.setInput(data, offset, size);
        outputStream.reset();
        byte[] buffer = new byte[C.BUFFER_SIZE];
        while (!inflater.finished()) {
            int count = inflater.inflate(buffer, 0, size);
            outputStream.write(buffer, 0, count);
        }
        outputStream.close();
        byte[] output = outputStream.toByteArray();
        outputStream.reset();
        inflater.end();
        return output;
    }
}
