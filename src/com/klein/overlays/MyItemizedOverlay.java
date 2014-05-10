package com.klein.overlays;

import java.util.ArrayList;


import org.mapsforge.android.maps.Projection;
import org.mapsforge.android.maps.overlay.ArrayItemizedOverlay;
import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.android.maps.overlay.OverlayWay;
import org.mapsforge.core.GeoPoint;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.klein.aistcpopenmapplotter051.AISTCPOpenMapPlotter;
import com.klein.aistcpopenmapplotter051.R;
import com.klein.commons.PositionTools;
import com.klein.service.AISTarget;
/**
 * MyItemizedOverlay holds the info about AISOverlayItems and RouteOverlayItems
 * both can be tapped and show information in a dialog, see onTap
 * @author vkADM
 *
 */


public class MyItemizedOverlay extends ArrayItemizedOverlay {
	private static final String TAG = "ItemizedOverlay";
	private boolean test = false;
	private final AISTCPOpenMapPlotter context;
    
	private final ArrayList<AISOverlayItem> mAISOverlayItems;
	private final ArrayList<RouteOverlayItem> mRouteOverlayItems;
	
	// for the center circle
	private Paint mPaint;
	private RectF mOval;
	private boolean mMustShow = false;
	
	/**
	 * Constructs a new ItemizedOverlay.
	 * the Itemized Overlay contains clickable items , 
	 * this may be 
	 * route-point-symbols or 
	 * AISTarget-symbols
	 * we must mix them in the overlay caused by memory problems with too many overlays
	 * @param defaultMarker
	 *            the default marker (may be null).
	 * @param context
	 *            the reference to the application context.
	 */
	public MyItemizedOverlay(Drawable defaultMarker, AISTCPOpenMapPlotter context) {
		super(defaultMarker);
		this.context = context;
		this.mAISOverlayItems = new ArrayList<AISOverlayItem>();
		this.mRouteOverlayItems = new ArrayList<RouteOverlayItem>();
	}
	
	
	@Override
	protected void drawOverlayBitmap(Canvas canvas, Point drawPosition, Projection projection, byte drawZoomLevel) {
		super.drawOverlayBitmap(canvas, drawPosition, projection, drawZoomLevel);
		this.mPaint = new Paint();
		this.mPaint.setStyle(Paint.Style.STROKE);
		this.mPaint.setStrokeWidth(4);
		this.mPaint.setColor(Color.RED);

		int aWidth = canvas.getWidth();
		int aHeight = canvas.getHeight();
		Point aP = new Point(aWidth / 2, aHeight / 2);
		// projection.toPoint(drawPosition, aPixelPoint, drawZoomLevel);
		this.mOval = new RectF(aP.x - 5, aP.y - 5, aP.x + 5, aP.y + 5);
		if (mMustShow)
			canvas.drawOval(this.mOval, this.mPaint);
	}
	
	public void setMustShowCenter(boolean pMustShow) {
		mMustShow = pMustShow;
	}
	
	
	@Override
	protected boolean onTap(int index) {
		boolean result = false;
		// the tapped item might be a route-point-symbol or a AIStargetSysmbol
		OverlayItem aItem = createItem(index);
		if (aItem instanceof AISOverlayItem){
			AISOverlayItem item = (AISOverlayItem)aItem;
			result =  handleAISOverlayItem(item);
		}
		
		if (aItem instanceof RouteOverlayItem){
			RouteOverlayItem item = (RouteOverlayItem)aItem;
			result =  handleRoutePointOverlayItem(item);
		}
		return result;
	}
	
	private boolean handleRoutePointOverlayItem (RouteOverlayItem item){
		if (item != null) {
			GeoPoint aItemPoint = item.getPoint();
			String aNumberStr = item.getTitle();
			String aInfo = item.getSnippet();
			// find the index of the item in the route item list
			int index = item.getNumber();  
			
			/*int index = -1;
			try {
	    		  index = Integer.parseInt(aNumberStr); 
	    		} catch (Exception e){
	    			 e.printStackTrace();
	    		}*/
	    	int count = this.mRouteOverlayItems.size(); 
	    	if (count > index + 1 ){
	    		// there is a next RoutePoint in the array
				// we can calculate distance and course
	    		RouteOverlayItem nextItem = this.mRouteOverlayItems.get(index+1);
	    		GeoPoint nextPoint = nextItem.getPoint();
	    		double aDistance = PositionTools.calculateDistance(aItemPoint, nextPoint);
			    double aCourse = PositionTools.calculateCourse(aItemPoint, nextPoint);
			    String infoWp = context.getResources().getString(R.string.route_tap_info_waypoint);
			    String infoDist = context.getResources().getString(R.string.route_tap_info_distance);
			    String infoCourse = context.getResources().getString(R.string.route_tap_info_course);
			    aInfo = aInfo + "\n " +infoWp + " " + Integer.toString(index+1);
			    aInfo = aInfo + "\n " +infoDist+ " " + PositionTools.customFormat("000.000", aDistance) + " nM";
			    aInfo = aInfo + "\n "+ infoCourse + " " + PositionTools.customFormat("000.0",aCourse)+ "°";
			    
			    // calculate distance to last waypoint
				GeoPoint firstPoint =aItemPoint;
				double aSumDistance = 0;
				for (int index1 = index;index1 < count ;index1++) {
					nextItem = this.mRouteOverlayItems.get(index1);
					nextPoint = nextItem.getPoint();
					aDistance = PositionTools.calculateDistance(firstPoint, nextPoint);
					aSumDistance = aSumDistance + aDistance;
					firstPoint = nextPoint;
				}
				String infolastWp = context.getResources().getString(R.string.route_tap_info_lastwaypoint);
				aInfo = aInfo + "\n " + infolastWp + " " + Integer.toString(count-1);
				aInfo = aInfo + "\n " + infoDist + " " + PositionTools.customFormat("000.000", aSumDistance) + " nM";
				
	    	}
	    	if (count == index + 1) {
				// this is the last waypoint
				// we calculate the whole distance
				RouteOverlayItem firstItem = this.mRouteOverlayItems.get(0);
				GeoPoint firstPoint =firstItem.getPoint();
				double aSumDistance = 0;
				for (int index1 = 1;index1 < count ;index1++) {
					RouteOverlayItem nextItem = this.mRouteOverlayItems.get(index1);
					GeoPoint nextPoint = nextItem.getPoint();
					double aDistance = PositionTools.calculateDistance(firstPoint, nextPoint);
					aSumDistance = aSumDistance + aDistance;
					firstPoint = nextPoint;
				}
				String infoSumDist = context.getResources().getString(R.string.route_tap_info_sum_of_distance);
				aInfo = aInfo + "\n " + infoSumDist + " " + PositionTools.customFormat("000.000", aSumDistance) + " nM";
			}
		    	
			Builder builder = new AlertDialog.Builder(this.context);
			builder.setIcon(android.R.drawable.ic_menu_info_details);
			builder.setTitle(item.getTitle());
			builder.setMessage(aInfo);
			builder.setPositiveButton("OK", null);

			
			/*builder.setNeutralButton("move", new OnClickListener() { 
				 public void onClick(DialogInterface
			      dialog, int which) {
					 
				 } }); 
			*/
			 
			builder.show();

			return true;
		}
		return false;
	}
	
	/**
	 * Handles a tap event on the given item.
	 * we search in the targetList with the mmsi, which is stored in the title
	 */
	private boolean handleAISOverlayItem (AISOverlayItem item){
		if (item != null) {
			String aName = "---";
			GeoPoint aItemPoint = item.getPoint();
    		String aMMSIStr = item.getTitle();
    		String dialogTitle = aMMSIStr;
    		String aInfo = item.getSnippet();
    		AISTarget aTarget = null;
    		try {
    		  long aMMSI = Long.parseLong(aMMSIStr);
    		  aTarget = context.mTargetList.findTargetByMMSI(aMMSI);
    		  if (aTarget != null){
    		     if (!aTarget.getShipname().equals("")) {
	    		      aName = aTarget.getShipname();
	    	     }
    		  } else {
    			  if (aMMSIStr.equals(context.myShip.getMMSIString())){
      				aTarget = context.myShip; 
      				dialogTitle ="own boat";
      			 }
    		  }
    		} catch (Exception e){
    			 e.printStackTrace(); 
    			 // we get this exception if mmsi denotes myShip, cause it is not in the list
    			 // of the AISTargets
    			 if (aMMSIStr.equals(context.myShip.getMMSIString())){
    				aTarget = context.myShip;
    				dialogTitle ="own boat";
    			 }
    		}
    		
    		 // this is all set and available in the snippet,but the time is wrong  13_05_11
    		 // we do not have 
    		String specialManueverInfo = "xxx";
	      	  boolean showsBlueTable = false;
	      	  byte specialManueverStatus = aTarget.getManueverStatus();
	      	  switch (specialManueverStatus) {
	      	       case 0 : specialManueverInfo = context.getResources().getString(R.string.noBlueTableInfoAvailable);
	      	           break;
	      	       case 1 : specialManueverInfo = context.getResources().getString(R.string.blueTableNotSet);
	      	           break;
	      	       case 2 : specialManueverInfo = context.getResources().getString(R.string.blueTableIsSet);
	      	                showsBlueTable = true;
	      	           break;
	      	  }
    	    double aTargetLAT = aTarget.getLAT();
    		double aTargetLON = aTarget.getLON();
			  
	    	  
	    	String aCOGStr = aTarget.getCOGString();
	    	String aSOGStr = aTarget.getSOGString();
	    	long aUTCDiff = System.currentTimeMillis() - aTarget.getTimeOfLastPositionReport();
	    	String aTimeStr =PositionTools.getTimeString(aUTCDiff);
	    	int aDisplayStatus = aTarget.getStatusToDisplay();
	    	String aNavStatusString = aTarget.getNavStatusString();
	    	//String aInfoStr = AISTCPOpenMapPlotter.this.getResources().getString(R.string.aisItem_info1);
	    	String aInfoStr = context.getResources().getString(R.string.aisItem_info1);
	    	aInfo = aName  + "\n" + "LAT: " + PositionTools.getLATString(aTargetLAT) 
	                       + "\nLON: " + PositionTools.getLONString(aTargetLON) 
	                       + "\nCOG: " + aCOGStr 
	                       + "\nSOG: " + aSOGStr
	                       //+ "\nDisplayStatus " + aDisplayStatus  // for test 13_05_11
	                       + "\n" + specialManueverInfo
	                       + "\n" + aNavStatusString
	                       + "\n" + aInfoStr + " " + aTimeStr;
	    	  
	    	  
    		
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
			builder.setTitle(dialogTitle);
			builder.setMessage(aInfo);
			builder.setPositiveButton("OK", null);
			if (!item.hasTrack()){ 
				// targett has no track,we show a button to start recording the track
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
				// Target has a current track, pressing neutral button delete the track
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
										  if (test) Log.d(TAG,"Delete way "+ theMMSI + " " + index);
										  //context.mArrayAISTrackWayOverlay.removeWay(aOverlayWay); 13_03_16
										  context.mWaysOverlay.removeWay(theMMSI,aOverlayWay);
									  }
									  theWays.clear();
								  }
								aItem.setHasTrack( false);
								if (context.mDbAdapter.isTableToMMSIInShipTrackList(theMMSI)){
				            		context.mDbAdapter.deleteShipTrackTable(theMMSI);
				            		if (test) Log.d(TAG,"delete track table to "+ theMMSI);
				            		
				            	}
								actualTarget.setHasTrack(false);
								if (test) Log.d(TAG,"no further tracking on " + theName);
							}
						}
					}
				});
				// clicking negative button saves the track
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

	
	public AISOverlayItem getAISItem (int index) {
		AISOverlayItem result = null;
		OverlayItem aItem = createItem(index);
		if (aItem instanceof AISOverlayItem){
			result = (AISOverlayItem)aItem;
		}
		return result;
	}
	
	public RouteOverlayItem getRouteItem (int index) {
		RouteOverlayItem result = null;
		OverlayItem aItem = createItem(index);
		if (aItem instanceof RouteOverlayItem){
			result = (RouteOverlayItem)createItem(index);
		}
		return (RouteOverlayItem)aItem;
	}
	
	public int getAISListSize() {
		return this.mAISOverlayItems.size();
	}
	
	public int getRouteListSize(){
		return this.mRouteOverlayItems.size();
	}
	
	public AISOverlayItem getAISItemByMMSI(String pMMSIStr){
		AISOverlayItem result = null;
		boolean found = false;
		int aSize = this.mAISOverlayItems.size(); 
		  for (int index = 0;index < aSize; index++) {
	    	  AISOverlayItem aItem  =  this.mAISOverlayItems.get(index);
	    	  String aMMSIStr = aItem.getTitle();
	    	  if (aMMSIStr.equals(pMMSIStr)){
	    		  found = true;
	    		  result = aItem;
	    		  break;
	    	  }
		  }
		return result;
	}
	
	public void clearRouteItems() {
		
		while (this.mRouteOverlayItems.size() > 0){
			RouteOverlayItem  aItem = this.mRouteOverlayItems.get(0);
			this.removeItem(aItem);
		}	
	}
	
	public void clearAISItems() {
		while (this.mAISOverlayItems.size() > 0){
			AISOverlayItem  aItem = this.mAISOverlayItems.get(0);
			this.removeItem(aItem);
		}
	}
	
	@Override
	public void addItem(OverlayItem overlayItem) {
		super.addItem(overlayItem);
		if (overlayItem instanceof AISOverlayItem){
			this.mAISOverlayItems.add((AISOverlayItem)overlayItem);
		}
		if (overlayItem instanceof RouteOverlayItem) {
			this.mRouteOverlayItems.add((RouteOverlayItem) overlayItem);
		}
	}
	
	@Override
	public void clear() {
		super.clear();
		this.mAISOverlayItems.clear();
		this.mRouteOverlayItems.clear();
	}
	
	@Override
	public void removeItem(OverlayItem overlayItem) {
		if (overlayItem instanceof AISOverlayItem){
			this.mAISOverlayItems.remove((AISOverlayItem)overlayItem);
		}
		if (overlayItem instanceof RouteOverlayItem) {
			this.mRouteOverlayItems.remove((RouteOverlayItem) overlayItem);
		}
		super.removeItem(overlayItem);
	}
	
	/*public void updateItem (int index) {
		AISOverlayItem item = (AISOverlayItem)createItem(index);
		if (item != null) {
			String aMMSI = item.getTitle();
			
		}
	}*/
}
