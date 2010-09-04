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
package org.mapsforge.android.map;

import android.view.KeyEvent;
import android.view.View;

/**
 * An implementation of the MapController class from the Google Maps library.
 */
public final class MapController implements android.view.View.OnKeyListener {
	private MapView mapView;

	MapController(MapView mapView) {
		this.mapView = mapView;
	}

	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			// forward the event to the MapView
			return this.mapView.onKeyDown(keyCode, event);
		} else if (event.getAction() == KeyEvent.ACTION_UP) {
			// forward the event to the MapView
			return this.mapView.onKeyUp(keyCode, event);
		}
		return false;
	}

	/**
	 * Sets the center of the MapView.
	 * 
	 * @param point
	 *            the new center point.
	 */
	public void setCenter(GeoPoint point) {
		this.mapView.setCenter(point);
	}

	/**
	 * Sets the zoom level of the MapView.
	 * 
	 * @param zoomLevel
	 *            the new zoom level. This will be limited by the maximum possible zoom level.
	 * @return the new zoom level.
	 */
	public int setZoom(byte zoomLevel) {
		return this.mapView.setZoom(zoomLevel);
	}

	/**
	 * Increases the zoom level of the MapView.
	 * 
	 * @return true, if the zoom level was changed, false otherwise.
	 */
	public boolean zoomIn() {
		return this.mapView.zoom((byte) 1);
	}

	/**
	 * Decreases the zoom level of the MapView.
	 * 
	 * @return true, if the zoom level was changed, false otherwise.
	 */
	public boolean zoomOut() {
		return this.mapView.zoom((byte) -1);
	}

	void destroy() {
		this.mapView = null;
	}
}