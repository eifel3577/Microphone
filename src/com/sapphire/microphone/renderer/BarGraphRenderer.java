package com.sapphire.microphone.renderer;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import com.sapphire.microphone.MicrofonApp;
import com.sapphire.microphone.Util;

public class BarGraphRenderer implements Renderer {
    private int divisions;
    private Paint paint;
    private final int DP = Util.convertDpToPixel(1, MicrofonApp.getContext().getResources());

    public BarGraphRenderer(int divisions, Paint paint) {
        super();
        this.divisions = divisions;
        this.paint = paint;
    }

    @Override
    public void onRender(Canvas canvas, short[] data, Rect rect) {
        render(canvas, data, rect);
    }

    @Override
    public void clear() {

    }

    private void render(Canvas canvas, short[] data, Rect rect) {
        /*final float[] mFFTPoints = new float[data.length * 4];
        for (int i = 0; i < data.length / divisions; i++) {
            mFFTPoints[i * 4] = i * 4 * divisions;
            mFFTPoints[i * 4 + 2] = i * 4 * divisions;
            byte rfk = data[divisions * i];
            byte ifk = data[divisions * i + 1];
            float magnitude = (rfk + ifk);
            int dbValue = (int) (10 * Math.log10(magnitude));
            mFFTPoints[i * 4 + 1] = rect.height();
            mFFTPoints[i * 4 + 3] = rect.height() - (dbValue * 2 - 10);
        }*/
        int baseY = rect.bottom / 2;
        int startX = 0;
        for (short aData : data) {
            final int top = baseY + Math.abs(aData) / 50;
            final int bottom = baseY - Math.abs(aData) / 50;
            startX += DP * 2;
            canvas.drawLine(startX, baseY, startX, top, paint);
            canvas.drawLine(startX, baseY, startX, bottom, paint);
        }
        //canvas.drawLines(mFFTPoints, paint);
    }
}
