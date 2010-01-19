package org.traveler.track_manage.file.export;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.traveler.track_manage.file.operate.TrackPoint;



import android.location.Location;
import android.util.Log;


public class GpxFile extends FileHandle {

	private int pointsCount = 0;
    private SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd");		
    private SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss");		
	Calendar cal = Calendar.getInstance();
	

	
	private String gpxFoter =
		"</trkseg>\n"+
		"</trk>\n"+
		"</gpx>\n";

	
	/* Modify by Taiyu */
	GpxFile(String trackName, String trackDescription) throws IOException{ 
		super(trackName);
		String gpxHeader = 
			"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
			"<gpx\n"+
			" version=\"1.0\"\n"+
			"creator=\"NTU Traveler\"\n"+
			"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"+
			"xmlns=\"http://www.topografix.com/GPX/1/0\"\n"+
			"xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd\">\"\n"+
			"<trk>\n"+
			"<name><![CDATA["+trackName+"]]></name>\n"+
			"<desc><![CDATA["+trackDescription+"]]></desc>\n"+
			"<trkseg>\n";
		
		super.saveToFile(gpxHeader);
		
		}
	@Override
	public void closeFile() throws IOException{
		super.saveToFile(this.gpxFoter);
		super.closeFile();
		
	}
	
	public void saveLocation(Location loc) throws IOException {
		
		super.saveToFile(
				"<trkpt lat=\""+loc.getLatitude()+"\" lon=\""+loc.getLongitude()+"\">\n"+
//				"<ele>107.006119</ele>\n"+ 
				"<time>"+this.date.format(this.cal.getTime())+"T"+this.time.format(this.cal.getTime())+"Z</time>\n" +
				"<speed>0.000000</speed>\n"+
				"<name>TP"+(pointsCount++)+"</name>\n"+
				"<fix>none</fix>\n"+
				"</trkpt>\n");
	}
	
	
	/* Add by Taiyu */
	public void saveLocation(TrackPoint trackPoint)throws IOException {
		String myTrackFormatString = conformToMyTrackFormat(trackPoint.getTime());
		super.saveToFile(
				"<trkpt lat=\""+trackPoint.getLatitude()+"\" lon=\""+trackPoint.getLongitude()+"\">\n"+
				"<time>"+myTrackFormatString+"</time>\n"+
				"<ele>"+trackPoint.getElevation()+"</ele>\n"+
				"</trkpt>\n");
	}
	
	private String conformToMyTrackFormat(String timeString)
	{
		
		if(timeString.indexOf(" ")!= -1)
		{   
			Log.i("GpxFile","Find the non-formated time string="+timeString);
			
			timeString = timeString.replaceAll(" ", "T");
			timeString = timeString.replaceAll("(\\.0)", "");
			timeString = timeString + "Z";
			Log.i("GpxFile", "After conformToMyTrackFormat="+timeString);
			
		}
		
		return timeString;
	}

}
