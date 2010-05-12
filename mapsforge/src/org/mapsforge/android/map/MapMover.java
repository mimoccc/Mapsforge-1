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

import android.os.SystemClock;

/**
 * This class constantly moves the map horizontally and vertically. It is implemented as a
 * separate thread to avoid blocking the UI thread.
 */
class MapMover extends Thread {
	private static final float MOVE_SPEED = 0.25f;
	private MapView mapView;
	private long moveTimeCurrent;
	private long moveTimeElapsed;
	private long moveTimePrevious;
	private float moveX;
	private float moveY;

	@Override
	public void run() {
		while (!isInterrupted()) {
			synchronized (this) {
				while (!isInterrupted() && this.moveX == 0 && this.moveY == 0) {
					try {
						wait();
					} catch (InterruptedException e) {
						// restore the interrupted status
						interrupt();
					}
				}
			}

			if (isInterrupted()) {
				break;
			}

			// calculate the time difference to previous call
			this.moveTimeCurrent = SystemClock.uptimeMillis();
			this.moveTimeElapsed = this.moveTimeCurrent - this.moveTimePrevious;

			synchronized (this.mapView) {
				// add the movement to the transformation matrix
				this.mapView.matrix.postTranslate(this.moveTimeElapsed * this.moveX,
						this.moveTimeElapsed * this.moveY);

				// calculate the new position of the map center
				this.mapView.latitude = MercatorProjection.pixelYToLatitude(
						(MercatorProjection.latitudeToPixelY(this.mapView.latitude,
								this.mapView.zoomLevel) - (this.moveTimeElapsed * this.moveY)),
						this.mapView.zoomLevel);
				this.mapView.longitude = MercatorProjection.pixelXToLongitude(
						(MercatorProjection.longitudeToPixelX(this.mapView.longitude,
								this.mapView.zoomLevel) - (this.moveTimeElapsed * this.moveX)),
						this.mapView.zoomLevel);
			}

			this.mapView.handleTiles(false);
			this.moveTimePrevious = this.moveTimeCurrent;

			try {
				sleep(20);
			} catch (InterruptedException e) {
				interrupt();
			}
		}

		// set the pointer to null to avoid memory leaks
		this.mapView = null;
	}

	void moveDown() {
		if (this.moveY > 0) {
			// stop moving the map vertically
			this.moveY = 0;
		} else if (this.moveY == 0) {
			// start moving the map
			this.moveY = -MOVE_SPEED;
			this.moveTimePrevious = SystemClock.uptimeMillis();
			synchronized (this) {
				this.notify();
			}
		}
	}

	void moveLeft() {
		if (this.moveX < 0) {
			// stop moving the map horizontally
			this.moveX = 0;
		} else if (this.moveX == 0) {
			// start moving the map
			this.moveX = MOVE_SPEED;
			this.moveTimePrevious = SystemClock.uptimeMillis();
			synchronized (this) {
				this.notify();
			}
		}
	}

	void moveRight() {
		if (this.moveX > 0) {
			// stop moving the map horizontally
			this.moveX = 0;
		} else if (this.moveX == 0) {
			// start moving the map
			this.moveX = -MOVE_SPEED;
			this.moveTimePrevious = SystemClock.uptimeMillis();
			synchronized (this) {
				this.notify();
			}
		}
	}

	void moveUp() {
		if (this.moveY < 0) {
			// stop moving the map vertically
			this.moveY = 0;
		} else if (this.moveY == 0) {
			// start moving the map
			this.moveY = MOVE_SPEED;
			this.moveTimePrevious = SystemClock.uptimeMillis();
			synchronized (this) {
				this.notify();
			}
		}
	}

	void setMapView(MapView mapView) {
		this.mapView = mapView;
	}

	void stopHorizontalMove() {
		// stop moving the map horizontally
		this.moveX = 0;
	}

	void stopVerticalMove() {
		// stop moving the map vertically
		this.moveY = 0;
	}
}