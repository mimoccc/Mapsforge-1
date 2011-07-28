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
package org.mapsforge.pc.maps;

//import android.os.SystemClock;
import org.mapsforge.core.os.SystemClock;

/**
 * A ZoomAnimator handles the zoom-in and zoom-out animations of the
 * corresponding MapView. It runs in a separate thread to avoid blocking the UI
 * thread.
 */
class ZoomAnimator extends Thread {
	private static final int DEFAULT_DURATION = 300;
	private static final int FRAME_LENGTH = 15;
	private static final String THREAD_NAME = "ZoomAnimator";

	private float currentZoom;
	private int duration;
	private boolean executeAnimation;
	private MapView mapView;
	private float pivotX;
	private float pivotY;
	private float scaleFactor;
	private float scaleFactorApplied;
	// private long timeCurrent;
	// private long timeElapsed;
	// private float timeElapsedPercent;
	// private long timeStart;
	private float zoomDifference;
	private float zoomEnd;
	private float zoomStart;

	/**
	 * Constructs a new ZoomAnimator with the default duration.
	 */
	ZoomAnimator() {
		setDuration(DEFAULT_DURATION);
	}

	@Override
	public void run() {
		setName(THREAD_NAME);

		while (!isInterrupted()) {
			synchronized (this) {
				while (!isInterrupted() && ((this.zoomDifference == 0))) {
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

			// calculate the elapsed time
			// this.timeCurrent = SystemClock.uptimeMillis();
			// this.timeElapsed = this.timeCurrent - this.timeStart;
			// this.timeElapsedPercent = Math.min(1, this.timeElapsed / (float)
			// this.duration);
			// calculate the zoom and scale values at the current moment
			// this.currentZoom = this.zoomStart + this.timeElapsedPercent *
			// this.zoomDifference;
			this.currentZoom = this.zoomStart + this.zoomDifference;
			this.mapView.zoomLevel += this.currentZoom;
			this.scaleFactor = this.currentZoom / this.scaleFactorApplied;
			this.scaleFactorApplied *= this.scaleFactor;
			this.mapView.matrixPostScale(this.scaleFactor, this.scaleFactor,
					this.pivotX, this.pivotY);

			// check if the animation time is over
			// if (this.timeElapsed >= this.duration) {
			this.executeAnimation = false;
			this.mapView.handleTiles(false);
			/*
			 * } else { this.mapView.postInvalidate();
			 */
			synchronized (this) {
				try {
					wait(FRAME_LENGTH);
				} catch (InterruptedException e) {
					// restore the interrupted status
					interrupt();
				}
			}
			// }
		}

		// set the pointer to null to avoid memory leaks
		this.mapView = null;
	}

	void zoomIn() {
		if (this.zoomDifference < 0) {
			//
			this.zoomDifference = 0;
		} else if (this.zoomDifference == 0) {
			//
			this.zoomDifference = 1f;
			synchronized (this) {
				notify();
			}
		}
	}

	void zoomOut() {
		if (this.zoomDifference > 0) {
			//
			this.zoomDifference = 0;
		} else if (this.zoomDifference == 0) {
			//
			this.zoomDifference = -1f;
			synchronized (this) {
				notify();
			}
		}
	}

	void stopZoom() {
		this.zoomDifference = 0;
	}

	/**
	 * Returns the status of the ZoomAnimator.
	 * 
	 * @return true if the ZoomAnimator is working, false otherwise.
	 */
	boolean isExecuting() {
		return this.executeAnimation;
	}

	/**
	 * Sets the duration of the animation in milliseconds.
	 * 
	 * @param duration
	 *            the duration of the animation in milliseconds.
	 * @throws IllegalArgumentException
	 *             if the duration is negative.
	 */
	void setDuration(int duration) {
		if (duration < 0) {
			throw new IllegalArgumentException();
		}
		this.duration = duration;
	}

	/**
	 * Sets the MapView for this MapMover.
	 * 
	 * @param mapView
	 *            the MapView.
	 */
	void setMapView(MapView mapView) {
		this.mapView = mapView;
	}

	/**
	 * Sets the parameters for the zoom animation.
	 * 
	 * @param zoomStart
	 *            the zoom factor at the begin of the animation.
	 * @param zoomEnd
	 *            the zoom factor at the end of the animation.
	 * @param pivotX
	 *            the x coordinate of the animation center.
	 * @param pivotY
	 *            the y coordinate of the animation center.
	 */
	void setParameters(float zoomStart, float zoomEnd, float pivotX,
			float pivotY) {
		this.zoomStart = zoomStart;
		this.zoomEnd = zoomEnd;
		this.pivotX = pivotX;
		this.pivotY = pivotY;
	}

	/**
	 * Starts a zoom animation with the current parameters.
	 */
	void startAnimation() {
		this.zoomDifference = this.zoomEnd - this.zoomStart;
		this.scaleFactorApplied = this.zoomStart;
		this.executeAnimation = true;
		// this.timeStart = SystemClock.uptimeMillis();
		synchronized (this) {
			notify();
		}
	}
}