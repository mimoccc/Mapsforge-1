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

/**
 * A ZoomAnimator handles the zoom-in and zoom-out animations of the corresponding MapView. It
 * runs in a separate thread to avoid blocking the UI thread.
 */
class ZoomAnimator extends Thread {
	private static final int DEFAULT_DURATION = 300;
	private static final int SLEEP_MILLISECONDS = 15;
	private static final String THREAD_NAME = "ZoomAnimator";

	private int currentFrame;
	private float currentProgress;
	private float currentZoom;
	private int duration;
	private boolean executeAnimation;
	private MapView mapView;
	private int numberOfFrames;
	private float pivotX;
	private float pivotY;
	private float progressPerFrame;
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
				while (!isInterrupted() && !this.executeAnimation) {
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

			// calculate the scale for the current frame
			this.currentProgress = (this.currentZoom + this.progressPerFrame)
					/ this.currentZoom;
			this.currentZoom += this.progressPerFrame;

			// set the current scale of the transformation matrices
			this.mapView.matrixPostScale(this.currentProgress, this.currentProgress,
					this.pivotX, this.pivotY);
			synchronized (this.mapView.overlays) {
				for (Overlay overlay : this.mapView.overlays) {
					overlay.matrixPostScale(this.currentProgress, this.currentProgress,
							this.pivotX, this.pivotY);
				}
			}

			// increase the frame counter
			++this.currentFrame;

			// check if the last frame of the animation has been reached
			if (this.currentFrame >= this.numberOfFrames) {
				this.executeAnimation = false;
				synchronized (this.mapView.overlays) {
					for (Overlay overlay : this.mapView.overlays) {
						overlay.requestRedraw();
					}
				}
				this.mapView.handleTiles(false);
			} else {
				this.mapView.postInvalidate();
				try {
					sleep(SLEEP_MILLISECONDS);
				} catch (InterruptedException e) {
					// restore the interrupted status
					interrupt();
				}
			}
		}

		// set the pointer to null to avoid memory leaks
		this.mapView = null;
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
		this.numberOfFrames = this.duration / SLEEP_MILLISECONDS;
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
	void setParameters(float zoomStart, float zoomEnd, float pivotX, float pivotY) {
		this.zoomStart = zoomStart;
		this.zoomEnd = zoomEnd;
		this.pivotX = pivotX;
		this.pivotY = pivotY;
	}

	/**
	 * Starts a zoom animation with the current parameters.
	 */
	void startAnimation() {
		this.currentFrame = 0;
		this.currentZoom = this.zoomStart;
		this.progressPerFrame = (this.zoomEnd - this.zoomStart) / this.numberOfFrames;
		this.executeAnimation = true;
		synchronized (this) {
			notify();
		}
	}
}