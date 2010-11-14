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

import java.util.List;

import org.mapsforge.core.Rect;
import org.mapsforge.preprocessing.map.osmosis.TileData.TDNode;
import org.mapsforge.preprocessing.map.osmosis.TileData.TDWay;

/**
 * A TileBasedDataStore allows tile based access to OpenStreetMap geo data. POIs and ways are
 * mapped to tiles on configured base zoom levels.
 * 
 * @author bross
 * 
 */
interface TileBasedDataStore {

	static final int MAX_TILES_SUPPORTED = 1000000;

	/**
	 * Get the bounding box that describes this TileBasedDataStore.
	 * 
	 * @return The bounding box that defines the area that is covered by the data store.
	 */
	public Rect getBoundingBox();

	/**
	 * Get the TileCoordinate of the upper left tile in the bounding box of the data store on a
	 * given base zoom level.
	 * 
	 * @param zoomIntervalIndex
	 *            the index of the zoom interval that defines the base zoom level
	 * @return The upper left tile
	 */
	public TileCoordinate getUpperLeft(int zoomIntervalIndex);

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
	public boolean addNode(TDNode node);

	/**
	 * Retrieve the node with the given id from the data store.
	 * 
	 * @param id
	 *            the id of the node
	 * @return the node if existent in the data store, null otherwise
	 */
	public TDNode getNode(long id);

	/**
	 * Checks if the node with the given id exists in the data store.
	 * 
	 * @param id
	 *            the id of the node
	 * @return true, if the node exists, false otherwise
	 */
	public boolean containsNode(long id);

	/**
	 * Get the total amount of distinct nodes that exist in the data store.
	 * 
	 * @return total amount of distinct nodes in this data store
	 */
	public int numberOfNodes();

	/**
	 * Add a way to the data store. No association with a tile is performed.
	 * 
	 * @param way
	 *            the way which is to be added to the data store
	 * @return true if the way was successfully added, false otherwise
	 */
	public boolean addWay(TDWay way);

	/**
	 * Retrieve the way from the data store with the given id.
	 * 
	 * @param id
	 *            the id of the way
	 * @return the way if existent in the data store, null otherwise
	 */
	public TDWay getWay(long id);

	public List<TDWay> getInnerWaysOfMultipolygon(long outerWayID);

	public boolean addPOI(TDNode poi);

	public boolean addMultipolygon(long outerWayID, long[] innerWayIDs);

	public TileData getTile(int baseZoomIndex, long tileCoordinateX, long tileCoordinateY);

	public int numberOfHorizontalTiles(int zoomIntervalIndex);

	public int numberOfVerticalTiles(int zoomIntervalIndex);

	long cumulatedNumberOfTiles();

}
