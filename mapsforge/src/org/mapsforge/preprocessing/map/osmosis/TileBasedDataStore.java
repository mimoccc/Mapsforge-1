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
package org.mapsforge.preprocessing.map.osmosis;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.mapsforge.core.Rect;
import org.mapsforge.preprocessing.map.osmosis.TileData.TDNode;
import org.mapsforge.preprocessing.map.osmosis.TileData.TDWay;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

/**
 * A TileBasedDataStore allows tile based access to OpenStreetMap geo data. POIs and ways are
 * mapped to tiles on configured base zoom levels.
 * 
 * @author bross
 * 
 */
interface TileBasedDataStore extends EntityResolver<TDNode> {

	static final int MAX_TILES_SUPPORTED = 1000000;

	/**
	 * Get the bounding box that describes this TileBasedDataStore.
	 * 
	 * @return The bounding box that defines the area that is covered by the data store.
	 */
	public Rect getBoundingBox();

	public TileGridLayout getTileGridLayout(int zoomIntervalIndex);

	/**
	 * Get the zoom interval configuration of the data store.
	 * 
	 * @return the underlying zoom interval configuration
	 */
	public ZoomIntervalConfiguration getZoomIntervalConfiguration();

	/**
	 * Add a node to the data store. No association with a tile is performed.
	 * 
	 * @param node
	 *            the node that is to be added to the data store
	 * @return true, if the node was successfully added, false otherwise
	 */
	public boolean addNode(Node node);

	/**
	 * Add a way to the data store. No association with a tile is performed.
	 * 
	 * @param way
	 *            the way which is to be added to the data store
	 * @return true if the way was successfully added, false otherwise
	 */
	public boolean addWay(Way way);

	/**
	 * Retrieve the all the inner ways that are associated with an outer way that represents a
	 * multipolygon.
	 * 
	 * @param outerWayID
	 *            id of the outer way
	 * @return all associated inner ways
	 */
	public List<TDWay> getInnerWaysOfMultipolygon(long outerWayID);

	/**
	 * Adds a multipolygon consisting of ways to the tile data store.
	 * 
	 * @param outerWayID
	 *            id of the outer way
	 * @param innerWayIDs
	 *            ids of all inner ways
	 * @param relationTags
	 *            all supported tags that are attached to the relation
	 * @return true if the multipolygon has been successfully added
	 */
	public boolean addWayMultipolygon(long outerWayID, long[] innerWayIDs,
			EnumSet<WayEnum> relationTags);

	/**
	 * Retrieves all the data that is associated with a tile.
	 * 
	 * @param baseZoomIndex
	 *            index of the base zoom, as defined in a ZoomIntervalConfiguration
	 * @param tileCoordinateX
	 *            x coordinate of the tile
	 * @param tileCoordinateY
	 *            y coordinate of the tile
	 * @return tile, or null if the tile is outside the bounding box of this tile data store
	 */
	public TileData getTile(int baseZoomIndex, int tileCoordinateX, int tileCoordinateY);

	/**
	 * Retrieve the total amount of tiles cumulated over all base zoom levels that is needed to
	 * represent the underlying bounding box of this tile data store.
	 * 
	 * @return total amount of tiles
	 */
	long cumulatedNumberOfTiles();

	Set<TDWay> getCoastLines(TileCoordinate tc);

	void complete();

}
