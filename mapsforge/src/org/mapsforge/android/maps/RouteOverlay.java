/*
 * Copyright 2010 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.android.maps;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;

/**
 * This overlay is intended for visualizing routes. A route is a collection of waypoints forming
 * a line.
 * 
 * @author Karsten Groll
 */
public class RouteOverlay extends Overlay {
	/** Canvas used for drawing onto {@link #shadowBmp}. */
	private Canvas internalCanvas;
	/** Matrix used for translating the overlay. */
	private Matrix matrix;
	/** Paint used for styling the path. */
	private Paint paint;
	/** Path holding the waypoints pixel coordinates. */
	private Path path;
	/** Array holding the waypoints. */
	private double routeData[][];
	/** The bitmap the route is drawn onto. */
	private Bitmap shadowBmp;
	/** Temporary bitmap used for switching bitmap and shadowBmp. */
	private Bitmap tmpBmp;
	/** Map's x-position after rendering. */
	private double xPosAfter;
	/** Map's x-position before rendering. */
	private double xPosBefore;
	/** Map's y-position after rendering. */
	private double yPosAfter;
	/** Map's y-position before rendering. */
	private double yPosBefore;

	/**
	 * Constructor
	 */
	public RouteOverlay() {
		this.matrix = new Matrix();
		this.internalCanvas = new Canvas();

		this.paint = new Paint();
		this.paint.setColor(Color.BLUE);
		this.paint.setAlpha(100);
		this.paint.setStyle(Paint.Style.STROKE);
		this.paint.setStrokeJoin(Paint.Join.ROUND);
		this.paint.setStrokeCap(Paint.Cap.ROUND);
		this.paint.setStrokeWidth(5);
		this.path = new Path();

		this.start();
	}

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		canvas.drawBitmap(this.bmp, this.matrix, null);
		// Log.d("test", this.matrix.toShortString());
	}

	/**
	 * Sets the route data (Array of waypoints).
	 * 
	 * @param routeData
	 *            Array containing of arrays containing the route data. Structure
	 * 
	 *            <pre>
	 * {{lat1, lon1}, {lat2, lon2}, ...}
	 * </pre>
	 */
	public void setRouteData(double[][] routeData) {
		this.routeData = routeData.clone();
	}

	@Override
	protected void createOverlayBitmapsAndCanvas(int width, int height) {
		this.bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		this.shadowBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
	}

	@Override
	protected Matrix getMatrix() {
		return this.matrix;
	}

	@Override
	protected void prepareOverlayBitmap(MapView mapView) {
		if (this.routeData == null) {
			return;
		}

		// Tell the internalCanvas to use a new bitmap
		this.internalCanvas.setBitmap(this.shadowBmp);
		// Erase the bmp's content
		this.shadowBmp.eraseColor(Color.TRANSPARENT);

		// Save current position
		this.xPosBefore = mapView.mapViewPixelX;
		this.yPosBefore = mapView.mapViewPixelY;

		// Create our path
		this.path.reset();
		this.matrix.reset();

		float x1, y1, x2, y2;
		double dx = mapView.mapViewPixelX;
		double dy = mapView.mapViewPixelY;

		x1 = (float) (MercatorProjection.longitudeToPixelX(this.routeData[0][1],
				mapView.zoomLevel) - dx);
		y1 = (float) (MercatorProjection.latitudeToPixelY(this.routeData[0][0],
				mapView.zoomLevel) - dy);
		this.path.moveTo(x1, y1);
		for (int i = 1; i < this.routeData.length; i++) {
			x2 = (float) (MercatorProjection.longitudeToPixelX(this.routeData[i][1],
					mapView.zoomLevel) - dx);
			y2 = (float) (MercatorProjection.latitudeToPixelY(this.routeData[i][0],
					mapView.zoomLevel) - dy);

			this.path.lineTo(x2, y2);

			x1 = x2;
			y1 = y2;
		}

		// Draw the path
		this.internalCanvas.drawPath(this.path, this.paint);

		// Save current position (again)
		this.xPosAfter = mapView.mapViewPixelX;
		this.yPosAfter = mapView.mapViewPixelY;

		synchronized (this.bmp) {
			// this.matrix.reset();

			// Calculate how many pixels the MapView has been shifted since the beginning of the
			// calculation and fix the matrix according to it
			this.matrix.postTranslate((float) (this.xPosBefore - this.xPosAfter),
					(float) (this.yPosBefore - this.yPosAfter));
			// Swap bitmaps
			this.tmpBmp = this.bmp;
			this.bmp = this.shadowBmp;
			this.shadowBmp = this.tmpBmp;
		}

		// Force redraw
		mapView.postInvalidate();
	}
}