package com.klein.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Stack;
import java.util.TimeZone;

import org.json.JSONException;
import org.json.JSONObject;
import org.mapsforge.core.GeoPoint;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.util.Log;

import com.klein.commons.AISPlotterGlobals;
import com.klein.commons.PositionTools;
import com.klein.logutils.Logger;




/**
 * @author vk1
 *
 */
public class NMEAParser {
	private static boolean test = false; 
	private static final double aThousandOfMile = 1d / 60000;
	   private static final String TAG = "NMEAParser";
	   private Stack<String> mNEMEAStack = new Stack<String>() ;
	   private long msgNr = 0;
	   private int nrOfInfo = 1000;
	   private int nrOfTarget = 0;
	   private TargetList mTargetList;
	   private TrackDbAdapter mDbAdapter;
	   
	   private double lastLAT = 91d;  //  we keep the last known position of us
	   private double lastLON = 181d;
	   private int lastSOG = 0;
	   private int lastCOG = 0;
	   private int lastSpeedThruWater = 0;
	   private Double lastDepth = 0d;
	   private Double lastWindSpeed = 0d;
	   private Double lastWindAngle = 0d;
	   private long lastNMEASpeedUpdateTime = 0;
	   private long mCurrentNmeaTime = 0l;
	   private Context ctx;
	   private int mUseGPSSource = 0;
	   private boolean mSavePositionData = true;
	   private boolean mLogPositionData = false;
	   private boolean mLogAll = false;
	   private boolean mLogParsing = false;
	   private long mTimeOfLastRMCMessage = 0;
	   // new Version with GeoPoint 12_01_24
	   // mMyGeoPoint is a mapsforge GeoPoint
	   private GeoPoint mMyGeoPoint;
	   
	   //private ShipTrackList mMyShipTrackList ;never used 12_04_19
	   
	   /**
		 * Constructs a new Parser 
		 * 
		 * 
		 * @param ctx 
		 *        the context of the service, with is using the parser
		 *  
		 * @param useGPSSourceboolean
		 *            0 we have no gps
		 *            1 we use the internal gps
		 *            2 we get gps-ata from the nmea-stream
		 * @param savePositionData
		 *            if true, we save the data in the database
		 *            if false we only keep the data in the AIStargetlist
		 * @param logPositionData
		 *            if true, we log the data in the data monitor
		 *            if false we only keep the data in the AIStargetlist
		 */
		public NMEAParser(Context ctx ,int useGPSSource, boolean savePositionData, boolean logPositionData) {
			
			if (test) Log.d(TAG, "NMEAParser create ");
			msgNr = 0;
			this.ctx = ctx;
			mTargetList = new TargetList(ctx);
			//mMyShipTrackList = new ShipTrackList(ctx); never used 12_04_19
			mDbAdapter = new TrackDbAdapter(ctx);
			mDbAdapter.open();
			mCurrentNmeaTime = System.currentTimeMillis();
			mSavePositionData = savePositionData;
			mLogPositionData = logPositionData;
			mUseGPSSource = useGPSSource;
			fillTargetListWithData();
			
		}
		
		public void setLogPositionData (boolean logIt){
			mLogPositionData = logIt;
			if (!mLogPositionData){
				Logger.d(TAG,"not logging decoded messages");
			}
			else {
				Logger.d(TAG,"logging decoded messages");
			}
		}
		
		/**
		 * we must close the database adapter 
		 * 
		 */  
		
		public void destroy(){
			mDbAdapter.close();
			if (mTargetList != null){
				// the target list opens a dpadapter, this must be closed 2014_02_01
				mTargetList.destroy();
			}
		}
		
		
		
		/* never used 12_04_19
		*//**
		 * fill the shipTrackList with data from the database
		 *//*
		
		private void fillShipTrackListWithData(){
			String aMMSI = "12345678";
			Cursor cursor = mDbAdapter.fetchShipTrackTable(aMMSI);
			if ((cursor != null)&& (cursor.getCount() > 0)) {
	        	cursor.moveToFirst();
	        	String aId = cursor.getString(cursor.getColumnIndexOrThrow(AISDbAdapter.KEY_ROWID));
	            String aLATStr = cursor.getString(cursor.getColumnIndexOrThrow(AISDbAdapter.KEY_LAT));
	            String aLONStr = cursor.getString(cursor.getColumnIndexOrThrow(AISDbAdapter.KEY_LON));
	            long aUTC = cursor.getLong(cursor.getColumnIndexOrThrow(AISDbAdapter.KEY_UTC));
	            double aLAT = 0;
	            double aLON = 0;
	            try {
	            	aLAT = Double.parseDouble(aLATStr);
	            	aLON = Double.parseDouble(aLONStr);
	            } catch (NumberFormatException e){
	            	Log.d(TAG," Error in parsing LAT/LON");
	            	e.printStackTrace();
	            }
	           GeoPoint aGeoPoint = new GeoPoint(aLAT,aLON);
	           mMyShipTrackList.add(aGeoPoint);
			}
		}
		
		
		
		public ShipTrackList getMyShipTrackList(){
			return mMyShipTrackList;
		}
		
		public void addShipTrackPoint ( GeoPoint aGeoPoint) {
			mMyShipTrackList.add(aGeoPoint);
			String aMMSI = "12345678";
			String aLAT = Double.toString(aGeoPoint.getLatitude());
			String aLON = Double.toString(aGeoPoint.getLongitude());
			long aUTC = System.currentTimeMillis();
			try {
				mDbAdapter.insertTrackPointToTable(aMMSI, aLAT, aLON, aUTC);
		    } catch (SQLException e){
		    	
		    }
		}
		
		public void addShipTrackPoint (String aMMSI, GeoPoint aGeoPoint) {
			
			String aLAT = Double.toString(aGeoPoint.getLatitude());
			String aLON = Double.toString(aGeoPoint.getLongitude());
			long aUTC = System.currentTimeMillis();
			try {
				mDbAdapter.insertTrackPointToTable(aMMSI, aLAT, aLON, aUTC);
		    } catch (SQLException e){
		    	
		    }
		}
			*/
		
		/**
		 * fill the AISTargetList with data from the database 
		 * 
		 */ 
		
		private void fillTargetListWithData() { 
	        // Get all of the rows from the database and fill the targetList
			
			ArrayList<String> aTableList = mDbAdapter.listAllShipTrackTablesInDatabase();
			String aResult = aTableList.toString();
			Log.d(TAG,"Tracks: " + aResult); 
			
			Cursor cursor = null;;
			int x = 1;
			try {
				 cursor = mDbAdapter.fetchAllTargets();
			} catch (SQLException e) {
				Log.d(TAG, "error fetching targets from database");
				if (cursor != null)cursor.close();
                return;
			}
	       
	       
	        if ((cursor != null)&& (cursor.getCount() > 0)) {
	        	cursor.moveToFirst();
	        	String aId = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_ROWID));
	        	String aMMSI = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_MMSI));
	            String aName = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_NAME));
	            String aLATStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LAT));
	            String aLONStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LON));
	            String aCOGStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_COG));
	            String aSOGStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_SOG));
	            String aHDGStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_HDG));
	            long aUTC = cursor.getLong(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_UTC));
	            int aStatus = cursor.getInt(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_DISPLAYSTATUS));
	            String hasTrackStr =cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_HAS_TRACK));
	            boolean hasTrack = hasTrackStr.equals("true");
	           
	            byte aShipType = (byte)cursor.getInt(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_SHIPTYPE));
		        byte aNavStatus = (byte) cursor.getInt(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_NAVSTATUS));
	            AISTarget aTarget = new AISTarget(aId,aMMSI,aName,aLATStr,aLONStr,aSOGStr, aCOGStr,aHDGStr,aUTC, aStatus,aNavStatus, aShipType,hasTrack);
	            if (hasTrack)Log.d(TAG, "found target with track " + aMMSI + " " + aName);
	            mTargetList.add(aTarget);
	            //sendInitGUIUpdateEvent(aTarget);
	            while (cursor.moveToNext()){
	        	   aId = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_ROWID));
	               aMMSI = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_MMSI));
	               
	               aName = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_NAME));
	               if (aMMSI.contains("378"))
	            	   Log.d(TAG, aMMSI +" " + aName);
	               aLATStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LAT));
	               aLONStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LON));
	               aCOGStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_COG));
	               aSOGStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_SOG));
	               aHDGStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_HDG));
	               aUTC = cursor.getLong(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_UTC));
	               aStatus = cursor.getInt(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_DISPLAYSTATUS));
	               hasTrackStr =cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_HAS_TRACK));
		           hasTrack = hasTrackStr.equals("true");
		           aShipType = (byte)cursor.getInt(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_SHIPTYPE));
		           aNavStatus = (byte) cursor.getInt(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_NAVSTATUS));
	               aTarget = new AISTarget(aId,aMMSI,aName,aLATStr,aLONStr,aSOGStr, aCOGStr,aHDGStr,aUTC,aStatus,aNavStatus, aShipType,hasTrack);
	               if (hasTrack){
	            	   Log.d(TAG, "found target with track " + aMMSI + " " + aName);
	               }
	               mTargetList.add(aTarget);
	               //sendInitGUIUpdateEvent(aTarget);
	               if (test) Log.v(TAG,"target added "+ aMMSI);
	        	}
	         }
	        if (cursor != null) cursor.close();
	    }
		
		/**
		 * process a Message read from the nmea-stream
		 * 
		 * @param pMessage
		 *       pMessage contains a nmea-sentence
		 *       if we have data from Amarino, we delete the wrapper at the front
		 *       to get a clear nmea-sentence
		 */
		public void processMessage (String pMessage) {
			boolean savedTestStatus = test;
			//test = true;
			if (test) 
				Log.d(TAG, "Nr " + msgNr + " " + pMessage);
			msgNr ++;
			// get the utc time for the time stamp
			mCurrentNmeaTime = System.currentTimeMillis(); // this is  UTC
			// if there are Amarino wrapper (char 18 or char 19) at the beginning delete them
			char char19 = (char) 19; // Armarino wrapper
			char char18 = (char) 18;
			if ((pMessage.charAt(0) == char19)|| (pMessage.charAt(0) == char18)) pMessage = pMessage.substring(1);
			if ((pMessage.charAt(0) == char19)|| (pMessage.charAt(0) == char18)) pMessage = pMessage.substring(1);
			if (mLogAll) Logger.d(pMessage);
			if ( pMessage.charAt(0)== '{') {
				parseJSON(pMessage);
			} 
			else {
				processNMEA0183Message( pMessage);
			}
			test = savedTestStatus;
			
		}
		
		private void parseJSON( String data) {
	 		JSONObject aJSONObject = null;
	 		try {
	 			aJSONObject = new JSONObject(data);
	 			if (aJSONObject != null) {
	 				try {
	 					int pgnNumber = aJSONObject.getInt("pgn");
	 					processPGN(aJSONObject);
	 				} catch (JSONException e) {
	 					Log.d(TAG," no pgn " + e.toString());
	 				}
	 			}
	 		} catch (JSONException e ) {
	 			Log.d(TAG," no JSON " + e.toString());
	 		}  
	 	}
		
		private void processPGN (JSONObject aJSONObject){
	 		//System.out.println(aJSONObject.toString());
	 		// Wind data {"timestamp":"2013-06-20-11:23:02.839","prio":"2","src":"115","dst":"255",
	 		// "pgn":"130306","description":"Wind Data","fields":{"SID":"0","Wind Speed":"8.55","Wind Angle":"116.0","Reference":"Apparent"}}
	 		// water depth {"timestamp":"2013-06-20-11:22:57.325","prio":"3","src":"115","dst":"255",
	 		// "pgn":"128267","description":"Water Depth","fields":{"SID":"0","Depth":"1.74","Offset":"-0.001"}}
	 		// speed {"timestamp":"2013-06-20-11:22:57.830","prio":"2","src":"115","dst":"255",
	 		// "pgn":"128259","description":"Speed","fields":{"SID":"0","Speed Water Referenced":"0.00","Speed Water Referenced Type":"-0"}}
	        // GPS Position
	 	    // "pgn":"129029"
	 		//,"description":"GNSS Position Data","fields":{"SID":"14","Date":"2013.07.07", "Time": "10:54:15.03930"
	 		//,"Latitude":"58.7431575","Longitude":"17.0208108","GNSS type":"GPS","Method":"GNSS fix","Integrity":"No integrity checking"
	 		//,"Number of SVs":"9","HDOP":"0.80","PDOP":"-0.01","Reference Station ID":"15"}}
	        // to do
	 		// done  "pgn":"128267","description":"Water Depth", "SID":"7","Depth":"2.80","Offset":"0.000"
	        // done "pgn":"128259","description":"Speed" , "Speed Water Referenced":"0.00" oder "Speed Water Referenced":"0.00","Speed Ground Referenced":"0.22"
	 		// done "pgn":"129029","description":"GNSS Position Data", "Latitude":"51.4427095","Longitude":"06.9606649"
	 		// done wind pgn 130306 Wind data {"SID":"0","Wind Speed":"8.55","Wind Angle":"116.0","Reference":"Apparent"}}
	 		//  "pgn":"130577","description":"Direction Data" , "COG":"116.3","SOG":"0.22"
	 		//  "pgn":"129540","description":"GNSS Sats in View", SID":"64","Sats in View":"4","PRN":"3","Elevation":"70.0"
	 		//	"pgn":"129033","description":"Time & Date", Date":"2013.11.12", "Time": "12:35:57.08690"
	 		//	"pgn":"126992","description":"System Time", "SID":"88","Source":"GPS","Date":"2013.11.12", "Time": "12:35:58.01220"
	 		
	 		//	"pgn":"130310","description":"Environmental Parameters","Water Temperature":"15.72"
	 		//	"pgn":"129044","description":"Datum", "Delta Latitude":"00.0000000"
	 		//	"pgn":"128275","description":"Distance Log", "Log":"259","Trip Log":"259"
	 		//	
	 		//	"pgn":"129025","description":"Position, Rapid Update","Latitude":"51.4427095","Longitude":"06.9606649"
	 		//	"pgn":"262386","description":"Actisense: System status"
	 		//	"pgn":"129283","description":"Cross Track Error", "Navigation Terminated":"Yes"
	 		
	 		//	"pgn":"129026","description":"COG & SOG, Rapid Update","COG":"116.3","SOG":"0.22"
	        

	 		try {
					int pgnNumber = aJSONObject.getInt("pgn");
					//Log.d(TAG," processed PGN:  " + pgnNumber);
					if (pgnNumber == 130306) {
						// Wind
						String fields = aJSONObject.getString("fields");
						if (test)System.out.println("process PGN 130306  "  + fields);
						processPGN130306(fields);
					} else if (pgnNumber ==128267) {
						// Water Depth
						String fields = aJSONObject.getString("fields");
						if (test) System.out.println("process PGN 128267  "  + fields);
						processPGN128267(fields);
					} else if (pgnNumber ==128259) {
					// Speed
						String fields = aJSONObject.getString("fields");
						if (test) System.out.println("process PGN 128259  "  + fields);
						processPGN128259(fields);
					} else if (pgnNumber == 129029 ) {
				    //GPS
						String fields = aJSONObject.getString("fields");
						if (test) System.out.println("process PGN 129029  "  + fields);
						processPGN129029(fields);
					} else if (pgnNumber == 129026 ) {
				    // SOG & COG
						String fields = aJSONObject.getString("fields");
						if (test) System.out.println("process PGN 129026  "  + fields);
						processPGN129026(fields);
					}
					
						else {
						if (test)System.out.println(" unknown pgn found " + pgnNumber);
					}
				} catch (JSONException e) {
					Log.d(TAG," no pgn " + e.toString());
				}
	 	}
		
		private void processPGN129029(String aDataStr){
	 		// GPS Position
	 		// {"timestamp":"2013-07-07-10:54:15.909","prio":"3","src":"160","dst":"255","pgn":"129029"
	 		//,"description":"GNSS Position Data","fields":{"SID":"14","Date":"2013.07.07", "Time": "10:54:15.03930"
	 		//,"Latitude":"58.7431575","Longitude":"17.0208108","GNSS type":"GPS","Method":"GNSS fix","Integrity":"No integrity checking"
	 		//,"Number of SVs":"9","HDOP":"0.80","PDOP":"-0.01","Reference Station ID":"15"}}
	 		JSONObject aJSONObject = null;
	 		
	 		try {
	 			aJSONObject = new JSONObject(aDataStr);
	 			String aLatStr = aJSONObject.getString("Latitude");
	 			//setLATPositionInGUI(aLatStr);
	 			String aLonStr = aJSONObject.getString("Longitude");
	 			//setLONPositionInGUI(aLonStr);
	 			Double aLAT = Double.parseDouble(aLatStr);
				Double aLON = Double.parseDouble(aLonStr);
	 			setPositionOfOwnShipFromPGN129029(aLAT, aLON);
	 		} catch (JSONException e) {
				Log.d(TAG," processPGN129029 " + e.toString());
			}
	 	}
	 	
	 	private void processPGN130306(String aDataStr){
	 		// wind
	 	    // Wind data {"SID":"0","Wind Speed":"8.55","Wind Angle":"116.0","Reference":"Apparent"}}
	 		JSONObject aJSONObject = null;
	 		
	 		try {
	 			aJSONObject = new JSONObject(aDataStr);
	 			String windSpeedStr = aJSONObject.getString("Wind Speed");
	 			 //setWindSpeedInGUI(windSpeedStr);
	 			lastWindSpeed = Double.parseDouble(windSpeedStr);
	 			
	 		} catch (JSONException e) {
	 			Log.d(TAG," processPGN130306 " + e.toString());
			}
	 		try {
	 			aJSONObject = new JSONObject(aDataStr);
	 			
	 			String aDirectionString = aJSONObject.getString("Wind Angle");
	 			//setRelWindDirectionInGUI(aDirectionString);
	 			lastWindAngle = Double.parseDouble(aDirectionString);
	 		} catch (JSONException e) {
	 			Log.d(TAG," processPGN130306 " + e.toString());
			}
	 		
	 	}
	 	
	 	private void processPGN128267(String aDataStr){
	 	    // water depth {"SID":"0","Depth":"1.74","Offset":"-0.001"}}
	 		JSONObject aJSONObject = null;
	 		try {
	 			aJSONObject = new JSONObject(aDataStr);
	 			String depthStr = aJSONObject.getString("Depth");
	 			//setDepthInGUI(depthStr);
	 			lastDepth = Double.parseDouble(depthStr);
	 		} catch (JSONException e) {
	 			Log.d(TAG," processPGN128267" + e.toString());
			}
	 	}
	 	
	 	private void processPGN128259(String aDataStr){
	 		// speed {"SID":"0","Speed Water Referenced":"0.00","Speed Water Referenced Type":"-0"}}
	        // sometimes the JSON does not contain a Speed Ground Referenced  or the SPPED Water Referenced field
	 		// this is not an error, so we do not log it
	 		JSONObject aJSONObject = null;
	 		try {
	 			aJSONObject = new JSONObject(aDataStr);
	 			String speedStr = aJSONObject.getString("Speed Water Referenced");
	 			// we will get a double, but we need a int * 10 in lastSpeedThruWater
	 			Double aSpeedThruWater = Double.parseDouble(speedStr) * 10d;
	 			lastSpeedThruWater = (int)Math.round(aSpeedThruWater);
	 			//setSpeedThruWaterInGUI(speedStr); 
	 			
	 		} catch (JSONException e) {
	 			if (test) Log.d(TAG," processPGN128259 " + e.toString()); 
			}
	 		try {
	 			aJSONObject = new JSONObject(aDataStr);
	 			String speedStr = aJSONObject.getString("Speed Ground Referenced");
	 		// we will get a double, but we need a int * 10 in last SOG
	 			//setSpeedOverGroundInGUI(speedStr); 
	 			Double aSOG = Double.parseDouble(speedStr) * 10d;
	 			lastSOG = (int)Math.round(aSOG);
	 		} catch (JSONException e) {
	 			if (test) Log.d(TAG," processPGN128259 " + e.toString()); 
			}
	 	}
	 	
	 	private void processPGN129026(String aDataStr){
	 		// COG & SOG Rapid update ":{"SID":"66","COG Reference":"True","COG":"116.3","SOG":"0.22"}}
	 
	 		JSONObject aJSONObject = null;
	 		try {
	 			aJSONObject = new JSONObject(aDataStr);
	 			String cogStr = aJSONObject.getString("COG");
	 			//setCourseOverGroundInGUI(cogStr); 
	 			
	 		} catch (JSONException e) {
	 			Log.d(TAG," processPGN129026" + e.toString()); 
			}
	 		
	 	}
		
		
		/**
		 * 
		 * @param aNMEAString
		 *        a NMEA-sentence 
		 * @return  the checksum which is a two-chars string
		 *         
		 */
	   	
	   	private String getNMEAChecksum(String aNMEAString) {
	   		int start = 1;
	   	    if (aNMEAString.indexOf('$') > 0 ) start = aNMEAString.indexOf('$') +1;  // $GPGSV V.Klein 13_08_20
	   	    if (aNMEAString.indexOf('!') > 0 ) start = aNMEAString.indexOf('!') +1;  // !AIVDM
	   	    int end = aNMEAString.indexOf('*') -1 ;
	   	    char ch = aNMEAString.charAt(start);
	   	    int checksum = (int) ch;
	   	    for (int i= start+1;i<=end;i++) {
	   	    	ch = aNMEAString.charAt(i);
	   	    	checksum = checksum ^ (int) ch;
	   	    }
	   	    String result = Integer.toHexString(checksum); 
	   	    if (checksum < 16) result = "0" + result; 
	   	    return result;
	   	}
	   	
	   	/**
	   	 *  checks if the nmea-sentence has a valid checksum
	   	 * @param aNMEAString
	   	 * @return true if checksum is valid
	   	 */
	   	private boolean checkSumValid (String aNMEAString) {
	   		if (test) Log.d(TAG,"checksumvalid " + aNMEAString);
	   		int index = aNMEAString.indexOf('*');
	   		if (index < 0 ) 
	   			{
	   			 if (test)Log.d(TAG," no checksum found . " + aNMEAString);
	   			 return false;
	   			} 
	   		String checksumString = aNMEAString.substring(index +1);
	   		String calculatedString = getNMEAChecksum(aNMEAString).toUpperCase();
	   		boolean result = checksumString.equals(calculatedString);
	   		if (result == false) {
	   			//if (test)
	   				Log.d(TAG,"Checksum " + checksumString + " calc. Checksum "  + calculatedString);
	   		}
	   		return result;
	   	}
	   	
	   	private String removeLeadingTimeStamp(String pMessage){
	   	// 2013-07-31 00:34:00: $GPGGA,151453.49,5631.101,N,01616.865,E,1,09,0.80,6.49,M,,M,,*78
        // 2013-07-31 00:34:00: !AIVDM,1,1,,A,D02R3T16DO6DU8Nfp0,4*2A
        // the data payload begins with a $ or a !
	   		String result="";
	   		if (pMessage != null){
		   		int strLength = pMessage.length();
		   		int markerDollar = pMessage.indexOf("$");
		   		
		   		int markerBeginNMEAMsg = -1;
		   		if (markerDollar > -1 && markerDollar < strLength) {
		   			markerBeginNMEAMsg = markerDollar;
		   			result = pMessage.substring(markerBeginNMEAMsg);
		   			return result;
		   		}
		   		int markerExcl = pMessage.indexOf("!");
		   		if (markerExcl > -1 && markerExcl < strLength) {
		   			markerBeginNMEAMsg = markerExcl;
		   			result = pMessage.substring(markerBeginNMEAMsg);
		   			return result;
		   		}
	   		}
	   		return result;
	   	}
		
	   	/**
	   	 * process a valid NMEA-Message
	   	 *    we process VDM, RMC and GGA messages
	   	 * @param pMessage
	   	 */
		private void processNMEA0183Message(String pMessage ) {
			
		  if (test) 
				Log.d(TAG, "process NMEA Message " + pMessage);
			// remove blanks in front
			while (( pMessage.length() > 0 ) &&( pMessage.charAt(0) == ' ')){
				pMessage = pMessage.substring(1); 
			}
			/*while (( pMessage.length() > 0 ) &&( pMessage.charAt(0) != '!')){
				pMessage = pMessage.substring(1); 
			}*/
			// remove a possible timestamp from a transmitted  log file  13_12_11
			// 2013-07-31 00:34:00: $GPGGA,151453.49,5631.101,N,01616.865,E,1,09,0.80,6.49,M,,M,,*78
            //pMessage = removeLeadingTimeStamp(pMessage);
			pMessage = removeWrongChars(pMessage);// only chars from  ascii 32 to z
			if (checkSumValid(pMessage)){
				// remove the checksum from the message
				int checksumMarker = pMessage.indexOf("*");
				pMessage = pMessage.substring(0,checksumMarker);
				String[] fields = pMessage.split(",");
				if (test) Log.d(TAG, "process Message Fields " + fields[0] + " " + fields[1] + " " + fields[2]);
				if (fields[0].indexOf("VDM") == 3){
				    if (test) Log.d(TAG, "process Message  call VDMMess ");
					processVDMMessage(pMessage);
				}
				if (fields[0].indexOf("RMC") == 3) {
					if (test) Log.d(TAG, "process NMEAMessage  call RMCMess ");
					
					if (mUseGPSSource == AISPlotterGlobals.NMEA_GPS)decodeRMC_Message(pMessage); //  19.8.2011 new 28.2.12
				}
				if (fields[0].indexOf("GGA") == 3) {
					if (test) Log.d(TAG, "process NMEAMessage  call GGAMess ");
					// cause there is not gps-service on the table we handle GGA
					
					if (mUseGPSSource == AISPlotterGlobals.NMEA_GPS)decodeGGA_Message(pMessage); // 19.8.2011 new 18.11.2011
				}
				
				if (fields[0].indexOf("GLL") == 3) {
					if (test) Log.d(TAG, "process NMEAMessage  call GLLMess ");
					// cause there is not gps-service on the table we handle GGA
					
					if (mUseGPSSource == AISPlotterGlobals.NMEA_GPS)decodeGLL_Message(pMessage); // new in 2013_12_12
				}
				if (fields[0].indexOf("MWV") == 3) {
					if (test) Log.d(TAG, "process NMEAMessage  call MWVMess ");
					
					// since we get gps from the service we do not handle RMC
					if (mUseGPSSource == AISPlotterGlobals.NMEA_GPS)decodeMWV_Message(pMessage); //  new in 13_12_10
				}
				if (fields[0].indexOf("DBT") == 3) {
					if (test) 
						Log.d(TAG, "process NMEAMessage  call DBTVMess ");
					
					// since we get gps from the service we do not handle RMC
					if (mUseGPSSource == AISPlotterGlobals.NMEA_GPS)decodeDBT_Message(pMessage); //  new in 13_12_10
				}
				
			} else {
				if (test) Log.v(TAG,"Checksum invalid" + pMessage);
				Logger.d("invalid checksum msg skipped " + pMessage);
			}
		}
		
		/**
		 * 
		 * @param aMsg
		 * @return a string that contains only the valid chars from  ascii 32 to z
		 */
		private String removeWrongChars (String aMsg) {
		 String result = "";
		  for (int i = 0;i<aMsg.length();i++) {
			  if ((aMsg.charAt(i)> ' ') && (aMsg.charAt(i) <= 'z')) {
				   result = result + aMsg.charAt(i);
			  }
		  }
		  return result;
		  
		}
		/**
		 * process a valid VDMMessage
		 * @param pMessage
		 */
		public void processVDMMessage(String pMessage) {
	/*     do we have a AIVDM Message and the follow message ??
		   z.B.
		   !AIVDM,2,1,3,B,539LnWP00000@8TL00084B0ADD8Dp000000000000orv240Ht00000000000,0*0D
		   !AIVDM,2,2,3,B,00000000008,2*2C

		   AIVDM/AIVDO Sentence Layer
		   AIVDM/AIVDO is a two-layer protocol. The outer layer is a variant of NMEA 0183, the ancient standard for data interchange in marine navigation systems; NMEA 0183 is described at [NMEA].
		   Here is a typical AIVDM data packet:
		   !AIVDM,1,1,,B,177KQJ5000G?tO`K>RA1wUbN0TKH,0*5C
		   And here is what the fields mean:
		   Field 0, !AIVDM, identifies this as an AIVDM packet.
		   Field 1 (1 in this example) is the count of fragments in the currently accumulating message. The payload size of each sentence is limited by NMEA 0183’s 82-character maximum, so it is sometimes required to split a payload over several fragment sentences.
		   Field 2 (1 in this example) is the fragment number of this sentence. It will be one-based. A sentence with a fragment count of 1 and a fragment number of 1 is complete in itself.
		   Field 3 (empty in this example) is a sequential message ID for multi-sentence messages.
		   Field 4 (B in this example) is a radio channel code. AIS uses the high side of the duplex from two VHF radio channels: AIS Channel A is 161.975Mhz (87B); AIS Channel B is 162.025Mhz (88B).
		   Field 5 (177KQJ5000G?tO`K>RA1wUbN0TKH in this example) is the data payload. We’ll describe how to decode this in later sections.
		   Field 6 (0) is the number of fill bits requires to pad the data payload to a 6 bit boundary, ranging from 0 to 5.
		   The *-separated suffix (*5C) is the NMEA 0183 data-integrity checksum for the sentence, preceded by "*". It is computed on the entire sentence including the AIVDM tag but excluding the leading "!".
	       For comparison, here is an example of a multifragment sentence with a nonempty message ID field:
	       !AIVDM,2,1,3,B,55P5TL01VIaAL@7WKO@mBplU@<PDhh000000001S;AJ::4A80?4i@E53,0*3E
	       !AIVDM,2,2,3,B,1@0000000000000,2*55
	       we have to test, if the two chunks in the field 5 belong to another
	       then we can put them together.
	       
           The problem is that possibly we lost the second part of the message, cause one 
           of the members of the data-processing chain is to slow.
           Especially the telnet-server on the arduino may loose the second sentence, 
           if they follow without delay. This problem occurs when the Arduino reads
           with the software UART, which has no data buffer like the hardware UART.
	       */
		  // we do it here without ChecksumCheck
			
			if (test) 
				Log.d(TAG, "process VDM Message " + pMessage);
			String aVDMString = "";
			String[] fields = pMessage.split(",");
			if ((fields[1].equals("1"))&& (fields[2].equals("1"))){  
				// message complete
				aVDMString = fields[5]; // data payload
			}
			if (fields[1].equals("2")){
				//Logger.d(TAG,"Combined MSG " + pMessage);
				// this is a combined Message
				if  (fields[2].equals("1")) {
					 //first part , throw away garbage on the stack
					while  (!mNEMEAStack.isEmpty()) mNEMEAStack.pop();
					// and push new message
					// the field 5 may contain trailing chars 
					// we check field 6, is contains the number before the *
					// we had a severe fault here, found 13_12_17 by simulating with gpsfake
					// the * is put aside in process NMEAMessage
					String aIndexStr = fields[6];
					//int index = aStr.indexOf('*');
					//Log.d(TAG,"VDM MSG " + pMessage + "field6 " + aIndexStr);
					int numberOfFillBits = -1;
					try {
						numberOfFillBits = Integer.parseInt(aIndexStr);
					} catch (Exception e) {
						
					}
					if (numberOfFillBits <0 || numberOfFillBits > 5 ){
						Logger.d(TAG,"wrong payload fill number " + numberOfFillBits);
						return;
					}
					//String aIndexStr = aStr.substring(0,index);
					if (numberOfFillBits == 0) {
					    mNEMEAStack.push(fields[5]);
					}
					else {
						
						// delete the trailing chars
						Logger.d(TAG," trailing chars " + fields[5] + " " + fields[6]);
						//int index = Integer.parseInt(aIndexStr);
						String dataload = fields[5].substring(0, fields[5].length() - numberOfFillBits);
						Logger.d(TAG," dataload " + fields[5] + " " + dataload);
						mNEMEAStack.push(dataload);
					}
					if (test) Log.d(TAG,"first part " + fields[5] + " " + "trailing " + aIndexStr);
					//mLogAll = true;
					return;
				} else {
					if (test)Log.d(TAG,"second part "+ pMessage);
					// second Part of the message
				   if (!mNEMEAStack.isEmpty()) {
					  String oldMessage = mNEMEAStack.pop();
					  aVDMString = oldMessage + fields[5]; //old and new payload
					  if(test)Log.d(TAG,"found combined message");
					  if (test) Log.d(TAG,"complete MSG " + oldMessage + " " + fields[5]);
					  //mLogAll = false;
				   }
			    }
		    }
			// we put the chunks of the VDM message together , now we can decode
			decodeVDMString(aVDMString) ;
		    if (test) Log.d(TAG, "process VDMMessage end " + aVDMString);
		} 
		
		// the VDM String is complete, so we decode it to a sixBitString 
		// the next step is to decode the sixBitMessage with decode6BitMsg
		

		/**
		 * Decode a VDM Message
		 * @param aVDMString
		 */
		public void decodeVDMString (String aVDMString) {
			if (test) Log.d(TAG, "decodeVDMString " + aVDMString);
			if (test) Log.d(TAG,  "decodeVDMString Laenge " + aVDMString.length());
			StringBuilder aBuf = new StringBuilder();
			int aLength = aVDMString.length();
			for (int i= 0;i<aLength ;i++){
			   aBuf.append(asciiTo6Binary(aVDMString.charAt(i)));	
			}
			String sixBitString = aBuf.toString();
			if (test) Log.d(TAG, "decodeVDMString " + sixBitString);
			if (test) Log.d(TAG, "decodeVDMString Laenge " + sixBitString.length());
			long oldMilliseconds = System.currentTimeMillis(); 
			decode6BitMsg(sixBitString);
			long newMillis =System.currentTimeMillis();
			long diffmilli =newMillis - oldMilliseconds ;
			if (test)Log.v(TAG," decoding VDM message  used miliiseconds: " + diffmilli);
		}
		
		
        /** convert a Ascii-char to a six-bit-binary-string
         * @param aChar
         * @return the six-bit-binary-string
         */
		public String asciiTo6Binary(char aChar) {
			/* what is sixBit
			 * 48 = '0'	87 = 'W'	96 = '´'	119 = 'w'	
				000000	100111	101000	111111	
				0	      39	  40	  63	
			*/
			String result = "";
			if (aChar < '0') return result;
			if (aChar > 'w') return result;
			if (aChar > 'W') {
				char ch1 =(char) (64+32);
				if (aChar < ch1) return result;
	        }
			int sum = (int) aChar + 32 + 8; // add 101000
			if (sum > 128) {
					sum = sum + 32;
			} else {
					sum = sum+ 8 + 32;
			}
			for (int i = 1;i <= 6;i++) {
				if ((sum % 2) == 1 ) {
						result = "1" + result;
				}else {
						result = "0" + result;
				}
				sum = sum / 2;
			}
			return result;
		}
		
		/**
		 *  decode the sixBitString, extract first the Message ID
		 * @param aSixBitString
		 */
		
	    public void decode6BitMsg (String aSixBitString) {
	    	// the msgID value determinates what kind of message we have
		    // msgID == 1,2,3 is a Class A position report
	    	// msgID == 4 Base station report
		    // msgID == 5 is a Class A ship report
	    	// msgID == 8 binary message
	    	// msgID == 18,19 is a Class B position report
	    	// msgID == 24 is a Class B ship report
	    	if (test)Log.v(TAG,"aisplotter --> decode6BitMsg begin");
	    	if (aSixBitString.length() < 6 ) return;
	    	String tempstr = aSixBitString.substring(0,6);
	    	long msgIDValue = bitStringToLong(tempstr);
	    	if (test) Log.v(TAG,"aisplotter --> decode6BitMsg " + aSixBitString);
	    	if (test) Log.v(TAG,"aisplotter --> decode6BitMsg msgID=" + msgIDValue);
	    	String trailer = "Decode Msg " + msgNr + " ";
	    	// some msg with id 3 will not be decoded correctly
	        if (( msgIDValue == 1)|| (msgIDValue == 2 )|| (msgIDValue == 3 )) {  // 
	        	// msg 1,2,(3)
	        	//Logger.d(TAG,trailer + msgIDValue );
	        	if (aSixBitString.length() == 168) decodeMsg_123(aSixBitString);
	        	else if (test) Log.v(TAG,"aisplotter --> decode6BitMsg: lenght <> 168");
	        }
	        if (msgIDValue == 4) {
	        	if (mLogParsing)Logger.d(TAG,trailer + " Base station report");
	        	decodeMsg_4(aSixBitString);
	        	// msg 4
	        }
	        if ((msgIDValue == 5)) {
	        	if (mLogParsing)Logger.d(TAG,trailer + msgIDValue);
	        	// msg 5
	        	if (aSixBitString.length() == 426) decodeMsg_5( aSixBitString);
	        	else if (test) Log.v(TAG,"aisplotter --> decode6BitMsg: lenght <> 426");
	        }
	        
	        if (msgIDValue == 8) {
	        	if (mLogParsing)Logger.d(TAG,trailer + " binary Message");
	        	//msg 8
	        }
	        if (msgIDValue == 11) {
	        	if (mLogParsing)Logger.d(TAG,"Decode Msg "+ msgNr + " " + msgIDValue + " utc response");
	        	//msg 11
	        }
	        if (msgIDValue == 15) {
	        	if (mLogParsing)Logger.d(TAG,trailer + " interrogation");
	        	// msg 15
	        }
	        if  ((msgIDValue == 18 )|| (msgIDValue == 19 )){
	        	//Logger.d(TAG,"Decode Msg "+ msgNr + " " + msgIDValue + " Class B position report");
	        	decodeMsg_1819 (aSixBitString);
	        	// msg 18,19
	        }
	        if  (msgIDValue == 20 ){
	        	if (mLogParsing)Logger.d(TAG,trailer + "  data link message");
	        	// msg 20
	        }
	        if (msgIDValue == 24) {
	        	if (mLogParsing)Logger.d(TAG,trailer + " static data report");
	        	decodeMsg_24 (aSixBitString);
	        	// msg 24
	        }
	        
	    }
	    
	    /**
	     * extract the long value from the bitstring
	     * @param aBitString
	     * @return the long value form the BitString
	     */
	    private long bitStringToLong(String aBitString) {
	    	long value = 0;
	    	for (int i = 0;i< aBitString.length();i++) {
	    		if (aBitString.charAt(i) =='1') {
	    			value = value  * 2 +1; 
	    		} else {
	    			value = value * 2;
	    		}
	    	}
	    	return value;
	    }
	    
	    /**
	     * Decode a Class A position report (msg 123)
	     * 
	     * @param aSixBitString
	     */
	    
	    public void decodeMsg_123 (String aSixBitString){
	    	 // 18 m ??
	    	// we extract the MMSI of the target first.
	    	// then we have a look on the AISTarget list
	    	// if we found the MMSI , we update, else we create a new entry
	    	// if we should save, we update the database
	    	// we send an updateRequest to the Gui
	    	String name = "";
	    	boolean savedStatus = test;
	    	//test = true;
	    	if (test)Log.v(TAG,"NMEAParser --> decodeMsg_123 begin");
	    	nrOfInfo ++;
	    	String tempstr = aSixBitString.substring(0,6);
	    	long msgIDValue = bitStringToLong(tempstr);
	    	tempstr = aSixBitString.substring (8,38);
	    	long aMMSI = bitStringToLong(tempstr);
	    	AISTarget aTarget = mTargetList.findTargetByMMSI(aMMSI);
	    	if (aTarget == null) {
	    		AISTarget newTarget = new AISTarget(-1);
	    		newTarget.setInfoFromPositionReport123(aSixBitString);
	    		newTarget.setStatusToDisplay(AISPlotterGlobals.DISPLAYSTATUS_HAS_POSITION);
	    		newTarget.setTimeOfLastPositionReport(mCurrentNmeaTime);
	    		newTarget.setTimeOfLastStaticUpdate(mCurrentNmeaTime);
	    		name = newTarget.getShipname();
	    		if (mLogParsing)Logger.d("first Position report Type"+ msgIDValue + " " + name + " " + aMMSI);
	    		mTargetList.add(newTarget);
	    		sendGUIUpdateEvent(newTarget);
	    		try {
	    		  mDbAdapter.createTarget(newTarget);
	    		}
	    		catch (SQLException e){
	    			e.printStackTrace();
	    		}
	    		
	    	}else {
	    		// remember the old position
	    		// do only update if the position differs more then 
	    		// aThousandOfMile or the COG differs more than 5 degrees
	    		boolean haveToSendGUIUpdate = false;
	    		double aOldLAT = aTarget.getLAT();
	    		double aOldLON = aTarget.getLON();
	    		int aOldCOG = aTarget.getCOG();
	    		
	    		aTarget.setInfoFromPositionReport123(aSixBitString);
	    		// Did  we get a new position?
	    		if (Math.abs(aTarget.getLAT() - aOldLAT) > aThousandOfMile ){
	    			haveToSendGUIUpdate = true;
	    		}
	    		if (Math.abs(aTarget.getLON() - aOldLON) > aThousandOfMile ){
	    			haveToSendGUIUpdate = true;
	    		}
	    		if (Math.abs(aTarget.getCOG()- aOldCOG) > 50 )  {// more than 5 degree difference ,COG is mesured in degree * 10
	    			haveToSendGUIUpdate = true;
	    		}
	    		String aMMSIStr = Long.toString(aMMSI);
	    		if (aMMSIStr.startsWith("970")) {
	    			haveToSendGUIUpdate = true;	
	    		}
	    		if (aMMSIStr.startsWith("235")) {
	    			haveToSendGUIUpdate = true;	
	    		}
	    		
	    		aTarget.setTimeOfLastPositionReport(mCurrentNmeaTime);
	    		name = aTarget.getShipname();
	    		// if we are marked as inactive change status to has_position
	    		if (aTarget.getStatusToDisplay() == AISPlotterGlobals.DISPLAYSTATUS_INACTIVE ) {
	    			if (aTarget.getShipname().equals("")) {
	    				aTarget.setStatusToDisplay(AISPlotterGlobals.DISPLAYSTATUS_HAS_POSITION);
	    			} else {
	    				aTarget.setStatusToDisplay(AISPlotterGlobals.DISPLAYSTATUS_HAS_SHIPREPORT);
	    			}
	    			haveToSendGUIUpdate = true;
	    			if (mLogParsing)Logger.d("Status set to has_position " + msgIDValue + " " + name + " " + aMMSI);
	    		   haveToSendGUIUpdate = true;
	    		}
	    		
	    		if (mSavePositionData){
	    			if (mLogPositionData) Logger.d("next Position report Type " + msgIDValue + " " + name + " " + aMMSI);
		    		try {
		    		   mDbAdapter.updateTarget(aTarget);
		    		}
		    		catch (SQLException e){
		    			Logger.d("update target failed " );
		    			e.printStackTrace();
		    		}
		    	}
	    		if (haveToSendGUIUpdate) {
	    			sendGUIUpdateEvent(aTarget);
	    		}
	    	}
	    
	    	if (test) Log.d(TAG,"MSG 123  Target " + name);
	    	test = savedStatus;
	    }

	    /**
	     *  Decode a base station report
	     * @param aSixBitString
	     */
	    public void decodeMsg_4 (String aSixBitString){
	    	String name = "";
	    	boolean savedStatus = test;
	    	//test = true;
	    	if (test)Log.v(TAG,"NMEAParser --> decodeMsg_4 begin");
	    	nrOfInfo ++;
	    	String tempstr = aSixBitString.substring(0,6);
	    	long msgIDValue = bitStringToLong(tempstr);
	    	tempstr = aSixBitString.substring (8,38);
	    	long aMMSI = bitStringToLong(tempstr);
	    	AISTarget aTarget = mTargetList.findTargetByMMSI(aMMSI);
	    	if (aTarget == null) {
	    		AISTarget newTarget = new AISTarget(-1);
	    		newTarget.setInfoFromBaseStationReport(aSixBitString);
	    		newTarget.setStatusToDisplay(AISPlotterGlobals.DISPLAYSTATUS_BASE_STATION);
	    		newTarget.setTimeOfLastPositionReport(mCurrentNmeaTime);
	    		newTarget.setTimeOfLastStaticUpdate(mCurrentNmeaTime);
	    		name = newTarget.getShipname();
	    		if (mLogParsing)Logger.d("first Base Station report Type"+ msgIDValue + " " + name + " " + aMMSI);
	    		mTargetList.add(newTarget);
	
	    		sendGUIUpdateEvent(newTarget);
	    		try {
	    		  mDbAdapter.createTarget(newTarget);
	    		}
	    		catch (SQLException e){
	    			Logger.d("create target failed " );
	    			e.printStackTrace();
	    		}
	    	}else {
	    		double aOldLAT = aTarget.getLAT();
	    		double aOldLON = aTarget.getLON();
	    		boolean haveToSendGUIUpdate = false;
	    		aTarget.setInfoFromBaseStationReport(aSixBitString);
	    		aTarget.setStatusToDisplay(AISPlotterGlobals.DISPLAYSTATUS_BASE_STATION);
	    		aTarget.setTimeOfLastPositionReport(mCurrentNmeaTime);
	    		aTarget.setTimeOfLastStaticUpdate(mCurrentNmeaTime);
	    		name = aTarget.getShipname();
	    		// Did  we get a new position?
	    		if (Math.abs(aTarget.getLAT() - aOldLAT) > aThousandOfMile ){
	    			haveToSendGUIUpdate = true;
	    		}
	    		if (Math.abs(aTarget.getLON() - aOldLON) > aThousandOfMile ){
	    			haveToSendGUIUpdate = true;
	    		}
	    		
	    		
	    		if (haveToSendGUIUpdate) {
	    			sendGUIUpdateEvent(aTarget);
	    		}
	    		
	    		if (mSavePositionData) {
	    			if (mLogPositionData) Logger.d("next Base Station report Type " + msgIDValue + " " + name + " " + aMMSI);
		    		try {
		    		  mDbAdapter.updateTarget(aTarget);
		    		}
		    		catch (SQLException e){
		    			Logger.d("update target failed " );
		    			e.printStackTrace();
		    		}
	    		}
	    	}
	    	if (test) Log.d(TAG,"MSG 4  Target " + name);
	    	test = savedStatus;
	    	
	    }
	    
	   
	    /**
	     * DEcode a base station report and write the info to the log
	     * @param sixBitString
	     */
	    public void decodeMsg_4_andLog (String sixBitString){
	    	// base station report
	    	if (test)Log.v(TAG,"aisplotter --> decodeMsg_4 begin");
	    
        	if (!(sixBitString.length() == 168)) return;
        	String tempstr = "";
        	tempstr = sixBitString.substring (0,6);
            int theLastMSGType = (int) bitStringToLong(tempstr);
            tempstr = sixBitString.substring (6,8);
            int theRepeatIndicator = (int) bitStringToLong(tempstr);
            tempstr = sixBitString.substring (8,38);
            long theMMSI = bitStringToLong(tempstr);
            tempstr = sixBitString.substring (38,52);
            int aUTCYear = (int )bitStringToLong(tempstr);
            tempstr = sixBitString.substring (52,56);
            int aUTCMonth = (int )bitStringToLong(tempstr);
            tempstr = sixBitString.substring (56,61);
            int aUTCDay = (int )bitStringToLong(tempstr);
            tempstr = sixBitString.substring (61,66);
            int aUTCHour = (int )bitStringToLong(tempstr);
            tempstr = sixBitString.substring (66,72);
            int aUTCMinute = (int )bitStringToLong(tempstr);
            tempstr = sixBitString.substring (72,78);
            int aUTCSecond = (int )bitStringToLong(tempstr);
            tempstr = sixBitString.substring (78,79);
            Logger.d(TAG,"baseStation " + theMMSI);
            long aTime = 0;
			try {
	            Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT")); 
				cal.set(aUTCYear, aUTCMonth, aUTCDay, aUTCHour, aUTCMinute, aUTCSecond); 
				aTime = cal.getTime().getTime(); 
				// we doo nothing, cause the time comes from 
				// the currentmillis mCurrentUTCTime = aTime;
            } catch (Exception e) {
				
			}
            Logger.d(TAG,"Date " + aUTCHour + ":" + aUTCMinute + ":" + aUTCSecond );
            byte theAccuracy = (byte) bitStringToLong(tempstr);
            tempstr = sixBitString.substring (79,107);
            double theLON =  ((double)bitStringToLong(tempstr))/10000/60;
            tempstr = sixBitString.substring (107,134);
            double theLAT =  ((double)bitStringToLong(tempstr))/10000/60;
            tempstr = sixBitString.substring (134,138);
            int aTypeOfFixing =  (int)bitStringToLong(tempstr);
	        
	    }
	    
	    /**
	     * decode a Class B position report
	     * @param aSixBitString
	     */
	    public void decodeMsg_1819 (String aSixBitString){
	    	// position report
	    	if (test)Log.v(TAG,"aisplotter --> decodeMsg_18_19 begin");
	    	nrOfInfo ++;
	    	String tempstr = aSixBitString.substring(0,6);
	    	long msgIDValue = bitStringToLong(tempstr);
	    	tempstr = aSixBitString.substring (8,38);
	    	long aMMSI = bitStringToLong(tempstr);
	    	String name = "";
	    	AISTarget aTarget = mTargetList.findTargetByMMSI(aMMSI);
	    	if (aTarget == null) {
	    		AISTarget newTarget = new AISTarget(-1);
	    		newTarget.setInfoFromPositionReport_18_19(aSixBitString);
	    		newTarget.setStatusToDisplay(AISPlotterGlobals.DISPLAYSTATUS_HAS_POSITION);
	    		newTarget.setTimeOfLastPositionReport(mCurrentNmeaTime);
	    		newTarget.setTimeOfLastStaticUpdate(mCurrentNmeaTime);
	    		name = newTarget.getShipname();
	    		if (mLogParsing)Logger.d("first Position report Type"+ msgIDValue + " " + name + " " + aMMSI);
	    		mTargetList.add(newTarget);
	    		sendGUIUpdateEvent(newTarget);
	    		try {
	    		  mDbAdapter.createTarget(newTarget);
	    		}
	    		catch (SQLException e){
	    			Logger.d("create target failed " );
	    			e.printStackTrace();
	    		}
	    	}else {
	    		// remember the old position and COG
	    		double aOldLAT = aTarget.getLAT();
	    		double aOldLON = aTarget.getLON();
	    		int aOldCOG = aTarget.getCOG();
	    		boolean haveToSendGUIUpdate = false;
	    		aTarget.setInfoFromPositionReport_18_19(aSixBitString);
	    		long oldTime = aTarget.getTimeOfLastPositionReport();
	    	    aTarget.setTimeOfLastPositionReport(mCurrentNmeaTime);
	    	    name = aTarget.getShipname();
	    	    // if we are marked as inactive change status to has_position
	    		if (aTarget.getStatusToDisplay() == AISPlotterGlobals.DISPLAYSTATUS_INACTIVE ) {
	    			if (aTarget.getShipname().equals("")) {
	    			    aTarget.setStatusToDisplay(AISPlotterGlobals.DISPLAYSTATUS_HAS_POSITION);
	    			} else {
	    				aTarget.setStatusToDisplay(AISPlotterGlobals.DISPLAYSTATUS_HAS_SHIPREPORT);
	    			}
	    			if (mLogParsing)Logger.d("Status set to has_position " + msgIDValue + " " + name + " " + aMMSI);
	    		}
	    		
	    		if (mSavePositionData) {
	    			if (mLogPositionData)Logger.d("next Position report Type " + msgIDValue + " " + name + " " + aMMSI);
		    		try {
		    		  mDbAdapter.updateTarget(aTarget);
		    		}
		    		catch (SQLException e){
		    			Logger.d("update target failed " );
		    			e.printStackTrace();
		    		}
	    		}
	    		
	    		// do only update if the position differs more than 
	    		// aThousandOfMile or the COG differs more than 5 degrees
	    		
	    		if (Math.abs(aTarget.getLAT() - aOldLAT) > aThousandOfMile ){
	    			haveToSendGUIUpdate = true;
	    		}
	    		if (Math.abs(aTarget.getLON() - aOldLON) > aThousandOfMile ){
	    			haveToSendGUIUpdate = true;
	    		}
	    		if (Math.abs(aTarget.getCOG()- aOldCOG) > 50 )  {// more than 5 degree difference ,COG is mesured in degree * 10
	    			haveToSendGUIUpdate = true;
	    		}
	    		if (haveToSendGUIUpdate) {
	    			sendGUIUpdateEvent(aTarget);
	    		}
	    	}
	    }
	    
	    /**
	     * Decode a Class A ship report
	     * @param aSixBitString
	     */
	    public void decodeMsg_5 (String aSixBitString){
	       boolean savedStatus = test;
	       //test = true;
	       if (test) Log.v(TAG,"NMEA-Parser --> decodeMsg_5 begin");
	       String name ="";
	       String tempstr = aSixBitString.substring (8,38);
	       long aMMSI = bitStringToLong(tempstr);
	       nrOfTarget ++;
	       AISTarget aTarget = mTargetList.findTargetByMMSI(aMMSI);
		   	if (aTarget == null) {
		   		AISTarget newTarget = new AISTarget(-1);
		   		
		   		newTarget.setInfoFromClassAShipReport(aSixBitString);
		   		newTarget.setStatusToDisplay(AISPlotterGlobals.DISPLAYSTATUS_HAS_SHIPREPORT);
		   		newTarget.setTimeOfLastPositionReport(mCurrentNmeaTime);
		   		newTarget.setTimeOfLastStaticUpdate(mCurrentNmeaTime);
		   		name = newTarget.getShipname();
		   	    if (test) Log.d(TAG," first ship report " + name + " " + PositionTools.getTimeString(mCurrentNmeaTime));
		   	    if (mLogPositionData)Logger.d(" first ship report " + name + " " + PositionTools.getTimeString(mCurrentNmeaTime));
		   	    mTargetList.add(newTarget);
		   	    sendGUIUpdateEvent(newTarget);
		   	    try {
		   	      mDbAdapter.createTarget(newTarget);
		   	    }
		   	 catch (SQLException e){
		   		    Logger.d("create target failed " );
	    			e.printStackTrace();
	    		}
		   	}else {
		   		double aOldLAT = aTarget.getLAT();
	    		double aOldLON = aTarget.getLON();
	    		int aOldCOG = aTarget.getCOG();
	    		boolean haveToSendGUIUpdate = false;
		   	    name = aTarget.getShipname();
		   		aTarget.setInfoFromClassAShipReport(aSixBitString);
		   		if (aTarget.getStatusToDisplay() != AISPlotterGlobals.DISPLAYSTATUS_SELECTED)
		   		    aTarget.setStatusToDisplay(AISPlotterGlobals.DISPLAYSTATUS_HAS_SHIPREPORT);
		   		long lastUpdate = aTarget.getTimeOfLastStaticUpdate();
		   		aTarget.setTimeOfLastStaticUpdate(mCurrentNmeaTime);
		   		name = aTarget.getShipname();
		   		if (test) Log.d(TAG,"next ship report " + name + " " 
		   				 + PositionTools.getTimeString(mCurrentNmeaTime)+ " last " 
		   				 + PositionTools.getTimeString(lastUpdate));
		   		if (mLogPositionData) Logger.d("next ship report " + name + " " 
		   				 + PositionTools.getTimeString(mCurrentNmeaTime)+ " last " 
		   				 + PositionTools.getTimeString(lastUpdate));
		   	// Did  we get a new position?
	    		if (Math.abs(aTarget.getLAT() - aOldLAT) > aThousandOfMile ){
	    			haveToSendGUIUpdate = true;
	    		}
	    		if (Math.abs(aTarget.getLON() - aOldLON) > aThousandOfMile ){
	    			haveToSendGUIUpdate = true;
	    		}
	    		if (Math.abs(aTarget.getCOG()- aOldCOG) > 50 )  {// more than 5 degree difference ,COG is mesured in degree * 10
	    			haveToSendGUIUpdate = true;
	    		}
	    		if (haveToSendGUIUpdate) {
	    			sendGUIUpdateEvent(aTarget);
	    		}
		   		try {
		   		  mDbAdapter.updateTarget(aTarget);
		   		}
		   		catch (SQLException e){
		   			Logger.d("update target failed " );
	    			e.printStackTrace();
	    		}
		   	}
	       if (test) Log.d(TAG,"MSG 5 Target " + name);
	       test = savedStatus;
	    }
	    
	    public void decodeMsg_24 (String aSixBitString){
	    	// ship report
	       if (test) Log.v(TAG,"aisplotter --> decodeMsg_24 begin");
	       String tempstr = aSixBitString.substring (8,38);
	       long aMMSI = bitStringToLong(tempstr);
	       nrOfTarget ++;
	       AISTarget aTarget = mTargetList.findTargetByMMSI(aMMSI);
		   	if (aTarget == null) {
		   		AISTarget newTarget = new AISTarget(-1);
		   		newTarget.setInfoFromClassBShipReport(aSixBitString);
		   		newTarget.setStatusToDisplay(AISPlotterGlobals.DISPLAYSTATUS_HAS_SHIPREPORT);
		   		newTarget.setTimeOfLastStaticUpdate(mCurrentNmeaTime);
		   		mTargetList.add(newTarget);
		   		sendGUIUpdateEvent(newTarget);
		   		try {
		   	    mDbAdapter.createTarget(newTarget);
		   		}
		   		catch (SQLException e){
		   			Logger.d("create target failed " );
	    			e.printStackTrace();
	    		}
		   	    if (test) Log.d(TAG, "first class B Shipreport: " + newTarget.getShipname() + " " + PositionTools.getTimeString(mCurrentNmeaTime));
		   	    if (mLogPositionData)Logger.d("first class B Shipreport: " + newTarget.getShipname() + " " + PositionTools.getTimeString(mCurrentNmeaTime));
		   	}else {
		   		double aOldLAT = aTarget.getLAT();
	    		double aOldLON = aTarget.getLON();
	    		int aOldCOG = aTarget.getCOG();
	    		boolean haveToSendGUIUpdate = false;
		   		//String name = aTarget.getShipname();
		   		aTarget.setInfoFromClassBShipReport(aSixBitString);
		   		if (aTarget.getStatusToDisplay() != AISPlotterGlobals.DISPLAYSTATUS_SELECTED)
		   		    aTarget.setStatusToDisplay(AISPlotterGlobals.DISPLAYSTATUS_HAS_SHIPREPORT);
		   		long uTCTimeOfPreviousShipReport = aTarget.getTimeOfLastStaticUpdate();
		   		aTarget.setTimeOfLastStaticUpdate(mCurrentNmeaTime);
		   	// Did  we get a new position or a new COG?
	    		if (Math.abs(aTarget.getLAT() - aOldLAT) > aThousandOfMile ){
	    			haveToSendGUIUpdate = true;
	    		}
	    		if (Math.abs(aTarget.getLON() - aOldLON) > aThousandOfMile ){
	    			haveToSendGUIUpdate = true;
	    		}
	    		if (Math.abs(aTarget.getCOG()- aOldCOG) > 50 )  {// more than 5 degree difference ,COG is mesured in degree * 10
	    			haveToSendGUIUpdate = true;
	    		}
	    		if (haveToSendGUIUpdate) {
	    			sendGUIUpdateEvent(aTarget);
	    		}
		   		try {
		   		   mDbAdapter.updateTarget(aTarget);
		   		}
		   		catch (SQLException e){
		   			Logger.d("update target failed " );
	    			e.printStackTrace();
	    		}
		   		if (test) Log.d(TAG, "new Class B Shipreport: " + aTarget.getShipname() 
		   				+ " " + PositionTools.getTimeString(mCurrentNmeaTime) 
		   				 + " prev " + PositionTools.getTimeString(uTCTimeOfPreviousShipReport));
		   		if (mLogPositionData)Logger.d("next Class B Shipreport: " + aTarget.getShipname() 
		   				+ " " + PositionTools.getTimeString(mCurrentNmeaTime) 
		   				 + " prev " + PositionTools.getTimeString(uTCTimeOfPreviousShipReport));
		   	}
	       
	    }
	    
	    public void sendInitGUIUpdateEvent(AISTarget pTarget){
	    	// we are in the init state of the GUI 
	    	// so we have to inform the GUI
	    	Intent aGUIUpdateIntent = new Intent (AISPlotterGlobals.ACTION_INITGUI);
	    	aGUIUpdateIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_MMSI_KEY,pTarget.getMMSIString());
	    	aGUIUpdateIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_STATUS_KEY, pTarget.getStatusToDisplay());
	    	aGUIUpdateIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_LON_KEY,pTarget.getLON());
	    	aGUIUpdateIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_LAT_KEY, pTarget.getLAT());
	    	aGUIUpdateIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_NAME_KEY,pTarget.getShipname());
	    	aGUIUpdateIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_COG_KEY,pTarget.getCOG());
	    	aGUIUpdateIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_COGSTR_KEY,pTarget.getCOGString());
	    	aGUIUpdateIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_SOG_KEY,pTarget.getSOG());
	    	aGUIUpdateIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_SOGSTR_KEY,pTarget.getSOGString());
	    	aGUIUpdateIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_LASTPOSUTC_KEY,pTarget.mTimeOfLastPositionReport);
	    	aGUIUpdateIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_HASTRACK_KEY, pTarget.mHasTrack);
	    	if (mLogPositionData)Logger.d("GUI-Update for " + pTarget.getMMSIString());
	    	ctx.sendBroadcast(aGUIUpdateIntent);
	    }
	    
	    public void sendGUIUpdateEvent(AISTarget pTarget){
	    	// we have a new position or the status changed
	    	// so we have to inform the GUI
	    	Intent aGUIUpdateIntent = new Intent (AISPlotterGlobals.ACTION_UPDATEGUI);
	    	aGUIUpdateIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_MMSI_KEY,pTarget.getMMSIString());
	    	aGUIUpdateIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_STATUS_KEY, pTarget.getStatusToDisplay());
	    	aGUIUpdateIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_LON_KEY,pTarget.getLON());
	    	aGUIUpdateIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_LAT_KEY, pTarget.getLAT());
	    	aGUIUpdateIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_NAME_KEY,pTarget.getShipname());
	    	aGUIUpdateIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_COG_KEY,pTarget.getCOG());
	    	aGUIUpdateIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_COGSTR_KEY,pTarget.getCOGString());
	    	aGUIUpdateIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_SOG_KEY,pTarget.getSOG());
	    	aGUIUpdateIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_SOGSTR_KEY,pTarget.getSOGString());
	    	aGUIUpdateIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_LASTPOSUTC_KEY,pTarget.mTimeOfLastPositionReport);
	    	aGUIUpdateIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_HASTRACK_KEY, pTarget.mHasTrack);
	    	aGUIUpdateIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_SPECIAL_MANUEVER_KEY,pTarget.getManueverStatus());
	    	aGUIUpdateIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_SHIP_LENGTH_KEY,pTarget.getLength());
	    	aGUIUpdateIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_SHIP_WIDTH_KEY,pTarget.getWidth());
	    	aGUIUpdateIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_NAV_STATUS_KEY, pTarget.getNavStatus());
	    	
	    	if (mLogPositionData)Logger.d("GUI-Update for " + pTarget.getMMSIString());
	    	ctx.sendBroadcast(aGUIUpdateIntent);
	    	logSpecialManuever(pTarget);
	    }
	    
	    public void sendNMEAPositionViaBroadcast(){
	    	if (Math.abs(lastLON) > 180d) return;
	    	if (Math.abs(lastLAT) > 90d) return;
	    	Intent aGPSNMEAIntent = new Intent(AISPlotterGlobals.ACTION_GPSNMEADATA);
	    	aGPSNMEAIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_LON_KEY, lastLON);
	    	aGPSNMEAIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_LAT_KEY, lastLAT);
	    	int aCOG = lastCOG;
	    	int aSOG = lastSOG;
	    	aGPSNMEAIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_COG_KEY,aCOG);
	    	//aGPSNMEAIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_COGSTR_KEY,PositionTools.customFormat ("000.0",aCOG/10.0d));
	    	aGPSNMEAIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_SOG_KEY,aSOG);
	    	//aGPSNMEAIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_SOGSTR_KEY,PositionTools.customFormat ("000.0",aSOG/10.0d));
	    	aGPSNMEAIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_LASTPOSUTC_KEY,lastNMEASpeedUpdateTime);
	    	// new additional data from 13_12_10
	    	aGPSNMEAIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_DEPTH_KEY,lastDepth);
	    	aGPSNMEAIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_WIND_ANGLE_KEY,lastWindAngle);
	    	aGPSNMEAIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_WIND_SPEED_KEY,lastWindSpeed);
		    ctx.sendBroadcast(aGPSNMEAIntent);
	    }
	    
	    private int getBearingToNewPos (double aLAT, double aLON) {
	    	// Source http://www.movable-type.co.uk/scripts/latlong.html
	    	// Formula: theta = atan2 (sin(delta_long).cos(lat2),
	    	//              cos(lat1).sin(lat2) - sin(lat1).cos(lat2).cos(delta_long))
	    	
	        // var dLat = (lat2-lat1).toRad();
	    	// var dLon = (lon2-lon1).toRad();
	    	// var lat1 = lat1.toRad();
	    	// var lat2 = lat2.toRad();
	    	// var y = Math.sin(dLon) * Math.cos(lat2);
	    	// var x = Math.cos(lat1)*Math.sin(lat2) -  Math.sin(lat1)*Math.cos(lat2)*Math.cos(dLon);
	    	// var brng = Math.atan2(y, x).toDeg();
	    	double dLat = Math.toRadians(aLAT-lastLAT);
	    	double dLon = Math.toRadians(aLON -lastLON);
	    	double lat1 = Math.toRadians(lastLAT);
	    	double lat2 = Math.toRadians(aLAT);
	    	double y = Math.sin(dLon) * Math.cos(lat2);
	    	double x = Math.cos(lat1)*Math.sin(lat2) -  Math.sin(lat1)*Math.cos(lat2)*Math.cos(dLon);
	    	double brng  = Math.atan2(y, x);
	    	double bearing = Math.toDegrees(brng);
	    	if (bearing < 0 ) {
	    		bearing = 360.0d + bearing;
	    	}
	    	return (int)Math.round(bearing * 10d);
	    	
	    }
	    
	    private int getSpeedFromLastPoint(double aLAT,double aLON){
	    	GeoPoint fromPoint = new GeoPoint (lastLAT,lastLON);
	    	GeoPoint toPoint = new GeoPoint(aLAT,aLON);
	    	double aDistance = PositionTools.calculateDistance(fromPoint, toPoint);
	    	long timeDeltaMillis = System.currentTimeMillis()-lastNMEASpeedUpdateTime;
	    	double timeDeltaInHours = 1.0d * timeDeltaMillis / (1000 * 60*60);
	    	double speedInKnots = aDistance / timeDeltaInHours;
	    	int speedIn10Knots = (int)(speedInKnots * 10.0d);
	    	return speedIn10Knots;
	    }
	    
	    private void setSpeedOfOwnShipFromRMC(String aSOGString){
	    	 // 7) 022.4        Speed in Knots
	    	// maybe 5.6 in $GPRMC,151457.83,A,5631.094,N,01616.863,E,5.6,190.0,310713,004.3,E,A*37

	    	//if (aSOGString.length()==5){
	    		try {
	    		  double aSOG = Double.parseDouble(aSOGString);
	    		  lastSOG = (int) (aSOG*10);
	    		} catch (NumberFormatException no){
	    			Logger.d(TAG,"error in setSpeedOfOwnShip  "+ no.toString());
	    		}
	    	//}
	    }
	    
	    private void setCourseOfOwnShipFromRMC(String aCOGString){
	    	 // 8) $GPRMC,151457.83,A,5631.094,N,01616.863,E,5.6,190.0,310713,004.3,E,A*37
	    	//{
	    		try {
	    		  double aCOG = Double.parseDouble(aCOGString);
	    		  if (0.0d <= aCOG && aCOG <=360.0d){
		    		  lastCOG = (int) (aCOG*10);
	    		  }
	    		} catch (NumberFormatException no){
	    			Logger.d(TAG,"error in setCourseOfOwnShip  "+ no.toString());
	    		}
	    	//}
	    }
	    
	    
	  
	    
	    private boolean setPositionOfOwnShipFromNMEA0183(String aLATString,String pNoSStr, String aLONString, String pWoEStr){
	    	/*  http://catb.org/gpsd/NMEA.txt
	    	 * Where a numeric latitude or longitude is given, the two digits
	    	 * immediately to the left of the decimal point are whole minutes, to the
	    	 * right are decimals of minutes, and the remaining digits to the left of
	    	 * the whole minutes are whole degrees.
	    	 * Eg. 4533.35 is 45 degrees and 33.35 minutes. ".35" of a minute is
             * exactly 21 seconds.
	    	 */
	    	double aLAT = 0d;
			double aLON = 0d;
			
			boolean mustBroadcast = false;
			//Intent aGPSNMEAIntent = new Intent("com.klein.aistcpmapplotter011.GPSNMEADATA");
			try {
				 // locate the decimal point inthe lat string
			  int aDecimalPointMarker = aLATString.indexOf(".");
			  if (aDecimalPointMarker > -1 && aLATString.length() >= 7) {
			      // we get the grad first
				  int minuteMarker = aDecimalPointMarker - 2;
				  String degreeLATStr = aLATString.substring(0,minuteMarker);
				  // we have to convert the NMEA-data-String to grad
				  // LAT is between 0 and 90,
				  double degreeLAT = Double.parseDouble(degreeLATStr);
				  String minLATStr = aLATString.substring(minuteMarker);
				  double minLAT = Double.parseDouble(minLATStr);
				  //double minLatTodegree = minLAT / 60;
				  // aLat is defined in degrees
				  aLAT = degreeLAT + minLAT / 60;
				  if (pNoSStr.equals("S")){
					  aLON = - aLON;
				  }
			      if (Math.abs(lastLAT - aLAT ) > aThousandOfMile ){
				    mustBroadcast = true;
			      }
			   }
			  aDecimalPointMarker = aLONString.indexOf(".");
			  if (aDecimalPointMarker > -1 && aLONString.length() >= 7) {
			      // we get the grad first
				// we have to convert the NMEA-data-String to grad
				// LON is between 0 and 180
				  int minuteMarker = aDecimalPointMarker - 2;
				  String degreeLONStr = aLONString.substring(0,minuteMarker);
				  double degreeLON = Double.parseDouble(degreeLONStr);
				  String minLONStr =  aLONString.substring(minuteMarker);
				  double minLON = Double.parseDouble(minLONStr);
				  //double minLONTodegree = minLON / 60;
				  //aLon is defined in degrees
				  aLON = degreeLON + minLON / 60;
				  if (pWoEStr.equals("W")){
					  aLON = - aLON;
				  }
				  
				  if (Math.abs(lastLON - aLON ) > aThousandOfMile ){
					mustBroadcast = true;
				  }
				   
				
			    }
			   if (mustBroadcast) {
					 if (System.currentTimeMillis()- mTimeOfLastRMCMessage > 5000) {
						 // we calculate COG and SOG only if there is no actual RMC Message 13_12_26
						 lastCOG = getBearingToNewPos(aLAT,aLON); // in Degree*10
						 lastSOG = getSpeedFromLastPoint(aLAT,aLON); // in Kn * 10
						 
						 lastNMEASpeedUpdateTime = System.currentTimeMillis();
					 //Log.d(TAG,"new own pos received, must broadcast " + "lat " + PositionTools.getLATString(lastLAT) + " lon " + PositionTools.getLONString(lastLON));
					 }
					 lastLAT = aLAT;
					 lastLON = aLON;
				 }
			 
			   return mustBroadcast;
				
			   
			}
		    catch (IndexOutOfBoundsException io) {
		    	return false;
		    }
		    catch (NumberFormatException no){
		    	return false;
		    }
	    }
	    
	    private void setPositionOfOwnShipFromPGN129029(Double aLAT, Double aLON){
	    	
			boolean mustBroadcast = false;
			
			if (Math.abs(lastLAT - aLAT ) > aThousandOfMile ){
				  mustBroadcast = true;
			     }
			if (Math.abs(lastLON - aLON ) > aThousandOfMile ){
				mustBroadcast = true;
			  }
			if (mustBroadcast) {
				 
				 lastCOG = getBearingToNewPos(aLAT,aLON); // in Degree*10
				 lastSOG = getSpeedFromLastPoint(aLAT,aLON); // in Kn * 10
				 lastLAT = aLAT;
				 lastLON = aLON;
				 lastNMEASpeedUpdateTime = System.currentTimeMillis();
				 //Log.d(TAG,"new own pos received, must broadcast " + "lat " + PositionTools.getLATString(lastLAT) + " lon " + PositionTools.getLONString(lastLON));
				 sendNMEA2000PositionViaBroadcast();
			 }
	    }
	    
	    public void sendNMEA2000PositionViaBroadcast(){
	    	if (Math.abs(lastLON) > 180d) return;
	    	if (Math.abs(lastLAT) > 90d) return;
	    	Intent aGPSNMEAIntent = new Intent(AISPlotterGlobals.ACTION_GPSNMEADATA);
	    	aGPSNMEAIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_LON_KEY, lastLON);
	    	aGPSNMEAIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_LAT_KEY, lastLAT);
	    	int aCOG = lastCOG;
	    	int aSOG = lastSOG;
	    	int aSpeedThruWater = lastSpeedThruWater;
	    	aGPSNMEAIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_COG_KEY,aCOG);
	    	//aGPSNMEAIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_COGSTR_KEY,PositionTools.customFormat ("000.0",aCOG/10.0d));
	    	aGPSNMEAIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_SOG_KEY,aSOG);
	    	//aGPSNMEAIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_SOGSTR_KEY,PositionTools.customFormat ("000.0",aSOG/10.0d));
	    	aGPSNMEAIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_LASTPOSUTC_KEY,lastNMEASpeedUpdateTime);
	    	aGPSNMEAIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_DEPTH_KEY,lastDepth);
	    	aGPSNMEAIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_WIND_ANGLE_KEY,lastWindAngle);
	    	aGPSNMEAIntent.putExtra(AISPlotterGlobals.GUIUpdateEvent_WIND_SPEED_KEY,lastWindSpeed);
	    	if (test) Log.d(TAG, "new NMEA2000 data depth " + lastDepth.toString() );
		    ctx.sendBroadcast(aGPSNMEAIntent);
	    }
	    
	    
	    
	   
	     /*
	    === GLL - Geographic Position - Latitude/Longitude ===

	    	------------------------------------------------------------------------------
	    		1       2 3        4 5         6 7   8
	    		|       | |        | |         | |   |
	    	 $--GLL,llll.ll,a,yyyyy.yy,a,hhmmss.ss,a,m,*hh<CR><LF>
	    	------------------------------------------------------------------------------

	    	Field Number: 

	    	1. Latitude
	    	2. N or S (North or South)
	    	3. Longitude
	    	4. E or W (East or West)
	    	5. Universal Time Coordinated (UTC)
	    	6. Status A - Data Valid, V - Data Invalid
	    	7. FAA mode indicator (NMEA 2.3 and later)
	    	8. Checksum 
	   */
	    
	    public void decodeGLL_Message(String pMessage) {

			String[] fields = pMessage.split(",");
			//Log.d(TAG, "decode GLL_message " +pMessage);
			//Logger.d(TAG,"decode GGA_message " +pMessage);
			if (fields[0].indexOf("GLL") == 3){
				
				String aLATString = fields[1];
				String aNoSStr = fields[2];
				String aLONString = fields[3];
				String aEoWStr = fields[4];
				if (test) Log.d(TAG,"from GLL " + " lat " + aLATString + " lon "+ aLONString)	;
				boolean  mustBroadcast = setPositionOfOwnShipFromNMEA0183(aLATString,aNoSStr,aLONString,aEoWStr);
                if (mustBroadcast) {
					 sendNMEAPositionViaBroadcast();
				 }
			}
	    }
	    
	 // $GPGGA, 161229.487, 3723.2475, N, 12158.3416, W, 1, 07, 1.0, 9.0, M, , , ,0000*18   
	 // 3723.2475,N   Latitude 37 deg 23.2475' N
	 // 12158.3416,W  Longitude 121 deg 58.3416' W
	    public void decodeGGA_Message(String pMessage)
	    {
			String[] fields = pMessage.split(",");
			//Log.d(TAG, "decode GGA_message " +pMessage);
			//Logger.d(TAG,"decode GGA_message " +pMessage);
			if (fields[0].indexOf("GGA") == 3){
				
				String aLATString = fields[2];
				String aNoSStr = fields[3];
				String aLONString = fields[4];
				String aEoWStr = fields[5];
				//Log.d(TAG,"from CGA " + " lat " + aLATString + " lon "+ aLONString)	;
				boolean  mustBroadcast = setPositionOfOwnShipFromNMEA0183(aLATString,aNoSStr,aLONString,aEoWStr);
                if (mustBroadcast) {
					 sendNMEAPositionViaBroadcast();
				 }
				
			
			} // if  "GGA" Message
	    }
	    
	    
	 // from www.nmea.de   RMC Message
	 //  0     1     2  3        4 5         6 7     8      9     10
	 // $GPRMC,123519,A,4807.038,N,01131.000,E,022.4,084.4,230394,003.1,W*6A
	 //   Field Number:  
	 //   	  1) UTC Time 
	 //   	  2) Status, V = Navigation receiver warning 
	 //   	  3) Latitude 
	 //   	  4) N or S 
	 //  	  5) Longitude 
	 //   	  6) E or W 
	 //   	  7) Speed over ground, knots 
	 //   	  8) Track made good, degrees true 
	 //   	  9) Date, ddmmyy 
	 //   	 10) Magnetic Variation, degrees 
	 //   	 11) E or W 
	 //   	 12) Checksum 

	 // 3) 4807.038,N   Latitude 48 deg 07.038' N
	 // 5) 01131.000,E  Longitude 11 deg 31.000' E
     // 7) 022.4        Speed in Knots
	    public void decodeRMC_Message(String pMessage)
	    {
			String[] fields = pMessage.split(",");
			//Log.d(TAG, "decode RMC_message " +pMessage);
			//Logger.d(TAG,"decode RMC_message " +pMessage);
			if (fields[0].indexOf("RMC") == 3){
				
				String aLATString = fields[3];
				String aNoSStr = fields[4];
				String aLONString = fields[5];
				String aEoWStr = fields[6];
				String aSOGString = fields[7];
				String aCOGString = fields[8];
				//Log.d(TAG,"from RMC " + " lat " + aLATString + " lon "+ aLONString)	;
				lastNMEASpeedUpdateTime = System.currentTimeMillis();
				mTimeOfLastRMCMessage = System.currentTimeMillis();
				boolean mustBroadcast = setPositionOfOwnShipFromNMEA0183(aLATString,aNoSStr,aLONString,aEoWStr);
			    setSpeedOfOwnShipFromRMC(aSOGString); // we override the calculated values with the value from the msg
			    setCourseOfOwnShipFromRMC(aCOGString);
			    if (mustBroadcast) {
					 sendNMEAPositionViaBroadcast();
				 }
			} // if  "RMC" Message
	    }
	    
	    public void decodeDBT_Message(String pMessage) {
	    // $SDDBT,55.3,f,16.8,M,9.2,F*01	
	    //Log.d(TAG, "DBT Msg: "+ pMessage);
	    	String[] fields = pMessage.split(",");
                if (fields[0].indexOf("DBT") == 3){
                 if (fields[4].equals("M") ){
                	 String aDepthString = fields[3];
                	 lastDepth = Double.parseDouble(aDepthString);
                	 //Log.d(TAG,"lstDepth " + lastDepth);
                	 if (System.currentTimeMillis() -  lastNMEASpeedUpdateTime  > 1000){
                		 lastNMEASpeedUpdateTime = System.currentTimeMillis();
                	 sendNMEAPositionViaBroadcast();
                	 }
                 }
				
				
				
			} // if  "DBT" Message
	    }
	    
	    /*
	     * === VHW - Water speed and heading ===

------------------------------------------------------------------------------
        1   2 3   4 5   6 7   8 9
        |   | |   | |   | |   | |
 $--VHW,x.x,T,x.x,M,x.x,N,x.x,K*hh<CR><LF>
------------------------------------------------------------------------------

Field Number: 

1. Degress True
2. T = True
3. Degrees Magnetic
4. M = Magnetic
5. Knots (speed of vessel relative to the water)
6. N = Knots
7. Kilometers (speed of vessel relative to the water)
8. K = Kilometers
9. Checksum
	     */
	    
	    public void decodeVHW_Message(String pMessage) {
	     // $IIVHW,186.0,T,194,M,004.6,N,008.5,K*47	
	    	String[] fields = pMessage.split(",");
            if (fields[0].indexOf("VHW") == 3){
            /* if (fields[4].equals("M") ){
            	 String aDepthString = fields[3];
            	 lastDepth = Double.parseDouble(aDepthString);
             }*/
			
		    } // if  "VHW" Message
	    }
	    
	    public void decodeMWV_Message(String pMessage) {
		     // $IIMWV,006.5,R,017.2,N,A*3A	
		    	String[] fields = pMessage.split(",");
	            if (fields[0].indexOf("MWV") == 3){ 
	             if (fields[2].equals("R") ){
	            	 String aWindangleString = fields[1];
	            	 lastWindAngle = Double.parseDouble(aWindangleString);
	             }
	             if (fields[4].equals("N") ){
	            	 String aWindspeedString = fields[3];
	            	 lastWindSpeed = Double.parseDouble(aWindspeedString);
	             }
	             if (System.currentTimeMillis() -  lastNMEASpeedUpdateTime  > 1000){
            		 lastNMEASpeedUpdateTime = System.currentTimeMillis();
            	     sendNMEAPositionViaBroadcast();
            	 }
	             
				
			    } // if  "MWV" Message
		    }
		
	
	    
		public long UTCDateTime(String pUTCTime, String pUTCDate) {
			long aTime = 0;
			try {
			  
			  int day = Integer.parseInt(pUTCDate.substring(0,2));
			  int month = Integer.parseInt(pUTCDate.substring(2,4));
			  int year = 100 + Integer.parseInt(pUTCDate.substring(4,6));
			  int hour = Integer.parseInt(pUTCTime.substring(0,2));
			  int minute = Integer.parseInt(pUTCTime.substring(2,4));
			  int second = Integer.parseInt(pUTCTime.substring(4,6));
			  Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT")); 
			  cal.set(year + 1900, month, day, hour, minute, second); 
			  aTime = cal.getTime().getTime(); 
			} catch (Exception e) {
				
			}
			return aTime;
		}
	 
	   public TargetList getAISTargetList() {
		   return mTargetList;
	   }
	   
	   private void logSpecialManuever(AISTarget pTarget){
		byte theSpecialManeuverStatus = pTarget.getManueverStatus();  // blue flag
		String mmsiStr = pTarget.getMMSIString();
		String name = pTarget.getShipname();
		if (theSpecialManeuverStatus == 2) {
			Log.d(TAG, "found Blue Flag on ship " + mmsiStr + " " + name );
		}
	   }

	    
	}