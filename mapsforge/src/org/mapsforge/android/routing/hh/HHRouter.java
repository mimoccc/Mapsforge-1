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
package org.mapsforge.android.routing.hh;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import org.mapsforge.android.routing.hh.ObjectPool.PoolableFactory;
import org.mapsforge.core.GeoCoordinate;

/**
 * This class implements the only interface of the complete package to the outer world. The
 * reason for encapsulating the routing graph and the algorithm with this class is mainly to
 * hide object pooling as far as possible and to provide java style method signatures (internal
 * package classes often use c style void functions writing their result into buffers).
 * 
 * All objects (Vertices and Edges) returned by this class should be released to the object pool
 * using this classes's release methods. This can be useful with regard to performance due to
 * avoiding garbage collection. The Object pools are implemented in a way that there will be no
 * memory leaks if one forgets to release one of the objects. That is to say no pointer is
 * stored somewhere and the garbage collection will waste these objects later on.
 * 
 * This class is not thread safe. Shortest path queries are not likely to be a candidate for
 * thread safety. But maybe it will be desired to access the graph while shortest path queries
 * are running. To achieve this only little work has to be done. Points to start will be
 * synchronizing the borrow / release methods of the object pool, synchronizing the LRU cache
 * and last but not least the block reads.
 */
public class HHRouter {

	private static final int POOL_VERTICES_INITIAL_SIZE = 0;
	private static final int POOL_EDGES_INITIAL_SIZE = 0;

	private final HighwayHierarchiesAlgorithm algorithm;
	private final RoutingGraph routingGraph;
	private final ObjectPool<HHVertex> poolVertices;
	private final ObjectPool<HHEdge> poolEdges;

	/**
	 * Construct a Highway hierarchies router object on a specific binary file.
	 * 
	 * @param hhBinaryFile
	 *            the highway hierarchies binary file.
	 * @param cacheSizeBytes
	 *            size of cache used for the blocked graph.
	 * @throws IOException
	 *             if there was an error reading the file.
	 */
	public HHRouter(File hhBinaryFile, int cacheSizeBytes) throws IOException {
		this.poolVertices = new ObjectPool<HHVertex>(new PoolableFactory<HHVertex>() {

			@Override
			public HHVertex makeObject() {
				return new HHVertex();
			}

		}, POOL_VERTICES_INITIAL_SIZE);
		this.poolEdges = new ObjectPool<HHEdge>(new PoolableFactory<HHEdge>() {
			@Override
			public HHEdge makeObject() {
				return new HHEdge();
			}
		}, POOL_EDGES_INITIAL_SIZE);

		this.routingGraph = new RoutingGraph(hhBinaryFile, cacheSizeBytes);
		this.algorithm = new HighwayHierarchiesAlgorithm(routingGraph, poolVertices, poolEdges);
	}

	/**
	 * Computes the shortest path between the source and the target. The sum of the edge weights
	 * along the path is returned. If no path was found, Integer.MAX_VALUE is returned. The path
	 * description is put into the given edge list sorted from source to target.
	 * 
	 * @param sourceId
	 *            id of the source vertex.
	 * @param targetId
	 *            id of the target vertex.
	 * @param shortestPathBuff
	 *            list to put the path description in, must be empty!
	 * @return sum of edge weights along the shortest path or Integer.MAX_VALUE if none has been
	 *         found.
	 * @throws IOException
	 *             on error reading file.
	 */
	public int getShortestPath(int sourceId, int targetId, LinkedList<HHEdge> shortestPathBuff)
			throws IOException {
		return algorithm.getShortestPath(sourceId, targetId, shortestPathBuff);
	}

	/**
	 * Finds the nearest Vertex to the given Coordinate. At Least all vertices v having
	 * distance(c, v) < maxDistanceMeters are examined.
	 * 
	 * @param c
	 *            the key coordinate.
	 * @param distanceMeters
	 *            lower bound on distance to c, which decides if a vertex is a potential
	 *            candidate result.
	 * @return the vertex nearest to c.
	 * @throws IOException
	 *             on error reading file.
	 */
	public HHVertex getNearestVertex(GeoCoordinate c, double distanceMeters) throws IOException {
		HHVertex v = poolVertices.borrow();
		if (routingGraph.getNearestVertex(c, distanceMeters, v)) {
			return v;
		}
		poolVertices.release(v);
		return null;
	}

	/**
	 * Looks up the vertex identified by the given id. If the vertex of given id does not
	 * exists, the result is not defined. It can be either an IOException or returning null.
	 * 
	 * @param vertexId
	 *            vertex identifier.
	 * @return the vertex, or null.
	 * @throws IOException
	 *             caused by invalid vertexId or by errors reading file.
	 */
	public HHVertex getVertex(int vertexId) throws IOException {
		HHVertex v = poolVertices.borrow();
		if (routingGraph.getVertex(vertexId, v)) {
			return v;
		}
		poolVertices.release(v);
		return null;
	}

	/**
	 * Gives the i-th outbound edge of the given vertex.
	 * 
	 * @param v
	 *            a vertex.
	 * @param i
	 *            the index to the adjacency list of v.
	 * @return returns null if i is out of range, the edge otherwise.
	 * @throws IOException
	 *             on error reading file.
	 */
	public HHEdge getOutboundEdge(HHVertex v, int i) throws IOException {
		HHEdge e = poolEdges.borrow();
		if (routingGraph.getOutboundEdge(v, i, e)) {
			return e;
		}
		poolEdges.release(e);
		return null;
	}

	/**
	 * All objects returned by this class should be released to the object pool later on.
	 * Releasing the same object multiple times causes memory leaks! Be aware of that!
	 * 
	 * @param v
	 *            vertex to be released to the pool.
	 */
	public void release(HHVertex v) {
		poolVertices.release(v);
	}

	/**
	 * All objects returned by this class should be released to the object pool later on.
	 * Releasing the same object multiple times causes memory leaks! Be aware of that!
	 * 
	 * @param e
	 *            edge to be released to the pool.
	 */
	public void release(HHEdge e) {
		poolEdges.release(e);
	}

	@Override
	public String toString() {
		return "vertex pool : " + poolVertices.numBorrowed() + "b "
				+ poolVertices.numReleased() + "r" + "\n" + "edge pool : "
				+ poolEdges.numBorrowed() + "b " + poolVertices.numReleased() + "r";
	}

}
