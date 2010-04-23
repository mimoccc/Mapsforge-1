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
import org.mapsforge.server.routing.core.Issue;
import org.mapsforge.server.routing.core.PropertyConfiguration;
import org.mapsforge.server.routing.core.Router;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntIntHashMap;

abstract class AbstractAsrRouter extends Router {

	protected static final int[] EMPTY_INT_ARRAY = new int[0];

	protected static final int INFINITE_COST = IAsrRoutingGraph.INFINITE_COST;

	protected static final int VIRTUAL_DST_VTX = Integer.MAX_VALUE;

	protected final int initQueueSize;

	protected final int nVertices;

	protected final IAsrRoutingGraph routingGraph;

	public AbstractAsrRouter(IVehicle vehicle, IRoutingGraph routingGraph, IGeoMap geoMap,
			PropertyConfiguration<?> propConf) {
		super(geoMap, vehicle);
		if (!(routingGraph instanceof IAsrRoutingGraph))
			throw new IllegalArgumentException(Issue.R__ROUTINGGRAPH_NOT_APPROPRIATE.msg());
		this.routingGraph = (IAsrRoutingGraph) routingGraph;
		this.nVertices = getRoutingGraph().getNVertices();
		this.initQueueSize = (int) Math.sqrt(this.nVertices);
	}

	@Override
	public IAsrRoutingGraph getRoutingGraph() {
		return this.routingGraph;
	}

	protected final int[] computeRoute(int v, int[] predecessors) {
		assert v >= 0;
		int vtxId = v;
		TIntArrayList res = new TIntArrayList(this.initQueueSize);
		do {
			assert !res.contains(vtxId);
			res.insert(0, vtxId);
		} while ((vtxId = predecessors[vtxId]) >= 0);
		return res.toNativeArray();
	}

	protected final int[] computeRoute(int v, TIntIntHashMap predecessors) {
		assert v >= 0;
		int vtxId = v;
		TIntArrayList res = new TIntArrayList(this.initQueueSize);
		do {
			assert !res.contains(vtxId);
			res.insert(0, vtxId);
		} while ((vtxId = predecessors.get(vtxId)) >= 0);
		return res.toNativeArray();
	}
}
