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
package org.mapsforge.applications.android.samples;

import org.mapsforge.android.maps.ArrayCircleOverlay;
import org.mapsforge.android.maps.ArrayItemizedOverlay;
import org.mapsforge.android.maps.ArrayRouteOverlay;
import org.mapsforge.android.maps.GeoPoint;
import org.mapsforge.android.maps.ItemizedOverlay;
import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.OverlayCircle;
import org.mapsforge.android.maps.OverlayItem;
import org.mapsforge.android.maps.OverlayRoute;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

/**
 * An application which demonstrates how to use different types of overlays.
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

		// create some points to be used in the different overlays
		GeoPoint geoPoint1 = new GeoPoint(52.514446, 13.350150); // Berlin Victory Column
		GeoPoint geoPoint2 = new GeoPoint(52.516272, 13.377722); // Brandenburg Gate
		GeoPoint geoPoint3 = new GeoPoint(52.525, 13.369444); // Berlin Central Station
		GeoPoint geoPoint4 = new GeoPoint(52.52, 13.369444); // German Chancellery

		// create the default paint objects for overlay circles
		Paint circleDefaultPaintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
		circleDefaultPaintFill.setStyle(Paint.Style.FILL);
		circleDefaultPaintFill.setColor(Color.BLUE);
		circleDefaultPaintFill.setAlpha(64);

		Paint circleDefaultPaintOutline = new Paint(Paint.ANTI_ALIAS_FLAG);
		circleDefaultPaintOutline.setStyle(Paint.Style.STROKE);
		circleDefaultPaintOutline.setColor(Color.BLUE);
		circleDefaultPaintOutline.setAlpha(128);
		circleDefaultPaintOutline.setStrokeWidth(3);

		// create an individual paint object for an overlay circle
		Paint circleIndividualPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		circleIndividualPaint.setStyle(Paint.Style.FILL);
		circleIndividualPaint.setColor(Color.MAGENTA);
		circleIndividualPaint.setAlpha(96);

		// create the CircleOverlay and add the circles
		ArrayCircleOverlay circleOverlay = new ArrayCircleOverlay(circleDefaultPaintFill,
				circleDefaultPaintOutline);
		OverlayCircle circle1 = new OverlayCircle(geoPoint3, 200);
		OverlayCircle circle2 = new OverlayCircle(geoPoint4, 150, circleIndividualPaint, null);
		circleOverlay.addCircle(circle1);
		circleOverlay.addCircle(circle2);

		// create the default paint objects for overlay routes
		Paint routeDefaultPaintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
		routeDefaultPaintFill.setStyle(Paint.Style.STROKE);
		routeDefaultPaintFill.setColor(Color.BLUE);
		routeDefaultPaintFill.setAlpha(160);
		routeDefaultPaintFill.setStrokeWidth(7);
		routeDefaultPaintFill.setStrokeJoin(Paint.Join.ROUND);
		routeDefaultPaintFill.setPathEffect(new DashPathEffect(new float[] { 20, 20 }, 0));

		Paint routeDefaultPaintOutline = new Paint(Paint.ANTI_ALIAS_FLAG);
		routeDefaultPaintOutline.setStyle(Paint.Style.STROKE);
		routeDefaultPaintOutline.setColor(Color.BLUE);
		routeDefaultPaintOutline.setAlpha(128);
		routeDefaultPaintOutline.setStrokeWidth(7);
		routeDefaultPaintOutline.setStrokeJoin(Paint.Join.ROUND);

		// create an individual paint object for an overlay route
		Paint routeIndividualPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		routeIndividualPaint.setStyle(Paint.Style.FILL);
		routeIndividualPaint.setColor(Color.YELLOW);
		routeIndividualPaint.setAlpha(192);

		// create the RouteOverlay and add the routes
		ArrayRouteOverlay routeOverlay = new ArrayRouteOverlay(routeDefaultPaintFill,
				routeDefaultPaintOutline);
		OverlayRoute route1 = new OverlayRoute(new GeoPoint[] { geoPoint1, geoPoint2 });
		OverlayRoute route2 = new OverlayRoute(new GeoPoint[] { geoPoint1, geoPoint3,
				geoPoint4, geoPoint1 }, routeIndividualPaint, null);
		routeOverlay.addRoute(route1);
		routeOverlay.addRoute(route2);

		// create the default marker for overlay items
		Drawable itemDefaultMarker = getResources().getDrawable(R.drawable.marker_red);

		// create an individual marker for an overlay item
		Drawable itemIndividualMarker = getResources().getDrawable(R.drawable.marker_green);

		// create the ItemizedOverlay and add the items
		ArrayItemizedOverlay itemizedOverlay = new ArrayItemizedOverlay(itemDefaultMarker, this);
		OverlayItem item1 = new OverlayItem(geoPoint1, "Berlin Victory Column",
				"The Victory Column is a monument in Berlin, Germany.");
		OverlayItem item2 = new OverlayItem(geoPoint2, "Brandenburg Gate",
				"The Brandenburg Gate is one of the main symbols of Berlin and Germany.",
				ItemizedOverlay.boundCenterBottom(itemIndividualMarker));
		itemizedOverlay.addItem(item1);
		itemizedOverlay.addItem(item2);

		// add all overlays to the MapView
		mapView.getOverlays().add(routeOverlay);
		mapView.getOverlays().add(circleOverlay);
		mapView.getOverlays().add(itemizedOverlay);
	}
}