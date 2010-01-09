package org.traveler.track_manage.file.operate;

public class TrackPoint {

	private double latitude;//�n��
	private double longitude;//�g��
	private String time;
	private String elevation;//����
	
	public TrackPoint() {
		// TODO Auto-generated constructor stub
		latitude = 23.5;
		longitude = 121;
		time="0:0:0:0";
		elevation="0";
	}
	
	public String getTime(){
		return this.time;
	}
	public void setTime(String time){
		this.time = time;
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
