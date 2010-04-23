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
import android.os.Debug;
import android.view.KeyEvent;

public abstract class MapActivity extends Activity {
	private static final String PREFERENCES = "MapActivity";
	private MapGenerator mapGenerator;
	private Thread mapGeneratorThread;
	private MapView mapView;
	private boolean traceModeEnabled;

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
		if (this.mapView.hasValidCenter()) {
			// save the current map position and zoomLevel level
			editor.putFloat("latitude", (float) this.mapView.getLatitude());
			editor.putFloat("longitude", (float) this.mapView.getLongitude());
			editor.putInt("zoomLevel", this.mapView.getZoomLevel());
		}
		editor.commit();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
			if (this.traceModeEnabled) {
				Debug.stopMethodTracing();
				Logger.d("stopMethodTracing");
			} else {
				Logger.d("startMethodTracing");
				Debug.startMethodTracing();
			}
			this.traceModeEnabled = !this.traceModeEnabled;
			return true;
		}
		return false;
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (this.mapView == null) {
			return;
		}
		SharedPreferences preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
		if (preferences.contains("latitude") && preferences.contains("longitude")
				&& preferences.contains("zoomLevel")) {
			// get the current map position and zoomLevel level
			this.mapView.setCenterAndZoom(preferences.getFloat("latitude", 0), preferences
					.getFloat("longitude", 0), (byte) preferences.getInt("zoomLevel", 0));
		}
	}

	final MapGenerator getMapGenerator() {
		return this.mapGenerator;
	}

	final void setMapView(MapView mapView) {
		this.mapView = mapView;
	}
}