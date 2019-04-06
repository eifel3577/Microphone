package com.sapphire.microphone.recorder;

import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import com.sapphire.microphone.C;
import com.sapphire.microphone.L;
import com.sapphire.microphone.MicrofonApp;
import com.sapphire.microphone.Util;
import com.sapphire.microphone.core.ConcatVideoService;
import com.sapphire.microphone.session.Session;
import com.sapphire.microphone.util.PrefUtil;

public class AudioRecorder {
    public static enum Status {
        STATUS_UNKNOWN,
        STATUS_READY_TO_RECORD,
        STATUS_RECORDING,
        STATUS_RECORD_PAUSED
    }

    private boolean isCameraLocked = false;
    private int increment = 0;
    private String tempFileName = "1.tmp";

    public void lockCamera() {
        if (config.camera != null && !isCameraLocked) {
            config.camera.lock();
            isCameraLocked = true;
        }
    }

    public void unlockCamera() {
        if (config.camera != null && isCameraLocked) {
            config.camera.unlock();
            isCameraLocked = false;
        }
    }

    public int getRotation() {
        return config.getOrientation();
    }

    public void setRotation(int rotation) {
        if (rotation == -1)
            rotation = 0;
        if (Util.getDeviceDefaultOrientation() == Configuration.ORIENTATION_PORTRAIT) {
            switch (rotation) {
                case 0:
                    rotation = 90;
                    break;
                case 270:
                    rotation = 0;
                    break;
                case 90:
                    rotation = 180;
                    break;
                case 180:
                    rotation = 270;
            }
        }
        config.orientation = rotation;
    }

    public static class MediaRecorderConfig {
        private Camera camera = null;
        private int orientation;
        private MediaRecorder.OnErrorListener onErrorListener;

        public MediaRecorderConfig() {}

        public void setCamera(final Camera camera) {
            this.camera = camera;
        }

        public void setOrientation(final int orientation) {
            this.orientation = orientation;
        }

        public int getOrientation() {
            return orientation;
        }

        public void setOnErrorListener(MediaRecorder.OnErrorListener onErrorListener) {
            this.onErrorListener = onErrorListener;
        }
    }

    private void startRecording() throws Exception {
        if (mediaRecorder == null) {
            mediaRecorder = new MediaRecorder();
        }
        if (config.camera != null) {
            unlockCamera();
            mediaRecorder.setCamera(config.camera);
        }
        mediaRecorder.setOrientationHint(config.getOrientation());
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        if (Session.current().isSCO()) {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION);
        }
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        if (Session.current().isSCO()) {
            L.e("media recorder SCO config");
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioEncodingBitRate(Util.getAudioBitrate());
            mediaRecorder.setAudioChannels(C.AAC_ENCODING_PARAMS.CHANNELS);
            mediaRecorder.setAudioSamplingRate(C.AAC_ENCODING_PARAMS.SAMPLE_RATE);
        }

        mediaRecorder.setOnErrorListener(config.onErrorListener);
        setVideoQuality();
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        tempFileName = getTemporaryFileName();
        mediaRecorder.setOutputFile(tempFileName);
        Exception exception = null;
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (Exception e) {
            if (mediaRecorder != null) {
                mediaRecorder.release();
                mediaRecorder = null;
            }
            exception = e;
        }
        setStatus(AudioRecorder.Status.STATUS_RECORDING);
        if (exception != null) {
            setStatus(AudioRecorder.Status.STATUS_READY_TO_RECORD);
            throw exception;
        }
    }

    private void setVideoQuality() {
        final int quality = PrefUtil.getVideoQuality();
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        switch (quality) {
            case C.VIDEO_QUALITY_LOW:
                profile = CamcorderProfile.get(CamcorderProfile.QUALITY_LOW);
                break;
            case C.VIDEO_QUALITY_HIGH:
                profile = getBestProfile();
                break;
            case C.VIDEO_QUALITY_NORMAL:
                profile = getNormalProfile(getBestProfile().quality);
        }
        mediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
        mediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);
        mediaRecorder.setVideoFrameRate(profile.videoFrameRate);
        L.e("width=" + profile.videoFrameWidth + ", height=" + profile.videoFrameHeight);
        L.e("bitrate=" + profile.videoBitRate);
        L.e("framerate=" + profile.videoFrameRate);
    }

    private static final int cameraId = 0;

    private CamcorderProfile getBestProfile() {
        if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_1080P)) {
            return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_1080P);
        } else if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_720P)) {
            return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_720P);
        } else if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_480P)) {
            return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_480P);
        } else if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_HIGH)) {
            return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH);
        } else if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_QVGA)) {
            return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_QVGA);
        } else if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_QCIF)) {
            return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_QCIF);
        } else if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_CIF)) {
            return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_CIF);
        } else {
            return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW);
        }
    }

    private CamcorderProfile getNormalProfile(final int bestProfile) {
        if (bestProfile <= CamcorderProfile.QUALITY_LOW)
            return CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW);
        CamcorderProfile result;
        for (int i = bestProfile - 1; i >=0 ;i--) {
            try {
                result = CamcorderProfile.get(cameraId, i);
                return result;
            } catch (Exception e) {
                L.e("camcorder profile " + i + " is not supported");
            }
        }
        return CamcorderProfile.get(CamcorderProfile.QUALITY_LOW);
    }

    private void pauseRecording() throws Exception {
        Exception exception = null;
        try {
            mediaRecorder.stop();
            mediaRecorder.reset();
            lockCamera();
        } catch (Exception e) {
            exception = e;
        }
        if ( exception == null ) {
            appendToFile(targetRecordFileName, tempFileName);
            setStatus(AudioRecorder.Status.STATUS_RECORD_PAUSED);
        } else {
            setStatus(AudioRecorder.Status.STATUS_READY_TO_RECORD);
            throw exception;
        }
    }

    private void stopRecord() throws Exception {
        Exception exception = null;
        try {
            mediaRecorder.release();
            mediaRecorder = null;
            lockCamera();
        } catch (Exception e) {
            exception = e;
        }
        if ( exception == null ) {
            appendToFileLast(targetRecordFileName, tempFileName);
            setStatus(Status.STATUS_RECORD_PAUSED);
        } else {
            setStatus(Status.STATUS_READY_TO_RECORD);
            throw exception;
        }
    }

    private Status mStatus;
    private volatile static MediaRecorder mediaRecorder;
    private String targetRecordFileName;
    private final MediaRecorderConfig config;

    private AudioRecorder(final String targetRecordFileName,
                          final MediaRecorderConfig mediaRecorderConfig) {
        this.targetRecordFileName = targetRecordFileName;
        config = mediaRecorderConfig;
        mStatus = Status.STATUS_UNKNOWN;
    }

    public static AudioRecorder build(final String targetFileName,
                                      final MediaRecorderConfig mediaRecorderConfig) {
        AudioRecorder rvalue = new AudioRecorder(targetFileName, mediaRecorderConfig);
        rvalue.mStatus = Status.STATUS_READY_TO_RECORD;
        return rvalue;
    }

    public void start() throws Exception {
        startRecording();
    }

    public void pause() throws Exception {
        if (mediaRecorder == null)
            return;
        pauseRecording();
    }

    public void stop() throws Exception {
        if (mediaRecorder == null)
            return;
        stopRecord();
    }

    public Status getStatus() {
        return mStatus;
    }

    public String getRecordFileName() {
        return targetRecordFileName;
    }

    public boolean isRecording() {
        return mStatus == Status.STATUS_RECORDING;
    }

    public boolean isReady() {
        return mStatus == Status.STATUS_READY_TO_RECORD;
    }

    public boolean isPaused() {
        return mStatus == Status.STATUS_RECORD_PAUSED;
    }

    private void setStatus(final Status status) {
        mStatus = status;
    }

    private String getTemporaryFileName() {
        return targetRecordFileName +  ".tmp" + ++increment;
    }

    public static void appendToFile(final String targetFileName, final String newFileName) {
        final Intent i = new Intent(MicrofonApp.getContext(), ConcatVideoService.class);
        i.setAction(C.ACTION_CONCAT_VIDEO);
        i.putExtra(C.DATA, targetFileName);
        i.putExtra(C.VIDEO, newFileName);
        MicrofonApp.getContext().startService(i);
    }

    public static void appendToFileLast(final String targetFileName, final String newFileName) {
        L.e("append to file last " + newFileName);
        final Intent i = new Intent(MicrofonApp.getContext(), ConcatVideoService.class);
        i.setAction(C.ACTION_CONCAT_VIDEO_LAST);
        i.putExtra(C.DATA, targetFileName);
        i.putExtra(C.VIDEO, newFileName);
        MicrofonApp.getContext().startService(i);
    }
}
