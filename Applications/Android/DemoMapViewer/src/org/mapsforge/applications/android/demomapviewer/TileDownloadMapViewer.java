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
package org.mapsforge.applications.android.demomapviewer;

import org.mapsforge.android.map.MapActivity;
import org.mapsforge.android.map.MapView;
import org.mapsforge.android.map.MapViewMode;

import android.os.Bundle;

/**
 * A simple application which demonstrates how to use the MapView in TILE_DOWNLOAD mode.
 */
public class TileDownloadMapViewer extends MapActivity {
	private MapView mapView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.mapView = new MapView(this, MapViewMode.TILE_DOWNLOAD);
		this.mapView.setClickable(true);
		this.mapView.setBuiltInZoomControls(true);
		setContentView(this.mapView);
	}
}