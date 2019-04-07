package com.sapphire.microphone.activities;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.download.ImageDownloader;
import com.sapphire.microphone.C;
import com.sapphire.microphone.L;
import com.sapphire.microphone.R;
import com.sapphire.microphone.Util;
import com.sapphire.microphone.core.CameraService;
import com.sapphire.microphone.dialogs.SaveVideoDialog;
import com.sapphire.microphone.fragments.*;
import com.sapphire.microphone.session.Session;
import com.sapphire.microphone.util.PrefUtil;
import com.sapphire.microphone.view.CustomViewPager;
import com.sapphire.microphone.view.PagerSlidingTabStrip;
import com.yandex.metrica.Counter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainFragmentActivity extends FragmentActivity implements
		ViewPager.OnPageChangeListener {
	
	private CustomViewPager pager;
	private final static int MODE_NONE = 1;
	private final static int MODE_DELETE = 2;
	private final static int MODE_SHARE = 3;
	private int currentMode = MODE_NONE;
	private PagerSlidingTabStrip tabs;

	private final static int SETTINGS_TYPE_ROLE = 1;
	private int currentSettingsType = SETTINGS_TYPE_ROLE;

	private FragmentStatePagerAdapter pagerAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PrefUtil.saveOrientation(getResources().getConfiguration().orientation);
		setContentView(R.layout.main);
		initUI();
		initUIL();
		//установка WifiP2pManager.
		//Context.WIFI_P2P_SERVICE получает WifiP2pManager для обработки управления одноранговыми соединениями Wi-Fi.
		Session.current().setManager(
				(WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE),
				getApplicationContext());
		//если не включен WifiDirect
		if (!Util.enableWifiDirect()) {
			//появляется диалог чтобы его включили иначе будет использоваться только Bluetooth
			final SaveVideoDialog d = new SaveVideoDialog(this);
			d.setMessage(getString(R.string.ENABLE_WIFI_DIRECT));
			d.show();
		}
	}

	
	private void initUIL() {
		//заменить эту хрень на Picasso
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(
				getApplicationContext()).denyCacheImageMultipleSizesInMemory()
				.memoryCacheSize(4 * 1024 * 1024)
				.diskCacheSize(100 * 1024 * 1024)
				.imageDownloader(new ImageDownloader() {
					@Override
					public InputStream getStream(String s, Object o)
							throws IOException {
						final Bitmap bitmap = ThumbnailUtils
								.createVideoThumbnail(s,
										MediaStore.Video.Thumbnails.MINI_KIND);
						if (bitmap == null)
							return new ByteArrayInputStream(new byte[0]);
						final ByteArrayOutputStream bos = new ByteArrayOutputStream();
						bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos);
						byte[] bitmapdata = bos.toByteArray();
						return new ByteArrayInputStream(bitmapdata);
					}
				}).build();
		ImageLoader.getInstance().init(config);
	}

	//отображение viewPager
	private void initUI() {
		pager = (CustomViewPager) findViewById(R.id.pager);
		pager.setOffscreenPageLimit(100);
		//в зависимости от того что выбрано микрофон или камера разный viewPager
		if (PrefUtil.isMic())
			pagerAdapter = new MicPagerAdapter(getSupportFragmentManager());
		else
			pagerAdapter = new CameraPagerAdapter(getSupportFragmentManager());
		pager.setAdapter(pagerAdapter);
		tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
		tabs.setViewPager(pager);
		tabs.setIndicatorColorResource(R.color.action_bar_red);
		tabs.setUnderlineColorResource(R.color.action_bar_red);
		tabs.setTextColorResource(R.color.white);
		tabs.setOnPageChangeListener(this);

		//устанавливается фильтр активити строка пакет + ACTION_START_RECORDING
		final IntentFilter intentFilter = new IntentFilter(
				C.ACTION_START_RECORDING);
		//устанавливается action что изменился системный язык
		intentFilter.addAction(Intent.ACTION_LOCALE_CHANGED);
		//региструруется broadcast который будет отлавливать события что изменился системный язык
		registerReceiver(actionsReceiver, intentFilter);
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		Counter.sharedInstance().onResumeActivity(this);
		// pagerAdapter.notifyDataSetChanged();
		//обновляет табы
		tabs.notifyDataSetChanged();
		//кладется сообщение в очередь handler через 3 секунды
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				//отображает страницу в pager, если ее номер сохранен в префе.По дефолту покажет страницу с индексом 0
				pager.setCurrentItem(PrefUtil.getLastCheckedTabIndex(), false);
			}
		}, 300);

	}

	@Override
	public void onPageScrolled(int i, float v, int i2) {
	}

	//Этот метод будет вызван, когда будет выбрана новая страница
	@Override
	public void onPageSelected(int i) {
		if (i != 2)
			currentMode = MODE_NONE;
		//обновляет меню,если оно менялось
		invalidateOptionsMenu();
		//номерстраницы ложится в преф
		PrefUtil.saveLastCheckedTabIndex(i);
	}

	@Override
	public void onPageScrollStateChanged(int i) {
	}

	private class MicPagerAdapter extends FragmentStatePagerAdapter {

		public MicPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int i) {
			switch (i) {
			case 0:
					
				return new DevicesFragmentMic();
			case 1:
				return new MicRecordFragment();
			case 2:
				return new OtherSettingsFragment();

			}
			return null;
		}

		@Override
		public int getCount() {
			return 3;
		}

		@Override
		public void unregisterDataSetObserver(DataSetObserver observer) {
			if (observer != null) {
				try {
					super.unregisterDataSetObserver(observer);
				} catch (Exception ignored) {
				}
			}
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
			case 0:
					//устройства
				return getString(R.string.DEVICES);
			case 1:
					//запись
				return getString(R.string.RECORD);
			case 2:
					//настройки
				return getString(R.string.SETTINGS);
			}
			return "";
		}

		
		@Override
		public int getItemPosition(Object object) {
			if (!(object instanceof DevicesFragmentMic)
					&& !(object instanceof MicRecordFragment)
					&& !(object instanceof OtherSettingsFragment))
				return POSITION_UNCHANGED;
			return POSITION_NONE;
		}
	}

	private class CameraPagerAdapter extends FragmentStatePagerAdapter {

		public CameraPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int i) {
			switch (i) {
			case 0:
					
				if (PrefUtil.getLastConnectionType() == C.TYPE_BLUETOOTH
						&& BluetoothAdapter.getDefaultAdapter() != null) {
					return new DevicesFragmentCameraBT();
				} else {
					return new DevicesFragmentCameraWIFI();
				}
			case 1:
				return new CameraRecordFragment();
			case 2:
				return new GalleryFragment();
			case 3:
				return new OtherSettingsFragment();
			}
			return null;
		}

		@Override
		public int getCount() {
			return 4;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
			case 0:
				return getString(R.string.DEVICES);
			case 1:
				return getString(R.string.RECORD);
			case 2:
				return getString(R.string.DATA);
			case 3:
				return getString(R.string.SETTINGS);
			}
			return "";
		}

		@Override
		public void unregisterDataSetObserver(DataSetObserver observer) {
			if (observer != null) {
				try {
					super.unregisterDataSetObserver(observer);
				} catch (Exception ignored) {
				}
			}
		}

		@Override
		public int getItemPosition(Object object) {
			if (!(object instanceof DevicesFragmentCameraBT)
					&& !(object instanceof DevicesFragmentCameraWIFI)
					&& !(object instanceof CameraRecordFragment)
					&& !(object instanceof OtherSettingsFragment))
				return POSITION_UNCHANGED;
			return POSITION_NONE;
		}
	}

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (getCurrentPageNumber() == 2 && !PrefUtil.isMic()) {
			if (currentMode == MODE_NONE)
				getMenuInflater().inflate(R.menu.main_menu, menu);
			else if (currentMode == MODE_SHARE)
				getMenuInflater().inflate(R.menu.main_menu_mode_share, menu);
			else
				getMenuInflater().inflate(R.menu.main_menu_mode_delete, menu);
		} else if (getCurrentPageNumber() == 0) {
			getMenuInflater().inflate(R.menu.update_menu, menu);
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.delete:
			if (currentMode == MODE_NONE) {
				currentMode = MODE_DELETE;
				getGalleryFragment().startDeleteMode();
			} else if (currentMode == MODE_DELETE) {
				getGalleryFragment().deleteSelected();
				currentMode = MODE_NONE;
			}
			invalidateOptionsMenu();
			return true;
		case R.id.share:
			if (currentMode == MODE_NONE) {
				currentMode = MODE_SHARE;
				getGalleryFragment().startShareMode();
			} else if (currentMode == MODE_SHARE) {
				currentMode = MODE_NONE;
				getGalleryFragment().shareSelected();
			}
			invalidateOptionsMenu();
			return true;
		case R.id.refresh:
			if (!Session.current().isConnected())
				refreshFragmentByIndex(0);
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public int getCurrentPageNumber() {
		return pager.getCurrentItem();
	}

	private void refreshFragmentByIndex(final int index) {
		FragmentManager manager = getSupportFragmentManager();
		android.support.v4.app.FragmentTransaction transaction = manager
				.beginTransaction();
		transaction.remove(manager.getFragments().get(index)).commit();
		try {
			pagerAdapter.notifyDataSetChanged();
		} catch (Exception ignored) {
		}
		// invalidateOptionsMenu();
	}

	public void switchToWifi() {
		PrefUtil.saveLastConnectionType(C.TYPE_WIFI);
		FragmentManager manager = getSupportFragmentManager();
		android.support.v4.app.FragmentTransaction transaction = manager
				.beginTransaction();
		transaction.remove(manager.getFragments().get(0)).commit();
		try {
			pagerAdapter.notifyDataSetChanged();
		} catch (Exception ignored) {
		}
		invalidateOptionsMenu();
	}

	public void switchToBT() {
		PrefUtil.saveLastConnectionType(C.TYPE_BLUETOOTH);
		FragmentManager manager = getSupportFragmentManager();
		android.support.v4.app.FragmentTransaction transaction = manager
				.beginTransaction();
		transaction.remove(manager.getFragments().get(0)).commit();
		try {
			pagerAdapter.notifyDataSetChanged();
		} catch (Exception ignored) {
		}
		invalidateOptionsMenu();
	}

	public void switchRole() {
		final FragmentStatePagerAdapter adapter;
		if (PrefUtil.isMic())
			adapter = new MicPagerAdapter(getSupportFragmentManager());
		else
			adapter = new CameraPagerAdapter(getSupportFragmentManager());
		pager.setAdapter(adapter);
		tabs.notifyDataSetChanged();
		pagerAdapter = adapter;
		invalidateOptionsMenu();
	}

	public void showRecordButton() {
		FragmentManager manager = getSupportFragmentManager();
		for (final Fragment fragment : manager.getFragments()) {
			if (fragment instanceof MicRecordFragment) {
				((MicRecordFragment) fragment).showRecordButton();
				return;
			}
		}
	}

	public void lockPage() {
		FragmentManager manager = getSupportFragmentManager();
		for (final Fragment fragment : manager.getFragments()) {
			if (fragment instanceof MicRecordFragment) {
				pager.setCurrentItem(1);
				pager.lockPage();
				invalidateOptionsMenu();
				return;
			}
		}
	}

	public void unlock() {
		pager.unlock();
		invalidateOptionsMenu();
	}

	public void hideRecordButton() {
		FragmentManager manager = getSupportFragmentManager();
		for (final Fragment fragment : manager.getFragments()) {
			if (fragment instanceof MicRecordFragment) {
				((MicRecordFragment) fragment).hideRecordButton();
				return;
			}
		}
	}

	public void switchSettings() {
		currentSettingsType = 3 - currentSettingsType;
		FragmentManager manager = getSupportFragmentManager();
		android.support.v4.app.FragmentTransaction transaction = manager
				.beginTransaction();
		transaction.remove(
				manager.getFragments().get(manager.getFragments().size() - 1))
				.commit();
		try {
			pagerAdapter.notifyDataSetChanged();
		} catch (Exception ignored) {
		}
		invalidateOptionsMenu();
	}

	@Override
	public void onBackPressed() {
		if (currentMode != MODE_NONE) {
			currentMode = MODE_NONE;
			getGalleryFragment().cancelMode();
			invalidateOptionsMenu();
			return;
		}
		if (pager != null && pager.isLocked()) {
			return;
		}
		super.onBackPressed();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == DevicesFragmentMic.REQUEST_DISCOVERABLE) {
			pagerAdapter.getItem(0).onActivityResult(requestCode, resultCode,
					data);
			return;
		} else if (requestCode == OtherSettingsFragment.REQUEST_CHANGE_LOCALE
				&& resultCode == RESULT_OK) {
			if (data == null)
				return;
			final String newLang = data.getStringExtra(C.DATA);
			if (newLang == null || newLang.isEmpty())
				return;
			Util.changeLanguage(data.getStringExtra(C.DATA), getBaseContext()
					.getResources());
			onConfigurationChanged(getResources().getConfiguration());
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		L.e("destroy activity");
		Util.disableWifiDirect();
		Session.current().getManager()
				.cancelConnect(Session.current().getChannel(), null);
		try {
			if (Session.current().getBluetoothSocket() != null)
				Session.current().getBluetoothSocket().close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		unregisterReceiver(actionsReceiver);
		stopService(new Intent(getApplicationContext(), CameraService.class));
	}

	private final BroadcastReceiver actionsReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (C.ACTION_START_RECORDING.equals(action) && !PrefUtil.isMic()) {
				if (CameraActivity.isRunning())
					return;
				final Intent i = new Intent(getApplicationContext(),
						CameraActivity.class);
				i.setAction(action);
				i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(i);
			} else if (Intent.ACTION_LOCALE_CHANGED.equals(action)) {
				L.e("broadcast locale changed");
			}
		}
	};

	private GalleryFragment getGalleryFragment() {
		for (final Fragment fragment : getSupportFragmentManager()
				.getFragments()) {
			if (fragment instanceof GalleryFragment) {
				return (GalleryFragment) fragment;
			}
		}
		return (GalleryFragment) getSupportFragmentManager().getFragments()
				.get(2);
	}

	@Override
	protected void onPause() {
		super.onPause();
		Counter.sharedInstance().onPauseActivity(this);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		L.e("config changed");
		if (!PrefUtil.isMic()) {
			getGalleryFragment().refresh();
		}
		PrefUtil.saveOrientation(newConfig.orientation);
		super.onConfigurationChanged(newConfig);
	}
}
