package com.klein.overlays;

import java.util.ArrayList;

import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.android.maps.overlay.OverlayWay;
import org.mapsforge.core.GeoPoint;

import android.graphics.drawable.Drawable;



/**
 * AISOverlayItem holds the data for a AISItem
 * mhasTrack
 * mWayList
 * @author vkADM
 * ways could be added to the item addWay()
 * a reference to all ways of this item is hold in mWaylist, to allow deleteing the ways to this item
 * with clearWays()
 *
 */
public class AISOverlayItem extends OverlayItem {
	/*  private double mLON;
	  private double mLAT;*/
	  private boolean mHasTrack;
	  private ArrayList<OverlayWay> mWayList;
	  
	/* public  AISOverlayItem(boolean hasTrack) {
		  super();
		  mHasTrack = hasTrack;
		  mWayList = new ArrayList<OverlayWay>();
	  }*/
	  /**
	   * @param point hols  the geoPoint of the item
	   * @param title holds the mmsi, is the main reference for serching in the lists
	   * @param snippet  holds initial information about course and speed
	   * @param marker holds the  sysmbol for the target
	   * @param hasTrack has the target a track?
	   */
	  public AISOverlayItem(GeoPoint point, String title, String snippet,
			Drawable marker , boolean hasTrack) {
		super(point,title,snippet, marker);
		mHasTrack = hasTrack;
		mWayList = new ArrayList<OverlayWay>();
	}
	/*public double mLON() {
		  return mLON;
	  }
	  public double getLAT() {
		  return mLAT;
	  }
	  
	  public void setLON (double pLON){
		  mLON = pLON;
	  }
	  public void setLAT (double pLAT){
		  mLAT = pLAT;
	  }*/
	  
	  public void addWay (OverlayWay pOverlayWay) {
		 mWayList.add(pOverlayWay); 
	  }
	  
	  public ArrayList<OverlayWay> getOverlayways() {
		  return mWayList;
	  }
	  public void clearWays() {
		  mWayList.clear();
	  }
	  
	  public boolean hasTrack(){
		  return mHasTrack;
	  }
	  
	  public void setHasTrack(boolean hasTrack) {
		  mHasTrack = hasTrack;
	  }
}
