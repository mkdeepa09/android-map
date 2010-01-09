package org.traveler.track_manage.simulate;


import java.util.ArrayList;

import org.traveler.track_manage.file.operate.myParseThread;
import org.traveler.track_manage.view.ExtendedCheckBoxListActivity;
import org.traveler.track_manage.view.TrackListViewActivity;

import com.nevilon.bigplanet.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class GpsTrackStorageSimulatorActivity extends Activity {
	private Button gpsSimu;
	private ArrayList<Location> locationList = new ArrayList<Location>();
	public static ProgressDialog myGPSDialog = null;
	private Handler mainHandler;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.track_storage_simulate);
		Log.i("Message", "At GpsTrackStorageSimulatorActivity");
		
		
		
       mainHandler = new Handler(){
			
			
			public void handleMessage(Message msg) {
               
				Log.i("Message", "flag="+msg.what); //1:全部import成功;0:部分import失敗
				switch (msg.what)
				{
				  
				   case GpsLocationLogParsingThread.SUCCESSFULLY:
					   /*成功*/
					    
						new AlertDialog.Builder(GpsTrackStorageSimulatorActivity.this)
				        .setTitle(getString(R.string.track_importing_result))
				        .setMessage(getString(R.string.track_importing_successfully))
				        .setPositiveButton(getString(R.string.OK),new DialogInterface.OnClickListener()
				            {
				              public void onClick(DialogInterface dialoginterface, int i)
				              {
				            	  
				            	  
				            	  //The entry to call the TrackDisplayViewActivity
				            	  showAllImportedTracks();
				              }
				             }
				            )
				        .show();
						
						break;
						
					case GpsLocationLogParsingThread.FAIL:
						/*有錯*/
				
						new AlertDialog.Builder(GpsTrackStorageSimulatorActivity.this)
				        .setTitle(getString(R.string.track_importing_result))
				        .setMessage(R.string.some_error+(String)msg.obj)
				        .setPositiveButton(getString(R.string.OK),new DialogInterface.OnClickListener()
				            {
				              public void onClick(DialogInterface dialoginterface, int i)
				              {
				            	  Toast.makeText(
				            			  GpsTrackStorageSimulatorActivity.this,
				            		        getString(R.string.some_successful),
				            		        Toast.LENGTH_LONG).show();
				            	//The entry to call the TrackDisplayViewActivity
				            	  //showAllImportedTracks();
				              }
				             }
				            )
				        .show();
						
				         break;
				
				
				
				}
				//((Activity)ctx).setTitle((String)msg.obj);
				

         }};
         mainHandler.removeMessages(0);
         
         
		gpsSimu =(Button) findViewById(R.id.gps_track_storage_simu);
		gpsSimu.setOnClickListener(new Button.OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				final CharSequence strDialogTitle = "正在剖析GPS Log";
				final CharSequence strDialogBody = getString(R.string.str_dialog_body);
		      
				// 顯示Progress對話方塊
				myGPSDialog = ProgressDialog.show
		                 (
		                   GpsTrackStorageSimulatorActivity.this,
		                   strDialogTitle,
		                   strDialogBody, 
		                   true
		                 );
				Log.i("Message","Start GpsLogParsing...");
				GpsLocationLogParsingThread myThread = new GpsLocationLogParsingThread();
				myThread.setMainHandler(mainHandler);
				myThread.start();
				//Log.i("Message", "Retrive TrackName="+myThread.getTrackName());
				//locationList = myThread.getLocationList();
				
			}});
		
		
		
	}
    private void showAllImportedTracks(){
    	
        Intent myIntent = new Intent();
        myIntent.setClass(GpsTrackStorageSimulatorActivity.this, TrackListViewActivity.class);
        Log.i("Message", "calling TrackListViewActivity");
        startActivity(myIntent);
        
        

    	
    	
    }
	

}
