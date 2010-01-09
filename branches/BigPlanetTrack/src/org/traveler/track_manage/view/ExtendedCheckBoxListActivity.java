/*
 * Copyright 2009 Moritz Wundke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traveler.track_manage.view;











import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


import org.travel.track_manage.file.database.TravelDataBaseAdapter;
//import org.traveler.file.database.TravelerTrackDataBaseHelper;
import org.traveler.track_manage.file.operate.ImportFileProcessing;
import org.traveler.track_manage.file.operate.myParseThread;


import com.nevilon.bigplanet.BigPlanet;
import com.nevilon.bigplanet.R;




import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.RelativeLayout.LayoutParams;



public class ExtendedCheckBoxListActivity extends Activity {
	
	//public static TravelerTrackDataBaseHelper myTrackHelper;
	private ExtendedCheckBoxListAdapter mListAdapter;
	private RelativeLayout myRelativeLayout;
	private ImportFileProcessing fp;
	private Handler mainHandler;
	public static Context ctx;
	public static ProgressDialog myGPXParseDialog = null;
	//public static TravelerTrackDataBaseHelper DBHelper;
	public  TravelDataBaseAdapter DBAdapter;
	private Cursor trackDBCursor;
	private CheckBox selectAllCheckBox;
	private List<String> duplicateTrackNameList = null;
	
	
	//private MyControl myControl;
	private ListView myListView;
	private List<String> gpxFileItems = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DBAdapter = BigPlanet.DBAdapter;
        //setContentView(R.layout.main);
        /*------------------------------------------------------------------------------------------*/
         
        Log.i("Message", "new a DBAdapter instance");
        //DBHelper = new TravelerTrackDataBaseHelper(this);
        //DBAdapter = new TravelDataBaseAdapter(this);
        
        //trackDBCursor = DBHelper.select();
        
        
        /*-------------------------------------------------------------------------------------------*/
       // myTrackHelper = new TravelerTrackDataBaseHelper(this);
        WindowManager wm = this.getWindowManager();
		Display display = wm.getDefaultDisplay();
		int height = display.getHeight();
		int width = display.getWidth();
		ctx=this;
		fp = new ImportFileProcessing();
		//fp.setTrackDBHelper(DBHelper);
		fp.setTrackDBAdapter(DBAdapter);
		
		
		mainHandler = new Handler(){
			
			
			public void handleMessage(Message msg) {
               
				Log.i("Message", "flag="+msg.what); //1:全部import成功;0:部分import失敗
				switch (msg.what)
				{
				  
				   case myParseThread.ALL_Files_PARSE_SUCCESSFULLY:
					   /* 全部parse成功*/
					    
						new AlertDialog.Builder(ExtendedCheckBoxListActivity.this)
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
						/*部分parse有錯*/
				
						new AlertDialog.Builder(ExtendedCheckBoxListActivity.this)
				        .setTitle(getString(R.string.track_importing_result))
				        .setMessage(R.string.some_error+(String)msg.obj)
				        .setPositiveButton(getString(R.string.OK),new DialogInterface.OnClickListener()
				            {
				              public void onClick(DialogInterface dialoginterface, int i)
				              {
				            	  Toast.makeText(
				            		        ExtendedCheckBoxListActivity.this,
				            		        getString(R.string.some_successful),
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
		
		myRelativeLayout = new RelativeLayout(this);
        //myLinearLayout.setOrientation(LinearLayout.VERTICAL);
        //myLinearLayout.setBackgroundColor(android.graphics.Color.WHITE);
        
		/* 新增ListView */
        myListView = new ListView(this);
        ViewGroup.LayoutParams param2 = new ViewGroup.LayoutParams(width,height-100);
        //myListView.setBackgroundColor(getResources().getColor(R.drawable.ltgray));
        
        /* 將ListView加到myLinearLayout */
        myRelativeLayout.addView(myListView,0, param2);
       
        /* 新增SelectAllCheckBox */
        selectAllCheckBox = new CheckBox(this);
        selectAllCheckBox.setWidth(100);
        selectAllCheckBox.setHint(getString(R.string.select_all));
        selectAllCheckBox.setHintTextColor(Color.BLACK);
        
        /*將CheckBox預設為未選取狀態*/
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
        
        /* 新增RefreshTracks Button */
      /*
        Button bt_refresh = new Button(this);
        bt_refresh.setWidth(100);
        //bt_view_tracks.setHeight(80);
        bt_refresh.setText("Refresh Tracks");
        bt_refresh.setOnClickListener(new Button.OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				//showAllImportedTracks();
				RefreshListView();
				
			}
        	
        });
      */  
        
        /*-----------------------------------------------------------------------------------------------------------*/
        /* 新增ImportTracks Button */
        Button bt = new Button(this);
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
						builder = new AlertDialog.Builder(ExtendedCheckBoxListActivity.this);
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
						      
								// 顯示Progress對話方塊
								myGPXParseDialog = ProgressDialog.show
						                 (
						                   ExtendedCheckBoxListActivity.this,
						                   strDialogTitle,
						                   strDialogBody, 
						                   true
						                 );
						      
						     
									fp.parseGPXFile(mListAdapter.getListItems(),getMainHandler());
								
							}});
						builder.create().show();
						
						
						
						
					}
					else
					{
						
						final CharSequence strDialogTitle = getString(R.string.str_dialog_title);
						final CharSequence strDialogBody = getString(R.string.str_dialog_body);
				      
						// 顯示Progress對話方塊
						myGPXParseDialog = ProgressDialog.show
				                 (
				                   ExtendedCheckBoxListActivity.this,
				                   strDialogTitle,
				                   strDialogBody, 
				                   true
				                 );
				      
				     
							fp.parseGPXFile(mListAdapter.getListItems(),getMainHandler());
						
						
						
					}
					
					
				/*	
					
					
				*/
				}
				else
					Toast.makeText(
            		        ExtendedCheckBoxListActivity.this,
            		        getString(R.string.have_yet_select_track_message),
            		        Toast.LENGTH_SHORT).show();
			}
			
		});
		
		/*---------------------------------------------------------------------------------------------------------------*/
		RelativeLayout.LayoutParams lp1 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		lp1.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		//lp1.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		
		
		LinearLayout myLinearLayout = new LinearLayout(this);
		LinearLayout.LayoutParams li1 = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.FILL_PARENT);
		//li1.setMargins(10, 10, 10, 10);
		
		//myLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
		myLinearLayout.setHorizontalGravity(Gravity.RIGHT);
		//myLinearLayout.addView(bt_view_tracks,li1);
		myLinearLayout.addView(selectAllCheckBox,li1);
		
		//myLinearLayout.addView(bt_refresh,li1);
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
        	 /* 將LinearLayout加到ContentView */
        	myListView.setAdapter(this.mListAdapter);
            setContentView(myRelativeLayout);
        	
        }
        else{
        	Log.i("Message", "No Content!");
        	setContentView(R.layout.track_import_layout);
        }
        myListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				if(view != null)
				{
					ExtendedCheckBoxListView CurrentView = (ExtendedCheckBoxListView)view;
					if ( CurrentView != null )
					{
						CurrentView.toggleCheckBoxState();
					}
				}
				
			}
        	
        });
        
        
        
    }//end of onCreate
    
    public Handler getMainHandler(){
    	return this.mainHandler;
    }
    
    
    private void showAllImportedTracks(){
    	
        Intent myIntent = new Intent();
        myIntent.setClass(ExtendedCheckBoxListActivity.this, TrackListViewActivity.class);
        Log.i("Message", "calling TrackListViewActivity");
        startActivity(myIntent);
        
        

    	
    	
    }
    
    private void ResetContentView(){
   
    	System.out.println("ResetContentView");
    	myListView.setAdapter(null);
    	myListView.setAdapter(this.mListAdapter);
        setContentView(myRelativeLayout);
    }
    
    private void RefreshListView(){
    	System.out.println("------------------------------");
    	System.out.println("RefreshListView");
    	mListAdapter = null;
    	mListAdapter = new ExtendedCheckBoxListAdapter(this);
        this.gpxFileItems = fp.findAllGPXFiles();
        for(String itemName : gpxFileItems )
        {
        	System.out.println("add gpx File="+itemName);
        	mListAdapter.addItem( new ExtendedCheckBox(itemName,false));
        }
        selectAllCheckBox.setChecked(false);
        ResetContentView();
        Toast.makeText(
		        ExtendedCheckBoxListActivity.this,
		        "重新載入完成",
		        Toast.LENGTH_SHORT).show();
    	
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
					
					if(checkBox.getText().equals(track_name)){
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
    
   
    
    /**
     * If a list item is clicked 
     * we need to toggle the checkbox too!
     */
/*
    @Override

    protected void onListItemClick(ListView l, View v, int position, long id) {
		// Toggle the checkbox state!
		if ( v != null )
		{
			ExtendedCheckBoxListView CurrentView = (ExtendedCheckBoxListView)v;
			if ( CurrentView != null )
			{
				CurrentView.toggleCheckBoxState();
			}
		}
		
		super.onListItemClick(l, v, position, id);
	}
	*/
}