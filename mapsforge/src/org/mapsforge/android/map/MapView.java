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

import java.nio.ByteBuffer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ZoomControls;

public class MapView extends ViewGroup {
	private static final int BITMAP_CACHE_SIZE = 25;
	private static final int DEFAULT_FILE_CACHE_SIZE = 100;
	private static final String DEFAULT_UNIT_SYMBOL_KILOMETER = " km";
	private static final String DEFAULT_UNIT_SYMBOL_METER = " m";
	private static final byte DEFAULT_ZOOM_LEVEL = 14;
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
	private static final long ZOOM_CONTROLS_TIMEOUT = ViewConfiguration
			.getZoomControlsTimeout();
	private static final byte ZOOM_MAX = 23;
	private static final byte ZOOM_MIN = 0;

	/**
	 * Returns the size of a single map tile in bytes.
	 * 
	 * @return the tile size
	 */
	public static int getTileSizeInBytes() {
		return Tile.TILE_SIZE_IN_BYTES;
	}

	private Tile currentTile;
	private long currentTime;
	private Database database;
	private int fps;
	private Paint fpsPaint;
	private short frame_counter;
	private ImageBitmapCache imageBitmapCache;
	private ImageFileCache imageFileCache;
	private final MapActivity mapActivity;
	private MapController mapController;
	private String mapFile;
	private MapGenerator mapGenerator;
	private MapMover mapMover;
	private float mapMoveX;
	private float mapMoveY;
	private int mapScale;
	private Bitmap mapScaleBitmap;
	private Canvas mapScaleCanvas;
	private float mapScaleLength;
	private double mapScalePreviousLatitude;
	private byte mapScalePreviousZoomLevel;
	private Bitmap mapViewBitmap1;
	private Bitmap mapViewBitmap2;
	private Canvas mapViewCanvas;
	private double mapViewPixelX;
	private double mapViewPixelY;
	private long mapViewTileX1;
	private long mapViewTileX2;
	private long mapViewTileY1;
	private long mapViewTileY2;
	private double meterPerPixel;
	private float previousPositionX;
	private float previousPositionY;
	private long previousTime;
	private boolean showFpsCounter;
	private boolean showMapScale;
	private boolean showZoomControls;
	private Bitmap swapMapViewBitmap;
	private Bitmap tileBitmap;
	private ByteBuffer tileBuffer;
	private long tileX;
	private long tileY;
	private String unit_symbol_kilometer;
	private String unit_symbol_meter;
	private Handler zoomControlsHideHandler;
	double latitude;
	double longitude;
	Matrix matrix;
	ZoomControls zoomControls;
	byte zoomLevel;

	/**
	 * Constructs a new MapView.
	 * 
	 * @param context
	 *            the enclosing MapActivity object.
	 * @throws IllegalArgumentException
	 *             if the context object is not an instance of {@link MapActivity}.
	 */
	public MapView(Context context) {
		super(context);
		if (!(context instanceof MapActivity)) {
			throw new IllegalArgumentException();
		}
		this.mapActivity = (MapActivity) context;
		setupMapView();
	}

	/**
	 * Constructs a new MapView.
	 * 
	 * @param context
	 *            the enclosing MapActivity object.
	 * @param attrs
	 *            A set of attributes (currently ignored).
	 * @throws IllegalArgumentException
	 *             if the context object is not an instance of {@link MapActivity}.
	 */
	public MapView(Context context, AttributeSet attrs) {
		super(context, attrs);
		if (!(context instanceof MapActivity)) {
			throw new IllegalArgumentException();
		}
		this.mapActivity = (MapActivity) context;
		setupMapView();
	}

	public MapController getController() {
		return this.mapController;
	}

	public GeoPoint getMapCenter() {
		return new GeoPoint(this.latitude, this.longitude);
	}

	/**
	 * Returns the currently used map file.
	 * 
	 * @return the map file.
	 */
	public String getMapFile() {
		return this.mapFile;
	}

	/**
	 * Returns the center coordinates of the current map file as a GeoPoint object.
	 * 
	 * @return the center coordinates of the map file.
	 */
	public GeoPoint getMapFileCenter() {
		if (this.database == null || this.database.getMapBoundary() == null) {
			return null;
		}
		return this.database.getMapBoundary().getCenter();
	}

	/**
	 * Returns the maximum zoom level of the map.
	 * 
	 * @return the maximum zoom level.
	 */
	public int getMaxZoomLevel() {
		return ZOOM_MAX;
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
	 * Convenience method to get isValidMapFile() on the current map file.
	 * 
	 * @return true if the MapView currently has a valid map file, false otherwise.
	 */
	public boolean hasValidMapFile() {
		return isValidMapFile(this.mapFile);
	}

	public boolean isValidMapFile(String file) {
		// TODO: implement this
		return file != null;
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
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			// save the position of the event
			this.previousPositionX = event.getX();
			this.previousPositionY = event.getY();
			showZoomControls();
			return true;
		} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
			// calculate the distance between previous and current position
			this.mapMoveX = event.getX() - this.previousPositionX;
			this.mapMoveY = event.getY() - this.previousPositionY;

			// save the position of the event
			this.previousPositionX = event.getX();
			this.previousPositionY = event.getY();

			synchronized (this) {
				// add the movement to the transformation matrix
				this.matrix.postTranslate(this.mapMoveX, this.mapMoveY);

				// calculate the new position of the map center
				this.latitude = MercatorProjection.pixelYToLatitude((MercatorProjection
						.latitudeToPixelY(this.latitude, this.zoomLevel) - this.mapMoveY),
						this.zoomLevel);
				this.longitude = MercatorProjection.pixelXToLongitude((MercatorProjection
						.longitudeToPixelX(this.longitude, this.zoomLevel) - this.mapMoveX),
						this.zoomLevel);
			}
			handleTiles(true);
			return true;
		} else if (event.getAction() == MotionEvent.ACTION_UP
				|| event.getAction() == MotionEvent.ACTION_CANCEL) {
			hideZoomControlsDelayed();
			return true;
		}
		// the event was not handled
		return false;
	}

	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		if (!isClickable()) {
			return false;
		}
		if (event.getAction() == MotionEvent.ACTION_MOVE) {
			// calculate the map move
			this.mapMoveX = event.getX() * 40;
			this.mapMoveY = event.getY() * 40;

			synchronized (this) {
				// add the movement to the transformation matrix
				this.matrix.postTranslate(this.mapMoveX, this.mapMoveY);

				// calculate the new position of the map center
				this.latitude = MercatorProjection.pixelYToLatitude((MercatorProjection
						.latitudeToPixelY(this.latitude, this.zoomLevel) - this.mapMoveY),
						this.zoomLevel);
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
	 * Controls the visibility of the zoom controls.
	 * 
	 * @param showZoomControls
	 *            true if the zoom controls should be visible, false otherwise.
	 */
	public void setBuiltInZoomControls(boolean showZoomControls) {
		this.showZoomControls = showZoomControls;
	}

	/**
	 * Set the new size of the file cache. If the cache already contains more items than the new
	 * capacity allows, items are discarded based on the normal file cache policy.
	 * 
	 * @param newCacheSize
	 *            the new capacity of the file cache.
	 * @throws IllegalArgumentException
	 *             if the new capacity is negative.
	 */
	public void setFileCacheSize(int newCacheSize) {
		if (newCacheSize < 0) {
			throw new IllegalArgumentException();
		}
		this.imageFileCache.setCapacity(newCacheSize);
	}

	/**
	 * Controls the visibility of the frame rate.
	 * 
	 * @param showFpsCounter
	 *            true if the map frame rate should be visible, false otherwise.
	 */
	public void setFpsCounter(boolean showFpsCounter) {
		this.showFpsCounter = showFpsCounter;
		// invalidate the MapView
		invalidate();
	}

	public void setMapFile(String newMapFile) {
		if (newMapFile == null) {
			// no map file is given
			return;
		} else if (this.mapFile != null && this.mapFile.equals(newMapFile)) {
			// same map file as before
			return;
		}

		stopMapGenerator();
		this.database.closeFile();
		if (this.database.setFile(newMapFile)) {
			this.mapFile = newMapFile;
			setCenter(this.database.getMapBoundary().getCenter());
			handleTiles(true);
		} else {
			this.mapFile = null;
		}
	}

	/**
	 * Controls the visibility of the map scale.
	 * 
	 * @param showMapScale
	 *            true if the map scale should be visible, false otherwise.
	 */
	public void setMapScale(boolean showMapScale) {
		this.showMapScale = showMapScale;
		if (showMapScale) {
			renderMapScale();
		}
		// invalidate the MapView
		invalidate();
	}

	/**
	 * Set an internal text variable to a certain string.
	 * 
	 * @param name
	 *            the name of the text variable to set.
	 * @param value
	 *            the new value of the text variable.
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

	private byte getValidZoomLevel(byte zoom) {
		if (zoom < ZOOM_MIN) {
			return ZOOM_MIN;
		} else if (zoom > ZOOM_MAX) {
			return ZOOM_MAX;
		} else {
			return zoom;
		}
	}

	private void renderMapScale() {
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
					PAINT_MAP_SCALE_TEXT);
		} else {
			this.mapScaleCanvas.drawText((this.mapScale / 1000) + this.unit_symbol_kilometer,
					10, 15, PAINT_MAP_SCALE_TEXT);
		}
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
	}

	private void setupMapView() {
		this.latitude = Double.NaN;
		this.longitude = Double.NaN;
		this.zoomLevel = DEFAULT_ZOOM_LEVEL;

		setBackgroundColor(MAP_VIEW_BACKGROUND);
		setWillNotDraw(false);
		setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);

		setupZoomControls();
		setupMapScale();

		// create the paint for drawing the FPS text
		this.fpsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		this.fpsPaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		this.fpsPaint.setTextSize(20);

		// create the transformation matrix
		this.matrix = new Matrix();
		this.tileBitmap = Bitmap.createBitmap(Tile.TILE_SIZE, Tile.TILE_SIZE,
				Bitmap.Config.RGB_565);
		this.tileBuffer = ByteBuffer.allocate(Tile.TILE_SIZE_IN_BYTES);

		// create the image bitmap cache
		this.imageBitmapCache = new ImageBitmapCache(BITMAP_CACHE_SIZE);

		// create the image file cache
		this.imageFileCache = new ImageFileCache(System.getProperty("java.io.tmpdir"),
				DEFAULT_FILE_CACHE_SIZE);

		// create the MapController for this MapView
		this.mapController = new MapController(this);

		// create the database
		this.database = new Database();

		// get the MapGenerator from the MapActivity
		this.mapGenerator = this.mapActivity.getMapGenerator();
		this.mapGenerator.setDatabase(this.database);
		this.mapGenerator.setImageCaches(this.imageBitmapCache, this.imageFileCache);
		this.mapGenerator.setMapView(this);

		// get the MapMover from the MapActivity
		this.mapMover = this.mapActivity.getMapMover();
		this.mapMover.setMapView(this);

		// register the MapView in the MapActivity
		this.mapActivity.setMapView(this);
	}

	private void setupZoomControls() {
		// create the ZoomControls and set the click listeners
		this.zoomControls = new ZoomControls(this.mapActivity);
		this.zoomControls.setVisibility(View.GONE);

		// set the click listeners for each zoom button
		this.zoomControls.setOnZoomInClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				zoomIn();
			}
		});
		this.zoomControls.setOnZoomOutClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				zoomOut();
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

	private void showZoomControls() {
		if (this.showZoomControls) {
			this.zoomControlsHideHandler.removeMessages(MSG_ZOOM_CONTROLS_HIDE);
			if (this.zoomControls.getVisibility() != VISIBLE) {
				this.zoomControls.show();
			}
		}
	}

	private void stopMapGenerator() {
		this.mapGenerator.clearJobs();
		while (!this.mapGenerator.isReady()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// restore the interrupted status
				Thread.currentThread().interrupt();
				Logger.e(e);
			}
		}
	}

	@Override
	protected void onDetachedFromWindow() {
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
		if (this.imageBitmapCache != null) {
			this.imageBitmapCache.destroy();
			this.imageBitmapCache = null;
		}

		// destroy the image file cache
		if (this.imageFileCache != null) {
			this.imageFileCache.destroy();
			this.imageFileCache = null;
		}

		// close the map file
		if (this.database != null) {
			this.database.closeFile();
			this.database = null;
		}
	}

	@Override
	protected final void onDraw(Canvas canvas) {
		synchronized (this) {
			// draw the map and the map scale
			canvas.drawBitmap(this.mapViewBitmap1, this.matrix, null);

			if (this.showMapScale) {
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
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if (!changed) {
			// neither size nor position have changed
			return;
		}

		// position the ZoomControls at the bottom right corner
		this.zoomControls.layout(r - this.zoomControls.getMeasuredWidth() - 5, b
				- this.zoomControls.getMeasuredHeight(), r - 5, b);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
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

	void handleTiles(boolean calledByUiThread) {
		if (this.mapFile == null) {
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

			this.mapViewTileX1 = MercatorProjection.pixelXToTileX(this.mapViewPixelX);
			this.mapViewTileY1 = MercatorProjection.pixelYToTileY(this.mapViewPixelY);
			this.mapViewTileX2 = MercatorProjection.pixelXToTileX(this.mapViewPixelX
					+ getWidth());
			this.mapViewTileY2 = MercatorProjection.pixelYToTileY(this.mapViewPixelY
					+ getHeight());

			// go through all tiles that intersect the screen rectangle
			for (this.tileY = this.mapViewTileY2; this.tileY >= this.mapViewTileY1; --this.tileY) {
				for (this.tileX = this.mapViewTileX2; this.tileX >= this.mapViewTileX1; --this.tileX) {
					this.currentTile = new Tile(this.tileX, this.tileY, this.zoomLevel);
					if (this.imageBitmapCache.containsKey(this.currentTile)) {
						// bitmap cache hit
						putTileOnBitmap(this.currentTile, this.imageBitmapCache
								.get(this.currentTile), false);
					} else if (this.imageFileCache.containsKey(this.currentTile)) {
						// file cache hit
						this.imageFileCache.get(this.currentTile, this.tileBuffer);
						this.tileBitmap.copyPixelsFromBuffer(this.tileBuffer);
						putTileOnBitmap(this.currentTile, this.tileBitmap, true);
					} else {
						// cache miss
						this.mapGenerator.addJob(this.currentTile);
					}
				}
			}
		}

		if (this.showMapScale) {
			renderMapScale();
		}

		// invalidate the MapView
		if (calledByUiThread) {
			invalidate();
		} else {
			postInvalidate();
		}

		// notify the map generator to process the job list
		this.mapGenerator.requestSchedule();
	}

	boolean hasValidCenter() {
		if (Double.isNaN(this.latitude) || this.latitude > 90 || this.latitude < -90) {
			return false;
		} else if (Double.isNaN(this.longitude) || this.longitude > 180
				|| this.longitude < -180) {
			return false;
		} else if (this.database == null || this.database.getMapBoundary() == null
				|| !this.database.getMapBoundary().contains(getMapCenter())) {
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

	void putTileOnBitmap(Tile tile, Bitmap bitmap, boolean putToBitmapCache) {
		synchronized (this) {
			// check if the tile and the current MapView rectangle intersect
			if (this.mapViewPixelX - tile.pixelX > Tile.TILE_SIZE
					|| this.mapViewPixelX + getWidth() < tile.pixelX) {
				// no intersection in x direction
				return;
			} else if (this.mapViewPixelY - tile.pixelY > Tile.TILE_SIZE
					|| this.mapViewPixelY + getHeight() < tile.pixelY) {
				// no intersection in y direction
				return;
			}

			// check if the bitmap should go to the image bitmap cache
			if (putToBitmapCache) {
				this.imageBitmapCache.put(tile, bitmap);
			}

			if (!this.matrix.isIdentity()) {
				// change the current MapViewBitmap
				this.mapViewBitmap2.eraseColor(MAP_VIEW_BACKGROUND);
				this.mapViewCanvas.setBitmap(this.mapViewBitmap2);

				// draw the previous MapViewBitmap on the mapViewBitmap
				this.mapViewCanvas.drawBitmap(this.mapViewBitmap1, this.matrix, null);

				// swap the two MapViewBitmaps
				this.swapMapViewBitmap = this.mapViewBitmap1;
				this.mapViewBitmap1 = this.mapViewBitmap2;
				this.mapViewBitmap2 = this.swapMapViewBitmap;

				// reset the matrix
				this.matrix.reset();
			}

			// draw the tile bitmap at the correct position
			this.mapViewCanvas.drawBitmap(bitmap, (float) (tile.pixelX - this.mapViewPixelX),
					(float) (tile.pixelY - this.mapViewPixelY), null);
		}
	}

	void setCenter(GeoPoint point) {
		if (hasValidCenter()) {
			synchronized (this) {
				// add the movement to the transformation matrix
				this.matrix.postTranslate((float) (MercatorProjection.longitudeToPixelX(
						this.longitude, this.zoomLevel) - MercatorProjection.longitudeToPixelX(
						point.getLongitude(), this.zoomLevel)), (float) (MercatorProjection
						.latitudeToPixelY(this.latitude, this.zoomLevel) - MercatorProjection
						.latitudeToPixelY(point.getLatitude(), this.zoomLevel)));
			}
		}
		setCenterAndZoom(point, this.zoomLevel);
	}

	void setCenterAndZoom(GeoPoint point, byte zoom) {
		if (this.database != null && this.database.getMapBoundary() != null
				&& this.database.getMapBoundary().contains(point)) {
			this.latitude = point.getLatitude();
			this.longitude = point.getLongitude();
			this.zoomLevel = getValidZoomLevel(zoom);
			// enable or disable the zoom buttons if necessary
			this.zoomControls.setIsZoomInEnabled(this.zoomLevel != ZOOM_MAX);
			this.zoomControls.setIsZoomOutEnabled(this.zoomLevel != ZOOM_MIN);
			handleTiles(true);
		}
	}

	void setMapFileFromPreferences(String newMapFile) {
		if (newMapFile == null) {
			this.mapFile = null;
		} else if (this.database.setFile(newMapFile)) {
			this.mapFile = newMapFile;
		} else {
			this.mapFile = null;
		}
	}

	Tile setTilePriority(Tile tile) {
		if (tile.zoomLevel != this.zoomLevel) {
			tile.renderPriority = 1000 * Math.abs(tile.zoomLevel - this.zoomLevel);
		} else {
			// calculate the center of the MapView
			double mapViewCenterX = this.mapViewPixelX + (getWidth() >> 1);
			double mapViewCenterY = this.mapViewPixelY + (getHeight() >> 1);

			// calculate the center of the tile
			long tileCenterX = tile.pixelX + (Tile.TILE_SIZE >> 1);
			long tileCenterY = tile.pixelY + (Tile.TILE_SIZE >> 1);

			// set tile priority to the distance from the MapView center
			int diffX = (int) (mapViewCenterX - tileCenterX);
			int diffY = (int) (mapViewCenterY - tileCenterY);
			tile.renderPriority = SquareRoot.sqrt(diffX * diffX + diffY * diffY);
		}
		return tile;
	}

	byte setZoom(byte zoomLevel) {
		this.zoomLevel = getValidZoomLevel(zoomLevel);
		// enable or disable the zoom buttons if necessary
		this.zoomControls.setIsZoomInEnabled(this.zoomLevel != ZOOM_MAX);
		this.zoomControls.setIsZoomOutEnabled(this.zoomLevel != ZOOM_MIN);
		handleTiles(true);
		return this.zoomLevel;
	}

	boolean zoomIn() {
		if (this.zoomLevel < ZOOM_MAX) {
			++this.zoomLevel;
			// enable or disable the zoom buttons if necessary
			this.zoomControls.setIsZoomInEnabled(this.zoomLevel != ZOOM_MAX);
			this.zoomControls.setIsZoomOutEnabled(this.zoomLevel != ZOOM_MIN);
			hideZoomControlsDelayed();
			synchronized (this) {
				this.matrix.postScale(2, 2, getWidth() >> 1, getHeight() >> 1);
			}
			handleTiles(true);
			return true;
		}
		return false;
	}

	boolean zoomOut() {
		if (this.zoomLevel > ZOOM_MIN) {
			--this.zoomLevel;
			// enable or disable the zoom buttons if necessary
			this.zoomControls.setIsZoomInEnabled(this.zoomLevel != ZOOM_MAX);
			this.zoomControls.setIsZoomOutEnabled(this.zoomLevel != ZOOM_MIN);
			hideZoomControlsDelayed();
			synchronized (this) {
				this.matrix.postScale(0.5f, 0.5f, getWidth() >> 1, getHeight() >> 1);
			}
			handleTiles(true);
			return true;
		}
		return false;
	}
}