package com.nevilon.bigplanet;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Proxy;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.nevilon.bigplanet.core.BigPlanetApp;
import com.nevilon.bigplanet.core.MarkerManager;
import com.nevilon.bigplanet.core.PhysicMap;
import com.nevilon.bigplanet.core.Place;
import com.nevilon.bigplanet.core.Preferences;
import com.nevilon.bigplanet.core.RawTile;
import com.nevilon.bigplanet.core.SHA1Hash;
import com.nevilon.bigplanet.core.db.DAO;
import com.nevilon.bigplanet.core.db.GeoBookmark;
import com.nevilon.bigplanet.core.geoutils.GeoUtils;
import com.nevilon.bigplanet.core.loader.TileLoader;
import com.nevilon.bigplanet.core.providers.MapStrategyFactory;
import com.nevilon.bigplanet.core.storage.LocalStorageWrapper;
import com.nevilon.bigplanet.core.storage.SQLLocalStorage;
import com.nevilon.bigplanet.core.tools.savemap.MapSaverUI;
import com.nevilon.bigplanet.core.ui.AddBookmarkDialog;
import com.nevilon.bigplanet.core.ui.MapControl;
import com.nevilon.bigplanet.core.ui.OnDialogClickListener;
import com.nevilon.bigplanet.core.ui.OnMapLongClickListener;
import com.nevilon.bigplanet.core.ui.SmoothZoomEngine;

public class BigPlanet extends Activity {

	//public static final int GO_TO_LOCATION = 20;

	private static final String BOOKMARK_DATA = "bookmark";

	//private static int MY_LOCATION_ZOOM = 1;

	private static int SEARCH_ZOOM = 2;

	private Toast textMessage;

	public static String identifier = null;
	
	/*
	 * Графический движок, реализующий карту
	 */
	private MapControl mapControl;

	private MarkerManager mm;

	private static LocationManager locationManager;
	
	private Location currentLocation;

	private boolean inHome = false;

	public static boolean isFollowMode = true; // default value is auto follow
	
	public static boolean isMapInCenter = false;
	
	private static String strSQLiteName = "";
	
	private static String locationType = "";
	
	private boolean SDCARD_AVAILABLE = true;
	
	private MyIntentReceiver intentReceiver;
	
	private MyUpdateScreenIntentReceiver updateScreenIntentReceiver;
	
	public static RelativeLayout mAutoFollowRelativeLayout;
	
	private static ImageView scaleImageView;
	
	private Point myGPSOffset;
	
	public static float density;
	
	/**
	 * Конструктор
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		boolean hasSD = false;
		// проверка на доступность sd
		String status = Environment.getExternalStorageState();
		if (!status.equals(Environment.MEDIA_MOUNTED)) {
			SDCARD_AVAILABLE = false;

			new AlertDialog.Builder(this)

			.setMessage(R.string.sdcard_unavailable)
					.setCancelable(false).setNeutralButton(R.string.OK_LABEL,
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface arg0,
										int arg1) {
									finish();
								}

							}).show();
		} else {
			
			hasSD = true;
			intentReceiver = new MyIntentReceiver();
			IntentFilter intentFilter = new IntentFilter("com.nevilon.bigplanet.INTENTS.GOTO");
			registerReceiver(intentReceiver, intentFilter);

			updateScreenIntentReceiver = new MyUpdateScreenIntentReceiver();
			registerReceiver(updateScreenIntentReceiver, new IntentFilter("com.nevilon.bigplanet.INTENTS.UpdateScreen"));

			locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			
			SmoothZoomEngine.stop = false;
			TileLoader.stop = false;
			mAutoFollowRelativeLayout = getAutoFollowRelativeLayout();
			mAutoFollowRelativeLayout.setVisibility(View.INVISIBLE);
			
			File mapsDBFolder = new File(SQLLocalStorage.DATA_PATH);
			if (!mapsDBFolder.exists())
				mapsDBFolder.mkdirs();
			mapsDBFolder = null;
			
			String proxyHost = Proxy.getDefaultHost();
			int proxyPort = Proxy.getDefaultPort();
			if (proxyHost != null && proxyPort != -1) { 
				System.setProperty("http.proxyHost", proxyHost);
				System.setProperty("http.proxyPort", Integer.toString(proxyPort));
				Log.i("Proxy", proxyHost+":"+proxyPort);
			}
			initializeMap();
			/* Create a ImageView with a auto-follow icon. */
			mapControl.addView(mAutoFollowRelativeLayout); // We can just run it once.
			/* Create a ImageView with a scale image. */
			scaleImageView = new ImageView(this);
			scaleImageView.setImageResource(R.drawable.scale1);
			mapControl.addView(scaleImageView);
			
			if (!verify(identifier)) {
				showTrialDialog(R.string.this_is_demo_title, R.string.this_is_demo_message);
			}
			setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
		}
		
		if (hasSD) {
			myGPSOffset = Preferences.getGPSOffset();
			strSQLiteName = Preferences.getSQLiteName();
			setActivityTitle(BigPlanet.this);
		}
	}
	
	public static void disabledAutoFollow(Context context) {
		if (isFollowMode) {
			Toast.makeText(context, R.string.auto_follow_disabled, Toast.LENGTH_SHORT).show();
			mAutoFollowRelativeLayout.setVisibility(View.VISIBLE);
			finishGPSLocationListener(false);
		}
		strSQLiteName = Preferences.getSQLiteName();
		setActivityTitle((Activity) context);
	}
	
	public void enabledAutoFollow(Context context) {
		if (!isFollowMode) {
			Toast.makeText(context, R.string.auto_follow_enabled, Toast.LENGTH_SHORT).show();
			mAutoFollowRelativeLayout.setVisibility(View.INVISIBLE);
			startGPSLocationListener();
		}
		strSQLiteName = Preferences.getSQLiteName();
		setActivityTitle((Activity) context);
	}
	
	private RelativeLayout getAutoFollowRelativeLayout() {
		final RelativeLayout relativeLayout = new RelativeLayout(this);
		
		/* Create a ImageView with a auto-follow icon. */
		final ImageView ivAutoFollow = new ImageView(this);
		ivAutoFollow.setImageResource(R.drawable.autofollow);
		
		ivAutoFollow.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				enabledAutoFollow(BigPlanet.this);
			}
        });
		
		/* Create RelativeLayoutParams, that position in in the bottom right corner. */
		final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		relativeLayout.addView(ivAutoFollow, params);
		
		return relativeLayout;
	}

	private void initializeMap() {
		// создание карты
		mm = new MarkerManager(getResources());
		RawTile savedTile = Preferences.getTile();
		//savedTile.s = 0;
		configMapControl(savedTile);
		// использовать ли сеть
		boolean useNet = Preferences.getUseNet();
		mapControl.getPhysicalMap().getTileResolver().setUseNet(useNet);
		// источник карты
		int mapSourceId = Preferences.getSourceId();
		mapControl.getPhysicalMap().getTileResolver().setMapSource(mapSourceId);
		mapControl.getPhysicalMap().getDefaultTile().s = mapSourceId;
		// величина отступа
		Point globalOffset = Preferences.getOffset();
		//globalOffset.x = 0;
		//globalOffset.y = -32;
		System.out.println("offset " + globalOffset + " " + savedTile);
		mapControl.getPhysicalMap().setGlobalOffset(globalOffset);
		mapControl.getPhysicalMap().reloadTiles();
	}

	public class MyIntentReceiver extends BroadcastReceiver {

		/**
		 * 
		 * @see adroid.content.BroadcastReceiver#onReceive(android.content.Context,
		 *      android.content.Intent)
		 */

		@Override
		public void onReceive(Context context, Intent intent) {
			isFollowMode = true;
			disabledAutoFollow(BigPlanet.this);
			int z = SEARCH_ZOOM;
			Place place = (Place) intent.getSerializableExtra("place");
			mm.clearMarkerManager();
			mm.addMarker(place, z, false, MarkerManager.SEARCH_MARKER);
			com.nevilon.bigplanet.core.geoutils.Point p = GeoUtils.toTileXY(
					place.getLat(), place.getLon(), z);
			com.nevilon.bigplanet.core.geoutils.Point off = GeoUtils
					.getPixelOffsetInTile(place.getLat(), place.getLon(), z);
			mapControl.goTo((int) p.x, (int) p.y, z, (int) off.x, (int) off.y);
		}

	}
	
	public class MyUpdateScreenIntentReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i("BP", "onReceive");
			// see MapControl zoomPanel.setOnZoomOutClickListener
			if (intent.getBooleanExtra(MapControl.FIX_ZOOM, false)) {
				Log.i("BP", "%% "+MapControl.FIX_ZOOM);
				configMapControl(mapControl.getPhysicalMap().getDefaultTile());
				onResume();
			}
			// center map
			if (isFollowMode && !isMapInCenter) {
				if (currentLocation != null) {
					Log.i("BP", "%% goToMyLocation");
					Log.i("BP", "%% "+currentLocation.getProvider()+" "
							+currentLocation.getLongitude()+", "+currentLocation.getLatitude());
					isMapInCenter = true;
					goToMyLocation(currentLocation, PhysicMap.getZoomLevel());
				}
			}
			// refresh the activity title
			if (strSQLiteName == null) {
				strSQLiteName = Preferences.getSQLiteName();
			}
			setActivityTitle(BigPlanet.this);
		}
	}

	@Override
	public boolean onSearchRequested() {
		startSearch("", false, null, false);
		return true;
	}

	/**
	 * Обрабатывает поворот телефона
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		System.gc();
		configMapControl(mapControl.getPhysicalMap().getDefaultTile());
		if (isFollowMode && currentLocation != null)
			goToMyLocation(currentLocation, PhysicMap.getZoomLevel());
		else
			mapControl.updateScreen();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (SDCARD_AVAILABLE) {
			if (isFollowMode) {
				isFollowMode = false;
				new Handler().postDelayed(new Runnable(){
					@Override
					public void run() {
						followMyLocation();
					}
				}, 100);
			}
		}
	}

	/**
	 * Запоминает текущий тайл и отступ при выгрузке приложения
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		finishGPSLocationListener(true); // release the GPS resources
		SmoothZoomEngine.sze = null; // release the variable
		SmoothZoomEngine.stop = true; // stop the thread
		TileLoader.stop = true; // stop the thread
		isFollowMode = true; // enable auto follow when executing next time
		if(intentReceiver!=null){
			unregisterReceiver(intentReceiver);
			Preferences.putTile(mapControl.getPhysicalMap().getDefaultTile());
			Preferences.putOffset(mapControl.getPhysicalMap().getGlobalOffset());
		}
		if(updateScreenIntentReceiver!=null){
			unregisterReceiver(updateScreenIntentReceiver);
		}
		if (textMessage != null) {
			textMessage.cancel();
		}
		System.gc();
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (resultCode) {
		case RESULT_OK:
			if (requestCode == 0) {
				isFollowMode = true;
				disabledAutoFollow(BigPlanet.this);
				GeoBookmark bookmark = (GeoBookmark) data.getSerializableExtra(BOOKMARK_DATA);
				mapControl.getPhysicalMap().setDefTile(bookmark.getTile());
	
				Point offset = new Point();
				offset.set(bookmark.getOffsetX(), bookmark.getOffsetY());
				Preferences.putSourceId(bookmark.getTile().s);
				mapControl.getPhysicalMap().setGlobalOffset(offset);
				mapControl.getPhysicalMap().reloadTiles();
				mapControl.setMapSource(bookmark.getTile().s);
			}
		default:
			break;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent ev) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			/**
			 * если текущий режим SELECT_MODE - изменить на ZOOM_MODE если
			 * текущий режим ZOOM_MODE - делегировать обработку
			 */
			if (mapControl.getMapMode() == MapControl.SELECT_MODE) {
				mapControl.setMapMode(MapControl.ZOOM_MODE);
				return true;
			}
		default:
			return super.onKeyDown(keyCode, ev);
		}
	}

	/**
	 * Создает элементы меню
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(1, 11, 0, R.string.SEARCH_MENU).setIcon(R.drawable.search);

		SubMenu sub = menu.addSubMenu(0, 6, 0, R.string.BOOKMARKS_MENU).setIcon(R.drawable.bookmark);
		sub.add(2, 21, 0, R.string.BOOKMARK_ADD_MENU);
		sub.add(2, 22, 1, R.string.BOOKMARKS_VIEW_MENU);

		menu.add(3, 33, 0, R.string.MY_LOCATION_MENU).setIcon(R.drawable.globe_download);

		// add More menu
		sub = menu.addSubMenu(0, 1, 0, R.string.TOOLS_MENU).setIcon(R.drawable.tools);
		sub.add(4, 41, 0, R.string.NETWORK_MODE_MENU);
		sub.add(4, 42, 1, R.string.CACHE_MAP_MENU);
		sub.add(4, 43, 2, R.string.MAP_SOURCE_MENU);
		sub.add(4, 44, 3, R.string.SQLiteDB_MENU);
		sub.add(4, 45, 4, R.string.GPS_OFFSET_MENU);
		sub.add(4, 49, 10, R.string.ABOUT_MENU);

		return true;
	}

	/**
	 * Устанавливает статус(активен/неактивен) пунктов меню
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean useNet = Preferences.getUseNet();
		menu.findItem(42).setEnabled(useNet);
		return true;
	}

	/**
	 * Устанавливает размеры карты и др. свойства
	 */
	private void configMapControl(RawTile tile) {
		WindowManager wm = this.getWindowManager();
		Display display = wm.getDefaultDisplay();
		int width = display.getWidth();
		int height = display.getHeight();
		height = height - 50; // minus the space of the status bar
		
		DisplayMetrics dm = new DisplayMetrics();
		wm.getDefaultDisplay().getMetrics(dm);
		density = dm.density;
		
		if (mapControl == null) {
			identifier = getString(R.string.ABOUT_URL);
			mapControl = new MapControl(this, width, height, tile, mm);
			mapControl.setOnMapLongClickListener(new OnMapLongClickListener() {

				@Override
				public void onMapLongClick(int x, int y) {
					hideMessage();
					final GeoBookmark newGeoBookmark = new GeoBookmark();
					newGeoBookmark.setOffsetX(mapControl.getPhysicalMap()
							.getGlobalOffset().x);
					newGeoBookmark.setOffsetY(mapControl.getPhysicalMap()
							.getGlobalOffset().y);

					newGeoBookmark.setTile(mapControl.getPhysicalMap()
							.getDefaultTile());
					newGeoBookmark.getTile().s = mapControl.getPhysicalMap()
							.getTileResolver().getMapSourceId();

					AddBookmarkDialog.show(BigPlanet.this, newGeoBookmark,
							new OnDialogClickListener() {

								@Override
								public void onCancelClick() {

								}

								@Override
								public void onOkClick(Object obj) {
									GeoBookmark geoBookmark = (GeoBookmark) obj;
									DAO d = new DAO(BigPlanet.this);
									d.saveGeoBookmark(geoBookmark);
									mapControl.setMapMode(MapControl.ZOOM_MODE);

								}

							});
				}

			});
		} else {
			mapControl.setSize(width, height);
		}
		mapControl.updateZoomControls();
		setContentView(mapControl, new ViewGroup.LayoutParams(width, height));
	}

	/**
	 * Создает радиокнопку с заданными параметрами
	 * 
	 * @param label
	 * @param id
	 * @return
	 */
	private RadioButton buildRadioButton(String label, int id) {
		RadioButton btn = new RadioButton(this);
		btn.setText(label);
		btn.setId(id);
		return btn;
	}

	/**
	 * Обрабатывает нажатие на меню
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		hideMessage();
		switch (item.getItemId()) {
		case 11:
			showSearch();
			break;
		case 21:
			switchToBookmarkMode();
			break;
		case 22:
			showAllGeoBookmarks();
			break;
		case 33:
			followMyLocation();
			break;
		case 41:
			selectNetworkMode();
			break;
		case 42:
			if (BigPlanetApp.isDemo) {
				if (PhysicMap.getZoomLevel() <= 6) {
					showTrialDialog(R.string.try_demo_title, R.string.try_demo_message);
				} else {
					showMapSaver();
				}
			} else {
				showMapSaver();
			}
			break;
		case 43:
			selectMapSource();
			break;
		case 44:
			if (BigPlanetApp.isDemo) {
				showTrialDialog(R.string.try_demo_title, R.string.try_demo_message);
			} else {
				selectSQLiteDBFile();
			}
			break;
		case 45:
			selectGPSOffset();
			break;
		case 49:
			showAbout();
			break;
		}
		return false;

	}

	private void showTrialDialog(int title, int message) {
		final Dialog paramsDialog = new Dialog(this);

		final View v = View.inflate(this, R.layout.demodialog, null);

		final TextView messageValue = (TextView) v.findViewById(R.id.message);
		messageValue.setText(message);
		final Button okBtn = (Button) v.findViewById(R.id.okBtn);
		okBtn.setEnabled(false);
		okBtn.setClickable(false);
		okBtn.setOnClickListener(new OnClickListener() {

			public void onClick(View arg0) {
				paramsDialog.dismiss();
			}

		});
		paramsDialog.setTitle(title);
		paramsDialog.setCanceledOnTouchOutside(false);
		paramsDialog.setCancelable(false);
		paramsDialog.setContentView(v);

		paramsDialog.show();

		final Handler handler = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				int okValue = (Integer) msg.what;

				if (okValue == 0) {
					okBtn.setText(R.string.OK_LABEL);
					okBtn.setEnabled(true);
				} else {
					okBtn.setText(String.valueOf(okValue));
				}

			}
		};

		new Thread() {

			int count = 5;

			boolean exec = true;

			@Override
			public void run() {
				while (exec) {
					try {
						Thread.sleep(1000);
						count--;
						if (count == 0) {
							exec = false;
						}
						Message message = handler.obtainMessage(count);
						handler.sendMessage(message);

					} catch (InterruptedException e) {
						e.printStackTrace();
					}

				}
			}

		}.start();
	}

	public static boolean verify() {
		return verify(identifier);
	}
	
	private static boolean verify(String key) {
		boolean result = false;
		try {
			if (!SHA1Hash.encode(key).equalsIgnoreCase("671b82291403cf7bc530b40bb302dd08fb4a3ce0")) {
				BigPlanetApp.isDemo = true;
				result = true;
			}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return !result;
	}

	/* Begin of the GPS LocationListener code */
	
//	Handler mHandler;
//	
//	class StartGPSLocationThread implements Runnable {
//		public void run() {
//			if (mHandler != null) {
//				mHandler.getLooper().quit();
//			}
//			Looper.prepare();
//			mHandler = new Handler();
//			startGPSLocationListener();
//			Looper.loop();
//		}
//	};

	private void startGPSLocationListener() {
		isFollowMode = true;
		inHome = false;
		
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setAltitudeRequired(false);
		criteria.setSpeedRequired(false);
		criteria.setBearingRequired(false);
		criteria.setCostAllowed(true);
		criteria.setPowerRequirement(Criteria.POWER_LOW);
		String provider = locationManager.getBestProvider(criteria, true); //gps
		if (provider != null) {
			locationType = provider;
			if (!inHome) {
				Location location = locationManager.getLastKnownLocation(provider);
				if (location != null) {
					currentLocation = location;
					goToMyLocation(currentLocation, PhysicMap.getZoomLevel());
				}
				inHome = true;
			}
		} else { // gps and network are both disabled
			Toast.makeText(this, R.string.msg_unable_to_get_current_location, 3000).show();
		}

		/* GPS_PROVIDER */
		if (gpsLocationListener == null) {
			gpsLocationListener = new MyLocationListener("gps");
		}
		String gpsProvider = LocationManager.GPS_PROVIDER; // gps
		locationManager.requestLocationUpdates(gpsProvider, minTime, minDistance, gpsLocationListener);

		/* NETWORK_PROVIDER */
		if (networkLocationListener == null) {
			networkLocationListener = new MyLocationListener("network");
		}
		String networkProvider = LocationManager.NETWORK_PROVIDER; // network
		locationManager.requestLocationUpdates(networkProvider, minTime, minDistance, networkLocationListener);
	}
	
	public static void finishGPSLocationListener(boolean isSetNull) {
		isFollowMode = false;
		locationType = "";
		if (locationManager != null) {
			locationManager.removeUpdates(networkLocationListener);
			locationManager.removeUpdates(gpsLocationListener);
			if (isSetNull) {
				networkLocationListener = null;
				gpsLocationListener = null;
			}
		}
//		if (mHandler != null) {
//			mHandler.getLooper().quit();
//		}
	}
	
	private static LocationListener gpsLocationListener;
	private static LocationListener networkLocationListener;
	private final long minTime = 100; // ms
	private final float minDistance = 5; // m
	
	class MyLocationListener implements LocationListener {
		String myLocationType;
		
		MyLocationListener(String locationType) {
			this.myLocationType = locationType;
		}
		
		public void onLocationChanged(Location location) {
			String longitude = String.valueOf(location.getLongitude());
			String latitude = String.valueOf(location.getLatitude());
			Log.i("Location", location.getProvider()+" onLocationChanged(): longitude="+longitude+", latitude="+latitude);
			goToMyLocation(location, PhysicMap.getZoomLevel());
			currentLocation = location;
			inHome = true;
			BigPlanet.locationType = location.getProvider();
			setActivityTitle(BigPlanet.this);
			// gpsLocationListener has higher priority than networkLocationListener
			if (myLocationType.equals("gps")) {
				locationManager.removeUpdates(networkLocationListener);
			}
		}
		
		public void onProviderDisabled(String provider) {
			Log.i("Location", provider + " is disabled.");
			if (myLocationType.equals("gps")) {
				locationManager.requestLocationUpdates(provider, minTime, minDistance, networkLocationListener);
			}
		}
		
		public void onProviderEnabled(String provider) {
			Log.i("Location", provider + " is enabled.");
			if (myLocationType.equals("gps")) {
				locationManager.requestLocationUpdates(provider, minTime, minDistance, gpsLocationListener);
			} else {
				locationManager.requestLocationUpdates(provider, minTime, minDistance, networkLocationListener);
			}
		}
		
		public void onStatusChanged(String provider, int status, Bundle extras) {
			if (status == 0) {
				Log.i("Location", provider + " is OUT OF SERVICE");
			} else if (status == 1) {
				Log.i("Location", provider + " is TEMPORARILY UNAVAILABLE");
			} else {
				Log.i("Location", provider + " is AVAILABLE");
			}
		}
	}
	
	private void followMyLocation() {
		if (!isFollowMode) { // isFollowMode = false;
			//new Thread(new StartGPSLocationThread(), "GPSLocationListener").start();
			//startGPSLocationListener(); // isFollowMode = true;
			enabledAutoFollow(this);
		} else { // isFollowMode = true
			//finishGPSLocationListener(false); // isFollowMode = false;
			disabledAutoFollow(this);
		}
	}

	private void goToMyLocation(Location location, int zoom) {
		double lat = location.getLatitude() + myGPSOffset.y*Math.pow(10, -5);
		double lon = location.getLongitude() + myGPSOffset.x*Math.pow(10, -5);
		com.nevilon.bigplanet.core.geoutils.Point p = GeoUtils.toTileXY(lat, lon, zoom);
		com.nevilon.bigplanet.core.geoutils.Point off = GeoUtils.getPixelOffsetInTile(lat, lon, zoom);
		mapControl.goTo((int) p.x, (int) p.y, zoom, (int) off.x, (int) off.y);

		Place place = new Place();
		place.setLat(lat);
		place.setLon(lon);
		mm.addMarker(place, zoom, true, MarkerManager.MY_LOCATION_MARKER);
	}
		
	/* End of the GPS LocationListener code */

	private void showSearch() {
		onSearchRequested();
	}
	
	private void showAbout() {
		String about = getString(R.string.ABOUT_MESSAGE).replace("{url}", getString(R.string.ABOUT_URL));
		TextView tv = new TextView(this);
		tv.setLinksClickable(true);
		tv.setAutoLinkMask(Linkify.EMAIL_ADDRESSES);
		tv.setAutoLinkMask(Linkify.WEB_URLS);
		tv.setGravity(Gravity.CENTER);
		tv.setText(about);
		tv.setTextSize(12f);
		PackageInfo info;
		String versionName = "";
		String packageNum = "";
		try {
			String PACKAGE_NAME = BigPlanet.class.getPackage().getName();
			info = getPackageManager().getPackageInfo(PACKAGE_NAME, 0);
			versionName = info.versionName;
			packageNum = SHA1Hash.encode(info.packageName);
			packageNum = packageNum.substring(packageNum.length()-4);
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
	    } catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		try {
			if (Integer.parseInt(packageNum)==8435) {
				new AlertDialog.Builder(this).setTitle(getString(R.string.ABOUT_TITLE)+" "+versionName)
				.setView(tv).setIcon(R.drawable.globe).setPositiveButton(
						R.string.OK_LABEL,
						new DialogInterface.OnClickListener() {
	
							public void onClick(DialogInterface dialog,
									int whichButton) {
	
							}
	
						}).show();
			}
		} catch (NumberFormatException e) {
		}
	}

	private void showAllGeoBookmarks() {
		Intent intent = new Intent();
		intent.setClass(this, AllGeoBookmarks.class);
		startActivityForResult(intent, 0);
	}

	private void switchToBookmarkMode() {
		if (mapControl.getMapMode() != MapControl.SELECT_MODE) {
			mapControl.setMapMode(MapControl.SELECT_MODE);
			showMessage();
		}
	}

	private void showMessage() {
		textMessage = Toast.makeText(this, R.string.SELECT_OBJECT_MESSAGE,
				Toast.LENGTH_LONG);
		textMessage.show();
	}

	private void hideMessage() {
		if (textMessage != null) {
			textMessage.cancel();
		}
	}

	/**
	 * Отображает диалоги для кеширования карты в заданном радиусе
	 */
	private void showMapSaver() {
		MapSaverUI mapSaverUI = new MapSaverUI(this, 
				PhysicMap.getZoomLevel(), 
				mapControl.getPhysicalMap().getAbsoluteCenter(), 
				mapControl.getPhysicalMap().getTileResolver().getMapSourceId());
		mapSaverUI.show();
	}

	/**
	 * Создает диалог для выбора режима работы(оффлайн, онлайн)
	 */
	private void selectNetworkMode() {
		final Dialog networkModeDialog;
		networkModeDialog = new Dialog(this);
		networkModeDialog.setCanceledOnTouchOutside(true);
		networkModeDialog.setCancelable(true);
		networkModeDialog.setTitle(R.string.SELECT_NETWORK_MODE_LABEL);

		final LinearLayout mainPanel = new LinearLayout(this);
		mainPanel.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));
		mainPanel.setOrientation(LinearLayout.VERTICAL);

		RadioGroup modesRadioGroup = new RadioGroup(this);

		LinearLayout.LayoutParams layoutParams = new RadioGroup.LayoutParams(
				RadioGroup.LayoutParams.WRAP_CONTENT,
				RadioGroup.LayoutParams.WRAP_CONTENT);

		modesRadioGroup.addView(buildRadioButton(getResources().getString(
				(R.string.OFFLINE_MODE_LABEL)), 0), 0, layoutParams);

		modesRadioGroup.addView(buildRadioButton(getResources().getString(
				R.string.ONLINE_MODE_LABEL), 1), 0, layoutParams);

		boolean useNet = Preferences.getUseNet();
		int checked = 0;
		if (useNet) {
			checked = 1;
		}
		modesRadioGroup.check(checked);

		modesRadioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					public void onCheckedChanged(RadioGroup group, int checkedId) {
						boolean useNet = checkedId == 1;
						mapControl.getPhysicalMap().getTileResolver()
								.setUseNet(useNet);
						Preferences.putUseNet(useNet);
						networkModeDialog.dismiss();
					}
				});

		mainPanel.addView(modesRadioGroup);
		networkModeDialog.setContentView(mainPanel);
		networkModeDialog.show();

	}

	/**
	 * Создает диалог для выбора источника карт
	 */
	private void selectMapSource() {
		final Dialog mapSourceDialog;
		mapSourceDialog = new Dialog(this);
		mapSourceDialog.setCanceledOnTouchOutside(true);
		mapSourceDialog.setCancelable(true);
		mapSourceDialog.setTitle(R.string.SELECT_MAP_SOURCE_TITLE);

		ScrollView scrollPanel = new ScrollView(this);
		scrollPanel.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));

		final LinearLayout mainPanel = new LinearLayout(this);
		mainPanel.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));
		mainPanel.setOrientation(LinearLayout.VERTICAL);

		RadioGroup sourcesRadioGroup = new RadioGroup(this);

		LinearLayout.LayoutParams layoutParams = new RadioGroup.LayoutParams(
				RadioGroup.LayoutParams.WRAP_CONTENT,
				RadioGroup.LayoutParams.WRAP_CONTENT);

		for (Integer id : MapStrategyFactory.strategies.keySet()) {
			sourcesRadioGroup.addView(
					buildRadioButton(MapStrategyFactory.strategies.get(id)
							.getDescription(), id), 0, layoutParams);
		}

		sourcesRadioGroup.check(Preferences.getSourceId());

		sourcesRadioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					public void onCheckedChanged(RadioGroup group, int checkedId) {
						Preferences.putSourceId(checkedId);
						mapControl.setMapSource(checkedId);
						mapSourceDialog.dismiss();
					}
				});

		mainPanel.addView(sourcesRadioGroup);
		scrollPanel.addView(mainPanel);
		mapSourceDialog.setContentView(scrollPanel);
		mapSourceDialog.show();
	}
	
	private void selectSQLiteDBFile() {
		final Dialog mSQLiteDBFileDialog;
		mSQLiteDBFileDialog = new Dialog(this);
		mSQLiteDBFileDialog.setCanceledOnTouchOutside(true);
		mSQLiteDBFileDialog.setCancelable(true);
		mSQLiteDBFileDialog.setTitle(R.string.SELECT_SQLite_DATABASE);

		ScrollView scrollPanel = new ScrollView(this);
		scrollPanel.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));

		final LinearLayout mainPanel = new LinearLayout(this);
		mainPanel.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));
		mainPanel.setOrientation(LinearLayout.VERTICAL);
		
		TextView mTextView = new TextView(this);
		String folderMessage = getString(R.string.SELECT_SQLite_Folder).replace("%s", SQLLocalStorage.DATA_PATH);
		mTextView.setText("  "+folderMessage+"\n");
		mainPanel.addView(mTextView);

		RadioGroup sqliteRadioGroup = new RadioGroup(this);

		LinearLayout.LayoutParams layoutParams = new RadioGroup.LayoutParams(
				RadioGroup.LayoutParams.WRAP_CONTENT,
				RadioGroup.LayoutParams.WRAP_CONTENT);
		
		final Map<Integer, String> sqliteMaps = new HashMap<Integer, String>();
		int mapsIndex = 1;
		// SQLLocalStorage.DATA_PATH = "/sdcard/RMaps/maps/";
		File mapsDBFolder = new File(SQLLocalStorage.DATA_PATH);
		if (mapsDBFolder.exists() && mapsDBFolder.isDirectory()) {
			String[] files = mapsDBFolder.list();
//			for (int i = 0; i < files.length; i++) {
//				Log.i("sqlitedb", files[i]);
//			}
			java.util.Arrays.sort(files);
			String[] filesSorted = new String[files.length];
			for (int i = 0; i < files.length; i++) {
				filesSorted[files.length-1-i] = files[i];
			}
			for (int i = 0; i < filesSorted.length; i++) {
				File sqliteFile = new File(mapsDBFolder, filesSorted[i]);
				if (sqliteFile.isFile()) {
					String strSQLiteName = sqliteFile.getName();
					if (strSQLiteName.endsWith(".sqlitedb")) {
						sqliteMaps.put(mapsIndex, strSQLiteName);
						mapsIndex++;
					} else {
						// skip...
					}
				}
			}
		}
		if (sqliteMaps.isEmpty()) {
			sqliteMaps.put(0, SQLLocalStorage.DATA_FILE);
			if (!mapsDBFolder.exists())
				mapsDBFolder.mkdirs();
		}
		mapsDBFolder = null;
		for (Integer key : sqliteMaps.keySet()) {
			String strSQLiteName = sqliteMaps.get(key);
			sqliteRadioGroup.addView(
					buildRadioButton(strSQLiteName, key), 0, layoutParams);
			if (strSQLiteName.equalsIgnoreCase(Preferences.getSQLiteName())) {
				sqliteRadioGroup.check(key);
			}
		}

		sqliteRadioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					public void onCheckedChanged(RadioGroup group, int checkedId) {
						strSQLiteName = sqliteMaps.get(checkedId);
						Preferences.putSQLiteName(strSQLiteName);
						// switch SQLite DB
						LocalStorageWrapper.switchLocalStorage();
						// update screen
						initializeMap();
						isFollowMode = false;
						enabledAutoFollow(BigPlanet.this);
						mSQLiteDBFileDialog.dismiss();
					}
				});

		mainPanel.addView(sqliteRadioGroup);
		scrollPanel.addView(mainPanel);
		mSQLiteDBFileDialog.setContentView(scrollPanel);
		mSQLiteDBFileDialog.show();
	}
	
	private void selectGPSOffset() {
		final Dialog mGPSOffsetDialog;
		mGPSOffsetDialog = new Dialog(this);
		mGPSOffsetDialog.setCancelable(true);
		mGPSOffsetDialog.setTitle(R.string.GPS_OFFSET_MENU);
		
		final LinearLayout mainPanel = new LinearLayout(this);
		mainPanel.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));
		mainPanel.setOrientation(LinearLayout.VERTICAL);
		
		LayoutParams params = new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.WRAP_CONTENT);

		final TextView mTextView = new TextView(this);
		mTextView.setPadding(5, 0, 5, 5);
		mTextView.setText(R.string.OFFSET_MESSAGE);
		mainPanel.addView(mTextView);
		
		String msg;
		
		final TextView mTextViewX = new TextView(this);
		mTextViewX.setPadding(5, 5, 5, 0);
		msg = getString(R.string.OFFSET_X).replace("%s", ""+toGPSOffset(0));
		mTextViewX.setText(msg);
		mainPanel.addView(mTextViewX);
		final SeekBar seekBarX = new SeekBar(this);
		seekBarX.setLayoutParams(params);
		seekBarX.setPadding(5, 0, 5, 0);
		seekBarX.setMax(400);
		seekBarX.setKeyProgressIncrement(1);
		seekBarX.setProgress((int) (myGPSOffset.x/10 + 200));
		seekBarX.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				myGPSOffset.x = (int) ((progress-200) * 10);
				String msg = getString(R.string.OFFSET_X).replace("%s", ""+toGPSOffset(0));
				mTextViewX.setText(msg);
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});
		mainPanel.addView(seekBarX);
		
		final TextView mTextViewY = new TextView(this);
		mTextViewY.setPadding(5, 5, 5, 0);
		msg = getString(R.string.OFFSET_Y).replace("%s", ""+toGPSOffset(1));
		mTextViewY.setText(msg);
		mainPanel.addView(mTextViewY);
		final SeekBar seekBarY = new SeekBar(this);
		seekBarY.setLayoutParams(params);
		seekBarY.setPadding(5, 0, 5, 0);
		seekBarY.setMax(400);
		seekBarY.setKeyProgressIncrement(1);
		seekBarY.setProgress((int) (myGPSOffset.y/10 + 200));
		seekBarY.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				myGPSOffset.y = (int) ((progress-200) * 10);
				String msg = getString(R.string.OFFSET_Y).replace("%s", ""+toGPSOffset(1));
				mTextViewY.setText(msg);
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});
		mainPanel.addView(seekBarY);
		
		final Button saveButton = new Button(this);
		saveButton.setText(R.string.SAVE_BUTTON);
		saveButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Point savedGPSOffset = new Point(
						(int) (myGPSOffset.x),
						(int) (myGPSOffset.y));
				Preferences.putGPSOffset(savedGPSOffset);
				goToMyLocation(currentLocation, PhysicMap.getZoomLevel());
				mGPSOffsetDialog.dismiss();
			}
		});
		mainPanel.addView(saveButton);
		
		mGPSOffsetDialog.setContentView(mainPanel);
		mGPSOffsetDialog.show();
	}
	
	private float toGPSOffset(int type) {
		float distance;
		if (type == 0) { // lon
			Location EndLocation = getLocation(myGPSOffset.x * Math.pow(10, -5), 0);
			distance = currentLocation.distanceTo(EndLocation);
			if (myGPSOffset.x < 0)
				distance = -distance;
		} else { // lat
			Location EndLocation = getLocation(0, myGPSOffset.y * Math.pow(10, -5));
			distance = currentLocation.distanceTo(EndLocation);
			if (myGPSOffset.y < 0)
				distance = -distance;
		}
		return distance;
	}
	
	private Location getLocation(double latOffset, double lonOffset) {
		double startLatitude = currentLocation.getLatitude();
		double startLongitude = currentLocation.getLongitude();
		Location endLocation = new Location("");
		endLocation.setLatitude(startLatitude + latOffset);
		endLocation.setLongitude(startLongitude + lonOffset);
		return endLocation;
	}
	
	public static void setActivityTitle(Activity activity) {
		// remove ".sqlitedb"
		String mSQLiteName = strSQLiteName.substring(0, strSQLiteName.lastIndexOf("."));
		// add more info
		String title = "";
		if (isFollowMode) {
			title = mSQLiteName + " @";
		} else {
			title = mSQLiteName;
		}
		title += " "+ locationType;
		String zoom = String.valueOf(17-PhysicMap.getZoomLevel());
		title += " ["+ zoom + "]";
		activity.setTitle(title);
		int imageID = activity.getResources().getIdentifier("scale"+zoom, "drawable", activity.getPackageName());
		scaleImageView.setImageResource(imageID);
	}
}