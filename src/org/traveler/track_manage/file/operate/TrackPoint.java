package org.traveler.track_manage.file.operate;

import java.sql.Timestamp;

import android.util.Log;

public class TrackPoint {

	private double latitude;//緯度
	private double longitude;//經度
	private String time;
	private String elevation;//高度
	private long time_long;
	
	public TrackPoint() {
		// TODO Auto-generated constructor stub
		latitude = 23.5;
		longitude = 121;
		time="0:0:0:0";
		elevation="0";
		time_long=0;
	}
	
	public String getTime(){
		return this.time;
	}
	public void setTime(String time){
		this.time = time;
		
	}
	
	public void computeTimeLongValue(){
		
		String time_filter = this.time.replaceAll("T", " ");
		time_filter = time_filter.replaceAll("Z", "");
		Timestamp stamp = Timestamp.valueOf(time_filter);
		this.time_long = stamp.getTime();
		Log.i("Message","Time long value="+stamp.getTime());
	}
	
	public long getTimeLong(){
		return this.time_long;
	}
	public String getElevation(){
		return this.elevation;
	}
	public void setElevation(String elevation){
		this.elevation = elevation;
	}
	
	public double getLatitude(){
		return this.latitude;
	}
	
	public double getLongitude(){
		return this.longitude;
	}
	
	public void setLatitude(double latitude){
		this.latitude = latitude;
	}
	public void setLongitude(double longitude){
		this.longitude = longitude;
	}
	
	public String toString(){
		return "TrackPoint: latitude="+this.latitude+",longitude="+this.longitude+
		                    ",time="+this.time+",elevation="+this.elevation+"\n";
	}

}
