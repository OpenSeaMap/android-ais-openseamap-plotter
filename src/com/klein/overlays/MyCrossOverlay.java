/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.klein.overlays;

import org.mapsforge.android.maps.Projection;
import org.mapsforge.android.maps.overlay.Overlay;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;

/**
 * 
 * @author vkADM
 *  not used
 */
public class MyCrossOverlay extends Overlay {
	private Paint mPaint;
	private RectF mOval;
	private boolean mMustShow = false;

	@Override
	protected void drawOverlayBitmap(Canvas canvas, Point drawPosition, Projection projection, byte drawZoomLevel) {
		// TODO Auto-generated method stub
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

	public void setMustShow(boolean pMustShow) {
		mMustShow = pMustShow;
	}

}
