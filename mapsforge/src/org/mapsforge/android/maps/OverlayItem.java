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

import android.graphics.Point;
import android.graphics.drawable.Drawable;

/**
 * OverlayItem holds all parameters of a single element on an {@link ItemizedOverlay}, such as
 * position, marker, title and textual description. Once created, only the marker of an
 * OverlayItem can be modified via the {@link #setMarker(Drawable)} method.
 */
public class OverlayItem {
	/**
	 * Marker used to indicate the item.
	 */
	protected Drawable marker;

	/**
	 * Geographical position of the item.
	 */
	protected final GeoPoint point;

	/**
	 * Short description of the item.
	 */
	protected final String snippet;

	/**
	 * Title of the item.
	 */
	protected final String title;

	/**
	 * Cached position of the item on the map.
	 */
	Point cachedMapPosition;

	/**
	 * Zoom level of the cached map position.
	 */
	byte cachedZoomLevel;

	/**
	 * Constructs a new OverlayItem.
	 * 
	 * @param point
	 *            the geographical position of the item.
	 * @param title
	 *            the title of the item (may be null).
	 * @param snippet
	 *            the short description of the item (may be null).
	 */
	public OverlayItem(GeoPoint point, String title, String snippet) {
		this.point = point;
		this.title = title;
		this.snippet = snippet;
		this.cachedZoomLevel = Byte.MIN_VALUE;
	}

	/**
	 * Returns the marker used to indicate the item.
	 * 
	 * @return the marker used to indicate the item (may be null).
	 */
	public synchronized Drawable getMarker() {
		return this.marker;
	}

	/**
	 * Returns the position of the item.
	 * 
	 * @return the position of the item.
	 */
	public GeoPoint getPoint() {
		return this.point;
	}

	/**
	 * Returns the short description of the item.
	 * 
	 * @return the short description of the item (may be null).
	 */
	public String getSnippet() {
		return this.snippet;
	}

	/**
	 * Returns the title of the item.
	 * 
	 * @return the title of the item (may be null).
	 */
	public String getTitle() {
		return this.title;
	}

	/**
	 * Sets the marker that is drawn for this item. If the item marker is null, the default
	 * marker of the overlay will be drawn instead.
	 * <p>
	 * The bounds of the marker must already have been set properly, for example by calling
	 * {@link ItemizedOverlay#boundCenterBottom(Drawable)}.
	 * 
	 * @param marker
	 *            the marker that is drawn for this item (may be null).
	 */
	public synchronized void setMarker(Drawable marker) {
		this.marker = marker;
	}
}