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

import org.mapsforge.core.conf.IVehicle;
import org.mapsforge.server.routing.core.IGeoMap;
import org.mapsforge.server.routing.core.IRoutingGraph;
import org.mapsforge.server.routing.core.PropertyConfiguration;

public class AStarRouter extends DijkstraRouter {

	int[] estimations;

	public AStarRouter(IVehicle vehicle, IRoutingGraph routingGraph, IGeoMap geoMap,
			PropertyConfiguration<?> propConf) {
		super(vehicle, routingGraph, geoMap, propConf);
	}

	@Override
	public String getAlgorithmName() {
		return "A* Algorithm"; //$NON-NLS-1$
	}

	@Override
	void activate(int id, int cost, int pdcId) {
		this.costs[id] = cost;
		this.predecessors[id] = pdcId;
		this.estimations[id] = estimate(id) + this.costs[id];
		this.forward.add(id);
	}

	int estimate(int id) {
		int optimisticDistance = this.destination.distanceTo(this.getGeoMap()
				.getVertexPoint(id));

		// TODO: only as long as Vehicle.getCost uses meters, not decimeters as
		// distance measure
		optimisticDistance /= 11;

		return getVehicle().getCost(optimisticDistance);
	}

	@Override
	void initQueues() {
		this.estimations = new int[this.nVertices];
		this.forward = new TIntInt_PriorityMinHeap(this.initQueueSize, this.estimations);
	}

	@Override
	void reactivate(int id, int cost, int pdcId) {
		int dist = this.estimations[id] - this.costs[id];
		this.closed[id] = false;
		this.costs[id] = cost;
		this.predecessors[id] = pdcId;
		this.estimations[id] = dist + this.costs[id];
		this.forward.add(id);
	}

	@Override
	void update(int id, int cost, int pdcId) {
		int dist = this.estimations[id] - this.costs[id];
		this.forward.remove(id);
		this.costs[id] = cost;
		this.predecessors[id] = pdcId;
		this.estimations[id] = dist + this.costs[id];
		this.forward.add(id);
	}

}
