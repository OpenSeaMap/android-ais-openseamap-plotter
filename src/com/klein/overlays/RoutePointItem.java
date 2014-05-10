package com.klein.overlays;

import org.mapsforge.android.maps.overlay.OverlayWay;
import org.mapsforge.core.GeoPoint;


/**
 * 
 * @author vkADM
 * a RouteItem holds information 
 * about a single RouteItemPoint
 * point: the Geopoint
 * overlayWay: the way between two points
 * routeOverlayItem: the tapable symbol
 */
public class RoutePointItem {
	GeoPoint mGeoPoint;
	RouteOverlayItem mRouteOverlayItem;
	OverlayWay mOverlayWay;

	public RoutePointItem(GeoPoint pGeoPoint) {
		mGeoPoint = pGeoPoint;
	}

	public GeoPoint getPoint() {
		return mGeoPoint;
	}
	
	public void setRouteOverlayItem(RouteOverlayItem pRouteOverlayItem) {
		mRouteOverlayItem = pRouteOverlayItem;
	}
	
	public RouteOverlayItem getRouteOverlayItem () {
		return mRouteOverlayItem;
	}

	public void setOverlayWay(OverlayWay pOverlayWay) {
		mOverlayWay = pOverlayWay;
	}

	public OverlayWay getOverlayWay() {
		return mOverlayWay;
	}
}
