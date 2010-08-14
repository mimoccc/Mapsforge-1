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
package org.mapsforge.android.routing;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import org.mapsforge.android.routing.hh.HHEdge;
import org.mapsforge.android.routing.hh.HHRouter;
import org.mapsforge.android.routing.hh.HHVertex;
import org.mapsforge.core.GeoCoordinate;

class RoutingSample {

	public static void main(String[] args) throws IOException {
		// initialize the router
		HHRouter router = new HHRouter(new File("germany.hhmobile"), 1024 * 1000);

		// lookup source and target vertices
		HHVertex s = router.getNearestVertex(new GeoCoordinate(52.509769, 13.4567655), 300);
		HHVertex t = router.getNearestVertex(new GeoCoordinate(52.4556941, 13.2918805), 300);

		// compute the shortest path
		LinkedList<HHEdge> shortestPath = new LinkedList<HHEdge>();
		int distance = router.getShortestPath(s.getId(), t.getId(), shortestPath);

		System.out.println("travelTime = " + (distance / 10) + "s");
		System.out.println("numEdges = " + shortestPath.size());

		// release objects to the pool
		for (HHEdge e : shortestPath) {
			router.release(e);
		}
		router.release(s);
		router.release(t);

		System.out.println(router.toString());
	}

}
