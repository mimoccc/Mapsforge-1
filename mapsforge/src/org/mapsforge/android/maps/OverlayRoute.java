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

import android.graphics.Paint;
import android.graphics.Point;

/**
 * OverlayRoute holds all parameters of a single route on a {@link RouteOverlay}. All rendering
 * parameters like color, stroke width, pattern and transparency can be configured via two
 * {@link Paint} objects. Each route is drawn twice – once with each paint object – to allow for
 * different outlines and fillings.
 */
public class OverlayRoute {
	/**
	 * Checks the given way nodes for null elements.
	 * 
	 * @param wayNodes
	 *            the way nodes to check for null elements.
	 * @return true if the way nodes contain at least one null element, false otherwise.
	 */
	private static boolean containsNullElements(GeoPoint[] wayNodes) {
		for (int i = wayNodes.length - 1; i >= 0; --i) {
			if (wayNodes[i] == null) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Paint which will be used to fill the route.
	 */
	protected Paint paintFill;

	/**
	 * Paint which will be used to draw the route outline.
	 */
	protected Paint paintOutline;

	/**
	 * Geographical coordinates of the way nodes.
	 */
	protected GeoPoint[] wayNodes;

	/**
	 * Cached way positions of the route on the map.
	 */
	Point[] cachedWayPositions;

	/**
	 * Zoom level of the cached way positions.
	 */
	byte cachedZoomLevel;

	/**
	 * Flag to indicate if at least one paint is set for this route.
	 */
	boolean hasPaint;

	/**
	 * Constructs a new OverlayRoute.
	 */
	public OverlayRoute() {
		this.cachedWayPositions = new Point[0];
	}

	/**
	 * Constructs a new OverlayRoute.
	 * 
	 * @param wayNodes
	 *            the geographical coordinates of the way nodes, must not contain null elements.
	 * @throws IllegalArgumentException
	 *             if the way nodes contain at least one null element.
	 */
	public OverlayRoute(GeoPoint[] wayNodes) {
		this.cachedWayPositions = new Point[0];
		setRouteData(wayNodes);
	}

	/**
	 * Constructs a new OverlayRoute.
	 * 
	 * @param wayNodes
	 *            the geographical coordinates of the way nodes, must not contain null elements.
	 * @param paintFill
	 *            the paint which will be used to fill the route (may be null).
	 * @param paintOutline
	 *            the paint which will be used to draw the route outline (may be null).
	 * @throws IllegalArgumentException
	 *             if the way nodes contain at least one null element.
	 */
	public OverlayRoute(GeoPoint[] wayNodes, Paint paintFill, Paint paintOutline) {
		this.cachedWayPositions = new Point[0];
		setRouteData(wayNodes);
		setPaint(paintFill, paintOutline);
	}

	/**
	 * Constructs a new OverlayRoute.
	 * 
	 * @param paintFill
	 *            the paint which will be used to fill the route (may be null).
	 * @param paintOutline
	 *            the paint which will be used to draw the route outline (may be null).
	 * @throws IllegalArgumentException
	 *             if the way nodes contain at least one null element.
	 */
	public OverlayRoute(Paint paintFill, Paint paintOutline) {
		this.cachedWayPositions = new Point[0];
		setPaint(paintFill, paintOutline);
	}

	/**
	 * Returns the way nodes of the route.
	 * 
	 * @return the way nodes of the route.
	 */
	public synchronized GeoPoint[] getRouteData() {
		return this.wayNodes;
	}

	/**
	 * Sets the paints which will be used to draw the route.
	 * 
	 * @param paintFill
	 *            the paint which will be used to fill the route (may be null).
	 * @param paintOutline
	 *            the paint which will be used to draw the route outline (may be null).
	 */
	public synchronized void setPaint(Paint paintFill, Paint paintOutline) {
		this.paintFill = paintFill;
		this.paintOutline = paintOutline;
		this.hasPaint = paintFill != null || paintOutline != null;
	}

	/**
	 * Sets the way nodes of the route.
	 * 
	 * @param wayNodes
	 *            the geographical coordinates of the way nodes, must not contain null elements.
	 * @throws IllegalArgumentException
	 *             if the way nodes contain at least one null element.
	 */
	public synchronized void setRouteData(GeoPoint[] wayNodes) {
		// check for illegal null elements
		if (wayNodes != null && containsNullElements(wayNodes)) {
			throw new IllegalArgumentException("way nodes must not contain null elements");
		}

		this.wayNodes = wayNodes;
		if (this.wayNodes != null && this.wayNodes.length != this.cachedWayPositions.length) {
			this.cachedWayPositions = new Point[this.wayNodes.length];
		} else {
			this.cachedWayPositions = new Point[0];
		}
		this.cachedZoomLevel = Byte.MIN_VALUE;
	}
}