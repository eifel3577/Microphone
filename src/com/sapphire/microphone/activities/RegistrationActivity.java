package com.sapphire.microphone.activities;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import com.sapphire.microphone.R;
import com.sapphire.microphone.Util;
import com.sapphire.microphone.fragments.slides.ChooseRoleSlide;
import com.sapphire.microphone.fragments.slides.EighthSlide;
import com.sapphire.microphone.fragments.slides.FifthSlide;
import com.sapphire.microphone.fragments.slides.FirstSlide;
import com.sapphire.microphone.fragments.slides.FourthSlide;
import com.sapphire.microphone.fragments.slides.SecondSlide;
import com.sapphire.microphone.fragments.slides.SeventhSlide;
import com.sapphire.microphone.fragments.slides.SixthSlide;
import com.sapphire.microphone.fragments.slides.ThirdSlide;
import com.sapphire.microphone.util.PrefUtil;
import com.viewpagerindicator.CirclePageIndicator;

public class RegistrationActivity extends FragmentActivity implements
		ViewPager.OnPageChangeListener {
	private static final int PAGES_COUNT = 9;
	private static final int INDEX = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getWindow().setFormat(PixelFormat.RGBA_8888);
		Util.changeLanguage(PrefUtil.getLanguage(getResources()
				.getConfiguration().locale.getLanguage()), getResources());
		super.onCreate(savedInstanceState);
		setContentView(R.layout.registraion_activity_layout);
		ViewPager pager = (ViewPager) findViewById(R.id.pager);
		final String action = getIntent().getAction();
		if (Intent.ACTION_VIEW.equals(action)) {
			pager.setAdapter(new ViewPagerAdapter(getSupportFragmentManager()));
		} else {
			pager.setAdapter(new ViewPagerAdapter(getSupportFragmentManager()));
		}
		pager.setOnPageChangeListener(this);
		final CirclePageIndicator indicator = (CirclePageIndicator) findViewById(R.id.indicator);
		indicator.setOnPageChangeListener(this);
		indicator.setViewPager(pager);
		indicator.setFillColor(getResources().getColor(R.color.action_bar_red));
		indicator.setRadius(Util.convertDpToPixel(10, getResources()));
		indicator.setStrokeColor(Color.TRANSPARENT);
		indicator.setCentered(false);
	}

	@Override
	public void onPageScrolled(int i, float v, int i2) {

	}

	@Override
	public void onPageSelected(int i) {

	}

	@Override
	public void onPageScrollStateChanged(int i) {

	}

	private static class ViewPagerAdapter extends FragmentPagerAdapter {
		public ViewPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int i) {
			switch (i) {
			case INDEX + 1:
				return new FirstSlide();
			case INDEX + 2:
				return new SecondSlide();
			case INDEX + 3:
				return new ThirdSlide();
			case INDEX + 4:
				return new FourthSlide();
			case INDEX + 5:
				return new FifthSlide();
			case INDEX + 6:
				return new SixthSlide();
			case INDEX + 7:
				return new EighthSlide();
			case INDEX + 8:
				return new SeventhSlide();
			case INDEX + 9:
				return new ChooseRoleSlide();
			}
			return null;
		}

		@Override
		public int getCount() {
			return PAGES_COUNT;
		}
	}

	private static class ViewOpportunitiesAdapter extends FragmentPagerAdapter {
		public ViewOpportunitiesAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int i) {
			switch (i) {
			case INDEX + 1:
				return new FirstSlide();
			case INDEX + 2:
				return new SecondSlide();
			case INDEX + 3:
				return new ThirdSlide();
			case INDEX + 4:
				return new FourthSlide();
			case INDEX + 5:
				return new FifthSlide();
			case INDEX + 6:
				return new SixthSlide();
			case INDEX + 7:
				return new EighthSlide();
			case INDEX + 8:
				return new SeventhSlide();
			}
			return null;
		}

		@Override
		public int getCount() {
			return PAGES_COUNT - 1;
		}
	}
}
