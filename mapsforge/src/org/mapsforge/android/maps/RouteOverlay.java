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
 * RouteOverlay is an abstract base class to display {@link OverlayRoute OverlayRoutes}. The
 * class defines some methods to access the backing data structure of deriving subclasses.
 * <p>
 * The overlay may be used to show additional ways such as calculated routes. Closed polygons,
 * for example buildings or areas, are also supported. A way node sequence is considered as a
 * closed polygon if the first and the last way node are equal.
 * 
 * @param <Route>
 *            the type of routes handled by this overlay.
 */
public abstract class RouteOverlay<Route extends OverlayRoute> extends Overlay {
	private static final String THREAD_NAME = "RouteOverlay";

	private final Paint defaultPaintFill;
	private final Paint defaultPaintOutline;
	private int numberOfRoutes;
	private Route overlayRoute;
	private final Path path;

	/**
	 * Constructs a new RouteOverlay.
	 * 
	 * @param defaultPaintFill
	 *            the default paint which will be used to fill the routes (may be null).
	 * @param defaultPaintOutline
	 *            the default paint which will be used to draw the route outlines (may be null).
	 */
	public RouteOverlay(Paint defaultPaintFill, Paint defaultPaintOutline) {
		this.defaultPaintFill = defaultPaintFill;
		this.defaultPaintOutline = defaultPaintOutline;
		this.path = new Path();
	}

	/**
	 * Returns the numbers of routes in this overlay.
	 * 
	 * @return the numbers of routes in this overlay.
	 */
	public abstract int size();

	/**
	 * Creates a route in this overlay.
	 * 
	 * @param i
	 *            the index of the route.
	 * @return the route.
	 */
	protected abstract Route createRoute(int i);

	@Override
	protected void drawOverlayBitmap(Canvas canvas, Point drawPosition, Projection projection,
			byte drawZoomLevel) {
		this.numberOfRoutes = size();
		if (this.numberOfRoutes < 1) {
			// no routes to draw
			return;
		}

		// draw the overlay routes
		for (int routeIndex = 0; routeIndex < this.numberOfRoutes; ++routeIndex) {
			// get the current route
			this.overlayRoute = createRoute(routeIndex);

			synchronized (this.overlayRoute) {
				// make sure that the current route is not null and has way nodes
				if (this.overlayRoute == null || this.overlayRoute.wayNodes == null) {
					continue;
				}

				// make sure that the cached way node positions are valid
				if (drawZoomLevel != this.overlayRoute.cachedZoomLevel) {
					for (int i = 0; i < this.overlayRoute.cachedWayPositions.length; ++i) {
						this.overlayRoute.cachedWayPositions[i] = projection.toPoint(
								this.overlayRoute.wayNodes[i],
								this.overlayRoute.cachedWayPositions[i], drawZoomLevel);
					}
					this.overlayRoute.cachedZoomLevel = drawZoomLevel;
				}

				// assemble the path
				this.path.reset();
				this.path.moveTo(this.overlayRoute.cachedWayPositions[0].x - drawPosition.x,
						this.overlayRoute.cachedWayPositions[0].y - drawPosition.y);
				for (int j = 1; j < this.overlayRoute.cachedWayPositions.length; ++j) {
					this.path.lineTo(
							this.overlayRoute.cachedWayPositions[j].x - drawPosition.x,
							this.overlayRoute.cachedWayPositions[j].y - drawPosition.y);
				}

				// draw the path on the canvas
				if (this.overlayRoute.hasPaint) {
					// use the paints from the current route
					if (this.overlayRoute.paintOutline != null) {
						canvas.drawPath(this.path, this.overlayRoute.paintOutline);
					}
					if (this.overlayRoute.paintFill != null) {
						canvas.drawPath(this.path, this.overlayRoute.paintFill);
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
	 * This method should be called after routes have been added to the overlay.
	 */
	protected final void populate() {
		super.requestRedraw();
	}
}