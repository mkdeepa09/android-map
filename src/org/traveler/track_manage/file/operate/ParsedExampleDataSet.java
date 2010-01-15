package org.traveler.track_manage.file.operate;

import java.util.ArrayList;

public class ParsedExampleDataSet {

	
	private String trackName;
	private String trackDescription;
	private ArrayList<TrackPoint> trackPointList = null;
	
	private String extractedString; 
    private int extractedInt;
    
    public ParsedExampleDataSet(){
    	this.trackName="my_track";
    	this.trackDescription="no track description";
    	this.extractedInt=0;
    	this.extractedString="ya";
    	this.trackPointList = new ArrayList<TrackPoint>();
    }
    
    public void setTrackName(String trackName){
    	this.trackName = trackName;
    }
    public String getTrackName(){
    	return this.trackName;
    }
    public void setTrackDescription(String trackDescription){
    	this.trackDescription = trackDescription;
    }
    public String getTrackDescription(){
    	return this.trackDescription;
    }
    public void addTrackPoint(TrackPoint trackPoint){
    	this.trackPointList.add(trackPoint);
    }
    public ArrayList<TrackPoint> getTrackPointList(){
    	return this.trackPointList;
    }

    public String getExtractedString() { 
         return extractedString; 
    } 
    public void setExtractedString(String extractedString) { 
         this.extractedString = extractedString;
    } 

    public int getExtractedInt() { 
         return extractedInt; 
    } 
    public void setExtractedInt(int extractedInt) { 
         this.extractedInt = extractedInt; 
    } 
     
    public String toString(){ 
         //return "ExtractedString = " + this.extractedString 
         //          + "\nExtractedInt = " + this.extractedInt;
    	
    	
    	StringBuffer sb = new StringBuffer();
    	if(trackPointList == null | trackPointList.size()==0)
    		return "trackPointList has no trackPoint";
    	else
    	{
    		sb.append("Track name="+this.trackName+",description="+"\n");
    		for(TrackPoint trackPoint:this.getTrackPointList())
    		{
    			sb.append("lat="+trackPoint.getLatitude()+",lon="+trackPoint.getLongitude()+",time="+trackPoint.getTime()+",ele="+trackPoint.getElevation()+"\n");
    		}
    		return sb.toString();
    	}
    }
}
