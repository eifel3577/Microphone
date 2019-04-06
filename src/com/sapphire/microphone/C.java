package com.sapphire.microphone;

import android.graphics.Point;
import android.util.SparseArray;
import android.util.SparseIntArray;

import java.util.UUID;

public final class C {
	public static final String INIT_CONNECTION = "INIT_CONNECTION";
	public static final String PREPARE_RECEIVING_AUDIO_STREAM = "PREPARE_RECEIVING_AUDIO_STREAM";
	public static final String RESPONSE_OK = "RESPONSE_OK";
	public static final String RESPONSE_CANCEL = "RESPONSE_CANCEL";
	public static final String CONNECTION_CANCELED = "CONNECTION_CANCELED";
	public static final String MIC = "MIC";
	public static final String CAMERA = "CAMERA";
	public static final String UNKNOWN = "UNKNOWN";
	public static final String DATA = "DATA";
	public static final String DURATION = "DURATION";
	public static final String LAST_CONNECTION_TYPE = "LAST_CONNECTION_TYPE";
	public static final String IS_REGISTERED = "IS_REGISTERED";
	public static final String CHECKED_TAB_INDEX = "CHECKED_TAB_INDEX";
	public static final String LANGUAGE = "language";
	public static final String VIDEO = "VIDEO";
	public static final String AUDIO = "AUDIO";
	public static final String ORIENTATION = "ORIENTATION";
	public static final String DELAY = "DELAY";
	public static final String SHIFT = "SHIFT";

	public static final int TYPE_BLUETOOTH = 1;
	public static final int TYPE_WIFI = 2;

	public static final UUID SERVICE_UUID = UUID
			.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

	public static final int[] CONNECTION_PORTS = new int[] { 12345, 2345, 3456,
			4567, 5678, 6789 };

	public static final String ROLE = "ROLE";
	public static final String CONNECTION_INFO = "CONNECTION_INFO";

	public static final String PACKAGE_NAME = C.class.getPackage().getName();

	public static final String CONNECTION_ESTABLISHED = "CONNECTION_ESTABLISHED";
	public static final String CONNECTION_ERROR = "CONNECTION_ERROR";

	public static final String EMPTY = "EMPTY";

	public static final String ACTION_START_RECORDING = PACKAGE_NAME
			+ ".ACTION_START_RECORDING";
	public static final String ACTION_PAUSE_RECORDING = PACKAGE_NAME
			+ ".ACTION_PAUSE_RECORDING";
	public static final String ACTION_STOP_RECORDING = PACKAGE_NAME
			+ ".ACTION_STOP_RECORDING";
	public static final String ACTION_CONNECT_WIFI = PACKAGE_NAME
			+ ".ACTION_CONNECT_WIFI";
	public static final String ACTION_CONNECT_BT = PACKAGE_NAME
			+ ".ACTION_CONNECT_BT";
	public static final String ACTION_ABORT_CONNECTION = PACKAGE_NAME
			+ ".ACTION_ABORT_CONNECTION";

	public static final String ACTION_PCM_CREATED = PACKAGE_NAME
			+ ".ACTION_PCM_CREATED";
	public static final String ACTION_CHANGE_LANGUAGE = PACKAGE_NAME
			+ ".ACTION_CHANGE_LANGUAGE";
	public static final String ACTION_CONCAT_VIDEO = PACKAGE_NAME
			+ ".ACTION_CONCAT_VIDEO";
	public static final String ACTION_CONCAT_VIDEO_LAST = PACKAGE_NAME
			+ ".ACTION_CONCAT_VIDEO_LAST";
	public static final String ACTION_MERGE_VIDEO_AUDIO = PACKAGE_NAME
			+ ".ACTION_MERGE_VIDEO_AUDIO";
	public static final String ACTION_SEND_PREVIEW = PACKAGE_NAME
			+ ".ACTION_SEND_PREVIEW";
	public static final String ACTION_AUDIO_DATA = PACKAGE_NAME
			+ ".ACTION_AUDIO_DATA";
	public static final String ACTION_RENAME_VIDEO = PACKAGE_NAME
			+ ".ACTION_RENAME_VIDEO";
	public static final String ACTION_START_MERGING = PACKAGE_NAME
			+ ".ACTION_START_MERGING";
	public static final String ACTION_STOP_MERGING = PACKAGE_NAME
			+ ".ACTION_STOP_MERGING";

	public static final String ACTION_CONCAT_START = PACKAGE_NAME
			+ ".ACTION_CONCAT_START";
	public static final String ACTION_CONCAT_END = PACKAGE_NAME
			+ ".ACTION_CONCAT_END";

	public static final String MP4_FILE_EXTENSION = ".mp4";
	public static final String WAV_FILE_EXTENSION = ".wav";

	public static final SparseArray<String> COMMANDS = new SparseArray<String>(
			5);

	static {
		COMMANDS.put(0, EMPTY);
		COMMANDS.put(1, ACTION_START_RECORDING);
		COMMANDS.put(2, ACTION_PAUSE_RECORDING);
		COMMANDS.put(3, ACTION_STOP_RECORDING);
	}

	public static final int BUFFER_SIZE = 4 * 1024;

	public static class AAC_ENCODING_PARAMS {
		public static final int CHANNELS = 1;
		public static final int SAMPLE_RATE = Util.SAMPLERATE;
		public static final int BITRATE = 128000;
		public static final int BITS_PER_SAMPLE = 16;
	}

	public static final int VIDEO_QUALITY_BITRATE_HIGH = 12000000;
	public static final int VIDEO_QUALITY_BITRATE_NORMAL = 8000000;
	public static final int VIDEO_QUALITY_BITRATE_LOW = 4000000;

	public static final int AUDIO_QUALITY_BITRATE_HIGH = 320000;
	public static final int AUDIO_QUALITY_BITRATE_NORMAL = 192000;
	public static final int AUDIO_QUALITY_BITRATE_LOW = 128000;

	public static final int VIDEO_QUALITY_HIGH = 1;
	public static final int VIDEO_QUALITY_NORMAL = 2;
	public static final int VIDEO_QUALITY_LOW = 3;

	public static final int AUDIO_QUALITY_HIGH = 1;
	public static final int AUDIO_QUALITY_NORMAL = 2;
	public static final int AUDIO_QUALITY_LOW = 3;

	public static final String VIDEO_QUALITY = "VIDEO_QUALITY";
	public static final String AUDIO_QUALITY = "AUDIO_QUALITY";

	public static final SparseIntArray VIDEO_QUALITY_MAP = new SparseIntArray(3);
	public static final SparseIntArray AUDIO_QUALITY_MAP = new SparseIntArray(3);
	static {
		VIDEO_QUALITY_MAP.put(VIDEO_QUALITY_HIGH, VIDEO_QUALITY_BITRATE_HIGH);
		VIDEO_QUALITY_MAP.put(VIDEO_QUALITY_NORMAL,
				VIDEO_QUALITY_BITRATE_NORMAL);
		VIDEO_QUALITY_MAP.put(VIDEO_QUALITY_LOW, VIDEO_QUALITY_BITRATE_LOW);

		AUDIO_QUALITY_MAP.put(AUDIO_QUALITY_HIGH, AUDIO_QUALITY_BITRATE_HIGH);
		AUDIO_QUALITY_MAP.put(AUDIO_QUALITY_NORMAL,
				AUDIO_QUALITY_BITRATE_NORMAL);
		AUDIO_QUALITY_MAP.put(AUDIO_QUALITY_LOW, AUDIO_QUALITY_BITRATE_LOW);
	}

	public static final Point PREVIEW_BITMAP_SIZE = new Point(320, 240);

	public static final int[] ORIENTATIONS = new int[] { 0, 90, 180, 270 };

}
