package org.traveler.track_manage.view;


















import java.sql.Timestamp;
import java.util.ArrayList;

import org.traveler.track_manage.file.database.TravelDataBaseAdapter;
import org.traveler.track_manage.file.export.DBRecordToGpxFile;
import org.traveler.track_manage.file.operate.TrackPoint;

import com.nevilon.bigplanet.BigPlanet;
import com.nevilon.bigplanet.R;
import com.nevilon.bigplanet.core.Place;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class TrackListViewActivity extends ListActivity{
	
	 

	  private static TravelDataBaseAdapter myTrackDBAdapter;
	  private Cursor myCursor;
	  public static ProgressDialog myTrackExportDialog;
	  private static long operatedTrackID;
	  private static Cursor operatedTrackCursor;
	  private static Handler trackListViewHandler;
	  //private static ListView listView;
	  private static View editTrackDialogLayout;
	  //private TextView mTextView01;
	  private static final int EDIT_DIALOG = 0;
	  private static String track_name;
	  private static String track_des;

	  //private ProgressDialog myProgressDialog;
	  private BaseAdapter listViewAdapter;
	  public static String mClickItemedText;
	  public static ArrayList<Place> placeList = null;
	  
	  
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		TrackListViewActivity.operatedTrackID = 0;
		//TrackListViewActivity.listView = this.getListView();
		myTrackDBAdapter = BigPlanet.DBAdapter;
		operatedTrackCursor = null;
		editTrackDialogLayout = null;
		
		
		
		if(myTrackDBAdapter != null)
		{
			Log.i("Message","myTrackDBAdapter != null" );
			//myCursor = this.myTrackHelper.select();
			retrieveAllTracksFromDB();
			 
		}
		else
			
			Log.e("Error","myTrackHelper == null" );
		
		
		
       trackListViewHandler = new Handler(){
			
			
			public void handleMessage(Message msg) {
               
				Log.i("Message", "flag="+msg.what+" indicating for export result "); //1:export成功;0:export失敗
				switch (msg.what)
				{
				  
				   case DBRecordToGpxFile.EXPORT_SUCCESSFULLY:
					   /* Export GpxFile成功*/
					   Log.i("Message","Export Successfully");
					 Toast.makeText(TrackListViewActivity.this, getString(R.string.save_successfully), 
		 	                	Toast.LENGTH_SHORT).show();
						
						
						break;
						
					case DBRecordToGpxFile.EXPORT_FAIL:
						/*Export GpxFile失敗*/
						Log.i("Message","Export Fail");
						Toast.makeText(TrackListViewActivity.this, getString(R.string.save_fail), 
		 	                	Toast.LENGTH_SHORT).show();
						
						
				         break;
				
				
				
				}
				//((Activity)ctx).setTitle((String)msg.obj);
				

         }};
         trackListViewHandler.removeMessages(0);
         
         
         ListView lv = getListView();
         lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override    //AdapterView's Method
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				Log.i( "TAG", "onItemClick id=" + id);
				TrackListViewActivity.operatedTrackID = id;
				
				final ProgressDialog myProgressDialog = ProgressDialog.show(TrackListViewActivity.this,      
						getString(R.string.drawing_track_progressdialog_title), getString(R.string.drawing_track_progressdialog_body), true);
				//Show this track on the map
				
					myTrackDBAdapter.open();
					operatedTrackCursor = myTrackDBAdapter.getTrack(TrackListViewActivity.operatedTrackID);
					final String track_name = operatedTrackCursor.getString(1);
					final String track_coordinates = operatedTrackCursor.getString(3);
					final String track_times = operatedTrackCursor.getString(4);
					final String track_elevations = operatedTrackCursor.getString(5);
					Log.i("Message","track_name="+track_name);
					Log.i("Message","track_coordinates="+track_coordinates);
					myTrackDBAdapter.close();
					
					
					
					new Thread(){
						
						public void run(){
							
							try{
								placeList = ConvertToPlaceList(track_coordinates,track_times,track_elevations);
								Log.i("Message", "placeList has been created.......");
								//sleep(1000);
								//Intent myIntent = new Intent();
						        //myIntent.setClass(TrackListViewActivity.this, BigPlanet.class);
						        //myIntent.putExtra("drawing_mode", "2");
						        Log.i("Message", "calling BigPlanet...........");
						        //startActivity(myIntent);
						        BigPlanet.addMarkersForDrawing(TrackListViewActivity.this, placeList, 2);
						        finish();
							}catch(Exception e)
							{
								e.printStackTrace();
							}
							finally
							{
								myProgressDialog.dismiss();
							}
							
						}
						
						
						
					}.start();
					
					
					
				
			}
		});
                 //AdapterView's Method
 		lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				onLongListItemClick(view,position,id); 
				return false;
			}});
 		
 		
 		

	}
	
	
	protected void onLongListItemClick(View v,int pos,long id) { 
	      Log.i( "TAG", "onItemLongClick id=" + id);
	      LinearLayout lay = (LinearLayout)v;
			TextView textView = (TextView)lay.findViewById(R.id.icontext);
			Log.i("Message", "onItemLongClick TextView selected is="+textView.getText());
			TrackListViewActivity.operatedTrackID = id;
			alert_dialog_selection();
	} 
	
	@Override  //ListActivity's Method
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		super.onListItemClick(l, v, position, id);
		LinearLayout lay = (LinearLayout)v;
		TextView textView = (TextView)lay.findViewById(R.id.icontext);
		Log.i("Message", "onListItemClick TextView selected is="+textView.getText());
		mClickItemedText = textView.getText().toString();
    	Log.i("Message", "onListItemClick mClickItemedText="+mClickItemedText);
        //The entry to put a alert_dialog
		TrackListViewActivity.operatedTrackID = id;
		//alert_dialog_selection();

	}

	public void DisplayTitle(Cursor c)
    {
        Toast.makeText(this, 
                "id: " + c.getString(0) + "\n" +
                "Name: " + c.getString(1) + "\n" +
                "Des: " + c.getString(2) + "\n" +
                "Coordinate:  " + c.getString(3),
                Toast.LENGTH_LONG).show();        
    } 
	
	private void alert_dialog_selection(){
		
		
		new AlertDialog.Builder(TrackListViewActivity.this)
		  .setTitle(getString(R.string.what_do_you_want_to_do))
		  .setItems(R.array.items_track_what_to_do_dialog,
		  new DialogInterface.OnClickListener()
             {

			  		@Override
			  		public void onClick(DialogInterface dialog, int which) {
			  			// TODO Auto-generated method stub
			  			
			  			switch (which)
			  			{	
			  			  
			  				case 0:
			  					//Edit this track
			  					Log.i("Message", "Dialog Item- Track Editing is selected");
			  					showDialog(EDIT_DIALOG);
			  					
			  					//edit_dialog.show();
			  					//TextView edit_view = (TextView) edit_dialog.findViewById(R.id.name_edit_view);
			  					
			  					break;
			  				case 1:
			  					//Delete this track
			  					//String a = getString(R.string.);
			  					Log.i("Message", "Dialog Item- Track Deleting is selected");
			  					new AlertDialog.Builder(TrackListViewActivity.this)
			  					.setMessage(getString(R.string.sure_to_delete_track))
			  					.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener(){

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										// TODO Auto-generated method stub
										
										
										deleteSelectedTrackFromDB();
									}})
								.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener(){

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										// TODO Auto-generated method stub
										
									}})
								.show();
			  					break;
			  					
			  				case 2:
			  					//Save the track into SD-Card
			  					Log.i("Message", "Dialog Item- Track Exporting is selected");
			  					final CharSequence strDialogTitle = getString(R.string.str_export_dialog_title);
								final CharSequence strDialogBody = getString(R.string.str_export_dialog_body);
								// 顯示Progress對話方塊
			  					myTrackExportDialog = ProgressDialog.show
						                 (
						                   TrackListViewActivity.this,
						                   strDialogTitle,
						                   strDialogBody, 
						                   true
						                 );
			  					DBRecordToGpxFile db_to_gpx = new DBRecordToGpxFile();
			  					//db_to_gpx.setActivity(TrackListViewActivity.this);
			  					db_to_gpx.setDBAdapter(TrackListViewActivity.myTrackDBAdapter);
			  					db_to_gpx.setTrackID(TrackListViewActivity.operatedTrackID);
			  					db_to_gpx.setHandler(TrackListViewActivity.trackListViewHandler);
			  					db_to_gpx.saveToFile();
			  					
			  					
			  					
			  			
			  			}//end of switch
				
			  		}
			
             }
		  ).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener()
		  	{ 
			  	@Override 
			  	public void onClick(DialogInterface d, int which)
			  	{ 
			  		d.dismiss(); 
			  	} 
		  	}
		  ).show();
		
	
	     
		
		
		
	}
	private void retrieveAllTracksFromDB(){
		
		
		myTrackDBAdapter.open();
		myCursor = this.myTrackDBAdapter.getAllTracks();
		if(myCursor.moveToFirst()){
			
			//DisplayTitle(myCursor);
			listViewAdapter = new IconAdapter(this,myCursor);
			setListAdapter(listViewAdapter);
		}
		else
		{
			
			setContentView(R.layout.track_browse_layout); 
			Toast.makeText(this, 
					getString(R.string.no_stored_tracks),
	                Toast.LENGTH_SHORT).show(); 
			return;
		}
		
		 
		 myTrackDBAdapter.close();
		
		
		
		
		
	}
	private void deleteSelectedTrackFromDB(){
		
		if(TrackListViewActivity.operatedTrackID > 0 )
		{	
			Log.i("Message","deletedTrackID="+TrackListViewActivity.operatedTrackID);
			try{
				myTrackDBAdapter.open();
				if(myTrackDBAdapter.deleteTrack(TrackListViewActivity.operatedTrackID))
				{
					//myTrackDeleteDialog.dismiss();
					//myCursor.requery();
					//TrackListViewActivity.listView.invalidate();
					
					Toast.makeText(this, getString(R.string.delete_track_successfully), 
							Toast.LENGTH_SHORT).show();
					retrieveAllTracksFromDB();

				}
				else
				{
					//myTrackDeleteDialog.dismiss(); 
					Toast.makeText(this, getString(R.string.delete_track_fail), 
			                	Toast.LENGTH_SHORT).show();            
				}
			
				myTrackDBAdapter.close();
				
			}catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		else
			Log.e("Error", "TrackListViewActivity.deleteTrackID <= 0");
		
		
		
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		// TODO Auto-generated method stub
		 
		//Context mContext = getApplicationContext();
   		//Dialog  dialog = new Dialog(this);
		
		AlertDialog.Builder builder;
		AlertDialog alertDialog;
		//View layout = null;
		builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
		
		Log.i("Message", "TrackListViewActivity.onCreateDialog() is called");
   		Log.i("Message", "Dialog id="+ new Integer(id).toString());
		 switch(id)
		  {
		   	
		    case EDIT_DIALOG:   //Dialog id = 0
		    	//get the Edit_Track_Dialog's Layout
		    	editTrackDialogLayout = inflater.inflate(R.layout.track_edit_layout,(ViewGroup) findViewById(R.id.layout_root));
		    	TextView track_name_text = (TextView) editTrackDialogLayout.findViewById(R.id.name_edit_view);
		    	track_name_text.setTextColor(Color.BLUE);
		    	TextView track_des_text = (TextView) editTrackDialogLayout.findViewById(R.id.des_edit_view);
		    	track_des_text.setTextColor(Color.BLUE);
		    	//TextView track_note_text = (TextView) editTrackDialogLayout.findViewById(R.id.note_edit_view);
		    	//track_note_text.setTextColor(Color.BLUE);
		    	
		    	
		    	myTrackDBAdapter.open();
		    	TrackListViewActivity.operatedTrackCursor = myTrackDBAdapter.getTrack(TrackListViewActivity.operatedTrackID);
		    	track_name = TrackListViewActivity.operatedTrackCursor.getString(1);
		    	track_des =  TrackListViewActivity.operatedTrackCursor.getString(2);
		    	//String track_coordinate = modifiedTrackCursor.getString(3);
		    	//String track_time = modifiedTrackCursor.getString(4);
		    	//String track_elevation = modifiedTrackCursor.getString(5);
		    	Log.i("Message", "Edit Track's ID="+TrackListViewActivity.operatedTrackID+" Edit Track's Name="+track_name);
		    	myTrackDBAdapter.close();
		    	
		    	//layout = inflater.inflate(R.layout.track_edit_layout,(ViewGroup) findViewById(R.id.layout_root));
		    	
		    	
		    	builder.setView(editTrackDialogLayout);
		    	builder.setTitle(getString(R.string.edit_track_dialog_tile)+"\n"+track_name);
		    	builder.setMessage(getString(R.string.edit_track_dialog_body));
		    	builder.setInverseBackgroundForced(true);
		    	
		    	builder.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener(){

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						// Save these string values into Sqlite Database
						//TextView track_name_text = (TextView) layout.findViewById(R.id.name_edit_view);
						//LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
						//inflater.inflate(R.layout.track_edit_layout,(ViewGroup) findViewById(R.id.layout_root));
						//TextEdit track_name_edit = (TextEdit) 
						myTrackDBAdapter.open();
						//long track_id = TrackListViewActivity.operatedTrackCursor.getLong(0);
						//String track_name = TrackListViewActivity.operatedTrackCursor.getString(1);
						EditText track_name_edit = (EditText)editTrackDialogLayout.findViewById(R.id.name_edit_text);
				    	String changed_track_name = track_name_edit.getText().toString();
				    	
				    	/**********filter non-allowed characters out due to the rule of File Name*************/
				    	if(changed_track_name.equals("")){
				    		changed_track_name = track_name;
				    	}
				    	else
				    		changed_track_name = FileterOutForFileNameRule(changed_track_name);
				    	
				    	EditText track_des_edit = (EditText)editTrackDialogLayout.findViewById(R.id.des_edit_text);
				    	String changed_track_des = track_des_edit.getText().toString();
				    	if(changed_track_des.equals("")){
				    		changed_track_des = track_des;
				    	}		    	
				    	
				    	Log.i("Message", "chaneged track name="+changed_track_name);
				    	Log.i("Message", "chaneged track description="+changed_track_des);
						//EditText track_note_edit = (EditText)editTrackDialogLayout.findViewById(R.id.note_edit_text);
				    	//String changed_track_note = track_note_edit.getText().toString();
				    	
						//String track_des = TrackListViewActivity.operatedTrackCursor.getString(2);
						String track_coordinate = TrackListViewActivity.operatedTrackCursor.getString(3);
						String track_time = TrackListViewActivity.operatedTrackCursor.getString(4);
						String track_elevation = TrackListViewActivity.operatedTrackCursor.getString(5);
						
						myTrackDBAdapter.updateTrack(TrackListViewActivity.operatedTrackID, changed_track_name, changed_track_des, track_coordinate, track_time, track_elevation);
						myTrackDBAdapter.close();
						
						Toast.makeText(TrackListViewActivity.this,getString(R.string.edit_successfully), 
			                	Toast.LENGTH_SHORT).show();
						retrieveAllTracksFromDB();
						
					}});
		    	builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener(){

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						dialog.dismiss();
					}});
		    	
		    	
		    	
		   		//dialog.setContentView(R.layout.track_edit_layout);
		   		//dialog.setTitle("�s����u");
		   		break;
		    default:
		    	alertDialog = null;
		  
		  
		  
		  }
		 
		 
		 alertDialog = builder.create();
		 return alertDialog;
		
	}
	
	 @Override
		protected void onPrepareDialog(int id, Dialog dialog) {
			// TODO Auto-generated method stub
		 Log.i("Message", "TrackListViewActivity.onPrepareDialog is called");
		 switch(id)
		  {
		     
		  	case EDIT_DIALOG:
		  		//
		  		myTrackDBAdapter.open();
		    	TrackListViewActivity.operatedTrackCursor = myTrackDBAdapter.getTrack(TrackListViewActivity.operatedTrackID);
		    	String track_name = TrackListViewActivity.operatedTrackCursor.getString(1);
		    	Log.i("Message", "Edit Track's ID="+TrackListViewActivity.operatedTrackID+" Edit Track's Name="+track_name);
		    	myTrackDBAdapter.close();
		    	((AlertDialog)dialog).setTitle(getString(R.string.edit_track_dialog_tile)+"\n"+track_name);
		    	((AlertDialog)dialog).setMessage(getString(R.string.edit_track_dialog_body));
		    	((AlertDialog)dialog).setInverseBackgroundForced(true);
		    	EditText name_text = (EditText)dialog.findViewById(R.id.name_edit_text);
		    	name_text.setText("");
		    	Log.i("Message","clean the name_edit_text");
		    	EditText des_text = (EditText)dialog.findViewById(R.id.des_edit_text);
		    	des_text.setText("");
		    	Log.i("Message","clean the description_edit_text");
		  		break;
		 
		 
		 
		 
		 
		 
		 
		 
		  }
		   
		 
		 
		 
		 
			super.onPrepareDialog(id, dialog);
		}
	 
	 
	 public static String FileterOutForFileNameRule(String trackName)
	 {
		 
		 trackName = trackName.replaceAll("[/:*><|?\"]", "");
		 trackName = trackName.replace('\\', '_');
		 Log.i("Message","the trackName after Filter Out Method="+trackName);
		 return trackName;
		 
	 }
	 
	 private ArrayList<Place> ConvertToPlaceList(String all_track_coordinates, String all_track_time, String all_track_elevation)
	 {
		 ArrayList<Place> myPlaceList = new ArrayList<Place>();
		    String[] coordinate_array = null;
			//String[] time_array = null;
			//String[] elevation_array = null;
			//coordinate_array = "11,22;33,44;55,66;".split(";");
			coordinate_array = all_track_coordinates.split(";");
			//time_array = all_track_time.split(";");
			//elevation_array = all_track_elevation.split(";");
			
			Log.i("Message", "Dump Coordinate array");
			dump(coordinate_array);
			//Log.i("Message", "Dump Time array");
			//dump(time_array);
			//Log.i("Message", "Dump Elevation array");
			//dump(elevation_array);
			
			for(int i=0;i<coordinate_array.length;i++)
			{
				
				//TrackPoint track_point = new TrackPoint();
				Place place = new Place();
				Location location = new Location("NTU Travler");
				String[] lat_lon = coordinate_array[i].split(",");
				String lat = lat_lon[0];
				String lon = lat_lon[1];
				location.setLatitude(Double.parseDouble(lat));
				location.setLongitude(Double.parseDouble(lon));
				place.setLocation(location);
				place.setLat(Double.parseDouble(lat));
				place.setLon(Double.parseDouble(lon));
				myPlaceList.add(place);
				//place.getLocation().setLatitude(Double.parseDouble(lat));
				//place.getLocation().setLongitude(Double.parseDouble(lon));
				//track_point.setLatitude(Double.parseDouble(lat));
				//track_point.setLongitude(Double.parseDouble(lon));
				
				//track_point.setTime(time_array[i]);
				//String time = time_array[i].replaceAll("T", " ");
				//time = time.replaceAll("Z", ".0");
				//Timestamp time_stamp = Timestamp.valueOf(time);
				//Log.i("Message", "Time="+time_array[i]+", Timestamp="+time_stamp+", long="+time_stamp.getTime());
				//Log.i("Message","time_array="+time_array[i]+" time="+time);
				
				
				//place.getLocation().setTime(time)
				//track_point.setElevation(elevation_array[i]);
				
				
				//myTrackPointList.add(track_point);
				
			}
			System.out.println("------------");
			Log.i("Message", "MyPlaceList=");
            for(Place place: myPlaceList){
				
				Log.i("Message","lat="+place.getLocation().getLatitude()+",lon="+place.getLocation().getLongitude());
				
				
			}
            System.out.println("------------");
			return myPlaceList;
		/*	
			System.out.println("------------");
			
			Log.i("Message", "MyPlaceList for this Track is=");
		
			for(Place place: myPlaceList){
				
				Log.i("Message",point.toString());
				
				
			}
			System.out.println("------------");
		 
		 */
		 
		 
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


}
