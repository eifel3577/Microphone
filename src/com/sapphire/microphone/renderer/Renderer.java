package com.sapphire.microphone.renderer;

import android.graphics.Canvas;
import android.graphics.Rect;

public interface Renderer {
    void onRender(final Canvas canvas, final short[] data, final Rect rect);

    void clear();
}
