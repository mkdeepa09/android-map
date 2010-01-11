package com.nevilon.bigplanet.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.util.Log;

import com.nevilon.bigplanet.BigPlanet;
import com.nevilon.bigplanet.R;
import com.nevilon.bigplanet.core.geoutils.GeoUtils;
import com.nevilon.bigplanet.core.geoutils.Point;

public class MarkerManager {

	public static final int MY_LOCATION_MARKER = 0;
	
	public static final int BOOKMARK_MARKER = 1;
	
	public static final int SEARCH_MARKER = 2;
	
	public static final int START_POINT_MARKER = 3;
	
	public static final int END_POINT_MARKER = 4;
	
	public static final int START_MY_TRACK_MARKER = 5;
	
	public static final int END_MY_TRACK_MARKER = 6;
	
	public static final int START_MY_DB_MARKER = 7;
	
	public static final int END_MY_DB_MARKER = 8;
	
	public static final int END_LEADER_MARKER = 9;
	
	//private  HashMap<Integer,MarkerImage> images = new HashMap<Integer,MarkerImage>();
	
	public static  HashMap<Integer,MarkerImage> images = new HashMap<Integer,MarkerImage>();
		
	private List<Marker> markers = new ArrayList<Marker>();
	
	public static List<Marker_G> markers_G = new ArrayList<Marker_G>();
	
	public static List<Marker_G> saveTracks_G = new ArrayList<Marker_G>();
	
	public static List<Marker_G> markers_DB = new ArrayList<Marker_G>();
	
	public static List<Marker_G> markers_leader = new ArrayList<Marker_G>();
	
	private Resources resources;
	
	public MarkerManager(Resources resources){	
		this.resources = resources;
		images.put(MY_LOCATION_MARKER, new MarkerImage(decodeBitmap(R.drawable.person),24,39));
	    images.put(BOOKMARK_MARKER,new MarkerImage(decodeBitmap(R.drawable.bookmark_marker),12,32));
		images.put(SEARCH_MARKER, new MarkerImage(decodeBitmap(R.drawable.location_marker),12,32));	
		images.put(START_POINT_MARKER, new MarkerImage(decodeBitmap(R.drawable.ic_maps_indicator_startpoint),17,36));	
		images.put(END_POINT_MARKER, new MarkerImage(decodeBitmap(R.drawable.ic_maps_indicator_endpoint),17,36));	
		images.put(START_MY_TRACK_MARKER, new MarkerImage(decodeBitmap(R.drawable.ic_maps_indicator_startpoint),17,36));	
		images.put(END_MY_TRACK_MARKER, new MarkerImage(decodeBitmap(R.drawable.ic_maps_indicator_endpoint),17,36));
		images.put(START_MY_DB_MARKER, new MarkerImage(decodeBitmap(R.drawable.ic_maps_indicator_startpoint),17,36));	
		images.put(END_MY_DB_MARKER, new MarkerImage(decodeBitmap(R.drawable.ic_maps_indicator_endpoint),17,36));
		images.put(END_LEADER_MARKER, new MarkerImage(decodeBitmap(R.drawable.ic_maps_indicator_endpoint),17,36));
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
		for(Marker_G marker_G: markers_G){
			Point tileXY = GeoUtils.toTileXY(marker_G.place.getLat(), marker_G.place.getLon(), z);
			Point offsetXY = GeoUtils.getPixelOffsetInTile(marker_G.place.getLat(), marker_G.place.getLon(), z);
			marker_G.offset = offsetXY;
			marker_G.tile.x = (int) tileXY.x;
			marker_G.tile.y = (int) tileXY.y;
			marker_G.tile.z = z;
			// пересчет координат тайла и отступа
		}
		for(Marker_G saveTrack_G: saveTracks_G){
			Point tileXY = GeoUtils.toTileXY(saveTrack_G.place.getLat(), saveTrack_G.place.getLon(), z);
			Point offsetXY = GeoUtils.getPixelOffsetInTile(saveTrack_G.place.getLat(), saveTrack_G.place.getLon(), z);
			saveTrack_G.offset = offsetXY;
			saveTrack_G.tile.x = (int) tileXY.x;
			saveTrack_G.tile.y = (int) tileXY.y;
			saveTrack_G.tile.z = z;
			// пересчет координат тайла и отступа
		}
		for(Marker_G marker_DB: markers_DB){
			Point tileXY = GeoUtils.toTileXY(marker_DB.place.getLat(), marker_DB.place.getLon(), z);
			Point offsetXY = GeoUtils.getPixelOffsetInTile(marker_DB.place.getLat(), marker_DB.place.getLon(), z);
			marker_DB.offset = offsetXY;
			marker_DB.tile.x = (int) tileXY.x;
			marker_DB.tile.y = (int) tileXY.y;
			marker_DB.tile.z = z;
			// пересчет координат тайла и отступа
		}
		for(Marker_G marker_leader: markers_leader){
			Point tileXY = GeoUtils.toTileXY(marker_leader.place.getLat(), marker_leader.place.getLon(), z);
			Point offsetXY = GeoUtils.getPixelOffsetInTile(marker_leader.place.getLat(), marker_leader.place.getLon(), z);
			marker_leader.offset = offsetXY;
			marker_leader.tile.x = (int) tileXY.x;
			marker_leader.tile.y = (int) tileXY.y;
			marker_leader.tile.z = z;
			// пересчет координат тайла и отступа
		}
		
	}
	
	public void addMarker(Place place, int zoom,int trackType , int imageType){
		// trackType : 0 -> search, 1 -> track, 2 -> trackDB, 3 -> trackLeader //
		
		boolean isGPS;
		if (trackType == 0){
			isGPS = false;
		}else{
			isGPS = true;
		}
		Marker marker = new Marker(place, images.get(imageType), isGPS);
		Marker_G marker_G = new Marker_G(place, images.get(imageType), isGPS);
		Marker_G marker_leader = new Marker_G(place, images.get(imageType), isGPS);
		Marker_G marker_DB = new Marker_G(place, images.get(imageType), isGPS);
		updateParams(marker, zoom);
		updateParams(marker_G, zoom);
		updateParams(marker_leader, zoom);
		updateParams(marker_DB, zoom);
		
		if(trackType == 1){
			if(BigPlanet.isGPS_track){
				markers_G.add(marker_G);
			}
			Iterator<Marker> it = markers.iterator();
			while(it.hasNext()){
				Marker m = it.next();
				if(m.isGPS){
					it.remove();
				}
			}
			if(!BigPlanet.isGPS_track ){
				markers.add(marker);
			}
		}
		else if(trackType == 2){
			// clear lastTime DB
			if(BigPlanet.isDBdrawclear){
				markers_DB.clear();
				BigPlanet.isDBdrawclear = false;
			}
			markers_DB.add(marker_DB);

			Log.i("Message","At addMarker, lat="+place.getLocation().getLatitude()+",lon="+place.getLocation().getLongitude());

		}
		else if(trackType == 3){
			markers_leader.add(marker_leader);
		}
	}
	
	public void updateParams(Marker marker, int zoom){
		Point tileXY =  GeoUtils.toTileXY(marker.place.getLat(), marker.place.getLon(), zoom);
		RawTile mTile = new RawTile((int)tileXY.x, (int)tileXY.y, zoom,-1);
		marker.tile = mTile;
		Point offset = GeoUtils.getPixelOffsetInTile(marker.place.getLat(), marker.place.getLon(), zoom);
		marker.offset = offset;
	}
	
	public void updateParams(Marker_G marker_G, int zoom){
		Point tileXY =  GeoUtils.toTileXY(marker_G.place.getLat(), marker_G.place.getLon(), zoom);
		RawTile mTile = new RawTile((int)tileXY.x, (int)tileXY.y, zoom,-1);
		marker_G.tile = mTile;
		Point offset = GeoUtils.getPixelOffsetInTile(marker_G.place.getLat(), marker_G.place.getLon(), zoom);
		marker_G.offset = offset;
	}
	
	public void updateAll(int zoom){
		for(Marker marker : markers){
			updateParams(marker, zoom);
		}
		for(Marker_G marker_G : markers_G){
			updateParams(marker_G, zoom);
		}
		for(Marker_G saveTrack_G : saveTracks_G){
			updateParams(saveTrack_G, zoom);
		}
		for(Marker_G marker_DB : markers_DB){
			updateParams(marker_DB, zoom);
		}
		for(Marker_G marker_leader : markers_leader){
			updateParams(marker_leader, zoom);
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
	
	public List<Marker_G> getMarkers_G_type(int x, int y, int z, int i, int tracktype){
		// tracktype : 1 -> track, 2 -> trackSave, 3 -> trackDB, 4 -> trackLeader //
		List<Marker_G> result_G = new ArrayList<Marker_G>();	
		if(tracktype == 1){
			if(markers_G.get(i).tile.x ==x && markers_G.get(i).tile.y ==y && markers_G.get(i).tile.z ==z){
				Iterator<Marker_G> it = result_G.iterator();
				while(it.hasNext()){
						it.remove();
				}
				result_G.add(markers_G.get(i));
			}	
		}else if(tracktype == 2){
			if(saveTracks_G.get(i).tile.x ==x && saveTracks_G.get(i).tile.y ==y && saveTracks_G.get(i).tile.z ==z){
				Iterator<Marker_G> it = result_G.iterator();
				while(it.hasNext()){
						it.remove();
				}
				result_G.add(saveTracks_G.get(i));
			}
		}else if(tracktype == 3){
			if(markers_DB.get(i).tile.x ==x && markers_DB.get(i).tile.y ==y && markers_DB.get(i).tile.z ==z){
				Iterator<Marker_G> it = result_G.iterator();
				while(it.hasNext()){
						it.remove();
				}
				result_G.add(markers_DB.get(i));
			}
		}else if (tracktype == 4){
			if(markers_leader.get(i).tile.x ==x && markers_leader.get(i).tile.y ==y && markers_leader.get(i).tile.z ==z){
				Iterator<Marker_G> it = result_G.iterator();
				while(it.hasNext()){
						it.remove();
				}
				result_G.add(markers_leader.get(i));
			}
		}
		return result_G;
	}
	
	public void saveMarkerGTrack() {
		saveTracks_G.clear();
		saveTracks_G.addAll(markers_G);
		markers_G.clear();
	}
	
	public boolean clearSaveTracks_G(){
		if(!BigPlanet.isGPS_track){
			saveTracks_G.clear();
			BigPlanet.isGPS_track_save = false;
			return true;
		}else{
			return false;
		}
	}
	
	public void clearMarker_DB(){
		markers_DB.clear();
	}
	
	public void clearLeader(){
		markers_leader.clear();
	}
	
	public static ArrayList<Location> getLocationList(){
		ArrayList<Location> list = new ArrayList<Location>();
		for(int i=0; i<saveTracks_G.size();i++)
		{
			list.add(saveTracks_G.get(i).place.getLocation());
		}
		return list;
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
		
		public Place place;
		
		public RawTile tile;
		
		public Point offset;
		
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
		public void setMarkerImage(MarkerImage markerImage){
			this.markerImage = markerImage;
		}

	}
	public  class Marker_G extends Marker{
		public Marker_G(Place place, MarkerImage markerImage, boolean isGPS){
			super(place,markerImage,isGPS);
		}
		public Point getOffset(){
			return this.offset;
		}
	}
}
