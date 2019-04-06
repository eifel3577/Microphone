package com.sapphire.microphone.activities;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Camera.Face;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

public class DrawingView extends View {

	public static boolean haveFace;
	Paint drawingPaint;
	Face[] detectedFaces;
	boolean haveTouch;
	Rect touchArea;
	static boolean isManualFocusTrue = false;

	public DrawingView(Context context) {
		super(context);
		haveFace = false;
		drawingPaint = new Paint();
		drawingPaint.setColor(Color.GREEN);
		drawingPaint.setStyle(Paint.Style.STROKE);
		drawingPaint.setStrokeWidth(2);

		haveTouch = false;
	}

	public void setHaveFace(boolean h) {
		haveFace = h;
	}

	public void setHaveTouch(boolean t, Rect tArea) {
		haveTouch = t;
		touchArea = tArea;
	}

	@Override
	protected void onDraw(Canvas canvas) {

		if (haveFace) {

			int vWidth = getWidth();
			int vHeight = getHeight();

			for (int i = 0; i < detectedFaces.length; i++) {

				if (i == 0) {
					drawingPaint.setColor(Color.WHITE);
				} else {
					drawingPaint.setColor(Color.WHITE);
				}
				int l = detectedFaces[i].rect.left;
				int t = detectedFaces[i].rect.top;
				int r = detectedFaces[i].rect.right;
				int b = detectedFaces[i].rect.bottom;
				int left = (l + 1000) * vWidth / 2000;
				int top = (t + 1000) * vHeight / 2000;
				int right = (r + 1000) * vWidth / 2000;
				int bottom = (b + 1000) * vHeight / 2000;
				System.out.println("value of the left" + left);

			}
		} else {
			canvas.drawColor(Color.TRANSPARENT);

		}

		if (haveTouch) {
			isManualFocusTrue = true;
			drawingPaint.setColor(Color.WHITE);

			canvas.drawRect(touchArea.left, touchArea.top, touchArea.right,
					touchArea.bottom, drawingPaint);

			Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {

					setHaveTouch(false, new Rect(0, 0, 0, 0));
					invalidate();

				}

			}, 3000);

		}
	}
}
