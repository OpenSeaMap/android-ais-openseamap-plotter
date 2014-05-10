package com.klein.service;


import java.util.ArrayList;

import org.mapsforge.android.maps.Projection;
import org.mapsforge.core.GeoPoint;

import android.graphics.Canvas;
import android.graphics.Point;
import android.util.Log;

import com.klein.aistcpopenmapplotter051.AISTCPOpenMapPlotter;





public class DisplayObjectList {
    private ArrayList <PaintObject>mPaintObjectList = null;
    private static String TAG ="DisplayObjectList";
    private boolean test = false;
    //private int mGridDistance = 10;
    private AISTCPOpenMapPlotter mActivity = null;
    
    
	public DisplayObjectList() {
		if (test) Log.v(TAG,"DisplayObjectList--> create");
		mPaintObjectList = new ArrayList<PaintObject>();
	}
	
	public void clear(){
		mPaintObjectList.clear();	
	}
	
	public boolean isEmpty(){
		return mPaintObjectList.isEmpty();
	}
	
	public int getSize() {
		return mPaintObjectList.size();
	}
	
	/*public void setGridDistance (int aDistance){
		mGridDistance = aDistance;
	}*/
	
	public void setActivity(AISTCPOpenMapPlotter aActivity){
    	mActivity = aActivity;
    }
	
	
	/**
	 * 
	 * @param aTargetList  set the PaintObject list from the TargetList
	 */
	public void setPaintObjectList(TargetList aTargetList){
		if (test) Log.v(TAG,"DisplayObjectList--> setPaintObjectList");
		mPaintObjectList.clear();
		if ((!(aTargetList == null)) && (aTargetList.getSize()> 0)){
			
			for (int index = 0;index < aTargetList.getSize();index++) {
			  AISTarget aTarget = aTargetList.getTargetByNr(index);
			  int aDisplayStatus = aTarget.getStatusToDisplay();
			  if ((aDisplayStatus > 0)){
				  PaintObject aPaintObject = new PaintObject(aTarget);
				  mPaintObjectList.add(aPaintObject);
			  }
		    }
		}
	}
	
	/**
	 * 
	 * @param aLockedCanvas the actual canvas to draw on
	 */
	public void paintList(Canvas canvas , Projection aProjection, boolean showSpeedMarkerOfTarget, boolean showOnSatteliteMap){
		
		if ((!(mPaintObjectList == null))&&(mPaintObjectList.size()> 0 )){
			
			//float pixelsForMile  = aProjection.metersToEquatorPixels(1852);
			//if (test) Log.d(TAG,"PixelsForMile" + pixelsForMile);
			for (int index = 0; index < mPaintObjectList.size();index++){
				PaintObject aPaintObject = mPaintObjectList.get(index);
				
				AISTarget aTarget = aPaintObject.getAISTarget();
				if (aTarget != null) {
					// in google: GeoPoint gp = new GeoPoint((int )(aTarget.getLAT()*1E6), (int) (aTarget.getLON()*1E6));
			  	    GeoPoint gp= new GeoPoint(aTarget.getLAT(),aTarget.getLON());
			  	    Point point = new Point();
			  	      // in Point werden die relativen Pixelkoordinaten gesetzt:
			  	    aProjection.toPixels(gp, point);
					String aName = "xxxx";
					if (!(aTarget.getShipname().equals(""))) aName =aTarget.getShipname() ;
					//aPaintObject.setCanvas(canvas,pixelsForMile);
					/*boolean nearby = false;
					aPaintObject.paintShipPolygon(point, nearby);*/
					
					double aDiffX = aTarget.getLON() - mActivity.getMyShip().getLON();
					double aDiffY = aTarget.getLAT() - mActivity.getMyShip().getLAT();
					double aDiff = aDiffX * aDiffX + aDiffY * aDiffY;
					aDiff = Math.abs(Math.sqrt(aDiff));  // in Grad
					double aDiffMiles = aDiff * 60; // one minute equals one mile
					
					// google boolean canCover = mapView.canCoverCenter();
					boolean canCover = true;
					boolean nearby = (aDiffMiles < mActivity.getAISWarningDistance());
					
					aPaintObject.paintShipPolygon(point, nearby,canCover,showSpeedMarkerOfTarget,showOnSatteliteMap);
				}
			}
			
		}
	}
	
	public PaintObject findByPosition(Point aPoint, int aHitDistance){
		if ((!(mPaintObjectList == null))&&(mPaintObjectList.size()> 0 )){
			for (int index = 0; index < mPaintObjectList.size();index++){
				PaintObject aPaintObject = mPaintObjectList.get(index);
				if (aPaintObject.isHit(aPoint,aHitDistance)) {
					return aPaintObject;
				}
				/*Point aLastPoint = aPaintObject.getLastPoint();
				if ((Math.abs(aLastPoint.x - aPoint.x ) < aDistance)
						&&(Math.abs(aLastPoint.y - aPoint.y) < aDistance)){
					return aPaintObject;
				}*/
			}
		}
		return null;
	}
	
	
	
}
