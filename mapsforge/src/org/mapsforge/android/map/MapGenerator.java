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

import java.util.PriorityQueue;

import android.graphics.Bitmap;

/**
 * A MapGenerator provides map images. This abstract base class handles all thread specific
 * actions and provides the job queue for tiles, which need to be processed.
 */
abstract class MapGenerator extends Thread {
	private Tile currentTile;
	private Bitmap currentTileBitmap;
	private ImageBitmapCache imageBitmapCache;
	private ImageFileCache imageFileCache;
	private PriorityQueue<Tile> jobQueue1;
	private PriorityQueue<Tile> jobQueue2;
	private MapView mapView;
	private boolean pause;
	private boolean ready;
	private boolean requestMoreJobs;
	private boolean scheduleNeeded;
	private PriorityQueue<Tile> tempQueue;

	MapGenerator() {
		// set up the two job queues
		this.jobQueue1 = new PriorityQueue<Tile>(64);
		this.jobQueue2 = new PriorityQueue<Tile>(64);

		// create the currentTileBitmap for the tile content
		this.currentTileBitmap = Bitmap.createBitmap(Tile.TILE_SIZE, Tile.TILE_SIZE,
				Bitmap.Config.RGB_565);
	}

	@Override
	final public void run() {
		setName(getThreadName());
		setup(this.currentTileBitmap);

		while (!isInterrupted()) {
			prepareMapGeneration();

			synchronized (this) {
				while (!isInterrupted() && (this.jobQueue1.isEmpty() || this.pause)) {
					try {
						this.ready = true;
						wait();
					} catch (InterruptedException e) {
						// restore the interrupted status
						interrupt();
					}
				}
			}

			this.ready = false;

			if (isInterrupted()) {
				break;
			}

			// get the next tile from the job queue that needs to be processed
			synchronized (this) {
				if (this.scheduleNeeded) {
					schedule();
					this.scheduleNeeded = false;
				}
				this.currentTile = this.jobQueue1.poll();
			}

			// check if the current job can be skipped or must be processed
			if (!this.imageBitmapCache.containsKey(this.currentTile)
					&& !this.imageFileCache.containsKey(this.currentTile)) {
				// check if the tile was generated successfully
				if (generateTile(this.currentTile)) {
					if (isInterrupted()) {
						break;
					}

					// copy the tile to the MapView
					this.mapView
							.putTileOnBitmap(this.currentTile, this.currentTileBitmap, true);
					this.mapView.postInvalidate();

					// put the tile image in the cache
					this.imageFileCache.put(this.currentTile, this.currentTileBitmap);
				}
			}

			// if the job queue is empty, ask the MapView for more jobs
			if (!isInterrupted() && this.jobQueue1.isEmpty() && this.requestMoreJobs) {
				this.mapView.requestMoreJobs();
			}
		}

		cleanup();

		// free the currentTileBitmap memory
		if (this.currentTileBitmap != null) {
			this.currentTileBitmap.recycle();
			this.currentTileBitmap = null;
		}

		// set some fields to null to avoid memory leaks
		this.mapView = null;
		this.imageBitmapCache = null;
		this.imageFileCache = null;

		if (this.jobQueue1 != null) {
			this.jobQueue1.clear();
			this.jobQueue1 = null;
		}

		if (this.jobQueue2 != null) {
			this.jobQueue2.clear();
			this.jobQueue2 = null;
		}

		this.tempQueue = null;
	}

	/**
	 * Schedules all tiles in the job queue.
	 */
	private void schedule() {
		while (!this.jobQueue1.isEmpty()) {
			this.jobQueue2.offer(this.mapView.setTilePriority(this.jobQueue1.poll()));
		}
		// swap the two job queues
		this.tempQueue = this.jobQueue1;
		this.jobQueue1 = this.jobQueue2;
		this.jobQueue2 = this.tempQueue;
	}

	/**
	 * Adds the given tile to the job queue.
	 * 
	 * @param tile
	 *            the tile to be added to the job queue.
	 */
	final synchronized void addJob(Tile tile) {
		if (!this.jobQueue1.contains(tile)) {
			this.jobQueue1.offer(tile);
		}
	}

	/**
	 * This method will by called at the end of the run method when the thread was interrupted.
	 * It can be used to clean up objects and to close any open connections.
	 */
	abstract void cleanup();

	/**
	 * Clears the job queue.
	 */
	final synchronized void clearJobs() {
		this.jobQueue1.clear();
	}

	/**
	 * This method will by called when a map tile needs to be generated.
	 * 
	 * @param tile
	 *            the tile that is needed.
	 * @return true, if the tile was generated successfully, false otherwise.
	 */
	abstract boolean generateTile(Tile tile);

	/**
	 * Returns the default starting point on the map.
	 * 
	 * @return the default starting point.
	 */
	GeoPoint getDefaultStartPoint() {
		return new GeoPoint(51.33, 10.45);
	}

	/**
	 * Returns the default zoom level of the map.
	 * 
	 * @return the default zoom level.
	 */
	byte getDefaultZoomLevel() {
		return 5;
	}

	/**
	 * Returns the maximum zoom level that the MapGenerator can handle.
	 * 
	 * @return the maximum zoom level.
	 */
	abstract byte getMaxZoomLevel();

	/**
	 * Returns the name of the MapGenerator. It will be used as the name for the thread.
	 * 
	 * @return the name of the MapGenerator.
	 */
	abstract String getThreadName();

	/**
	 * Returns the status of the MapGenerator.
	 * 
	 * @return true, if the MapGenerator is not working, false otherwise.
	 */
	final boolean isReady() {
		return this.ready;
	}

	abstract void onAttachedToWindow();

	abstract void onDetachedFromWindow();

	/**
	 * Request the MapGenerator to stop working.
	 */
	final synchronized void pause() {
		this.pause = true;
	}

	/**
	 * This method is called each time before a tile needs to be processed. It can be used to
	 * clear any data structures that will be needed when the next map tile will be processed.
	 */
	abstract void prepareMapGeneration();

	/**
	 * Request a scheduling of all tiles that are currently in the job queue.
	 * 
	 * @param askForMoreJobs
	 *            true, if the MapGenerator may ask for more jobs, false otherwise.
	 */
	final synchronized void requestSchedule(boolean askForMoreJobs) {
		this.scheduleNeeded = true;
		this.requestMoreJobs = askForMoreJobs;
		if (!this.jobQueue1.isEmpty()) {
			this.notify();
		}
	}

	/**
	 * Sets the image caches that the MapGenerator should use.
	 * 
	 * @param imageBitmapCache
	 *            the ImageBitmapCache.
	 * @param imageFileCache
	 *            the ImageFileCache.
	 */
	final void setImageCaches(ImageBitmapCache imageBitmapCache, ImageFileCache imageFileCache) {
		this.imageBitmapCache = imageBitmapCache;
		this.imageFileCache = imageFileCache;
	}

	/**
	 * Sets the MapView for the MapGenerator.
	 * 
	 * @param mapView
	 *            the MapView.
	 */
	final void setMapView(MapView mapView) {
		this.mapView = mapView;
	}

	/**
	 * This method is called only once before any map tile is requested. It can be used to set
	 * up data structures or connections that will be needed.
	 * 
	 * @param bitmap
	 *            the bitmap on which all future tiles need to be copied.
	 */
	abstract void setup(Bitmap bitmap);

	/**
	 * Request the MapGenerator to continue working.
	 */
	final synchronized void unpause() {
		this.pause = false;
		if (!this.jobQueue1.isEmpty()) {
			this.notify();
		}
	}
}