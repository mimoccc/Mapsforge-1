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

import java.util.ArrayList;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/**
 * Abstract class which must be extended in order to use a MapView.
 */
public abstract class MapActivity extends Activity {
	private static final String PREFERENCES = "MapActivity";
	private ArrayList<MapView> mapViews = new ArrayList<MapView>(4);

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (this.mapViews != null) {
			this.mapViews.clear();
			this.mapViews = null;
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		Editor editor = getSharedPreferences(PREFERENCES, MODE_PRIVATE).edit();
		for (MapView currentMapView : this.mapViews) {
			currentMapView.onPause();

			editor.clear();
			if (currentMapView.hasValidCenter()) {
				if (currentMapView.getMapViewMode() != MapViewMode.TILE_DOWNLOAD
						&& currentMapView.hasValidMapFile()) {
					// save the map file
					editor.putString("mapFile", currentMapView.getMapFile());
				}
				// save the map position and zoom level
				GeoPoint mapCenter = currentMapView.getMapCenter();
				editor.putInt("latitude", mapCenter.getLatitudeE6());
				editor.putInt("longitude", mapCenter.getLongitudeE6());
				editor.putInt("zoomLevel", currentMapView.getZoomLevel());
			}
			editor.commit();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		for (MapView currentMapView : this.mapViews) {
			currentMapView.onResume();
		}
	}

	final void registerMapView(MapView mapView) {
		if (this.mapViews != null) {
			this.mapViews.add(mapView);

			SharedPreferences preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
			// restore the position
			if (preferences.contains("latitude") && preferences.contains("longitude")
					&& preferences.contains("zoomLevel")) {
				try {
					if (mapView.getMapViewMode() != MapViewMode.TILE_DOWNLOAD
							&& preferences.contains("mapFile")) {
						// get and set the map file
						mapView.setMapFileFromPreferences(preferences
								.getString("mapFile", null));
					}
					// get and set the map position and zoom level
					GeoPoint defaultStartPoint = mapView.getDefaultStartPoint();
					mapView.setCenterAndZoom(new GeoPoint(preferences.getInt("latitude",
							defaultStartPoint.getLatitudeE6()), preferences.getInt("longitude",
							defaultStartPoint.getLongitudeE6())), (byte) preferences.getInt(
							"zoomLevel", MapView.DEFAULT_ZOOM_LEVEL));
				} catch (ClassCastException e) {
					// bad coordinates, do nothing
				}
			}
		}
	}

	final void unregisterMapView(MapView mapView) {
		if (this.mapViews != null) {
			this.mapViews.remove(mapView);
		}
	}
}