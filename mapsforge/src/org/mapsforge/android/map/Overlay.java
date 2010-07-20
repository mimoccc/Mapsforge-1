package org.mapsforge.android.map;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;

/**
 * @author Karsten Groll
 * @author Sebastian Schlaak
 * 
 */
public abstract class Overlay extends Thread {
	/** This is where the overlays are drawn on before the canvas is touched */
	protected Bitmap bmp;
	private boolean isReady = false;
	/**
	 * Referenz zur Mapview
	 */
	protected MapView mapView;

	/**
	 * Interface for items that can be snapped. (Items that will be centered when the user zooms
	 * in/out.)
	 */

	/**
	 * The shadows x-offset<br />
	 * <b>This feature is not yet implemented!</b>
	 */
	protected static float SHADOW_X_SKEW = -0.8999999761581421f;

	/**
	 * The shadows y-offset<br />
	 * <b>This feature is not yet implemented!</b>
	 */
	protected static float SHADOW_Y_SKEW = 0.5f;

	/**
	 * Draws the overlay on the {@link MapView}
	 * 
	 * @param canvas
	 *            The canvas the overlay will be thrown onto
	 * 
	 * @param mapView
	 *            The {@link MapView} that called the draw-method
	 * 
	 * @param shadow
	 *            not yet implemented
	 */
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
	}

	/**
	 * Calls {@link Overlay#draw(Canvas, MapView, boolean)} and returns false.
	 * 
	 * @param canvas
	 *            The canvas the overlay will be drawn onto
	 * 
	 * @param mapView
	 *            The {@link MapView} that called the draw-method
	 * 
	 * @param shadow
	 *            not yet implemented
	 * @param when
	 *            not yet implemented
	 * @return false
	 */
	public boolean draw(android.graphics.Canvas canvas, MapView mapView, boolean shadow,
			long when) {
		draw(canvas, mapView, shadow);
		return false;
	}

	/**
	 * Handles behaviour on keypress. (Does nothing by default.)
	 * 
	 * @param keyCode
	 *            the keyCode of the event.
	 * @param event
	 *            the event.
	 * @param mapView
	 *            {@link MapView} that triggered the event
	 * @return false
	 */
	public boolean onKeyDown(int keyCode, android.view.KeyEvent event, MapView mapView) {
		return false;
	}

	/**
	 * Handles behaviour on keyrelease. (Does nothing by default.)
	 * 
	 * @param keyCode
	 *            the keyCode of the event.
	 * @param event
	 *            the event.
	 * @param mapView
	 *            {@link MapView} that triggered the event
	 * @return false
	 */
	public boolean onKeyUp(int keyCode, android.view.KeyEvent event, MapView mapView) {
		return false;
	}

	/**
	 * Handles a touch event. (Does nothing by default.)
	 * 
	 * @param event
	 *            the event.
	 * @param mapView
	 *            {@link MapView} that triggered the event
	 * @return True, if the event was handled by the overlay.
	 */
	public boolean onTouchEvent(android.view.MotionEvent event, MapView mapView) {
		return false;
	}

	/**
	 * Handle a trackball event.
	 * 
	 * @param event
	 *            the event.
	 * @param mapView
	 *            {@link MapView} that triggered the event
	 * @return True, if the event was handled by the overlay.
	 */
	public boolean onTrackballEvent(android.view.MotionEvent event, MapView mapView) {
		return false;
	}

	/**
	 * Prepares this overlay for drawing.
	 * 
	 * @param mapView
	 *            The parent mapview.
	 */
	protected abstract void prepareOverlayBitmap(MapView mapView);

	/**
	 * 
	 * Init the overlay-bitmap and the related canvas.
	 * 
	 * @param width
	 *            The width of the bitmap.
	 * @param height
	 *            The height of the bitmap.
	 */
	protected abstract void createOverlayBitmapsAndCanvas(int width, int height);

	/**
	 * 
	 * @return true if mapview is set.
	 */
	protected boolean isMapViewSet() {
		boolean ready = false;
		if (this.mapView == null)
			ready = false;
		else {
			ready = true;
		}
		return ready;
	}

	/**
	 * Set the mapview.
	 * 
	 * @param mapView
	 *            The mapview.
	 */
	protected void setMapViewAndCreateOverlayBitmaps(MapView mapView) {
		this.mapView = mapView;
		createOverlayBitmapsAndCanvas(mapView.getWidth(), mapView.getHeight());
	}

	/**
	 * @return the matrix of this overlay.
	 * 
	 */
	protected abstract Matrix getMatrix();

	@Override
	public final void run() {
		while (!isInterrupted()) {
			synchronized (this) {
				try {
					this.isReady = true;
					this.wait();
				} catch (InterruptedException e) {
					interrupt();
				}
			}
			this.isReady = false;
			if (isInterrupted()) {
				break;
			}
			prepareOverlayBitmap(this.mapView);
		}
	}
}
