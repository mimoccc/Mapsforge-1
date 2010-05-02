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
import android.os.Bundle;

public abstract class MapActivity extends Activity {
	private static final String PREFERENCES = "MapActivity";
	private MapGenerator mapGenerator;
	private Thread mapGeneratorThread;
	private MapView mapView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// create the map generator thread
		this.mapGenerator = new MapGenerator();
		this.mapGeneratorThread = new Thread(this.mapGenerator);
		this.mapGeneratorThread.start();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// stop the map generator thread
		if (this.mapGeneratorThread != null) {
			this.mapGeneratorThread.interrupt();
			try {
				this.mapGeneratorThread.join();
			} catch (InterruptedException e) {
				// restore the interrupted status
				Thread.currentThread().interrupt();
			}
			this.mapGeneratorThread = null;
		}
		this.mapView = null;
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (this.mapView == null) {
			return;
		}
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

	final MapGenerator getMapGenerator() {
		return this.mapGenerator;
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