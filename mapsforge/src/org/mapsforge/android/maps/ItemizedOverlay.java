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
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * ItemizedOverlay is an abstract base class to display {@link OverlayItem OverlayItems}. The
 * class defines some methods to access the backing data structure of deriving subclasses.
 * Besides organizing the redrawing process it handles tap events from the user to check if an
 * OverlayItem has been touched and {@link #onTap(int)} must be executed.
 * 
 * @param <Item>
 *            the type of items handled by this overlay.
 */
public abstract class ItemizedOverlay<Item extends OverlayItem> extends Overlay {
	private static final int ARRAY_LIST_INITIAL_CAPACITY = 8;
	private static final String THREAD_NAME = "ItemizedOverlay";

	/**
	 * Sets the bounds of the given drawable so that (0,0) is the center of the bottom row.
	 * 
	 * @param balloon
	 *            the drawable whose bounds should be set.
	 * @return the given drawable with set bounds.
	 */
	public static Drawable boundCenter(Drawable balloon) {
		balloon.setBounds(balloon.getIntrinsicWidth() / -2, balloon.getIntrinsicHeight() / -2,
				balloon.getIntrinsicWidth() / 2, balloon.getIntrinsicHeight() / 2);
		return balloon;
	}

	/**
	 * Sets the bounds of the given drawable so that (0,0) is the center of the bounding box.
	 * 
	 * @param balloon
	 *            the drawable whose bounds should be set.
	 * @return the given drawable with set bounds.
	 */
	public static Drawable boundCenterBottom(Drawable balloon) {
		balloon.setBounds(balloon.getIntrinsicWidth() / -2, -balloon.getIntrinsicHeight(),
				balloon.getIntrinsicWidth() / 2, 0);
		return balloon;
	}

	private int bottom;
	private final Drawable defaultMarker;
	private Drawable itemMarker;
	private final Point itemPosition;
	private int left;
	private Rect markerBounds;
	private int numberOfItems;
	private Item overlayItem;
	private int right;
	private int top;
	private ArrayList<Integer> visibleItems;
	private ArrayList<Integer> visibleItemsRedraw;
	private ArrayList<Integer> visibleItemsTemp;

	/**
	 * Constructs a new ItemizedOverlay.
	 * 
	 * @param defaultMarker
	 *            the default marker (may be null).
	 */
	public ItemizedOverlay(Drawable defaultMarker) {
		this.defaultMarker = defaultMarker;
		this.itemPosition = new Point();
		this.visibleItems = new ArrayList<Integer>(ARRAY_LIST_INITIAL_CAPACITY);
		this.visibleItemsRedraw = new ArrayList<Integer>(ARRAY_LIST_INITIAL_CAPACITY);
	}

	@Override
	public boolean onTap(GeoPoint geoPoint, MapView mapView) {
		Projection projection = mapView.getProjection();
		Point tapPosition = projection.toPixels(geoPoint, null);

		// check if the translation to pixel coordinates has failed
		if (tapPosition == null) {
			return false;
		}

		Item tapOverlayItem;
		Point tapItemPoint = new Point();
		Rect tapMarkerBounds;
		int tapLeft;
		int tapRight;
		int tapTop;
		int tapBottom;

		synchronized (this.visibleItems) {
			// iterate over all visible items
			for (Integer itemIndex : this.visibleItems) {
				// get the current item
				tapOverlayItem = createItem(itemIndex.intValue());
				if (tapOverlayItem == null) {
					continue;
				}

				synchronized (tapOverlayItem) {
					// make sure that the current item has a position
					if (tapOverlayItem.getPoint() == null) {
						continue;
					}

					tapItemPoint = projection.toPixels(tapOverlayItem.getPoint(), tapItemPoint);
					// check if the translation to pixel coordinates has failed
					if (tapItemPoint == null) {
						continue;
					}

					// select the correct marker for the item and get the position
					if (tapOverlayItem.getMarker() == null) {
						if (this.defaultMarker == null) {
							// no marker to draw the item
							continue;
						}
						tapMarkerBounds = this.defaultMarker.getBounds();
					} else {
						tapMarkerBounds = tapOverlayItem.getMarker().getBounds();
					}

					// calculate the bounding box of the marker
					tapLeft = tapItemPoint.x + tapMarkerBounds.left;
					tapRight = tapItemPoint.x + tapMarkerBounds.right;
					tapTop = tapItemPoint.y + tapMarkerBounds.top;
					tapBottom = tapItemPoint.y + tapMarkerBounds.bottom;

					// check if the tap position is within the bounds of the marker
					if (tapRight >= tapPosition.x && tapLeft <= tapPosition.x
							&& tapBottom >= tapPosition.y && tapTop <= tapPosition.y) {
						return onTap(itemIndex.intValue());
					}
				}
			}
		}

		// no hit
		return false;
	}

	/**
	 * Returns the numbers of items in this overlay.
	 * 
	 * @return the numbers of items in this overlay.
	 */
	public abstract int size();

	/**
	 * Creates an item in this overlay.
	 * 
	 * @param i
	 *            the index of the item.
	 * @return the item.
	 */
	protected abstract Item createItem(int i);

	@Override
	protected void drawOverlayBitmap(Canvas canvas, Point drawPosition, Projection projection,
			byte drawZoomLevel) {
		// erase the list of visible items
		this.visibleItemsRedraw.clear();

		this.numberOfItems = size();
		for (int itemIndex = 0; itemIndex < this.numberOfItems; ++itemIndex) {
			// get the current item
			this.overlayItem = createItem(itemIndex);
			if (this.overlayItem == null) {
				continue;
			}

			synchronized (this.overlayItem) {
				// make sure that the current item has a position
				if (this.overlayItem.getPoint() == null) {
					continue;
				}

				// make sure that the cached item position is valid
				if (drawZoomLevel != this.overlayItem.cachedZoomLevel) {
					this.overlayItem.cachedMapPosition = projection.toPoint(this.overlayItem
							.getPoint(), this.overlayItem.cachedMapPosition, drawZoomLevel);
					this.overlayItem.cachedZoomLevel = drawZoomLevel;
				}

				// calculate the relative item position on the canvas
				this.itemPosition.x = this.overlayItem.cachedMapPosition.x - drawPosition.x;
				this.itemPosition.y = this.overlayItem.cachedMapPosition.y - drawPosition.y;

				// get the correct marker for the item
				if (this.overlayItem.getMarker() == null) {
					if (this.defaultMarker == null) {
						// no marker to draw the item
						continue;
					}
					this.itemMarker = this.defaultMarker;
				} else {
					this.itemMarker = this.overlayItem.getMarker();
				}

				// get the position of the marker
				this.markerBounds = this.itemMarker.copyBounds();

				// calculate the bounding box of the marker
				this.left = this.itemPosition.x + this.markerBounds.left;
				this.right = this.itemPosition.x + this.markerBounds.right;
				this.top = this.itemPosition.y + this.markerBounds.top;
				this.bottom = this.itemPosition.y + this.markerBounds.bottom;

				// check if the bounding box of the marker intersects with the canvas
				if (this.right >= 0 && this.left <= canvas.getWidth() && this.bottom >= 0
						&& this.top <= canvas.getHeight()) {
					// set the position of the marker
					this.itemMarker.setBounds(this.left, this.top, this.right, this.bottom);

					// draw the item marker on the canvas
					this.itemMarker.draw(canvas);

					// restore the position of the marker
					this.itemMarker.setBounds(this.markerBounds);

					// add the current item index to the list of visible items
					this.visibleItemsRedraw.add(Integer.valueOf(itemIndex));
				}
			}
		}

		// swap the two visible item lists
		synchronized (this.visibleItems) {
			this.visibleItemsTemp = this.visibleItems;
			this.visibleItems = this.visibleItemsRedraw;
			this.visibleItemsRedraw = this.visibleItemsTemp;
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
	 *            the position of the item.
	 * @return true if the event was handled, false otherwise.
	 */
	protected boolean onTap(int index) {
		return false;
	}

	/**
	 * This method should be called after items have been added to the overlay.
	 */
	protected final void populate() {
		super.requestRedraw();
	}
}
