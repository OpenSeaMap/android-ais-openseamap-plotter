package com.klein.overlays;

import org.mapsforge.android.maps.Projection;
import org.mapsforge.android.maps.overlay.ArrayItemizedOverlay;
import org.mapsforge.core.GeoPoint;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;



import com.klein.aistcpopenmapplotter051.R;

import com.klein.commons.PositionTools;

/**
 * 
 * @author vkADM
 * used to display  Routepoints that may be taped (see onTap)
 * there is only one instance mArrayRoutePointsOverlay
 * the database holds the information in the defaultRoute
 * the routePoints are connected with ways hold in mRouteArrayWayOverlay;
 * not used 
 */
public class ArrayRoutePointsItemizedOverlay extends ArrayItemizedOverlay {
	private final Context context;
	private Paint mPaint;
	private RectF mOval;
	private boolean mMustShow = false;

	/**
	 * Constructs a new AISItemizedOverlay.
	 * 
	 * @param defaultMarker
	 *            the default marker (may be null).
	 * @param context
	 *            the reference to the application context.
	 */
	public ArrayRoutePointsItemizedOverlay(Drawable defaultMarker, Context context) {
		super(defaultMarker);
		this.context = context;
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

	/**
	 * Handles a tap event on the given item.
	 * if the last routepoint is tapped, it displays the length of the route
	 * if another oute pount is tapped, it displays the distance and course to the next route point,
	 * the sum of distances to the last routepoint
	 */
	@Override
	protected boolean onTap(int index) {
		RouteOverlayItem item = (RouteOverlayItem) getItem(index);
		if (item != null) {
			GeoPoint aItemPoint = item.getPoint();
			String aNumber = item.getTitle();
			String aInfo = item.getSnippet();
			int count = size();
			if ( count > index +1) {
				// there is a next item in the array
				// we can calculate distance and course
				RouteOverlayItem nextItem = getItem(index+1);
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
			    RouteOverlayItem firstItem = getItem(index);
				GeoPoint firstPoint =firstItem.getPoint();
				double aSumDistance = 0;
				for (int index1 = index;index1 < count ;index1++) {
					nextItem = getItem(index1);
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
				RouteOverlayItem firstItem = getItem(0);
				GeoPoint firstPoint =firstItem.getPoint();
				double aSumDistance = 0;
				for (int index1 = 1;index1 < count ;index1++) {
					RouteOverlayItem nextItem = getItem(index1);
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
	 * helper to get a item
	 * @param index
	 * @return
	 */
	public RouteOverlayItem getItem(int index) {
		return (RouteOverlayItem) createItem(index);
	}
	/**
	 * update a item,does nothing
	 * @param index
	 */
	public void updateItem(int index) {
		RouteOverlayItem item = (RouteOverlayItem) createItem(index);
		if (item != null) {
			
		}
	}
}
