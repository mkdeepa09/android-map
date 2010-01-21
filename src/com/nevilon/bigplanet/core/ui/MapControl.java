package com.nevilon.bigplanet.core.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.nevilon.bigplanet.BigPlanet;
import com.nevilon.bigplanet.R;
import com.nevilon.bigplanet.core.AbstractCommand;
import com.nevilon.bigplanet.core.MarkerManager;
import com.nevilon.bigplanet.core.PhysicMap;
import com.nevilon.bigplanet.core.Place;
import com.nevilon.bigplanet.core.RawTile;
import com.nevilon.bigplanet.core.MarkerManager.Marker;
import com.nevilon.bigplanet.core.MarkerManager.Marker_G;

/**
 * Виджет, реализующий карту
 * 
 * @author hudvin
 * 
 */
public class MapControl extends RelativeLayout {

	private static final int TILE_SIZE = 256;

	public static final int ZOOM_MODE = 0;

	public static final int SELECT_MODE = 1;

	private int mapMode = ZOOM_MODE;

	//private int SMOOTH_ZOOM_INTERVAL = 20;

	/*
	 * Панель с картой
	 */
	private Panel main;

	Canvas cs;

	/*
	 * Детектор двойного тача
	 */
	private DoubleClickDetector dcDetector = new DoubleClickDetector();

	/*
	 * Движок карты
	 */
	private PhysicMap pmap;

	/*
	 * Панель с зум-контролами
	 */
	private ZoomPanel zoomPanel;

	private boolean isNew = true;

	private Bitmap cb = null;

	/*
	 * Размер ячейки фона
	 */
	private final static int BCG_CELL_SIZE = 16;

	private OnMapLongClickListener onMapLongClickListener;

	private MarkerManager markerManager;

	public static Bitmap CELL_BACKGROUND = BitmapUtils.drawBackground(
			BCG_CELL_SIZE, TILE_SIZE, TILE_SIZE);

	public static Bitmap EMPTY_BACKGROUND = BitmapUtils
			.drawEmptyBackground(TILE_SIZE);

	public Bitmap PLACE_MARKER = BitmapFactory.decodeResource(getResources(),
			R.drawable.marker);

	private Point scalePoint = new Point();

	private SmoothZoomEngine szEngine;

	public Handler h;
	
	private Context context;
	
	int z2 = 3;
	
	private List<Marker> markers_temp = new ArrayList<Marker>();

	/**
	 * Конструктор
	 * 
	 * @param context
	 * @param width
	 * @param height
	 * @param startTile
	 */
	public MapControl(Context context, int width, int height,
			RawTile startTile, MarkerManager markerManager) {
		super(context);
		this.context = context;
		scalePoint.set(width / 2, height / 2);
		this.markerManager = markerManager;
		buildView(width, height, startTile);
		
		
		final Handler updateControlsHandler = new Handler() {

			@Override
			public void handleMessage(Message msg) {

				switch (msg.what) {
				case 0:
					updateZoomControls();
					break;
				}
				super.handleMessage(msg);
			}
		};
		
		szEngine = SmoothZoomEngine.getInstance();
		szEngine.setReloadMapCommand(new AbstractCommand() {

			public void execute(Object object) {
				double sf = (Float) object;
				pmap.zoomS(sf);
				updateControlsHandler.sendEmptyMessage(0);
			}

		});
		szEngine.setUpdateScreenCommand(new AbstractCommand() {
			public void execute(Object object) {

				pmap.scaleFactor = (Float) object;
				postInvalidate();

			}

		});
	}

	public int getMapMode() {
		return mapMode;
	}

	/**
	 * Устанавливает режим карты и состояние зум-контролов(выбор объекта для
	 * добавления в закладки либо навигация)
	 * 
	 * @param mapMode
	 */
	public void setMapMode(int mapMode) {
		this.mapMode = mapMode;
		updateZoomControls();
	}

	public void setOnMapLongClickListener(
			OnMapLongClickListener onMapLongClickListener) {
		this.onMapLongClickListener = onMapLongClickListener;
	}

	/**
	 * Устанавливает размеры карты и дочерних контролов
	 * 
	 * @param width
	 * @param height
	 */
	public void setSize(int width, int height) {
		if (main != null) {
			removeView(main);
		}
		buildView(width, height, pmap.getDefaultTile());

	}

	/**
	 * Возвращает движок карты
	 * 
	 * @return
	 */
	public PhysicMap getPhysicalMap() {
		return pmap;
	}

	public void goTo(int x, int y, int z, int offsetX, int offsetY) {
		getPhysicalMap().goTo(x, y, z, offsetX, offsetY);
		updateZoomControls();
		updateScreen();
	}
	
	// Fix Zoom <= 3
	public void fixZoom() {
		new Handler().postDelayed(new Runnable(){
			@Override
			public void run() {
				Intent i = new Intent("com.nevilon.bigplanet.INTENTS.UpdateScreen");
				context.sendBroadcast(i);
			}
		}, 2000);
	}

	/**
	 * Строит виджет, устанавливает обработчики, размеры и др.
	 * 
	 * @param width
	 * @param height
	 * @param startTile
	 */
	private void buildView(int width, int height, RawTile startTile) {
		h = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				updateZoomControls();
			}
		};
		
		// создание панели с картой
		main = new Panel(this.getContext());
		addView(main, 0, new ViewGroup.LayoutParams(width, height));
		// создание зум-панели
		if (zoomPanel == null) { // если не создана раньше
			zoomPanel = new ZoomPanel(this.getContext());
			// обработчик уменьшения
			zoomPanel.setOnZoomOutClickListener(new OnClickListener() {
				public void onClick(View v) {
					Log.i("MapControl", "OnZoomOutClick");
					BigPlanet.isMapInCenter = false;
					int zoomLevel = PhysicMap.getZoomLevel();
					if (zoomLevel >= 17) {
						return;
					}
					if (zoomLevel >= 13) {  // >= 13 doesn't zoom out anymore
						fixZoom();
					}
					//zoomPanel.setIsZoomOutEnabled(false); // avoid double click to cause grey screen
					scalePoint.set(pmap.getWidth() / 2, pmap.getHeight() / 2);
					smoothZoom(-1);
				}
			});

			// обработчик увеличения
			zoomPanel.setOnZoomInClickListener(new OnClickListener() {
				public void onClick(View v) {
					Log.i("MapControl", "OnZoomInClick");
					BigPlanet.isMapInCenter = false;
					int zoomLevel = PhysicMap.getZoomLevel();
					if (zoomLevel <= -2) {
						return;
					}
					if (zoomLevel >= 15) {  // >= 15 doesn't zoom out anymore
						fixZoom();
					}
					//zoomPanel.setIsZoomInEnabled(false); // avoid double click to cause grey screen
					scalePoint.set(pmap.getWidth() / 2, pmap.getHeight() / 2);
					smoothZoom(1);
				}
			});

			addView(zoomPanel, new LayoutParams(LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT));

		}
		zoomPanel.setPadding((width - 160) / 2, height - 50, 0, 0);

		if (pmap == null) { // если не был создан раньше
			pmap = new PhysicMap(startTile, new AbstractCommand() {

				/**
				 * Callback, выполняющий перерисовку карты по запросу
				 */
				@Override
				public void execute() {
					updateScreen();
				}

			});
		}
		pmap.setHeight(height);
		pmap.setWidth(width);
		pmap.quickHack();
	}

	private void smoothZoom(int direction) {
		System.out.println(getPhysicalMap().getTileResolver().getLoaded());
		szEngine.addToScaleQ(direction);
	}

	public synchronized void updateScreen() {
		if (main != null) {
			main.postInvalidate();
			Intent i = new Intent("com.nevilon.bigplanet.INTENTS.UpdateScreen");
			context.sendBroadcast(i);
		}
	}
	
	private void invokeGoToMyLocation(double lat, double lon, int zoom) {
		Intent i = new Intent("com.nevilon.bigplanet.INTENTS.UpdateScreen");
		i.putExtra("type", 1);
		i.putExtra("lat", lat);
		i.putExtra("lon", lon);
		i.putExtra("zoom", zoom);
		context.sendBroadcast(i);
	}

	/**
	 * Устанавливает состояние zoomIn/zoomOut контролов в зависимости от уровня
	 * зума
	 */
	public void updateZoomControls() {
		pmap.getTileResolver().clearCache();
		System.gc();
		int zoomLevel = PhysicMap.getZoomLevel();
		markerManager.updateAll(zoomLevel);
		if (getMapMode() == MapControl.SELECT_MODE) {
			zoomPanel.setVisibility(View.INVISIBLE);
		} else {
			zoomPanel.setVisibility(View.VISIBLE);
			if (zoomLevel >= 17) {
				zoomPanel.setIsZoomOutEnabled(false);
				zoomPanel.setIsZoomInEnabled(true);
			} else if (zoomLevel <= -2) {
				zoomPanel.setIsZoomOutEnabled(true);
				zoomPanel.setIsZoomInEnabled(false);
			} else {
				zoomPanel.setIsZoomOutEnabled(true);
				zoomPanel.setIsZoomInEnabled(true);
			}
		}
	}

	/**
	 * Перерисовывает карту
	 * 
	 * @param canvas
	 * @param paint
	 */
	private synchronized void doDraw(Canvas c, Paint paint, boolean isScalable) {
		if (cb == null || cb.getHeight() != pmap.getHeight()) {
			if (isScalable) {
				cs = new Canvas();
			} else {
				cs = c;
			}
			cb = Bitmap.createBitmap(pmap.getWidth(), pmap.getHeight(),
					Bitmap.Config.RGB_565);
			cs.setBitmap(cb);
		}
		System.out.println("doDraw scaleFactor " + pmap.scaleFactor);
		Bitmap tmpBitmap;
		for (int i = 0; i < 7; i++) {
			for (int j = 0; j < 7; j++) {
				if ((i > 1 && i < 5) && ((j > 1 && j < 5))) {
					tmpBitmap = pmap.getCell(i - 2, j - 2);
					if (tmpBitmap != null) {
						isNew = false;
						cs.drawBitmap(tmpBitmap, (i - 2) * TILE_SIZE
								+ pmap.getGlobalOffset().x, (j - 2) * TILE_SIZE
								+ pmap.getGlobalOffset().y, paint);
					}
				} else {
					if (pmap.scaleFactor == 1) {
						cs.drawBitmap(CELL_BACKGROUND, (i - 2) * TILE_SIZE
								+ pmap.getGlobalOffset().x, (j - 2) * TILE_SIZE
								+ pmap.getGlobalOffset().y, paint);
					} else {
						cs.drawBitmap(EMPTY_BACKGROUND, (i - 2) * TILE_SIZE
								+ pmap.getGlobalOffset().x, (j - 2) * TILE_SIZE
								+ pmap.getGlobalOffset().y, paint);

					}
				}
			}
		}

		if (pmap.scaleFactor == 1) {
			// отрисовка маркеров
			if(!BigPlanet.isGPS_track){
				for (int i = 0; i < 7; i++) {
					for (int j = 0; j < 7; j++) {
						if ((i > 1 && i < 5) && ((j > 1 && j < 5))) {
							RawTile tile = pmap.getDefaultTile();
							int z =PhysicMap.getZoomLevel();
							int tileX = tile.x + (i - 2);
							int tileY = tile.y + (j - 2);
							List<Marker> markers = markerManager.getMarkers(tileX,tileY, z);
							
							for (Marker marker : markers) {
								cs.drawBitmap(marker.getMarkerImage().getImage(),
										(i - 2) * TILE_SIZE	+ pmap.getGlobalOffset().x
										+ (int) marker.getOffset().x
										- marker.getMarkerImage().getOffsetX(), 
										(j - 2) * TILE_SIZE	+ pmap.getGlobalOffset().y
										+ (int) marker.getOffset().y
										- marker.getMarkerImage().getOffsetY(), paint);
							}
						}
					}
				}
			}
		}		
		
		boolean markers_G_draw = false;
		boolean saveTracks_G_draw = false;
		boolean markers_DB_draw = false;
		boolean markers_leader_draw = false;
		if (BigPlanet.isGPS_track && MarkerManager.markers_G.size()!=0){
			markers_G_draw = true;}
		if (BigPlanet.isGPS_track_save && MarkerManager.saveTracks_G.size()!=0){
			saveTracks_G_draw = true;}
		if (MarkerManager.markers_DB.size()!=0){
			markers_DB_draw = true;}
		if (MarkerManager.markers_leader.size()!=0){
			markers_leader_draw = true;}
		int MaxSize = FindMaxSize(MarkerManager.markers_G.size(),MarkerManager.saveTracks_G.size(),MarkerManager.markers_DB.size(),MarkerManager.markers_leader.size());
		
		if (markers_G_draw || saveTracks_G_draw || markers_DB_draw || markers_leader_draw){
			float x11=0,x12=0,x1=0,y11=0,y12=0,y1=0,x21=0,x22=0,x2=0,y21=0,y22=0,y2=0,x31=0,x32=0,x3=0,y31=0,y32=0,y3=0,x41=0,x42=0,y41=0,y42=0;
			int count_G = 0,count_Tracks_G=0,count_DB = 0,count_leader=0;
			for (int i2=0;i2<MaxSize;i2++) {
				for (int i = 0; i < 7; i++) {
					for (int j = 0; j < 7; j++) {
						if ((i > 1 && i < 5) && ((j > 1 && j < 5))) {
							RawTile tile = pmap.getDefaultTile();
							int z =PhysicMap.getZoomLevel();
							int tileX = tile.x + (i - 2);
							int tileY = tile.y + (j - 2);
							
							if(markers_G_draw && i2 < MarkerManager.markers_G.size()){
								paint.setColor(Color.BLUE);
								paint.setStrokeWidth(5);
								List<Marker_G> markers_G = markerManager.getMarkers_G_type(tileX, tileY, z, i2, 1);
									
								for (Marker_G marker_G : markers_G) {
									if(count_G == 0){
										x11 = (i - 2)* TILE_SIZE + pmap.getGlobalOffset().x + (int) marker_G.getOffset().x;
										y11 = (j - 2)* TILE_SIZE + pmap.getGlobalOffset().y	+ (int) marker_G.getOffset().y+3;
										if(x11 == 0 && y11 == 0){
											markers_G.clear();
										}else{
											x1 = x11;
											y1 = y11;
											count_G = 1;
										}	
									}else{
										x12 = (i - 2)* TILE_SIZE + pmap.getGlobalOffset().x	+ (int) marker_G.getOffset().x;
										y12 = (j - 2)* TILE_SIZE + pmap.getGlobalOffset().y	+ (int) marker_G.getOffset().y+3;
										if(x12 != 0 && y12 != 0){
											cs.drawLine(x11,y11,x12,y12,paint);
											x11 = x12;
											y11 = y12;
										}
									}
										
									if (i2==MarkerManager.markers_G.size()-1){
										MarkerManager.markers_G.get(0).setMarkerImage(MarkerManager.images.get(MarkerManager.START_MY_TRACK_MARKER));
										cs.drawBitmap(MarkerManager.markers_G.get(0).getMarkerImage().getImage(),
												x1- MarkerManager.markers_G.get(0).getMarkerImage().getOffsetX(),
												y1- MarkerManager.markers_G.get(0).getMarkerImage().getOffsetY(),paint);
										MarkerManager.markers_G.get(i2).setMarkerImage(MarkerManager.images.get(MarkerManager.END_MY_TRACK_MARKER));
										cs.drawBitmap(MarkerManager.markers_G.get(MarkerManager.markers_G.size()-1).getMarkerImage().getImage(),
												x12- MarkerManager.markers_G.get(MarkerManager.markers_G.size()-1).getMarkerImage().getOffsetX(),
												y12- MarkerManager.markers_G.get(MarkerManager.markers_G.size()-1).getMarkerImage().getOffsetY(),paint);
									}												
								}								
							}
							if(saveTracks_G_draw && i2 < MarkerManager.saveTracks_G.size()){
								paint.setColor(Color.GREEN);
								paint.setStrokeWidth(5);
								List<Marker_G> SaveTracks_G = markerManager.getMarkers_G_type(tileX, tileY, z, i2, 2);
								for (Marker_G SaveTrack_G : SaveTracks_G) {
									if(count_Tracks_G == 0){
										x21 = (i - 2)* TILE_SIZE + pmap.getGlobalOffset().x + (int) SaveTrack_G.getOffset().x;
										y21 = (j - 2)* TILE_SIZE + pmap.getGlobalOffset().y + (int) SaveTrack_G.getOffset().y+3;
										if(x21 == 0 && y21 == 0){
											SaveTracks_G.clear();
										}else{
											x2 = x21;
											y2 = y21;
											count_Tracks_G = 1;
										}	
									}else{
										x22 = (i - 2)* TILE_SIZE + pmap.getGlobalOffset().x + (int) SaveTrack_G.getOffset().x;
										y22 = (j - 2)* TILE_SIZE + pmap.getGlobalOffset().y	+ (int) SaveTrack_G.getOffset().y+3;
										if(x22 != 0 && y22 != 0){
											cs.drawLine(x21,y21,x22,y22,paint);
											x21 = x22;
											y21 = y22;
										}
									}
										
									if (i2==MarkerManager.saveTracks_G.size()-1){
										MarkerManager.saveTracks_G.get(i2).setMarkerImage(MarkerManager.images.get(MarkerManager.START_MY_TRACK_MARKER));
										cs.drawBitmap(MarkerManager.saveTracks_G.get(0).getMarkerImage().getImage(),
												x2- MarkerManager.saveTracks_G.get(0).getMarkerImage().getOffsetX(),
												y2- MarkerManager.saveTracks_G.get(0).getMarkerImage().getOffsetY(),paint);
										MarkerManager.saveTracks_G.get(i2).setMarkerImage(MarkerManager.images.get(MarkerManager.END_MY_TRACK_MARKER));
										cs.drawBitmap(SaveTrack_G.getMarkerImage().getImage(),
												x22- SaveTrack_G.getMarkerImage().getOffsetX(),
												y22- SaveTrack_G.getMarkerImage().getOffsetY(),paint);
										
									}
								}					
							}
							
							if(markers_DB_draw && i2 < MarkerManager.markers_DB.size()){
								paint.setColor(Color.CYAN);
								paint.setStrokeWidth(5);
								List<Marker_G> markers_DB = markerManager.getMarkers_G_type(tileX, tileY, z, i2, 3);
								for (Marker_G marker_DB : markers_DB) {
									if(count_DB == 0){
										x31 = (i - 2)* TILE_SIZE + pmap.getGlobalOffset().x + (int) marker_DB.getOffset().x;
										y31 = (j - 2)* TILE_SIZE + pmap.getGlobalOffset().y	+ (int) marker_DB.getOffset().y+3;
										if(x31 == 0 && y31 == 0){
											markers_DB.clear();
										}else{
											
											x3 = x31;
											y3 = y31;
											count_DB = 1;
										}	
									}else{
										x32 = (i - 2)* TILE_SIZE + pmap.getGlobalOffset().x	+ (int) marker_DB.getOffset().x;
										y32 = (j - 2)* TILE_SIZE + pmap.getGlobalOffset().y	+ (int) marker_DB.getOffset().y+3;
										if(x32 != 0 && y32 != 0){
											cs.drawLine(x31,y31,x32,y32,paint);
											x31 = x32;
											y31 = y32;
										}
									}
										
									if (i2==MarkerManager.markers_DB.size()-1){
										MarkerManager.markers_DB.get(0).setMarkerImage(MarkerManager.images.get(MarkerManager.START_MY_DB_MARKER));
										cs.drawBitmap(MarkerManager.markers_DB.get(0).getMarkerImage().getImage(),
												x3- MarkerManager.markers_DB.get(0).getMarkerImage().getOffsetX(),
												y3- MarkerManager.markers_DB.get(0).getMarkerImage().getOffsetY(),paint);		
										MarkerManager.markers_DB.get(i2).setMarkerImage(MarkerManager.images.get(MarkerManager.END_MY_DB_MARKER));
										cs.drawBitmap(marker_DB.getMarkerImage().getImage(),
												x32- marker_DB.getMarkerImage().getOffsetX(),
												y32- marker_DB.getMarkerImage().getOffsetY(),paint);
									}
								}									
							}
							if(markers_leader_draw && i2 <= MarkerManager.markers_leader.size()){
								paint.setColor(Color.RED);
								paint.setStrokeWidth(5);
								List<Marker_G> markers_leader = markerManager.getMarkers_G_type(tileX, tileY, z, i2, 4);
								
								for (Marker_G marker_leader : markers_leader) {
									if(count_leader == 0){
										x41 = (i - 2)* TILE_SIZE + pmap.getGlobalOffset().x + (int) marker_leader.getOffset().x;
										y41 = (j - 2)* TILE_SIZE + pmap.getGlobalOffset().y	+ (int) marker_leader.getOffset().y+3;
										if(x41 == 0 && y41 == 0){
											markers_leader.clear();
										}else{
											count_leader = 1;
										}	
									}else{
										x42 = (i - 2)* TILE_SIZE + pmap.getGlobalOffset().x	+ (int) marker_leader.getOffset().x;
										y42 = (j - 2)* TILE_SIZE + pmap.getGlobalOffset().y	+ (int) marker_leader.getOffset().y+3;
										if(x42 != 0 && y42 != 0){
											cs.drawLine(x41,y41,x42,y42,paint);
											x41 = x42;
											y41 = y42;
										}
									}
									if (i2==MarkerManager.markers_leader.size()-1){
										MarkerManager.markers_leader.get(i2).setMarkerImage(MarkerManager.images.get(MarkerManager.END_LEADER_MARKER));
										cs.drawBitmap(marker_leader.getMarkerImage().getImage(),
													x42- marker_leader.getMarkerImage().getOffsetX(),
													y42- marker_leader.getMarkerImage().getOffsetY(),paint);
										}
									}
								}
							}
						}
					}
				}
			}
		
		

		if (isScalable) {
			Matrix matr = new Matrix();
			matr.postScale((float) pmap.scaleFactor, (float) pmap.scaleFactor,
					scalePoint.x, scalePoint.y);
			c.drawColor(BitmapUtils.BACKGROUND_COLOR);
			c.drawBitmap(cb, matr, paint);
		}
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		new Thread() {
			@Override
			public void run() {
				while (isNew) {
					try {
						Thread.sleep(200);
						postInvalidate();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

		}.start();
	}

	/**
	 * Панель, на которую выводится карта
	 * 
	 * @author hudvin
	 * 
	 */
	class Panel extends View {
		Paint paint;

		public Panel(Context context) {
			super(context);
			paint = new Paint();

		}

		@Override
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);
			
			
			if (BigPlanet.autoDisplayDB && MarkerManager.markers_DB.size()!=0){
				if(BigPlanet.autoDisplayDBforMarker){
					markers_temp.add(MarkerManager.markers.get(0));
				}
				BigPlanet.autoDisplayDBforMarker = false;
				
				if(BigPlanet.autoDisplayDBFirst){
					invokeGoToMyLocation(markers_temp.get(0).place.getLat(), markers_temp.get(0).place.getLon(), 3);
					BigPlanet.autoDisplayDBFirst = false;
				}else{
					boolean check = markers_DBfordisplay();
					
					if (z2<13 && !check){
						z2 = z2+1;	
						//invokeGoToMyLocation(MarkerManager.markers_DB.get(1).place.getLat(), MarkerManager.markers_DB.get(1).place.getLon(), z2);
						invokeGoToMyLocation(markers_temp.get(0).place.getLat(), markers_temp.get(0).place.getLon(), z2);
							
					}
					else{
						int z3 = z2+2;
						invokeGoToMyLocation(markers_temp.get(0).place.getLat(), markers_temp.get(0).place.getLon(), z3);
						BigPlanet.autoDisplayDB = false;
						BigPlanet.autoDisplayDBforMarker = true;
						z2 = 3;
						invokeDoDraw(canvas);
					}
				}
				
			}else{
				if(BigPlanet.autoDisplayDBforMarker && MarkerManager.markers_DB.size()!=0){
					MarkerManager.markers.clear();
					MarkerManager.markers.add(markers_temp.get(0));
					BigPlanet.autoDisplayDBforMarker = false;
				}
				invokeDoDraw(canvas);
			}
		}
		
		void invokeDoDraw(Canvas canvas) {
			int orientation = getResources().getConfiguration().orientation;
			int height = 480;
			if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
				height = 320;
			}
			if (this.getHeight() == height-50) {
				/**
				 * scale map when zooming in/out
				 * for 320x480 (e.g. HTC Dream, HTC Magic, HTC Hero, Samsung i7500)
				 */
				doDraw(canvas, paint, true);
			} else {
				/** 
				 * don't scale map when zooming in/out
				 * because the effect of scaling map will cause wrong map resolution after zooming in/out
				 * for following devices:
				 * 240x320 (e.g. HTC Tattoo)
				 * 480x800(e.g. Acer Liquid, Google Nexus One)
				 * 480x854 (e.g. Motorola Droid)
				 */
				doDraw(canvas, paint, false);
			}
		}
		
		public boolean markers_DBfordisplay(){
			boolean forMarkers = true;
			int num = Math.round(MarkerManager.markers_DB.size()/10)+1;
			int countDB = 0;
			for (int i2=0;i2<num;i2++) {
				for (int i = 0; i < 7; i++) {
					for (int j = 0; j < 7; j++) {
						if ((i > 1 && i < 5) && ((j > 1 && j < 5))) {
							RawTile tile = pmap.getDefaultTile();
							
							int tileX = tile.x + (i - 2);
							int tileY = tile.y + (j - 2);

							if(MarkerManager.markers_DB.get(i2*10).tile.x == tileX && MarkerManager.markers_DB.get(i2*10).tile.y ==tileY && MarkerManager.markers_DB.get(i2*10).tile.z ==z2){
								countDB = countDB +1 ;
							}		
							
							/*if (markers_temp.get(0).tile.x ==tileX && markers_temp.get(0).tile.y ==tileY && markers_temp.get(0).tile.z ==z2){
								forMarkers = true;
							}*/
							
						}
					}
				}
			}
			if (forMarkers && countDB == num){
				return true;
			}else{
				return false;
			}
		}
		

		/**
		 * Обработка касаний
		 */
		@Override
		public boolean onTouchEvent(final MotionEvent event) {
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				pmap.inMove = false;
				pmap.getNextMovePoint().set((int) event.getX(),
						(int) event.getY());
				break;
			case MotionEvent.ACTION_MOVE:
				if(pmap.scaleFactor==1){
					System.out.println("inmove " + pmap.inMove);
					pmap.inMove = true;
					pmap.moveCoordinates(event.getX(), event.getY());
				}
				// for Auto-Follow
				BigPlanet.disabledAutoFollow(MapControl.this.context);
				break;
			case MotionEvent.ACTION_UP:
				if (dcDetector.process(event)) { // double-tap
					if (pmap.scaleFactor == 1) {
						if (mapMode == MapControl.ZOOM_MODE) { // double-tap zoom-in
							// точка, по которой будет производиться
							// центирование
//							System.gc();
//							scalePoint.set((int) event.getX(), (int) event.getY());
//							final float STEP = 0.2f;
//							float sx = (pmap.getWidth() / 2 - event.getX());
//							float sy = (pmap.getHeight() / 2 - event.getY());
//							final float dx = (sx / (1f / STEP));
//							final float dy = (sy / (1f / STEP));
//
//							new Thread() {
//
//								@Override
//								public void run() {
//									float tx = 0;
//									float ty = 0;
//									int scaleX = scalePoint.x;
//									int scaleY = scalePoint.y;
//
//									float ox = pmap.getGlobalOffset().x;
//									float oy = pmap.getGlobalOffset().y;
//
//									float endScaleFactor = pmap.scaleFactor * 2;
//									while (pmap.scaleFactor <= endScaleFactor) {
//										try {
//											Thread.sleep(40);
//											pmap.scaleFactor += STEP;
//
//											tx += dx;
//											ty += dy;
//											scalePoint.set(
//													(int) (Math.round(scaleX + tx)),
//													(int) (Math.round
//															(scaleY + ty)));
//											ox += dx;
//											oy += dy;
//
//											pmap.getGlobalOffset().x = (int) Math
//													.floor(ox);
//											pmap.getGlobalOffset().y = (int) Math
//													.floor(oy);
//											postInvalidate();
//
//										} catch (InterruptedException e) {
//											e.printStackTrace();
//										}
//									}
//
//									try {
//										Thread.sleep(600);
//										pmap.zoomInCenter();
//										h.sendEmptyMessage(0);
//									} catch (InterruptedException e) {
//										e.printStackTrace();
//									}
//
//								}
//
//							}.start();
						} else { // not double-tap zoom-in
							if (onMapLongClickListener != null) {
								onMapLongClickListener.onMapLongClick(0, 0);
							}
						}
					}
				} else { // not double-tap
					if (pmap.inMove) {
						pmap.inMove = false;
						pmap.moveCoordinates(event.getX(), event.getY());
						pmap.quickHack();
						pmap.loadFromCache();
						updateScreen();
						// pmap.reloadTiles();
					}
				}
				break;
			}

			return true;
		}
	}

	public void setMapSource(int sourceId) {
		getPhysicalMap().getTileResolver().setMapSource(sourceId);
		getPhysicalMap().reloadTiles();
		updateScreen();
	}
	public int FindMaxSize(int a, int b, int c,int d){
		return Math.max(Math.max(Math.max(a, b),c),d);		
	}
}
