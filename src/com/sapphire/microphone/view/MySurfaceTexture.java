package com.sapphire.microphone.view;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.TextureView;
import com.sapphire.microphone.Util;

import java.util.List;


public class MySurfaceTexture extends TextureView {
    private List<Camera.Size> sizes;
    private Camera.Size optimalSize;

    public MySurfaceTexture(Context context) {
        super(context);
    }

    public MySurfaceTexture(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MySurfaceTexture(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setPreviewSizes(final List<Camera.Size> sizes) {
        this.sizes = sizes;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);

        if (sizes != null) {
            optimalSize = Util.getOptimalPreviewSize(sizes, width, height);
        }
    }

    public Camera.Size getOptimalSize() {
        return optimalSize;
    }
}
