package com.nevilon.bigplanet.core.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class DBHelper extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "data";
		
	    public DBHelper(Context context) {
			super(context, DATABASE_NAME, null, 4);
			

		}
	
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DAO.TABLE_DDL);
		}
	
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS "+ DAO.TABLE_GEOBOOKMARKS);
			onCreate(db);
		}
		
}
