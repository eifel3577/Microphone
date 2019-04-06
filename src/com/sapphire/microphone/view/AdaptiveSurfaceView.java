package com.sapphire.microphone.view;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Area;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceView;

import com.sapphire.microphone.activities.CameraActivity;
import com.sapphire.microphone.activities.DrawingView;
import com.viewpagerindicator.CirclePageIndicator;

public class AdaptiveSurfaceView extends SurfaceView {
	private int previewWidth;
	private int previewHeight;
	private float ratio;
	public Camera.Size size;
	boolean listenerSet = false;
	boolean drawingViewSet = false;
	private CameraActivity camPreview;
	DrawingView drawingView;

	public AdaptiveSurfaceView(Context context) {
		super(context);
	}

	public AdaptiveSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public AdaptiveSurfaceView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	// set the preview size of the camera for the particular
	public void setPreviewSize(Camera.Size size) {
		this.size = size;
		int screenW = getResources().getDisplayMetrics().widthPixels;
		int screenH = getResources().getDisplayMetrics().heightPixels;
		if (screenW < screenH) {
			previewWidth = size.width < size.height ? size.width : size.height;
			previewHeight = size.width >= size.height ? size.width
					: size.height;
		} else {
			previewWidth = size.width > size.height ? size.width : size.height;
			previewHeight = size.width <= size.height ? size.width
					: size.height;
		}
		ratio = previewHeight / (float) previewWidth;
		requestLayout();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int previewW = MeasureSpec.getSize(widthMeasureSpec);
		int previewWMode = MeasureSpec.getMode(widthMeasureSpec);
		int previewH = MeasureSpec.getSize(heightMeasureSpec);
		int previewHMode = MeasureSpec.getMode(heightMeasureSpec);

		int measuredWidth;
		int measuredHeight;

		if (previewWidth > 0 && previewHeight > 0) {
			measuredWidth = defineWidth(previewW, previewWMode);

			measuredHeight = (int) (measuredWidth * ratio);
			if (previewHMode != MeasureSpec.UNSPECIFIED
					&& measuredHeight > previewH) {
				measuredWidth = (int) (previewH / ratio);
				measuredHeight = previewH;
			}

			setMeasuredDimension(measuredWidth, measuredHeight);
		} else {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		}
	}

	private int defineWidth(int previewW, int previewWMode) {
		int measuredWidth;
		if (previewWMode == MeasureSpec.UNSPECIFIED) {
			measuredWidth = previewWidth;
		} else if (previewWMode == MeasureSpec.EXACTLY) {
			measuredWidth = previewW;
		} else {
			measuredWidth = Math.min(previewW, previewWidth);
		}
		return measuredWidth;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		drawingView = new DrawingView(getContext());
		// TODO Auto-generated method stub
		// return super.onTouchEvent(event);
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			float x = event.getX();
			float y = event.getY();
			float touchMajor = event.getTouchMajor();
			float touchMinor = event.getTouchMinor();
			ArrayList<Camera.Area> areas = getAreas(event.getX(), event.getY());

			Rect touchRect = new Rect((int) (x - touchMajor / 2),
					(int) (y - touchMinor / 2), (int) (x + touchMajor / 2),
					(int) (y + touchMinor / 2));

			((CameraActivity) getContext()).touchFocus(touchRect);
			if (drawingViewSet) {
				drawingView.setHaveTouch(true, touchRect);
				drawingView.invalidate();

			}

		}

		return false;
	}

	private ArrayList<Area> getAreas(float x, float y) {
		float[] coords = { x, y };

		float focus_x = coords[0];
		float focus_y = coords[1];

		int focus_size = 50;

		Rect rect = new Rect();
		rect.left = (int) focus_x - focus_size;
		rect.right = (int) focus_x + focus_size;
		rect.top = (int) focus_y - focus_size;
		rect.bottom = (int) focus_y + focus_size;
		if (rect.left < -1000) {
			rect.left = -1000;
			rect.right = rect.left + 2 * focus_size;
		} else if (rect.right > 1000) {
			rect.right = 1000;
			rect.left = rect.right - 2 * focus_size;
		}
		if (rect.top < -1000) {
			rect.top = -1000;
			rect.bottom = rect.top + 2 * focus_size;
		} else if (rect.bottom > 1000) {
			rect.bottom = 1000;
			rect.top = rect.bottom - 2 * focus_size;
		}

		ArrayList<Camera.Area> areas = new ArrayList<Camera.Area>();
		areas.add(new Camera.Area(rect, 1000));
		return areas;
	}

}
