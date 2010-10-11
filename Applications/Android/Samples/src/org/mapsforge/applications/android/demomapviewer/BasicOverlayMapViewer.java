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

import java.util.List;

import org.mapsforge.android.maps.ArrayItemizedOverlay;
import org.mapsforge.android.maps.GeoPoint;
import org.mapsforge.android.maps.ItemizedOverlay;
import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.Overlay;
import org.mapsforge.android.maps.OverlayItem;

import android.R;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

/**
 * A simple application which demonstrates how to use the overlays.
 */
public class BasicOverlayMapViewer extends MapActivity {
	private MapView mapView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.mapView = new MapView(this);
		this.mapView.setClickable(true);
		this.mapView.setBuiltInZoomControls(true);
		this.mapView.setMapFile("/sdcard/berlin.map");
		setContentView(this.mapView);

		// geopoint (Berlin, U Schillingstr.)
		GeoPoint geoPoint = new GeoPoint(52.520456604518344, 13.421924114227295);

		// default-marker for all items
		Drawable marker = getResources().getDrawable(R.drawable.btn_star);

		// create the overlay
		ItemizedOverlay myOverlay = new ArrayItemizedOverlay(marker, this);

		// create item
		OverlayItem item = new OverlayItem(geoPoint, "title", "short decripiton of this item");

		// add item to overlay
		myOverlay.addOverLay(item);

		// add overlay to MapView
		List<Overlay> overlays = this.mapView.getOverlays();
		overlays.add(myOverlay);
	}
}