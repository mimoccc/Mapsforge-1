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
 * CircleOverlay is a special Overlay to display a circle on top of the map. The radius of the
 * circle is specified in meters and will be automatically converted to pixels at each redraw.
 * <p>
 * All rendering parameters like color, stroke width, pattern and transparency can be configured
 * via the two {@link android.graphics.Paint Paint} objects in the
 * {@link #CircleOverlay(Paint,Paint)} constructor.
 */
public class CircleOverlay extends Overlay {
	private static final String THREAD_NAME = "CircleOverlay";

	private Point cachedCenterPosition;
	private byte cachedZoomLevel;
	private GeoPoint center;
	private Paint fillPaint;
	private Paint outlinePaint;
	private final Path path;
	private float radius;

	/**
	 * Constructs a new CircleOverlay.
	 * 
	 * @param fillPaint
	 *            the paint object which will be used to fill the overlay.
	 * @param outlinePaint
	 *            the paint object which will be used to draw the outline of the overlay.
	 */
	public CircleOverlay(Paint fillPaint, Paint outlinePaint) {
		this.path = new Path();
		this.cachedCenterPosition = new Point();
		setPaint(fillPaint, outlinePaint);
	}

	/**
	 * Sets the parameters of the circle.
	 * 
	 * @param center
	 *            the geographical coordinates of the center point.
	 * @param radius
	 *            the radius of the circle in meters.
	 */
	public void setCircleData(GeoPoint center, float radius) {
		synchronized (this.path) {
			this.center = center;
			this.radius = radius;
			this.cachedZoomLevel = Byte.MIN_VALUE;
		}
		super.requestRedraw();
	}

	/**
	 * Sets the paint objects which will be used to draw the overlay.
	 * 
	 * @param fillPaint
	 *            the paint object which will be used to fill the overlay.
	 * @param outlinePaint
	 *            the paint object which will be used to draw the outline of the overlay.
	 */
	public void setPaint(Paint fillPaint, Paint outlinePaint) {
		synchronized (this.path) {
			this.fillPaint = fillPaint;
			this.outlinePaint = outlinePaint;
		}
		super.requestRedraw();
	}

	@Override
	protected void drawOverlayBitmap(Canvas canvas, Point drawPosition, Projection projection,
			byte drawZoomLevel) {
		synchronized (this.path) {
			if (this.center == null || this.radius < 0) {
				// no valid parameters to draw the circle
				return;
			} else if (this.fillPaint == null && this.outlinePaint == null) {
				// no paint to draw
				return;
			}

			// make sure that the cached center position is valid
			if (drawZoomLevel != this.cachedZoomLevel) {
				this.cachedCenterPosition = projection.toPoint(this.center,
						this.cachedCenterPosition, drawZoomLevel);
				this.cachedZoomLevel = drawZoomLevel;
			}

			// assemble the path
			this.path.reset();
			this.path.addCircle(this.cachedCenterPosition.x - drawPosition.x,
					this.cachedCenterPosition.y - drawPosition.y, projection.metersToPixels(
							this.radius, drawZoomLevel), Path.Direction.CCW);

			// draw the path on the canvas
			if (this.outlinePaint != null) {
				canvas.drawPath(this.path, this.outlinePaint);
			}
			if (this.fillPaint != null) {
				canvas.drawPath(this.path, this.fillPaint);
			}
		}
	}

	@Override
	protected String getThreadName() {
		return THREAD_NAME;
	}
}