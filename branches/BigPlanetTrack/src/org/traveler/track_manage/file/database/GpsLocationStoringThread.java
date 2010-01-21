package org.traveler.track_manage.file.database;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.traveler.track_manage.track.TrackContentAnalyser;

import com.nevilon.bigplanet.BigPlanet;
import com.nevilon.bigplanet.R;
import com.nevilon.bigplanet.core.Place;

import android.location.Location;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class GpsLocationStoringThread extends Thread {
	private Handler mainThreadHandler = null;
	private ArrayList<Location> locationList = null;
	private ArrayList<Place> placeList = new ArrayList<Place>();
	private String trackName = null;
	Timestamp timestamp;
	public static final int SUCCESSFULLY=1;
    public static final int FAIL=0;
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		//super.run();
		Message m = null;
		StringBuffer coordinates_buff = new StringBuffer();
		StringBuffer time_buff = new StringBuffer();
		StringBuffer elevation_buff = new StringBuffer();
		if(this.mainThreadHandler == null)
			Log.i("Message", "MainHandler is Null");
		else 
			Log.i("Message", "MainHandler is not Null");
		
	  try{ 	
		if(this.locationList != null)
		{
			Log.i("Message", "Processing GpsLocationList.......");
		 if(this.locationList.size()>1)	
		  { 	
			 
				java.util.Date current=new java.util.Date();
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				this.trackName = formatter.format(current);
				Log.i("Message","trackName="+this.trackName);
				
				for(int i =0;i<this.locationList.size();i++)
				{
				    if(locationList.get(i)==null || locationList.get(i).getLatitude()==0 || locationList.get(i).getLongitude()==0)
				    {
				    	Log.i("GpsLocationStoringThread", "locationList.get(i)==null || Latitude==0 ||Longitude()==0");
				    }
				    else
				    {	
				    	coordinates_buff.append(locationList.get(i).getLatitude()+","+locationList.get(i).getLongitude()+";");
				    	timestamp = new Timestamp(locationList.get(i).getTime());
				    	time_buff.append(timestamp.toString()+";");
				    	elevation_buff.append(locationList.get(i).getAltitude()+";");
				    }
				
				}
				
				
				//create the PlaceList from LocationList
				for(Location my_location: this.locationList){
					
					Place place = new Place();
				    place.setLocation(my_location);
					place.setLat(my_location.getLatitude());
					place.setLon(my_location.getLongitude());
					this.placeList.add(place);
				}
				
				
				TrackContentAnalyser analyser = new TrackContentAnalyser();
            	Log.i("trackName", "Perform TrackContentAnalyser");
            	analyser.analyzeContent(this.placeList);
            	
            	long consumedTime = analyser.getConsumedTime();
            	Time time = new Time(consumedTime);
        		Log.i("Message","ConsumedTime="+time.toString() );
            	
            	float totalDistance = analyser.getTotalDistance();
            	Log.i("Message","totalDistance="+totalDistance+"m");
            	
            	
            	double averageSpeed = analyser.getAverageSpeed();
            	Log.i("Message","averageSpeed="+averageSpeed+"m/s");
            	
            	double manximumSpeed = analyser.getMaximumSpeed();
            	Log.i("Message","manximumSpeed="+manximumSpeed+"m/s");
            	
            	long trackPointNumber = analyser.getTrackPointNumber();
            	Log.i("Message","trackPointNumber="+trackPointNumber);
			
				BigPlanet.DBAdapter.open();
				long id = BigPlanet.DBAdapter.insertTrack(this.trackName, "no track description", coordinates_buff.toString(), time_buff.toString(), elevation_buff.toString(),consumedTime
						,totalDistance,averageSpeed,manximumSpeed,trackPointNumber,"GPS");
				Log.i("Message", "Insert a new track successfully");
				BigPlanet.DBAdapter.close();
			
				String obj = "Successfully!";
				if(mainThreadHandler != null)
				{
					m  = mainThreadHandler.obtainMessage(SUCCESSFULLY, 1, 1, obj);
		    
           	 		mainThreadHandler.sendMessage(m);
           	 	    //throw new Exception("Just Test, Not Thing");
				}
				
				
				else
					throw new Exception("mainHandler is Null");
		   	}// end of this.locationList.size()>0
		   else
		   {
			    
			    String obj = "LocationList size <= 0";
				 if(mainThreadHandler != null)
				 {
					 m  = mainThreadHandler.obtainMessage(FAIL, 0, 1, obj);
			     
			    	 mainThreadHandler.sendMessage(m);
				 }
	       	     else
	       	    	 throw new Exception("mainHandler is Null");
		   }
			
		}// end of 	if(this.locationList != null)
		else
		{
			 String obj = "LocationList is Null";
			 if(mainThreadHandler != null)
			 {
				 m  = mainThreadHandler.obtainMessage(FAIL, 0, 1, obj);
		     
		    	 mainThreadHandler.sendMessage(m);
			 }
       	     else
       	    	 throw new Exception("mainHandler is Null");
			 Log.e("Error", "LocationList is Null");
		 }
		
		
	  }
	  catch(Exception e){
		  Log.e("Error", "Save the error message into the file(/sdcard/RMaps/tracks/error_log/error.txt)");
		    String obj = "Some exceptions occur";
		   Log.i("GpsLocationStoringThread", "Save the error message into the file(/sdcard/RMaps/tracks/error_log/error.txt)");
		   try {
			   File error_file_path = new File("/sdcard/RMaps/tracks/error_log/");
			   if(!error_file_path.exists())	error_file_path.mkdirs();
			   File error_file = new File("/sdcard/RMaps/tracks/error_log/error.txt");
			    
			   //error_file.mkdir();//BufferedWriter out = new BufferedWriter(new FileWriter("/sdcard/RMaps/tracks/error_log/error.txt"));
			    PrintWriter pw = new PrintWriter(new FileWriter(error_file));
			    e.printStackTrace(pw);
			    pw.close(); 
			    //out.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
 
		   e.printStackTrace();
		   
		   if(mainThreadHandler != null)
		    {
		       m  = mainThreadHandler.obtainMessage(FAIL, 2, 1, obj);
		    
        	  mainThreadHandler.sendMessage(m);
		    }
    	    else
    		  throw new Error("mainHandler is Null");
	  }
	  finally
	  {
		  BigPlanet.myGPSDialog.dismiss();
	  }
		
		
	}
	
	public void setLocationList(ArrayList<Location> locationList)
	{
		this.locationList = locationList;
	}
	
    public void setMainHandler(Handler handler){
		
		this.mainThreadHandler = handler;
	}

}
