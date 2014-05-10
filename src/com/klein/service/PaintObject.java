package com.klein.service;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.Log;

import com.klein.commons.AISPlotterGlobals;
import com.klein.commons.PositionTools;


/**
 * @author vk1
 *  depreciated since Version 031
 */
public class PaintObject {
     private Canvas mCanvas = null;
	 private AISTarget mAISTarget = null;
	 private float mDistance = 1;
	 private Point mLastPoint;
	 private static String TAG ="PaintObject";
	 private static boolean test = false;
	 
	/* this information is kept in AISPlotterGlobals
	 * public static final int DISPLAYSTATUS_0 = 0; // nicht angezeigt
	 
	 public static final int DISPLAYSTATUS_INACTIVE = 1; // durchgestrichen, zu alt
	 public static final int DISPLAYSTATUS_HAS_POSITION = 2; // nur PositionReport, Rahmen
	 //public static final int DISPLAYSTATUS_MOORED = 3; // cyan
	 public static final int DISPLAYSTATUS_HAS_SHIPREPORT = 4; // ShipReport, als ausgefüllt
	 public static final int DISPLAYSTATUS_SELECTED = 5; // mit Name und Kreis
	 													// rot, wenn innerhalb des Alarmkreises
	 public static final int DISPLAYSTATUS_BASE_STATION = 10;
	 public static final int DISPLAYSTATUS_OWN_SHIP = 100;*/
	/**
	 * 
	 * @param pTarget   AISTarget to paint
	 * @param pLastPoint last known point
	 */
	public PaintObject(AISTarget pTarget , Point pStartPoint) {
		mAISTarget = pTarget;
		mLastPoint = pStartPoint;  // will be initalized to (0,0)
	}
	
	public PaintObject(AISTarget pTarget){
		mAISTarget = pTarget;
		mLastPoint = new Point();
		mLastPoint.x = 0;
		mLastPoint.y = 0;
	}
	/**
	 * 
	 * @return the associated AISTarget
	 */
	public AISTarget getAISTarget() {
		return mAISTarget;
	}
	
	public Point getLastPoint() {
		return mLastPoint;
	}
	
	private static int abs (int a){
		if ( a < 0 ){ 
			return -a;
		}
		else  { 
			return a;
		}
	}
	/**
	 * 
	 * @param aPoint the Point to test 
	 * @return true, if the object is in a Square of radius 10 of the last point
	 * @see the visible status of the assiciated object is changed 
	 * @
	 */
	public boolean isHit (Point aPoint, int aHitDistance){
		String aName = mAISTarget.getShipname();
	    if (test) Log.v(TAG,"isHit " + aName);
		boolean result = false;
		if ( (abs(mLastPoint.x - aPoint.x) < aHitDistance)
		     && (abs(mLastPoint.y- aPoint.y ) < aHitDistance ) ) {
		   	result = true;
		}
		return result;
	}

	
	/**
	 * 
	 * @param pCanvas the actual Canvas to draw on
	 * @param pWidth  width of the Canvas, used for calculation the length of the speed indicator
	 * @param pDistance the distance of the Grid
	 */
	
	public void setCanvas (Canvas pCanvas , float pDistance){
		mCanvas = pCanvas;
		mDistance = pDistance;  // pixel for a mile on map with actual zoom // aLatSpann Entfernung von Karte oben bis Karte unten in meilen
	}

	/**
	 * 
	 * @param aPoint the object should be painted
	 * @param nearby is the object nearby to us, then paint red
	 */
	public void paintShipPolygon ( Point aPoint , boolean nearby, boolean canCover, 
			                       boolean showSpeedMarkerOfTarget, boolean showOnSatteliteMap) {
		mLastPoint = aPoint;
		int status = mAISTarget.getStatusToDisplay();
		
		String aName = mAISTarget.getShipname();
		/*int midX = mCanvas.getWidth() / 2;
		int midY = mCanvas.getHeight() / 2;
		int x = mLastPoint.x - midX;
		int y = -(mLastPoint.y - midY);*/
		
		int targetColor ;
		int textColor;
		
		if ( status > AISPlotterGlobals.DISPLAYSTATUS_0 ){
			// we have to display
			// if (test) Log.v(TAG,"Name " + status + " " + aName + " x " +  x + " y " + y );
			 int h = 5;
			 Paint paint = new Paint(); 
		   	 paint.setStyle(Paint.Style.STROKE);
		   	 targetColor = Color.GREEN;
	   		 textColor = Color.BLACK;
		   	 // if canCover is true we have a map tile in the background
		   	 // we show the targets on the satellite tile in white
		   	 if (canCover&& showOnSatteliteMap) {
		   		 targetColor = Color.WHITE;
		   		 textColor =Color.WHITE;
		   	 }
		   	 if (nearby) targetColor = Color.RED;
		   	 double theCOG = mAISTarget.getCOG() / 10;
		   	 String navstat = mAISTarget.getNavStatusString();
			 boolean isMoored = (navstat.equals("Moored")) || (theCOG < 0.1f);
		   	 double aCOG=  -theCOG  * Math.PI / 180f;   // Kurs ist rechtsdrehend, sin und cos linksdrehend, also - theCOG
		   	 
		   	 /*double x1Rot =  -1 * h * Math.cos(aCOG);
	         double y1Rot =  -1 * h * Math.sin(aCOG);
	         double x2Rot =   1 * h * Math.cos(aCOG);
	         double y2Rot =   1 * h * Math.sin(aCOG);
	         double x3Rot =  -4 * h * Math.sin(aCOG);
	         double y3Rot =   4 * h * Math.cos(aCOG);
	         
	         
	         float pts[] = new float[12];
	         pts[0]  = aPoint.x + (float)x1Rot;
	         pts[1]  = aPoint.y - (float)y1Rot;
	         pts[2]  = aPoint.x + (float)x2Rot;
	         pts[3]  = aPoint.y - (float)y2Rot;
	         
	         pts[4]  = aPoint.x + (float)x2Rot;
	         pts[5]  = aPoint.y - (float)y2Rot;
	         pts[6]  = aPoint.x + (float)x3Rot;
	         pts[7]  = aPoint.y - (float)y3Rot;
	         
	         pts[8]  = aPoint.x + (float)x3Rot;
	         pts[9]  = aPoint.y - (float)y3Rot;
	         pts[10] = aPoint.x + (float)x1Rot;
	         pts[11] = aPoint.y - (float)y1Rot;
	         
	         Path aPath = new Path();
	         aPath.moveTo(aPoint.x + (float) x1Rot, aPoint.y - (float)y1Rot);
	         aPath.lineTo(aPoint.x + (float) x2Rot, aPoint.y - (float)y2Rot);
	         aPath.lineTo(aPoint.x + (float) x3Rot, aPoint.y - (float)y3Rot);
	         aPath.lineTo(aPoint.x + (float) x1Rot, aPoint.y - (float)y1Rot);
	         */
		   	 //aCog ist linksdrehend wie sind und cos
		   	 double angleToP3 = (90 - theCOG)* Math.PI / 180f ;  // 90 - theCOG
		   	 double angleToP1 = (180 - theCOG)* Math.PI / 180f;  // angleToP3 + 90
		   	 double angleToP2 = (-theCOG)* Math.PI / 180f;   // angleToP3 - 90
	         double x1Rot =   h * Math.cos(angleToP1);  //      pt3
	         double y1Rot =   h * Math.sin(angleToP1);  //       |
	         double x2Rot =   h * Math.cos(angleToP2);  //  pt1--|--- pt2
	         double y2Rot =   h * Math.sin(angleToP2);
	         double x3Rot =   4 * h * Math.cos(angleToP3);
	         double y3Rot =   4 * h * Math.sin(angleToP3);
	         
	         
	         float pts[] = new float[12];
	         pts[0]  = aPoint.x + (float)x1Rot;
	         pts[1]  = aPoint.y - (float)y1Rot;
	         pts[2]  = aPoint.x + (float)x2Rot;
	         pts[3]  = aPoint.y - (float)y2Rot;
	         
	         pts[4]  = aPoint.x + (float)x2Rot;
	         pts[5]  = aPoint.y - (float)y2Rot;
	         pts[6]  = aPoint.x + (float)x3Rot;
	         pts[7]  = aPoint.y - (float)y3Rot;
	         
	         pts[8]  = aPoint.x + (float)x3Rot;
	         pts[9]  = aPoint.y - (float)y3Rot;
	         pts[10] = aPoint.x + (float)x1Rot;
	         pts[11] = aPoint.y - (float)y1Rot;
	         
	         Path aPath = new Path();
	         aPath.moveTo(aPoint.x + (float) x1Rot, aPoint.y - (float)y1Rot);
	         aPath.lineTo(aPoint.x + (float) x2Rot, aPoint.y - (float)y2Rot);
	         aPath.lineTo(aPoint.x + (float) x3Rot, aPoint.y - (float)y3Rot);
	         aPath.lineTo(aPoint.x + (float) x1Rot, aPoint.y - (float)y1Rot);
	         
	         
	         if (status == AISPlotterGlobals.DISPLAYSTATUS_HAS_POSITION) {
	        	 paint.setColor(targetColor);
	        	 paint.setStyle(Paint.Style.STROKE);
	        	 mCanvas.drawLines(pts,0,12,paint);
	         }
	         if (status == AISPlotterGlobals.DISPLAYSTATUS_INACTIVE) {
	        	 //targetColor = Color.YELLOW;
	        	 paint.setColor(targetColor);
	        	 paint.setStyle(Paint.Style.STROKE);
	        	 mCanvas.drawLines(pts,0,12,paint);
	        	// Schräger Strich zur Objekt
	        	 double angleToP5 = (30 - theCOG)* Math.PI / 180f ;  // schräger Strich 30 - theCOG
			   	 double angleToP6 = (210 - theCOG)* Math.PI / 180f;  //          angleToP3 + 90 + 30
			   	 
		         double x5Rot =   3 *h * Math.cos(angleToP5);  //      
		         double y5Rot =   3 *h * Math.sin(angleToP5);  //      
		         double x6Rot =   3 *h * Math.cos(angleToP6);
		         double y6Rot =   3 *h * Math.sin(angleToP6);
		         // move basic loc in direction of p3 half
		         x5Rot = x5Rot + (float) (x3Rot / 2);
		         y5Rot = y5Rot + (float) (y3Rot / 2);
		         x6Rot = x6Rot + (float) (x3Rot / 2);
		         y6Rot = y6Rot + (float) (y3Rot / 2);
		         mCanvas.drawLine(aPoint.x +(float)x5Rot, aPoint.y -(float)y5Rot, 
		        		          aPoint.x + (float)x6Rot,aPoint.y - (float)y6Rot, paint);
	         }
	         
	         if (status == AISPlotterGlobals.DISPLAYSTATUS_BASE_STATION) {
	        	 targetColor = Color.BLUE;
	        	 if (showOnSatteliteMap)targetColor = Color.GREEN;
	        	 paint.setColor(targetColor); 
	        	 paint.setStyle(Paint.Style.FILL);
	        	 mCanvas.drawRect(aPoint.x -5, aPoint.y-5, aPoint.x+5, aPoint.y+5, paint);
	        	 String aString =  "Base Station";
	        	 mCanvas.drawText(aString, aPoint.x + 10, aPoint.y, paint); paint.setColor(Color.BLACK);
	         }
	         if (isMoored) {
	        	 targetColor = Color.CYAN;
	        	 paint.setColor(targetColor); 
	        	 paint.setStyle(Paint.Style.FILL);
	   
	         }
	         if (status == AISPlotterGlobals.DISPLAYSTATUS_HAS_SHIPREPORT) {
	        	 if (aName.equals("myShip")) {
	        		 targetColor = Color.BLUE;
	        		 if (showOnSatteliteMap)targetColor = Color.GREEN;
	        	 }
	        	 paint.setColor(targetColor); 
	        	 paint.setStyle(Paint.Style.FILL);
	        	 mCanvas.drawPath(aPath, paint);
	        	 //mCanvas.drawLines(pts,0,12,paint);
	        	 
	         }
	         if (status == AISPlotterGlobals.DISPLAYSTATUS_SELECTED) {
	        	 paint.setColor(targetColor);
	        	 paint.setStyle(Paint.Style.FILL);
	        	 //mCanvas.drawLines(pts,0,12,paint);
	        	 mCanvas.drawPath(aPath, paint);
	        	 paint.setColor(textColor);
	        	 
			      long aUTCDiff = System.currentTimeMillis() - mAISTarget.getTimeOfLastPositionReport();
			      String aString = mAISTarget.getShipname()  ;
			      
			      float aOldTextSize = paint.getTextSize();
			      //paint.setTextSize(12);
			      
			      if (test) 
			    	  {
			    	  Log.d(TAG,aString);
			    	  
			    	  }
			     mCanvas.drawText(aString, aPoint.x + 10, aPoint.y-10, paint); 
			     aString = PositionTools.getTimeString(aUTCDiff) + " " + mAISTarget.getSOGString()+ "kn ";
			     mCanvas.drawText(aString, aPoint.x + 10, aPoint.y+10, paint); 
			     
			     
			     paint.setColor(Color.BLACK);
			     //paint.setTextSize(aOldTextSize);	 
	        	 paint.setStyle(Paint.Style.STROKE); 
	        	 mCanvas.drawCircle(aPoint.x, aPoint.y,10, paint);
	        	 paint.setColor(targetColor);
	         }
	         // drawSpeedIndicator
	         //aCOG  = ((theCOG + 90)* Math.PI/180f);
	         
	         if (showSpeedMarkerOfTarget == true) {
		         float theSOG = mAISTarget.getSOG() / 10;
		         //float aFactor = (float)(mWidth / mDistance) / h * theSOG;
		         //float aFactor = (float) mDistance / 4 / h * theSOG; 
		         // warum /4 /h ?? das ist der Faktor für die Spitze, steckt in x3Rot und y3Rot drin
		         float aFactor = (float) mDistance / 4 / h * theSOG;
		         /*float x4Rot = (float) aFactor * (float)(theSOG * Math.cos(aCOG))*(-1);
		         float y4Rot = (float) aFactor * (float)(theSOG * Math.sin(aCOG));*/
		         float x4Rot = (float)(x3Rot * aFactor);
		         float y4Rot = (float)(y3Rot * aFactor);
		         paint.setStyle(Paint.Style.FILL);
		         mCanvas.drawLine(aPoint.x, aPoint.y, aPoint.x + x4Rot,aPoint.y - y4Rot, paint);
	         }
	         // for test only bullets at the tops of the triangle
	         /*paint.setColor(Color.MAGENTA);
	         mCanvas.drawCircle(pts[0], pts[1],3, paint);
	         paint.setColor(Color.BLACK);
	         mCanvas.drawCircle(pts[4], pts[5],3, paint);
	         paint.setColor(Color.CYAN);
	         mCanvas.drawCircle(pts[8], pts[9],3, paint);*/
		} // status > 0
	}
	
	
	
	

}
