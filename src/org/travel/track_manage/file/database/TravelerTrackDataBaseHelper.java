package org.travel.track_manage.file.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;

public class TravelerTrackDataBaseHelper extends SQLiteOpenHelper {
	
	 private final static String DATABASE_NAME = "track_db";
	 private final static int DATABASE_VERSION = 1;
	 public final static String TABLE_NAME = "track_table";
	 public final static String FIELD_id = "_id";
	 
	 public final static String FIELD_name = "track_name";
	 public final static String FIELD_description = "track_description";
	 public final static String FIELD_coordinate = "track_coordinate";
	 public final static String FIELD_time = "track_time";
	 public final static String FIELD_elevation = "track_elevation";
	 private SQLiteDatabase db;

	 
	 

	public TravelerTrackDataBaseHelper(Context context) {
		
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		// TODO Auto-generated constructor stub
	}
	
	public Cursor select()
	  {
		Log.i("Message","select() is called" );
		SQLiteDatabase db = this.getReadableDatabase();
	    Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, null);
	    return cursor;
	  }
	
	public Cursor getAllFields(){
		db = this.getReadableDatabase();
		return db.query(TABLE_NAME,null, null, null, null, null, null);
		
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		Log.i("Message","onCreate is called" );
		Log.i("Message","create table now!" );
		
		String sql_create_table = "CREATE TABLE " + TABLE_NAME + " (" + FIELD_id
        + " INTEGER primary key autoincrement, " + " " + FIELD_name + " text,"
        + " "+ FIELD_description + " text, "
        + " "+ FIELD_coordinate + " text, "
        + " "+ FIELD_time + " text, "
        + " "+ FIELD_elevation + " text"+")";
		db.execSQL(sql_create_table);

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		Log.i("Message","onUpgrade is called" );
		String sql = "DROP TABLE IF EXISTS " + TABLE_NAME;
	    db.execSQL(sql);
	    onCreate(db);

	}
	
	public long insert(String trackName, String trackDescription, String allTrackCoordinate, String allTrackTime, String allTrackElevation)
	  {
	    SQLiteDatabase db = this.getWritableDatabase();
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

}
