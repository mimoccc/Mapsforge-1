/*
 * Copyright 2010, 2011 mapsforge.org
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
package org.mapsforge.android.maps;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;

/**
 * WayOverlay is an abstract base class to display {@link OverlayWay OverlayWays}. The class defines
 * some methods to access the backing data structure of deriving subclasses.
 * <p>
 * The overlay may be used to show additional ways such as calculated routes. Closed polygons, for
 * example buildings or areas, are also supported. A way node sequence is considered as a closed polygon
 * if the first and the last way node are equal.
 * 
 * @param <Way>
 *            the type of ways handled by this overlay.
 */
public abstract class WayOverlay<Way extends OverlayWay> extends Overlay {
	private static final String THREAD_NAME = "WayOverlay";

	private final Paint defaultPaintFill;
	private final Paint defaultPaintOutline;
	private int numberOfWays;
	private Way overlayWay;
	private final Path path;

	/**
	 * Constructs a new WayOverlay.
	 * 
	 * @param defaultPaintFill
	 *            the default paint which will be used to fill the ways (may be null).
	 * @param defaultPaintOutline
	 *            the default paint which will be used to draw the way outlines (may be null).
	 */
	public WayOverlay(Paint defaultPaintFill, Paint defaultPaintOutline) {
		this.defaultPaintFill = defaultPaintFill;
		this.defaultPaintOutline = defaultPaintOutline;
		this.path = new Path();
		this.path.setFillType(Path.FillType.EVEN_ODD);
	}

	/**
	 * Returns the numbers of ways in this overlay.
	 * 
	 * @return the numbers of ways in this overlay.
	 */
	public abstract int size();

	/**
	 * Creates a way in this overlay.
	 * 
	 * @param i
	 *            the index of the way.
	 * @return the way.
	 */
	protected abstract Way createWay(int i);

	@Override
	protected void drawOverlayBitmap(Canvas canvas, Point drawPosition, Projection projection,
			byte drawZoomLevel) {
		this.numberOfWays = size();
		for (int wayIndex = 0; wayIndex < this.numberOfWays; ++wayIndex) {
			if (isInterrupted() || sizeHasChanged()) {
				// stop working
				return;
			}

			// get the current way
			this.overlayWay = createWay(wayIndex);
			if (this.overlayWay == null) {
				continue;
			}

			synchronized (this.overlayWay) {
				// make sure that the current way has way nodes
				if (this.overlayWay.wayNodes == null || this.overlayWay.wayNodes.length == 0) {
					continue;
				}

				// make sure that the cached way node positions are valid
				if (drawZoomLevel != this.overlayWay.cachedZoomLevel) {
					for (int i = 0; i < this.overlayWay.cachedWayPositions.length; ++i) {
						for (int j = 0; j < this.overlayWay.cachedWayPositions[i].length; ++j) {
							this.overlayWay.cachedWayPositions[i][j] = projection.toPoint(
									this.overlayWay.wayNodes[i][j],
									this.overlayWay.cachedWayPositions[i][j], drawZoomLevel);
						}
					}
					this.overlayWay.cachedZoomLevel = drawZoomLevel;
				}

				// assemble the path
				this.path.reset();
				for (int i = 0; i < this.overlayWay.cachedWayPositions.length; ++i) {
					this.path.moveTo(this.overlayWay.cachedWayPositions[i][0].x - drawPosition.x,
							this.overlayWay.cachedWayPositions[i][0].y - drawPosition.y);
					for (int j = 1; j < this.overlayWay.cachedWayPositions[i].length; ++j) {
						this.path.lineTo(this.overlayWay.cachedWayPositions[i][j].x - drawPosition.x,
								this.overlayWay.cachedWayPositions[i][j].y - drawPosition.y);
					}
				}

				// draw the path on the canvas
				if (this.overlayWay.hasPaint) {
					// use the paints from the current way
					if (this.overlayWay.paintOutline != null) {
						canvas.drawPath(this.path, this.overlayWay.paintOutline);
					}
					if (this.overlayWay.paintFill != null) {
						canvas.drawPath(this.path, this.overlayWay.paintFill);
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
	 * This method should be called after ways have been added to the overlay.
	 */
	protected final void populate() {
		super.requestRedraw();
	}
}