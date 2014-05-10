package com.klein.service;

import java.util.ArrayList;

import org.mapsforge.core.GeoPoint;

import android.content.Context;
import android.util.Log;

public class ShipTrackList {
	private ArrayList<GeoPoint> mShipTrackList = null;
	private static String TAG ="ShipTrackList";
	private boolean test = true;
	private Context ctx;
	private TrackDbAdapter mDbAdapter;
	
	public  ShipTrackList(Context ctx) {
		if (test) Log.v(TAG,"Targetlist create");
		this.ctx = ctx;
		mShipTrackList = new ArrayList<GeoPoint>();
		mDbAdapter = new TrackDbAdapter(this.ctx);
		mDbAdapter.open();
	}
	
	public int getSize() {
		return mShipTrackList.size();
	}
	
	public void add(GeoPoint aGeoPoint){
		mShipTrackList.add(aGeoPoint);
	}
	
	public GeoPoint get(int aIndex){
		return mShipTrackList.get(aIndex);
	}
	
	public void clear() {
		mShipTrackList.clear();
	}
	
	public void destroy() {
		mDbAdapter.close();
	}
}
