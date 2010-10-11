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

import android.graphics.drawable.Drawable;

/**
 * This is the basic element of {@link Overlay}.
 * 
 * @author Sebastian Schlaak
 * @author Karsten Groll
 */
public class OverlayItem {

	/**
	 * State bitset bit. This feature is not yet implemented!
	 */
	static int ITEM_STATE_FOCUSED_MASK;
	/**
	 * State bitset bit. This feature is not yet implemented!
	 */
	static int ITEM_STATE_PRESSED_MASK;
	/**
	 * State bitset bit. This feature is not yet implemented!
	 */
	static int ITEM_STATE_SELECTED_MASK;
	/**
	 * The marker used to indicate the item.
	 */
	protected Drawable marker;
	/**
	 * The position of the item as {@link GeoPoint}.
	 */
	protected GeoPoint point;
	/**
	 *The position relative to display
	 */
	protected Point posOnDisplay;

	/**
	 * The zoomlevel of posOnDisplay.
	 */
	protected int zoomLevel;

	/**
	 * A short description.
	 */
	protected String snippet;
	/**
	 * The title of the item.
	 */
	protected String title;

	/**
	 * Construct an overlay item.
	 * 
	 * @param point
	 *            position of the item.
	 * @param title
	 *            title of the item.
	 * @param snippet
	 *            snippet-text of the item.
	 */
	public OverlayItem(GeoPoint point, String title, String snippet) {
		this.point = point;
		this.title = title;
		this.snippet = snippet;
	}

	/**
	 * Returns the overlay marker used to indicate the item.
	 * 
	 * @return the overlay marker.
	 */
	public Drawable getMarker() {
		return this.marker;
	}

	/**
	 * Returns the position of the item as {@link GeoPoint}.
	 * 
	 * @return the GeoPoint.
	 */
	public GeoPoint getPoint() {
		return this.point;
	}

	/**
	 * Returns the snippet-text of the item.
	 * 
	 * @return the Snippet-text.
	 */
	public String getSnippet() {
		return this.snippet;
	}

	/**
	 * Returns the title of the item.
	 * 
	 * @return the title.
	 */
	public String getTitle() {
		return this.title;
	}

	/**
	 * Returns the position of the item as map-routable {@link String}.
	 * 
	 * @return not yet implemented!
	 */
	public String routableAddress() {
		Logger.e(new Exception("Not Implemented"));
		return null;
	}

	/**
	 * Sets the marker to draw this item on the {@link MapView}.
	 * 
	 * @param drawable
	 *            the marker to draw.
	 * 
	 * @param stateBitset
	 *            not yet implemented!
	 */
	public void setMarker(Drawable drawable, int stateBitset) {
		this.marker = drawable;
		Logger.e(new Exception("Not Implemented"));
	}
}
