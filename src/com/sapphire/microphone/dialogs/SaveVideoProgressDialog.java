package com.sapphire.microphone.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.sapphire.microphone.C;
import com.sapphire.microphone.R;
import com.sapphire.microphone.Util;


public class SaveVideoProgressDialog extends Dialog implements View.OnClickListener {
    private ProgressBar progressBar;
    private boolean mustAdjustOrientation = false;
    private int lastOrientation = -1;
    private final boolean isDefaultLandscape = Util.getDeviceDefaultOrientation() == Configuration.ORIENTATION_LANDSCAPE;
    private String text = null;

    public SaveVideoProgressDialog(Context context) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.save_video_progress_dialog_layout);
        progressBar = (ProgressBar) findViewById(R.id.save_video_progress);
        TextView message = (TextView) findViewById(R.id.message);
        if (text != null)
            message.setText(text);
        final OrientationEventListener orientationEventListener = new OrientationEventListener(context) {
            @Override
            public void onOrientationChanged(final int orientation) {
                if (mustAdjustOrientation) {
                    progressBar.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            for (final int o : C.ORIENTATIONS) {
                                if (orientation <= o + 30 && orientation >= o - 30) {
                                    if (o == lastOrientation) {
                                        return;
                                    }
                                    RelativeLayout mainLayout = (RelativeLayout) findViewById(R.id.root);
                                    int w = mainLayout.getWidth();
                                    int h = mainLayout.getHeight();

                                    if (!isDefaultLandscape)
                                        if (o == 270)
                                            mainLayout.setRotation(90);
                                        else if (o == 90)
                                            mainLayout.setRotation(270);
                                        else
                                            mainLayout.setRotation(o);
                                    else {
                                        if (o == 0) {
                                            mainLayout.setRotation(90);
                                        } else if (o == 90) {
                                            mainLayout.setRotation(0);
                                        } else if (o == 180) {
                                            mainLayout.setRotation(270);
                                        } else {
                                            mainLayout.setRotation(180);
                                        }
                                    }
                                    mainLayout.setTranslationX((w - h) / 2);
                                    mainLayout.setTranslationY((h - w) / 2);

                                    ViewGroup.LayoutParams lp = mainLayout.getLayoutParams();
                                    lp.height = w;
                                    lp.width = h;
                                    mainLayout.requestLayout();
                                    lastOrientation = o;
                                }
                            }
                        }
                    }, 50);
                }
            }
        };
        orientationEventListener.enable();
        setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                orientationEventListener.disable();
            }
        });
    }

    public void setProgress(final int progress) {
        progressBar.setProgress(progress);
    }

    public void setMax(final int max) {
        progressBar.setMax(max);
    }

    public void inc(final int value) {
        if (progressBar.getProgress() <= progressBar.getMax())
            progressBar.setProgress(progressBar.getProgress() + value);
    }

    public void adjustOrientation(boolean must) {
        mustAdjustOrientation = must;
    }

    public void setMessage(final String text) {
        this.text = text;
        if (progressBar != null) {
            ((TextView)findViewById(R.id.message)).setText(text);
        }
    }

    @Override
    public void onClick(View v) {
        dismiss();
    }
}
