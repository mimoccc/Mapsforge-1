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
package org.mapsforge.applications.android.samples;

import java.util.Collection;
import java.util.Vector;

import org.mapsforge.android.maps.GeoPoint;
import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.overlay.ArrayItemizedOverlay;
import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.storage.poi.PoiPersistenceManager;
import org.mapsforge.storage.poi.PoiPersistenceManagerFactory;
import org.mapsforge.storage.poi.PointOfInterest;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

/**
 * A simple application which demonstrates how to use the POI API for read access. Please take a
 * look at {@link OverlayMapViewer} to learn more about using overlays.
 */
public class POIViewer extends MapActivity {
	private class MyItemizedOverlay extends ArrayItemizedOverlay {
		private final Context context;

		/**
		 * Constructs a new MyItemizedOverlay.
		 * 
		 * @param defaultMarker
		 *            the default marker (may be null).
		 * @param context
		 *            the reference to the application context.
		 */
		MyItemizedOverlay(Drawable defaultMarker, Context context) {
			super(defaultMarker);
			this.context = context;
		}

		/**
		 * Handles a tap event on the given item.
		 */
		@Override
		protected boolean onTap(int index) {
			OverlayItem item = createItem(index);
			if (item != null) {
				Builder builder = new AlertDialog.Builder(this.context);
				builder.setIcon(android.R.drawable.ic_menu_info_details);
				builder.setTitle(item.getTitle());
				builder.setMessage(item.getSnippet());
				builder.setPositiveButton("OK", null);
				builder.show();
			}
			return true;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MapView mapView = new MapView(this);
		mapView.setClickable(true);
		mapView.setBuiltInZoomControls(true);
		mapView.setMapFile("/sdcard/berlin.map");
		setContentView(mapView);

		// The persistence manager is used for reading and storing POIs
		PoiPersistenceManager pm = PoiPersistenceManagerFactory
				.getSQLitePoiPersistenceManager("/sdcard/mapsforge/berlin.poi");

		// Get all POIs within a radius of 500 meters from the Brandenburg Gate
		int radius = 500;
		Collection<PointOfInterest> pois = pm.findNearPosition(new GeoCoordinate(52.516272,
				13.377722), radius, "Root category", 1000);

		// Close the persistence manager as it is no longer needed
		pm.close();

		// Overlay stuff
		Drawable itemDefaultMarker = getResources().getDrawable(R.drawable.marker_green);
		ArrayItemizedOverlay poiOverlay = new MyItemizedOverlay(itemDefaultMarker, this);

		// Create overlay items from retrieved POIs
		Collection<OverlayItem> poiOverlayItems = new Vector<OverlayItem>();
		for (PointOfInterest p : pois) {
			GeoPoint point = new GeoPoint(p.getLatitude(), p.getLongitude());
			OverlayItem i = new OverlayItem(point, p.getName(), "Category: "
					+ p.getCategory().getTitle());
			poiOverlayItems.add(i);
		}

		poiOverlayItems.add(new OverlayItem(new GeoPoint(52.516272, 13.377722),
				"Brandenburger Tor", "POI lookup center point"));

		poiOverlay.addItems(poiOverlayItems);
		mapView.getOverlays().add(poiOverlay);

	}
}