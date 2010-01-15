package org.traveler.track_manage.simulate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import org.traveler.track_manage.track.TrackContentAnalyser;



import com.nevilon.bigplanet.BigPlanet;
import com.nevilon.bigplanet.core.Place;

import android.location.Location;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class GpsLocationLogParsingThread extends Thread {
    //private int line_count = 1;
	private Handler mainThreadHandler;
    //private ArrayList<Location> locationList = new ArrayList<Location>();
	private ArrayList<Place> placeList = new ArrayList<Place>();
    private String trackName = null;
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd G 'at' hh:mm:ss a zzz");
    Timestamp timestamp;
  
    public static final int SUCCESSFULLY=1;
    public static final int FAIL=0;
    
	@Override
	public void run() {
		// TODO Auto-generated method stub
		//super.run();
		Message m = null;
		String gpsLogFilePath = "/sdcard/RMaps/tracks/import/gps1.log"; // need to create the folder
		File gpsLogFile = new File(gpsLogFilePath);
		FileInputStream file_stream;
		
		//Reader reader;
		try{
			Log.i("Message:","GPS_Log_File_Path="+gpsLogFilePath);
			Log.i("Message:","Parsing Log begins...");
			file_stream = new FileInputStream(gpsLogFile);
			//reader = new InputStreamReader(file_stream,"UTF-8");
			BufferedReader in = new BufferedReader(new InputStreamReader(file_stream,"UTF-8"));
			String str;
			in.readLine();
			
			String[] second_line = in.readLine().split("	");
			
			Location location = new Location("NTU Traveler");
			location.setLongitude(Double.parseDouble(second_line[0]));
			location.setLatitude(Double.parseDouble(second_line[1]));
			location.setAltitude(Double.parseDouble(second_line[2]));
			location.setTime(Long.parseLong(second_line[3]));
			location.setSpeed(Float.parseFloat(second_line[4]));
			location.setBearing(Float.parseFloat(second_line[5]));
			location.setAccuracy(Float.parseFloat(second_line[6]));
			Log.i("Message:","Add the First Location to First Place");
			Place place = new Place();
			place.setLon(Double.parseDouble(second_line[0]));
			place.setLat(Double.parseDouble(second_line[1]));
			place.setLocation(location);
			this.placeList.add(place);
			Log.i("Message:","Add the First Place to placeList");
			
			
			Log.i("Message", "TimeValue="+second_line[3]);
			timestamp = new Timestamp(Long.parseLong(second_line[3]));
			Log.i("Message", "Timestamp="+timestamp.toString());
		    this.trackName = timestamp.toString();
			
		    while ((str = in.readLine()) != null) {
		        process(str);
		        
		        
		    }
		    in.close();
		    Log.i("Message:","Parsing Log finishes...");
		    System.out.println("------------------------");
		    //
		    
		    
		    Log.i("Message:","Analyze Track Content begins...");
		    TrackContentAnalyser trackAnalyser = new TrackContentAnalyser();
		    trackAnalyser.analyzeContent(this.placeList);
		    
		    
		    Log.i("Message:","Saving to DB begins...");
		    StoreGpsLocationListToDB(this.placeList);
		    
		    
		    
		    
		    
		    
		    // send message back to GpsTrackStorageSimulationActivity
		    
		    String obj = "Successfully!";
		    m  = mainThreadHandler.obtainMessage(SUCCESSFULLY, 1, 1, obj);
		    if(mainThreadHandler != null)
           	 mainThreadHandler.sendMessage(m);
       	    else
       		 throw new Error("mainHandler is Null");
		    
		    
		    
		    
		    
			
		}catch(Exception e){
			String obj = "Fail!";
		    m  = mainThreadHandler.obtainMessage(FAIL, 1, 1, obj);
		    if(mainThreadHandler != null)
           	 mainThreadHandler.sendMessage(m);
       	    else
       		 throw new Error("mainHandler is Null");
			e.printStackTrace();
		}
		finally
		{
			GpsTrackStorageSimulatorActivity.myGPSDialog.dismiss();
		}
		
	}
	
	public void setMainHandler(Handler handler){
		
		this.mainThreadHandler = handler;
	}
	
	
	
	
	
	
	/*
	public void SetGpsLocationList(ArrayList<Location> locationList){
		
		this.locationList = locationList;
		
	}
	*/
	
	private void StoreGpsLocationListToDB(ArrayList<Place> placeList)
	{
		
		StringBuffer coordinates_buff = new StringBuffer();
		StringBuffer time_buff = new StringBuffer();
		StringBuffer elevation_buff = new StringBuffer();
		
		for(Place place: placeList)
		{
			Location location = place.getLocation();
			coordinates_buff.append(location.getLatitude()+","+location.getLongitude()+";");
			Timestamp timestamp = new Timestamp(location.getTime());
			time_buff.append(timestamp.toString()+";");
			elevation_buff.append(location.getAltitude()+";");
			
			
		}
		
		TrackContentAnalyser analyser = new TrackContentAnalyser();
    	Log.i("trackName", "Perform TrackContentAnalyser");
    	analyser.analyzeContent(placeList);
    	
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
		//Log.i("Message:","coordinate_buff="+coordinates_buff.toString());
		//System.out.println("------------------------");
		//Log.i("Message:","time_buff="+time_buff.toString());
		//System.out.println("------------------------");
		//Log.i("Message:","elevation_buff="+elevation_buff.toString());
		//System.out.println("------------------------");
		BigPlanet.DBAdapter.open();
		long id = BigPlanet.DBAdapter.insertTrack(this.trackName, "FromGpsLocation", coordinates_buff.toString(), time_buff.toString(), elevation_buff.toString(),
				consumedTime,totalDistance,averageSpeed,manximumSpeed,trackPointNumber,"GPS");
		Log.i("Message", "Insert a new track successfully");
		BigPlanet.DBAdapter.close();
		
		
		
	}
	
	
	
	
	private void process(String line)
	{
		
		String[] element = line.split("	");
		Location location = new Location("NTU Traveler");
		location.setLongitude(Double.parseDouble(element[0]));
		location.setLatitude(Double.parseDouble(element[1]));
		location.setAltitude(Double.parseDouble(element[2]));
		location.setTime(Long.parseLong(element[3]));
		location.setSpeed(Float.parseFloat(element[4]));
		location.setBearing(Float.parseFloat(element[5]));
		location.setAccuracy(Float.parseFloat(element[6]));
		Place place = new Place();
		place.setLon(Double.parseDouble(element[0]));
		place.setLat(Double.parseDouble(element[1]));
		place.setLocation(location);
		placeList.add(place);
		//dump(element);
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
	
	public String getTrackName(){
		
		if(this.trackName != null)
			return this.trackName;
		else
		{
			return null;
		}
	}
	
	public ArrayList<Place> getPlaceList(){
		
		
		return this.placeList;
		
	}
	
	
	
	
}
