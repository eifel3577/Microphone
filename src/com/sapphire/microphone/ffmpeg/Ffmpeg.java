package com.sapphire.microphone.ffmpeg;

import com.sapphire.microphone.L;
import com.sapphire.microphone.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Ffmpeg {

    static {
        System.loadLibrary("ffmpeg");
    }

    public boolean process(List<String> args) {
        args.add(0, "ffmpeg");
        String[] params = new String[args.size()];
        args.toArray(params);
        L.e(args.toString().replaceAll(",", ""));
        return run(params);
    }

    public boolean combineAudioAndVideo(final String video, final String audio, final int rotation, int shift, final String out) throws Exception {
        final List<String> cmd = new ArrayList<String>();
        cmd.add("-y");

        cmd.add("-i");
        cmd.add(video);

        cmd.add("-i");
        cmd.add(audio);

        if (shift != 0) {
            cmd.add("-af");
            cmd.add("aresample=first_pts=" + String.format(Locale.ENGLISH, "%.2f", (shift * -1) / 1000f * Util.SAMPLERATE));
        }

        cmd.add("-c:v");
        cmd.add("copy");

        cmd.add("-c:a");
        cmd.add("aac");

        if (rotation != 0) {
            cmd.add("-metadata:s:v:0");
            cmd.add("rotate=" + rotation);
        }

        cmd.add("-strict");
        cmd.add("experimental");

        cmd.add("-loglevel");
        cmd.add("debug");

        cmd.add(out);

        return process(cmd);
    }

    public boolean concatVideo(final String txtFilePath, final String out) {
        //L.e("ffmpeg -f concat -i mylist.txt -c copy output");
        final List<String> cmd = new ArrayList<String>();
        cmd.add("-y");

        cmd.add("-f");
        cmd.add("concat");
        cmd.add("-i");
        cmd.add(txtFilePath);

        cmd.add("-c");
        cmd.add("copy");

        cmd.add("-loglevel");
        cmd.add("debug");

        cmd.add(out);

        return process(cmd);
    }

    private native boolean run(String[] args);

}