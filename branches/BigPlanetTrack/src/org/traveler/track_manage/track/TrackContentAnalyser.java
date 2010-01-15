package org.traveler.track_manage.track;

import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.location.Location;
import android.util.Log;

import com.nevilon.bigplanet.core.Place;

public class TrackContentAnalyser {
	
	
	private ArrayList<Place> trackPlaceList = null;
	ArrayList<Double> speedList = null;
	private long consumedTime;
	private float totalDistance;
	private double averageSpeed;
	private double manximumSpeed;
	private long trackPointNumber;
	
	
	public TrackContentAnalyser(){ 
		
		speedList = new ArrayList<Double>();
		consumedTime = 0;
		totalDistance = 0;
		averageSpeed = 0.0;
		manximumSpeed= 0.0;
		trackPointNumber =0;
		
		
	}
	
	
	
	public void analyzeContent(ArrayList<Place> trackPlaceList)
	{
		this.trackPlaceList = trackPlaceList;
		consumedTimeComputing();
		distanceComputing();
		averageSpeedComputing();
		manximumSpeedComputing();
		//Pattern p = Pattern.compile("(\\d{2})-(\\d{2})-(\\d{4})");
		//Matcher m = p.matcher("12-15-2003");
	   /*	
		Pattern p = Pattern.compile("(20\\d{2})-(0[0-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])T([0-1][0-9]|2[0-3]):([0-5][0-9]):([0-5][0-9])Z");
		Matcher m = p.matcher("2009-09-15T05:38:51Z");
		if(m.matches()){   
			Log.i("Message","Matched="+m.group());
			String time_filter = m.group().replaceAll("T", " ");
			time_filter = time_filter.replaceAll("Z", "");
			Timestamp stamp = Timestamp.valueOf(time_filter);
			Log.i("Message","Matched="+m.group()+",after filter T and Z="+time_filter+",long value="+stamp.getTime());
			//T01:56:00Z
        } 
		*/
	}
	
	
	
	private void consumedTimeComputing(){
		
		Log.i("Message","Computing consumedTime....");
		Place firstPlace = this.trackPlaceList.get(0);
		Place lastPlace = this.trackPlaceList.get(this.trackPlaceList.size()-1);
		long firstTimePoint = firstPlace.getLocation().getTime();
		long lastTimePoint = lastPlace.getLocation().getTime();
		Log.i("Message","ConsumedTime long type="+(lastTimePoint-firstTimePoint));
		Time consumedTime = new Time(lastTimePoint-firstTimePoint);
		SimpleDateFormat formatter = new SimpleDateFormat("hh:mm:ss z");
		String time_a = formatter.format(consumedTime);
		//Log.i("Message","time="+consumedTime.toString() );
		Log.i("Message","time="+time_a);
		this.consumedTime = lastTimePoint-firstTimePoint;
		
	}
	
	private void distanceComputing(){
		float totalDistance=0;
		//long count =0;
		Log.i("Message","Computing distance....");
		Log.i("Message","PlaceList's size="+this.trackPlaceList.size());
		
		for(int count =0 ; count<this.trackPlaceList.size();count++)
		{
			 Log.i("Message", "count="+count);
			if(count >= 1)
			{
				 Log.i("Message", "Compute Distance......");
				 Location location = this.trackPlaceList.get(count).getLocation();
				 Location previous_location = this.trackPlaceList.get(count-1).getLocation();
				 float differ = location.distanceTo(previous_location);
				 if(differ<=10)
					  totalDistance = totalDistance + location.distanceTo(previous_location);
				 else
					 totalDistance = totalDistance + 0;
				 Log.i("Message","differDistance="+location.distanceTo(previous_location)+"totalDistance="+totalDistance);
				
			}
			
			
			
			
		}
		Log.i("Message", "Compute Distance ends......");
		this.totalDistance = totalDistance;
		
		
	}
	private void averageSpeedComputing(){
		
		double speed;
		Log.i("Message","Computing averageSpeed....");
		Log.i("Message","PlaceList's size="+this.trackPlaceList.size());
		
		for(int count =0 ; count<this.trackPlaceList.size();count++)
		{
			 Log.i("Message", "count="+count);
			if(count >= 1)
			{
				 Log.i("Message", "Compute Speed......");
				 Location location = this.trackPlaceList.get(count).getLocation();
				 Location previous_location = this.trackPlaceList.get(count-1).getLocation();
				 float distance = location.distanceTo(previous_location);
				 long time = location.getTime()-previous_location.getTime();
				 if(distance == 0 || time == 0)
				 {
					 speed = 0;
				 }
				 else
				 speed = distance / (time/1000);
				 //totalDistance = totalDistance + location.distanceTo(next_location);
				 Log.i("Message","time="+time+"s");
				 Log.i("Message","distance="+distance+"m");
				 Log.i("Message","count="+count+",lat="+location.getLatitude()+",lon="+location.getLongitude()+",speed="+speed+"m/s");
				 Log.i("Message","Add speed="+speed+",into arrayList");
				 speedList.add(speed);
				
			}
			
			
			
			
		}
		
		double totalSpeed = 0.0;
		for(Double speedi: speedList)
		{
			totalSpeed = totalSpeed + speedi;
			
		}
		
		double averageSpeed = totalSpeed / speedList.size();
		Log.i("Message","averageSpeed="+averageSpeed+"m/s");
		Log.i("Message", "Compute averageSpeed ends......");
		this.averageSpeed = averageSpeed;
		
		
		
		
		
		
		
		
	}
	private void manximumSpeedComputing(){
		Log.i("Message","Computing manximumSpeed....");
		Collections.sort(speedList);
		for(Double speed: speedList)
		{
			Log.i("Message","speed="+speed);
			
		}
		Log.i("Message","MinSpeed="+speedList.get(0));
		Log.i("Message","MaxSpeed="+speedList.get(speedList.size()-1));
		this.manximumSpeed = speedList.get(speedList.size()-1);
		
	}
	
	public long getConsumedTime(){
		
		return this.consumedTime;
	}
	
	public float getTotalDistance(){
		return this.totalDistance;
	}
	
	public double getAverageSpeed(){
		return this.averageSpeed;
	}
	
	public double getMaximumSpeed(){
		
		return this.manximumSpeed;
	}
	
	public long getTrackPointNumber(){
		this.trackPointNumber = this.trackPlaceList.size();
		return this.trackPointNumber;
	}
	
	
	
	

}
