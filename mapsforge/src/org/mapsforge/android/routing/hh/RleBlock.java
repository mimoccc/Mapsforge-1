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

import java.io.IOException;

import org.mapsforge.preprocessing.routing.hhmobile.util.BitArrayInputStream;
import org.mapsforge.preprocessing.routing.hhmobile.util.BitSerializer;

final class RleBlock implements CacheItem {

	private final BitArrayInputStream stream;

	private final int bId;
	private final RleBlockReader blockReader;
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
			bpOffsBlockPrevLvl, bpOffsBlockNextLvl, bpOffsBlockZeroLvl, bpEdgeWeight;

	private final int startAddrBlockAdj;
	private final int startAddrBlockPrevLvl;
	private final int startAddrBlockNextLvl;
	private final int startAddrBlockZeroLvl;

	private final int startAddrVOffsBlockPrevLvl;
	private final int startAddrVOffsVertexPrevLvl;
	private final int startAddrVOffsBlockNextLvl;
	private final int startAddrVOffsVertexNextLvl;
	private final int startAddrVOffsBlockZeroLvl;
	private final int startAddrVOffsVertexZeroLvl;

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
	private int startAddrEExtTargetOffsBlockZeroLvl;
	private int startAddrEExtTargetOffsVertexZeroLvl;

	private int startAddrEExtWeight;
	private int startAddrEExtIsShortcut;
	private int startAddrEExtIsForward;
	private int startAddrEExtIsBackward;
	private int startAddrEExtIsCore;

	public RleBlock(byte[] b, RleBlockReader blockReader, int blockId) throws IOException {
		this.bId = blockId;
		this.data = b;
		this.blockReader = blockReader;

		this.stream = new BitArrayInputStream(data);
		this.lvl = stream.readByte();

		this.numVerticesWithNeighborhood = (short) stream.readUInt(blockReader.bpVertexCount);
		this.numVerticesHavingHigherLevel = (short) stream.readUInt(blockReader.bpVertexCount);

		this.numVertices = (short) stream.readUInt(blockReader.bpVertexCount);
		this.numEdgesInt = (short) stream.readUInt(blockReader.bpEdgeCount);
		this.numEdgesExt = (short) stream.readUInt(blockReader.bpEdgeCount);

		short numBlocksAdj = (short) stream.readUInt(blockReader.bpClusterId);
		short numBlocksSubj = (short) stream.readUInt(blockReader.bpClusterId);
		short numBlocksOverly = (short) stream.readUInt(blockReader.bpClusterId);
		short numBlocksLvlZero = (short) stream.readUInt(blockReader.bpClusterId);

		this.minLon = stream.readInt();
		this.minLat = stream.readInt();

		this.bpLon = (byte) stream.readUInt(5);
		this.bpLat = (byte) stream.readUInt(5);
		this.bpOffsEdgeInt = (byte) stream.readUInt(5);
		this.bpOffsEdgeExt = (byte) stream.readUInt(5);
		this.bpOffsBlockAdj = (byte) stream.readUInt(5);
		this.bpOffsBlockPrevLvl = (byte) stream.readUInt(5);
		this.bpOffsBlockNextLvl = (byte) stream.readUInt(5);
		this.bpOffsBlockZeroLvl = (byte) stream.readUInt(5);
		this.bpEdgeWeight = (byte) stream.readUInt(5);

		stream.alignPointer(1);

		// -------- START ADDRESSES --------
		int offset = stream.getByteOffset() * 8;

		// referenced blocks :
		this.startAddrBlockAdj = offset;
		offset += byteAlign(numBlocksAdj * blockReader.bpClusterId);
		this.startAddrBlockPrevLvl = offset;
		offset += byteAlign(numBlocksSubj * blockReader.bpClusterId);
		this.startAddrBlockNextLvl = offset;
		offset += byteAlign(numBlocksOverly * blockReader.bpClusterId);
		this.startAddrBlockZeroLvl = offset;
		offset += byteAlign(numBlocksLvlZero * blockReader.bpClusterId);

		// vertices :
		this.startAddrVOffsBlockPrevLvl = offset;
		if (lvl > 1)
			offset += byteAlign(numVertices * bpOffsBlockPrevLvl);
		this.startAddrVOffsVertexPrevLvl = offset;
		if (lvl > 1)
			offset += byteAlign(numVertices * blockReader.bpVertexCount);
		this.startAddrVOffsBlockNextLvl = offset;
		if (lvl < blockReader.numLevels - 1)
			offset += byteAlign(numVerticesHavingHigherLevel * bpOffsBlockNextLvl);
		this.startAddrVOffsVertexNextLvl = offset;
		if (lvl < blockReader.numLevels - 1)
			offset += byteAlign(numVerticesHavingHigherLevel * blockReader.bpVertexCount);
		this.startAddrVOffsBlockZeroLvl = offset;
		if (lvl > 0)
			offset += byteAlign(numVertices * bpOffsBlockZeroLvl);
		this.startAddrVOffsVertexZeroLvl = offset;
		if (lvl > 0)
			offset += byteAlign(numVertices * blockReader.bpVertexCount);
		this.startAddrVNeighborhood = offset;
		offset += byteAlign(numVerticesWithNeighborhood * blockReader.bpNeighborhood);
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
		offset += byteAlign(numEdgesInt * blockReader.bpVertexCount);
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
		offset += byteAlign(numEdgesExt * blockReader.bpVertexCount);
		this.startAddrEExtTargetOffsBlockZeroLvl = offset;
		if (lvl > 0)
			offset += byteAlign(numEdgesExt * bpOffsBlockZeroLvl);
		this.startAddrEExtTargetOffsVertexZeroLvl = offset;
		if (lvl > 0)
			offset += byteAlign(numEdgesExt * blockReader.bpVertexCount);
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

	public boolean getVertex(int i, HHVertex buff) {
		if (i > numVertices || i < 0) {
			return false;
		}
		int id = blockReader.getVertexId(bId, i);

		int idLvlZero;
		if (lvl > 0) {
			int offset = startAddrVOffsBlockZeroLvl + (bpOffsBlockZeroLvl * i);
			int _blockOffset = (int) BitSerializer.readUInt(data, bpOffsBlockZeroLvl,
					offset / 8, offset % 8);
			offset = startAddrBlockZeroLvl + (blockReader.bpClusterId * _blockOffset);
			int _blockId = (int) BitSerializer.readUInt(data, blockReader.bpClusterId,
					offset / 8, offset % 8);
			offset = startAddrVOffsVertexZeroLvl + (blockReader.bpVertexCount * i);
			int _vertexOffset = (int) BitSerializer.readUInt(data, blockReader.bpVertexCount,
					offset / 8, offset % 8);
			idLvlZero = blockReader.getVertexId(_blockId, _vertexOffset);
		} else {
			idLvlZero = id;
		}

		int idPrevLvl;
		if (lvl > 1) {
			int offset = startAddrVOffsBlockPrevLvl + (bpOffsBlockPrevLvl * i);
			int _blockOffset = (int) BitSerializer.readUInt(data, bpOffsBlockPrevLvl,
					offset / 8, offset % 8);
			offset = startAddrBlockPrevLvl + (blockReader.bpClusterId * _blockOffset);
			int _blockId = (int) BitSerializer.readUInt(data, blockReader.bpClusterId,
					offset / 8, offset % 8);
			offset = startAddrVOffsVertexPrevLvl + (blockReader.bpVertexCount * i);
			int _vertexOffset = (int) BitSerializer.readUInt(data, blockReader.bpVertexCount,
					offset / 8, offset % 8);
			idPrevLvl = blockReader.getVertexId(_blockId, _vertexOffset);
		} else if (lvl == 1) {
			idPrevLvl = idLvlZero;
		} else {
			idPrevLvl = -1;
		}

		int idNextLvl;
		if (lvl < blockReader.numLevels - 1 && i < numVerticesHavingHigherLevel) {
			int offset = startAddrVOffsBlockNextLvl + (bpOffsBlockNextLvl * i);
			int _blockOffset = (int) BitSerializer.readUInt(data, bpOffsBlockNextLvl,
					offset / 8, offset % 8);
			offset = startAddrBlockNextLvl + (blockReader.bpClusterId * _blockOffset);
			int _blockId = (int) BitSerializer.readUInt(data, blockReader.bpClusterId,
					offset / 8, offset % 8);
			offset = startAddrVOffsVertexNextLvl + (blockReader.bpVertexCount * i);
			int _vertexOffset = (int) BitSerializer.readUInt(data, blockReader.bpVertexCount,
					offset / 8, offset % 8);
			idNextLvl = blockReader.getVertexId(_blockId, _vertexOffset);
		} else {
			idNextLvl = -1;
		}

		int neighborhood;
		if (i < numVerticesWithNeighborhood) {
			int offset = startAddrVNeighborhood + (blockReader.bpNeighborhood * i);
			neighborhood = (int) BitSerializer.readUInt(data, blockReader.bpNeighborhood,
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

		buff.neighborhood = neighborhood;
		buff.id = id;
		buff.idPrevLvl = idPrevLvl;
		buff.idNextLvl = idNextLvl;
		buff.idZeroLvl = idLvlZero;
		buff.lvl = lvl;
		buff.longitude = lon;
		buff.latitude = lat;
		buff.externalEdgeStartIdx = _externalEdgeStartIdx;
		buff.numExternalEdges = (short) (_externalEdgeEndIdx - _externalEdgeStartIdx);
		buff.internalEdgeStartIdx = _internalEdgeStartIdx;
		buff.numInternalEdges = (short) (_internalEdgeEndIdx - _internalEdgeStartIdx);

		return true;
	}

	public boolean getOutboundEdge(HHVertex v, int i, HHEdge buff) {
		if (i > v.numInternalEdges + v.numExternalEdges - 1) {
			return false;
		}
		if (i < v.numInternalEdges) {
			getInternalEdge(v.internalEdgeStartIdx + i, buff);
		} else {
			getExternalEdge(v.externalEdgeStartIdx + (i - v.numInternalEdges), buff);
		}
		return true;
	}

	private void getInternalEdge(int i, HHEdge buff) {
		int offset = startAddrEIntTargetOffset + (i * blockReader.bpVertexCount);
		int _targetVertexOffset = (int) BitSerializer.readUInt(data, blockReader.bpVertexCount,
				offset / 8, offset % 8);
		buff.targetId = blockReader.getVertexId(bId, _targetVertexOffset);

		if (lvl == 0) {
			buff.targetIdZeroLvl = buff.targetId;
		} else {
			// read level zero id of internal vertex (analog to code within getVertex())
			offset = startAddrVOffsBlockZeroLvl + (bpOffsBlockZeroLvl * _targetVertexOffset);
			int _blockOffsetLvlZero = (int) BitSerializer.readUInt(data, bpOffsBlockZeroLvl,
					offset / 8, offset % 8);
			offset = startAddrBlockZeroLvl + (blockReader.bpClusterId * _blockOffsetLvlZero);
			int _blockIdLvlZero = (int) BitSerializer.readUInt(data, blockReader.bpClusterId,
					offset / 8, offset % 8);
			offset = startAddrVOffsVertexZeroLvl
					+ (blockReader.bpVertexCount * _targetVertexOffset);
			int _vertexOffsetLvlZero = (int) BitSerializer.readUInt(data,
					blockReader.bpVertexCount, offset / 8, offset % 8);
			buff.targetIdZeroLvl = blockReader.getVertexId(_blockIdLvlZero,
					_vertexOffsetLvlZero);
		}

		offset = startAddrEIntWeight + (i * bpEdgeWeight);
		buff.weight = (int) BitSerializer.readUInt(data, bpEdgeWeight, offset / 8, offset % 8);

		offset = startAddrEIntIsShortcut + i;
		buff.isShortcut = BitSerializer.readBit(data, offset / 8, offset % 8);

		offset = startAddrEIntIsForward + i;
		buff.isForward = BitSerializer.readBit(data, offset / 8, offset % 8);

		offset = startAddrEIntIsBackward + i;
		buff.isBackward = BitSerializer.readBit(data, offset / 8, offset % 8);

		offset = startAddrEIntIsCore + i;
		buff.isCore = BitSerializer.readBit(data, offset / 8, offset % 8);
	}

	private void getExternalEdge(int i, HHEdge buff) {

		int offset = startAddrEExtTargetOffsBlockAdj + (i * bpOffsBlockAdj);
		int _targetBlockOffset = (int) BitSerializer.readUInt(data, bpOffsBlockAdj, offset / 8,
				offset % 8);
		offset = startAddrBlockAdj + (_targetBlockOffset * blockReader.bpClusterId);
		int _targetBlockId = (int) BitSerializer.readUInt(data, blockReader.bpClusterId,
				offset / 8, offset % 8);
		offset = startAddrEExtTargetOffsVertexAdj + (i * blockReader.bpVertexCount);
		int _targetVertexOffset = (int) BitSerializer.readUInt(data, blockReader.bpVertexCount,
				offset / 8, offset % 8);
		buff.targetId = blockReader.getVertexId(_targetBlockId, _targetVertexOffset);

		if (lvl == 0) {
			buff.targetIdZeroLvl = buff.targetId;
		} else {
			offset = startAddrEExtTargetOffsBlockZeroLvl + (i * bpOffsBlockZeroLvl);
			int _targetBlockOffsetLvlZero = (int) BitSerializer.readUInt(data,
					bpOffsBlockZeroLvl, offset / 8, offset % 8);
			offset = startAddrBlockZeroLvl
					+ (blockReader.bpClusterId * _targetBlockOffsetLvlZero);
			int _targetBlockIdLvlZero = (int) BitSerializer.readUInt(data,
					blockReader.bpClusterId, offset / 8, offset % 8);
			offset = startAddrEExtTargetOffsVertexZeroLvl + (i * blockReader.bpVertexCount);
			int _targetVertexOffsetLvlZero = (int) BitSerializer.readUInt(data,
					blockReader.bpVertexCount, offset / 8, offset % 8);
			buff.targetIdZeroLvl = blockReader.getVertexId(_targetBlockIdLvlZero,
					_targetVertexOffsetLvlZero);
		}

		offset = startAddrEExtWeight + (i * bpEdgeWeight);
		buff.weight = (int) BitSerializer.readUInt(data, bpEdgeWeight, offset / 8, offset % 8);

		offset = startAddrEExtIsShortcut + i;
		buff.isShortcut = BitSerializer.readBit(data, offset / 8, offset % 8);

		offset = startAddrEExtIsForward + i;
		buff.isForward = BitSerializer.readBit(data, offset / 8, offset % 8);

		offset = startAddrEExtIsBackward + i;
		buff.isBackward = BitSerializer.readBit(data, offset / 8, offset % 8);

		offset = startAddrEExtIsCore + i;
		buff.isCore = BitSerializer.readBit(data, offset / 8, offset % 8);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(RleBlock.class.getName() + " (\n");
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
		sb.append("  bpOffsBlockPrevLvl = " + bpOffsBlockPrevLvl + "\n");
		sb.append("  bpOffsBlockNextLvl = " + bpOffsBlockNextLvl + "\n");
		sb.append("  bpOffsBlockZeroLvl = " + bpOffsBlockZeroLvl + "\n");
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