package org.mapsforge.android.map;

import android.graphics.drawable.Drawable;
import android.util.Log;

/**
 * @author Sebastian Schlaak
 * @author Karsten Groll
 */
public class OverlayItem {
	/** This is the basic element of {@link Overlay} **/

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
	 * Short text to describe the item.
	 */

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
	 * Construct an overlay Item.
	 * 
	 * @param point
	 *            Position of the item.
	 * @param title
	 *            Title of the item.
	 * @param snippet
	 *            Snippet-text of the item.
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
	 * @return The GeoPoint.
	 */
	public GeoPoint getPoint() {
		return this.point;
	}

	/**
	 * Returns the snippet-text of the item.
	 * 
	 * @return The Snippet-text.
	 */
	public String getSnippet() {
		return this.snippet;
	}

	/**
	 * Returns the title of the item.
	 * 
	 * @return The title.
	 */
	public String getTitle() {
		return this.title;
	}

	/**
	 * Returns the position of the item as map-routable {@link String}.
	 * 
	 * @return not yet implemented.
	 */
	public String routableAddress() {
		Log.e("todo", "OverlayItem.routableAddress() is not yet implemented");
		return null;
	}

	/**
	 * Sets the marker to draw this item on the {@link MapView}.
	 * 
	 * @param drawable
	 *            The marker.
	 * 
	 * @param stateBitset
	 *            The state.
	 */

	public void setMarker(Drawable drawable, int stateBitset) {
		this.marker = drawable;
		Log.e("todo", "OverlayItem.routableAddress() is not yet implemented");
	}
}
