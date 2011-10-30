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
package org.mapsforge.android.maps.mapdatabase;

import org.mapsforge.android.maps.GeoPoint;

import android.graphics.Rect;

/**
 * Contains the immutable metadata of a map file.
 * 
 * @see MapDatabase#getMapFileInfo()
 */
public class MapFileInfo {
	private final String commentText;
	private final boolean debugFile;
	private final long fileSize;
	private final int fileVersion;
	private final String languagePreference;
	private final Rect mapBoundary;
	private final GeoPoint mapCenter;
	private final long mapDate;
	private final String projectionName;
	private final GeoPoint startPosition;

	MapFileInfo(MapFileInfoBuilder mapFileInfoBuilder) {
		this.commentText = mapFileInfoBuilder.getCommentText();
		this.debugFile = mapFileInfoBuilder.isDebugFile();
		this.fileSize = mapFileInfoBuilder.getFileSize();
		this.fileVersion = mapFileInfoBuilder.getFileVersion();
		this.languagePreference = mapFileInfoBuilder.getLanguagePreference();
		this.mapBoundary = mapFileInfoBuilder.getMapBoundary();
		this.mapCenter = new GeoPoint(this.mapBoundary.centerY(), this.mapBoundary.centerX());
		this.mapDate = mapFileInfoBuilder.getMapDate();
		this.projectionName = mapFileInfoBuilder.getProjectionName();
		this.startPosition = mapFileInfoBuilder.getStartPosition();
	}

	/**
	 * Returns the comment text of the map file (may be null).
	 * 
	 * @return the comment text of the map file (may be null).
	 */
	public String getCommentText() {
		return this.commentText;
	}

	/**
	 * Returns the size of the map file, measured in bytes.
	 * 
	 * @return the size of the map file, measured in bytes.
	 */
	public long getFileSize() {
		return this.fileSize;
	}

	/**
	 * Returns the file version number of the map file.
	 * 
	 * @return the file version number of the map file.
	 * @see MapDatabase#SUPPORTED_FILE_VERSION
	 */
	public int getFileVersion() {
		return this.fileVersion;
	}

	/**
	 * Returns the preferred language for names as defined in ISO 3166-1 (may be null).
	 * 
	 * @return the preferred language for names as defined in ISO 3166-1 (may be null).
	 */
	public String getLanguagePreference() {
		return this.languagePreference;
	}

	/**
	 * Returns the area coordinates of the map file in microdegrees.
	 * 
	 * @return the area coordinates of the map file in microdegrees.
	 */
	public Rect getMapBoundary() {
		return this.mapBoundary;
	}

	/**
	 * Returns the center point of the map file.
	 * 
	 * @return the center point of the map file.
	 */
	public GeoPoint getMapCenter() {
		return this.mapCenter;
	}

	/**
	 * Returns the date of the map data in milliseconds since January 1, 1970.
	 * 
	 * @return the date of the map data in milliseconds since January 1, 1970.
	 */
	public long getMapDate() {
		return this.mapDate;
	}

	/**
	 * Returns the projection name of the map file.
	 * 
	 * @return the projection name of the map file.
	 */
	public String getProjection() {
		return this.projectionName;
	}

	/**
	 * Returns the map start position from the file header (may be null).
	 * 
	 * @return the map start position from the file header (may be null).
	 */
	public GeoPoint getStartPosition() {
		return this.startPosition;
	}

	/**
	 * Informs about the existence of debug information in the map file.
	 * 
	 * @return true if the map file includes debug information, false otherwise.
	 */
	public boolean isDebugFile() {
		return this.debugFile;
	}
}
