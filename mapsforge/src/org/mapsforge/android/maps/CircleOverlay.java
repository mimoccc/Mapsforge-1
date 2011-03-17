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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;

/**
 * CircleOverlay is an abstract base class to display {@link OverlayCircle OverlayCircles}. The
 * class defines some methods to access the backing data structure of deriving subclasses.
 * <p>
 * The overlay may be used to indicate positions which have a known accuracy, such as GPS fixes.
 * The radius of the circles is specified in meters and will be automatically converted to
 * pixels at each redraw.
 * 
 * @param <Circle>
 *            the type of circles handled by this overlay.
 */
public abstract class CircleOverlay<Circle extends OverlayCircle> extends Overlay {
	private static final String THREAD_NAME = "CircleOverlay";

	private final Paint defaultPaintFill;
	private final Paint defaultPaintOutline;
	private int numberOfCircles;
	private Circle overlayCircle;
	private final Path path;

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
		this.path = new Path();
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
		this.numberOfCircles = size();
		if (this.numberOfCircles < 1) {
			// no circles to draw
			return;
		}

		// draw the overlay circles
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
				}

				// assemble the path
				this.path.reset();
				this.path.addCircle(this.overlayCircle.cachedCenterPosition.x - drawPosition.x,
						this.overlayCircle.cachedCenterPosition.y - drawPosition.y, projection
								.metersToPixels(this.overlayCircle.radius, drawZoomLevel),
						Path.Direction.CCW);

				// draw the path on the canvas
				if (this.overlayCircle.hasPaint) {
					// use the paints from the current circle
					if (this.overlayCircle.paintOutline != null) {
						canvas.drawPath(this.path, this.overlayCircle.paintOutline);
					}
					if (this.overlayCircle.paintFill != null) {
						canvas.drawPath(this.path, this.overlayCircle.paintFill);
					}
				} else {
					// use the default paint objects
					if (this.defaultPaintOutline != null) {
						canvas.drawPath(this.path, this.defaultPaintOutline);
					}
					if (this.defaultPaintFill != null) {
						canvas.drawPath(this.path, this.defaultPaintFill);
					}
				}
			}
		}
	}

	@Override
	protected String getThreadName() {
		return THREAD_NAME;
	}

	/**
	 * This method should be called after circles have been added to the overlay.
	 */
	protected final void populate() {
		super.requestRedraw();
	}
}