package org.traveler.track_manage.file.operate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;


import org.traveler.track_manage.file.database.TravelDataBaseAdapter;
import org.traveler.track_manage.file.database.TravelerTrackDataBaseHelper;
import org.traveler.track_manage.track.TrackContentAnalyser;
import org.traveler.track_manage.view.ExtendedCheckBox;
import org.traveler.track_manage.view.ExtendedCheckBoxListActivity;
import org.traveler.track_manage.view.TrackTabViewActivity;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.nevilon.bigplanet.R;
import com.nevilon.bigplanet.core.Place;



import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;





public class myParseThread extends Thread {

    private Handler mainThreadHandler;
    private List<ExtendedCheckBox> fileParsingList;
    private String GPXFilePath;
    private ArrayList<File> failedFileList;
    public static final int ALL_Files_PARSE_SUCCESSFULLY=1;
    public static final int Some_Files_PARSE_FAIL=0;
    public int count;
    private TravelerTrackDataBaseHelper trackDBHelper;
    private TravelDataBaseAdapter trackDBAdapter;
    private static String trackSourceString;
    

	public myParseThread(String threadName,List<ExtendedCheckBox> fileParsingList) {
		super(threadName);
		// TODO Auto-generated constructor stub
		
		this.fileParsingList = fileParsingList;
		mainThreadHandler = null;
		failedFileList = new ArrayList<File>();
		//trackDBHelper = null;
		trackDBAdapter = null;
		count = 0;
	}
	
	public void setTrackSourceString(String source)
	{
		this.trackSourceString = source;
	}
	
	public void setTrackDBAdapter(TravelDataBaseAdapter dbAdapter)
	{
		//this.trackDBHelper = dbHelper;
		this.trackDBAdapter = dbAdapter;
	}
	public  void setMainThreadHandler(Handler mainHandler){
		this.mainThreadHandler = mainHandler;
	}
	
	public void setGPXFileDirectory(String myGPXFilePath){
		
		this.GPXFilePath = myGPXFilePath;
	}
	
	 public void run() {

		 Message m = null;
         try{   
        	 /* Parse all selected tracks*/  
        	 for(ExtendedCheckBox extendedCheckBox:fileParsingList){
        		 
        		 if(extendedCheckBox.getChecked()){
        			 
        			 String GPX_file_path = this.GPXFilePath+File.separator+extendedCheckBox.getText()+".gpx";
        			 Log.i("GPXFileNeededToParsing", this.GPXFilePath+File.separator+extendedCheckBox.getText()+".gpx");
        			 parsingGPXFile(new File(GPX_file_path));
        		 }
        	 }
        	 sleep(1000);
        	 
        	
        	/*transfer the results of file parsing */
           if(this.getFailedFileList().size()>0){
        		 StringBuffer obj = new StringBuffer();
        		 for (File file: this.getFailedFileList())
        		 {
        			 obj.append(file.getName()+" ");
        		 }
        		
        		 m  = mainThreadHandler.obtainMessage(Some_Files_PARSE_FAIL, 0, 1, obj.toString());
        	 }
           else{
        		 String obj = "Successfully!";
        		 m  = mainThreadHandler.obtainMessage(ALL_Files_PARSE_SUCCESSFULLY, 1, 1, obj);
        	 }
        	
           
           
            if(mainThreadHandler != null)
            	 mainThreadHandler.sendMessage(m);
            
        	 else
        		 throw new Error("mainHandler is Null");
        	 /*
        	 if(mainThreadHandler != null)
               mainThreadHandler.sendMessage(m);
            */
         }
         catch(Exception e){
        	 e.printStackTrace();
        	 //Log.i("Message","error:"+e.getMessage());
        	 //String obj = "parsing fail:"+e.getMessage();
        	 // m  = mainThreadHandler.obtainMessage(1, 1, 1, obj);
        	 /*
        	 
              */
         }
         finally{
        	 TrackTabViewActivity.myGPXParseDialog.dismiss();
         }

         

      }
	 
	 private void parsingGPXFile(File GPX_file){
		 
		       		
				FileInputStream file_stream;
				Reader reader;
				
			try {
					file_stream = new FileInputStream(GPX_file);
					reader = new InputStreamReader(file_stream);
			    /*
					count++;
				if(count==2|count==4){
					int len;
					StringBuffer sb = new StringBuffer();
					while((len = reader.read()) != -1)
					  {
					    sb.append((char) len);
					  }
					Log.i("Message:","count="+count);
					Log.i("Message:","stringBuffer"+sb.toString());
				}
				*/
					Log.i("Message:","GPXFile="+GPX_file);
					Log.i("Message:","Parsing XML begins...");
					/* Get a SAXParser from the SAXPArserFactory. */ 
					SAXParserFactory spf = SAXParserFactory.newInstance(); 
					SAXParser sp;
					sp = spf.newSAXParser();
					/* Get the XMLReader of the SAXParser we created. */ 
		            XMLReader xr;
		            xr = sp.getXMLReader();
		            /* Create a new ContentHandler and apply it to the XML-Reader*/ 
		            ExampleHandler myExampleHandler = new ExampleHandler(); 
		            xr.setContentHandler(myExampleHandler);
		            /* Parse the xml-data from our URL. */
		            InputSource inputSource = new InputSource(reader);
		            inputSource.setEncoding("UTF-8");
		            //sp.parse(inputSource, myExampleHandler);
		            xr.parse(inputSource);
		            /* Parsing has finished. */
		            Log.i("Message:","Parsing has finished...");
		            
		          /* view the content of the track
		            Log.i("Message:","The Content of the track");
		            ParsedExampleDataSet parsedExampleDataSet = 
                        myExampleHandler.getParsedData();
		            System.out.println(parsedExampleDataSet.toString());
		          */
		            
		          //The entry to save the track(successful) to sqlite database   
		          /* Save the parsedData into sqlite DB */  
		            ParsedExampleDataSet parsedExampleDataSet = 
                        myExampleHandler.getParsedData();
		            
		            
		            if(this.trackDBAdapter == null)
		            {
		            	Log.e("Error", "trackDBAdapter is null");
		            	throw new Error("trackDBAdapter is null");
		            	
		            }
		            else{
		            	
		            	ArrayList<Place> placeList = new ArrayList<Place>();
		            	String trackName = parsedExampleDataSet.getTrackName();
		            	String trackDes = parsedExampleDataSet.getTrackDescription();
		            	StringBuffer trackCoordinateBuffer = new StringBuffer();
		            	StringBuffer trackTimeBuffer = new StringBuffer();
		            	StringBuffer trackElevationBuffer = new StringBuffer();
		            	
		            	
		            	for(TrackPoint trackPoint: parsedExampleDataSet.getTrackPointList()){
		            		trackCoordinateBuffer.append(trackPoint.getLatitude()+","+trackPoint.getLongitude()+";");
		            		trackTimeBuffer.append(trackPoint.getTime()+";");
		            		trackElevationBuffer.append(trackPoint.getElevation()+";");
		            		
		            		Location location = new Location("NTU Traveler");
		            		location.setLatitude(trackPoint.getLatitude());
		            		location.setLongitude(trackPoint.getLongitude());
		            		location.setTime(trackPoint.getTimeLong());
		            		Place place = new Place();
		            		place.setLat(trackPoint.getLatitude());
		            		place.setLon(trackPoint.getLongitude());
		            		place.setLocation(location);
		            		placeList.add(place);
		            	}
		            	
		            	Log.i("trackName", trackName);
		                Log.i("trackDes", trackDes);
		                Log.i("trackCoordi", trackCoordinateBuffer.toString());
		                Log.i("trackTime", trackTimeBuffer.toString());
		                Log.i("trackEle", trackElevationBuffer.toString());
		            	
		            	TrackContentAnalyser analyser = new TrackContentAnalyser();
		            	Log.i("Message", "Perform TrackContentAnalyser.........................");
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
		            	
		                
		                
		            	Log.i("myParseThread", "Track Source="+trackSourceString);
		                trackDBAdapter.open();
		                long id;
		                id = trackDBAdapter.insertTrack(trackName, trackDes, trackCoordinateBuffer.toString(), trackTimeBuffer.toString(), trackElevationBuffer.toString(),consumedTime,totalDistance,averageSpeed,manximumSpeed,trackPointNumber,
		                		trackSourceString);
		            	//this.trackDBHelper.insert(trackName,trackDes,trackCoordinateBuffer.toString() , trackTimeBuffer.toString(),trackElevationBuffer.toString());
		                //this.trackDBHelper.insert("name","des","coordinate" , "time","elevation");
		                Log.i("Message", "Insert a new track successfully");
		                trackDBAdapter.close();

		            }
		            
		            
				} catch (FileNotFoundException e){
					// TODO Auto-generated catch block
					e.printStackTrace();
					Log.i("error",e.toString());
					Log.i("Message","Add to failedList");
					failedFileList.add(GPX_file);
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Log.i("error",e.toString());
					failedFileList.add(GPX_file);
					Log.i("Message","Add to failedList");
				} catch (ParserConfigurationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Log.i("error",e.toString());
					failedFileList.add(GPX_file);
					Log.i("Message","Add to failedList");
				} catch (SAXException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Log.i("error",e.toString());
					failedFileList.add(GPX_file);
					Log.i("Message","Add to failedList");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Log.i("error",e.toString());
					failedFileList.add(GPX_file);
					Log.i("Message","Add to failedList");
				}
       
	            
	 }
	 
	 public ArrayList<File> getFailedFileList(){
		 
		 return this.failedFileList;
	 }


	
}
