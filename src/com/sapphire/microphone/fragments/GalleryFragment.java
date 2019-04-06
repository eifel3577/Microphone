package com.sapphire.microphone.fragments;

import android.app.Activity;
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.sapphire.microphone.*;
import com.sapphire.microphone.activities.VideoViewActivity;
import com.sapphire.microphone.dialogs.AcceptConnectionDialog;
import com.sapphire.microphone.quilt.QuiltView;

import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GalleryFragment extends Fragment implements View.OnClickListener {
    private QuiltView tile;
    private boolean isSelectionMode = false;
    private final List<View> selectedViews = new ArrayList<View>(30);
    ContentObserver observer;

    private final DisplayImageOptions options = new DisplayImageOptions.Builder()
            .cacheInMemory(true)
            .imageScaleType(ImageScaleType.IN_SAMPLE_INT)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .displayer(new FadeInBitmapDisplayer(300)).build();

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        observer = new MyContentObserver();
        activity.getContentResolver().registerContentObserver(Uri.fromFile(MicrofonApp.getAppDir()), true, observer);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (observer != null)
            getActivity().getContentResolver().unregisterContentObserver(observer);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.gallery_fragment_layout, container, false);
        tile = (QuiltView) v.findViewById(R.id.tile);
        L.e("memory cache size" + ImageLoader.getInstance().getMemoryCache().keys().size());
        loadPreviews(inflater);
        return v;
    }

    private void loadPreviews(final LayoutInflater inflater) {
        L.e("load previews");
        for (final View v : tile.quilt.views) {
            tile.removeQuilt(v);
        }
        tile.setup();
        final File[] videoFiles = getVideoFiles();
        if (videoFiles == null)
            return;
        for (File f : videoFiles) {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                retriever.setDataSource(f.getAbsolutePath());
            } catch (Exception e) {
                continue;
            }
            final ViewGroup v = (ViewGroup) inflater.inflate(R.layout.video_patch_view, null);
            final ImageView imageView = (ImageView) v.getChildAt(0);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setOnClickListener(this);
            imageView.setTag(f);
            tile.addPatchView(v);
            String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (time == null || time.equals("null"))
                time = "0";
            long timeInmillisec = Long.parseLong(time);
            long d = timeInmillisec / 1000;
            long hours = d / 3600;
            long minutes = (d - hours * 3600) / 60;
            long seconds = d - (hours * 3600 + minutes * 60);
            Date lastModDate = new Date(f.lastModified());
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
            ImageLoader.getInstance().displayImage(f.getAbsolutePath(), imageView, options);
            final TextView duration = (TextView) v.findViewById(R.id.text_duration);
            duration.setText(formatDuration(hours, minutes, seconds));
            final TextView date = (TextView) v.findViewById(R.id.text_date);
            date.setText(dateFormat.format(lastModDate));
            retriever.release();
        }
    }

    public void refresh() {
        loadPreviews(getActivity().getLayoutInflater());
    }

    private String formatDuration(long h, long m, long s) {
        StringBuilder sb = new StringBuilder();
        if (h > 0) {
            if (h < 10)
                sb.append("0");
            sb.append(h);
            sb.append(":");
        }
        if (m < 10)
            sb.append("0");
        sb.append(m);
        sb.append(":");
        if (s < 10)
            sb.append("0");
        sb.append(s);
        return sb.toString();
    }

    private File[] getVideoFiles() {
        return MicrofonApp.getAppDir().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.toUpperCase().endsWith(".VA" + C.MP4_FILE_EXTENSION.toUpperCase());
            }
        });
    }

    @Override
    public void onClick(View v) {
        final File f = (File) v.getTag();
        if (isSelectionMode) {
            final ViewGroup viewGroup = (ViewGroup) v.getParent();
            final CheckBox checkBox = (CheckBox) viewGroup.findViewById(R.id.checkbox);
            checkBox.setChecked(!checkBox.isChecked());
            if (checkBox.isChecked())
                selectedViews.add(viewGroup);
            else
                selectedViews.remove(viewGroup);
        } else {
            viewFile(f);
        }
    }

    private void viewFile(final File f) {
        final Intent i = new Intent(getActivity(), VideoViewActivity.class);
        i.putExtra(C.DATA, Uri.fromFile(f));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    private void startSelectionMode() {
        for (final View v : tile.quilt.views) {
            final CheckBox checkBox = (CheckBox) v.findViewById(R.id.checkbox);
            checkBox.setVisibility(View.VISIBLE);
        }
        selectedViews.clear();
        isSelectionMode = true;
    }

    private void stopSelectionMode() {
        for (final View v : tile.quilt.views) {
            final CheckBox checkBox = (CheckBox) v.findViewById(R.id.checkbox);
            checkBox.setChecked(false);
            checkBox.setVisibility(View.GONE);
        }
        selectedViews.clear();
        isSelectionMode = false;
    }

    public void startDeleteMode() {
        startSelectionMode();
    }

    public void deleteSelected() {
        if (selectedViews.isEmpty()) {
            stopSelectionMode();
            return;
        }
        final AcceptConnectionDialog d = new AcceptConnectionDialog(getActivity());
        String message;
        if (selectedViews.size() == 1) {
            final File f = (File) ((ViewGroup)selectedViews.get(0)).getChildAt(0).getTag();
            message = getString(R.string.DELETE_VIDEO, f.getName());
        } else {
            message = getString(R.string.DELETE_FILES, selectedViews.size());
        }
        d.setMessage(message);
        d.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (view.getId() == R.id.ok) {
                    for (View v : selectedViews) {
                        final File f = (File) v.findViewById(R.id.image).getTag();
                        if (f.delete()) {
                            tile.removeQuilt(v);
                        }
                    }
                }
                stopSelectionMode();
            }
        });
        d.show();
    }

    public void startShareMode() {
        startSelectionMode();
    }

    public void shareSelected() {
        if (!selectedViews.isEmpty()) {
            ArrayList<Uri> uris = new ArrayList<Uri>();
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND_MULTIPLE);
            intent.setType("video/*");
            for (final View v : selectedViews) {
                final File f = (File) v.findViewById(R.id.image).getTag();
                Uri u = Uri.fromFile(f);
                uris.add(u);
            }
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        stopSelectionMode();
    }

    public void cancelMode() {
        stopSelectionMode();
    }



    private class MyContentObserver extends ContentObserver {

        public MyContentObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            Util.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    loadPreviews(LayoutInflater.from(getActivity()));
                }
            });
        }
    }

}
