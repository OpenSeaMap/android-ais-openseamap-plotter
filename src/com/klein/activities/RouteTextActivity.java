package com.klein.activities;

import java.util.ArrayList;

import org.mapsforge.core.GeoPoint;

import android.app.Activity;
import android.database.Cursor;
import android.database.SQLException;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;



import com.klein.aistcpopenmapplotter051.R;

import com.klein.commons.AISPlotterGlobals;
import com.klein.commons.PositionTools;
import com.klein.service.TrackDbAdapter;

public class RouteTextActivity extends Activity {

	private boolean test = true;
	private TrackDbAdapter mDbAdapter;
	private ArrayList<String> mStringList;
	private static final String TAG = "TrackText";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.routetext);
		TextView tv = (TextView) findViewById(R.id.route_text);
		mDbAdapter = new TrackDbAdapter(this);
		mDbAdapter.open();
		mStringList = new ArrayList<String>();
		String text = "no data";
		Cursor cursor = null;
		try {
			cursor = mDbAdapter.fetchRouteTable(AISPlotterGlobals.DEFAULTROUTE);
			if (cursor != null) {
				int count = cursor.getCount();
			}
			if ((cursor != null) && (cursor.getCount() > 0)) {

				String aId;
				String aLATStr;
				String aLONStr;
				long aUTC;
				// we use the simpleDataFormat to convert millis to gpx-readable form
				cursor.moveToFirst();
				aId = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_ROWID));
				aLATStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LAT));
				aLONStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LON));
				aUTC = cursor.getLong(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_UTC));
				GeoPoint aRoutePoint = new GeoPoint(Double.parseDouble(aLATStr), Double.parseDouble(aLONStr));
				StringBuffer bufPoint = new StringBuffer();
				// <trkpt lon="6.96045754" lat="51.44282806"> <time>2011-07-16T13:24:55</time>
				// </trkpt>
				bufPoint.append("\n <rtept lon=" + '"' + aRoutePoint.getLongitude() + '"' + " lat=" + '"'
						+ aRoutePoint.getLatitude() + '"' + ">" + "</rtept>");

				String aString = bufPoint.toString();
				mStringList.add(aString);
				while (cursor.moveToNext()) {
					aId = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_ROWID));

					aLATStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LAT));
					aLONStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LON));
					aUTC = cursor.getLong(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_UTC));

					double lat = Double.parseDouble(aLATStr);
					double lon = Double.parseDouble(aLONStr);
					aRoutePoint = new GeoPoint(lat, lon);
					bufPoint = new StringBuffer();
					// <trkpt lon="6.96045754" lat="51.44282806"> </trkpt>
					bufPoint.append("\n <rtept lon=" + '"' + aRoutePoint.getLongitude() + '"' + " lat=" + '"'
							+ aRoutePoint.getLatitude() + '"' + ">" + "</rtept>");

					aString = bufPoint.toString();
					mStringList.add(aString);
					// Update the progress bar
				} // while
				String aInfo = "nr of routePoints " + mStringList.size();
				mStringList.add(aInfo);
				// text = mStringList.toString(); see later
				// show Points, Distance and Course
				mStringList.clear();
				cursor.moveToFirst();
				int index = 0;
				double sumOfDistances = 0;
				aId = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_ROWID));
				aLATStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LAT));
				aLONStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LON));
				aUTC = cursor.getLong(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_UTC));
				GeoPoint firstPoint = new GeoPoint(Double.parseDouble(aLATStr), Double.parseDouble(aLONStr));
				GeoPoint nextPoint;
				mStringList.add(" Route ");
				while (cursor.moveToNext()) {
					
					aId = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_ROWID));
					aLATStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LAT));
					aLONStr = cursor.getString(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_LON));
					aUTC = cursor.getLong(cursor.getColumnIndexOrThrow(TrackDbAdapter.KEY_UTC));

					double lat = Double.parseDouble(aLATStr);
					double lon = Double.parseDouble(aLONStr);
					nextPoint = new GeoPoint(lat,lon);
					double aDistance = PositionTools.calculateDistance(firstPoint, nextPoint);
					double aCourse = PositionTools.calculateCourse(firstPoint, nextPoint);
					bufPoint = new StringBuffer();
					bufPoint.append("\n" + PositionTools.customFormat("000", index) + " LAT " + PositionTools.getLATString(firstPoint.getLatitude())
									+ " LON " + PositionTools.getLONString(firstPoint.getLongitude()));
					bufPoint.append("  Distance: " + PositionTools.customFormat("000.000", aDistance) + " nM");
				    bufPoint.append("  Course: " + PositionTools.customFormat("000.0",aCourse)+ "°");
					aString = bufPoint.toString();
					mStringList.add(aString);
					firstPoint = nextPoint;
					index++;
					sumOfDistances = sumOfDistances + aDistance;
					/*aRoutePoint = new GeoPoint(lat, lon);
					bufPoint = new StringBuffer();
					// <trkpt lon="6.96045754" lat="51.44282806"> </trkpt>
					bufPoint.append("\n " + index +  " LON " + aRoutePoint.getLongitude() 
					                + " LAT " + aRoutePoint.getLatitude()  + " id" + aId);
					aString = bufPoint.toString();
					mStringList.add(aString);*/
					// Update the progress bar
				} // while
				aInfo = "\n \ntotal length of route" +  PositionTools.customFormat("000.000", sumOfDistances) + " nM";;
				mStringList.add(aInfo);
				text = mStringList.toString();
			} // if
		} catch (SQLException e) {
			Log.d(TAG, e.toString());
		} catch (Exception e) {
			Log.d(TAG, "Exception in onCreate " + e.toString());
		} finally {
			if (cursor != null)
				cursor.close();
			// Finally stick the string into the text view.
			tv.setText(text);
		}

	}

	public void onDestroy() {

		mDbAdapter.close();
		super.onDestroy();
	}
}
