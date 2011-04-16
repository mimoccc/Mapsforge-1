/*
 * Copyright 2010, 2011 mapsforge.org
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
package org.mapsforge.android.maps;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 * A container class that holds all immutable rendering parameters for a single map image
 * together with a mutable priority field, which indicates the importance of this task.
 */
class MapGeneratorJob implements Comparable<MapGeneratorJob>, Serializable {
	private static final long serialVersionUID = 1L;

	private transient int hashCode;
	private transient MapGeneratorJob other;

	/**
	 * Flag if the tile coordinates should be drawn on the image for debugging.
	 */
	final boolean drawTileCoordinates;

	/**
	 * Flag if a frame should be drawn around the image for debugging.
	 */
	final boolean drawTileFrames;

	/**
	 * Flag to highlight the tile in case it is a water tile.
	 */
	final boolean highlightWater;

	/**
	 * Map file used to render the map image (may be null).
	 */
	final String mapFile;

	/**
	 * MapViewMode used to render the map image.
	 */
	final MapViewMode mapViewMode;

	/**
	 * Rendering priority of this job.
	 */
	transient int priority;

	/**
	 * Text scale for the map rendering.
	 */
	final float textScale;

	/**
	 * Tile that is rendered to a map image.
	 */
	final Tile tile;

	/**
	 * Creates a new job for the MapGenerator with the given parameters.
	 * 
	 * @param tile
	 *            the tile to be rendered as a map image.
	 * @param mapViewMode
	 *            the operation mode in which the map image should be generated.
	 * @param mapFile
	 *            the map file or null, if no map file is needed.
	 * @param textScale
	 *            the text scale for map rendering.
	 * @param drawTileFrames
	 *            flag to enable tile frames.
	 * @param drawTileCoordinates
	 *            flag to enable tile coordinates.
	 * @param highlightWater
	 *            flag to enable water tile highlighting.
	 */
	MapGeneratorJob(Tile tile, MapViewMode mapViewMode, String mapFile, float textScale,
			boolean drawTileFrames, boolean drawTileCoordinates, boolean highlightWater) {
		this.tile = tile;
		this.mapViewMode = mapViewMode;
		this.mapFile = mapFile;
		this.textScale = textScale;
		this.drawTileFrames = drawTileFrames;
		this.drawTileCoordinates = drawTileCoordinates;
		this.highlightWater = highlightWater;
		calculateTransientValues();
	}

	@Override
	public int compareTo(MapGeneratorJob another) {
		return this.priority - another.priority;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof MapGeneratorJob)) {
			return false;
		} else {
			this.other = (MapGeneratorJob) obj;
			if (!this.tile.equals(this.other.tile)) {
				return false;
			} else if (this.mapViewMode != this.other.mapViewMode) {
				return false;
			} else if (this.mapFile == null && this.other.mapFile != null) {
				return false;
			} else if (this.mapFile != null && !this.mapFile.equals(this.other.mapFile)) {
				return false;
			} else if (this.textScale != this.other.textScale) {
				return false;
			} else if (this.drawTileFrames != this.other.drawTileFrames) {
				return false;
			} else if (this.drawTileCoordinates != this.other.drawTileCoordinates) {
				return false;
			} else if (this.highlightWater != this.other.highlightWater) {
				return false;
			}
			return true;
		}
	}

	@Override
	public int hashCode() {
		return this.hashCode;
	}

	/**
	 * Calculates the hash value of this object.
	 * 
	 * @return the hash value of this object.
	 */
	private int calculateHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.tile == null) ? 0 : this.tile.hashCode());
		result = prime * result
				+ ((this.mapViewMode == null) ? 0 : this.mapViewMode.hashCode());
		result = prime * result + ((this.mapFile == null) ? 0 : this.mapFile.hashCode());
		result = prime * result + Float.floatToIntBits(this.textScale);
		result = prime * result + (this.drawTileFrames ? 1231 : 1237);
		result = prime * result + (this.drawTileCoordinates ? 1231 : 1237);
		result = prime * result + (this.highlightWater ? 1231 : 1237);
		return result;
	}

	/**
	 * Calculates the values of some transient variables.
	 */
	private void calculateTransientValues() {
		this.hashCode = calculateHashCode();
	}

	private void readObject(ObjectInputStream objectInputStream) throws IOException,
			ClassNotFoundException {
		objectInputStream.defaultReadObject();
		calculateTransientValues();
	}
}