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
package org.mapsforge.preprocessing.routing.hhmobile.testImpl.routingGraph;

import java.io.IOException;

import org.mapsforge.preprocessing.routing.hhmobile.binaryFile.graph.BlockedGraphHeader;
import org.mapsforge.preprocessing.routing.hhmobile.util.BitArrayInputStream;
import org.mapsforge.preprocessing.routing.hhmobile.util.BitSerializer;

class Block implements CacheItem {

	private final BitArrayInputStream stream;

	private final int bId;
	private final BlockedGraphHeader graphHeader;
	private final byte[] data;

	private final byte lvl;

	private final short numVerticesWithNeighborhood;
	private final short numVerticesHavingHigherLevel;

	private final short numVertices;
	private final short numEdgesInt;
	private final short numEdgesExt;

	private final int minLon;
	private final int minLat;

	private final byte bpLon, bpLat, bpOffsEdgeInt, bpOffsEdgeExt, bpOffsBlockAdj,
			bpOffsBlockSubj, bpOffsBlockOverly, bpOffsBlockLvlZero, bpEdgeWeight;

	private final int startAddrBlockAdj;
	private final int startAddrBlockSubj;
	private final int startAddrBlockOverly;
	private final int startAddrBlockLevelZero;

	private final int startAddrVOffsBlockSubj;
	private final int startAddrVOffsVertexSubj;
	private final int startAddrVOffsBlockOverly;
	private final int startAddrVOffsVertexOverly;
	private final int startAddrVOffsBlockLvlZero;
	private final int startAddrVOffsVertexLvlZero;

	private final int startAddrVNeighborhood;
	private final int startAddrVLongitude;
	private final int startAddrVLatitude;
	private final int startAddrVOffsIntEdge;
	private final int startAddrVOffsExtEdge;

	private final int startAddrEIntTargetOffset;
	private final int startAddrEIntWeight;
	private final int startAddrEIntIsShortcut;
	private final int startAddrEIntIsForward;
	private final int startAddrEIntIsBackward;
	private final int startAddrEIntIsCore;

	private int startAddrEExtTargetOffsBlockAdj;
	private int startAddrEExtTargetOffsVertexAdj;
	private int startAddrEExtTargetOffsBlockLvlZero;
	private int startAddrEExtTargetOffsVertexLvlZero;

	private int startAddrEExtWeight;
	private int startAddrEExtIsShortcut;
	private int startAddrEExtIsForward;
	private int startAddrEExtIsBackward;
	private int startAddrEExtIsCore;

	public Block(byte[] b, BlockedGraphHeader graphHeader, int blockId) throws IOException {
		this.bId = blockId;
		this.data = b;
		this.graphHeader = graphHeader;

		this.stream = new BitArrayInputStream(data);
		this.lvl = stream.readByte();

		this.numVerticesWithNeighborhood = (short) stream.readUInt(graphHeader.bpVertexCount);
		this.numVerticesHavingHigherLevel = (short) stream.readUInt(graphHeader.bpVertexCount);

		this.numVertices = (short) stream.readUInt(graphHeader.bpVertexCount);
		this.numEdgesInt = (short) stream.readUInt(graphHeader.bpEdgeCount);
		this.numEdgesExt = (short) stream.readUInt(graphHeader.bpEdgeCount);

		short numBlocksAdj = (short) stream.readUInt(graphHeader.bpClusterId);
		short numBlocksSubj = (short) stream.readUInt(graphHeader.bpClusterId);
		short numBlocksOverly = (short) stream.readUInt(graphHeader.bpClusterId);
		short numBlocksLvlZero = (short) stream.readUInt(graphHeader.bpClusterId);

		// System.out.println("numBlocksAdj = " + numBlocksAdj);
		// System.out.println("numBlocksSubj = " + numBlocksSubj);
		// System.out.println("numBlocksOverly = " + numBlocksOverly);
		// System.out.println("numBlocksLvlZero = " + numBlocksLvlZero);

		this.minLon = stream.readInt();
		this.minLat = stream.readInt();

		this.bpLon = (byte) stream.readUInt(5);
		this.bpLat = (byte) stream.readUInt(5);
		this.bpOffsEdgeInt = (byte) stream.readUInt(5);
		this.bpOffsEdgeExt = (byte) stream.readUInt(5);
		this.bpOffsBlockAdj = (byte) stream.readUInt(5);
		this.bpOffsBlockSubj = (byte) stream.readUInt(5);
		this.bpOffsBlockOverly = (byte) stream.readUInt(5);
		this.bpOffsBlockLvlZero = (byte) stream.readUInt(5);
		this.bpEdgeWeight = (byte) stream.readUInt(5);

		stream.alignPointer(1);

		// -------- START ADDRESSES --------
		int offset = stream.getByteOffset() * 8;

		// referenced blocks :
		this.startAddrBlockAdj = offset;
		offset += byteAlign(numBlocksAdj * graphHeader.bpClusterId);
		this.startAddrBlockSubj = offset;
		offset += byteAlign(numBlocksSubj * graphHeader.bpClusterId);
		this.startAddrBlockOverly = offset;
		offset += byteAlign(numBlocksOverly * graphHeader.bpClusterId);
		this.startAddrBlockLevelZero = offset;
		offset += byteAlign(numBlocksLvlZero * graphHeader.bpClusterId);

		// vertices :
		this.startAddrVOffsBlockSubj = offset;
		if (lvl > 1)
			offset += byteAlign(numVertices * bpOffsBlockSubj);
		this.startAddrVOffsVertexSubj = offset;
		if (lvl > 1)
			offset += byteAlign(numVertices * graphHeader.bpVertexCount);
		this.startAddrVOffsBlockOverly = offset;
		if (lvl < graphHeader.numLevels - 1)
			offset += byteAlign(numVerticesHavingHigherLevel * bpOffsBlockOverly);
		this.startAddrVOffsVertexOverly = offset;
		if (lvl < graphHeader.numLevels - 1)
			offset += byteAlign(numVerticesHavingHigherLevel * graphHeader.bpVertexCount);
		this.startAddrVOffsBlockLvlZero = offset;
		if (lvl > 0)
			offset += byteAlign(numVertices * bpOffsBlockLvlZero);
		this.startAddrVOffsVertexLvlZero = offset;
		if (lvl > 0)
			offset += byteAlign(numVertices * graphHeader.bpVertexCount);
		this.startAddrVNeighborhood = offset;
		offset += byteAlign(numVerticesWithNeighborhood * graphHeader.bpNeighborhood);
		this.startAddrVLongitude = offset;
		if (lvl == 0)
			offset += byteAlign(numVertices * bpLon);
		this.startAddrVLatitude = offset;
		if (lvl == 0)
			offset += byteAlign(numVertices * bpLat);
		this.startAddrVOffsIntEdge = offset;
		offset += byteAlign((numVertices + 1) * bpOffsEdgeInt);
		this.startAddrVOffsExtEdge = offset;
		offset += byteAlign((numVertices + 1) * bpOffsEdgeExt);

		// internal edges :
		this.startAddrEIntTargetOffset = offset;
		offset += byteAlign(numEdgesInt * graphHeader.bpVertexCount);
		this.startAddrEIntWeight = offset;
		offset += byteAlign(numEdgesInt * bpEdgeWeight);
		this.startAddrEIntIsShortcut = offset;
		offset += byteAlign(numEdgesInt);
		this.startAddrEIntIsForward = offset;
		offset += byteAlign(numEdgesInt);
		this.startAddrEIntIsBackward = offset;
		offset += byteAlign(numEdgesInt);
		this.startAddrEIntIsCore = offset;
		offset += byteAlign(numEdgesInt);

		// external edges :
		this.startAddrEExtTargetOffsBlockAdj = offset;
		offset += byteAlign(numEdgesExt * bpOffsBlockAdj);
		this.startAddrEExtTargetOffsVertexAdj = offset;
		offset += byteAlign(numEdgesExt * graphHeader.bpVertexCount);
		this.startAddrEExtTargetOffsBlockLvlZero = offset;
		if (lvl > 0)
			offset += byteAlign(numEdgesExt * bpOffsBlockLvlZero);
		this.startAddrEExtTargetOffsVertexLvlZero = offset;
		if (lvl > 0)
			offset += byteAlign(numEdgesExt * graphHeader.bpVertexCount);
		this.startAddrEExtWeight = offset;
		offset += byteAlign(numEdgesExt * bpEdgeWeight);
		this.startAddrEExtIsShortcut = offset;
		offset += byteAlign(numEdgesExt);
		this.startAddrEExtIsForward = offset;
		offset += byteAlign(numEdgesExt);
		this.startAddrEExtIsBackward = offset;
		offset += byteAlign(numEdgesExt);
		this.startAddrEExtIsCore = offset;
		offset += byteAlign(numEdgesExt);
	}

	@Override
	public int getId() {
		return bId;
	}

	@Override
	public int getSizeBytes() {
		return data.length;
	}

	public int getLevel() {
		return lvl;
	}

	public int getNumVertices() {
		return numVertices;
	}

	public int getNumEdges() {
		return numEdgesExt + numEdgesInt;
	}

	public Vertex getVertex(int i) {
		int id = getVertexId(bId, i);

		int idLvlZero;
		if (lvl > 0) {
			int offset = startAddrVOffsBlockLvlZero + (bpOffsBlockLvlZero * i);
			int _blockOffset = (int) BitSerializer.readUInt(data, bpOffsBlockLvlZero,
					offset / 8, offset % 8);
			offset = startAddrBlockLevelZero + (graphHeader.bpClusterId * _blockOffset);
			int _blockId = (int) BitSerializer.readUInt(data, graphHeader.bpClusterId,
					offset / 8, offset % 8);
			offset = startAddrVOffsVertexLvlZero + (graphHeader.bpVertexCount * i);
			int _vertexOffset = (int) BitSerializer.readUInt(data, graphHeader.bpVertexCount,
					offset / 8, offset % 8);
			idLvlZero = getVertexId(_blockId, _vertexOffset);
		} else {
			idLvlZero = id;
		}

		int idSubj;
		if (lvl > 1) {
			int offset = startAddrVOffsBlockSubj + (bpOffsBlockSubj * i);
			int _blockOffset = (int) BitSerializer.readUInt(data, bpOffsBlockSubj, offset / 8,
					offset % 8);
			offset = startAddrBlockSubj + (graphHeader.bpClusterId * _blockOffset);
			int _blockId = (int) BitSerializer.readUInt(data, graphHeader.bpClusterId,
					offset / 8, offset % 8);
			offset = startAddrVOffsVertexSubj + (graphHeader.bpVertexCount * i);
			int _vertexOffset = (int) BitSerializer.readUInt(data, graphHeader.bpVertexCount,
					offset / 8, offset % 8);
			idSubj = getVertexId(_blockId, _vertexOffset);
		} else if (lvl == 1) {
			idSubj = idLvlZero;
		} else {
			idSubj = -1;
		}

		int idOverly;
		if (lvl < graphHeader.numLevels - 1 && i < numVerticesHavingHigherLevel) {
			int offset = startAddrVOffsBlockOverly + (bpOffsBlockOverly * i);
			int _blockOffset = (int) BitSerializer.readUInt(data, bpOffsBlockOverly,
					offset / 8, offset % 8);
			offset = startAddrBlockOverly + (graphHeader.bpClusterId * _blockOffset);
			int _blockId = (int) BitSerializer.readUInt(data, graphHeader.bpClusterId,
					offset / 8, offset % 8);
			offset = startAddrVOffsVertexOverly + (graphHeader.bpVertexCount * i);
			int _vertexOffset = (int) BitSerializer.readUInt(data, graphHeader.bpVertexCount,
					offset / 8, offset % 8);
			idOverly = getVertexId(_blockId, _vertexOffset);
		} else {
			idOverly = -1;
		}

		int neighborhood;
		if (i < numVerticesWithNeighborhood) {
			int offset = startAddrVNeighborhood + (graphHeader.bpNeighborhood * i);
			neighborhood = (int) BitSerializer.readUInt(data, graphHeader.bpNeighborhood,
					offset / 8, offset % 8);
		} else {
			neighborhood = Integer.MAX_VALUE;
		}

		int lon;
		int lat;
		if (lvl == 0) {
			int offset = startAddrVLongitude + (bpLon * i);
			lon = (int) BitSerializer.readUInt(data, bpLon, offset / 8, offset % 8) + minLon;
			offset = startAddrVLatitude + (bpLat * i);
			lat = (int) BitSerializer.readUInt(data, bpLat, offset / 8, offset % 8) + minLat;
		} else {
			lat = -1;
			lon = -1;
		}

		int _internalEdgeStartIdx;
		int offset = startAddrVOffsIntEdge + (i * bpOffsEdgeInt);
		_internalEdgeStartIdx = (int) BitSerializer.readUInt(data, bpOffsEdgeInt, offset / 8,
				offset % 8);

		int _internalEdgeEndIdx;
		offset = startAddrVOffsIntEdge + ((i + 1) * bpOffsEdgeInt);
		_internalEdgeEndIdx = (int) BitSerializer.readUInt(data, bpOffsEdgeInt, offset / 8,
				offset % 8);

		int _externalEdgeStartIdx;
		offset = startAddrVOffsExtEdge + (i * bpOffsEdgeExt);
		_externalEdgeStartIdx = (int) BitSerializer.readUInt(data, bpOffsEdgeExt, offset / 8,
				offset % 8);

		int _externalEdgeEndIdx;
		offset = startAddrVOffsExtEdge + ((i + 1) * bpOffsEdgeExt);
		_externalEdgeEndIdx = (int) BitSerializer.readUInt(data, bpOffsEdgeExt, offset / 8,
				offset % 8);

		int _numEdges = (_internalEdgeEndIdx - _internalEdgeStartIdx)
				+ (_externalEdgeEndIdx - _externalEdgeStartIdx);
		Edge[] outboundEdges = new Edge[_numEdges];
		{
			int j = 0;
			for (int k = _internalEdgeStartIdx; k < _internalEdgeEndIdx; k++) {
				outboundEdges[j++] = getInternalEdge(k);
			}

			for (int k = _externalEdgeStartIdx; k < _externalEdgeEndIdx; k++) {
				outboundEdges[j++] = getExternalEdge(k);
			}
		}
		return new Vertex(neighborhood, id, idSubj, idOverly, idLvlZero, lvl, lon, lat,
				outboundEdges);
	}

	private Edge getInternalEdge(int i) {

		int targetId;
		int offset = startAddrEIntTargetOffset + (i * graphHeader.bpVertexCount);
		int _targetVertexOffset = (int) BitSerializer.readUInt(data, graphHeader.bpVertexCount,
				offset / 8, offset % 8);
		targetId = getVertexId(bId, _targetVertexOffset);

		int targetIdLvlZero;
		if (lvl == 0) {
			targetIdLvlZero = targetId;
		} else {
			// read level zero id of internal vertex (analog to code within getVertex())
			offset = startAddrVOffsBlockLvlZero + (bpOffsBlockLvlZero * _targetVertexOffset);
			int _blockOffsetLvlZero = (int) BitSerializer.readUInt(data, bpOffsBlockLvlZero,
					offset / 8, offset % 8);
			offset = startAddrBlockLevelZero + (graphHeader.bpClusterId * _blockOffsetLvlZero);
			int _blockIdLvlZero = (int) BitSerializer.readUInt(data, graphHeader.bpClusterId,
					offset / 8, offset % 8);
			offset = startAddrVOffsVertexLvlZero
					+ (graphHeader.bpVertexCount * _targetVertexOffset);
			int _vertexOffsetLvlZero = (int) BitSerializer.readUInt(data,
					graphHeader.bpVertexCount, offset / 8, offset % 8);
			targetIdLvlZero = getVertexId(_blockIdLvlZero, _vertexOffsetLvlZero);
		}

		int weight;
		offset = startAddrEIntWeight + (i * bpEdgeWeight);
		weight = (int) BitSerializer.readUInt(data, bpEdgeWeight, offset / 8, offset % 8);

		boolean isShortcut;
		offset = startAddrEIntIsShortcut + i;
		isShortcut = BitSerializer.readBit(data, offset / 8, offset % 8);

		boolean isForward;
		offset = startAddrEIntIsForward + i;
		isForward = BitSerializer.readBit(data, offset / 8, offset % 8);

		boolean isBackward;
		offset = startAddrEIntIsBackward + i;
		isBackward = BitSerializer.readBit(data, offset / 8, offset % 8);

		boolean isCore;
		offset = startAddrEIntIsCore + i;
		isCore = BitSerializer.readBit(data, offset / 8, offset % 8);

		return new Edge(targetId, targetIdLvlZero, weight, isShortcut, isForward, isBackward,
				isCore);
	}

	private Edge getExternalEdge(int i) {

		int targetId;
		int offset = startAddrEExtTargetOffsBlockAdj + (i * bpOffsBlockAdj);
		int _targetBlockOffset = (int) BitSerializer.readUInt(data, bpOffsBlockAdj, offset / 8,
				offset % 8);
		offset = startAddrBlockAdj + (_targetBlockOffset * graphHeader.bpClusterId);
		int _targetBlockId = (int) BitSerializer.readUInt(data, graphHeader.bpClusterId,
				offset / 8, offset % 8);
		offset = startAddrEExtTargetOffsVertexAdj + (i * graphHeader.bpVertexCount);
		int _targetVertexOffset = (int) BitSerializer.readUInt(data, graphHeader.bpVertexCount,
				offset / 8, offset % 8);
		targetId = getVertexId(_targetBlockId, _targetVertexOffset);

		int targetIdLvlZero;
		if (lvl == 0) {
			targetIdLvlZero = targetId;
		} else {
			offset = startAddrEExtTargetOffsBlockLvlZero + (i * bpOffsBlockLvlZero);
			int _targetBlockOffsetLvlZero = (int) BitSerializer.readUInt(data,
					bpOffsBlockLvlZero, offset / 8, offset % 8);
			offset = startAddrBlockLevelZero
					+ (graphHeader.bpClusterId * _targetBlockOffsetLvlZero);
			int _targetBlockIdLvlZero = (int) BitSerializer.readUInt(data,
					graphHeader.bpClusterId, offset / 8, offset % 8);
			offset = startAddrEExtTargetOffsVertexLvlZero + (i * graphHeader.bpVertexCount);
			int _targetVertexOffsetLvlZero = (int) BitSerializer.readUInt(data,
					graphHeader.bpVertexCount, offset / 8, offset % 8);
			targetIdLvlZero = getVertexId(_targetBlockIdLvlZero, _targetVertexOffsetLvlZero);
		}

		int weight;
		offset = startAddrEExtWeight + (i * bpEdgeWeight);
		weight = (int) BitSerializer.readUInt(data, bpEdgeWeight, offset / 8, offset % 8);

		boolean isShortcut;
		offset = startAddrEExtIsShortcut + i;
		isShortcut = BitSerializer.readBit(data, offset / 8, offset % 8);

		boolean isForward;
		offset = startAddrEExtIsForward + i;
		isForward = BitSerializer.readBit(data, offset / 8, offset % 8);

		boolean isBackward;
		offset = startAddrEExtIsBackward + i;
		isBackward = BitSerializer.readBit(data, offset / 8, offset % 8);

		boolean isCore;
		offset = startAddrEExtIsCore + i;
		isCore = BitSerializer.readBit(data, offset / 8, offset % 8);

		return new Edge(targetId, targetIdLvlZero, weight, isShortcut, isForward, isBackward,
				isCore);
	}

	private int getVertexId(int blockId, int vertexOffset) {
		return (blockId << graphHeader.bpVertexCount) | vertexOffset;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(Block.class.getName() + " (\n");
		sb.append("  lvl = " + lvl + "\n");
		sb.append("  blockId = " + bId + "\n");

		sb.append("  numVerticesWithNeighborhood = " + numVerticesWithNeighborhood + "\n");
		sb.append("  numVerticesHavingHigherLevel = " + numVerticesHavingHigherLevel + "\n");

		sb.append("  numVertices = " + numVertices + "\n");
		sb.append("  numEdgesInt = " + numEdgesInt + "\n");
		sb.append("  numEdgesExt = " + numEdgesExt + "\n");

		sb.append("  minLon = " + minLon + "\n");
		sb.append("  minLat = " + minLat + "\n");

		sb.append("  bpLon = " + bpLon + "\n");
		sb.append("  bpLat = " + bpLat + "\n");
		sb.append("  bpOffsEdgeInt = " + bpOffsEdgeInt + "\n");
		sb.append("  bpOffsEdgeExt = " + bpOffsEdgeExt + "\n");
		sb.append("  bpOffsBlockAdj = " + bpOffsBlockAdj + "\n");
		sb.append("  bpOffsBlockSubj = " + bpOffsBlockSubj + "\n");
		sb.append("  bpOffsBlockOverly = " + bpOffsBlockOverly + "\n");
		sb.append("  bpOffsBlockLvlZero = " + bpOffsBlockLvlZero + "\n");
		sb.append("  bpEdgeWeight = " + bpEdgeWeight + "\n");
		sb.append(")");

		return sb.toString();
	}

	private int byteAlign(int bitOffset) {
		if (bitOffset % 8 != 0) {
			return bitOffset + (8 - (bitOffset % 8));
		}
		return bitOffset;
	}

}
