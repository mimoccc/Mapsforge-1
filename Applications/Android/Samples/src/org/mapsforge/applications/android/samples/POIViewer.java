/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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

import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.overlay.ArrayItemizedOverlay;
import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.core.GeoPoint;
import org.mapsforge.storage.poi.PoiPersistenceManager;
import org.mapsforge.storage.poi.PoiPersistenceManagerFactory;
import org.mapsforge.storage.poi.PointOfInterest;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;

/**
 * This application shows how the POI component is used.
 */
public class POIViewer extends MapActivity {
	private static class MyItemizedOverlay extends ArrayItemizedOverlay {
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

		// Create DAO object for a given POI database
		// Created with "osmosis --rb /path/to/berlin.osm.pbf --poi-writer /path/to/berlin.poi
		// categoryConfigPath=svn_trunk/mapsforge-poi-writer/src/main/config/POICategoriesOsmosis.xml"
		PoiPersistenceManager pm = PoiPersistenceManagerFactory.getSQLitePoiPersistenceManager(Environment
				.getExternalStorageDirectory().getAbsolutePath() + "/berlin.poi");

		// Brandenburg Gate
		GeoCoordinate geoCoordinate = new GeoCoordinate(52.516272, 13.377722);
		// Search radius
		int radiusMeters = 2000;
		// Maximum number of POIs to be returned
		int searchLimit = 1000;

		// Search for all POIs within a given radius
		Collection<PointOfInterest> searchResults = pm.findNearPosition(geoCoordinate, radiusMeters, searchLimit);

		// create the default marker for overlay items
		Drawable itemDefaultMarker = getResources().getDrawable(R.drawable.marker_red);

		// create the ItemizedOverlay and add the items
		ArrayItemizedOverlay itemizedOverlay = new MyItemizedOverlay(itemDefaultMarker, this);

		// Create overlay objects
		for (PointOfInterest p : searchResults) {
			GeoPoint coords = new GeoPoint(p.getLatitude(), p.getLongitude());
			// For this version data does only consist out of a the POI's name
			String data = p.getData();
			long id = p.getId();

			OverlayItem i = new OverlayItem(coords, "ID: " + id, "Data: " + data + " Category: "
					+ p.getCategory().getTitle());
			itemizedOverlay.addItem(i);
		}

		// add all overlays to the MapView
		mapView.getOverlays().add(itemizedOverlay);
	}
}
