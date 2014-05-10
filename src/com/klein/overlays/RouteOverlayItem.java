package com.klein.overlays;

import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.core.GeoPoint;

import android.graphics.drawable.Drawable;

public class RouteOverlayItem extends OverlayItem {
	private double mLON;
	private double mLAT;
	private int mNumber;
	

	public RouteOverlayItem(GeoPoint point, int number, String title, String snippet, Drawable marker) {
		super(point, title, snippet, marker);
		mNumber = number;
	}
	
	public int getNumber() {
		return mNumber;
	}

	public double mLON() {
		return mLON;
	}

	public double getLAT() {
		return mLAT;
	}

	public void setLON(double pLON) {
		mLON = pLON;
	}

	public void setLAT(double pLAT) {
		mLAT = pLAT;
	}
}
