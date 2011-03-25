/*
 * Copyright 2010, 2011 mapsforge.org
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
import android.graphics.Point;

/**
 * Overlay is the abstract base class for all types of overlays. It handles the lifecycle of the
 * overlay thread and implements those parts of the redrawing process which all overlays have in
 * common.
 * <p>
 * To add an overlay to a <code>MapView</code>, create a subclass of this class and add an
 * instance to the list returned by {@link MapView#getOverlays()}. When an overlay gets removed
 * from the list, the corresponding thread is automatically interrupted and all its resources
 * are freed. Re-adding a previously removed overlay to the list will therefore cause an
 * {@link IllegalThreadStateException}.
 */
public abstract class Overlay extends Thread {
	private static final String THREAD_NAME = "Overlay";

	/**
	 * Flag to indicate if this overlay is set up and ready to work.
	 */
	private boolean isSetUp;

	/**
	 * A cached reference to the MapView projection.
	 */
	private Projection mapViewProjection;

	/**
	 * Transformation matrix for the overlay.
	 */
	private final Matrix matrix;

	/**
	 * Used to calculate the scale of the transformation matrix.
	 */
	private float matrixScaleFactor;

	/**
	 * First internal bitmap for the overlay to draw on.
	 */
	private Bitmap overlayBitmap1;

	/**
	 * Second internal bitmap for the overlay to draw on.
	 */
	private Bitmap overlayBitmap2;

	/**
	 * A temporary reference to swap the two overlay bitmaps.
	 */
	private Bitmap overlayBitmapSwap;

	/**
	 * Canvas that is used in the overlay for drawing.
	 */
	private Canvas overlayCanvas;

	/**
	 * Stores the top-left map position at which the redraw should happen.
	 */
	private final Point point;

	/**
	 * Stores the map position before drawing starts.
	 */
	private Point positionAfterDraw;

	/**
	 * Stores the map position after drawing is finished.
	 */
	private Point positionBeforeDraw;

	/**
	 * Flag to indicate if the overlay should redraw itself.
	 */
	private boolean redraw;

	/**
	 * Stores the zoom level after drawing is finished.
	 */
	private byte zoomLevelAfterDraw;

	/**
	 * Stores the zoom level before drawing starts.
	 */
	private byte zoomLevelBeforeDraw;

	/**
	 * Used to calculate the zoom level difference.
	 */
	private byte zoomLevelDiff;

	/**
	 * Reference to the MapView instance.
	 */
	protected MapView internalMapView;

	/**
	 * Default constructor which must be called by all subclasses.
	 */
	protected Overlay() {
		this.isSetUp = false;
		this.matrix = new Matrix();
		this.point = new Point();
		this.positionBeforeDraw = new Point();
		this.positionAfterDraw = new Point();
	}

	/**
	 * Handles a tap event.
	 * <p>
	 * The default implementation of this method does nothing and returns false.
	 * 
	 * @param geoPoint
	 *            the point which has been tapped.
	 * @param mapView
	 *            the MapView that triggered the tap event.
	 * @return true if the tap event was handled, false otherwise.
	 */
	public boolean onTap(GeoPoint geoPoint, MapView mapView) {
		return false;
	}

	/**
	 * Requests a redraw of the overlay.
	 */
	public final void requestRedraw() {
		this.redraw = true;
		synchronized (this) {
			notify();
		}
	}

	@Override
	public final void run() {
		setName(getThreadName());

		while (!isInterrupted()) {
			synchronized (this) {
				while (!isInterrupted() && (!this.redraw)) {
					try {
						wait();
					} catch (InterruptedException e) {
						// restore the interrupted status
						interrupt();
					}
				}
			}

			if (isInterrupted()) {
				break;
			}

			this.redraw = false;
			if (this.isSetUp) {
				redraw();
			}
		}

		// free the overlay bitmaps memory
		if (this.overlayBitmap1 != null) {
			this.overlayBitmap1.recycle();
			this.overlayBitmap1 = null;
		}

		if (this.overlayBitmap2 != null) {
			this.overlayBitmap2.recycle();
			this.overlayBitmap2 = null;
		}

		// set some fields to null to avoid memory leaks
		this.internalMapView = null;
		this.mapViewProjection = null;
		this.overlayCanvas = null;
	}

	/**
	 * Redraws the overlay.
	 */
	private void redraw() {
		this.mapViewProjection = this.internalMapView.getProjection();

		// clear the second bitmap and make the canvas use it
		this.overlayBitmap2.eraseColor(Color.TRANSPARENT);
		this.overlayCanvas.setBitmap(this.overlayBitmap2);

		// save the zoom level and map position before drawing
		synchronized (this.internalMapView) {
			this.zoomLevelBeforeDraw = this.internalMapView.getZoomLevel();
			this.positionBeforeDraw = this.mapViewProjection.toPoint(this.internalMapView
					.getMapCenter(), this.positionBeforeDraw, this.zoomLevelBeforeDraw);
		}

		// calculate the top-left point of the visible rectangle
		this.point.x = this.positionBeforeDraw.x - (this.overlayCanvas.getWidth() >> 1);
		this.point.y = this.positionBeforeDraw.y - (this.overlayCanvas.getHeight() >> 1);

		// call the draw implementation of the subclass
		this.drawOverlayBitmap(this.overlayCanvas, this.point, this.mapViewProjection,
				this.zoomLevelBeforeDraw);

		// save the zoom level and map position after drawing
		synchronized (this.internalMapView) {
			this.zoomLevelAfterDraw = this.internalMapView.getZoomLevel();
			this.positionAfterDraw = this.mapViewProjection.toPoint(this.internalMapView
					.getMapCenter(), this.positionAfterDraw, this.zoomLevelBeforeDraw);
		}

		// adjust the transformation matrix of the overlay
		synchronized (this.matrix) {
			this.matrix.reset();
			this.matrix.postTranslate(this.positionBeforeDraw.x - this.positionAfterDraw.x,
					this.positionBeforeDraw.y - this.positionAfterDraw.y);

			this.zoomLevelDiff = (byte) (this.zoomLevelAfterDraw - this.zoomLevelBeforeDraw);
			if (this.zoomLevelDiff > 0) {
				// zoom level has increased
				this.matrixScaleFactor = 1 << this.zoomLevelDiff;
				this.matrix
						.postScale(this.matrixScaleFactor, this.matrixScaleFactor,
								this.overlayCanvas.getWidth() >> 1, this.overlayCanvas
										.getHeight() >> 1);
			} else if (this.zoomLevelDiff < 0) {
				// zoom level has decreased
				this.matrixScaleFactor = 1.0f / (1 << -this.zoomLevelDiff);
				this.matrix
						.postScale(this.matrixScaleFactor, this.matrixScaleFactor,
								this.overlayCanvas.getWidth() >> 1, this.overlayCanvas
										.getHeight() >> 1);
			}

			// swap the two overlay bitmaps
			this.overlayBitmapSwap = this.overlayBitmap1;
			this.overlayBitmap1 = this.overlayBitmap2;
			this.overlayBitmap2 = this.overlayBitmapSwap;
		}

		// request the MapView to redraw
		this.internalMapView.postInvalidate();
	}

	/**
	 * Draws the overlay on the canvas.
	 * 
	 * @param canvas
	 *            the canvas to draw the overlay on.
	 * @param drawPosition
	 *            the top-left position of the map relative to the world map.
	 * @param projection
	 *            the projection to be used for the drawing process.
	 * @param drawZoomLevel
	 *            the zoom level of the map.
	 */
	protected abstract void drawOverlayBitmap(Canvas canvas, Point drawPosition,
			Projection projection, byte drawZoomLevel);

	/**
	 * Returns the name of the overlay implementation. It will be used as the name for the
	 * overlay thread.
	 * 
	 * @return the name of the overlay implementation.
	 */
	protected String getThreadName() {
		return THREAD_NAME;
	}

	/**
	 * Draws the overlay on top of the map. This will be called by the MapView.
	 * 
	 * @param canvas
	 *            the canvas the overlay will be drawn onto.
	 * @param mapView
	 *            the calling MapView.
	 * @param shadow
	 *            true if the shadow layer should be drawn, false otherwise.
	 */
	final void draw(Canvas canvas, MapView mapView, boolean shadow) {
		synchronized (this.matrix) {
			canvas.drawBitmap(this.overlayBitmap1, this.matrix, null);
		}
	}

	/**
	 * @param sx
	 *            the horizontal scale.
	 * @param sy
	 *            the vertical scale.
	 * @param px
	 *            the horizontal pivot point.
	 * @param py
	 *            the vertical pivot point.
	 */
	final void matrixPostScale(float sx, float sy, float px, float py) {
		synchronized (this.matrix) {
			this.matrix.postScale(sx, sy, px, py);
		}
	}

	/**
	 * @param dx
	 *            the horizontal translation.
	 * @param dy
	 *            the vertical translation.
	 */
	final void matrixPostTranslate(float dx, float dy) {
		synchronized (this.matrix) {
			this.matrix.postTranslate(dx, dy);
		}
	}

	/**
	 * Initializes the overlay. This method must be called by the MapView once on each new
	 * overlay and every time the size or the projection of the MapView has changed.
	 * 
	 * @param mapView
	 *            the calling MapView.
	 */
	final void setupOverlay(MapView mapView) {
		if (isInterrupted() || !isAlive()) {
			throw new IllegalThreadStateException("overlay thread already destroyed");
		}

		// check if the previous overlay bitmaps must be recycled
		if (this.overlayBitmap1 != null) {
			this.overlayBitmap1.recycle();
		}
		if (this.overlayBitmap2 != null) {
			this.overlayBitmap2.recycle();
		}

		// check if the MapView has valid dimensions
		if (mapView.getWidth() <= 0 || mapView.getHeight() <= 0) {
			return;
		}

		this.internalMapView = mapView;

		// create the two overlay bitmaps with the correct dimensions
		this.overlayBitmap1 = Bitmap.createBitmap(this.internalMapView.getWidth(),
				this.internalMapView.getHeight(), Bitmap.Config.ARGB_8888);
		this.overlayBitmap2 = Bitmap.createBitmap(this.internalMapView.getWidth(),
				this.internalMapView.getHeight(), Bitmap.Config.ARGB_8888);
		this.overlayCanvas = new Canvas();

		this.isSetUp = true;
		requestRedraw();
	}
}