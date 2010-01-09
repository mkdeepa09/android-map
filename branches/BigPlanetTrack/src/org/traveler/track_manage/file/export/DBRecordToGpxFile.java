package org.traveler.track_manage.file.export;

import java.io.IOException;
import java.util.ArrayList;

import org.traveler.track_manage.file.database.TravelDataBaseAdapter;
import org.traveler.track_manage.file.operate.TrackPoint;
import org.traveler.track_manage.view.TrackListViewActivity;










import android.app.Activity;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class DBRecordToGpxFile {
    private ArrayList<TrackPoint> trackPointList = null;
	private GpxFile gpxFile = null;
    private TravelDataBaseAdapter myTrackDBAdapter = null;
    private Cursor myCursor = null;
    private long trackID=0;
    private Activity myActivity = null;
    private Handler trackListViewHandler=null;
    public static final int EXPORT_SUCCESSFULLY=1;
    public static final int EXPORT_FAIL=0; 
    
	/*
	public DBRecordToGpxFile(){
		try {
			//this.gpxFile = new GpxFile();
			this.trackPointList = new ArrayList<TrackPoint>();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	*/
    
    class myThread extends Thread{
    	
    	String track_name;
		String track_desc;
		String all_track_coordinates;
		String all_track_time;
		String all_track_elevation;
		
	     public void run() {
	    	 
	    	 Message m = null;
	    	 if(myTrackDBAdapter != null && trackID > 0){
	 			Log.i("Message", "Pick NO."+trackID+" DB record to export to the GPX File");
	 			try{
	 				//Retrive the track's attributes from Sqlite
	 				myTrackDBAdapter.open();
	 				myCursor = myTrackDBAdapter.getTrack(trackID);
	 				track_name = myCursor.getString(1);
	 				track_desc = myCursor.getString(2);
	 				all_track_coordinates = myCursor.getString(3);
	 				all_track_time = myCursor.getString(4);
	 				all_track_elevation = myCursor.getString(5);
	 				myTrackDBAdapter.close();
	 				
	 				
	 				Log.i("Message", "track name="+track_name);
	 				Log.i("Message", "track description="+track_desc);
	 				Log.i("Message", "all track coordinates="+all_track_coordinates);
	 				Log.i("Message", "all track time="+all_track_time);
	 				Log.i("Message", "all track elevation="+all_track_elevation);
	 				
	 				// Generate the TrackPointList data structure
	 				trackPointList=ConvertToTrackPointList(all_track_coordinates, all_track_time, all_track_elevation);
	 				
	 				//Need to check the capacity  of the SD-Card before generating the GPX file
	 				
	 				// Generate the GPX File
	 				System.out.println("------------");
	 				Log.i("Message","Generate GPX File...");
	 				gpxFile = new GpxFile(track_name,track_desc);
	 				for(TrackPoint trackPoint: trackPointList)
	 				{
	 					
	 					gpxFile.saveLocation(trackPoint);
	 					
	 				}
	 				gpxFile.closeFile();
	 				Log.i("Message","Finish...the GPX file exists in the directory:/sdcard/Traveler/Export/");
	 				System.out.println("------------");
	 				
	 				//sent message back to TrackListViewAvtivity handler
	 				 String obj = "Successfully!";
	        		 m  = trackListViewHandler.obtainMessage(EXPORT_SUCCESSFULLY, 1, 1, obj);
	 				
	        		 if(trackListViewHandler != null)
	        			 trackListViewHandler.sendMessage(m);
	            	 else
	            		 throw new Error("trackListViewHandler is Null");
	 				
	 				
	 				
	 				
	 			  }
	 			catch(Exception e){
	 				
	 				//sent message back to TrackListViewAvtivity handler
	 				String obj = "Fail!";
	 				m  = trackListViewHandler.obtainMessage(EXPORT_FAIL, 0, 1, obj.toString());
	 				if(trackListViewHandler != null)
	        			 trackListViewHandler.sendMessage(m);
	            	 else
	            		 throw new Error("trackListViewHandler is Null");
	 				e.printStackTrace();
	 				
	 			}
	 			finally{
	 				TrackListViewActivity.myTrackExportDialog.dismiss();
	 			}
	 		}// end of if
	    	 
	    	 else
	    	 {
	    		//sent message back to TrackListViewAvtivity handler
	    		 TrackListViewActivity.myTrackExportDialog.dismiss();
	    		 String obj = "Fail!";
	 				m  = trackListViewHandler.obtainMessage(EXPORT_FAIL, 0, 1, obj.toString());
	 				if(trackListViewHandler != null)
	        			 trackListViewHandler.sendMessage(m);
	            	 else
	            		 throw new Error("trackListViewHandler is Null");
	    		 Log.e("DBRecordToGpxFile", "myTrackDBAdapter == null or trackID <= 0");
	    	 }

	      }// end of run

	  }
	
    public void setHandler(Handler handler){
    	
    	
    	this.trackListViewHandler = handler;
    }
    public void setActivity(Activity activity){
    	
    	this.myActivity = activity;
    }
    public void setDBAdapter(TravelDataBaseAdapter dbAdapter){
		
		this.myTrackDBAdapter=dbAdapter;		
	}
	
	public void setTrackID(long trackID){
		this.trackID = trackID;
	}
	
	public void saveToFile(){
		
		Log.i("Message", "Start GpxFile Export Thread");
		myThread exportThread = new myThread();
		exportThread.setName("Export Thread");
		exportThread.start();
		
	}
	
	private ArrayList<TrackPoint> ConvertToTrackPointList(String all_track_coordinates, String all_track_time, String all_track_elevation)
	{
		
		ArrayList<TrackPoint> myTrackPointList = new ArrayList<TrackPoint>();
		String[] coordinate_array = null;
		String[] time_array = null;
		String[] elevation_array = null;
		//coordinate_array = "11,22;33,44;55,66;".split(";");
		coordinate_array = all_track_coordinates.split(";");
		time_array = all_track_time.split(";");
		elevation_array = all_track_elevation.split(";");
		Log.i("Message", "Dump Coordinate array");
		dump(coordinate_array);
		Log.i("Message", "Dump Time array");
		dump(time_array);
		Log.i("Message", "Dump Elevation array");
		dump(elevation_array);
		
		
		for(int i=0;i<coordinate_array.length;i++)
		{
			
			TrackPoint track_point = new TrackPoint();
			String[] lat_lon = coordinate_array[i].split(",");
			String lat = lat_lon[0];
			String lon = lat_lon[1];
			track_point.setLatitude(Double.parseDouble(lat));
			track_point.setLongitude(Double.parseDouble(lon));
			
			track_point.setTime(time_array[i]);
			track_point.setElevation(elevation_array[i]);
			//System.out.println("------------");
			//Log.i("Message","track_point= "+track_point.toString());
			//System.out.println("------------");
			myTrackPointList.add(track_point);
			
		}
		System.out.println("------------");
		Log.i("Message", "TrackPointList for this Track is=");
		for(TrackPoint point:myTrackPointList){
			
			Log.i("Message",point.toString());
			
			
		}
		System.out.println("------------");
		
		
		return myTrackPointList;
		
	}
	
	private void dump(String[] array)
	{
		    System.out.println("------------");
		    System.out.println("array size="+array.length);
		    for (int i = 0 ; i < array.length ; i++) {
		        System.out.println(array[i]);
		    }
		    System.out.println("------------");
		
		
		
	}
	
	
	
	
}
