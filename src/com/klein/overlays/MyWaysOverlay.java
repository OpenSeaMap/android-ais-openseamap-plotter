package com.klein.overlays;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Set;

import org.mapsforge.android.maps.overlay.ArrayWayOverlay;
import org.mapsforge.android.maps.overlay.OverlayWay;

import android.graphics.Paint;

/**
 * MyWaysOverlay holds information about the route ways and the ways from an AIS-Track
 * @author vkADM
 * the tracks and routes  are referenced by a wayKey ( the mmsi or the route number) 
 * as mmsi have always 9 digits there is no problem 
 *
 */
public class MyWaysOverlay extends ArrayWayOverlay {
	
	private LinkedHashMap<String,ArrayList<OverlayWay>> overlayWayDictionary = null;
	
  public MyWaysOverlay  (Paint defaultPaintFill, Paint defaultPaintOutline) {
	  super(defaultPaintFill,defaultPaintOutline);
	  overlayWayDictionary = new LinkedHashMap<String,ArrayList<OverlayWay>>();
  }
  
  public void addOverlayWayKey (String wayKey) {
	  ArrayList<OverlayWay> aWayList = new ArrayList<OverlayWay>();
	  overlayWayDictionary.put(wayKey, aWayList);
  }
  
  public void removeOverlayWayKey(String wayKey){
	  overlayWayDictionary.remove(wayKey);
  }
  
  public void addWay(String wayKey, OverlayWay way){
	  super.addWay(way);
	  if (overlayWayDictionary.containsKey(wayKey)){
	    ArrayList<OverlayWay> aWayList = overlayWayDictionary.get(wayKey);
	    aWayList.add(way);
	  }
	  else {
		  addOverlayWayKey(wayKey); 
		  ArrayList<OverlayWay> aWayList = overlayWayDictionary.get(wayKey);
		  aWayList.add(way);
	  }
  }
  
  public void removeWay(String wayKey, OverlayWay way){
	  ArrayList<OverlayWay> aWayList = overlayWayDictionary.get(wayKey); 
	  // we may be called with a wayKey that is not in dictionary
	  // then we do nothing
	  if (aWayList != null) {
		  aWayList.remove(way);
		  super.removeWay(way);
	  }
	  
  }
  
  public ArrayList<OverlayWay> getWayList(String wayKey){
	  return overlayWayDictionary.get(wayKey); 
  }
  
  public void clearWay(String wayKey){
	  ArrayList<OverlayWay> aWayList = overlayWayDictionary.get(wayKey); 
	  // we may be called with a wayKey that is not in dictionary
	  // then we do nothing
	  if (aWayList != null ) {
		  int count = aWayList.size();
		  for (int index = 0; index < count; index ++){
		    OverlayWay  aWay = aWayList.get(index);
		    super.removeWay(aWay);
	      }
	  }
	  
  }
  @Override
  public void clear() {
	Set keysSet = overlayWayDictionary.keySet();
	String[] keys = (String[]) keysSet.toArray();
	int count =  keys.length;
	for (int index = 0;index < count; index ++){
		String wayKey = keys[index];
		clearWay(wayKey);
	}
	overlayWayDictionary.clear();
	super.clear();
  }
  
}
