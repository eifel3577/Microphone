package com.sapphire.microphone.quilt;

import android.content.Context;
import android.support.v7.widget.GridLayout;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.FrameLayout;

import java.util.ArrayList;


public class QuiltViewBase extends GridLayout {
	
	public int[] size;
	public int columns;
	public int rows;
	public int view_width = -1;
	public int view_height = -1;
	public boolean isVertical = true;
	public ArrayList<View> views;
	
	public QuiltViewBase(Context context, boolean isVertical) {
		super(context);
		this.isVertical = isVertical;
		if(view_width == -1){
			DisplayMetrics metrics = this.getResources().getDisplayMetrics();
			int width = metrics.widthPixels;
			int height = metrics.heightPixels - 120;
			view_width = width - this.getPaddingLeft() - this.getPaddingRight();
			view_height = height - this.getPaddingTop() - this.getPaddingBottom();
		}
		views = new ArrayList<View>();
		setup();
	}
	
	public void setup() {
		if(isVertical) {
			setupVertical();
		} else {
			setupHorizontal();
		}
	}
	
	public void setupVertical() {
		size = getBaseSizeVertical();
		this.setColumnCount(columns);
		this.setRowCount(-1);
		this.setOrientation(HORIZONTAL);
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
		this.setLayoutParams(params);
	}
	
	public void setupHorizontal(){
		size = getBaseSizeHorizontal();
		this.setRowCount(rows);
		this.setColumnCount(-1);
		this.setOrientation(VERTICAL);
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
		this.setLayoutParams(params);
	}
	
	public void addPatch(View view) {
        int count = this.getChildCount();
        QuiltViewPatch child = QuiltViewPatch.init(count, columns);
		GridLayout.LayoutParams params = new GridLayout.LayoutParams();
		params.width = size[0]*child.width_ratio;
		params.height = size[1]*child.height_ratio;
		params.rowSpec = GridLayout.spec(Integer.MIN_VALUE, child.height_ratio);
		params.columnSpec = GridLayout.spec(Integer.MIN_VALUE, child.width_ratio);
		view.setLayoutParams(params);
		addView(view);
		views.add(view);
	}
	
	public void refresh(){
		this.removeAllViewsInLayout();
		setup();
		for(View view : views){
			addPatch(view);
		}
	}
	
	public int[] getBaseSize(){
		int[] size = new int[2];
		
		float width_height_ratio = (3.0f/4.0f);
		
		int base_width = getBaseWidth();
		int base_height = (int) (base_width*width_height_ratio);
		
		size[0] = base_width; // width
		size[1] = base_height; // height
		return size;
	}
	
	public int[] getBaseSizeVertical(){
		int[] size = new int[2];
		
		float width_height_ratio = (3.5f/4.0f);

		int base_width = getBaseWidth();
		int base_height = (int) (base_width*width_height_ratio);
		
		size[0] = base_width; // width
		size[1] = base_height; // height
		return size;
	}
	
	public int[] getBaseSizeHorizontal(){
		int[] size = new int[2];
		
		//float width_height_ratio = (4.0f/3.0f);
		float width_height_ratio = 1;

		int base_height = getBaseHeight();
		int base_width = (int) (base_height*width_height_ratio);
		
		size[0] = base_width; // width
		size[1] = base_height; // height
		return size;
	}
	
	public int getBaseWidth() {
		if(view_width < 500) {
			columns = 2;
		} else if(view_width < 801) {
			columns = 2;
		} else if(view_width < 1201) {
			columns = 3;
		} else if(view_width < 1601) {
			columns = 4;
		} else {
			columns = 5;
		}
		return (view_width / columns);
	}
	
	public int getBaseHeight() {
		if(view_height < 350) {
			rows = 2;
		} else if(view_height < 650) {
			rows = 2;
		} else if(view_height < 1050) {
			rows = 2;
		} else if(view_height < 1250) {
			rows = 3;
		} else {
			rows = 4;
		}
		return (view_height / rows);
	}

	@Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld){
            super.onSizeChanged(xNew, yNew, xOld, yOld);
            view_width = xNew;
            view_height = yNew;
    }
	

}
