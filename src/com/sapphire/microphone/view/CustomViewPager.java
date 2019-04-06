package com.sapphire.microphone.view;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;


public class CustomViewPager extends ViewPager {
    private boolean canSwipe = true;

    public CustomViewPager(Context context) {
        super(context);
    }

    public CustomViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return canSwipe && super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return canSwipe && super.onInterceptTouchEvent(event);
    }

    public void lockPage() {
        canSwipe = false;
    }

    public void unlock() {
        canSwipe = true;
    }

    public boolean isLocked() {
        return !canSwipe;
    }
}
