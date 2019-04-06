package com.sapphire.microphone.renderer;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import com.sapphire.microphone.MicrofonApp;
import com.sapphire.microphone.Util;


public class LineGraphRenderer implements Renderer {
    private final Paint linePaint;
    private final int DP = Util.convertDpToPixel(1, MicrofonApp.getContext().getResources());

    public LineGraphRenderer(final Paint paint) {
        linePaint = paint;
    }

    @Override
    public void onRender(final Canvas canvas, final short[] data, final Rect rect) {
        canvas.drawColor(Color.TRANSPARENT);
        int h = canvas.getHeight();
        int w = canvas.getWidth();
        int index = 1;
        int scale = h / 2;
        int startX = 0;
        if (startX >= w) {
            canvas.save();
            return;
        }
        while (startX < w - 1 && index < data.length) {
            int startBaseY = data[(index - 1)] / scale;
            int stopBaseY = data[index] / scale;
            if (startBaseY > h / 2) {
                startBaseY = 2 + h / 2;
                int checkSize = h / 2;
                if (stopBaseY <= checkSize)
                    return;
                stopBaseY = 2 + h / 2;
            }
            int startY = startBaseY + h / 2;
            int stopY = stopBaseY + h / 2;
            canvas.drawLine(startX, startY, startX + DP, stopY, linePaint);
            index++;
            startX += DP;
        }
    }

    @Override
    public void clear() {

    }


}
