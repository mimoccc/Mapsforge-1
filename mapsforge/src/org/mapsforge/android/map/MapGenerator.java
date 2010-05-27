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
 * The MapGenerator reads map data from a database and renders map images.
 */
abstract class MapGenerator extends Thread {
	static final boolean DRAW_TILE_FRAMES = false;
	boolean scheduleNeeded;
	PriorityQueue<Tile> tempQueue;
	ImageBitmapCache imageBitmapCache;
	ImageFileCache imageFileCache;
	PriorityQueue<Tile> jobQueue1;
	PriorityQueue<Tile> jobQueue2;
	MapView mapView;
	boolean pause;
	boolean ready;
	Tile currentTile;
	Bitmap bitmap;

	abstract String getThreadName();

	abstract void prepareMapGeneration();

	abstract void doMapGeneration();

	abstract void setup();

	@Override
	public void run() {
		setName(getThreadName());

		// create the bitmap
		this.bitmap = Bitmap
				.createBitmap(Tile.TILE_SIZE, Tile.TILE_SIZE, Bitmap.Config.RGB_565);

		this.jobQueue1 = new PriorityQueue<Tile>(64);
		this.jobQueue2 = new PriorityQueue<Tile>(64);

		setup();

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
				doMapGeneration();

				if (isInterrupted()) {
					break;
				}

				this.mapView.putTileOnBitmap(this.currentTile, this.bitmap, true);
				this.mapView.postInvalidate();
				Thread.yield();

				// put the image in the cache
				this.imageFileCache.put(this.currentTile, this.bitmap);

			}

			// if the job queue is empty, ask the MapView for more jobs
			if (!isInterrupted() && this.jobQueue1.isEmpty()) {
				this.mapView.requestMoreJobs();
			}
		}

		cleanup();

		// free the bitmap memory
		if (this.bitmap != null) {
			this.bitmap.recycle();
			this.bitmap = null;
		}

		// set some fields to null to avoid memory leaks
		this.mapView = null;
		this.imageBitmapCache = null;
		this.imageFileCache = null;
	}

	abstract void cleanup();

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
	 * Clears the job queue.
	 */
	final synchronized void clearJobs() {
		this.jobQueue1.clear();
	}

	/**
	 * Returns the status of the MapGenerator.
	 * 
	 * @return true, if the MapGenerator is not working, false otherwise.
	 */
	final boolean isReady() {
		return this.ready;
	}

	/**
	 * Request that the MapGenerator should stop working.
	 */
	final synchronized void pause() {
		this.pause = true;
	}

	/**
	 * Request a scheduling of all tiles that are currently in the job queue.
	 */
	final synchronized void requestSchedule() {
		this.scheduleNeeded = true;
		if (!this.jobQueue1.isEmpty()) {
			this.notify();
		}
	}

	final void schedule() {
		// long t1 = SystemClock.currentThreadTimeMillis();
		while (!this.jobQueue1.isEmpty()) {
			this.jobQueue2.offer(this.mapView.setTilePriority(this.jobQueue1.poll()));
		}
		this.tempQueue = this.jobQueue1;
		this.jobQueue1 = this.jobQueue2;
		this.jobQueue2 = this.tempQueue;
		// long t2 = SystemClock.currentThreadTimeMillis();
		// Logger.d("scheduled " + this.jobQueue1.size() + " jobs: " + (t2 -
		// t1));
	}

	final void setImageCaches(ImageBitmapCache imageBitmapCache, ImageFileCache imageFileCache) {
		this.imageBitmapCache = imageBitmapCache;
		this.imageFileCache = imageFileCache;
	}

	final void setMapView(MapView mapView) {
		this.mapView = mapView;
	}

	/**
	 * Request that the MapGenerator should continue working.
	 */
	final synchronized void unpause() {
		this.pause = false;
		if (!this.jobQueue1.isEmpty()) {
			this.notify();
		}
	}
}