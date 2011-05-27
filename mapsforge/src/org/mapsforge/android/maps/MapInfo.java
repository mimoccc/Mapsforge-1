/*
 * Copyright 2010, 2011 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.android.maps;

import android.graphics.Rect;

/**
 * 
 * @author Karsten Groll
 * 
 *         Helper class for retrieving metadata from a map, for example its bounding box and its base
 *         zoom level.
 */
public class MapInfo {
	private Rect mapBoundary;
	private GeoPoint mapCenter;
	private long mapDate;
	private GeoPoint startPosition;
	private boolean isDebugFile;
	private String projection;

	/**
	 * 
	 * @param fileName
	 *            Absolute path of the map's file whose meta data should be retrieved.
	 */
	public MapInfo(String fileName) {
		MapDatabase mapDB;
		boolean fileOpenSuccessful = false;
		mapDB = new MapDatabase();

		// Error handling is done by MapDatabase.
		fileOpenSuccessful = mapDB.openFile(fileName);

		// Retrieve the map's properties
		this.mapBoundary = mapDB.getMapBoundary();
		this.mapCenter = mapDB.getMapCenter();
		this.mapDate = mapDB.getMapDate();
		this.startPosition = mapDB.getStartPosition();
		this.isDebugFile = mapDB.isDebugFile();
		this.projection = mapDB.getProjection();

		if (fileOpenSuccessful) {
			mapDB.closeFile();
		}
	}

	/**
	 * Returns the area coordinates of the current map file in microdegrees.
	 * 
	 * @return the area coordinates of the current map file in microdegrees.
	 */
	public Rect getMapBoundary() {
		return mapBoundary;
	}

	/**
	 * Returns the center of the current map file (may be null).
	 * 
	 * @return the center of the current map file (may be null).
	 */
	public GeoPoint getMapCenter() {
		return mapCenter;
	}

	/**
	 * Returns the date of the map data in the current map file.
	 * 
	 * @return the date of the map data in the current map file.
	 */
	public long getMapDate() {
		return mapDate;
	}

	/**
	 * Returns the start position from the map file header (may be null).
	 * 
	 * @return the start position from the map file header (may be null).
	 */
	public GeoPoint getStartPosition() {
		return startPosition;
	}

	/**
	 * Informs about the existence of debug information in the current map file.
	 * 
	 * @return true if the current map file includes debug information, false otherwise.
	 */
	public boolean isDebugFile() {
		return isDebugFile;
	}

	/**
	 * Returns the name of the projection as it is encoded in the map file.
	 * 
	 * @return the projection name of the map file.
	 */
	public String getProjection() {
		return projection;
	}

}
