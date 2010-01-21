package org.traveler.track_manage.view;


import java.util.ArrayList;
import java.util.List;

import org.traveler.track_manage.file.export.DBRecordToGpxFile;
import org.traveler.track_manage.file.operate.ImportFileProcessing;
import org.traveler.track_manage.file.operate.myParseThread;

import com.nevilon.bigplanet.BigPlanet;
import com.nevilon.bigplanet.R;
import com.nevilon.bigplanet.core.Place;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TabActivity;
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
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TabHost.OnTabChangeListener;

public class TrackTabViewActivity extends TabActivity{
	
	private TabHost mTabHost;
	private RelativeLayout myRelativeLayout;
	//private MyControl myControl;
	private ListView myListView;
	private List<String> gpxFileItems = null;
	public static Context ctx;
	protected static CheckBox selectAllCheckBox;
	private LayoutInflater mlin; //to get the Context's layout
	private ExtendedCheckBoxListAdapter mListAdapter;
	private IconAdapter listViewAdapter;
	private ImportFileProcessing fp;
	private Cursor trackDBCursor;
	private Handler mainHandler;
	private List<String> duplicateTrackNameList = null;
	public static ProgressDialog myGPXParseDialog = null;
	
	private static long operatedTrackID;
	private static Cursor operatedTrackCursor;
	public static ArrayList<Place> placeList = null;
	private static final int EDIT_DIALOG = 0;
	private static final int GET_INFORMATION_DIALOG = 1;
	public static ProgressDialog myTrackExportDialog;
	private static Handler trackListViewHandler;
	private static View editTrackDialogLayout;
	private static View getMoreInformationDialogLayout;
	private static String track_name;
	private static String track_des;
	private static long track_consumedTime;
	private static float track_totalDistance;
	private static double track_averageSpeed;
	private static double track_manximumSpeed;
	private static long track_PointNumber;
	
	
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ctx=this;
        setContentView(R.layout.tab_layout);
        mlin=LayoutInflater.from(ctx);
        mlin.inflate(R.layout.tab_layout,null);
        
        mainHandler = new Handler(){
			
			
			public void handleMessage(Message msg) {
               
				Log.i("Message", "flag="+msg.what); //1:ALL import successfully; 0:PART import fail
				switch (msg.what)
				{
				  
				   case myParseThread.ALL_Files_PARSE_SUCCESSFULLY:
					   /* ALL parse successfully*/
					    
						new AlertDialog.Builder(TrackTabViewActivity.this)
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
						
					case myParseThread.Some_Files_PARSE_FAIL:
						/*PART parse fail*/
				        final String fail_file_list = (String)msg.obj;
						new AlertDialog.Builder(TrackTabViewActivity.this)
				        .setTitle(getString(R.string.track_importing_result))
				        .setMessage(getString(R.string.some_error)+" "+(String)msg.obj)
				        .setPositiveButton(getString(R.string.OK),new DialogInterface.OnClickListener()
				            {
				              public void onClick(DialogInterface dialoginterface, int i)
				              {
				            	  Toast.makeText(
				            			    TrackTabViewActivity.this,
				            		        getString(R.string.import_fail)+":"+fail_file_list,
				            		        Toast.LENGTH_LONG).show();
				            	//The entry to call the TrackDisplayViewActivity
				            	  showAllImportedTracks();
				              }
				             }
				            )
				        .show();
						
				         break;
				
				
				
				}
				//((Activity)ctx).setTitle((String)msg.obj);
				

         }};
        mainHandler.removeMessages(0);
        //------------------------------------------------------------------------------------------------------------------------------
       trackListViewHandler = new Handler(){
			
			
			public void handleMessage(Message msg) {
               
				Log.i("Message", "flag="+msg.what+" indicating for export result "); //1:export成功;0:export失敗
				switch (msg.what)
				{
				  
				   case DBRecordToGpxFile.EXPORT_SUCCESSFULLY:
					   /* Export GpxFile Successfully*/
					   Log.i("Message","Export Successfully");
					 Toast.makeText(TrackTabViewActivity.this, getString(R.string.save_successfully), 
		 	                	Toast.LENGTH_SHORT).show();
						
						
						break;
						
					case DBRecordToGpxFile.EXPORT_FAIL:
						/*Export GpxFile Fail*/
						Log.i("Message","Export Fail");
						Toast.makeText(TrackTabViewActivity.this, getString(R.string.save_fail), 
		 	                	Toast.LENGTH_SHORT).show();
						
						
				         break;
				
				
				
				}
				//((Activity)ctx).setTitle((String)msg.obj);
				

         }};
         trackListViewHandler.removeMessages(0);
        
        //-----------------------------------------------------------------------------------------------------------------------
        createImportTrackListView();
        createBrosweTrackListView();
    
		
        mTabHost = getTabHost();
        mTabHost.setOnTabChangedListener(new OnTabChangeListener(){

			@Override
			public void onTabChanged(String tabId) {
				// TODO Auto-generated method stub
				Log.i("Message", "Tab's id="+tabId);
				if (tabId == "browse_tab") {
					
					createBrosweTrackListView();
				}
				
			}});
        mTabHost.addTab(mTabHost.newTabSpec("import_tab").setIndicator(getString(R.string.IMPORT_TRACK_MENU),getResources().getDrawable(R.drawable.track_import_icon)).setContent(R.id.myRelativeLayout));
        mTabHost.addTab(mTabHost.newTabSpec("browse_tab").setIndicator(getString(R.string.BROWSE_TRACK_MENU),getResources().getDrawable(R.drawable.track_broswe_icon)).setContent(R.id.broswe_linear_layout));    
        //mTabHost.addTab(mTabHost.newTabSpec("tab_test3").setIndicator("TAB 3").setContent(R.id.textview3));
        if(checkIfDBHasTracks())
        {
        	mTabHost.setCurrentTab(1);
        }
        else
        {
        	mTabHost.setCurrentTab(0);
        }
        
        TabWidget tw = getTabWidget(); 
        for (int i=0; i<tw.getChildCount(); i++) { 
            RelativeLayout relLayout = (RelativeLayout)tw.getChildAt(i); 
            TextView tv = (TextView)relLayout.getChildAt(1);// index 0: Tab's text
            ImageView im = (ImageView)relLayout.getChildAt(0);// index 1:Tab's image
            im.setPadding(0, 0, 0, 10);
            tv.setTextSize(12.0f);

        } 
        

		
       
		//View convertView=mlin.inflate(R.layout.tab_layout, null);
		//myRelativeLayout = new RelativeLayout(this);
		//myRelativeLayout=(RelativeLayout)convertView.findViewById(R.id.myRelativeLayout);
		
		
		
        
        
    }
    
    private boolean checkIfDBHasTracks(){
    	
    	BigPlanet.DBAdapter.open();
		Cursor myCursor = BigPlanet.DBAdapter.getAllTracks();
		if(myCursor.moveToFirst()){
			
			Log.i("Message", "DB has track records");
			BigPlanet.DBAdapter.close();
			return true;
			
		}
		else
		{
			Log.i("Message", "DB has no track records");
			BigPlanet.DBAdapter.close();
			return false;
		}
		
    }
   
    
    @Override
	protected Dialog onCreateDialog(int id) {
		// TODO Auto-generated method stub
		//return super.onCreateDialog(id);
    	AlertDialog.Builder builder;
		AlertDialog alertDialog;
		//View layout = null;
		builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
		
		Log.i("Message", "TrackTabViewActivity.onCreateDialog() is called");
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
		    	
		    	
		    	BigPlanet.DBAdapter.open();
		    	TrackTabViewActivity.operatedTrackCursor = BigPlanet.DBAdapter.getTrack(TrackTabViewActivity.operatedTrackID);
		    	track_name = TrackTabViewActivity.operatedTrackCursor.getString(1);
		    	track_des =  TrackTabViewActivity.operatedTrackCursor.getString(2);
		    	//String track_coordinate = modifiedTrackCursor.getString(3);
		    	//String track_time = modifiedTrackCursor.getString(4);
		    	//String track_elevation = modifiedTrackCursor.getString(5);
		    	Log.i("Message", "Edit Track's ID="+TrackTabViewActivity.operatedTrackID+" Edit Track's Name="+track_name);
		    	BigPlanet.DBAdapter.close();
		    	
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
						BigPlanet.DBAdapter.open();
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
						String track_coordinate = TrackTabViewActivity.operatedTrackCursor.getString(3);
						String track_time = TrackTabViewActivity.operatedTrackCursor.getString(4);
						String track_elevation = TrackTabViewActivity.operatedTrackCursor.getString(5);
						long track_consumedTime = TrackTabViewActivity.operatedTrackCursor.getLong(6);
						float track_totalDistance = TrackTabViewActivity.operatedTrackCursor.getFloat(7);
						double track_averageSpeed = TrackTabViewActivity.operatedTrackCursor.getDouble(8);
						double track_manximumSpeed = TrackTabViewActivity.operatedTrackCursor.getDouble(9);
						long track_PointNumber = TrackTabViewActivity.operatedTrackCursor.getLong(10);
						String track_source = TrackTabViewActivity.operatedTrackCursor.getString(11);
						
						
						BigPlanet.DBAdapter.updateTrack(TrackTabViewActivity.operatedTrackID, changed_track_name, changed_track_des, track_coordinate, track_time, track_elevation
								,track_consumedTime,track_totalDistance,track_averageSpeed,track_manximumSpeed,track_PointNumber,track_source);
						BigPlanet.DBAdapter.close();
						
						Toast.makeText(TrackTabViewActivity.this,getString(R.string.edit_successfully), 
			                	Toast.LENGTH_SHORT).show();
						//retrieveAllTracksFromDB();
						createBrosweTrackListView();
						
					}});
		    	builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener(){

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						dialog.dismiss();
					}});
		    	
		    	
		    	
		   		//dialog.setContentView(R.layout.track_edit_layout);
		   		
		   		break;
		    case GET_INFORMATION_DIALOG:
		    	getMoreInformationDialogLayout = inflater.inflate(R.layout.track_get_information_layout,(ViewGroup) findViewById(R.id.ScrollView01));
		    	TextView track_title_text = (TextView) getMoreInformationDialogLayout.findViewById(R.id.track_title_content_view);
		    	TextView track_distance_text = (TextView) getMoreInformationDialogLayout.findViewById(R.id.track_distance_content_view);
		    	TextView track_time_text = (TextView) getMoreInformationDialogLayout.findViewById(R.id.track_time_content_view);
		    	TextView track_speed_text = (TextView) getMoreInformationDialogLayout.findViewById(R.id.track_speed_content_view);
		    	TextView track_maxSpeed_text = (TextView) getMoreInformationDialogLayout.findViewById(R.id.track_maxSpeed_content_view);
		    	TextView track_pointNumber_text = (TextView) getMoreInformationDialogLayout.findViewById(R.id.track_pointNumber_content_view);
		    	TextView track_description_text = (TextView) getMoreInformationDialogLayout.findViewById(R.id.track_description_content_view);
		    	
		    	BigPlanet.DBAdapter.open();
		    	TrackTabViewActivity.operatedTrackCursor = BigPlanet.DBAdapter.getTrack(TrackTabViewActivity.operatedTrackID);
		    	track_name = TrackTabViewActivity.operatedTrackCursor.getString(1);
		    	track_des =  TrackTabViewActivity.operatedTrackCursor.getString(2);
		    	track_consumedTime = TrackTabViewActivity.operatedTrackCursor.getLong(6);
		    	String track_consumedTime_string = IconAdapter.generateTimeString(track_consumedTime,this);
		    	track_totalDistance = TrackTabViewActivity.operatedTrackCursor.getFloat(7);
		    	String track_totalDistance_string = IconAdapter.generateDistanceString(track_totalDistance, this);
		    	track_averageSpeed = TrackTabViewActivity.operatedTrackCursor.getDouble(8);
		    	String track_averageSpeed_string = IconAdapter.generateSpeedString(track_averageSpeed,this);
		    	track_manximumSpeed = TrackTabViewActivity.operatedTrackCursor.getDouble(9);
		    	String track_manximumSpeed_string = IconAdapter.generateSpeedString(track_manximumSpeed,this);
		    	track_PointNumber = TrackTabViewActivity.operatedTrackCursor.getLong(10);
		    	String track_PointNumber_string = Long.toString(track_PointNumber);
		    	Log.i("Message", "get inforamtion Track's ID="+TrackTabViewActivity.operatedTrackID+" Track's Name="+track_name);
		    	BigPlanet.DBAdapter.close();
		    	
		    	track_title_text.setText(track_name);
		    	track_title_text.setTextColor(Color.BLACK);
		    	track_distance_text.setText(track_totalDistance_string);
		    	track_distance_text.setTextColor(Color.BLACK);
		    	track_time_text.setText(track_consumedTime_string);
		    	track_time_text.setTextColor(Color.BLACK);
		    	track_speed_text.setText(track_averageSpeed_string);
		    	track_speed_text.setTextColor(Color.BLACK);
		    	track_maxSpeed_text.setText(track_manximumSpeed_string);
		    	track_maxSpeed_text.setTextColor(Color.BLACK);
		    	track_pointNumber_text.setText(track_PointNumber_string);
		    	track_pointNumber_text.setTextColor(Color.BLACK);
		    	track_description_text.setText(IconAdapter.generateDescriptionString(track_des,this));
		    	track_description_text.setTextColor(Color.BLACK);
		    	
		    	builder.setView(getMoreInformationDialogLayout);
		    	builder.setTitle(getString(R.string.whole_track_information_dialog_title));
		    	builder.setMessage(getString(R.string.whole_track_information_dialog_body));
		    	builder.setInverseBackgroundForced(true);
		    	
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
		//super.onPrepareDialog(id, dialog);
		Log.i("Message", "TrackListViewActivity.onPrepareDialog is called");
		 switch(id)
		  {
		     
		  	case EDIT_DIALOG:
		  		//
		  		BigPlanet.DBAdapter.open();
		  		TrackTabViewActivity.operatedTrackCursor = BigPlanet.DBAdapter.getTrack(TrackTabViewActivity.operatedTrackID);
		    	String track_name = TrackTabViewActivity.operatedTrackCursor.getString(1);
		    	String track_desc = TrackTabViewActivity.operatedTrackCursor.getString(2);
		    	Log.i("Message", "Edit Track's ID="+TrackTabViewActivity.operatedTrackID+" Edit Track's Name="+track_name);
		    	BigPlanet.DBAdapter.close();
		    	((AlertDialog)dialog).setTitle(getString(R.string.edit_track_dialog_tile)+"\n"+track_name);
		    	((AlertDialog)dialog).setMessage(getString(R.string.edit_track_dialog_body));
		    	((AlertDialog)dialog).setInverseBackgroundForced(true);
		    	EditText name_text = (EditText)dialog.findViewById(R.id.name_edit_text);
		    	name_text.setText(track_name);
		    	Log.i("Message","clean the name_edit_text");
		    	EditText des_text = (EditText)dialog.findViewById(R.id.des_edit_text);
		    	des_text.setText(track_desc);
		    	Log.i("Message","clean the description_edit_text");
		  		break;
		  		
		  	case GET_INFORMATION_DIALOG:
		  		
		  		BigPlanet.DBAdapter.open();
		    	TrackTabViewActivity.operatedTrackCursor = BigPlanet.DBAdapter.getTrack(TrackTabViewActivity.operatedTrackID);
		    	this.track_name = TrackTabViewActivity.operatedTrackCursor.getString(1);
		    	track_des =  TrackTabViewActivity.operatedTrackCursor.getString(2);
		    	track_consumedTime = TrackTabViewActivity.operatedTrackCursor.getLong(6);
		    	String track_consumedTime_string = IconAdapter.generateTimeString(track_consumedTime,this);
		    	track_totalDistance = TrackTabViewActivity.operatedTrackCursor.getFloat(7);
		    	String track_totalDistance_string = IconAdapter.generateDistanceString(track_totalDistance, this);
		    	track_averageSpeed = TrackTabViewActivity.operatedTrackCursor.getDouble(8);
		    	String track_averageSpeed_string = IconAdapter.generateSpeedString(track_averageSpeed,this);
		    	track_manximumSpeed = TrackTabViewActivity.operatedTrackCursor.getDouble(9);
		    	String track_manximumSpeed_string = IconAdapter.generateSpeedString(track_manximumSpeed,this);
		    	track_PointNumber = TrackTabViewActivity.operatedTrackCursor.getLong(10);
		    	String track_PointNumber_string = Long.toString(track_PointNumber);
		    	Log.i("Message", "get inforamtion Track's ID="+TrackTabViewActivity.operatedTrackID+" Track's Name="+this.track_name);
		    	BigPlanet.DBAdapter.close();
		  		
		    	TextView track_title_text = (TextView) dialog.findViewById(R.id.track_title_content_view);
		    	TextView track_distance_text = (TextView) dialog.findViewById(R.id.track_distance_content_view);
		    	TextView track_time_text = (TextView) dialog.findViewById(R.id.track_time_content_view);
		    	TextView track_speed_text = (TextView) dialog.findViewById(R.id.track_speed_content_view);
		    	TextView track_maxSpeed_text = (TextView) dialog.findViewById(R.id.track_maxSpeed_content_view);
		    	TextView track_pointNumber_text = (TextView) dialog.findViewById(R.id.track_pointNumber_content_view);
		    	TextView track_description_text = (TextView) dialog.findViewById(R.id.track_description_content_view);
		    	
		    	track_title_text.setText(this.track_name);
		    	track_title_text.setTextColor(Color.BLACK);
		    	track_distance_text.setText(track_totalDistance_string);
		    	track_distance_text.setTextColor(Color.BLACK);
		    	track_time_text.setText(track_consumedTime_string);
		    	track_time_text.setTextColor(Color.BLACK);
		    	track_speed_text.setText(track_averageSpeed_string);
		    	track_speed_text.setTextColor(Color.BLACK);
		    	track_maxSpeed_text.setText(track_manximumSpeed_string);
		    	track_maxSpeed_text.setTextColor(Color.BLACK);
		    	track_pointNumber_text.setText(track_PointNumber_string);
		    	track_pointNumber_text.setTextColor(Color.BLACK);
		    	track_description_text.setText(IconAdapter.generateDescriptionString(track_des,this));
		    	track_description_text.setTextColor(Color.BLACK);
		    	
		    	((AlertDialog)dialog).setTitle(getString(R.string.whole_track_information_dialog_title));
		    	((AlertDialog)dialog).setMessage(getString(R.string.whole_track_information_dialog_body));
		    	((AlertDialog)dialog).setInverseBackgroundForced(true);
		  		
		  		
		  		break;
		 
		  }
		   	 
			super.onPrepareDialog(id, dialog);
	}




	private void createImportTrackListView()
    {
    	WindowManager wm = this.getWindowManager();
		Display display = wm.getDefaultDisplay();
		int height = display.getHeight();
		int width = display.getWidth();
		
		fp = new ImportFileProcessing();
		fp.setTrackDBAdapter(BigPlanet.DBAdapter);
		
		myRelativeLayout = (RelativeLayout) findViewById(R.id.myRelativeLayout);
		TextView myImportTextView = (TextView) findViewById(R.id.import_empty);
		/* New ListView */
        myListView = new ListView(this);
        ViewGroup.LayoutParams param2 = new ViewGroup.LayoutParams(width,height-100);
        /* Add ListView INTO myLinearLayout */
        myRelativeLayout.addView(myListView,0, param2);
        
        /* New SelectAllCheckBox */
        selectAllCheckBox = new CheckBox(this);
        selectAllCheckBox.setEnabled(false);
        selectAllCheckBox.setWidth(100);
        selectAllCheckBox.setHint(getString(R.string.select_all));
        selectAllCheckBox.setHintTextColor(Color.BLACK);
        
        /*Set CheckBox False*/
        selectAllCheckBox.setChecked(false);
        selectAllCheckBox.setOnClickListener(new CheckBox.OnClickListener()
        {
          @Override
          public void onClick(View v)
          {
            // TODO Auto-generated method stub
            if(selectAllCheckBox.isChecked())
            {
            	for(ExtendedCheckBox extendedCheckBox :mListAdapter.getListItems())
                {
            		//ExtendedCheckBox extendedCheckBox = (ExtendedCheckBox)mListAdapter.getItem(i);
                	System.out.println("selected gpx File="+extendedCheckBox.getText());
                	extendedCheckBox.setChecked(true); 	
                }
            	ResetContentView();
            }
            else
            {
            	for(ExtendedCheckBox extendedCheckBox :mListAdapter.getListItems())
                {
            		//ExtendedCheckBox extendedCheckBox = (ExtendedCheckBox)mListAdapter.getItem(i);
                	System.out.println("Unselected gpx File="+extendedCheckBox.getText());
                	extendedCheckBox.setChecked(false);
                } 
            	ResetContentView();
            }
          }
        });
        
        
        
        /*-----------------------------------------------------------------------------------------------------------*/
        /* 新增ImportTracks Button */
        Button bt = new Button(this);
        bt.setEnabled(false);
		bt.setWidth(100);
		//bt.setHeight(80);
		bt.setText(getString(R.string.import_track));
		
		bt.setOnClickListener(new Button.OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				boolean select_at_least_one_track = false;
				for(ExtendedCheckBox checkBox :mListAdapter.getListItems()){
					if(checkBox.getChecked())
					{
						select_at_least_one_track = true;
					}
					
				}
				if(select_at_least_one_track)
				{	
					
					boolean isDuplicateTrackInDB = ChechDuplicateTrackInDB(mListAdapter.getListItems());
					if(isDuplicateTrackInDB){
						
						
						AlertDialog.Builder builder;
						AlertDialog alertDialog;
						builder = new AlertDialog.Builder(TrackTabViewActivity.this);
						builder.setTitle(getString(R.string.find_exised_track_dialog_title));
						builder.setMessage(getString(R.string.find_exised_track_dialog_message)+duplicateTrackNameList.toString());
						builder.setNegativeButton(getString(R.string.cancel),new DialogInterface.OnClickListener(){

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								// TODO Auto-generated method stub
								return;
								
							}});
						builder.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener(){

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								
								// TODO Auto-generated method stub
								final CharSequence strDialogTitle = getString(R.string.str_dialog_title);
								final CharSequence strDialogBody = getString(R.string.str_dialog_body);
						      
								// show ProgressDialog
								myGPXParseDialog = ProgressDialog.show
						                 (
						                   TrackTabViewActivity.this,
						                   strDialogTitle,
						                   strDialogBody, 
						                   true
						                 );
						      
						     
									fp.parseGPXFile(mListAdapter.getListItems(),mainHandler,"File");
								
							}});
						builder.create().show();
						
						
						
						
					}
					else
					{
						
						final CharSequence strDialogTitle = getString(R.string.str_dialog_title);
						final CharSequence strDialogBody = getString(R.string.str_dialog_body);
				      
						// show ProgressDialog
						myGPXParseDialog = ProgressDialog.show
				                 (
				                   TrackTabViewActivity.this,
				                   strDialogTitle,
				                   strDialogBody, 
				                   true
				                 );
				      
				     
							fp.parseGPXFile(mListAdapter.getListItems(),getMainHandler(),"File");
							
					}
					
					
				
				}//end of if(select_at_least_one_track)
				else
					Toast.makeText(
							TrackTabViewActivity.this,
            		        getString(R.string.have_yet_select_track_message),
            		        Toast.LENGTH_SHORT).show();
			
			}
			
		});
		
		
		/*---------------------------------------------------------------------------------------------------------------*/
		RelativeLayout.LayoutParams lp1 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		lp1.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		LinearLayout myLinearLayout = new LinearLayout(this);
		LinearLayout.LayoutParams li1 = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.FILL_PARENT);
		myLinearLayout.setHorizontalGravity(Gravity.RIGHT);
		myLinearLayout.addView(selectAllCheckBox,li1);
		myLinearLayout.addView(bt,li1);
		myLinearLayout.setBaselineAligned(true);
		myLinearLayout.setBackgroundColor(android.graphics.Color.WHITE);
		myRelativeLayout.addView(myLinearLayout, lp1);
		// Build the list adapter
        mListAdapter = new ExtendedCheckBoxListAdapter(this);
        this.gpxFileItems = fp.findAllGPXFiles();
        for(String itemName : gpxFileItems )
        {
        	System.out.println("add gpx File="+itemName);
        	mListAdapter.addItem( new ExtendedCheckBox(itemName,false));
        }
        // Add some items
       /* 
         for( int i = 1; i < 20; i++ )
         {
         	String newItem = "Item " + i;
         	mListAdapter.addItem( new ExtendedCheckBox(newItem,false));
         }
        */
     // Bind it to the activity!
        if(this.gpxFileItems.size()!=0)
        {
        	 /* Add LinearLayout INTO ContentView */
        	myImportTextView.setVisibility(View.GONE);
        	myListView.setAdapter(this.mListAdapter);
        	bt.setEnabled(true);
        	selectAllCheckBox.setEnabled(true);
        	
            
        	
        }
        else{
        	Log.i("Message", "No Content!");
        	myImportTextView.setVisibility(View.VISIBLE);
        	
        	
        }
    }
    
    private void createBrosweTrackListView(){
    	
    	
		Log.i("Message", "createBrosweTrackListView");
		ListView myListView = (ListView) findViewById(R.id.list);
		TextView myTextView = (TextView) findViewById(R.id.browse_empty);
		
		myListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				// TODO Auto-generated method stub
				Log.i( "TAG", "onItemClick id=" + id);
				TrackTabViewActivity.operatedTrackID = id;
				
				final ProgressDialog myProgressDialog = ProgressDialog.show(TrackTabViewActivity.this,      
						getString(R.string.drawing_track_progressdialog_title), getString(R.string.drawing_track_progressdialog_body), true);
				//Show this track on the map
				
				    BigPlanet.DBAdapter.open();
					operatedTrackCursor = BigPlanet.DBAdapter.getTrack(TrackTabViewActivity.operatedTrackID);
					final String track_name = operatedTrackCursor.getString(1);
					final String track_coordinates = operatedTrackCursor.getString(3);
					final String track_times = operatedTrackCursor.getString(4);
					final String track_elevations = operatedTrackCursor.getString(5);
					Log.i("Message","track_name="+track_name);
					Log.i("Message","track_coordinates="+track_coordinates);
					BigPlanet.DBAdapter.close();
					
					
					
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
						        BigPlanet.addMarkersForDrawing(TrackTabViewActivity.this, placeList, 2);
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
					
					
				
			}});
		
		myListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				onLongListItemClick(view,position,id);
				return false;
			}});
		
		
		
		
    	BigPlanet.DBAdapter.open();
		Cursor myCursor = BigPlanet.DBAdapter.getAllTracks();
		if(myCursor.moveToFirst()){
			
			Log.i("Message", "DB has track records");
			myTextView.setVisibility(View.GONE);
			listViewAdapter = new IconAdapter(this,myCursor);
			//listViewAdapter.setTrackSource(getString(R.string.track_source_string));
			myListView.setAdapter(listViewAdapter);
			
		}
		else
		{
			myTextView.setVisibility(View.VISIBLE);
			return;
		}
		
		 
		BigPlanet.DBAdapter.close();
    	
    	
    }
    
    private void ResetContentView(){
    	   
    	System.out.println("ResetContentView");
    	myListView.setAdapter(null);
    	myListView.setAdapter(this.mListAdapter);
        //setContentView(myRelativeLayout);
    }
    
    private boolean ChechDuplicateTrackInDB(List<ExtendedCheckBox> extendedCheckBoxList){
    	System.out.println("Check Duplicate Track from DB...............");
    	duplicateTrackNameList = new ArrayList<String>();
    	BigPlanet.DBAdapter.open();
    	trackDBCursor = BigPlanet.DBAdapter.getAllTracks();
    	
    	if(trackDBCursor.moveToFirst()){
    		
			do{
				String track_name = trackDBCursor.getString(1); //get track's name
				System.out.println("Retrive track="+track_name+"from DB");
				for(ExtendedCheckBox checkBox:extendedCheckBoxList){
					
					if((checkBox.getText().equals(track_name))&& checkBox.getChecked()){
						System.out.println("In DB, find the duplicate track="+track_name);
						duplicateTrackNameList.add(track_name);
					}
				}
			}
			while(trackDBCursor.moveToNext());
			
			
			if(duplicateTrackNameList.size()>0)// find the duplicate tracks in DB
			{
				System.out.println("Duplicate tracks found...............");
				BigPlanet.DBAdapter.close();
				return true;
			}
			else
			{
				System.out.println("No duplicate tracks found...............");
				BigPlanet.DBAdapter.close();
				return false;
			}
    		
		}
		else
		{
			System.out.println("No track in DB...............");
			// no tracks in DB 
			BigPlanet.DBAdapter.close();
			return false;
		}
		
    }
    
    public Handler getMainHandler(){
    	return this.mainHandler;
    }
    
    private void showAllImportedTracks(){
    	/*
        Intent myIntent = new Intent();
        myIntent.setClass(ExtendedCheckBoxListActivity.this, TrackListViewActivity.class);
        Log.i("Message", "calling TrackListViewActivity");
        startActivity(myIntent);
        */
    	createBrosweTrackListView();
    	mTabHost.setCurrentTab(1);
    	
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
    
    protected void onLongListItemClick(View v,int pos,long id) { 
	      Log.i( "TAG", "onItemLongClick id=" + id);
	      LinearLayout lay = (LinearLayout)v;
			TextView textView = (TextView)lay.findViewById(R.id.track_name);
			Log.i("Message", "onItemLongClick TextView selected is="+textView.getText());
			TrackTabViewActivity.operatedTrackID = id;
			alert_dialog_selection();
	}
    
   private void alert_dialog_selection(){
		
		
		new AlertDialog.Builder(TrackTabViewActivity.this)
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
			  					new AlertDialog.Builder(TrackTabViewActivity.this)
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
			  					//Get more information
			  					Log.i("Message", "Dialog Item- Get more track's infromation is selected");
			  					showDialog(GET_INFORMATION_DIALOG);
			  					
			  					break;
			  				case 3:
			  					//Save the track into SD-Card
			  					Log.i("Message", "Dialog Item- Track Exporting is selected");
			  					final CharSequence strDialogTitle = getString(R.string.str_export_dialog_title);
								final CharSequence strDialogBody = getString(R.string.str_export_dialog_body);
								// 顯示Progress對話方塊
			  					myTrackExportDialog = ProgressDialog.show
						                 (
						                   TrackTabViewActivity.this,
						                   strDialogTitle,
						                   strDialogBody, 
						                   true
						                 );
			  					DBRecordToGpxFile db_to_gpx = new DBRecordToGpxFile();
			  					//db_to_gpx.setActivity(TrackListViewActivity.this);
			  					db_to_gpx.setDBAdapter(BigPlanet.DBAdapter);
			  					db_to_gpx.setTrackID(TrackTabViewActivity.operatedTrackID);
			  					db_to_gpx.setHandler(trackListViewHandler);
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

  private void deleteSelectedTrackFromDB(){
	
	if(TrackTabViewActivity.operatedTrackID > 0 )
	{	
		Log.i("Message","deletedTrackID="+TrackTabViewActivity.operatedTrackID);
		try{
			BigPlanet.DBAdapter.open();
			if(BigPlanet.DBAdapter.deleteTrack(TrackTabViewActivity.operatedTrackID))
			{
				//myTrackDeleteDialog.dismiss();
				//myCursor.requery();
				//TrackListViewActivity.listView.invalidate();
				
				Toast.makeText(this, getString(R.string.delete_track_successfully), 
						Toast.LENGTH_SHORT).show();
				//retrieveAllTracksFromDB();
				createBrosweTrackListView();

			}
			else
			{
				//myTrackDeleteDialog.dismiss(); 
				Toast.makeText(this, getString(R.string.delete_track_fail), 
		                	Toast.LENGTH_SHORT).show();            
			}
		
			BigPlanet.DBAdapter.close();
			
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	else
		Log.e("Error", "TrackListViewActivity.deleteTrackID <= 0");
	
	}
  
  public static String FileterOutForFileNameRule(String trackName)
	 {
		 
		 trackName = trackName.replaceAll("[/:*><|?\"]", "");
		 trackName = trackName.replace('\\', '_');
		 Log.i("Message","the trackName after Filter Out Method="+trackName);
		 return trackName;
		 
	 }

}
