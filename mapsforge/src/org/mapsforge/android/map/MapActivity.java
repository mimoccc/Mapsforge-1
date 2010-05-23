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

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/**
 * Abstract class which must be extended in order to use a MapView.
 */
public abstract class MapActivity extends Activity {
	private static final String PREFERENCES = "MapActivity";
	private MapGenerator mapGenerator;
	private MapMover mapMover;
	private MapView mapView;

	@Override
	protected void onDestroy() {
		super.onDestroy();

		// stop the MapMover thread
		if (this.mapMover != null) {
			this.mapMover.interrupt();
			try {
				this.mapMover.join();
			} catch (InterruptedException e) {
				// restore the interrupted status
				Thread.currentThread().interrupt();
			}
			this.mapMover = null;
		}

		// stop the MapGenerator thread
		if (this.mapGenerator != null) {
			this.mapGenerator.interrupt();
			try {
				this.mapGenerator.join();
			} catch (InterruptedException e) {
				// restore the interrupted status
				Thread.currentThread().interrupt();
			}
			this.mapGenerator = null;
		}

		this.mapView = null;
	}

	@Override
	protected void onPause() {
		super.onPause();

		// pause the MapMover thread
		if (this.mapMover != null) {
			this.mapMover.pause();
		}

		// pause the MapGenerator thread
		if (this.mapGenerator != null) {
			this.mapGenerator.pause();
		}

		if (this.mapView != null) {
			Editor editor = getSharedPreferences(PREFERENCES, MODE_PRIVATE).edit();
			editor.clear();
			if (this.mapView.hasValidMapFile() && this.mapView.hasValidCenter()) {
				// save the current map file, map position and zoom level
				editor.putString("mapFile", this.mapView.getMapFile());
				GeoPoint mapCenter = this.mapView.getMapCenter();
				editor.putInt("latitude", mapCenter.getLatitudeE6());
				editor.putInt("longitude", mapCenter.getLongitudeE6());
				editor.putInt("zoomLevel", this.mapView.getZoomLevel());
			}
			editor.commit();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		// unpause the MapMover thread
		if (this.mapMover != null) {
			this.mapMover.unpause();
		}

		// unpause the MapGenerator thread
		if (this.mapGenerator != null) {
			this.mapGenerator.unpause();
		}
	}

	final MapGenerator getMapGenerator() {
		if (this.mapGenerator == null) {
			// create and start the MapGenerator thread
			this.mapGenerator = new MapGenerator();
			this.mapGenerator.start();
		}
		return this.mapGenerator;
	}

	final MapMover getMapMover() {
		if (this.mapMover == null) {
			// create and start the MapMover thread
			this.mapMover = new MapMover();
			this.mapMover.start();
		}
		return this.mapMover;
	}

	/**
	 * This method is called only once by the MapView after the setup.
	 * 
	 * @param mapView
	 *            the calling MapView.
	 */
	final void setMapView(MapView mapView) {
		this.mapView = mapView;
		SharedPreferences preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
		if (preferences.contains("mapFile") && preferences.contains("latitude")
				&& preferences.contains("longitude") && preferences.contains("zoomLevel")) {
			try {
				// get and set the current map file, map position and zoom level
				this.mapView.setMapFileFromPreferences(preferences.getString("mapFile", null));
				this.mapView.setCenterAndZoom(new GeoPoint(preferences.getInt("latitude", 0),
						preferences.getInt("longitude", 0)), (byte) preferences.getInt(
						"zoomLevel", 0));
			} catch (ClassCastException e) {
				// bad coordinates, do nothing
			}
		}
	}
}