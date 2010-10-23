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

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ZoomControls;

/**
 * A MapView shows a map on the display of the device. It handles all user input and touch
 * gestures to move and zoom the map. This MapView also comes with an integrated scale bar,
 * which can be activated via the {@link #setScaleBar(boolean)} method. The built-in zoom
 * controls can be enabled with the {@link #setBuiltInZoomControls(boolean)} method. The
 * {@link #getController()} method returns a <code>MapController</code> to programmatically
 * modify the position and zoom level of the map.
 * <p>
 * This implementation supports offline map rendering as well as downloading map images (tiles)
 * over an Internet connection. All possible operation modes are listed in the
 * {@link MapViewMode} enumeration. The operation mode of a MapView can be set in the
 * constructor and changed at runtime with the {@link #setMapViewMode(MapViewMode)} method. In
 * offline rendering mode a special database file is required which contains the map data. Such
 * map files can be stored in any readable folder. The current map file for a MapView is set by
 * calling the {@link #setMapFile(String)} method.
 * <p>
 * Map tiles are automatically cached in a separate directory on the memory card. The size of
 * this cache may be adjusted via the {@link #setMemoryCardCacheSize(int)} method.
 * <p>
 * To draw an {@link Overlay} on top of the map, add it to the list returned by
 * {@link #getOverlays()}. More than one Overlay can be used at the same time to display
 * geographical data such as points and ways.
 * <p>
 * All text strings in the user interface of a MapView are customizable via the
 * {@link #setText(String, String)} method. The default texts are in English.
 */
public class MapView extends ViewGroup {
	/**
	 * Implementation for multi-touch capable devices.
	 */
	private class MultiTouchHandler extends TouchEventHandler {
		private static final int INVALID_POINTER_ID = -1;
		private int action;
		private int activePointerId;
		private int pointerIndex;
		private ScaleGestureDetector scaleGestureDetector;

		MultiTouchHandler() {
			this.activePointerId = INVALID_POINTER_ID;
			this.scaleGestureDetector = new ScaleGestureDetector(MapView.this.mapActivity,
					new ScaleListener());
		}

		@Override
		boolean handleTouchEvent(MotionEvent event) {
			// let the ScaleGestureDetector inspect all events
			this.scaleGestureDetector.onTouchEvent(event);

			// extract the action from the action code
			this.action = event.getAction() & MotionEvent.ACTION_MASK;

			if (this.action == MotionEvent.ACTION_DOWN) {
				// save the position of the event
				MapView.this.previousPositionX = event.getX();
				MapView.this.previousPositionY = event.getY();
				MapView.this.mapMoved = false;
				showZoomControls();
				// save the ID of the pointer
				this.activePointerId = event.getPointerId(0);
				return true;
			} else if (this.action == MotionEvent.ACTION_MOVE) {
				this.pointerIndex = event.findPointerIndex(this.activePointerId);

				if (this.scaleGestureDetector.isInProgress()) {
					return true;
				}

				// calculate the distance between previous and current position
				MapView.this.mapMoveX = event.getX(this.pointerIndex)
						- MapView.this.previousPositionX;
				MapView.this.mapMoveY = event.getY(this.pointerIndex)
						- MapView.this.previousPositionY;

				if (!MapView.this.mapMoved) {
					if (Math.abs(MapView.this.mapMoveX) > MapView.this.mapMoveDelta
							|| Math.abs(MapView.this.mapMoveY) > MapView.this.mapMoveDelta) {
						// the map movement delta has been reached
						MapView.this.mapMoved = true;
					} else {
						// do nothing
						return true;
					}
				}

				// save the position of the event
				MapView.this.previousPositionX = event.getX(this.pointerIndex);
				MapView.this.previousPositionY = event.getY(this.pointerIndex);

				synchronized (this) {
					// add the movement to the transformation matrix
					MapView.this.matrix.postTranslate(MapView.this.mapMoveX,
							MapView.this.mapMoveY);

					for (Overlay o : MapView.this.overlays) {
						o.getMatrix().postTranslate(MapView.this.mapMoveX,
								MapView.this.mapMoveY);
					}
					// calculate the new position of the map center
					MapView.this.latitude = getValidLatitude(MercatorProjection
							.pixelYToLatitude(
									(MercatorProjection.latitudeToPixelY(MapView.this.latitude,
											MapView.this.zoomLevel) - MapView.this.mapMoveY),
									MapView.this.zoomLevel));
					MapView.this.longitude = MercatorProjection.pixelXToLongitude(
							(MercatorProjection.longitudeToPixelX(MapView.this.longitude,
									MapView.this.zoomLevel) - MapView.this.mapMoveX),
							MapView.this.zoomLevel);
				}
				handleTiles(true);
				return true;
			} else if (this.action == MotionEvent.ACTION_UP) {
				hideZoomControlsDelayed();

				if (MapView.this.mapMoved) {
					// move-event: notify all overlays
					for (Overlay overlay : MapView.this.overlays) {
						synchronized (overlay) {
							overlay.notify();
						}
					}
				} else {
					// touch-event: forward the event to all overlays
					synchronized (MapView.this.overlays) {
						for (Overlay overlay : MapView.this.overlays) {
							overlay.onTouchEvent(event, MapView.this);
						}
					}
				}
				this.activePointerId = INVALID_POINTER_ID;
				return true;
			} else if (this.action == MotionEvent.ACTION_CANCEL) {
				hideZoomControlsDelayed();
				this.activePointerId = INVALID_POINTER_ID;
				return true;
			} else if (this.action == MotionEvent.ACTION_POINTER_UP) {
				// extract the index of the pointer that left the touch sensor
				this.pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
				if (event.getPointerId(this.pointerIndex) == this.activePointerId) {
					// the active pointer has gone up, choose a new one
					if (this.pointerIndex == 0) {
						this.pointerIndex = 1;
					} else {
						this.pointerIndex = 0;
					}
					// save the position of the event
					MapView.this.previousPositionX = event.getX(this.pointerIndex);
					MapView.this.previousPositionY = event.getY(this.pointerIndex);
					this.activePointerId = event.getPointerId(this.pointerIndex);
				}
				return true;
			}
			// the event was not handled
			return false;
		}
	}

	private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
		private float focusX;
		private float focusY;
		private float scaleFactor;

		ScaleListener() {
			// empty default constructor
		}

		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			this.scaleFactor *= detector.getScaleFactor();
			MapView.this.matrix.setScale(this.scaleFactor, this.scaleFactor, this.focusX,
					this.focusY);
			invalidate();
			return true;
		}

		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector) {
			// reset the current scale factor
			this.scaleFactor = 1;

			// save the focal point of the gesture
			this.focusX = detector.getFocusX();
			this.focusY = detector.getFocusY();
			return true;
		}

		@Override
		public void onScaleEnd(ScaleGestureDetector detector) {
			if (this.scaleFactor <= 0.5f || this.scaleFactor >= 2) {
				// change the zoom level according to the scale gesture
				zoom((byte) (Math.log(this.scaleFactor) / Math.log(2)));
			} else {
				// the gesture was too small for a zoom level change
				handleTiles(true);
			}
		}
	}

	/**
	 * Implementation for single-touch capable devices.
	 */
	private class SingleTouchHandler extends TouchEventHandler {
		SingleTouchHandler() {
			// do nothing
		}

		@Override
		boolean handleTouchEvent(MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				// save the position of the event
				MapView.this.previousPositionX = event.getX();
				MapView.this.previousPositionY = event.getY();
				MapView.this.mapMoved = false;
				showZoomControls();
				return true;
			} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
				// calculate the distance between previous and current position
				MapView.this.mapMoveX = event.getX() - MapView.this.previousPositionX;
				MapView.this.mapMoveY = event.getY() - MapView.this.previousPositionY;

				if (!MapView.this.mapMoved) {
					if (Math.abs(MapView.this.mapMoveX) > MapView.this.mapMoveDelta
							|| Math.abs(MapView.this.mapMoveY) > MapView.this.mapMoveDelta) {
						// the map movement delta has been reached
						MapView.this.mapMoved = true;
					} else {
						// do nothing
						return true;
					}
				}

				// save the position of the event
				MapView.this.previousPositionX = event.getX();
				MapView.this.previousPositionY = event.getY();

				synchronized (this) {
					// add the movement to the transformation matrix
					MapView.this.matrix.postTranslate(MapView.this.mapMoveX,
							MapView.this.mapMoveY);

					for (Overlay o : MapView.this.overlays) {
						o.getMatrix().postTranslate(MapView.this.mapMoveX,
								MapView.this.mapMoveY);
					}
					// calculate the new position of the map center
					MapView.this.latitude = getValidLatitude(MercatorProjection
							.pixelYToLatitude(
									(MercatorProjection.latitudeToPixelY(MapView.this.latitude,
											MapView.this.zoomLevel) - MapView.this.mapMoveY),
									MapView.this.zoomLevel));
					MapView.this.longitude = MercatorProjection.pixelXToLongitude(
							(MercatorProjection.longitudeToPixelX(MapView.this.longitude,
									MapView.this.zoomLevel) - MapView.this.mapMoveX),
							MapView.this.zoomLevel);
				}
				handleTiles(true);
				return true;
			} else if (event.getAction() == MotionEvent.ACTION_UP) {
				hideZoomControlsDelayed();

				if (MapView.this.mapMoved) {
					// move-event: notify all overlays
					for (Overlay overlay : MapView.this.overlays) {
						synchronized (overlay) {
							overlay.notify();
						}
					}
				} else {
					// touch-event: forward the event to all overlays
					synchronized (MapView.this.overlays) {
						for (Overlay overlay : MapView.this.overlays) {
							overlay.onTouchEvent(event, MapView.this);
						}
					}
				}
				return true;
			} else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
				hideZoomControlsDelayed();
				return true;
			}
			// the event was not handled
			return false;
		}
	}

	/**
	 * Abstract base class for all handlers of touch events.
	 */
	abstract class TouchEventHandler {
		abstract boolean handleTouchEvent(MotionEvent event);
	}

	private static final int DEFAULT_MAP_MOVE_DELTA = 10;
	private static final MapViewMode DEFAULT_MAP_VIEW_MODE = MapViewMode.CANVAS_RENDERER;
	private static final int DEFAULT_MOVE_SPEED = 10;
	private static final int DEFAULT_TILE_MEMORY_CARD_CACHE_SIZE = 100;
	private static final String DEFAULT_UNIT_SYMBOL_KILOMETER = " km";
	private static final String DEFAULT_UNIT_SYMBOL_METER = " m";
	private static final String EXTERNAL_STORAGE_DIRECTORY = File.separatorChar + "mapsforge";
	private static final short MAP_SCALE_HEIGHT = 35;
	private static final int[] MAP_SCALE_VALUES = { 10000000, 5000000, 2000000, 1000000,
			500000, 200000, 100000, 50000, 20000, 10000, 5000, 2000, 1000, 500, 200, 100, 50,
			20, 10, 5, 2, 1 };
	private static final short MAP_SCALE_WIDTH = 130;
	private static final int MAP_VIEW_BACKGROUND = Color.rgb(238, 238, 238);
	private static final int MSG_ZOOM_CONTROLS_HIDE = 0;
	private static final Paint PAINT_MAP_SCALE = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_MAP_SCALE_STROKE = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_MAP_SCALE_TEXT = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_MAP_SCALE_TEXT_WHITE_STROKE = new Paint(
			Paint.ANTI_ALIAS_FLAG);
	private static final int TILE_RAM_CACHE_SIZE = 20;
	private static final float TRACKBALL_MOVE_SPEED = 40;
	private static final long ZOOM_CONTROLS_TIMEOUT = ViewConfiguration
			.getZoomControlsTimeout();
	private static final byte ZOOM_MIN = 0;
	static final double LATITUDE_MAX = 85.05113;
	static final double LATITUDE_MIN = -85.05113;
	static final double LONGITUDE_MAX = 180;
	static final double LONGITUDE_MIN = -180;
	static final double TIME_DELAY_TO_PREPARE_IN_MS = 1000;

	/**
	 * Returns the default operation mode of a MapView.
	 * 
	 * @return the default operation mode.
	 */
	public static MapViewMode getDefaultMapViewMode() {
		return DEFAULT_MAP_VIEW_MODE;
	}

	/**
	 * Returns the size of a single map tile in bytes.
	 * 
	 * @return the tile size.
	 */
	public static int getTileSizeInBytes() {
		return Tile.TILE_SIZE_IN_BYTES;
	}

	/**
	 * Checks whether a given file is a valid map file.
	 * 
	 * @param mapFile
	 *            the path to the map file that should be tested.
	 * @return true, if the file is a valid map file, false otherwise.
	 */
	public static boolean isValidMapFile(String mapFile) {
		MapDatabase testDatabase = new MapDatabase();
		boolean isValid = testDatabase.openFile(mapFile);
		testDatabase.closeFile();
		return isValid;
	}

	private boolean attachedToWindow;
	private MapGeneratorJob currentJob;
	private Tile currentTile;
	private long currentTime;
	private MapDatabase database;
	private boolean drawTileFrames;
	private int fps;
	private Paint fpsPaint;
	private short frame_counter;
	private MapController mapController;
	private String mapFile;
	private MapGenerator mapGenerator;
	private MapMover mapMover;
	private int mapScale;
	private Bitmap mapScaleBitmap;
	private Canvas mapScaleCanvas;
	private float mapScaleLength;
	private double mapScalePreviousLatitude;
	private byte mapScalePreviousZoomLevel;
	private Bitmap mapViewBitmap1;
	private Bitmap mapViewBitmap2;
	private Canvas mapViewCanvas;
	private MapViewMode mapViewMode;
	private long mapViewTileX1;
	private long mapViewTileX2;
	private long mapViewTileY1;
	private long mapViewTileY2;
	private float matrixScaleFactor;
	private double meterPerPixel;
	private float moveSpeedFactor;
	private int numberOfTiles;
	private long previousTime;
	private long previousTimeSinceDrawOverlays;
	private boolean showFpsCounter;
	private boolean showScaleBar;
	private boolean showZoomControls;
	private Bitmap swapMapViewBitmap;
	private Bitmap tileBitmap;
	private ByteBuffer tileBuffer;
	private TileMemoryCardCache tileMemoryCardCache;
	private int tileMemoryCardCacheSize;
	private TileRAMCache tileRAMCache;
	private long tileX;
	private long tileY;
	private TouchEventHandler touchEventHandler;
	private String unit_symbol_kilometer;
	private String unit_symbol_meter;
	private Handler zoomControlsHideHandler;
	double latitude;
	double longitude;
	MapActivity mapActivity;
	boolean mapMoved;
	float mapMoveDelta;
	float mapMoveX;
	float mapMoveY;
	final int mapViewId;
	double mapViewPixelX;
	double mapViewPixelY;
	Matrix matrix;
	ArrayList<Overlay> overlays;
	float previousPositionX;
	float previousPositionY;
	ZoomControls zoomControls;
	byte zoomLevel;

	/**
	 * Constructs a new MapView with the default {@link MapViewMode}.
	 * 
	 * @param context
	 *            the enclosing MapActivity object.
	 * @throws IllegalArgumentException
	 *             if the context object is not an instance of {@link MapActivity}.
	 */
	public MapView(Context context) {
		this(context, DEFAULT_MAP_VIEW_MODE);
	}

	/**
	 * Constructs a new MapView. The {@link MapViewMode} can be configured via XML with the
	 * "mode" attribute in the layout file.
	 * 
	 * @param context
	 *            the enclosing MapActivity object.
	 * @param attrs
	 *            A set of attributes.
	 * @throws IllegalArgumentException
	 *             if the context object is not an instance of {@link MapActivity}.
	 */
	public MapView(Context context, AttributeSet attrs) {
		super(context, attrs);
		if (!(context instanceof MapActivity)) {
			throw new IllegalArgumentException();
		}
		this.mapActivity = (MapActivity) context;
		String modeValue = attrs.getAttributeValue(null, "mode");
		if (modeValue == null) {
			// no mode specified, use default
			this.mapViewMode = DEFAULT_MAP_VIEW_MODE;
		} else {
			try {
				// try to use the specified mode
				this.mapViewMode = MapViewMode.valueOf(modeValue);
			} catch (IllegalArgumentException e) {
				// invalid mode, use default
				this.mapViewMode = DEFAULT_MAP_VIEW_MODE;
			}
		}
		this.mapViewId = this.mapActivity.getMapViewId();
		setupMapView();
	}

	/**
	 * Constructs a new MapView with the given MapViewMode.
	 * 
	 * @param context
	 *            the enclosing MapActivity object.
	 * @param mapViewMode
	 *            the mode in which the MapView should operate.
	 * @throws IllegalArgumentException
	 *             if the context object is not an instance of {@link MapActivity}.
	 */
	public MapView(Context context, MapViewMode mapViewMode) {
		super(context);
		if (!(context instanceof MapActivity)) {
			throw new IllegalArgumentException();
		}
		this.mapActivity = (MapActivity) context;
		this.mapViewMode = mapViewMode;
		this.mapViewId = this.mapActivity.getMapViewId();
		setupMapView();
	}

	/**
	 * Returns the MapController for this MapView.
	 * 
	 * @return the MapController.
	 */
	public MapController getController() {
		return this.mapController;
	}

	/**
	 * Returns the current center of the map as a GeoPoint.
	 * 
	 * @return the current center of the map.
	 */
	public GeoPoint getMapCenter() {
		return new GeoPoint(this.latitude, this.longitude);
	}

	/**
	 * Returns the database which is currently used for reading the map file.
	 * 
	 * @return the map database.
	 * @throws UnsupportedOperationException
	 *             if the current MapView mode works with an Internet connection.
	 */
	public MapDatabase getMapDatabase() {
		if (this.mapViewMode.requiresInternetConnection()) {
			throw new UnsupportedOperationException();
		}
		return this.database;
	}

	/**
	 * Returns the currently used map file.
	 * 
	 * @return the map file.
	 * @throws UnsupportedOperationException
	 *             if the current MapView mode works with an Internet connection.
	 */
	public String getMapFile() {
		if (this.mapViewMode.requiresInternetConnection()) {
			throw new UnsupportedOperationException();
		}
		return this.mapFile;
	}

	/**
	 * Returns the host name of the tile download server.
	 * 
	 * @return the server name.
	 * @throws UnsupportedOperationException
	 *             if the current MapView mode works with an Internet connection.
	 */
	public String getMapTileDownloadServer() {
		if (!this.mapViewMode.requiresInternetConnection()) {
			throw new UnsupportedOperationException();
		}
		return ((TileDownloadMapGenerator) this.mapGenerator).getServerHostName();
	}

	/**
	 * Returns the current operation mode of the MapView.
	 * 
	 * @return the mode of the MapView.
	 */
	public MapViewMode getMapViewMode() {
		return this.mapViewMode;
	}

	/**
	 * Returns the maximum zoom level of the map.
	 * 
	 * @return the maximum zoom level.
	 */
	public int getMaxZoomLevel() {
		return this.mapGenerator.getMaxZoomLevel();
	}

	/**
	 * Returns the list of overlays for this MapView.
	 * 
	 * @return the overlay list.
	 */
	public final List<Overlay> getOverlays() {
		return this.overlays;
	}

	/**
	 * Returns the projection that is currently in use to convert pixel coordinates to
	 * geographical coordinates on the map.
	 * 
	 * @return The projection of the MapView. Do not keep this object for a longer time.
	 */
	public Projection getProjection() {
		return new MercatorProjection(this);
	}

	/**
	 * Returns the current zoom level of the map.
	 * 
	 * @return the current zoom level.
	 */
	public int getZoomLevel() {
		return this.zoomLevel;
	}

	/**
	 * Checks for a valid current map file.
	 * 
	 * @return true if the MapView currently has a valid map file, false otherwise.
	 * @throws UnsupportedOperationException
	 *             if the current MapView mode works with an Internet connection.
	 */
	public boolean hasValidMapFile() {
		if (this.mapViewMode.requiresInternetConnection()) {
			throw new UnsupportedOperationException();
		}
		return this.mapFile != null;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
			this.mapMover.moveLeft();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
			this.mapMover.moveRight();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
			this.mapMover.moveUp();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
			this.mapMover.moveDown();
			return true;
		}
		return false;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
			this.mapMover.stopHorizontalMove();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
			this.mapMover.stopVerticalMove();
			return true;
		}
		return false;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!isClickable()) {
			return false;
		}
		return this.touchEventHandler.handleTouchEvent(event);
	}

	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		if (!isClickable()) {
			return false;
		}
		if (event.getAction() == MotionEvent.ACTION_MOVE) {
			// calculate the map move
			this.mapMoveX = event.getX() * (TRACKBALL_MOVE_SPEED * this.moveSpeedFactor);
			this.mapMoveY = event.getY() * (TRACKBALL_MOVE_SPEED * this.moveSpeedFactor);

			synchronized (this) {
				// add the movement to the transformation matrix
				this.matrix.postTranslate(this.mapMoveX, this.mapMoveY);
				for (Overlay overlay : this.overlays) {
					overlay.getMatrix().postTranslate(this.mapMoveX, this.mapMoveY);
				}
				// calculate the new position of the map center
				this.latitude = getValidLatitude(MercatorProjection
						.pixelYToLatitude((MercatorProjection.latitudeToPixelY(this.latitude,
								this.zoomLevel) - this.mapMoveY), this.zoomLevel));
				this.longitude = MercatorProjection.pixelXToLongitude((MercatorProjection
						.longitudeToPixelX(this.longitude, this.zoomLevel) - this.mapMoveX),
						this.zoomLevel);
			}
			handleTiles(true);
			return true;
		}
		// the event was not handled
		return false;
	}

	/**
	 * Sets the visibility of the zoom controls.
	 * 
	 * @param showZoomControls
	 *            true if the zoom controls should be visible, false otherwise.
	 */
	public void setBuiltInZoomControls(boolean showZoomControls) {
		this.showZoomControls = showZoomControls;
	}

	/**
	 * Sets the visibility of the frame rate.
	 * 
	 * @param showFpsCounter
	 *            true if the map frame rate should be visible, false otherwise.
	 */
	public void setFpsCounter(boolean showFpsCounter) {
		this.showFpsCounter = showFpsCounter;
		// invalidate the MapView
		invalidate();
	}

	/**
	 * Sets the map file for this MapView.
	 * 
	 * @param newMapFile
	 *            the path to the new map file.
	 * @throws UnsupportedOperationException
	 *             if the current MapView mode works with an Internet connection.
	 */
	public void setMapFile(String newMapFile) {
		if (this.mapViewMode.requiresInternetConnection()) {
			throw new UnsupportedOperationException();
		}
		if (newMapFile == null) {
			// no map file is given
			return;
		} else if (this.mapFile != null && this.mapFile.equals(newMapFile)) {
			// same map file as before
			return;
		} else if (this.database == null) {
			// no database exists
			return;
		}

		this.mapMover.pause();
		this.mapMover.stopMove();

		this.mapGenerator.pause();
		this.mapGenerator.clearJobs();

		waitForReadyMapMover();
		waitForReadyMapGenerator();

		this.mapGenerator.unpause();
		this.mapMover.unpause();

		this.database.closeFile();
		if (this.database.openFile(newMapFile)) {
			this.mapFile = newMapFile;
			clearMapView();
			setCenter(this.database.getMapBoundary().getCenter());
			handleTiles(true);
		} else {
			this.mapFile = null;
			clearMapView();
			invalidate();
		}
	}

	/**
	 * Sets a new operation mode for the MapView.
	 * 
	 * @param newMapViewMode
	 *            the new mode.
	 */
	public void setMapViewMode(MapViewMode newMapViewMode) {
		// check if the new mode differs from the old one
		if (this.mapViewMode != newMapViewMode) {
			stopMapGeneratorThread();
			this.mapViewMode = newMapViewMode;
			startMapGeneratorThread();
			clearMapView();
			handleTiles(true);
		}
	}

	/**
	 * Sets the new size of the memory card cache. If the cache already contains more items than
	 * the new capacity allows, items are discarded based on the cache policy.
	 * 
	 * @param newCacheSize
	 *            the new capacity of the file cache.
	 * @throws IllegalArgumentException
	 *             if the new capacity is negative.
	 */
	public void setMemoryCardCacheSize(int newCacheSize) {
		if (newCacheSize < 0) {
			throw new IllegalArgumentException();
		}
		this.tileMemoryCardCacheSize = newCacheSize;
		this.tileMemoryCardCache.setCapacity(this.tileMemoryCardCacheSize);
	}

	/**
	 * Sets the move speed of the map.
	 * 
	 * @param moveSpeedFactor
	 *            the factor by which the move speed of the map will be multiplied.
	 * @throws IllegalArgumentException
	 *             if the new moveSpeedFactor is negative.
	 */
	public void setMoveSpeed(float moveSpeedFactor) {
		if (moveSpeedFactor < 0) {
			throw new IllegalArgumentException();
		}
		this.moveSpeedFactor = moveSpeedFactor;
	}

	/**
	 * Sets the visibility of the scale bar.
	 * 
	 * @param showScaleBar
	 *            true if the scale bar should be visible, false otherwise.
	 */
	public void setScaleBar(boolean showScaleBar) {
		this.showScaleBar = showScaleBar;
		if (showScaleBar) {
			renderScaleBar();
		}
		// invalidate the MapView
		invalidate();
	}

	/**
	 * Overrides an internal text field with the given string.
	 * 
	 * Currently the following text fields can be set:
	 * <ul>
	 * <li>unit_symbol_kilometer</li>
	 * <li>unit_symbol_meter</li>
	 * </ul>
	 * 
	 * @param name
	 *            the name of the text field to override.
	 * @param value
	 *            the new value of the text field.
	 * @return true, if the new value could be set, false otherwise.
	 */
	public boolean setText(String name, String value) {
		if (name.equals("unit_symbol_kilometer")) {
			this.unit_symbol_kilometer = value;
			return true;
		} else if (name.equals("unit_symbol_meter")) {
			this.unit_symbol_meter = value;
			return true;
		}
		return false;
	}

	/**
	 * Sets the drawing of tile frames for debugging purposes. Not all operation modes support
	 * this feature, some may simply ignore this request.
	 * 
	 * @param drawTileFrames
	 *            true if tile frames should be drawn, false otherwise.
	 */
	public void setTileFrames(boolean drawTileFrames) {
		this.drawTileFrames = drawTileFrames;
		clearMapView();
		handleTiles(true);
	}

	private synchronized void clearMapView() {
		// clear the MapView bitmaps
		if (this.mapViewBitmap1 != null) {
			this.mapViewBitmap1.eraseColor(MAP_VIEW_BACKGROUND);
		}
		if (this.mapViewBitmap2 != null) {
			this.mapViewBitmap2.eraseColor(MAP_VIEW_BACKGROUND);
		}
	}

	private byte getValidZoomLevel(byte zoom) {
		if (zoom < ZOOM_MIN) {
			return ZOOM_MIN;
		} else if (zoom > this.mapGenerator.getMaxZoomLevel()) {
			return this.mapGenerator.getMaxZoomLevel();
		}
		return zoom;
	}

	private void renderScaleBar() {
		// check if recalculating and drawing of the map scale is necessary
		if (this.zoomLevel == this.mapScalePreviousZoomLevel
				&& Math.abs(this.latitude - this.mapScalePreviousLatitude) < 0.2) {
			// no need to refresh the map scale
			return;
		}

		// save the current zoom level and latitude
		this.mapScalePreviousZoomLevel = this.zoomLevel;
		this.mapScalePreviousLatitude = this.latitude;

		// calculate an even value for the map scale
		this.meterPerPixel = MercatorProjection.calculateGroundResolution(this.latitude,
				this.zoomLevel);
		for (int i = 0; i < MAP_SCALE_VALUES.length; ++i) {
			this.mapScale = MAP_SCALE_VALUES[i];
			this.mapScaleLength = this.mapScale / (float) this.meterPerPixel;
			if (this.mapScaleLength < (MAP_SCALE_WIDTH - 10)) {
				break;
			}
		}

		// fill the bitmap with transparent color
		this.mapScaleBitmap.eraseColor(Color.TRANSPARENT);

		// draw the map scale
		this.mapScaleCanvas
				.drawLine(7, 20, this.mapScaleLength + 3, 20, PAINT_MAP_SCALE_STROKE);
		this.mapScaleCanvas.drawLine(5, 10, 5, 30, PAINT_MAP_SCALE_STROKE);
		this.mapScaleCanvas.drawLine(this.mapScaleLength + 5, 10, this.mapScaleLength + 5, 30,
				PAINT_MAP_SCALE_STROKE);
		this.mapScaleCanvas.drawLine(7, 20, this.mapScaleLength + 3, 20, PAINT_MAP_SCALE);
		this.mapScaleCanvas.drawLine(5, 10, 5, 30, PAINT_MAP_SCALE);
		this.mapScaleCanvas.drawLine(this.mapScaleLength + 5, 10, this.mapScaleLength + 5, 30,
				PAINT_MAP_SCALE);

		// draw the scale text
		if (this.mapScale < 1000) {
			this.mapScaleCanvas.drawText(this.mapScale + this.unit_symbol_meter, 10, 15,
					PAINT_MAP_SCALE_TEXT_WHITE_STROKE);
			this.mapScaleCanvas.drawText(this.mapScale + this.unit_symbol_meter, 10, 15,
					PAINT_MAP_SCALE_TEXT);
		} else {
			this.mapScaleCanvas.drawText((this.mapScale / 1000) + this.unit_symbol_kilometer,
					10, 15, PAINT_MAP_SCALE_TEXT_WHITE_STROKE);
			this.mapScaleCanvas.drawText((this.mapScale / 1000) + this.unit_symbol_kilometer,
					10, 15, PAINT_MAP_SCALE_TEXT);
		}
	}

	private void setupFpsText() {
		// create the paint1 for drawing the FPS text
		this.fpsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		this.fpsPaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		this.fpsPaint.setTextSize(20);
	}

	private void setupMapScale() {
		// create the bitmap for the map scale and the canvas to draw on it
		this.mapScaleBitmap = Bitmap.createBitmap(MAP_SCALE_WIDTH, MAP_SCALE_HEIGHT,
				Bitmap.Config.ARGB_4444);
		this.mapScaleCanvas = new Canvas(this.mapScaleBitmap);

		// set the default text fields for the map scale
		this.unit_symbol_kilometer = DEFAULT_UNIT_SYMBOL_KILOMETER;
		this.unit_symbol_meter = DEFAULT_UNIT_SYMBOL_METER;

		// set up the paints to draw the map scale
		PAINT_MAP_SCALE.setStrokeWidth(2);
		PAINT_MAP_SCALE.setStrokeCap(Paint.Cap.SQUARE);
		PAINT_MAP_SCALE.setColor(Color.BLACK);
		PAINT_MAP_SCALE_STROKE.setStrokeWidth(5);
		PAINT_MAP_SCALE_STROKE.setStrokeCap(Paint.Cap.SQUARE);
		PAINT_MAP_SCALE_STROKE.setColor(Color.WHITE);

		PAINT_MAP_SCALE_TEXT.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		PAINT_MAP_SCALE_TEXT.setTextSize(14);
		PAINT_MAP_SCALE_TEXT.setColor(Color.BLACK);
		PAINT_MAP_SCALE_TEXT_WHITE_STROKE.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		PAINT_MAP_SCALE_TEXT_WHITE_STROKE.setStyle(Paint.Style.STROKE);
		PAINT_MAP_SCALE_TEXT_WHITE_STROKE.setStrokeWidth(3);
		PAINT_MAP_SCALE_TEXT_WHITE_STROKE.setTextSize(14);
		PAINT_MAP_SCALE_TEXT_WHITE_STROKE.setColor(Color.WHITE);
	}

	private void setupMapView() {
		// set up the TouchEventHandler depending on the Android version
		if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.FROYO) {
			this.touchEventHandler = new SingleTouchHandler();
		} else {
			this.touchEventHandler = new MultiTouchHandler();
		}

		this.tileMemoryCardCacheSize = DEFAULT_TILE_MEMORY_CARD_CACHE_SIZE;
		this.moveSpeedFactor = DEFAULT_MOVE_SPEED;
		this.mapMoveDelta = DEFAULT_MAP_MOVE_DELTA
				* this.mapActivity.getResources().getDisplayMetrics().density;

		setBackgroundColor(MAP_VIEW_BACKGROUND);
		setWillNotDraw(false);
		setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);

		setupZoomControls();
		setupMapScale();
		setupFpsText();

		// create the transformation matrix
		this.matrix = new Matrix();

		// create overlay list
		this.overlays = new ArrayList<Overlay>(4);

		// create the tile bitmap and buffer
		this.tileBitmap = Bitmap.createBitmap(Tile.TILE_SIZE, Tile.TILE_SIZE,
				Bitmap.Config.RGB_565);
		this.tileBuffer = ByteBuffer.allocate(Tile.TILE_SIZE_IN_BYTES);

		// create the image bitmap cache
		this.tileRAMCache = new TileRAMCache(TILE_RAM_CACHE_SIZE);

		// create the image file cache with a unique directory
		this.tileMemoryCardCache = new TileMemoryCardCache(Environment
				.getExternalStorageDirectory().getAbsolutePath()
				+ EXTERNAL_STORAGE_DIRECTORY + File.separatorChar + this.mapViewId,
				this.tileMemoryCardCacheSize);

		// create the MapController for this MapView
		this.mapController = new MapController(this);

		// create the database
		this.database = new MapDatabase();

		startMapGeneratorThread();

		// set the default position and zoom level of the map
		GeoPoint defaultStartPoint = this.mapGenerator.getDefaultStartPoint();
		this.latitude = defaultStartPoint.getLatitude();
		this.longitude = defaultStartPoint.getLongitude();
		this.zoomLevel = this.mapGenerator.getDefaultZoomLevel();

		// create and start the MapMover thread
		this.mapMover = new MapMover();
		this.mapMover.setMapView(this);
		this.mapMover.start();

		// register the MapView in the MapActivity
		this.mapActivity.registerMapView(this);
	}

	private void setupZoomControls() {
		// create the ZoomControls and set the click listeners
		this.zoomControls = new ZoomControls(this.mapActivity);
		this.zoomControls.setVisibility(View.GONE);

		// set the click listeners for each zoom button
		this.zoomControls.setOnZoomInClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				zoom((byte) 1);
			}
		});
		this.zoomControls.setOnZoomOutClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				zoom((byte) -1);
			}
		});

		// create the handler for the fade out animation
		this.zoomControlsHideHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				MapView.this.zoomControls.hide();
			}
		};

		addView(this.zoomControls, new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
	}

	/**
	 * Create and start the MapGenerator thread.
	 */
	private void startMapGeneratorThread() {
		switch (this.mapViewMode) {
			case CANVAS_RENDERER:
				this.mapGenerator = new CanvasRenderer();
				((DatabaseMapGenerator) this.mapGenerator).setDatabase(this.database);
				break;
			case MAPNIK_TILE_DOWNLOAD:
				this.mapGenerator = new MapnikTileDownload();
				break;
			case OPENCYCLEMAP_TILE_DOWNLOAD:
				this.mapGenerator = new OpenCycleMapTileDownload();
				break;
			case OPENGL_RENDERER:
				this.mapGenerator = new OpenGLRenderer(this.mapActivity, this);
				((DatabaseMapGenerator) this.mapGenerator).setDatabase(this.database);
				break;
			case OSMARENDER_TILE_DOWNLOAD:
				this.mapGenerator = new OsmarenderTileDownload();
				break;
		}

		if (this.attachedToWindow) {
			this.mapGenerator.onAttachedToWindow();
		}
		this.mapGenerator.setTileCaches(this.tileRAMCache, this.tileMemoryCardCache);
		this.mapGenerator.setMapView(this);
		this.mapGenerator.start();
	}

	private void stopMapGeneratorThread() {
		// stop the MapGenerator thread
		if (this.mapGenerator != null) {
			this.mapGenerator.interrupt();
			try {
				this.mapGenerator.join();
			} catch (InterruptedException e) {
				// restore the interrupted status
				Thread.currentThread().interrupt();
			}
			this.mapGenerator.onDetachedFromWindow();
			this.mapGenerator = null;
		}
	}

	private void waitForReadyMapGenerator() {
		while (!this.mapGenerator.isReady()) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// restore the interrupted status
				Thread.currentThread().interrupt();
			}
		}
	}

	private void waitForReadyMapMover() {
		while (!this.mapMover.isReady()) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// restore the interrupted status
				Thread.currentThread().interrupt();
			}
		}
	}

	@Override
	protected void onAttachedToWindow() {
		this.attachedToWindow = true;
		if (this.mapGenerator != null) {
			this.mapGenerator.onAttachedToWindow();
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		this.attachedToWindow = false;
		if (this.mapGenerator != null) {
			this.mapGenerator.onDetachedFromWindow();
		}
	}

	@Override
	protected final void onDraw(Canvas canvas) {
		if (this.mapViewBitmap1 == null) {
			return;
		}

		synchronized (this) {
			// draw the map and the map scale
			canvas.drawBitmap(this.mapViewBitmap1, this.matrix, null);

			if (this.showScaleBar) {
				canvas.drawBitmap(this.mapScaleBitmap, 5, getHeight() - MAP_SCALE_HEIGHT - 5,
						null);
			}
		}

		if (this.showFpsCounter) {
			// do the FPS calculation
			this.currentTime = SystemClock.uptimeMillis();
			if (this.currentTime - this.previousTime > 1000) {
				this.fps = (int) ((this.frame_counter * 1000) / (this.currentTime - this.previousTime));
				this.previousTime = this.currentTime;
				this.frame_counter = 0;
			}
			canvas.drawText(String.valueOf(this.fps), 20, 30, this.fpsPaint);
			++this.frame_counter;
		}

		// draw overlays
		this.currentTime = SystemClock.uptimeMillis();
		boolean timeElapsed = false;
		if (this.currentTime - this.previousTimeSinceDrawOverlays > TIME_DELAY_TO_PREPARE_IN_MS) {
			timeElapsed = true;
			this.previousTimeSinceDrawOverlays = this.currentTime;
		}

		for (Overlay overlay : this.overlays) {
			if (!overlay.isMapViewSet()) {
				overlay.setMapViewAndCreateOverlayBitmaps(this);
				synchronized (overlay) {
					overlay.notify();
				}
			} else if (timeElapsed) {
				synchronized (overlay) {
					overlay.notify();
				}
			}
			overlay.draw(canvas, this, false);
		}
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if (!changed) {
			// neither size nor position have changed
			return;
		}
		// position the ZoomControls at the bottom right corner
		this.zoomControls.layout(r - this.zoomControls.getMeasuredWidth() - l - 5, b
				- this.zoomControls.getMeasuredHeight() - t, r - l - 5, b - t);
	}

	@Override
	protected final void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// find out how big the ZoomControls should be
		this.zoomControls.measure(MeasureSpec.makeMeasureSpec(MeasureSpec
				.getSize(widthMeasureSpec), MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(
				MeasureSpec.getSize(heightMeasureSpec), MeasureSpec.AT_MOST));

		// make sure that MapView is big enough to display the ZoomControls
		setMeasuredDimension(Math.max(MeasureSpec.getSize(widthMeasureSpec), this.zoomControls
				.getMeasuredWidth()), Math.max(MeasureSpec.getSize(heightMeasureSpec),
				this.zoomControls.getMeasuredHeight()));
	}

	@Override
	protected synchronized void onSizeChanged(int w, int h, int oldw, int oldh) {
		// check if the previous map view bitmaps must be recycled
		if (this.mapViewBitmap1 != null) {
			this.mapViewBitmap1.recycle();
		}
		if (this.mapViewBitmap2 != null) {
			this.mapViewBitmap2.recycle();
		}

		// check if the new size is positive
		if (w > 0 && h > 0) {
			// calculate how many tiles are needed to fill the view completely
			this.numberOfTiles = ((getWidth() / Tile.TILE_SIZE) + 1)
					* ((getHeight() / Tile.TILE_SIZE) + 1);

			// create the new map view bitmaps
			this.mapViewBitmap1 = Bitmap.createBitmap(getWidth(), getHeight(),
					Bitmap.Config.RGB_565);
			this.mapViewBitmap2 = Bitmap.createBitmap(getWidth(), getHeight(),
					Bitmap.Config.RGB_565);

			// create the mapViewCanvas
			this.mapViewBitmap1.eraseColor(MAP_VIEW_BACKGROUND);
			this.mapViewCanvas = new Canvas(this.mapViewBitmap1);
			handleTiles(true);
		}
	}

	void destroyMapView() {
		// unregister the MapView in the MapActivity
		if (this.mapActivity != null) {
			this.mapActivity.unregisterMapView(this);
			this.mapActivity = null;
		}

		// stop the overlay threads
		if (this.overlays != null) {
			for (Overlay overlay : this.overlays) {
				overlay.interrupt();
			}
			this.overlays = null;
		}

		// stop the MapMover thread
		if (this.mapMover != null) {
			this.mapMover.interrupt();
			try {
				this.mapMover.join();
			} catch (InterruptedException e) {
				// restore the interrupted status
				Thread.currentThread().interrupt();
			}
			this.mapMover = null;
		}

		stopMapGeneratorThread();

		// destroy the map controller
		if (this.mapController != null) {
			this.mapController.destroy();
			this.mapController = null;
		}

		// free the mapViewBitmap1 memory
		if (this.mapViewBitmap1 != null) {
			this.mapViewBitmap1.recycle();
			this.mapViewBitmap1 = null;
		}

		// free the mapViewBitmap2 memory
		if (this.mapViewBitmap2 != null) {
			this.mapViewBitmap2.recycle();
			this.mapViewBitmap2 = null;
		}

		// free the mapScaleBitmap memory
		if (this.mapScaleBitmap != null) {
			this.mapScaleBitmap.recycle();
			this.mapScaleBitmap = null;
		}

		// set the pointer to null to avoid memory leaks
		this.swapMapViewBitmap = null;

		// free the tileBitmap memory
		if (this.tileBitmap != null) {
			this.tileBitmap.recycle();
			this.tileBitmap = null;
		}

		// destroy the image bitmap cache
		if (this.tileRAMCache != null) {
			this.tileRAMCache.destroy();
			this.tileRAMCache = null;
		}

		// destroy the image file cache
		if (this.tileMemoryCardCache != null) {
			this.tileMemoryCardCache.destroy();
			this.tileMemoryCardCache = null;
		}

		// close the map file
		if (this.database != null) {
			this.database.closeFile();
			this.database = null;
		}
	}

	GeoPoint getDefaultStartPoint() {
		return this.mapGenerator.getDefaultStartPoint();
	}

	byte getDefaultZoomLevel() {
		return this.mapGenerator.getDefaultZoomLevel();
	}

	float getMoveSpeedFactor() {
		return this.moveSpeedFactor;
	}

	double getValidLatitude(double lat) {
		if (lat < LATITUDE_MIN) {
			return LATITUDE_MIN;
		} else if (lat > LATITUDE_MAX) {
			return LATITUDE_MAX;
		}
		return lat;
	}

	/**
	 * Calculates all necessary tiles and adds jobs accordingly.
	 * 
	 * @param calledByUiThread
	 *            true if called from the UI thread, false otherwise.
	 */
	void handleTiles(boolean calledByUiThread) {
		if (!this.mapViewMode.requiresInternetConnection() && this.mapFile == null) {
			return;
		} else if (this.getWidth() == 0) {
			return;
		}

		synchronized (this) {
			// calculate the XY position of the MapView
			this.mapViewPixelX = MercatorProjection.longitudeToPixelX(this.longitude,
					this.zoomLevel)
					- (getWidth() >> 1);
			this.mapViewPixelY = MercatorProjection.latitudeToPixelY(this.latitude,
					this.zoomLevel)
					- (getHeight() >> 1);

			this.mapViewTileX1 = MercatorProjection.pixelXToTileX(this.mapViewPixelX,
					this.zoomLevel);
			this.mapViewTileY1 = MercatorProjection.pixelYToTileY(this.mapViewPixelY,
					this.zoomLevel);
			this.mapViewTileX2 = MercatorProjection.pixelXToTileX(this.mapViewPixelX
					+ getWidth(), this.zoomLevel);
			this.mapViewTileY2 = MercatorProjection.pixelYToTileY(this.mapViewPixelY
					+ getHeight(), this.zoomLevel);

			// go through all tiles that intersect the screen rectangle
			for (this.tileY = this.mapViewTileY2; this.tileY >= this.mapViewTileY1; --this.tileY) {
				for (this.tileX = this.mapViewTileX2; this.tileX >= this.mapViewTileX1; --this.tileX) {
					this.currentTile = new Tile(this.tileX, this.tileY, this.zoomLevel);
					this.currentJob = new MapGeneratorJob(this.currentTile, this.mapViewMode,
							this.mapFile, this.drawTileFrames);
					if (this.tileRAMCache.containsKey(this.currentJob)) {
						// bitmap cache hit
						putTileOnBitmap(this.currentJob, this.tileRAMCache
								.get(this.currentJob), false);
					} else if (this.tileMemoryCardCache.containsKey(this.currentJob)) {
						// file cache hit
						this.tileMemoryCardCache.get(this.currentJob, this.tileBuffer);
						this.tileBitmap.copyPixelsFromBuffer(this.tileBuffer);
						putTileOnBitmap(this.currentJob, this.tileBitmap, true);
					} else {
						// cache miss
						this.mapGenerator.addJob(this.currentJob);
					}
				}
			}
		}

		if (this.showScaleBar) {
			renderScaleBar();
		}

		// invalidate the MapView
		if (calledByUiThread) {
			invalidate();
		} else {
			postInvalidate();
		}

		// notify the MapGenerator to process the job list
		this.mapGenerator.requestSchedule(true);
	}

	boolean hasValidCenter() {
		if (Double.isNaN(this.latitude) || this.latitude > LATITUDE_MAX
				|| this.latitude < LATITUDE_MIN) {
			return false;
		} else if (Double.isNaN(this.longitude) || this.longitude > LONGITUDE_MAX
				|| this.longitude < LONGITUDE_MIN) {
			return false;
		} else if (!this.mapViewMode.requiresInternetConnection()
				&& (this.database == null || this.database.getMapBoundary() == null || !this.database
						.getMapBoundary().contains(getMapCenter()))) {
			return false;
		}
		return true;
	}

	void hideZoomControlsDelayed() {
		if (this.showZoomControls) {
			this.zoomControlsHideHandler.removeMessages(MSG_ZOOM_CONTROLS_HIDE);
			if (this.zoomControls.getVisibility() != VISIBLE) {
				this.zoomControls.show();
			}
			this.zoomControlsHideHandler.sendEmptyMessageDelayed(MSG_ZOOM_CONTROLS_HIDE,
					ZOOM_CONTROLS_TIMEOUT);
		}
	}

	void onPause() {
		// pause the MapMover thread
		if (this.mapMover != null) {
			this.mapMover.pause();
		}

		// pause the MapGenerator thread
		if (this.mapGenerator != null) {
			this.mapGenerator.pause();
		}
	}

	void onResume() {
		// unpause the MapMover thread
		if (this.mapMover != null) {
			this.mapMover.unpause();
		}

		// unpause the MapGenerator thread
		if (this.mapGenerator != null) {
			this.mapGenerator.unpause();
		}
	}

	/**
	 * Draws a tile bitmap at the right position on the MapViewBitmap.
	 * 
	 * @param mapGeneratorJob
	 *            the job with the tile.
	 * @param bitmap
	 *            the bitmap to be drawn.
	 * @param putToTileRAMCache
	 *            true if the bitmap should be stored in the RAM cache, false otherwise.
	 */
	synchronized void putTileOnBitmap(MapGeneratorJob mapGeneratorJob, Bitmap bitmap,
			boolean putToTileRAMCache) {
		// check if the tile and the current MapView rectangle intersect
		if (this.mapViewPixelX - mapGeneratorJob.tile.pixelX > Tile.TILE_SIZE
				|| this.mapViewPixelX + getWidth() < mapGeneratorJob.tile.pixelX) {
			// no intersection in x direction
			return;
		} else if (this.mapViewPixelY - mapGeneratorJob.tile.pixelY > Tile.TILE_SIZE
				|| this.mapViewPixelY + getHeight() < mapGeneratorJob.tile.pixelY) {
			// no intersection in y direction
			return;
		} else if (mapGeneratorJob.tile.zoomLevel != this.zoomLevel) {
			// the tile doesn't fit to the current zoom level
			return;
		}

		// check if the bitmap should go to the image bitmap cache
		if (putToTileRAMCache) {
			this.tileRAMCache.put(mapGeneratorJob, bitmap);
		}

		if (!this.matrix.isIdentity()) {
			// change the current MapViewBitmap
			this.mapViewBitmap2.eraseColor(MAP_VIEW_BACKGROUND);
			this.mapViewCanvas.setBitmap(this.mapViewBitmap2);

			// draw the previous MapViewBitmap on the current MapViewBitmap
			this.mapViewCanvas.drawBitmap(this.mapViewBitmap1, this.matrix, null);

			// swap the two MapViewBitmaps
			this.swapMapViewBitmap = this.mapViewBitmap1;
			this.mapViewBitmap1 = this.mapViewBitmap2;
			this.mapViewBitmap2 = this.swapMapViewBitmap;

			// reset the matrix
			this.matrix.reset();
		}

		// draw the tile bitmap at the correct position
		this.mapViewCanvas.drawBitmap(bitmap,
				(float) (mapGeneratorJob.tile.pixelX - this.mapViewPixelX),
				(float) (mapGeneratorJob.tile.pixelY - this.mapViewPixelY), null);
	}

	/**
	 * This method is called by the MapGenerator when its job queue is empty.
	 */
	void requestMoreJobs() {
		if (!this.mapViewMode.requiresInternetConnection() && this.mapFile == null) {
			return;
		} else if (this.getWidth() == 0) {
			return;
		} else if (this.tileMemoryCardCacheSize < this.numberOfTiles * 3) {
			// the capacity of the file cache is to small, skip preprocessing
			return;
		} else if (this.zoomLevel == 0) {
			// there are no surrounding tiles on zoom level 0
			return;
		}

		synchronized (this) {
			// tiles below and above the visible area
			for (this.tileX = this.mapViewTileX2 + 1; this.tileX >= this.mapViewTileX1 - 1; --this.tileX) {
				this.currentTile = new Tile(this.tileX, this.mapViewTileY2 + 1, this.zoomLevel);
				this.currentJob = new MapGeneratorJob(this.currentTile, this.mapViewMode,
						this.mapFile, this.drawTileFrames);
				if (!this.tileMemoryCardCache.containsKey(this.currentJob)) {
					// cache miss
					this.mapGenerator.addJob(this.currentJob);
				}

				this.currentTile = new Tile(this.tileX, this.mapViewTileY1 - 1, this.zoomLevel);
				this.currentJob = new MapGeneratorJob(this.currentTile, this.mapViewMode,
						this.mapFile, this.drawTileFrames);
				if (!this.tileMemoryCardCache.containsKey(this.currentJob)) {
					// cache miss
					this.mapGenerator.addJob(this.currentJob);
				}
			}

			// tiles left and right from the visible area
			for (this.tileY = this.mapViewTileY2; this.tileY >= this.mapViewTileY1; --this.tileY) {
				this.currentTile = new Tile(this.mapViewTileX2 + 1, this.tileY, this.zoomLevel);
				this.currentJob = new MapGeneratorJob(this.currentTile, this.mapViewMode,
						this.mapFile, this.drawTileFrames);
				if (!this.tileMemoryCardCache.containsKey(this.currentJob)) {
					// cache miss
					this.mapGenerator.addJob(this.currentJob);
				}

				this.currentTile = new Tile(this.mapViewTileX1 - 1, this.tileY, this.zoomLevel);
				this.currentJob = new MapGeneratorJob(this.currentTile, this.mapViewMode,
						this.mapFile, this.drawTileFrames);
				if (!this.tileMemoryCardCache.containsKey(this.currentJob)) {
					// cache miss
					this.mapGenerator.addJob(this.currentJob);
				}
			}
		}

		// notify the MapGenerator to process the job list
		this.mapGenerator.requestSchedule(false);
	}

	void setCenter(GeoPoint point) {
		setCenterAndZoom(point, this.zoomLevel);
	}

	void setCenterAndZoom(GeoPoint point, byte zoom) {
		if (this.mapViewMode.requiresInternetConnection()
				|| (this.database != null && this.database.getMapBoundary() != null && this.database
						.getMapBoundary().contains(point))) {
			synchronized (this) {
				if (hasValidCenter()) {
					// add the movement to the transformation matrix
					this.matrix.postTranslate((float) (MercatorProjection.longitudeToPixelX(
							this.longitude, this.zoomLevel) - MercatorProjection
							.longitudeToPixelX(point.getLongitude(), this.zoomLevel)),
							(float) (MercatorProjection.latitudeToPixelY(this.latitude,
									this.zoomLevel) - MercatorProjection.latitudeToPixelY(point
									.getLatitude(), this.zoomLevel)));

					for (Overlay overlay : this.overlays) {
						overlay.getMatrix().postTranslate(
								(float) (MercatorProjection.longitudeToPixelX(this.longitude,
										this.zoomLevel) - MercatorProjection.longitudeToPixelX(
										point.getLongitude(), this.zoomLevel)),
								(float) (MercatorProjection.latitudeToPixelY(this.latitude,
										this.zoomLevel) - MercatorProjection.latitudeToPixelY(
										point.getLatitude(), this.zoomLevel)));
					}

				}

				this.latitude = getValidLatitude(point.getLatitude());
				this.longitude = point.getLongitude();
				this.zoomLevel = getValidZoomLevel(zoom);
			}

			// enable or disable the zoom buttons if necessary
			this.zoomControls.setIsZoomInEnabled(this.zoomLevel != this.mapGenerator
					.getMaxZoomLevel());
			this.zoomControls.setIsZoomOutEnabled(this.zoomLevel != ZOOM_MIN);
			handleTiles(true);
		}
	}

	/**
	 * Calculates the priority for the given job based on the current position and zoom level of
	 * the map.
	 * 
	 * @param mapGeneratorJob
	 *            the job for which the priority should be calculated.
	 * @return the MapGeneratorJob with updated priority.
	 */
	MapGeneratorJob setJobPriority(MapGeneratorJob mapGeneratorJob) {
		if (mapGeneratorJob.tile.zoomLevel != this.zoomLevel) {
			mapGeneratorJob.priority = 1000 * Math.abs(mapGeneratorJob.tile.zoomLevel
					- this.zoomLevel);
		} else {
			// calculate the center of the MapView
			double mapViewCenterX = this.mapViewPixelX + (getWidth() >> 1);
			double mapViewCenterY = this.mapViewPixelY + (getHeight() >> 1);

			// calculate the center of the tile
			long tileCenterX = mapGeneratorJob.tile.pixelX + (Tile.TILE_SIZE >> 1);
			long tileCenterY = mapGeneratorJob.tile.pixelY + (Tile.TILE_SIZE >> 1);

			// set tile priority to the distance from the MapView center
			int diffX = (int) (mapViewCenterX - tileCenterX);
			int diffY = (int) (mapViewCenterY - tileCenterY);
			mapGeneratorJob.priority = SquareRoot.sqrt(diffX * diffX + diffY * diffY);
		}
		return mapGeneratorJob;
	}

	/**
	 * Sets the map file for this MapView without displaying it.
	 * 
	 * @param newMapFile
	 *            the path to the new map file.
	 * @throws UnsupportedOperationException
	 *             if the current MapView mode works with an Internet connection.
	 */
	void setMapFileFromPreferences(String newMapFile) {
		if (this.mapViewMode.requiresInternetConnection()) {
			throw new UnsupportedOperationException();
		}
		if (newMapFile != null && this.database != null && this.database.openFile(newMapFile)) {
			this.mapFile = newMapFile;
		} else {
			this.mapFile = null;
		}
	}

	byte setZoom(byte zoomLevel) {
		synchronized (this) {
			this.zoomLevel = getValidZoomLevel(zoomLevel);
		}

		// enable or disable the zoom buttons if necessary
		this.zoomControls.setIsZoomInEnabled(this.zoomLevel != this.mapGenerator
				.getMaxZoomLevel());
		this.zoomControls.setIsZoomOutEnabled(this.zoomLevel != ZOOM_MIN);
		handleTiles(true);
		return this.zoomLevel;
	}

	void showZoomControls() {
		if (this.showZoomControls) {
			this.zoomControlsHideHandler.removeMessages(MSG_ZOOM_CONTROLS_HIDE);
			if (this.zoomControls.getVisibility() != VISIBLE) {
				this.zoomControls.show();
			}
		}
	}

	/**
	 * Zoom in or out by the given amount of zoom levels.
	 * 
	 * @param zoomLevelDiff
	 *            the difference to the current zoomlevel.
	 * @return true, if the zoom level was changed, false otherwise.
	 */
	boolean zoom(byte zoomLevelDiff) {
		if (zoomLevelDiff > 0) {
			// zoom in
			if (this.zoomLevel + zoomLevelDiff > this.mapGenerator.getMaxZoomLevel()) {
				return false;
			}
			this.matrixScaleFactor = 1 << zoomLevelDiff;
		} else if (zoomLevelDiff < 0) {
			// zoom out
			if (this.zoomLevel + zoomLevelDiff < ZOOM_MIN) {
				return false;
			}
			this.matrixScaleFactor = 1.0f / (1 << zoomLevelDiff);
		} else {
			// zoom level is unchanged
			return false;
		}

		// scale the matrix and change the zoom level
		synchronized (this) {
			this.matrix.postScale(this.matrixScaleFactor, this.matrixScaleFactor,
					getWidth() >> 1, getHeight() >> 1);
			this.zoomLevel += zoomLevelDiff;
		}

		// enable or disable the zoom buttons if necessary
		this.zoomControls.setIsZoomInEnabled(this.zoomLevel != this.mapGenerator
				.getMaxZoomLevel());
		this.zoomControls.setIsZoomOutEnabled(this.zoomLevel != ZOOM_MIN);

		hideZoomControlsDelayed();
		handleTiles(true);
		return true;
	}
}