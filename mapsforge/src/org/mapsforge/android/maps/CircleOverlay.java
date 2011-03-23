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

import java.util.ArrayList;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;

/**
 * CircleOverlay is an abstract base class to display {@link OverlayCircle OverlayCircles}. The
 * class defines some methods to access the backing data structure of deriving subclasses.
 * Besides organizing the redrawing process it handles tap events from the user to check if an
 * OverlayCircle has been touched and {@link #onTap(int)} must be executed.
 * <p>
 * The overlay may be used to indicate positions which have a known accuracy, such as GPS fixes.
 * The radius of the circles is specified in meters and will be automatically converted to
 * pixels at each redraw.
 * 
 * @param <Circle>
 *            the type of circles handled by this overlay.
 */
public abstract class CircleOverlay<Circle extends OverlayCircle> extends Overlay {
	private static final int ARRAY_LIST_INITIAL_CAPACITY = 8;
	private static final String THREAD_NAME = "CircleOverlay";

	private final Point circlePosition;
	private float circleRadius;
	private final Paint defaultPaintFill;
	private final Paint defaultPaintOutline;
	private boolean hasDefaultPaint;
	private int numberOfCircles;
	private Circle overlayCircle;
	private final Path path;
	private ArrayList<Integer> visibleCircles;
	private ArrayList<Integer> visibleCirclesRedraw;
	private ArrayList<Integer> visibleCirclesTemp;

	/**
	 * Constructs a new CircleOverlay.
	 * 
	 * @param defaultPaintFill
	 *            the default paint which will be used to fill the circles (may be null).
	 * @param defaultPaintOutline
	 *            the default paint which will be used to draw the circle outlines (may be
	 *            null).
	 */
	public CircleOverlay(Paint defaultPaintFill, Paint defaultPaintOutline) {
		this.defaultPaintFill = defaultPaintFill;
		this.defaultPaintOutline = defaultPaintOutline;
		this.hasDefaultPaint = defaultPaintFill != null || defaultPaintOutline != null;
		this.circlePosition = new Point();
		this.visibleCircles = new ArrayList<Integer>(ARRAY_LIST_INITIAL_CAPACITY);
		this.visibleCirclesRedraw = new ArrayList<Integer>(ARRAY_LIST_INITIAL_CAPACITY);
		this.path = new Path();
	}

	/**
	 * Handles a tap event.
	 */
	@Override
	public boolean onTap(GeoPoint geoPoint, MapView mapView) {
		Projection projection = mapView.getProjection();
		Point tapPosition = projection.toPixels(geoPoint, null);

		// check if the translation to pixel coordinates has failed
		if (tapPosition == null) {
			return false;
		}

		Circle tapOverlayCircle;
		Point tapCirclePoint = new Point();
		float diffX;
		float diffY;
		double distance;

		synchronized (this.visibleCircles) {
			// iterate over all visible circles
			for (Integer circleIndex : this.visibleCircles) {
				// get the current circle
				tapOverlayCircle = createCircle(circleIndex.intValue());
				if (tapOverlayCircle == null) {
					continue;
				}

				synchronized (tapOverlayCircle) {
					// make sure that the current circle has a center position and a radius
					if (tapOverlayCircle.center == null || tapOverlayCircle.radius < 0) {
						continue;
					}

					tapCirclePoint = projection.toPixels(tapOverlayCircle.center,
							tapCirclePoint);
					// check if the translation to pixel coordinates has failed
					if (tapCirclePoint == null) {
						continue;
					}

					// calculate the Euclidian distance between the circle and the tap position
					diffX = tapCirclePoint.x - tapPosition.x;
					diffY = tapCirclePoint.y - tapPosition.y;
					distance = Math.sqrt(diffX * diffX + diffY * diffY);

					// check if the tap position is within the circle radius
					if (distance <= tapOverlayCircle.cachedRadius) {
						return onTap(circleIndex.intValue());
					}
				}
			}
		}

		// no hit
		return false;
	}

	/**
	 * Returns the numbers of circles in this overlay.
	 * 
	 * @return the numbers of circles in this overlay.
	 */
	public abstract int size();

	/**
	 * Creates a circle in this overlay.
	 * 
	 * @param i
	 *            the index of the circle.
	 * @return the circle.
	 */
	protected abstract Circle createCircle(int i);

	@Override
	protected void drawOverlayBitmap(Canvas canvas, Point drawPosition, Projection projection,
			byte drawZoomLevel) {
		// erase the list of visible circles
		this.visibleCirclesRedraw.clear();

		this.numberOfCircles = size();
		for (int circleIndex = 0; circleIndex < this.numberOfCircles; ++circleIndex) {
			// get the current circle
			this.overlayCircle = createCircle(circleIndex);
			if (this.overlayCircle == null) {
				continue;
			}

			synchronized (this.overlayCircle) {
				// make sure that the current circle has a center position and a radius
				if (this.overlayCircle.center == null || this.overlayCircle.radius < 0) {
					continue;
				}

				// make sure that the cached center position is valid
				if (drawZoomLevel != this.overlayCircle.cachedZoomLevel) {
					this.overlayCircle.cachedCenterPosition = projection.toPoint(
							this.overlayCircle.center, this.overlayCircle.cachedCenterPosition,
							drawZoomLevel);
					this.overlayCircle.cachedZoomLevel = drawZoomLevel;
					this.overlayCircle.cachedRadius = projection.metersToPixels(
							this.overlayCircle.radius, drawZoomLevel);
				}

				// calculate the relative circle position on the canvas
				this.circlePosition.x = this.overlayCircle.cachedCenterPosition.x
						- drawPosition.x;
				this.circlePosition.y = this.overlayCircle.cachedCenterPosition.y
						- drawPosition.y;
				this.circleRadius = this.overlayCircle.cachedRadius;

				// check if the bounding box of the circle intersects with the canvas
				if ((this.circlePosition.x + this.circleRadius) >= 0
						&& (this.circlePosition.x - this.circleRadius) <= canvas.getWidth()
						&& (this.circlePosition.y + this.circleRadius) >= 0
						&& (this.circlePosition.y - this.circleRadius) <= canvas.getHeight()) {
					// assemble the path
					this.path.reset();
					this.path.addCircle(this.circlePosition.x, this.circlePosition.y,
							this.circleRadius, Path.Direction.CCW);

					// draw the path on the canvas
					if (this.overlayCircle.hasPaint) {
						// use the paints from the current circle
						if (this.overlayCircle.paintOutline != null) {
							canvas.drawPath(this.path, this.overlayCircle.paintOutline);
						}
						if (this.overlayCircle.paintFill != null) {
							canvas.drawPath(this.path, this.overlayCircle.paintFill);
						}

						// add the current circle index to the list of visible circles
						this.visibleCirclesRedraw.add(Integer.valueOf(circleIndex));
					} else if (this.hasDefaultPaint) {
						// use the default paint objects
						if (this.defaultPaintOutline != null) {
							canvas.drawPath(this.path, this.defaultPaintOutline);
						}
						if (this.defaultPaintFill != null) {
							canvas.drawPath(this.path, this.defaultPaintFill);
						}

						// add the current circle index to the list of visible circles
						this.visibleCirclesRedraw.add(Integer.valueOf(circleIndex));
					}
				}
			}
		}

		// swap the two visible circle lists
		synchronized (this.visibleCircles) {
			this.visibleCirclesTemp = this.visibleCircles;
			this.visibleCircles = this.visibleCirclesRedraw;
			this.visibleCirclesRedraw = this.visibleCirclesTemp;
		}
	}

	@Override
	protected String getThreadName() {
		return THREAD_NAME;
	}

	/**
	 * Handles a tap event.
	 * <p>
	 * The default implementation of this method does nothing and returns false.
	 * 
	 * @param index
	 *            the position of the circle.
	 * @return true if the event was handled, false otherwise.
	 */
	protected boolean onTap(int index) {
		return false;
	}

	/**
	 * This method should be called after circles have been added to the overlay.
	 */
	protected final void populate() {
		super.requestRedraw();
	}
}