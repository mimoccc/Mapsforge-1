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
package org.mapsforge.android.map;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;

public class RouteOverlay extends Overlay {

	private Matrix matrix;
	private Path path;
	private Path tmpPath;
	private static Paint paint;
	private static String TAG = "RouteOverlay";
	private byte zoomLevel;

	// Last known positions of the views (0,0) position
	// (used for determining how far the map has been moved)
	private double lastX;
	private double lastY;
	// The route
	private double[][] routeData;

	{
		paint = new Paint();
		paint.setColor(Color.BLUE);
		paint.setAlpha(100);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeJoin(Paint.Join.ROUND);
		paint.setStrokeCap(Paint.Cap.ROUND);
		paint.setStrokeWidth(5);
	}

	public RouteOverlay() {
		super();
		Log.d(TAG, "RouteOverlay has been created");

		this.matrix = new Matrix();
		this.matrix.reset();
		this.path = new Path();

		this.zoomLevel = 0;
		this.lastX = 0;
		this.lastY = 0;

		this.start();
	}

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		// Log.d(TAG, "draw");
		// canvas.drawBitmap(this.bmp, mapView.matrix, paint);
		synchronized (this.path) {
			canvas.drawPath(this.path, RouteOverlay.paint);
		}
	}

	/**
	 * 
	 * @param routeData
	 *            Array containing of arrays containing the route data. Structure
	 * 
	 *            <pre>
	 * {{lat1, lon1}, {lat2, lon2}, ...}
	 * </pre>
	 */
	public void setPath(double[][] routeData) {
		this.routeData = routeData;
	}

	@Override
	protected void createOverlayBitmapsAndCanvas(int width, int height) {
		Log.d(TAG, "createOverlayBitmaps");
	}

	@Override
	protected Matrix getMatrix() {
		return this.matrix;
	}

	@Override
	protected void setMapViewAndCreateOverlayBitmaps(MapView mapView) {
		this.mapView = mapView;
		this.lastX = mapView.mapViewPixelX;
		this.lastY = mapView.mapViewPixelY;
	}

	@Override
	protected void prepareOverlayBitmap(MapView mapView) {
		Log.d(TAG, "preparing overlay bitmap");

		// Current position of the view's (0,0) coordinate
		double dx = mapView.mapViewPixelX;
		double dy = mapView.mapViewPixelY;

		// Recalculate coordinates if zoomlevel has changed
		if (this.zoomLevel != mapView.zoomLevel) {
			Log.d(TAG, "zoomlevel has changed");

			if (this.routeData == null) {
				return;
			}

			this.tmpPath = new Path();
			this.zoomLevel = mapView.zoomLevel;

			for (int i = 0; i < routeData.length - 1; i++) {
				this.tmpPath.moveTo((float) (MercatorProjection.longitudeToPixelX(
						routeData[i][1], mapView.zoomLevel) - dx), (float) (MercatorProjection
						.latitudeToPixelY(routeData[i][0], mapView.zoomLevel) - dy));

				this.tmpPath.lineTo((float) (MercatorProjection.longitudeToPixelX(
						routeData[i + 1][1], mapView.zoomLevel) - dx),
						(float) (MercatorProjection.latitudeToPixelY(routeData[i + 1][0],
								mapView.zoomLevel) - dy));
			}
		} else { // Translate coordinates according to the distance the map was moved by

			this.matrix.reset();
			// this.matrix.postTranslate(-5f, 0.0f);
			this.matrix.postTranslate((float) (this.lastX - dx), (float) (this.lastY - dy));
			Log.d(TAG, "dx: " + (float) (this.lastX - dx));
			this.path.transform(this.matrix);

		}

		this.lastX = dx;
		this.lastY = dy;

		// for (int i = 0; i < 200; i++) {
		// this.tmpPath.moveTo(i, i);
		// this.tmpPath.lineTo(i + 1, i + 1);
		// }

		synchronized (this.path) {
			this.path = this.tmpPath;
		}

		this.mapView.postInvalidate();
	}
}
