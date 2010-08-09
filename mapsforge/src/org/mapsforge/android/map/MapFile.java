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
package org.mapsforge.android.map;

/**
 * Stores the parameters for a contained map file in a binary map file.
 */
class MapFile {
	/**
	 * The divisor for converting coordinates stored as integers to double values.
	 */
	private static final double COORDINATES_DIVISOR = 1000000.0;

	final byte baseZoomLevel;
	final long blocksHeight;
	final long blocksWidth;
	final long boundaryBottomTile;
	final long boundaryLeftTile;
	final long boundaryRightTile;
	final long boundaryTopTile;
	final long indexStartAddress;
	final long mapFileSize;
	final long numberOfBlocks;
	final int tileEntriesTableSize;
	final byte zoomLevelMax;
	final byte zoomLevelMin;

	/**
	 * Creates a new immutable set of parameters for a MapFile.
	 * 
	 * @param indexStartAddress
	 *            the start address of the index.
	 * @param mapFileSize
	 *            the size of the map file.
	 * @param baseZoomLevel
	 *            the base zoom level of the map file.
	 * @param tileZoomLevelMin
	 *            the minimum zoom level of the map file.
	 * @param tileZoomLevelMax
	 *            the maximum zoom level of the map file.
	 * @param mapBoundary
	 *            the boundary of the map file.
	 */
	MapFile(long indexStartAddress, long mapFileSize, byte baseZoomLevel,
			byte tileZoomLevelMin, byte tileZoomLevelMax, Rect mapBoundary) {
		this.indexStartAddress = indexStartAddress;
		this.mapFileSize = mapFileSize;
		this.baseZoomLevel = baseZoomLevel;
		this.zoomLevelMin = tileZoomLevelMin;
		this.zoomLevelMax = tileZoomLevelMax;

		// calculate the XY numbers of the boundary tiles in this map file
		this.boundaryTopTile = MercatorProjection.latitudeToTileY(mapBoundary.top
				/ COORDINATES_DIVISOR, this.baseZoomLevel);
		this.boundaryLeftTile = MercatorProjection.longitudeToTileX(mapBoundary.left
				/ COORDINATES_DIVISOR, this.baseZoomLevel);
		this.boundaryBottomTile = MercatorProjection.latitudeToTileY(mapBoundary.bottom
				/ COORDINATES_DIVISOR, this.baseZoomLevel);
		this.boundaryRightTile = MercatorProjection.longitudeToTileX(mapBoundary.right
				/ COORDINATES_DIVISOR, this.baseZoomLevel);

		// calculate the horizontal and vertical amount of blocks in this map file
		this.blocksWidth = this.boundaryRightTile - this.boundaryLeftTile + 1;
		this.blocksHeight = this.boundaryBottomTile - this.boundaryTopTile + 1;

		// calculate the total amount of blocks in this map file
		this.numberOfBlocks = this.blocksWidth * this.blocksHeight;

		// calculate the size of the tile entries table
		this.tileEntriesTableSize = 2 * (this.zoomLevelMax - this.zoomLevelMin + 1) * 2;
	}
}