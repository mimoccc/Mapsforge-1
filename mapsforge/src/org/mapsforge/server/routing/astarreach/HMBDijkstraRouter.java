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
package org.mapsforge.server.routing.astarreach;

import gnu.trove.TIntIntHashMap;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.logging.Level;

import org.mapsforge.core.conf.IVehicle;
import org.mapsforge.server.core.geoinfo.Point;
import org.mapsforge.server.routing.core.IGeoMap;
import org.mapsforge.server.routing.core.IRoutingGraph;
import org.mapsforge.server.routing.core.PropertyConfiguration;
import org.mapsforge.server.routing.core.IGeoMap.VertexDistance;

/**
 * This is a HashMap based (HMB) variant of the DijkstraRouter.
 * 
 */
public final class HMBDijkstraRouter extends AbstractAsrRouter {

	public HMBDijkstraRouter(IVehicle vehicle, IRoutingGraph routingGraph, IGeoMap geoMap,
			PropertyConfiguration<?> propConf) {
		super(vehicle, routingGraph, geoMap, propConf);
	}

	@Override
	public String getAlgorithmName() {
		return "Unidirectional Dijkstra Algorithm (HashMap based)"; //$NON-NLS-1$
	}

	@Override
	protected int[] route(Point source, Point destination) {
		/** create local cost field to store the calculation data in */
		final TIntIntHashMap costs = new TIntIntHashMap(this.initQueueSize);

		/** create predecessor's map */
		final TIntIntHashMap predecessors = new TIntIntHashMap(this.initQueueSize);

		/** create local Comparator for the costs */
		Comparator<Integer> cc = new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				int c1 = costs.get(o1);
				int c2 = costs.get(o2);
				return (c1 < c2) ? -1 : (c1 > c2) ? 1 : 0;
			}
		};

		PriorityQueue<Integer> forward = new PriorityQueue<Integer>(this.initQueueSize, cc);

		/** determine all vertices to start from and to end at respectively */
		VertexDistance[] srcs = getGeoMap().getAdjacentVertices(source);
		VertexDistance[] dsts = getGeoMap().getAdjacentVertices(destination);
		/** catch empty arrays */
		if (srcs.length == 0 || dsts.length == 0)
			return EMPTY_INT_ARRAY;

		/** add real source vertices to the queues */
		for (VertexDistance src : srcs) {
			costs.put(src.vtxId, getVehicle().getCost(src.distance));
			/** for sources set predecessor to -1 */
			predecessors.put(src.vtxId, -1);
			forward.add(src.vtxId);
		}

		/** calculate the virtual destination's edges costs */
		int[] dstCosts = new int[dsts.length];
		for (int d = 0; d < dsts.length; d++)
			dstCosts[d] = getVehicle().getCost(dsts[d].distance);

		/** process the active vertices */
		IAsrRoutingGraph rg = getRoutingGraph();
		int v, p;
		int newCost, oldCost;
		while (!forward.isEmpty()) {

			LOGGER.log(Level.FINE, "forward-queue contains " + forward.size()); //$NON-NLS-1$

			v = forward.poll();
			p = predecessors.get(v);

			if (v == VIRTUAL_DST_VTX) {
				/** virtual destination vertex and so the best route was found */
				return computeRoute(predecessors.get(VIRTUAL_DST_VTX), predecessors);
			}

			/** expand v: add virtual destination if v is a real destination */
			for (int d = 0; d < dsts.length; d++)
				if (dsts[d].vtxId == v) {
					/**
					 * found a route to the virtual destination vertex, possibly this is the
					 * best one, but we can't be sure! Further searching is necessary: insert
					 * the virtual destination vertex into the queue to be processed (like any
					 * other vertex). Therefore the cost needs to be updated only:
					 */
					/** determine the rule cost */
					newCost = rg.getRuleCost(p, v, dsts[d].vtxId);
					if (newCost == INFINITE_COST)
						break;
					/** add the current total cost and the partial edge's cost */
					newCost += costs.get(v) + dstCosts[d];

					if (forward.contains(VIRTUAL_DST_VTX)) {
						/**
						 * virtual destination already was active, now we know that better paths
						 * are impossible!
						 */
						if (newCost < costs.get(VIRTUAL_DST_VTX)) {
							/** the new path is the best one */
							return computeRoute(v, predecessors);
						}
						/** the old path is the best one */
						return computeRoute(predecessors.get(VIRTUAL_DST_VTX), predecessors);
					}

					/**
					 * virtual destination has not been active, better paths are possible!
					 */
					costs.put(VIRTUAL_DST_VTX, newCost);
					predecessors.put(VIRTUAL_DST_VTX, v);
					forward.add(VIRTUAL_DST_VTX);
					break;
				}

			/** expand v: for each real neighbor calculate new costs */
			for (int n : rg.getOutNeighbors(v)) {

				newCost = rg.getRuleCost(p, v, n);
				if (newCost == INFINITE_COST)
					continue;
				newCost += costs.get(v) + rg.getEdgeCost(v, n);
				oldCost = costs.get(n);

				LOGGER
						.log(
								Level.FINER,
								"expanding edge from ID " + v + " to ID " + n + " with newCost=" + newCost + " and oldCost=" + oldCost); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

				if (forward.contains(n)) {
					/** n is active */
					if (newCost < oldCost) {
						/** reset cost & update position in active queue */
						forward.remove(n);
						costs.put(n, newCost);
						predecessors.put(n, v);
						forward.add(n);
					}
				} else if (newCost < oldCost || !predecessors.containsKey(n)) {
					/**
					 * predecessor only has no key n if vertex n has not been visited before<br/>
					 * newCost < oldCost means: vertex n has been visited and vertex n is
					 * 'closed'<br/>
					 */
					costs.put(n, newCost);
					predecessors.put(n, v);
					forward.add(n);
				}
			}
		}
		return EMPTY_INT_ARRAY;
	}

}
