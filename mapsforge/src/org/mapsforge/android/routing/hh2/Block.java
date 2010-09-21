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
package org.mapsforge.android.routing.hh2;

import org.mapsforge.preprocessing.routing.hhmobile.util.BitSerializer;

/**
 *
 */
final class Block implements CacheItem {

	/**
	 * Bit offset to the first vertex of this block.
	 */
	private static final short FIRST_VERTEX_OFFSET = 17 * 8;
	/**
	 * the id of this block
	 */
	private final int blockId;
	/**
	 * The data of this block exactly like it is stored within the file.
	 */
	private final byte[] data;
	/**
	 * the routing graph this block belongs to.
	 */
	private final HHRoutingGraph routingGraph;
	/**
	 * the level of the multileveled graph this block belongs to.
	 */
	private final byte level;
	/**
	 * Number of vertices of typeA. These vertices have neighborhood and also belong to the next
	 * level.
	 */
	private final short numVerticesTypeA;
	/**
	 * Number of vertices of typeB. These vertices have neighborhood but do not belong to the
	 * next level.
	 */
	private final short numVerticesTypeB;
	/**
	 * number of vertices if tyoeC. The vertices have their neighborhood set to infinity and
	 * thus do not belong to the next level.
	 */
	private final short numVerticesTypeC;
	/**
	 * the minimum latitude in micro degrees of all coordinates stored within this block, way
	 * point coordinate and vertex coordinates.
	 */
	private final int minLatitudeE6;
	/**
	 * the minimum longitude in micro degrees of all coordinates stored within this block, way
	 * point coordinate and vertex coordinates.
	 */
	private final int minLongitudeE6;
	/**
	 * Number of bits for encoding a latitude or a longitude.
	 */
	private final byte bitsPerCoordinate;
	/**
	 * number of bits for encoding the neighborhood of the vertices.
	 */
	private final byte encBitsPerNeighborhood;
	/**
	 * Number of bits for encoding a vertex of type A.
	 */
	private final int bitsPerVertexTypeA;
	/**
	 * Number of bits for encoding a vertex of type B.
	 */
	private final int bitsPerVertexTypeB;
	/**
	 * Number of bits for encoding a vertex of type C.
	 */
	private final int bitsPerVertexTypeC;

	/**
	 * Constructs a block with the given id from the serialized representation.
	 * 
	 * @param blockId
	 *            the id of this block.
	 * @param data
	 *            the serialized representation of this block.
	 * @param routingGraph
	 *            the routing graph this block belongs to.
	 */
	public Block(int blockId, byte[] data, HHRoutingGraph routingGraph) {
		this.blockId = blockId;
		this.data = data;
		this.routingGraph = routingGraph;

		int bitOffset = 0;
		this.level = BitSerializer.readByte(data, bitOffset / 8, bitOffset % 8);
		bitOffset += 8;
		this.numVerticesTypeA = BitSerializer.readShort(data, bitOffset / 8, bitOffset % 8);
		bitOffset += 16;
		this.numVerticesTypeB = BitSerializer.readShort(data, bitOffset / 8, bitOffset % 8);
		bitOffset += 16;
		this.numVerticesTypeC = BitSerializer.readShort(data, bitOffset / 8, bitOffset % 8);
		bitOffset += 16;
		this.minLatitudeE6 = BitSerializer.readInt(data, bitOffset / 8, bitOffset % 8);
		bitOffset += 32;
		this.minLongitudeE6 = BitSerializer.readInt(data, bitOffset / 8, bitOffset % 8);
		bitOffset += 32;
		this.bitsPerCoordinate = BitSerializer.readByte(data, bitOffset / 8,
				bitOffset % 8);
		bitOffset += 8;
		this.encBitsPerNeighborhood = BitSerializer.readByte(data, bitOffset / 8,
				bitOffset % 8);
		bitOffset += 8;

		this.bitsPerVertexTypeC = ((routingGraph.bitsPerBlockId + routingGraph.bitsPerVertexOffset) * level)
				+ 32 + (level == 0 ? (2 * bitsPerCoordinate) : 0);
		this.bitsPerVertexTypeB = bitsPerVertexTypeC + encBitsPerNeighborhood;
		this.bitsPerVertexTypeA = bitsPerVertexTypeB + routingGraph.bitsPerBlockId
				+ routingGraph.bitsPerVertexOffset;
	}

	@Override
	public int getId() {
		return blockId;
	}

	@Override
	public int getSizeBytes() {
		return 36 + data.length;
	}

	/**
	 * @return Returns the level within the multileveled graph this block belongs to.
	 */
	public int getLevel() {
		return level;
	}

	/**
	 * @return Returns the number of vertices stored within this block.
	 */
	public int getNumVertices() {
		return numVerticesTypeA + numVerticesTypeB + numVerticesTypeC;
	}

	/**
	 * Look up the vertex stored at given offset.
	 * 
	 * @param vertexOffset
	 *            the offset of the vertex.
	 * @return the desired vertex.
	 */
	public HHVertex getVertex(int vertexOffset) {
		if (vertexOffset >= getNumVertices()) {
			return null;
		}
		// here, the first vertex of this block is stored.
		int bitOffset = FIRST_VERTEX_OFFSET;

		// recycle a vertex from the pool.
		HHVertex vertex = routingGraph.vertexPool.borrow();
		if (vertexOffset < numVerticesTypeA) {
			// calculate the offset of this vertex
			bitOffset += vertexOffset * bitsPerVertexTypeA;

			// read vertex id of lower levels
			vertex.vertexIds = new int[level + 2];
			for (int i = 0; i < level; i++) {
				int _blockId = (int) BitSerializer.readUInt(data, routingGraph.bitsPerBlockId,
						bitOffset / 8, bitOffset % 8);
				bitOffset += routingGraph.bitsPerBlockId;
				int _vertexOffset = (int) BitSerializer.readUInt(data,
						routingGraph.bitsPerVertexOffset, bitOffset / 8, bitOffset % 8);
				bitOffset += routingGraph.bitsPerVertexOffset;

				vertex.vertexIds[i] = routingGraph.getVertexId(_blockId, _vertexOffset);
			}

			// read vertex id of current level
			vertex.vertexIds[level] = routingGraph.getVertexId(blockId, vertexOffset);

			// read vertex id of higher level
			int _blockId = (int) BitSerializer.readUInt(data, routingGraph.bitsPerBlockId,
					bitOffset / 8, bitOffset % 8);
			bitOffset += routingGraph.bitsPerBlockId;
			int _vertexOffset = (int) BitSerializer.readUInt(data,
					routingGraph.bitsPerVertexOffset, bitOffset / 8, bitOffset % 8);
			bitOffset += routingGraph.bitsPerVertexOffset;

			vertex.vertexIds[level + 1] = routingGraph.getVertexId(_blockId, _vertexOffset);

			// read neighborhood
			vertex.neighborhood = (int) BitSerializer.readUInt(data, encBitsPerNeighborhood,
					bitOffset / 8, bitOffset % 8);
			bitOffset += encBitsPerNeighborhood;

			// read Bit-offset of first outbound edge
			vertex.bitOffsetFirstOutboundEdge = BitSerializer.readInt(data, bitOffset / 8,
					bitOffset % 8);
			bitOffset += 32;

			// read coordinate
			vertex.latitudeE6 = -1;
			vertex.longitudeE6 = -1;
			if (level == 0) {
				vertex.latitudeE6 = (int) BitSerializer.readUInt(data, bitsPerCoordinate,
						bitOffset / 8, bitOffset % 8)
						+ minLatitudeE6;
				bitOffset += bitsPerCoordinate;
				vertex.longitudeE6 = (int) BitSerializer.readUInt(data, bitsPerCoordinate,
						bitOffset / 8, bitOffset % 8)
						+ minLongitudeE6;
				bitOffset += bitsPerCoordinate;
			}
		} else if (vertexOffset < numVerticesTypeA + numVerticesTypeB) {
			// read vertex typeB

			// calculate the offset of this vertex
			bitOffset += (numVerticesTypeA * bitsPerVertexTypeA)
					+ ((vertexOffset - numVerticesTypeA) * bitsPerVertexTypeB);

			// read vertex id of lower levels
			vertex.vertexIds = new int[level + 2];
			for (int i = 0; i < level; i++) {
				int _blockId = (int) BitSerializer.readUInt(data, routingGraph.bitsPerBlockId,
						bitOffset / 8, bitOffset % 8);
				bitOffset += routingGraph.bitsPerBlockId;
				int _vertexOffset = (int) BitSerializer.readUInt(data,
						routingGraph.bitsPerVertexOffset, bitOffset / 8, bitOffset % 8);
				bitOffset += routingGraph.bitsPerVertexOffset;

				vertex.vertexIds[i] = routingGraph.getVertexId(_blockId, _vertexOffset);
			}

			// read vertex id of current level
			vertex.vertexIds[level] = routingGraph.getVertexId(blockId, vertexOffset);

			// read vertex id of higher level
			vertex.vertexIds[level + 1] = -1;

			// read neighborhood
			vertex.neighborhood = (int) BitSerializer.readUInt(data, encBitsPerNeighborhood,
					bitOffset / 8, bitOffset % 8);
			bitOffset += encBitsPerNeighborhood;

			// read Bit-offset of first outbound edge
			vertex.bitOffsetFirstOutboundEdge = BitSerializer.readInt(data, bitOffset / 8,
					bitOffset % 8);
			bitOffset += 32;

			// read coordinate
			vertex.latitudeE6 = -1;
			vertex.longitudeE6 = -1;
			if (level == 0) {
				vertex.latitudeE6 = (int) BitSerializer.readUInt(data, bitsPerCoordinate,
						bitOffset / 8, bitOffset % 8)
						+ minLatitudeE6;
				bitOffset += bitsPerCoordinate;
				vertex.longitudeE6 = (int) BitSerializer.readUInt(data, bitsPerCoordinate,
						bitOffset / 8, bitOffset % 8)
						+ minLongitudeE6;
				bitOffset += bitsPerCoordinate;
			}
		} else {
			// read vertex typeC

			// calculate the offset of this vertex
			bitOffset += (numVerticesTypeA * bitsPerVertexTypeA)
					+ (numVerticesTypeB * bitsPerVertexTypeB)
					+ ((vertexOffset - numVerticesTypeA - numVerticesTypeB) * bitsPerVertexTypeC);

			// read vertex id of lower levels
			vertex.vertexIds = new int[level + 2];
			for (int i = 0; i < level; i++) {
				int _blockId = (int) BitSerializer.readUInt(data, routingGraph.bitsPerBlockId,
						bitOffset / 8, bitOffset % 8);
				bitOffset += routingGraph.bitsPerBlockId;
				int _vertexOffset = (int) BitSerializer.readUInt(data,
						routingGraph.bitsPerVertexOffset, bitOffset / 8, bitOffset % 8);
				bitOffset += routingGraph.bitsPerVertexOffset;

				vertex.vertexIds[i] = routingGraph.getVertexId(_blockId, _vertexOffset);
			}

			// read vertex id of current level
			vertex.vertexIds[level] = routingGraph.getVertexId(blockId, vertexOffset);

			// read vertex id of higher level
			vertex.vertexIds[level + 1] = -1;

			// read neighborhood
			vertex.neighborhood = Integer.MAX_VALUE;

			// read Bit-offset of first outbound edge
			vertex.bitOffsetFirstOutboundEdge = BitSerializer.readInt(data, bitOffset / 8,
					bitOffset % 8);
			bitOffset += 32;

			// read coordinate
			vertex.latitudeE6 = -1;
			vertex.longitudeE6 = -1;
			if (level == 0) {
				vertex.latitudeE6 = (int) BitSerializer.readUInt(data, bitsPerCoordinate,
						bitOffset / 8, bitOffset % 8)
						+ minLatitudeE6;
				bitOffset += bitsPerCoordinate;
				vertex.longitudeE6 = (int) BitSerializer.readUInt(data, bitsPerCoordinate,
						bitOffset / 8, bitOffset % 8)
						+ minLongitudeE6;
				bitOffset += bitsPerCoordinate;
			}
		}
		return vertex;
	}

	/**
	 * Looks up the outgoing adjacency list of the given vertex.
	 * 
	 * @param vertex
	 *            the source vertex.
	 * @return all outgoing edges of the given vertex.
	 */
	public HHEdge[] getOutboundEdges(HHVertex vertex) {
		int bitOffset = vertex.bitOffsetFirstOutboundEdge;

		// read number of edges in adjacency list
		int numEdges = (int) BitSerializer.readUInt(data, 4, bitOffset / 8, bitOffset % 8);
		bitOffset += 4;

		if (numEdges == 15) {
			numEdges = (int) BitSerializer.readUInt(data, 24, bitOffset / 8, bitOffset % 8);
			bitOffset += 24;
		}

		HHEdge[] edges = new HHEdge[numEdges];
		for (int i = 0; i < edges.length; i++) {
			// recycle edge from pool
			HHEdge edge = routingGraph.edgePool.borrow();

			// set source id
			edge.sourceId = vertex.vertexIds[vertex.vertexIds.length - 2];

			// set weight
			edge.weight = (int) BitSerializer.readUInt(data, routingGraph.bitsPerEdgeWeight,
					bitOffset / 8, bitOffset % 8);
			bitOffset += routingGraph.bitsPerEdgeWeight;

			// set target id
			int _blockId = (int) BitSerializer.readUInt(data, routingGraph.bitsPerBlockId,
					bitOffset / 8, bitOffset % 8);
			bitOffset += routingGraph.bitsPerBlockId;
			int _vertexOffset = (int) BitSerializer.readUInt(data,
					routingGraph.bitsPerVertexOffset, bitOffset / 8, bitOffset % 8);
			bitOffset += routingGraph.bitsPerVertexOffset;
			edge.targetId = routingGraph.getVertexId(_blockId, _vertexOffset);

			// set forward
			edge.isForward = BitSerializer.readBit(data, bitOffset / 8, bitOffset % 8);
			bitOffset += 1;

			// set backward
			edge.isBackward = BitSerializer.readBit(data, bitOffset / 8, bitOffset % 8);
			bitOffset += 1;

			// set core
			edge.isCore = BitSerializer.readBit(data, bitOffset / 8, bitOffset % 8);
			bitOffset += 1;

			// clear satellite data
			edge.isMotorwayLink = false;
			edge.isRoundAbout = false;
			edge.name = null;
			edge.ref = null;
			edge.waypoints = null;
			// set satellite data (only for level-0 forward edges)
			if (level == 0 && edge.isForward) {
				// set motor-way link
				edge.isMotorwayLink = BitSerializer.readBit(data, bitOffset / 8,
						bitOffset % 8);
				bitOffset += 1;

				// set roundabout
				edge.isRoundAbout = BitSerializer
						.readBit(data, bitOffset / 8, bitOffset % 8);
				bitOffset += 1;

				// set name
				byte nameLen = BitSerializer.readByte(data, bitOffset / 8, bitOffset % 8);
				bitOffset += 8;
				if (nameLen > 0) {
					edge.name = new byte[nameLen];
					for (int j = 0; j < edge.name.length; j++) {
						edge.name[j] = BitSerializer.readByte(data, bitOffset / 8,
								bitOffset % 8);
						bitOffset += 8;
					}
				}

				// set ref
				byte refLen = BitSerializer.readByte(data, bitOffset / 8, bitOffset % 8);
				bitOffset += 8;
				if (refLen > 0) {
					edge.ref = new byte[refLen];
					for (int j = 0; j < edge.ref.length; j++) {
						edge.ref[j] = BitSerializer
								.readByte(data, bitOffset / 8, bitOffset % 8);
						bitOffset += 8;
					}
				}

				// set waypoints
				int numWaypoints = (int) BitSerializer.readUInt(data, 4, bitOffset / 8,
						bitOffset % 8);
				bitOffset += 4;
				if (numWaypoints == 15) {
					numWaypoints = (int) BitSerializer.readUInt(data, 16, bitOffset / 8,
							bitOffset % 8);
					bitOffset += 16;
				}
				edge.waypoints = new int[numWaypoints * 2];
				for (int j = 0; j < numWaypoints; j++) {
					edge.waypoints[j * 2] = minLatitudeE6
							+ (int) BitSerializer.readUInt(data, bitsPerCoordinate,
							bitOffset / 8,
							bitOffset % 8);
					bitOffset += bitsPerCoordinate;
					edge.waypoints[(j * 2) + 1] = minLongitudeE6
							+ (int) BitSerializer.readUInt(data, bitsPerCoordinate,
							bitOffset / 8,
							bitOffset % 8);
					bitOffset += bitsPerCoordinate;
				}
			}

			// set minLevel
			edge.minLevel = 0;
			if (level > 0) {
				edge.minLevel = BitSerializer.readByte(data, bitOffset / 8, bitOffset % 8);
				bitOffset += 8;
			}

			// set hop indices if this edge is a shortcut
			edge.hopIndices = null;
			if (edge.minLevel > 0 && routingGraph.hasShortcutHopIndices) {
				int numHopIndices = (int) BitSerializer.readUInt(data, 5, bitOffset / 8,
						bitOffset % 8);
				bitOffset += 5;
				edge.hopIndices = new int[numHopIndices];
				for (int j = 0; j < numHopIndices; j++) {
					edge.hopIndices[j] = (int) BitSerializer.readUInt(data, 4, bitOffset / 8,
							bitOffset % 8);
					bitOffset += 4;
					if (edge.hopIndices[j] == 15) {
						edge.hopIndices[j] = (int) BitSerializer.readUInt(data, 24,
								bitOffset / 8,
								bitOffset % 8);
						bitOffset += 24;
					}
				}
			}
			edges[i] = edge;
		}
		return edges;
	}
}
