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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * A MapGenerator that downloads map tiles from a server. To build an implementation for a
 * certain tile server, extend this class and implement the abstract methods.
 */
abstract class TileDownloadMapGenerator extends MapGenerator {
	private Bitmap decodedBitmap;
	private InputStream inputStream;
	private int[] pixelColors;
	private Bitmap tileBitmap;

	@Override
	final void cleanup() {
		this.tileBitmap = null;
		if (this.decodedBitmap != null) {
			this.decodedBitmap.recycle();
			this.decodedBitmap = null;
		}
	}

	@Override
	final boolean executeJob(MapGeneratorJob mapGeneratorJob) {
		try {
			// read the data from the tile URL
			this.inputStream = new URL(getTilePath(mapGeneratorJob.tile)).openStream();
			this.decodedBitmap = BitmapFactory.decodeStream(this.inputStream);
			this.inputStream.close();

			// check if the input stream could be decoded into a bitmap
			if (this.decodedBitmap == null) {
				return false;
			}

			// copy all pixels from the decoded bitmap to the tile bitmap
			this.decodedBitmap.getPixels(this.pixelColors, 0, Tile.TILE_SIZE, 0, 0,
					Tile.TILE_SIZE, Tile.TILE_SIZE);
			this.decodedBitmap.recycle();
			if (this.tileBitmap != null) {
				this.tileBitmap.setPixels(this.pixelColors, 0, Tile.TILE_SIZE, 0, 0,
						Tile.TILE_SIZE, Tile.TILE_SIZE);
			}
			return true;
		} catch (IOException e) {
			Logger.e(e);
			return false;
		}
	}

	@Override
	abstract byte getMaxZoomLevel();

	/**
	 * Returns the host name of the tile download server.
	 * 
	 * @return the server name.
	 */
	abstract String getServerHostName();

	/**
	 * Builds the absolute path to the image file for the requested tile.
	 * 
	 * @param tile
	 *            the tile which requires an image.
	 * @return the absolute address where the image can be downloaded from.
	 */
	abstract String getTilePath(Tile tile);

	@Override
	final void setup(Bitmap bitmap) {
		this.tileBitmap = bitmap;
		this.pixelColors = new int[Tile.TILE_SIZE * Tile.TILE_SIZE];
	}
}