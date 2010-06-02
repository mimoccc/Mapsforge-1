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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

/**
 * A MapGenerator that downloads map tiles from the openstreetmap server.
 */
class TileDownloadMapGenerator extends MapGenerator {
	private static final short DOWNLOAD_TILE_SIZE = 256;
	private static final String SERVER_HOST_NAME = "tah.openstreetmap.org";
	private static final String THREAD_NAME = "TileDownloadMapGenerator";
	private static final byte ZOOM_MAX = 17;
	private Bitmap decodedBitmap;
	private InputStream inputStream;
	private int[] pixelColors;
	private StringBuilder stringBuilder;
	private Bitmap tileBitmap;

	@Override
	void cleanup() {
		this.tileBitmap = null;

		if (this.decodedBitmap != null) {
			this.decodedBitmap.recycle();
			this.decodedBitmap = null;
		}
	}

	@Override
	void doMapGeneration(Tile tile) {
		// build the relative path to the tile
		this.stringBuilder.append(tile.zoomLevel);
		this.stringBuilder.append("/");
		this.stringBuilder.append(tile.x);
		this.stringBuilder.append("/");
		this.stringBuilder.append(tile.y);
		this.stringBuilder.append(".png");
		try {
			// read the data from the assembled URL
			this.inputStream = new URL(this.stringBuilder.toString()).openStream();
			this.decodedBitmap = BitmapFactory.decodeStream(this.inputStream);
			this.inputStream.close();

			if (this.decodedBitmap == null) {
				// erase the tileBitmap with the default color
				if (this.tileBitmap != null) {
					this.tileBitmap.eraseColor(Color.rgb(248, 248, 248));
				}
			} else {
				// copy all pixels from the decoded bitmap to the tile bitmap
				this.decodedBitmap.getPixels(this.pixelColors, 0, DOWNLOAD_TILE_SIZE, 0, 0,
						DOWNLOAD_TILE_SIZE, DOWNLOAD_TILE_SIZE);
				this.decodedBitmap.recycle();
				if (this.tileBitmap != null) {
					this.tileBitmap.setPixels(this.pixelColors, 0, DOWNLOAD_TILE_SIZE, 0, 0,
							DOWNLOAD_TILE_SIZE, DOWNLOAD_TILE_SIZE);
				}
			}
		} catch (MalformedURLException e) {
			Logger.e(e);
		} catch (IOException e) {
			Logger.e(e);
		}
	}

	@Override
	GeoPoint getDefaultStartPoint() {
		return new GeoPoint(51.33, 10.45);
	}

	@Override
	byte getMaxZoomLevel() {
		return ZOOM_MAX;
	}

	/**
	 * Returns the host name of the tile download server.
	 * 
	 * @return the server name.
	 */
	String getServerHostName() {
		return SERVER_HOST_NAME;
	}

	@Override
	String getThreadName() {
		return THREAD_NAME;
	}

	@Override
	void prepareMapGeneration() {
		this.stringBuilder.setLength(0);
		this.stringBuilder.append("http://");
		this.stringBuilder.append(SERVER_HOST_NAME);
		this.stringBuilder.append("/Tiles/tile/");
	}

	@Override
	void setup(Bitmap bitmap) {
		this.tileBitmap = bitmap;
		this.pixelColors = new int[DOWNLOAD_TILE_SIZE * DOWNLOAD_TILE_SIZE];
		this.stringBuilder = new StringBuilder(128);
	}
}