package com.nevilon.bigplanet.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.nevilon.bigplanet.R;
import com.nevilon.bigplanet.core.geoutils.GeoUtils;
import com.nevilon.bigplanet.core.geoutils.Point;

public class MarkerManager {

	public static final int MY_LOCATION_MARKER = 0;
	
	public static final int BOOKMARK_MARKER = 1;
	
	public static final int SEARCH_MARKER = 2;
	
	private  HashMap<Integer,MarkerImage> images = new HashMap<Integer,MarkerImage>(); 
	
	
	private List<Marker> markers = new ArrayList<Marker>();
	
	private Resources resources;
	
	public MarkerManager(Resources resources){	
		this.resources = resources;
		images.put(MY_LOCATION_MARKER, new MarkerImage(decodeBitmap(R.drawable.person),24,39));
	    images.put(BOOKMARK_MARKER,new MarkerImage(decodeBitmap(R.drawable.bookmark_marker),12,32));
		images.put(SEARCH_MARKER, new MarkerImage(decodeBitmap(R.drawable.location_marker),12,32));	
	}
	
	public void clearMarkerManager() {
		markers.clear();
	}
	
	// вызывается при зуммировании, пересчет отступа и координат тайла всех маркеров
	public void updateCoordinates(int z){
		for(Marker marker: markers){
			Point tileXY = GeoUtils.toTileXY(marker.place.getLat(), marker.place.getLon(), z);
			Point offsetXY = GeoUtils.getPixelOffsetInTile(marker.place.getLat(), marker.place.getLon(), z);
			marker.offset = offsetXY;
			marker.tile.x = (int) tileXY.x;
			marker.tile.y = (int) tileXY.y;
			marker.tile.z = z;
			// пересчет координат тайла и отступа
		}
	}
	
	public void addMarker(Place place, int zoom, boolean isGPS, int type){
		Marker marker = new Marker(place, images.get(type), isGPS);
		updateParams(marker, zoom);
		if(isGPS){
			Iterator<Marker> it = markers.iterator();
			while(it.hasNext()){
				Marker m = it.next();
				if(m.isGPS){
					it.remove();
				}
			}
		}
		markers.add(marker);	
	}
	
	public void updateParams(Marker marker, int zoom){
		Point tileXY =  GeoUtils.toTileXY(marker.place.getLat(), marker.place.getLon(), zoom);
		RawTile mTile = new RawTile((int)tileXY.x, (int)tileXY.y, zoom,-1);
		marker.tile = mTile;
		Point offset = GeoUtils.getPixelOffsetInTile(marker.place.getLat(), marker.place.getLon(), zoom);
		marker.offset = offset;
	}
	
	public void updateAll(int zoom){
		for(Marker marker : markers){
			updateParams(marker, zoom);
		}
	}
	
	public List<Marker> getMarkers(int x, int y, int z){
		List<Marker> result = new ArrayList<Marker>();
		for(Marker marker:markers){
			if(marker.tile.x ==x && marker.tile.y ==y && marker.tile.z ==z){
				result.add(marker);
			}
		}
		return result;
	}
	
	private Bitmap decodeBitmap(int resourceId){
		return BitmapFactory.decodeResource(resources, resourceId);
	}
	
	public static class MarkerImage{
		
		private Bitmap image;
		
		private int offsetX;
		
		private int offsetY;
		
		public MarkerImage(Bitmap image, int offsetX, int offsetY){
			this.image = image;
			this.offsetX = offsetX;
			this.offsetY = offsetY;
		}
		
		public Bitmap getImage(){
			return this.image;
		}
		
		public int getOffsetX(){
			return this.offsetX;
		}
		
		public int getOffsetY(){
			return this.offsetY;
		}
		
	}
	
	public  class Marker {
		
		private Place place;
		
		private RawTile tile;
		
		private Point offset;
		
		private boolean isGPS;
		
		private MarkerImage markerImage;
		
		public Marker(Place place, MarkerImage markerImage, boolean isGPS){
			this.place = place;	
			this.isGPS = isGPS;
			this.markerImage = markerImage;
		}
		
		public Point getOffset(){
			return this.offset;
		}
		
		public MarkerImage getMarkerImage(){
			return this.markerImage;
		}

		
	}
	
}
