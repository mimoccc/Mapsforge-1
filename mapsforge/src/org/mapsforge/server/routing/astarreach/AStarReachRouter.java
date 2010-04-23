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

public final class AStarReachRouter extends AStarRouter {

	public AStarReachRouter(IVehicle vehicle, IRoutingGraph routingGraph, IGeoMap geoMap,
			PropertyConfiguration<?> propConf) {
		super(vehicle, routingGraph, geoMap, propConf);
	}

	@Override
	public String getAlgorithmName() {
		return "A*/Reach Algorithm"; //$NON-NLS-1$
	}

	@Override
	void activate(int id, int cost, int pdcId) {
		this.costs[id] = cost;
		this.predecessors[id] = pdcId;
		/**
		 * estimations[n] holds the destination-estimation only; not the total costs estimation!
		 */
		// this.estimations[id] += this.costs[id];
		this.estimations[id] = estimate(id) + this.costs[id];
		this.forward.add(id);
	}

	@Override
	void expand(int v, int n, int newCost) {
		if (this.forward.contains(n)) {
			/** n is active */
			if (newCost < this.costs[n])
				update(n, newCost, v);
		} else if (this.closed[n]) {
			/** n is closed */
			if (newCost < this.costs[n])
				reactivate(n, newCost, v);
		} else {
			/** n is newly explored */
			int reach = this.routingGraph.getReachValue(n);

			this.estimations[n] = estimate(n);
			this.costs[n] = 0;
			if (newCost <= reach || this.estimations[n] /*- this.costs[n]*/<= reach)
				activate(n, newCost, v);
		}
	}
}
