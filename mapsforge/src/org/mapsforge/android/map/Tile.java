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

class Tile implements Comparable<Tile> {
	/**
	 * Amount of bytes per pixel of a map tile.
	 */
	static final byte TILE_BYTES_PER_PIXEL = 2;

	/**
	 * The width and height of a map tile in pixel.
	 */
	static final short TILE_SIZE = 256;

	/**
	 * The size of a single map tile in bytes.
	 */
	static final int TILE_SIZE_IN_BYTES = TILE_SIZE * TILE_SIZE * TILE_BYTES_PER_PIXEL;
	private final int hashCode;
	final MapViewMode mapViewMode;
	final long pixelX;
	final long pixelY;
	int renderPriority;
	final long x;
	final long y;
	final byte zoomLevel;

	/**
	 * Constructs a new tile with the specified XY number and zoom level.
	 * 
	 * @param x
	 *            the X number of the tile.
	 * @param y
	 *            the Y number of the tile.
	 * @param zoomLevel
	 *            the zoom level of the tile.
	 * @param mapViewMode
	 *            the map view mode.
	 */
	Tile(long x, long y, byte zoomLevel, MapViewMode mapViewMode) {
		this.x = x;
		this.y = y;
		this.zoomLevel = zoomLevel;
		this.mapViewMode = mapViewMode;
		this.hashCode = calculateHashCode();
		this.pixelX = x * TILE_SIZE;
		this.pixelY = y * TILE_SIZE;
		this.renderPriority = Integer.MAX_VALUE;
	}

	@Override
	public int compareTo(Tile another) {
		return this.renderPriority - another.renderPriority;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof Tile)) {
			return false;
		} else {
			Tile other = (Tile) obj;
			if (this.x != other.x) {
				return false;
			} else if (this.y != other.y) {
				return false;
			} else if (this.zoomLevel != other.zoomLevel) {
				return false;
			} else if (this.mapViewMode != other.mapViewMode) {
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
		result = prime * result + (int) (this.x ^ (this.x >>> 32));
		result = prime * result + (int) (this.y ^ (this.y >>> 32));
		result = prime * result + this.zoomLevel;
		result = prime * result
				+ ((this.mapViewMode == null) ? 0 : this.mapViewMode.hashCode());
		return result;
	}

	/**
	 * Get the bounding box of this tile.
	 * 
	 * @return the bounding box of this tile.
	 */
	Rect getBoundingBox() {
		return new Rect((long) (MercatorProjection.pixelXToLongitude(this.pixelX,
				this.zoomLevel) * 1000000), (long) (MercatorProjection.pixelYToLatitude(
				this.pixelY, this.zoomLevel) * 1000000), (long) (MercatorProjection
				.pixelXToLongitude(this.pixelX + TILE_SIZE, this.zoomLevel) * 1000000),
				(long) (MercatorProjection.pixelYToLatitude(this.pixelY + TILE_SIZE,
						this.zoomLevel) * 1000000));
	}
}