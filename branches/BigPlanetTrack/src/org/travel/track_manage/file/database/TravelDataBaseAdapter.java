package org.travel.track_manage.file.database;

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
	 
	 private final static String DATABASE_NAME = "track_db";
	 private final static int DATABASE_VERSION = 1;
	 private final static String TABLE_NAME = "track_table";
     private static final String TAG = "DBAdapter";
     
     private static final String DATABASE_CREATE ="CREATE TABLE " + TABLE_NAME + " (" + FIELD_id
     + " INTEGER primary key autoincrement, " + " " + FIELD_name + " text,"
     + " "+ FIELD_description + " text, "
     + " "+ FIELD_coordinate + " text, "
     + " "+ FIELD_time + " text, "
     + " "+ FIELD_elevation + " text"+")";
     
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
             db.execSQL("DROP TABLE IF EXISTS titles");
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
     
     
     public long insertTrack(String trackName, String trackDescription, String allTrackCoordinate, String allTrackTime, String allTrackElevation)
	  {
	    
	    /* �N�s�W���ȩ�JContentValues */
	    ContentValues cv = new ContentValues();
	    cv.put(TravelerTrackDataBaseHelper.FIELD_name, trackName);
	    cv.put(TravelerTrackDataBaseHelper.FIELD_description, trackDescription);
	    cv.put(TravelerTrackDataBaseHelper.FIELD_coordinate, allTrackCoordinate);
	    cv.put(TravelerTrackDataBaseHelper.FIELD_time, allTrackTime);
	    cv.put(TravelerTrackDataBaseHelper.FIELD_elevation, allTrackElevation);
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
        		 FIELD_elevation}, 
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
                		 FIELD_elevation
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
    		 String allTrackCoordinate, String allTrackTime, String allTrackElevation) 
     {
         ContentValues args = new ContentValues();
         args.put(FIELD_name, trackName);
         args.put(FIELD_description, trackDescription);
         args.put(FIELD_coordinate, allTrackCoordinate);
         args.put(FIELD_time, allTrackTime);
         args.put(FIELD_elevation, allTrackElevation);
         return db.update(TABLE_NAME, args, 
        		 FIELD_id + "=" + trackID, null) > 0;
     }











}
