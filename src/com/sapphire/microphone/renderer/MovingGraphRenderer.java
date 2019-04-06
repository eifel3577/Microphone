package com.sapphire.microphone.renderer;


import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import com.sapphire.microphone.MicrofonApp;
import com.sapphire.microphone.Util;
import com.sapphire.microphone.util.PrefUtil;

import java.util.LinkedList;

public class MovingGraphRenderer implements Renderer {
    private final LinkedList<Short> data;
    private int MAX_SIZE;
    private final int DP = Util.convertDpToPixel(1, MicrofonApp.getContext().getResources());
    private final Paint paint;

    public MovingGraphRenderer(final Paint paint) {
        data = new LinkedList<Short>();
        this.paint = paint;
        setMaxSize();
    }

    private void setMaxSize() {
        final DisplayMetrics metrics = MicrofonApp.getContext().getResources().getDisplayMetrics();
        final int min = metrics.widthPixels < metrics.heightPixels ? metrics.widthPixels : metrics.heightPixels;
        final int max = metrics.widthPixels == min ? metrics.heightPixels : metrics.widthPixels;
        if (PrefUtil.getOrientation() == Configuration.ORIENTATION_PORTRAIT)
            MAX_SIZE = (int) (Util.convertPixelsToDp(min, MicrofonApp.getContext().getResources()));
        else
            MAX_SIZE = (int) (Util.convertPixelsToDp(max, MicrofonApp.getContext().getResources()));
    }

    @Override
    public void onRender(Canvas canvas, short[] shorts, Rect rect) {
        setMaxSize();
        while (data.size() > MAX_SIZE) {
            data.removeFirst();
        }
        short avg = getAvg(shorts);
        if (avg < DP * 20)
            avg = (short) (DP * 20);
        data.add(avg);
        int baseY = rect.bottom / 2;
        int startX = 0;
        for (short aData : data) {
            final int top = baseY + (aData / 50) * DP;
            final int bottom = baseY - (aData / 50) * DP;
            startX += DP;
            canvas.drawLine(startX, baseY, startX, top, paint);
            canvas.drawLine(startX, baseY, startX, bottom, paint);
        }
    }

    @Override
    public void clear() {
        data.clear();
    }

    private short getAvg(final short[] d) {
        int result = 0;
        for (final short i : d) {
            result += Math.abs(i);
        }
        return (short) (result/d.length);
    }
}
