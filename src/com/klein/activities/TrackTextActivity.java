package com.klein.activities;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.mapsforge.core.GeoPoint;

import android.app.Activity;
import android.database.Cursor;
import android.database.SQLException;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;



import com.klein.aistcpopenmapplotter051.R;

import com.klein.service.TrackDbAdapter;

public class TrackTextActivity extends Activity {
	private String mMMSI;
	private boolean test = false;
	private TrackDbAdapter mDbAdapter;
	private ArrayList<String> mStringList;
	private static final String TAG = "TrackText";
	@Override
	protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.tracktext);
        TextView tv = (TextView)findViewById(R.id.track_text);
        mDbAdapter = new TrackDbAdapter(this);
        mDbAdapter.open();
        mStringList = new ArrayList<String>();
        
        mMMSI = (savedInstanceState == null) ? null :
            (String) savedInstanceState.getSerializable(TrackDbAdapter.KEY_MMSI);
        if (mMMSI == null) {
            Bundle extras = getIntent().getExtras();
            mMMSI = extras != null ? extras.getString(TrackDbAdapter.KEY_MMSI)
                                    : null;
        }
        Cursor aNameCursor = null;
        try {
          aNameCursor = mDbAdapter.fetchTargetFromMMSI(mMMSI);	
          int column = aNameCursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_NAME);
          if (column > -1) {
        	  String aName = aNameCursor.getString(column);
              if (!aName.equals("")) {
        	    mStringList.add("Shipname " + aName);
              }else {
        	    mStringList.add("MMSI " + mMMSI);
              }
          }
        } catch (Exception e){
        	mStringList.add("MMSI " + mMMSI);
        	Log.d(TAG,"target " + mMMSI + " has no name");
        } finally {
		    if (aNameCursor!= null) aNameCursor.close();
        }
        Cursor cursor = null;
        try {
        cursor = mDbAdapter.fetchShipTrackTable(mMMSI);
		int count = cursor.getCount();
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
			mStringList.add(aString);
			while (cursor.moveToNext()) {
				aId = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_ROWID));
				aMMSI = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_MMSI));
				aLATStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LAT));
				aLONStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LON));
				aUTC = cursor.getLong(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_UTC));
				if (test) {
					Log.i(TAG, "next route Point " + aId + " MMSI " + aMMSI + " LAT " + aLATStr
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
				mStringList.add(aString);
			}
			 
		}
        } catch (SQLException e){
        	Log.d(TAG,"error reading tracktable to " + mMMSI + " " + e.toString());
        } finally {
		    if (cursor!= null) cursor.close();
        }
        // Convert the buffer into a string.
        String text = mStringList.toString();
        
	    // Finally stick the string into the text view.
	    tv.setText(text);
        
    }
	
	public void onDestroy(){
   	 
    	mDbAdapter.close();
    	super.onDestroy();
    }   
}
