package org.traveler.track_manage.file.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class TravelDataBaseAdapter {
	
	 public final static String FIELD_id = "_id"; // column 0
	 public final static String FIELD_name = "track_name"; // column 1
	 public final static String FIELD_description = "track_description";// column 2
	 public final static String FIELD_coordinate = "track_coordinate";// column 3
	 public final static String FIELD_time = "track_time";// column 4
	 public final static String FIELD_elevation = "track_elevation";// column 5
	 public final static String FIELD_consumedTime = "track_consumedTime"; // column 6
	 public final static String FIELD_totalDistance = "track_totalDistance"; // column 7
	 public final static String FIELD_averageSpeed = "track_averageSpeed"; // column 8
	 public final static String FIELD_manximumSpeed = "track_manximumSpeed"; // column 9
	 public final static String FIELD_trackPointNumber = "track_PointNumber"; // column 10
	 public final static String FIELD_trackSource = "track_source"; // column 11
	 
	 
	 private final static String DATABASE_NAME = "track_db";
	 private final static int DATABASE_VERSION = 2;
	 private final static String TABLE_NAME = "track_table";
     private static final String TAG = "DBAdapter";
     
     private static final String DATABASE_CREATE ="CREATE TABLE " + TABLE_NAME + " (" + FIELD_id
     + " INTEGER primary key autoincrement, " + " " + FIELD_name + " text,"
     + " "+ FIELD_description + " text, "
     + " "+ FIELD_coordinate + " text, "
     + " "+ FIELD_time + " text, "
     + " "+ FIELD_elevation + " text, "
     + " "+ FIELD_consumedTime + " long, "
     + " "+ FIELD_totalDistance + " float, "
     + " "+ FIELD_averageSpeed + " double, "
     + " "+ FIELD_manximumSpeed + " double, "
     + " "+ FIELD_trackPointNumber + " long, "
     + " "+ FIELD_trackSource + " text);";
     
     private final Context context;
     private DatabaseHelper DBHelper;
     private SQLiteDatabase db;
     
     public TravelDataBaseAdapter(Context ctx) 
     {
         this.context = ctx;
         DBHelper = new DatabaseHelper(context);
     }
     
     private static class DatabaseHelper extends SQLiteOpenHelper 
     {
         DatabaseHelper(Context context) 
         {
             super(context, DATABASE_NAME, null, DATABASE_VERSION);
         }

         @Override
         public void onCreate(SQLiteDatabase db) 
         {
             db.execSQL(DATABASE_CREATE);
         }

         @Override
         public void onUpgrade(SQLiteDatabase db, int oldVersion, 
         int newVersion) 
         {
             Log.w(TAG, "Upgrading database from version " + oldVersion 
                     + " to "
                     + newVersion + ", which will destroy all old data");
             db.execSQL("DROP TABLE IF EXISTS "+TABLE_NAME);
             onCreate(db);
         }
     }    
     
     
   //---opens the database---
     public TravelDataBaseAdapter open() throws SQLException 
     {
         db = DBHelper.getWritableDatabase();
         return this;
     }

   //---closes the database---    
     public void close() 
     {
         DBHelper.close();
     }
     
     
     public long insertTrack(String trackName, String trackDescription, String allTrackCoordinate, String allTrackTime, String allTrackElevation, 
    		 long track_consumedTime, float track_totalDistance, double track_averageSpeed, double track_manximumSpeed, long track_trackPointNumber
    		 ,String track_source)
	  {
	    
	    /* �N�s�W���ȩ�JContentValues */
	    ContentValues cv = new ContentValues();
	    cv.put(TravelDataBaseAdapter.FIELD_name, trackName);
	    cv.put(TravelDataBaseAdapter.FIELD_description, trackDescription);
	    cv.put(TravelDataBaseAdapter.FIELD_coordinate, allTrackCoordinate);
	    cv.put(TravelDataBaseAdapter.FIELD_time, allTrackTime);
	    cv.put(TravelDataBaseAdapter.FIELD_elevation, allTrackElevation);
	    cv.put(TravelDataBaseAdapter.FIELD_consumedTime, track_consumedTime);
	    cv.put(TravelDataBaseAdapter.FIELD_totalDistance, track_totalDistance);
	    cv.put(TravelDataBaseAdapter.FIELD_averageSpeed, track_averageSpeed);
	    cv.put(TravelDataBaseAdapter.FIELD_manximumSpeed, track_manximumSpeed);
	    cv.put(TravelDataBaseAdapter.FIELD_trackPointNumber, track_trackPointNumber);
	    cv.put(TravelDataBaseAdapter.FIELD_trackSource, track_source);
	    long row = db.insert(TABLE_NAME, null, cv);
	    return row;
	  }
     
   //---deletes a particular track---
     public boolean deleteTrack(long rowId) 
     {
         return db.delete(TABLE_NAME, FIELD_id + 
         		"=" + rowId, null) > 0;
     }
     
   //---retrieves all the tracks---
     public Cursor getAllTracks() 
     {
         return db.query(TABLE_NAME, new String[] {
        		 FIELD_id, 
        		 FIELD_name,
        		 FIELD_description,
        		 FIELD_coordinate,
        		 FIELD_time,
        		 FIELD_elevation,
        		 FIELD_consumedTime,
        		 FIELD_totalDistance,
        		 FIELD_averageSpeed,
        		 FIELD_manximumSpeed,
        		 FIELD_trackPointNumber,
        		 FIELD_trackSource}, 
                 null, 
                 null, 
                 null, 
                 null, 
                 FIELD_id+" DESC");
     }
     
     
   //---retrieves a particular track---
     public Cursor getTrack(long rowId) throws SQLException 
     {
         Cursor mCursor =
                 db.query(true, TABLE_NAME, new String[] {
                		 FIELD_id, 
                		 FIELD_name,
                		 FIELD_description,
                		 FIELD_coordinate,
                		 FIELD_time,
                		 FIELD_elevation,
                		 FIELD_consumedTime,
                		 FIELD_totalDistance,
                		 FIELD_averageSpeed,
                		 FIELD_manximumSpeed,
                		 FIELD_trackPointNumber,
                		 FIELD_trackSource
                 		}, 
                 		FIELD_id + "=" + rowId, 
                 		null,
                 		null, 
                 		null, 
                 		null, 
                 		null);
         
         if (mCursor != null) {
             mCursor.moveToFirst();
         }
         return mCursor;
     }
     
     
   //---updates a track---
     public boolean updateTrack(long trackID, String trackName, String trackDescription, 
    		 String allTrackCoordinate, String allTrackTime, String allTrackElevation, long track_consumedTime, float track_totalDistance, 
    		 double track_averageSpeed, double track_manximumSpeed, long track_PointNumber, String track_Source) 
     {
         ContentValues args = new ContentValues();
         args.put(FIELD_name, trackName);
         args.put(FIELD_description, trackDescription);
         args.put(FIELD_coordinate, allTrackCoordinate);
         args.put(FIELD_time, allTrackTime);
         args.put(FIELD_elevation, allTrackElevation);
         args.put(FIELD_consumedTime, track_consumedTime);
         args.put(FIELD_totalDistance, track_totalDistance);
         args.put(FIELD_averageSpeed, track_averageSpeed);
         args.put(FIELD_manximumSpeed, track_manximumSpeed);
         args.put(FIELD_trackPointNumber, track_PointNumber);
         args.put(FIELD_trackSource, track_Source);
         return db.update(TABLE_NAME, args, 
        		 FIELD_id + "=" + trackID, null) > 0;
     }











}
