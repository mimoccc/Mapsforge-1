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
package org.mapsforge.pc.maps;

import java.util.ArrayList;

/**
 * MapActivity is the abstract base class which must be extended in order to use
 * a {@link MapView}. There are no abstract methods in this implementation that
 * subclasses need to override. In addition, no API key or registration is
 * required.
 * <p>
 * A subclass may create a MapView either via one of the MapView constructors or
 * by inflating an XML layout file. It is possible to use more than one MapView
 * at the same time as each of them works independently from the others.
 * <p>
 * When the MapActivity is shut down, the current center position, zoom level
 * and map file of the MapView are saved in a preferences file and restored
 * automatically during the setup process of a MapView.
 */
public class MapActivity extends Thread {
	/**
	 * Name of the file where the map position and other settings are stored.
	 */

	/**
	 * Counter to store the last ID given to a MapView.
	 */
	private int lastMapViewId;

	public MapActivity() {

	}

	/**
	 * Internal list which contains references to all running MapView objects.
	 */
	private ArrayList<MapView> mapViews = new ArrayList<MapView>(2);

	private void destroyMapViews() {
		if (this.mapViews != null) {
			MapView currentMapView;
			while (!this.mapViews.isEmpty()) {
				currentMapView = this.mapViews.get(0);
				currentMapView.destroy();
			}
			currentMapView = null;
			this.mapViews.clear();
			this.mapViews = null;
		}
	}

	protected void onDestroy() {
		destroyMapViews();
	}

	/**
	 * Returns a unique MapView ID on each call.
	 * 
	 * @return the new MapView ID.
	 */
	final int getMapViewId() {
		return ++this.lastMapViewId;
	}

	/**
	 * This method is called once by each MapView during its setup process.
	 * 
	 * @param mapView
	 *            the calling MapView.
	 */
	final void registerMapView(MapView mapView) {
		if (this.mapViews != null) {
			this.mapViews.add(mapView);
		}
	}

	/**
	 * This method is called once by each MapView when it gets destroyed.
	 * 
	 * @param mapView
	 *            the calling MapView.
	 */
	final void unregisterMapView(MapView mapView) {
		if (this.mapViews != null) {
			this.mapViews.remove(mapView);
		}
	}

	public final void runOnUiThread(Runnable action) {
		action.run();
	}
}