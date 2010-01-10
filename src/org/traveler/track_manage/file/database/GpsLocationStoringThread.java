package org.traveler.track_manage.file.database;

import java.sql.Timestamp;
import java.util.ArrayList;

import com.nevilon.bigplanet.BigPlanet;
import com.nevilon.bigplanet.R;

import android.location.Location;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class GpsLocationStoringThread extends Thread {
	private Handler mainThreadHandler;
	private ArrayList<Location> locationList = null;
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
	  try{ 	
		if(this.locationList != null)
		{
			Log.i("Message", "Processing GpsLocationList.......");
		 if(this.locationList.size()>0)	
		  { 	Location location = this.locationList.get(0); //取第一個Location
				coordinates_buff.append(location.getLatitude()+","+location.getLongitude()+";");
				Timestamp timestamp = new Timestamp(location.getTime());
				this.trackName = timestamp.toString();// use the first time as the track's name
				Log.i("Message","trackName="+this.trackName);
				time_buff.append(timestamp.toString()+";");
				elevation_buff.append(location.getAltitude()+";");
			
				//處理第二個之後的Location
				for(int i =1;i<this.locationList.size();i++)
				{
				
					coordinates_buff.append(locationList.get(i).getLatitude()+","+locationList.get(i).getLongitude()+";");
					timestamp = new Timestamp(locationList.get(i).getTime());
					time_buff.append(timestamp.toString()+";");
					elevation_buff.append(locationList.get(i).getAltitude()+";");
				
				
				}
			
				BigPlanet.DBAdapter.open();
				long id = BigPlanet.DBAdapter.insertTrack(this.trackName, "From Gps Location", coordinates_buff.toString(), time_buff.toString(), elevation_buff.toString());
				Log.i("Message", "Insert a new track successfully");
				BigPlanet.DBAdapter.close();
			
				String obj = "Successfully!";
				if(mainThreadHandler != null)
				{
					m  = mainThreadHandler.obtainMessage(SUCCESSFULLY, 1, 1, obj);
		    
           	 		mainThreadHandler.sendMessage(m);
				}
				else
					throw new Error("mainHandler is Null");
		   	}// end of this.locationList.size()>0
		   else
		   {
			    
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
       	    	 throw new Error("mainHandler is Null");
			 Log.e("Error", "LocationList is Null");
		 }
		
		
	  }
	  catch(Exception e){
		  
		    String obj = "MainHandler is Null";
		    if(mainThreadHandler != null)
		    {
		       m  = mainThreadHandler.obtainMessage(FAIL, 2, 1, obj);
		    
         	  mainThreadHandler.sendMessage(m);
		    }
     	    else
     		  throw new Error("mainHandler is Null");
     		  
		    e.printStackTrace();
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
