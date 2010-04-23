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

import java.util.Arrays;

import org.mapsforge.core.conf.IVehicle;
import org.mapsforge.server.core.geoinfo.Point;
import org.mapsforge.server.routing.core.IGeoMap;
import org.mapsforge.server.routing.core.IRoutingGraph;
import org.mapsforge.server.routing.core.Messages;
import org.mapsforge.server.routing.core.PropertyConfiguration;
import org.mapsforge.server.routing.core.Router;
import org.mapsforge.server.routing.core.IGeoMap.VertexDistance;

public class DijkstraRouter extends AbstractAsrRouter {

	static final int NO_PREDECESSOR = -1;

	final boolean[] closed = new boolean[this.nVertices];
	final int[] costs = new int[this.nVertices];
	Point destination;
	int dstCost;
	/** two destination vertices are possible in maximum */
	final int[] dstCosts = new int[2];
	int dstPdc;
	int expandedVtcs = 0;
	TIntInt_PriorityMinHeap forward;
	final int[] predecessors = new int[this.nVertices];

	Point source;

	int visitedVertices = 0;

	public DijkstraRouter(IVehicle vehicle, IRoutingGraph routingGraph, IGeoMap geoMap,
			PropertyConfiguration<?> propConf) {
		super(vehicle, routingGraph, geoMap, propConf);
		initQueues();
	}

	@Override
	public String getAlgorithmName() {
		return "Unidirectional Dijkstra Algorithm"; //$NON-NLS-1$
	}

	@Override
	protected final synchronized int[] route(Point source, Point destination) {
		this.source = source;
		this.destination = destination;

		/** prepare for new routing procedure */
		initRouting();

		/** determine all vertices to start from and to end at respectively */
		VertexDistance[] srcs = getGeoMap().getAdjacentVertices(source);
		VertexDistance[] dsts = getGeoMap().getAdjacentVertices(destination);

		/** catch empty arrays */
		if (srcs.length == 0 || dsts.length == 0)
			return EMPTY_INT_ARRAY;

		/** add post-source vertices to the queues */
		for (VertexDistance src : srcs)
			activate(src.vtxId, getVehicle().getCost(src.distance), NO_PREDECESSOR);

		/** calculate the virtual destination's edges costs */
		for (int d = 0; d < dsts.length; d++)
			this.dstCosts[d] = getVehicle().getCost(dsts[d].distance);

		/** process the active vertices */
		int v, p;
		int newCost;
		while (!this.forward.isEmpty()) {
			this.expandedVtcs++;

			/** get best vertex and its predecessor */
			v = this.forward.poll();
			p = this.predecessors[v];

			/** close it */
			this.closed[v] = true;

			if (this.costs[v] >= this.dstCost) {
				/**
				 * virtual destination vertex has best cost and so the best route was found
				 */
				LOGGER.fine(String.format(Messages
						.getString(Router.Message.ROUTER_IMPLEMENTATION), this.nVertices,
						this.visitedVertices, this.expandedVtcs, this.dstCost));

				return computeRoute(this.dstPdc, this.predecessors);
			}

			/**
			 * expand v: add virtual destination if v is a pre-destination vertex
			 */
			for (int d = 0; d < dsts.length; d++)
				if (dsts[d].vtxId == v) {
					/**
					 * found a route to the virtual destination vertex, possibly this is the
					 * best one, but we can't be sure! Further searching is necessary: insert
					 * the virtual destination vertex into the queue to be processed (like any
					 * other vertex). Therefore the cost needs to be updated only:
					 */
					/** determine the rule cost */
					newCost = this.routingGraph.getRuleCost(p, v, dsts[d].vtxId);
					if (newCost == INFINITE_COST)
						break;
					/** add the current total cost and the partial edge's cost */
					newCost += this.dstCosts[d];
					newCost += this.costs[v];

					if (this.dstPdc != NO_PREDECESSOR || newCost < this.dstCost) {
						/** the new path is better */
						this.dstCost = newCost;
						this.dstPdc = v;
					}

					break;
				}

			/** expand v: for each real neighbor calculate new costs */
			int[] neighborIds = this.routingGraph.getOutNeighbors(v);

			for (int n : neighborIds) {
				this.visitedVertices++;

				/** determine new cost for reaching n via v */
				newCost = this.routingGraph.getRuleCost(p, v, n);
				if (newCost == INFINITE_COST)
					continue;
				newCost += this.routingGraph.getEdgeCost(v, n);
				if (newCost == INFINITE_COST)
					continue;
				newCost += this.costs[v];
				if (newCost == INFINITE_COST)
					continue;

				/** do the expansion of the discovered graph */
				expand(v, n, newCost);
			}
		}
		/** no route was found */
		return null;
	}

	void activate(int id, int cost, int pdcId) {
		this.costs[id] = cost;
		this.predecessors[id] = pdcId;
		this.forward.add(id);
	}

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
			activate(n, newCost, v);
		}
	}

	void initQueues() {
		this.forward = new TIntInt_PriorityMinHeap(this.initQueueSize, this.costs);
	}

	void initRouting() {
		this.expandedVtcs = 0;
		this.visitedVertices = 0;

		/** reset priority queue for the active/open vertices */
		this.forward.clear();

		/** bitmap for the inactive/closed vertices */
		Arrays.fill(this.closed, false);

		/** handle virtual destination special */
		this.dstPdc = NO_PREDECESSOR;
		this.dstCost = INFINITE_COST;
	}

	void reactivate(int id, int cost, int pdcId) {
		this.closed[id] = false;
		this.costs[id] = cost;
		this.predecessors[id] = pdcId;
		this.forward.add(id);
	}

	void update(int id, int cost, int pdcId) {
		this.forward.remove(id);
		this.costs[id] = cost;
		this.predecessors[id] = pdcId;
		this.forward.add(id);
	}
}
