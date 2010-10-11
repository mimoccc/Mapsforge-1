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
package org.mapsforge.android.maps;

/**
 * A MapGenerator that downloads map tiles from the Osmarender/Tiles@home openstreetmap server.
 */
class OsmarenderTileDownload extends TileDownloadMapGenerator {
	private static final String SERVER_HOST_NAME = "tah.openstreetmap.org";
	private static final String THREAD_NAME = "OsmarenderTileDownload";
	private static final byte ZOOM_MAX = 17;
	private StringBuilder stringBuilder;

	OsmarenderTileDownload() {
		this.stringBuilder = new StringBuilder(128);
	}

	@Override
	byte getMaxZoomLevel() {
		return ZOOM_MAX;
	}

	@Override
	String getServerHostName() {
		return SERVER_HOST_NAME;
	}

	@Override
	final String getThreadName() {
		return THREAD_NAME;
	}

	@Override
	String getTilePath(Tile tile) {
		// add the path of the requested tile image to the address
		this.stringBuilder.append(tile.zoomLevel);
		this.stringBuilder.append("/");
		this.stringBuilder.append(tile.x);
		this.stringBuilder.append("/");
		this.stringBuilder.append(tile.y);
		this.stringBuilder.append(".png");
		return stringBuilder.toString();
	}

	@Override
	void prepareMapGeneration() {
		// add the general part of the URL to the address
		this.stringBuilder.setLength(0);
		this.stringBuilder.append("http://");
		this.stringBuilder.append(SERVER_HOST_NAME);
		this.stringBuilder.append("/Tiles/tile/");
	}
}