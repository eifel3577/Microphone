package com.sapphire.microphone.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.view.OrientationEventListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.sapphire.microphone.C;
import com.sapphire.microphone.R;
import com.sapphire.microphone.Util;


public class LoadingDialog extends Dialog {
    private boolean mustAdjustOrientation = false;
    private int lastOrientation = -1;
    private final boolean isDefaultLandscape = Util.getDeviceDefaultOrientation() == Configuration.ORIENTATION_LANDSCAPE;

    public LoadingDialog(Context context) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.loading_dialog_layout);
        final OrientationEventListener orientationEventListener = new OrientationEventListener(context) {
            @Override
            public void onOrientationChanged(final int orientation) {
                if (mustAdjustOrientation) {
                    findViewById(R.id.message).postDelayed(new Runnable() {
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


    public void setMessage(final String message) {
        ((TextView)findViewById(R.id.message)).setText(message);
    }

    public void adjustOrientation(boolean must) {
        mustAdjustOrientation = must;
    }

}
