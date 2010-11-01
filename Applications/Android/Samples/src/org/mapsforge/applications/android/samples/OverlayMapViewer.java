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
package org.mapsforge.applications.android.samples;

import org.mapsforge.android.maps.ArrayItemizedOverlay;
import org.mapsforge.android.maps.GeoPoint;
import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.OverlayItem;
import org.mapsforge.android.maps.RouteOverlay;

import android.R;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;

/**
 * An application which demonstrates how to use different types of Overlays.
 */
public class OverlayMapViewer extends MapActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MapView mapView = new MapView(this);
		mapView.setClickable(true);
		mapView.setBuiltInZoomControls(true);
		mapView.setMapFile("/sdcard/berlin.map");
		setContentView(mapView);

		// create some points to be shown on top of the map
		GeoPoint geoPoint1 = new GeoPoint(52.514446, 13.350150);
		GeoPoint geoPoint2 = new GeoPoint(52.516272, 13.377722);
		OverlayItem item1 = new OverlayItem(geoPoint1, "Victory Column",
				"a major tourist attraction");
		OverlayItem item2 = new OverlayItem(geoPoint2, "Brandenburg Gate",
				"one of the main symbols of Berlin");

		// create the paint object for the RouteOverlay and set all parameters
		Paint paint = new Paint();
		paint.setStyle(Paint.Style.STROKE);
		paint.setColor(Color.BLUE);
		paint.setAlpha(128);
		paint.setStrokeWidth(6);
		paint.setStrokeCap(Paint.Cap.ROUND);
		paint.setStrokeJoin(Paint.Join.ROUND);

		// create the RouteOverlay and set the way nodes
		RouteOverlay routeOverlay = new RouteOverlay(paint);
		routeOverlay.setRouteData(new GeoPoint[] { geoPoint1, geoPoint2 });

		// create the ItemizedOverlay and set the items
		ArrayItemizedOverlay itemizedOverlay = new ArrayItemizedOverlay(getResources()
				.getDrawable(R.drawable.btn_star), this);
		itemizedOverlay.addOverlay(item1);
		itemizedOverlay.addOverlay(item2);

		// add both Overlays to the MapView
		mapView.getOverlays().add(routeOverlay);
		mapView.getOverlays().add(itemizedOverlay);
	}
}