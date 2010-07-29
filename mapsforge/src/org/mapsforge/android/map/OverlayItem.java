package org.mapsforge.android.map;

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
