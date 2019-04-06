/*
 * Modified by Steelkiwi Development, Julia Zudikova
 */

/**
 * Copyright 2011, Felix Palmer
 *
 * Licensed under the MIT license:
 * http://creativecommons.org/licenses/MIT/
 */
package com.sapphire.microphone.view;

import android.content.Context;
import android.graphics.*;
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;
import android.media.audiofx.Visualizer;
import android.util.AttributeSet;
import android.view.View;
import com.sapphire.microphone.renderer.Renderer;

/**
 * A class that draws visualizations of data received from a
 * {@link Visualizer.OnDataCaptureListener#onWaveFormDataCapture } and
 * {@link Visualizer.OnDataCaptureListener#onFftDataCapture }
 */
public class VisualizerView extends View {

    private short[] shorts;
    private Rect rect = new Rect();
    private Matrix matrix = new Matrix();
    private boolean clear = false;

    private Renderer renderer;

    private Paint flashPaint = new Paint();
    private Paint fadePaint = new Paint();

    public VisualizerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        init();
    }

    public VisualizerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VisualizerView(Context context) {
        this(context, null, 0);
    }

    private void init() {
        flashPaint.setColor(Color.argb(122, 255, 255, 255));
        fadePaint.setColor(Color.argb(238, 255, 255, 255)); // Adjust alpha to change how quickly the image fades
        fadePaint.setXfermode(new PorterDuffXfermode(Mode.MULTIPLY));
    }

    public void clear() {
        clear = true;
    }

    public void setRenderer(Renderer renderer) {
        this.renderer = renderer;
    }

    public void clearRenderers() {
        if (renderer != null)
            renderer.clear();
    }

    public void updateVisualizer(short[] shorts) {
        this.shorts = shorts;
        invalidate();
    }

    boolean flash = false;

    public void flash() {
        flash = true;
        setVisibility(VISIBLE);
        setEnabled(true);
        invalidate();
    }

    Bitmap canvasBitmap;
    Canvas canvas;


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!isEnabled() && canvas != null) {
            canvas.drawColor(Color.TRANSPARENT);
        }

        rect.set(0, 0, getWidth(), getHeight());

        if (canvasBitmap == null) {
            canvasBitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Config.ARGB_8888);
        }
        if (this.canvas == null) {
            this.canvas = new Canvas(canvasBitmap);
        }

        if (clear) {
            clear = false;
            return;
        }

        if (shorts != null && renderer != null) {
            renderer.onRender(canvas, shorts, rect);
            shorts = null;
            this.canvas.drawPaint(fadePaint);
            return;
        }

        this.canvas.drawPaint(fadePaint);

        if (flash) {
            flash = false;
            this.canvas.drawPaint(flashPaint);
        }

        matrix.reset();
        canvas.drawBitmap(canvasBitmap, matrix, null);
    }
}