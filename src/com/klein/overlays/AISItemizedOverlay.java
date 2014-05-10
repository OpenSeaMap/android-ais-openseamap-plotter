package com.klein.overlays;

import java.util.ArrayList;

import org.mapsforge.android.maps.overlay.ArrayItemizedOverlay;
import org.mapsforge.android.maps.overlay.OverlayWay;
import org.mapsforge.core.GeoPoint;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.klein.aistcpopenmapplotter051.AISTCPOpenMapPlotter;
import com.klein.aistcpopenmapplotter051.R;

import com.klein.commons.PositionTools;
import com.klein.service.AISTarget;

/**
 * there is only one instance of AISItemizedOverlay, mAISItemizedOverlay
 * @author vkADM
 * a Item may be tapped and shows information over the target
 * if a target has a track, a dialog asks if the track can be saved or deleted
 * if a target has no track, a track recording can be initiated
 * there is no way to disable track recording, and restart it laster
 * 
 * is actually not used see MyItemizedOverlay 13_05_11
 */

public class AISItemizedOverlay extends ArrayItemizedOverlay {
	private static final String TAG = "ArrayItemizedOverlay";
	private boolean test = false;
	private final AISTCPOpenMapPlotter context;
    
	/**
	 * Constructs a new AISItemizedOverlay.
	 * 
	 * @param defaultMarker
	 *            the default marker (may be null).
	 * @param context
	 *            the reference to the application context.
	 */
	public AISItemizedOverlay(Drawable defaultMarker, AISTCPOpenMapPlotter context) {
		super(defaultMarker);
		this.context = context;
	}
	
	/**
	 * Handles a long press event.
	 * <p>
	 
	 * @param index
	 *            the index of the item that has been long pressed.
	 * @return true if the event was handled, false otherwise.
	 */
	
	/* This does not run without error
	 * the TargetEditActivity is shown
	 * but we crash when we leave AISPlotter
	@Override
	protected boolean onLongPress(int index) {
		OverlayItem item = getItem(index);
		if (item != null) {
			GeoPoint aItemPoint = item.getPoint();
    		String aMMSIStr = item.getTitle();
    		try {
	    		  long aMMSI = Long.parseLong(aMMSIStr);
	    		  AISTarget aTarget = mTargetList.findTargetByMMSI(aMMSI);
	    		  showEditTarget(aTarget);
    		}
              catch (Exception e){
    			 e.printStackTrace(); 
    		}
    		
		return true;
		}
	  return false;
	}*/

	/**
	 * Handles a tap event on the given item.
	 * we search in the targetList with the mmsi, which is stored in the title
	 */
	@Override
	protected boolean onTap(int index) {
		AISOverlayItem item = (AISOverlayItem)getItem(index);
		if (item != null) {
			String aName = "---";
			GeoPoint aItemPoint = item.getPoint();
    		String aMMSIStr = item.getTitle();
    		String aInfo = item.getSnippet();
    		AISTarget aTarget = null;
    		try {
    		  long aMMSI = Long.parseLong(aMMSIStr);
    		  aTarget = context.mTargetList.findTargetByMMSI(aMMSI);
    		  if (!aTarget.getShipname().equals("")) {
	    		  aName = aTarget.getShipname();
	    	  }
    		} catch (Exception e){
    			 e.printStackTrace(); 
    			 // we get this exception if mmsi denotes myShip, cause it is not in the list
    			 // of the AISTargets
    			 if (aMMSIStr.equals(context.myShip.getMMSIString())){
    				aTarget = context.myShip; 
    			 }
    		}
    		double aTargetLAT = aTarget.getLAT();
    		double aTargetLON = aTarget.getLON();
			  
	    	  
	    	String aCOGStr = aTarget.getCOGString();
	    	String aSOGStr = aTarget.getSOGString();
	    	long aUTCDiff = System.currentTimeMillis() - aTarget.getTimeOfLastPositionReport();
	    	String aTimeStr =PositionTools.getTimeString(aUTCDiff);
	    	int aDisplayStatus = aTarget.getStatusToDisplay();
	    	//String aInfoStr = AISTCPOpenMapPlotter.this.getResources().getString(R.string.aisItem_info1);
	    	String aInfoStr = context.getResources().getString(R.string.aisItem_info1);
	    	aInfo = aName  + "\n" + "LAT: " + PositionTools.getLATString(aTargetLAT) 
	                       + "\nLON: " + PositionTools.getLONString(aTargetLON) 
	                       + "\nCOG " + aCOGStr 
	                       + "\nSOG " + aSOGStr
	                       
	                       + "\n" + aInfoStr + " " + aTimeStr
	    	               ; 
	    	  
	    	  
    		
            if (!context.mDbAdapter.isTableToMMSIInShipTrackList(aMMSIStr)){
                	// track table was deleted or no track yet
                	aTarget.setHasTrack(false);
                	item.setHasTrack(false);
                	context.mDbAdapter.updateTarget(aTarget);
                		
            } else {
            	aTarget.setHasTrack(true);
            	item.setHasTrack( true);
            	context.mDbAdapter.updateTarget(aTarget);
            }
            if (item.hasTrack()){
	    		 aInfo = aInfo + "\n" + context.getResources().getString(R.string.aisItem_track_is_recorded) ;
	    	  } 
	    	  else {
	    		 aInfo = aInfo + "\n" + context.getResources().getString(R.string.aisItem_track_is_not_recorded);
	    	  } 
            final String theMMSI = aMMSIStr;
            final String theName = aName;
            final AISOverlayItem aItem = item;
            final AISTarget actualTarget = aTarget;
			Builder builder = new AlertDialog.Builder(this.context);
			builder.setIcon(android.R.drawable.ic_menu_info_details);
			builder.setTitle(item.getTitle());
			builder.setMessage(aInfo);
			builder.setPositiveButton("OK", null);
			if (!item.hasTrack()){ 
				builder.setNeutralButton(R.string.aisItem_record_track, new OnClickListener(){
					public void onClick(DialogInterface dialog , int which) {
						if (actualTarget != null){
							if (!actualTarget.getHasTrack()){
								aItem.setHasTrack( true);
								if (!context.mDbAdapter.isTableToMMSIInShipTrackList(theMMSI)){
				            		context.mDbAdapter.createShipTrackTable(theMMSI);
				            		Log.d(TAG,"create track table to "+ theMMSI);
				            	}
								actualTarget.setHasTrack(true);
								if (test)Log.d(TAG,"selected ship to track " + theMMSI + " " + theName);
							}
							
						}
						
					}
				});
				
				
			} else {
				builder.setNeutralButton(R.string.aisItem_delete_track, new OnClickListener(){
					public void onClick(DialogInterface dialog , int which) {
						if (actualTarget != null){
							if (actualTarget.getHasTrack()){
								if (aItem.hasTrack()){
									  // remove all ways from the ArrayWayOverlay concerning to this AISTarget
									  ArrayList<OverlayWay> theWays = aItem.getOverlayways();
									  int count = theWays.size();
									  for (int index = 0;index < count;index++){
										  OverlayWay aOverlayWay = theWays.get(index);
										  Log.d(TAG,"Delete way "+ theMMSI + " " + index);
										  context.mWaysOverlay.removeWay(theMMSI,aOverlayWay);
									  }
									  theWays.clear();
								  }
								aItem.setHasTrack( false);
								if (context.mDbAdapter.isTableToMMSIInShipTrackList(theMMSI)){
				            		context.mDbAdapter.deleteShipTrackTable(theMMSI);
				            		Log.d(TAG,"delete track table to "+ theMMSI);
				            		
				            	}
								actualTarget.setHasTrack(false);
								if (test) Log.d(TAG,"no further tracking on " + theName);
							}
						}
					}
				});
				
				builder.setNegativeButton(R.string.aisItem_save_track,new OnClickListener(){
					public void onClick(DialogInterface dialog , int which) {
						if (actualTarget != null){
							if (actualTarget.getHasTrack()){
								if (context.mDbAdapter.isTableToMMSIInShipTrackList(theMMSI)){
				            		context.mDbAdapter.WriteTrackDataToExternalStorage(theMMSI);
				            		Log.d(TAG,"save track table to "+ theMMSI);
				            	}
							}
						}
					}
				});
				
				
			}
			builder.show();
			
			return true;
		}
		return false;
	}
	
	public AISOverlayItem getItem (int index) {
		return (AISOverlayItem)createItem(index);
	}
	
	public void updateItem (int index) {
		AISOverlayItem item = (AISOverlayItem)createItem(index);
		if (item != null) {
			String aMMSI = item.getTitle();
			
		}
	}
}
