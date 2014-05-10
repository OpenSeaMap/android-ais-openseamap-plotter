package com.klein.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.mapsforge.core.GeoPoint;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

import com.klein.commons.AISPlotterGlobals;
import com.klein.commons.PositionTools;
import com.klein.logutils.Logger;




/**
 * 
 * @author vk1 2.3.11 derived from notepad v3 
 *
 */

public class TrackDbAdapter {
	    
	 private static final int DATABASE_VERSION = 29;  
	    // 11_03_16 ID is used in Target with utcTime and displaystatus
        // 13_03_16 got an error in update targets database schema has changes when a shiptrack exists
	    // has track seems not to be in the targets  record
	    public static final String KEY_ROWID = "_id";
	    public static final String KEY_MMSI = "mmsi";
	    public static final String KEY_NAME = "name";
	    public static final String KEY_LAT  = "lat";
	    public static final String KEY_LON  = "lon";
	    public static final String KEY_COG  = "cog";
	    public static final String KEY_SOG  = "sog";
	    public static final String KEY_HDG  = "hdg";
	    public static final String KEY_UTC = "utc";
	    public static final String KEY_DISPLAYSTATUS = "displaystatus";
	    public static final String KEY_NAVSTATUS = "navstatus";
	    public static final String KEY_SHIPTYPE = "shiptype";
	    public static final String KEY_HAS_TRACK ="hastrack";
	    	
	    
	    	

	    private static final String TAG = "TrackDbAdapter";
	    private DatabaseHelper mDbHelper;
	    private SQLiteDatabase mDb;
	    private boolean test = false;
	    
	    /**
	     * Table creation sql statement for AISTargets
	     */
	    private static final String AIS_TARGETS_TABLE = "targets";
	    private static final String AIS_TARGETS_TABLE_CREATE =
	        "create table " + AIS_TARGETS_TABLE + " (_id integer primary key autoincrement, "
	        + "mmsi text not null, name text not null, lat text not null,"
	        + "lon text not null, cog text not null, sog text not null,"
	        + "hdg text not null, utc integer not null, displaystatus integer not null,"
	        + "navstatus integer not null, shiptype integer not null, "
	        + "hastrack text not null);";
	    
	   
	    
	   /**
	    * table creation sql statement for shiptracks 
	    */
	    private static final String SHIPTRACK_TABLE = "shiptrack";

		private static final String SHIPTRACK_CREATE = "create table ";
		private static final String SHIPTRACK_CREATE_PARAMS = " (_id integer primary key autoincrement,"
				+ "mmsi text not null, lat text not null, lon text not null, utc integer not null);";
       
		private ArrayList<String> mTrackTableNames = new ArrayList<String>();
		
		/**
		    * table creation sql statement for routes 
		    */
		private static final String ROUTE_TABLE = "route";

		private static final String ROUTE_CREATE = "create table ";
		private static final String ROUTE_CREATE_PARAMS = " (_id integer primary key autoincrement,"
				+ "lat text not null, lon text not null, utc integer not null);";
        
	    private static final String DATABASE_NAME = "aisdata";
	    
	   
	   

	    private final Context mCtx;

	    private static class DatabaseHelper extends SQLiteOpenHelper {

	        DatabaseHelper(Context context) {
	            super(context, DATABASE_NAME, null, DATABASE_VERSION);
	        }

	        @Override
	        public void onCreate(SQLiteDatabase db) {

	            db.execSQL(AIS_TARGETS_TABLE_CREATE);
	        }

	        @Override
	        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
	                    + newVersion + ", which will destroy all old data");
	            
	            ArrayList<String> aTableList = listTrackTables(db);
	            for (int index = 0; index < aTableList.size();index++){
	            	String aTableName = aTableList.get(index);
	            	db.execSQL("DROP TABLE IF EXISTS "+aTableName);
	            }
	            db.execSQL("DROP TABLE IF EXISTS "+AIS_TARGETS_TABLE);
	            onCreate(db);
	        }
	        
	        private ArrayList<String> listTrackTables(SQLiteDatabase db) throws SQLException {

				try {
					// see How to get all table names in SQL database in Android Developers
					// http://groups.google.com/group/android-developers/browse_thread/thread/13cd2537a0adc9b9
					// www.sqlite.org/faq.html How do I list all tables/indices contained in an SQLite database
					// String aQuery = "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name;";
					String SQL_GET_ALL_TABLES = "SELECT name FROM " + "sqlite_master WHERE type='table' ORDER BY name";
					ArrayList<String> aStringList = new ArrayList<String>();
					Cursor aCursor = db.rawQuery(SQL_GET_ALL_TABLES, null);
					// Cursor aCursor = this.mDb.query("sqlite_master", null, "type='table'", null, null, null, "name");

					String[] names = aCursor.getColumnNames();
					int count = names.length;

					int rowcount = aCursor.getCount();
					aCursor.moveToFirst();
					int index = aCursor.getColumnIndexOrThrow("name");
					String s1 = aCursor.getString(index);
					if (s1.contains ("shiptrack") ) aStringList.add(s1);
					while (aCursor.moveToNext()) {
						index = aCursor.getColumnIndexOrThrow("name");
						s1 = aCursor.getString(index);
						if (s1.contains ("shiptrack")) aStringList.add(s1);
					}

					aCursor.close();

					return aStringList;

				} catch (SQLException e) {
					Log.d(TAG, " Error getting getting tables from the database ");
					e.printStackTrace();

				}
				return null;
	        }
	    }

	    /**
	     * Constructor - takes the context to allow the database to be
	     * opened/created
	     * 
	     * @param ctx the Context within which to work
	     */
	    public TrackDbAdapter(Context ctx) {
	        this.mCtx = ctx;
	    }

	    /**
	     * Open the notes database. If it cannot be opened, try to create a new
	     * instance of the database. If it cannot be created, throw an exception to
	     * signal the failure
	     * 
	     * @return this (self reference, allowing this to be chained in an
	     *         initialization call)
	     * @throws SQLException if the database could be neither opened or created
	     */
	    public TrackDbAdapter open() throws SQLException {
	        mDbHelper = new DatabaseHelper(mCtx);
	        mDb = mDbHelper.getWritableDatabase();
	        mTrackTableNames = listAllShipTrackTablesInDatabase();
	        if (test) Log.d(TAG,"tables: " + mTrackTableNames.toString());
	        return this;
	    }

	    public void close() throws SQLException{
	        mDbHelper.close();
	    }
	    
	    public void createRouteTable(String aNumberStr) throws SQLException {

			mDb.execSQL(ROUTE_CREATE + ROUTE_TABLE + aNumberStr + ROUTE_CREATE_PARAMS);

		}
	    
	    /**
	     * 
	     * @param aTableNumber
	     * @param aLATStr  in Grad as a String  e.g 10.45
	     * @param aLONStr  in Grad as a String  e.g 54.235
	     * @param aUTC
	     */

		public void insertRoutePointToTable(String aTableNumber, String aLATStr, String aLONStr, long aUTC) {
			ContentValues initialValues = new ContentValues();
			initialValues.put(KEY_LAT, aLATStr);
			initialValues.put(KEY_LON, aLONStr);
			initialValues.put(KEY_UTC, aUTC);
			long id = -1;
			try {
				id = mDb.insert(ROUTE_TABLE + aTableNumber, null, initialValues);
			} catch (SQLException e) {
				Log.d(TAG, " Error while inserting Point in RouteTable to " + aTableNumber);
				Logger.d(TAG, " Error while inserting Point in RouteTable to " + aTableNumber);
			}

		}
		
		public void truncateRouteTable(String aRouteTableNumber) throws SQLException {
			//mDb.execSQL("TRUNCATE TABLE " + ROUTE_TABLE + aRouteTableNumber);
			mDb.delete( ROUTE_TABLE + aRouteTableNumber, null, null);
		}

		public void deleteRouteTable(String aNumber) throws SQLException {

			mDb.execSQL("DROP TABLE IF EXISTS " + ROUTE_TABLE + aNumber);

		}
		
		/**
		 * Lat and Lon are Strings in Grad e.g 10.45  53.34562
		 * @param aNumber
		 * @return
		 * @throws SQLException
		 */

		public Cursor fetchRouteTable(String aNumber) throws SQLException {
			try {
				Cursor aCursor = mDb.query(ROUTE_TABLE + aNumber, new String[] { KEY_ROWID, KEY_LAT, KEY_LON, KEY_UTC },
						null, null, null, null, null); // name,
														// colums[],no
														// selection,no
														// selectionArgs.
														// nogroupBy,
														// no
														// having,
														// no
														// orderBy
				return aCursor;
			} catch (SQLException e) {
				Log.d(TAG, " Error quering RouteTable to " + aNumber + e.toString());
			}
			return null;
		}
	    
	    /** create a table for the track to the target with mmsi
	     * 
	     * @param aMMSI the mmsi of the target to which the track belongs
	     */
	    public void createShipTrackTable(String aMMSI)throws SQLException  {
	    	String tableName = SHIPTRACK_TABLE + aMMSI;
			try {
				mDb.execSQL(SHIPTRACK_CREATE + tableName + SHIPTRACK_CREATE_PARAMS);
				mTrackTableNames.add(tableName);
			} catch (SQLException e) {
				Log.d(TAG, " Error in creating ShipTrackTable to " + aMMSI);
				Log.d(TAG,"exception " + e.toString());
			}
		}
	    /**
	     * insert a track point to the table for this mmsi
	     * @param aMMSI  mmsi of target
	     * @param aLAT   LAT for trackpoint as string e.g. 52.3452
	     * @param aLON   LON for trackpoint as string eg. 6.3432
	     * @param aUTC   long denoting utc of inserting to db
	     */
	    
	    public void insertTrackPointToTable(String aMMSI, String aLAT, String aLON, long aUTC)throws SQLException {
	    	String tableName = SHIPTRACK_TABLE + aMMSI;
	    	 ContentValues initialValues = new ContentValues();
		     initialValues.put(KEY_MMSI, aMMSI);
		     initialValues.put(KEY_LAT, aLAT);
		     initialValues.put(KEY_LON, aLON);
		     initialValues.put(KEY_UTC, aUTC);
		     long id = -1;
		     try {
		       id = mDb.insert(tableName, null, initialValues);
		     }
		     catch (SQLException e){
		    	 String message = " Error while inserting Point in ShipTrackTable to " + aMMSI;
		    	 Logger.d(TAG,message + e.toString());
		     }
		     catch (IllegalStateException e){
		    	 String message = " found a illegal state Exception";
		    	 Logger.d(TAG, message + e.toString());
		    	 e.printStackTrace();
		     }
		     //Log.d(TAG,"TrackPoint Id " + id + " LAT " + aLAT + "  LON " + aLON);
	    }
	    
	    /**
	     *  delete a track table to target with mmsi
	     * @param aMMSI
	     * @throws SQLException
	     */
	    public void deleteShipTrackTable(String aMMSI) throws SQLException {
	    	String tableName = SHIPTRACK_TABLE + aMMSI;
	    	try {
	    		 mDb.execSQL("DROP TABLE IF EXISTS "+tableName);
	    		 mTrackTableNames.clear();
	    		 mTrackTableNames = listAllShipTrackTablesInDatabase();
	    	}
	    	catch(SQLException e) {
	    		Log.d(TAG," Error in deleting ShipTrackTable to " + aMMSI);
	    	}
	    }
	    
	    /**
	     * fetch track table to target with mmsi aMMSI :remember we get lat, lon in grad 53.244
	     * @param aMMSI
	     * @return a Cursor with KEY_ROWID, KEY_MMSI, KEY_LAT, KEY_LON, KEY_UTC
	     * @throws SQLException
	     */
	    public Cursor fetchShipTrackTable(String aMMSI) throws SQLException {
			try {
				Cursor aCursor = mDb.query(SHIPTRACK_TABLE + aMMSI, new String[] { KEY_ROWID, KEY_MMSI, KEY_LAT, KEY_LON,
						KEY_UTC }, null, null, null, null, null); // name,
																	// colums[],no
																	// selection,no
																	// selectionArgs.
																	// nogroupBy,
																	// no
																	// having,
																	// no
																	// orderBy
				return aCursor;
			} catch (SQLException e) {
				Log.d(TAG, " Error quering ShipTrackTable to " + aMMSI);
			}
			return null;
		}
	    
	    /**
	     * get the table size of track to mmsi aMMSI
	     * @param aMMSI
	     * @return  the table size
	     * @throws SQLException
	     */
	    
	    public long getShipTrackTableSize(String aMMSI) throws SQLException {
			long result = -1;
			try {
				Cursor aCursor = mDb.query(SHIPTRACK_TABLE + aMMSI, new String[] { KEY_ROWID }, null, null, null, null,
						null);
				result = aCursor.getCount();
			} catch (SQLException e) {
				Log.d(TAG, " Error getting size of  ShipTrackTable to " + aMMSI);
				e.printStackTrace();
			}
			return result;
		}
	    /**
	     * the returned list normally contains the names  android-metatdata and sqlite-sequence
	     * if a table is created in the db, the name is listed here 
	     * 
	     * @return a list containing the names of all tables in the database except android-metadata and sqlite-sequence
	     */
	    public ArrayList<String> listAllTablesInDatabase() throws SQLException {

			try {
				// see How to get all table names in SQL database in Android Developers
				// http://groups.google.com/group/android-developers/browse_thread/thread/13cd2537a0adc9b9
				// www.sqlite.org/faq.html How do I list all tables/indices contained in an SQLite database
				// String aQuery = "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name;";
				String SQL_GET_ALL_TABLES = "SELECT name FROM " + "sqlite_master WHERE type='table' ORDER BY name";
				ArrayList<String> aStringList = new ArrayList<String>();
				Cursor aCursor = this.mDb.rawQuery(SQL_GET_ALL_TABLES, null);
				// Cursor aCursor = this.mDb.query("sqlite_master", null, "type='table'", null, null, null, "name");

				String[] names = aCursor.getColumnNames();
				int count = names.length;

				int rowcount = aCursor.getCount();
				aCursor.moveToFirst();
				int index = aCursor.getColumnIndexOrThrow("name");
				String s1 = aCursor.getString(index);
				if (s1.contains ("shiptrack") || s1.contains("targets")) aStringList.add(s1);
				while (aCursor.moveToNext()) {
					index = aCursor.getColumnIndexOrThrow("name");
					s1 = aCursor.getString(index);
					if (s1.contains ("shiptrack") || s1.contains("targets")) aStringList.add(s1);
				}

				aCursor.close();

				return aStringList;

			} catch (SQLException e) {
				Log.d(TAG, " Error getting getting tables from the database ");
				e.printStackTrace();

			}
			return null;
		}
	    
	    public ArrayList<String> listAllShipTrackTablesInDatabase() throws SQLException {
	    	Cursor aCursor = null;
			try {
				// see How to get all table names in SQL database in Android Developers
				// http://groups.google.com/group/android-developers/browse_thread/thread/13cd2537a0adc9b9
				// www.sqlite.org/faq.html How do I list all tables/indices contained in an SQLite database
				// String aQuery = "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name;";
				String SQL_GET_ALL_TABLES = "SELECT name FROM " + "sqlite_master WHERE type='table' ORDER BY name";
				ArrayList<String> aStringList = new ArrayList<String>();
				aCursor = this.mDb.rawQuery(SQL_GET_ALL_TABLES, null);
				// Cursor aCursor = this.mDb.query("sqlite_master", null, "type='table'", null, null, null, "name");

				String[] names = aCursor.getColumnNames();
				int count = names.length;

				int rowcount = aCursor.getCount();
				aCursor.moveToFirst();
				int index = aCursor.getColumnIndexOrThrow("name");
				String s1 = aCursor.getString(index);
				if (s1.contains ("shiptrack")) aStringList.add(s1);
				while (aCursor.moveToNext()) {
					index = aCursor.getColumnIndexOrThrow("name");
					s1 = aCursor.getString(index);
					if (s1.contains ("shiptrack")) aStringList.add(s1);
				}

				aCursor.close();

				return aStringList;

			} catch (SQLException e) {
				Log.d(TAG, " Error getting getting tables from the database ");
				e.printStackTrace();
				return null;

			} catch (Exception e) {
				Log.d(TAG," other error getting tables from database");
				return null;
			}
			finally {
				if (aCursor != null) aCursor.close(); 
			}
		
		}
	    
	    
	    public boolean isTableToMMSIInShipTrackList(String aMMSI){
	    	String aTablename = SHIPTRACK_TABLE+aMMSI;
	    	ArrayList<String> aList = mTrackTableNames;
	    	for (int index = 0;index < aList.size();index ++){
	    		if (aTablename.equals(aList.get(index))) {
	    			return true;
	    		}
	    	}
	    	return false;
	    }
	    
	    

       /**
        * insert a target into the aistable  ; remember: the lat and lon are in readable form in  GradMinuteNotation 54'10,345
        * @param aTarget
        * @throws SQLException
        */
	    
        public void createTarget (AISTarget aTarget)throws SQLException{
        	String mmsi = aTarget.getMMSIString();
       	    String name = aTarget.getShipname();
       	    String lat  = aTarget.getLATString();
       	    String lon  = aTarget.getLONString();
       	    String cog  = aTarget.getCOGString();
            String sog =  aTarget.getSOGString();
            String hdg =  aTarget.getHDGString();
            long utc = aTarget.getTimeOfLastStaticUpdate();
            int status = aTarget.getStatusToDisplay();
            int navStatus = aTarget.getNavStatus();
            int shipType = aTarget.getShiptype();
            String hasTrackStr = "false";
            if (aTarget.getHasTrack())
            	{
            	  hasTrackStr = "true";
            	  createShipTrackTable(mmsi);
            	}
            ContentValues initialValues = new ContentValues();
	        initialValues.put(KEY_MMSI, mmsi);
	        initialValues.put(KEY_NAME, name);
	        initialValues.put(KEY_LAT, lat);
	        initialValues.put(KEY_LON, lon);
	        initialValues.put(KEY_COG, cog);
	        initialValues.put(KEY_SOG, sog);
	        initialValues.put(KEY_HDG, hdg);
	        initialValues.put(KEY_UTC, utc);
	        initialValues.put(KEY_DISPLAYSTATUS, status);
	        initialValues.put(KEY_NAVSTATUS,navStatus);
	        initialValues.put(KEY_SHIPTYPE,shipType);
	        initialValues.put(KEY_HAS_TRACK, hasTrackStr);
	        long id = mDb.insert(AIS_TARGETS_TABLE, null, initialValues);
       	    aTarget.setId(id);
       	   
        }
        
       /* unused
	    *//**
	     * Create a new target using the MMSI, name, lat, lon, cog ,sog and hdg as provided. If the target is
	     * successfully created return the new rowId for that target, otherwise return
	     * a -1 to indicate failure.
	     *
	     * @param mmsi the mmsi
	     * @param name the name of the ship
	     * @param lat  latitude in readable Form
	     * @param lon  longitude in readable form
	     * @param cog  course over ground
	     * @param sog  speed over ground
	     * @param hdg  heading 
	     * @return
	     *//*
	    
	    public long createTarget(String mmsi, String name, String lat, String lon, String cog, String sog, String hdg) {
	        ContentValues initialValues = new ContentValues();
	        initialValues.put(KEY_MMSI, mmsi);
	        initialValues.put(KEY_NAME, name);
	        initialValues.put(KEY_LAT, lat);
	        initialValues.put(KEY_LON, lon);
	        initialValues.put(KEY_COG, cog);
	        initialValues.put(KEY_SOG, sog);
	        initialValues.put(KEY_HDG, hdg);

	        return mDb.insert(DATABASE_TABLE, null, initialValues);
	    }
	    */
	    /**
	     * Delete the Target with the given rowId
	     * 
	     * @param rowId id of target to delete
	     * @return true if deleted, false otherwise
	     */
	    public boolean deleteTarget(long rowId) throws SQLException{

	        return mDb.delete(AIS_TARGETS_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
	    }
	    
	    
	    /**
	     * Return a Cursor with MMSI,Name and HasTrack
	     * @return
	     * @throws SQLException
	     */
	    public Cursor fetchAllTargetsMMSIandHasTrack() throws SQLException {
	    	return mDb.query(AIS_TARGETS_TABLE, new String[] {KEY_ROWID, KEY_MMSI,KEY_NAME,KEY_HAS_TRACK}, 
	        		null, null, null, null, KEY_MMSI + " ASC");
	    }
	    
	    /**
	     * Return a Cursor over the list of all targets in the database
	     * 
	     * @return Cursor over all targets
	     */
	    public Cursor fetchAllTargetsMMSIandName()throws SQLException {
           // params: table, columns,
	       // selection , selectionArgs, groupBy, having, orderBy
	       /* return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_MMSI,KEY_NAME}, 
	        		null, null, null, null, null);*/
	        
	        
	        return mDb.query(AIS_TARGETS_TABLE, new String[] {KEY_ROWID, KEY_MMSI,KEY_NAME}, 
	        		null, null, null, null, KEY_MMSI + " ASC");
	    }
	    
	    public Cursor fetchAllTargetsMMSI_Name_SOG()throws SQLException {
	           // params: table, columns,
		       // selection , selectionArgs, groupBy, having, orderBy
		       /* return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_MMSI,KEY_NAME}, 
		        		null, null, null, null, null);*/
		        
		        
		        return mDb.query(AIS_TARGETS_TABLE, new String[] {KEY_ROWID, KEY_MMSI,KEY_NAME,KEY_SOG}, 
		        		null, null, null, null, KEY_MMSI + " ASC");
		    }
	    
	    
	    /**
	     * Return a Cursor over the list of all targets in the database
	     * remember: the lat and lon are in readable form in  GradMinuteNotation 54'10,345
	     * @return Cursor over all targets
	     */
	    public Cursor fetchAllTargets() throws SQLException{

	        return mDb.query(AIS_TARGETS_TABLE, new String[] {KEY_ROWID, KEY_MMSI,
	                KEY_NAME,KEY_LAT,KEY_LON,KEY_COG, KEY_SOG, KEY_HDG, KEY_UTC, KEY_DISPLAYSTATUS,KEY_NAVSTATUS, KEY_SHIPTYPE,KEY_HAS_TRACK}, null, null, null, null, null);
	    }

	    /**
	     * Return a Cursor positioned at the target that matches the given rowId
	     * remember: the lat and lon are in readable form in  GradMinuteNotation 54'10,345
	     * @param rowId id of target to retrieve
	     * @return Cursor positioned to matching target, if found
	     * @throws SQLException if note could not be found/retrieved
	     */
	    public Cursor fetchTarget(long rowId) throws SQLException {

	        Cursor mCursor =

	            mDb.query(true, AIS_TARGETS_TABLE, new String[] {KEY_ROWID,
	                    KEY_MMSI, KEY_NAME, KEY_LAT,KEY_LON,KEY_COG, KEY_SOG,
	                    KEY_HDG,KEY_UTC,KEY_DISPLAYSTATUS, KEY_NAVSTATUS, KEY_SHIPTYPE, KEY_HAS_TRACK}, KEY_ROWID + "=" + rowId, null,
	                    null, null, null, null);
	        if (mCursor != null) {
	            mCursor.moveToFirst();
	        }
	        return mCursor;

	    }
	    
	    /**
	     * Return a Cursor positioned at the target that matches the given MMSI
	     * 
	     * @param aMMSI MMSI of target to retrieve
	     * @return Cursor positioned to matching target, if found
	     * @throws SQLException if target could not be found/retrieved
	     */
	    public Cursor fetchTargetFromMMSI(String aMMSIStr) throws SQLException {

	        Cursor mCursor =

	            mDb.query(true, AIS_TARGETS_TABLE, new String[] {KEY_ROWID,
	                    KEY_MMSI, KEY_NAME}, KEY_MMSI + "=" + aMMSIStr, null,
	                    null, null, null, null);
	        if (mCursor != null) {
	            mCursor.moveToFirst();
	        }
	        return mCursor;

	    }
	    
	    
	    public boolean updateTarget (AISTarget aTarget)throws SQLException{
        	long rowId = aTarget.getId();
        	String mmsi = aTarget.getMMSIString();
        	String name = aTarget.getShipname();
       	    String latStr  = aTarget.getLATString();  //!! readable form GradMinuteNotation 54'10,345
       	    String lonStr  = aTarget.getLONString();  //!! readable form GradMinuteNotation 54'10,345
       	    String cog  = aTarget.getCOGString();
            String sog =  aTarget.getSOGString();
            String hdg =  aTarget.getHDGString();
            long utcStatic = aTarget.getTimeOfLastStaticUpdate();
            long utcPosReport = aTarget.getTimeOfLastPositionReport();
            long utc = utcStatic;
            if (utcPosReport > utcStatic  ) utc = utcPosReport;
            int status = aTarget.getStatusToDisplay();
            int navStatus = aTarget.getNavStatus();
            int shipType = aTarget.getShiptype();
            String hasTrackStr = "false";
            if (aTarget.getHasTrack()) {
            	hasTrackStr = "true";
            	/* we do this in onTap in AISPlotter 12_04_18
            	 * if (!isTableToMMSIInShipTrackList(mmsi)){
            		createShipTrackTable(mmsi);
            		Log.d(TAG,"create track table to "+ mmsi);
            	}*/
            	// we keep the lat and lon in grad, e.g 54.4345 not in readable form in the TrackPointTable
            	String latInGrad = Double.toString(aTarget.getLAT());
            	String lonInGrad = Double.toString(aTarget.getLON());
            	insertTrackPointToTable(mmsi,latInGrad, lonInGrad, utcStatic);
            	if(test) {
            		Log.d(TAG, "insert track point to "+ mmsi + " " + latInGrad + " " + lonInGrad );
            	}
            } /* we do this in onTap in AISPlotter 12_04_18
              else {
            	if (isTableToMMSIInShipTrackList(mmsi)){
            		deleteShipTrackTable(mmsi);
            		Log.d(TAG,"deleted track table to "+ mmsi);
            	}
            }*/
            ContentValues args = new ContentValues();
            args.put(KEY_MMSI, mmsi);
	        args.put(KEY_NAME, name);
	        args.put(KEY_LAT, latStr);
	        args.put(KEY_LON, lonStr);
	        args.put(KEY_COG, cog);
	        args.put(KEY_SOG, sog);
	        args.put(KEY_HDG, hdg);
	        args.put(KEY_UTC, utc);
	        args.put(KEY_DISPLAYSTATUS, status);
	        args.put(KEY_NAVSTATUS,navStatus);
	        args.put(KEY_SHIPTYPE,shipType);
	        args.put(KEY_HAS_TRACK, hasTrackStr);
	        int theResult = mDb.update(AIS_TARGETS_TABLE, args, KEY_ROWID + "=" + rowId, null);
	        if (theResult >  1) {
	        	Log.d(TAG,"error in update db"  );
	        }
	        boolean result =  theResult > 0;
	        return result;
        }
	    
	   public void resetDatabase() {
		   mDb.execSQL("DROP TABLE IF EXISTS "+AIS_TARGETS_TABLE); 
		   mDb.execSQL(AIS_TARGETS_TABLE_CREATE);
	   }
	   
	   /**
		 * @param pattern
		 *            use a pattern like "000.00"
		 * @param value
		 *            the value to convert 45.34523
		 * @return aString with the value formatted 045.34
		 */
		public static String customFormat(String pattern, double value) {
			DecimalFormat myFormatter = new DecimalFormat(pattern);
			String output = myFormatter.format(value);
			return output;
		}  
	// the following is needed to save the data to the sd-card
		private String getCurrentDateTime() {
			final Calendar c = Calendar.getInstance();
			int mYear = c.get(Calendar.YEAR);
			if (mYear > 2000)
				mYear = mYear - 2000;
			int mMonth = c.get(Calendar.MONTH);
			int mDay = c.get(Calendar.DAY_OF_MONTH);
			int mHour = c.get(Calendar.HOUR_OF_DAY);
			int mMinute = c.get(Calendar.MINUTE);
			int mSecond = c.get(Calendar.SECOND);
			StringBuffer buf = new StringBuffer();
			buf.append(customFormat("00", mYear));
			buf.append("_");
			buf.append(customFormat("00", mMonth + 1));
			buf.append("_");
			buf.append(customFormat("00", mDay));
			buf.append("_");
			buf.append(customFormat("00", mHour));
			buf.append("_");
			buf.append(customFormat("00", mMinute));
			buf.append("_");
			buf.append(customFormat("00", mSecond));
			String aDateStr = buf.toString();
			if (test)
				Log.v(TAG, "Datum: " + aDateStr);
			return aDateStr;
		}
	   private void createTrackDirectoryIfNecessaryOld(String aDirectoryName) {
		   // unused since 12_10_30
		   // external sd-card may be not mounted on sdcard2
		   // use PositionTools.createExternalDirectoryIfNecessary()
			if (test)
				Log.v(TAG, "createAISDirectory");
			String result = Environment.getExternalStorageState();
			if (result.equals(Environment.MEDIA_MOUNTED)) {

				// first we check if there is a directory named sdcard2
				File path = Environment.getExternalStorageDirectory();
				File[] subDirs = path.listFiles(new FileFilter() {
					@Override
					public boolean accept(File d) {
						return d.isDirectory();
					}
				});
				boolean foundSdCard2 = false;
				if (test)
					Log.d(TAG, "Subdirs of " + path.getName());
				if (test)
					Log.d(TAG, "Subdirs of " + path.getAbsolutePath());
				for (int index = 0; index < subDirs.length; index++) {
					String aName = subDirs[index].getName();
					if (test)
						Log.d(TAG, aName);
					if (aName.equals("sdcard2"))
						foundSdCard2 = true;
				}
				StringBuffer buf = new StringBuffer();
				if (foundSdCard2)
					buf.append("sdcard2/");
				buf.append(aDirectoryName);
				String dirName = buf.toString();
				File file = new File(path, dirName);
				try {
					String filePathStr = file.getAbsolutePath();
					if (file.mkdir()) { // here we need android permission in the manifest
						if (test)
							Log.v(TAG, "erzeuge Directory: " + filePathStr);
					} else {
						if (test)
							Log.v(TAG, "directory schon vorhanden " + filePathStr);
					}
				} catch (SecurityException se) {
					se.printStackTrace();
					if (test)
						Log.v("TAG", "Security exception : Directory not created " + se);
				}// try
			}
		}
	   
	   public void WriteTrackDataToExternalStorage(String aMMSI) {
		    String aName = "";
		    String myMMSI = Long.toString(AISPlotterGlobals.myShipMMSI);
		    if (aMMSI.equals(myMMSI)){
		    	aName = "myShip";
		    } else {
		    	try {
				    Cursor cursor = this.fetchTargetFromMMSI(aMMSI);
					aName = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_NAME));
				    cursor.close();
		    	} 
		    	catch (Exception e) {
		    	  e.printStackTrace();
		    	}
		    }
		    
		    
		    final String theMMSI = aMMSI;
		    final String theName = aName;
		    
			new Thread(new Runnable() { // as we want response to the user we must use a separate thread
						public void run() {
							// Create a path where we will place our data in the user's
							// public directory. Note that you should be careful about
							// what you place here, since the user often manages these files.
							// we write the data in a directory called AISPlotter/Trackdata
							String result = Environment.getExternalStorageState();
							if (result.equals(Environment.MEDIA_MOUNTED)) {
								String aDirectoryName = AISPlotterGlobals.DEFAULT_TRACK_DATA_DIRECTORY;
								PositionTools.createExternalDirectoryIfNecessary(aDirectoryName);
								File path = PositionTools.getExternalStorageDir();
								StringBuffer buf = new StringBuffer();
								// if we found a sdcard2 directory we put it in front of the path
								
								buf.append(aDirectoryName);
								buf.append("/Track_");
								buf.append(theName);
								buf.append("_");
								buf.append(theMMSI);
								buf.append("_");
								buf.append(getCurrentDateTime());
								buf.append(".gpx");
								String fileName = buf.toString();
								File file = new File(path, fileName);
								String filePathStr = file.getAbsolutePath();
								try {
									if (file.createNewFile()) { // here we need android permission in the manifest
										if (test)
											Log.v(TAG, "create file: " + filePathStr);
									} else {
										if (test)
											Log.v(TAG, "file exists, overwrite " + filePathStr);
									}
									// the file exists or was opened for writing
									BufferedWriter fileBuf = new BufferedWriter(new FileWriter(file));
									// Write the gpx header
									// <?xml version="1.0" encoding="UTF-8"?>
									// write header
									String header = "<?xml version=" + '"' + "1.0" + '"' + " encoding=" + '"' + "UTF-8"
											+ '"' + "?>";
									fileBuf.write(header);
									// "creator="GPSTracker" version="1.1"
									String gpxStart = "\n <gpx creator=" + '"' + "AdvancedMapViewer" + '"' + " version="
											+ '"' + "0.3" + '"';
									// xsi:schemaLocation="http://www.topografix.com/GPX/1/1
									gpxStart = gpxStart + "\n xsi:schemaLocation=" + '"'
											+ "http://www.topografix.com/GPX/1/1 ";
									// http://www.topografix.com/GPX/1/1/gpx.xsd"
									gpxStart = gpxStart + "\n http://www.topografix.com/GPX/1/1/gpx.xsd" + '"';
									// xmlns="http://www.topografix.com/GPX/1/1"
									gpxStart = gpxStart + "\n xmlns=" + '"' + "http://www.topografix.com/GPX/1/1" + '"';
									// xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
									gpxStart = gpxStart + "\n xmlns:xsi=" + '"'
											+ "http://www.w3.org/2001/XMLSchema-instance" + '"' + ">";
									fileBuf.write(gpxStart);
									String metadataStart = "\n <metadata>";
									fileBuf.write(metadataStart);
									String metadataEnd = "\n </metadata>";
									fileBuf.write(metadataEnd);
									String trsegStart = "\n <trk>\n <trkseg>";
									fileBuf.write(trsegStart);

									Cursor cursor = fetchShipTrackTable(theMMSI);
									int count = cursor.getCount();
									int updateProgressbar = count / 100; // The progressbar is 0 to 100,
									// we calculate the counter if we have to update the progressbar
									int updateCounter = 0;
									if ((cursor != null) && (cursor.getCount() > 0)) {

										String aId;
										String aLATStr;
										String aLONStr;
										String aMMSI;
										long aUTC;
										// we use the simpleDataFormat to convert millis to gpx-readable form
										String format = "yyyy-MM-dd'T'HH:mm:ss";
										SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
										sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
										cursor.moveToFirst();
										aId = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_ROWID));
										aMMSI = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_MMSI));
										aLATStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LAT));
										aLONStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LON));
										aUTC = cursor.getLong(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_UTC));
										GeoPoint aTrackPoint = new GeoPoint(Double.parseDouble(aLATStr), Double
												.parseDouble(aLONStr));
										StringBuffer bufPoint = new StringBuffer();
										// <trkpt lon="6.96045754" lat="51.44282806"> <time>2011-07-16T13:24:55</time>
										// </trkpt>
										bufPoint.append("\n <trkpt lon=" + '"' + aTrackPoint.getLongitude() + '"' + " lat="
												+ '"' + aTrackPoint.getLatitude() + '"' + ">" + " <time>"
												+ sdf.format(new Date(aUTC)) + "</time> " + "</trkpt>");

										String aString = bufPoint.toString();
										fileBuf.write(aString);
										while (cursor.moveToNext()) {
											aId = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_ROWID));
											aMMSI = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_MMSI));
											aLATStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LAT));
											aLONStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LON));
											aUTC = cursor.getLong(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_UTC));
											if (test) {
												Log.i(TAG, "next route Point " + aId + " MMSI " + theMMSI + " LAT " + aLATStr
														+ " LON " + aLONStr + " UTC " + aUTC);
											}

											double lat = Double.parseDouble(aLATStr);
											double lon = Double.parseDouble(aLONStr);
											aTrackPoint = new GeoPoint(lat, lon);
											bufPoint = new StringBuffer();
											// <trkpt lon="6.96045754" lat="51.44282806"> </trkpt>
											bufPoint.append("\n <trkpt lon=" + '"' + aTrackPoint.getLongitude() + '"'
													+ " lat=" + '"' + aTrackPoint.getLatitude() + '"' + ">" + " <time>"
													+ sdf.format(new Date(aUTC)) + "</time> " + "</trkpt>");

											aString = bufPoint.toString();
											fileBuf.write(aString);
											
										}

									}
									cursor.close();
									String trsegEnd = "\n </trkseg>\n </trk>\n </gpx>";
									fileBuf.write(trsegEnd);
									fileBuf.flush();
									fileBuf.close();
									if (test)
										Log.v(TAG, "file write sucessfull " + filePathStr);
									

								} catch (IOException e) {

									e.printStackTrace();
									// Unable to create file, likely because external storage is
									// not currently mounted.
									if (test)
										Log.w("TAG", "Error writing " + filePathStr);
								}
							} // if media mounted
						} // run
					}).start();
		}
 
}
