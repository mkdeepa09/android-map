package org.traveler.track_manage.file.operate;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.travel.track_manage.file.database.TravelDataBaseAdapter;
import org.travel.track_manage.file.database.TravelerTrackDataBaseHelper;
import org.traveler.track_manage.view.ExtendedCheckBox;

import com.nevilon.bigplanet.core.storage.SQLLocalStorage;






import android.os.Environment;
import android.os.Handler;
import android.util.Log;

public class ImportFileProcessing {
    
	  //private File fileDir;
	  //private File sdcardDir;
	  private String SDcardPath;
	  //private String TrackPath;
	  public static String GPXFileImportPath;
	  private List<String> items = null;
	  private TravelerTrackDataBaseHelper DBHelper = null;
	  //private TravelDataBaseAdapter DBAdapter = null;
	  //
	  private TravelDataBaseAdapter DBAdapter = null;
	public  ImportFileProcessing(){
		
		
		/* 取得SD Card目錄 */
	    //sdcardDir = Environment.getExternalStorageDirectory();
	    //SDcardPath = sdcardDir.getParent() + sdcardDir.getName();
	    //Log.i("Message", "SDcardPath="+SDcardPath);
	    //TrackPath = "/RMaps/tracks/Import/";
	    //GPXFileImportPath = SDcardPath+TrackPath;
	    GPXFileImportPath = SQLLocalStorage.TRACK_IMPORT_PATH;
	    Log.i("Message", "GPXFilePath="+GPXFileImportPath);
	    items = new ArrayList<String>();
		
	}
	
	public void setTrackDBAdapter(TravelDataBaseAdapter dbAdapter){
		
		//this.DBHelper = dbHelper;
		this.DBAdapter = dbAdapter;
	}
	
	public List<String> findAllGPXFiles(){
		
		this.items = null;
		this.items = new ArrayList<String>(); 
		File file = new File(this.GPXFileImportPath);
		if (!file.exists())
		      file.mkdir();
		File[] files = file.listFiles();
		for (File f : files){
			System.out.println("file"+f.getName());
			if(f.getName().endsWith(".gpx")){
				
				
				this.items.add(f.getName().replace(".gpx", ""));
			}
		}
		
		System.out.println("gpx files found:"+items.toString());
		return items;
	}
	
	public void parseGPXFile(List<ExtendedCheckBox> extendedCheckBoxList, Handler mainHandler){
		
		myParseThread myParseThread = new myParseThread("GPXParseThread",extendedCheckBoxList);
		myParseThread.setMainThreadHandler(mainHandler);
		myParseThread.setGPXFileDirectory(GPXFileImportPath);
		//myParseThread.setTrackDBHelper(DBHelper);
		myParseThread.setTrackDBAdapter(DBAdapter);
		myParseThread.start();
		
	}
	
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ImportFileProcessing fp = new ImportFileProcessing();
		
	}

}
