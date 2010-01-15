package com.nevilon.bigplanet;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.XMPPException;
import org.traveler.googleclientlogin.GoogleAccountActivity;
import org.traveler.track_manage.file.database.GpsLocationStoringThread;
import org.traveler.track_manage.file.database.TravelDataBaseAdapter;
import org.traveler.track_manage.simulate.GpsLocationLogParsingThread;
import org.traveler.track_manage.simulate.GpsTrackStorageSimulatorActivity;
import org.traveler.track_manage.view.ExtendedCheckBoxListActivity;
import org.traveler.track_manage.view.TrackListViewActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
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
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.util.Linkify;
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
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RadioGroup.OnCheckedChangeListener;

import com.nevilon.bigplanet.core.BigPlanetApp;
import com.nevilon.bigplanet.core.MarkerManager;
import com.nevilon.bigplanet.core.PhysicMap;
import com.nevilon.bigplanet.core.Place;
import com.nevilon.bigplanet.core.Preferences;
import com.nevilon.bigplanet.core.RawTile;
import com.nevilon.bigplanet.core.MarkerManager.Marker;
import com.nevilon.bigplanet.core.MarkerManager.Marker_G;
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

	/*
	 * Графический движок, реализующий карту
	 */
	private MapControl mapControl;

	private static MarkerManager mm;

	public static NotificationManager mNotificationManager;
	public static int Notification_RecordTrack = 0;
	public static int Notification_XMPP = 1;
	
	private static LocationManager locationManager;
	
	// 阿超//
	//private Location currentLocation;
	public static Location currentLocation;

	private boolean inHome = false;
	public static boolean isFollowMode = true; // default value is auto follow
	public static boolean isGPS_track = false;  // default false
	public static boolean isGPS_track_save = false;  // default false
	public static boolean isMapInCenter = false;
	public static boolean isDBdrawclear = false; // default false for DB clear
	
	private static String locationType = "";
	
	private boolean SDCARD_AVAILABLE = true;
	
	private MyIntentReceiver intentReceiver;
	private MyUpdateScreenIntentReceiver updateScreenIntentReceiver;
	
	private static RelativeLayout mAutoFollowRelativeLayout;
	private static RelativeLayout mTrackRelativeLayout;
	private static ImageView scaleImageView;
	
	public static TravelDataBaseAdapter DBAdapter;
	public static ProgressDialog myGPSDialog = null;
    Handler handler;
    Handler myHandler;

	/**
	 * Конструктор
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.i("BigPlanet:", "At BigPlanet,Now!");
		Log.i("Message", "new a DBAdapter instance");
        DBAdapter = new TravelDataBaseAdapter(this);
        
        myHandler = new Handler(){
			public void handleMessage(Message msg) {
				Log.i("Message", "flag="+msg.what); //1:�券import��;0:�典�import憭望�
				switch (msg.what)
				{
				   case GpsLocationStoringThread.SUCCESSFULLY:
					   /*��*/
					    Intent myIntent = new Intent();
				        myIntent.setClass(BigPlanet.this, TrackListViewActivity.class);
				        Log.i("Message", "calling TrackListViewActivity");
				        startActivity(myIntent);
						break;
						
					case GpsLocationLogParsingThread.FAIL:
						/*�*/
						Toast.makeText(
							    BigPlanet.this,
	            		        getString(R.string.fail)+"\n"+(String)msg.obj,
	            		        Toast.LENGTH_LONG).show();
				         break;
				}
         }};
         myHandler.removeMessages(0);

		boolean hasSD = false;
		// проверка на доступность sd
		String status = Environment.getExternalStorageState();
		if (!status.equals(Environment.MEDIA_MOUNTED)) {
			SDCARD_AVAILABLE = false;
			new AlertDialog.Builder(this).setMessage(R.string.sdcard_unavailable)
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

			mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			
			SmoothZoomEngine.stop = false;
			TileLoader.stop = false;
			mAutoFollowRelativeLayout = getAutoFollowRelativeLayout();
			mAutoFollowRelativeLayout.setVisibility(View.INVISIBLE);
			mTrackRelativeLayout = getTrackRelativeLayout();
			mTrackRelativeLayout.setVisibility(View.VISIBLE);
			
			//Add by Taiyu
			File trackImportFolder = new File(SQLLocalStorage.TRACK_IMPORT_PATH);
			if(!trackImportFolder.exists())
			{
				trackImportFolder.mkdirs();
				Log.i("Message", "trackImportFolder creates="+trackImportFolder);
			}	
			trackImportFolder = null;
			
			File mapsDBFolder = new File(SQLLocalStorage.DATA_PATH);
			if (!mapsDBFolder.exists())
				mapsDBFolder.mkdirs();
			mapsDBFolder = null;

			initializeMap();
			/* Create a ImageView with a auto-follow icon. */
			mapControl.addView(mAutoFollowRelativeLayout); // We can just run it once.
			/* Create a ImageView with a Track icon. */
			mapControl.addView(mTrackRelativeLayout); // We can just run it once.
			/* Create a ImageView with a scale image. */
			scaleImageView = new ImageView(this);
			scaleImageView.setImageResource(R.drawable.scale1);
			mapControl.addView(scaleImageView);
			
			if (BigPlanetApp.isDemo) {
				showTrialDialog(R.string.this_is_demo_title, R.string.this_is_demo_message);
			}
			setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
		}
		
		if (hasSD) {
			setActivityTitle(BigPlanet.this);
		}
	}
	
	public static void disabledAutoFollow(Context context) {
		if (isFollowMode) {
			Toast.makeText(context, R.string.auto_follow_disabled, Toast.LENGTH_SHORT).show();
			mAutoFollowRelativeLayout.setVisibility(View.VISIBLE);
			isFollowMode = false;
		}
		setActivityTitle((Activity) context);
	}
	
	public void enabledAutoFollow(Context context) {
		if (!isFollowMode) {
			Toast.makeText(context, R.string.auto_follow_enabled, Toast.LENGTH_SHORT).show();
			mAutoFollowRelativeLayout.setVisibility(View.INVISIBLE);
			goToMyLocation(currentLocation, PhysicMap.getZoomLevel());
			isFollowMode = true;
		}
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
	
	public void enabledTrack(Context context) {
		if (isGPS_track) {
			Toast.makeText(context, R.string.track_enabled, Toast.LENGTH_SHORT).show();
			//startGPSLocationListener();
			setNotification(this, Notification_RecordTrack);
		}
		setActivityTitle((Activity) context);
		mapControl.invalidate();
	}
	
	public void disabledTrack(Context context) {
		if (!isGPS_track) {
			Toast.makeText(context, R.string.track_disabled, Toast.LENGTH_SHORT).show();
			//finishGPSLocationListener(false);
			clearNotification(Notification_RecordTrack);
		}
		mm.saveMarkerGTrack();
		isGPS_track_save = true;
		setActivityTitle((Activity) context);
		mapControl.invalidate();
	}

	private ImageView ivRecordTrack;
	
	private RelativeLayout getTrackRelativeLayout() {
		final RelativeLayout relativeLayout = new RelativeLayout(this);
		
		/* Create a ImageView with a track icon. */
		ivRecordTrack = new ImageView(this);
		ivRecordTrack.setImageResource(R.drawable.btn_record_start);
		
		ivRecordTrack.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if(!isGPS_track){
					isGPS_track = true;
					ivRecordTrack.setImageResource(R.drawable.btn_record_stop);
					enabledTrack(BigPlanet.this);
				}else{
					isGPS_track = false;
					ivRecordTrack.setImageResource(R.drawable.btn_record_start);
					disabledTrack(BigPlanet.this);
					Log.i("Message","Stop Recording GPS Location...");
					Log.i("Message","Start to Store GPS Location to DB...");
				
					/*
					ArrayList<Location> locationList = new ArrayList<Location>();
					String gpsLogFilePath = "/sdcard/RMaps/tracks/import/gps1.log"; // need to create the folder
					File gpsLogFile = new File(gpsLogFilePath);
					FileInputStream file_stream;
					try{
						Log.i("Message:","GPS_Log_File_Path="+gpsLogFilePath);
						Log.i("Message:","Parsing Log begins...");
						file_stream = new FileInputStream(gpsLogFile);
						//reader = new InputStreamReader(file_stream,"UTF-8");
						BufferedReader in = new BufferedReader(new InputStreamReader(file_stream,"UTF-8"));
						String str;
						in.readLine();
			
						while ((str = in.readLine()) != null) {
							String[] second_line = in.readLine().split("	");
							Location location = new Location("NTU Traveler");
							location.setLongitude(Double.parseDouble(second_line[0]));
							location.setLatitude(Double.parseDouble(second_line[1]));
							location.setAltitude(Double.parseDouble(second_line[2]));
							location.setTime(Long.parseLong(second_line[3]));
							location.setSpeed(Float.parseFloat(second_line[4]));
							location.setBearing(Float.parseFloat(second_line[5]));
							location.setAccuracy(Float.parseFloat(second_line[6]));
							Log.i("Message:","lan="+location.getLatitude()+",lon="+location.getLongitude()+",Altitude="+location.getAltitude()
									+",time="+location.getTime()+",speed="+location.getSpeed()+",bearing="+location.getBearing()+
									",accuracy="+location.getAccuracy());
							locationList.add(location);
						}
					}
					catch(Exception e){
						e.printStackTrace();
					}
					*/
					
				  if(MarkerManager.getLocationList().size()>0) // check out whether GPS LocationList contains any GPS data or not
				  {	
					// 憿舐內Progress撠店�孵�
						final CharSequence strDialogTitle = getString(R.string.str_store_gps_location_to_db_title);
						final CharSequence strDialogBody = getString(R.string.str_store_gps_location_to_db_body);
						myGPSDialog = ProgressDialog.show
				                 (
				                   BigPlanet.this,
				                   strDialogTitle,
				                   strDialogBody, 
				                   true
				                 );
					  
					GpsLocationStoringThread storingThread = new GpsLocationStoringThread();
					storingThread.setMainHandler(myHandler);
					storingThread.setLocationList(MarkerManager.getLocationList());
					//storingThread.setLocationList(locationList);
					storingThread.start();
				  }
				  else{
					  Toast.makeText(
							    BigPlanet.this,
	            		        getString(R.string.gps_locationlist_has_no_data),
	            		        Toast.LENGTH_LONG).show();
				  }
					
				}
			}
        });
		
		/* Create RelativeLayoutParams, that position in in the bottom right corner. */
		final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		relativeLayout.addView(ivRecordTrack, params);
		
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
			mm.addMarker(place, z, 0, MarkerManager.SEARCH_MARKER);
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
			mapControl.invalidate();
			// see MapControl zoomPanel.setOnZoomOutClickListener
			if (intent.getBooleanExtra(MapControl.FIX_ZOOM, false)) {
				Log.i("BP", "%% "+MapControl.FIX_ZOOM);
				mapControl.invalidate();
			}
			// center map
			if (isFollowMode && !isMapInCenter) {
				if (currentLocation != null) {
					Log.i("BP", "%% goToMyLocation");
					Log.i("BP", "%% "+currentLocation.getProvider()+" "
							+currentLocation.getLongitude()+", "+currentLocation.getLatitude());
					isMapInCenter = true;
					int zoom = PhysicMap.getZoomLevel();
					double lat = currentLocation.getLatitude();
					double lon = currentLocation.getLongitude();
					com.nevilon.bigplanet.core.geoutils.Point p = GeoUtils.toTileXY(lat, lon, zoom);
					com.nevilon.bigplanet.core.geoutils.Point off = GeoUtils.getPixelOffsetInTile(lat, lon, zoom);
					mapControl.goTo((int) p.x, (int) p.y, zoom, (int) off.x, (int) off.y);
				}
			}
			// refresh the activity title
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
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		if (SDCARD_AVAILABLE) {
			startGPSLocationListener();
			followMyLocation();
			if (isGPS_track){
				ivRecordTrack.setImageResource(R.drawable.btn_record_stop);
			} else{
				ivRecordTrack.setImageResource(R.drawable.btn_record_start);
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
		if (intentReceiver != null){
			unregisterReceiver(intentReceiver);
			Preferences.putTile(mapControl.getPhysicalMap().getDefaultTile());
			Preferences.putOffset(mapControl.getPhysicalMap().getGlobalOffset());
		}
		if (updateScreenIntentReceiver != null){
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

		/* Add by Taiyu */
		SubMenu sub = menu.addSubMenu(5, 101, 0, R.string.TRACK_MANAGE_MENU).setIcon(R.drawable.track_manage);
        sub.add(6, 102, 0, R.string.BROWSE_TRACK_MENU);
        sub.add(6, 103, 1, R.string.IMPORT_TRACK_MENU);
        //sub.add(6, 104, 2, R.string.RECORD_GPS_TRACK_MENU);
        sub.add(6, 105, 3, R.string.ERASE_LEADER_TRACK_MENU);
        sub.add(6, 106, 4, R.string.ERASE_RECORDED_TRACK_MENU);
        sub.add(6, 107, 5, R.string.ERASE_REFERENCE_TRACK_MENU);

		sub = menu.addSubMenu(0, 6, 0, R.string.BOOKMARKS_MENU).setIcon(R.drawable.bookmark);
		sub.add(2, 21, 0, R.string.BOOKMARK_ADD_MENU);
		sub.add(2, 22, 1, R.string.BOOKMARKS_VIEW_MENU);

		menu.add(3, 33, 0, R.string.MY_LOCATION_MENU).setIcon(R.drawable.home);

		menu.add(5, 51, 0, R.string.SHARE_MENU).setIcon(R.drawable.friends_icon);

		// add More menu
		sub = menu.addSubMenu(0, 1, 0, R.string.TOOLS_MENU).setIcon(R.drawable.tools);
		sub.add(4, 41, 0, R.string.NETWORK_MODE_MENU);
		sub.add(4, 42, 1, R.string.CACHE_MAP_MENU);
		sub.add(4, 43, 2, R.string.MAP_SOURCE_MENU);
		sub.add(4, 44, 3, R.string.SQLiteDB_MENU);
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
	
		menu.findItem(105).setEnabled(checkMarkers(MarkerManager.markers_leader));
		menu.findItem(106).setEnabled(checkMarkers(MarkerManager.saveTracks_G));
		menu.findItem(107).setEnabled(checkMarkers(MarkerManager.markers_DB));
		return true;
	}
	
	public boolean checkMarkers(List<Marker_G> list) {
		if(list.size()>0){
			return true;
		}
		return false;
	}

	/**
	 * Устанавливает размеры карты и др. свойства
	 */
	private void configMapControl(RawTile tile) {
		WindowManager wm = this.getWindowManager();
		Display display = wm.getDefaultDisplay();
		int height = display.getHeight();
		int width = display.getWidth();
//		if(height==480){
//			height =430;
//		}
		height = height - 50; // minus the space of the status bar
		
		if (mapControl == null) {
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
			selectSQLiteDBFile();
			break;
		case 49:
			showAbout();
			break;
		case 51:
			shareLocation();
			break;
		case 102: //browse tracks in SqliteDB
			browseTracks();
			break;
		case 103: //import tracks from SD card
			importTracks();
			break;
		case 104: //simulate GPS track storage
			recordGpsTracks();
			break;
		case 105: //erase track
			eraseLeader();
			break;
		case 106: //erase track
			eraseSaveTracks_G();
			break;
		case 107: //erase track
			eraseMarker_DB();
			break;

		}
		return false;
	}

	private void browseTracks(){
		Log.i("Message", "Press--Browse Track function");
		Intent myIntent = new Intent();
		myIntent.setClass(BigPlanet.this, TrackListViewActivity.class);
        Log.i("Message", "calling TrackListViewActivity");
        startActivity(myIntent);
	}
	
	private void importTracks(){
		Log.i("Message", "Press--Import Track function");
		Intent importTrackIntent = new Intent(this, ExtendedCheckBoxListActivity.class);
		startActivity(importTrackIntent);
	}
	
	private void recordGpsTracks(){
		Log.i("Message", "Press--Simulate GPS Track Storage function");
		Intent importTrackIntent = new Intent(this,GpsTrackStorageSimulatorActivity.class);
		startActivity(importTrackIntent);
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
		handler = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				int okValue = (Integer) msg.what;

				if (okValue == 0) {
					okBtn.setText(R.string.OK_LABEL);
					okBtn.setEnabled(true);
				}else if(okValue == 1)
				{
					Intent myIntent = new Intent();
			        myIntent.setClass(BigPlanet.this, TrackListViewActivity.class);
			        Log.i("Message", "calling TrackListViewActivity");
			        startActivity(myIntent);
				}
				else if(okValue == 2)
				{
					Toast.makeText(
							    BigPlanet.this,
	            		        getString(R.string.fail),
	            		        Toast.LENGTH_LONG).show();
				}
				else {
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
					if (!isGPS_track){
						goToMyLocation(currentLocation, PhysicMap.getZoomLevel());
					}
					if (isGPS_track){
						trackMyLocation(currentLocation, PhysicMap.getZoomLevel());
					}
				}
			}
		} else { // gps and network are both disabled
			Toast.makeText(this, R.string.msg_unable_to_get_current_location, 3000).show();
		}

		/* GPS_PROVIDER */
		if (gpsLocationListener == null) {
			gpsLocationListener = new MyLocationListener("gps");
			// LocationManager.GPS_PROVIDER = "gps"
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 
					minTime, minDistance, gpsLocationListener);
		}

		/* NETWORK_PROVIDER */
		if (networkLocationListener == null) {
			networkLocationListener = new MyLocationListener("network");
			// LocationManager.NETWORK_PROVIDER = "network"
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 
					minTime, minDistance, networkLocationListener);
		}
	}
	
	public static void finishGPSLocationListener(boolean isSetNull) {
		isFollowMode = false;
		locationType = "";
		if (!isGPS_track) {
			if (locationManager != null) {
				locationManager.removeUpdates(networkLocationListener);
				locationManager.removeUpdates(gpsLocationListener);
				if (isSetNull) {
					networkLocationListener = null;
					gpsLocationListener = null;
				}
			}
		}
//		if (mHandler != null) {
//			mHandler.getLooper().quit();
//		}
	}
	
	private static LocationListener gpsLocationListener;
	private static LocationListener networkLocationListener;
	private final long minTime = 2000; // ms
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
			currentLocation = location;
			if (!isGPS_track){
				if (isFollowMode) {
					goToMyLocation(location, PhysicMap.getZoomLevel());
				} else {
					addMarker(location, PhysicMap.getZoomLevel());
				}
			}
			if(isGPS_track){
				if (isFollowMode) {
					trackMyLocation(location, PhysicMap.getZoomLevel());
				} else {
					addMarker(location, PhysicMap.getZoomLevel());
				}
			}
			// send GPS location if connecting to XMPP server and the role is Leader
			// TODO: send out when getting more GPS coordinates, see GoogleAccountActivity.xmppHandler if modified
			if (GoogleAccountActivity.xmppService != null && 
					GoogleAccountActivity.xmppService.isConnected()) {
				if (GoogleAccountActivity.isLeader) {
					String groupname = GoogleAccountActivity.Groupname;
					String gps = longitude+","+latitude;
					String message = "group:"+groupname+";"+"gps:"+gps;
					Log.i("onLocationChanged", "xmppService.sendMessage("+ message +")");
					try {
						GoogleAccountActivity.xmppService.sendMessage(message);
					} catch (XMPPException e) {
						e.printStackTrace();
					}
				}
			}
			BigPlanet.locationType = location.getProvider();
			setActivityTitle(BigPlanet.this);
			// gpsLocationListener has higher priority than networkLocationListener
			if (myLocationType.equals("gps")) {
				locationManager.removeUpdates(networkLocationListener);
			}
			mapControl.invalidate();
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
		if (!isFollowMode) {
			enabledAutoFollow(this);
		}
	}

	// 阿超	
	//private void goToMyLocation(Location location, int zoom) {
	public void goToMyLocation(Location location, int zoom) {
		double lat = location.getLatitude();
		double lon = location.getLongitude();
		com.nevilon.bigplanet.core.geoutils.Point p = GeoUtils.toTileXY(lat, lon, zoom);
		com.nevilon.bigplanet.core.geoutils.Point off = GeoUtils.getPixelOffsetInTile(lat, lon, zoom);
		mapControl.goTo((int) p.x, (int) p.y, zoom, (int) off.x, (int) off.y);
		
		Place place = new Place();
		place.setLat(lat);
		place.setLon(lon);
		mm.addMarker(place, zoom, 1, MarkerManager.MY_LOCATION_MARKER);
	}
	
	private void trackMyLocation(Location location, int zoom) {
		double lat = location.getLatitude();
		double lon = location.getLongitude();
		com.nevilon.bigplanet.core.geoutils.Point p = GeoUtils.toTileXY(lat, lon, zoom);
		com.nevilon.bigplanet.core.geoutils.Point off = GeoUtils.getPixelOffsetInTile(lat, lon, zoom);
		mapControl.goTo((int) p.x, (int) p.y, zoom, (int) off.x, (int) off.y);

		Place place = new Place();
		place.setLat(lat);
		place.setLon(lon);
		place.setLocation(location);
		
//		SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd G 'at' hh:mm:ss a zzz");
//		List<String> items = new ArrayList<String>();
//		items.add(formatter.format(time));
		mm.addMarker(place, zoom, 1, MarkerManager.MY_LOCATION_MARKER);
	}
	
	private void addMarker(Location location, int zoom) {
		double lat = location.getLatitude();
		double lon = location.getLongitude();
		Place place = new Place();
		place.setLat(lat);
		place.setLon(lon);
		place.setLocation(location);
		mm.addMarker(place, zoom, 1, MarkerManager.MY_LOCATION_MARKER);
	}
	
	public static void addMarkersForDrawing(Context context, List<Place> placeList, int imageType) {
		/**
		 * imagetype:
		 * 2 -> from DB
		 * 3 -> trackLeader
		 */
		Log.i("Message", "At addMarkerForDrawing........Type="+imageType);

		int zoom = PhysicMap.getZoomLevel();

		double latTemp = 0, latMax = 0,latMin = 999, lonTemp = 0, lonMax = 0, lonMin = 999;
		for (int i=0;i<placeList.size();i++)
		{
			Place place = placeList.get(i);
			mm.addMarker(place, zoom, imageType, MarkerManager.MY_LOCATION_MARKER);
			
			if (imageType == 2){	
				latTemp = place.getLat();
				latMax = Math.max(latTemp, latMax);
				latMin = Math.min(latTemp, latMin);
				lonTemp = place.getLon();
				lonMax = Math.max(lonTemp, lonMax);
				lonMin = Math.min(lonTemp, lonMin);
				//com.nevilon.bigplanet.core.geoutils.Point p = GeoUtils.toTileXY(latMax, lonMax, zoom);
				//com.nevilon.bigplanet.core.geoutils.Point off = GeoUtils.getPixelOffsetInTile(latMax, lonMax, zoom);
				//mapControl.goTo((int) p.x, (int) p.y, zoom, (int) off.x, (int) off.y);
				int a = 1;
			}
		}
		

		
				
		if(!isDBdrawclear){
			isDBdrawclear = true;
		}
		Intent i = new Intent("com.nevilon.bigplanet.INTENTS.UpdateScreen");
		context.sendBroadcast(i);
	}
	
	public void eraseSaveTracks_G(){
		if(!mm.clearSaveTracks_G()){
			textMessage = Toast.makeText(this, R.string.clearSaveTrackFales,
					Toast.LENGTH_LONG);
			textMessage.show();
		}
		mapControl.invalidate();
	}
	
	public void eraseMarker_DB(){
		mm.clearMarker_DB();
		mapControl.invalidate();
	}
	
	public void eraseLeader(){
		mm.clearLeader();
		mapControl.invalidate();
	}

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
		String versionName = "";
		try {
			String PACKAGE_NAME = BigPlanet.class.getPackage().getName();
			PackageInfo info = getPackageManager().getPackageInfo(PACKAGE_NAME, 0);
			versionName = info.versionName;
		} catch (PackageManager.NameNotFoundException e) {
	    }	
		new AlertDialog.Builder(this).setTitle(getString(R.string.ABOUT_TITLE)+" "+versionName)
				.setView(tv).setIcon(R.drawable.globe).setPositiveButton(
						R.string.OK_LABEL,
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog,
									int whichButton) {

							}

						}).show();
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
			for (int i = 0; i < files.length; i++) {
				File sqliteFile = new File(mapsDBFolder, files[i]);
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
						String strSQLiteName = sqliteMaps.get(checkedId);
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
	
	private void shareLocation() {
		Intent intent = new Intent();
		intent.setClass(this, GoogleAccountActivity.class);
		startActivity(intent);
	}

	public static void setActivityTitle(Activity activity) {
		String strSQLiteName = Preferences.getSQLiteName();
		// remove ".sqlitedb"
		strSQLiteName = strSQLiteName.substring(0, strSQLiteName.lastIndexOf("."));
		// add more info
		String title = strSQLiteName + " @ "+ locationType;
		String zoom = String.valueOf(17-PhysicMap.getZoomLevel());
		title += " ["+ zoom + "]";
		activity.setTitle(title);
		int imageID = activity.getResources().getIdentifier("scale"+zoom, "drawable", activity.getPackageName());
		scaleImageView.setImageResource(imageID);
	}
	
	public static void setNotification(Context context, int notificationId) {
		int iconId = 0;
		String contentTitle = null;
		String contentText = null;
		
		if (notificationId == Notification_RecordTrack) {
			iconId = R.drawable.globe;
			contentTitle = context.getString(R.string.app_name);
			contentText = context.getString(R.string.notify_recording);
		} else if (notificationId == Notification_XMPP) {
			contentTitle = context.getString(R.string.msg_online);
			if (GoogleAccountActivity.isLeader) {
				iconId = R.drawable.user_out;
				contentText = context.getString(R.string.msg_send);
			} else {
				iconId = R.drawable.user_in;
				contentText = context.getString(R.string.msg_receive);
			}
		}
		
		Intent notifyIntent = new Intent(context, BigPlanet.class);
		PendingIntent pendingIntent = 
			PendingIntent.getActivity(context, 0, notifyIntent, Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);

		Notification notification = new Notification();
		notification.flags = Notification.FLAG_NO_CLEAR;
		notification.icon = iconId;
		notification.defaults = Notification.DEFAULT_SOUND;
		
		notification.setLatestEventInfo(context, contentTitle, contentText, pendingIntent);
		mNotificationManager.notify(notificationId, notification);
	}
	
	public static void clearNotification(int notificationId) {
		mNotificationManager.cancel(notificationId);
	}
}