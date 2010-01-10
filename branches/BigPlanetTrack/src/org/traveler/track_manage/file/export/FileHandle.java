package org.traveler.track_manage.file.export;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.traveler.track_manage.view.TrackListViewActivity;

import com.nevilon.bigplanet.core.storage.SQLLocalStorage;





import android.location.Location;
import android.util.Log;

public abstract class FileHandle {
	
	private String fileName;
	private String filePatch = SQLLocalStorage.TRACK_EXPORT_PATH;
    private FileWriter fstream;
    private BufferedWriter buffer;
    private File fileIOHandle;
    private FileOutputStream fous;
    private OutputStreamWriter ous;
	
	// Make default file name
    private String mkFileName(){
    	Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        return sdf.format(cal.getTime())+".gpx";
    	
    }
    private void initFile(String fileName) throws IOException{
	    File dirHandle		=	new File(this.filePatch);
	    if(!dirHandle.exists())	dirHandle.mkdirs();
	    
	    /* Modify by Taiyu */
	    
    	//this.fileName 		= 	fileName+".gpx";
	    this.fileName=TrackListViewActivity.mClickItemedText+".gpx";
    	String na = new String (this.fileName.getBytes("UTF-8"),"UTF8");
    	Log.i("Message", "na="+na);
    	Log.i("Message", "output file name="+this.fileName);
		this.fileIOHandle	=	new File(getFullFileName());
		//this.fstream  		= 	new FileWriter(this.fileIOHandle);
		this.fous = new FileOutputStream(this.fileIOHandle);
		this.ous            =   new OutputStreamWriter(this.fous,"UTF-8");
		//this.buffer 		= 	new BufferedWriter(this.fstream);
		this.buffer        = new BufferedWriter(this.ous);
		
    } 

    /* Modify by Taiyu */
    public FileHandle(String fileName) 	throws IOException {this.initFile(fileName); Log.i("Message", "GpxFileName="+fileName);		}
    public FileHandle() 				throws IOException {this.initFile(mkFileName());	}
    public long getFileSize() 				throws IOException {return this.fileIOHandle.length()/1024;	}


	
	public void closeFile() throws IOException{	this.buffer.close(); this.ous.close(); }
	public String getFullFileName(){ return this.filePatch+this.fileName; }
	public String getFileName(){ return this.fileName; }
	public void saveToFile(String data) throws IOException{
		this.buffer.write(data);
		this.buffer.flush();
		
		  
	}

	// Abstracts
    abstract void saveLocation(Location loc) throws IOException;

}
