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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.mapsforge.android.map.MapActivity;
import org.mapsforge.android.map.MapView;
import org.mapsforge.android.map.RouteOverlay;
import org.mapsforge.android.routing.blockedHighwayHierarchies.HHRouter;
import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.server.routing.IEdge;
import org.mapsforge.server.routing.IVertex;

import android.os.Bundle;
import android.os.Debug;
import android.util.Log;

/**
 * A simple application which demonstrates how to use the router overlays & routing.
 */
public class RouteOverlayMapViewer extends MapActivity {
	private MapView mapView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// initialize map renderer
		super.onCreate(savedInstanceState);
		this.mapView = new MapView(this);
		this.mapView.setClickable(true);
		this.mapView.setBuiltInZoomControls(true);
		this.mapView.setMapFile("/sdcard/berlin.map");
		setContentView(this.mapView);

		// compute route
		int routerCacheSizeBytes = 2 * 1000 * 1024; // 2MB
		try {
			HHRouter router = new HHRouter(new File("/sdcard/germany.blockedHH"),
					routerCacheSizeBytes);

			// choose source and target
			IVertex source = router.getNearestVertex(new GeoCoordinate(52.509769, 13.4567655));
			// IVertex target = router.getNearestVertex(new GeoCoordinate(52.4556941,
			// 13.2918805));
			IVertex target = router.getNearestVertex(new GeoCoordinate(48.103733, 11.582680));

			// compute shortest path
			long startTime = System.currentTimeMillis();
			Debug.startMethodTracing("calc");
			IEdge[] shortestPath = router.getShortestPath(source.getId(), target.getId());
			Debug.stopMethodTracing();
			long time = System.currentTimeMillis() - startTime;
			Log.i("Router", "computed route in " + time + "ms");

			// extract the coordinates
			ArrayList<GeoCoordinate> coordinates = new ArrayList<GeoCoordinate>();
			for (IEdge e : shortestPath) {
				GeoCoordinate[] waypoints = e.getAllWaypoints();
				for (int i = 1; i < waypoints.length; i++) {
					coordinates.add(waypoints[i]);
				}
			}

			// construct array for route overlay
			double[][] routeData = new double[coordinates.size()][2];
			for (int i = 0; i < coordinates.size(); i++) {
				routeData[i][0] = coordinates.get(i).getLatitude();
				routeData[i][1] = coordinates.get(i).getLongitude();
			}

			// create the route overlay
			RouteOverlay routeOverlay = new RouteOverlay();
			routeOverlay.setRouteData(routeData);
			this.mapView.getOverlays().add(routeOverlay);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}