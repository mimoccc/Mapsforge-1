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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.awt.Font;

//import org.mapsforge.core.content.Context;
//import android.graphics.Bitmap;
//import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.core.graphics.Bitmap;
//import android.graphics.Canvas;
import org.mapsforge.core.graphics.Canvas;
//import android.graphics.Color;
import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
//mport android.graphics.Matrix;
import org.mapsforge.core.graphics.Matrix;
//import android.graphics.Paint;
import org.mapsforge.core.graphics.Paint;
//import android.graphics.Typeface;
import org.mapsforge.core.graphics.Typeface;
//import android.graphics.Bitmap.CompressFormat;
import org.mapsforge.core.graphics.Bitmap.CompressFormat;

//import org.mapsforge.core.os.Message;
//import android.os.SystemClock;
import org.mapsforge.core.os.SystemClock;



/**
 * A MapView shows a map on the display of the device. It handles all user input and touch gestures to
 * move and zoom the map. This MapView also comes with an integrated scale bar, which can be activated
 * via the {@link #setScaleBar(boolean)} method. The built-in zoom controls can be enabled with the
 * {@link #setBuiltInZoomControls(boolean)} method. The {@link #getController()} method returns a
 * <code>MapController</code> to programmatically modify the position and zoom level of the map.
 * <p>
 * This implementation supports offline map rendering as well as downloading map images (tiles) over an
 * Internet connection. All possible operation modes are listed in the {@link MapViewMode} enumeration.
 * The operation mode of a MapView can be set in the constructor and changed at runtime with the
 * {@link #setMapViewMode(MapViewMode)} method. Some MapView parameters like the maximum possible zoom
 * level or the default starting point depend on the selected operation mode.
 * <p>
 * In offline rendering mode a special database file is required which contains the map data. Such map
 * files can be stored in any readable folder. The current map file for a MapView is set by calling the
 * {@link #setMapFile(String)} method. To retrieve a <code>MapDatabase</code> that returns some metadata
 * about the map file, use the {@link #getMapDatabase()} method.
 * <p>
 * Map tiles are automatically cached in a separate directory on the memory card. The size of this cache
 * may be adjusted via the {@link #setMemoryCardCacheSize(int)} method. The
 * {@link MapView#setMemoryCardCachePersistence(boolean)} method sets the cache persistence.
 * <p>
 * {@link Overlay Overlays} can be used to display geographical data such as points and ways. To draw an
 * overlay on top of the map, add it to the list returned by {@link #getOverlays()}. Overlays may be
 * added or removed from the list at any time.
 * <p>
 * All text fields from the {@link TextField} enumeration can be overridden at runtime via the
 * {@link #setText(TextField, String)} method. The default texts are in English.
 */
public class MapView {
	/**
	 * Enumeration of all text fields that can be overridden at runtime via the
	 * {@link MapView#setText(TextField, String)} method.
	 */
	public enum TextField {
		/**
		 * Unit symbol kilometer.
		 */
		KILOMETER,

		/**
		 * Unit symbol meter.
		 */
		METER,

		/**
		 * OK text message.
		 */
		OKAY;
	}

	/**
	 * Implementation for single-touch capable devices.
	 */

	

	/**
	 * Default operation mode of a MapView if no other mode is specified.
	 */
	private static final MapViewMode DEFAULT_MAP_VIEW_MODE = MapViewMode.CANVAS_RENDERER;

	/**
	 * Default move speed factor of the map, used for trackball and keyboard events.
	 */
	private static final int DEFAULT_MOVE_SPEED = 10;

	/**
	 * Default value for the kilometer text field.
	 */
	private static final String DEFAULT_TEXT_KILOMETER = " km";

	/**
	 * Default value for the meter text field.
	 */
	private static final String DEFAULT_TEXT_METER = " m";

	/**
	 * Default value for the OK text field.
	 */
	private static final String DEFAULT_TEXT_OK = "OK";

	/**
	 * Default text scale for the map rendering.
	 */
	private static final float DEFAULT_TEXT_SCALE = 1;

	/**
	 * Default capacity of the memory card cache.
	 */
	private static final int DEFAULT_TILE_MEMORY_CARD_CACHE_SIZE = 100;

	/**
	 * Default minimum zoom level.
	 */
	private static final byte DEFAULT_ZOOM_LEVEL_MIN = 0;

	/**
	 * Names which are used to detect the Android emulator from the SDK.
	 */
	private static final String[] EMULATOR_NAMES = { "google_sdk", "sdk" };

	/**
	 * Path to the caching folder on the external storage.
	 */
	private static final String EXTERNAL_STORAGE_DIRECTORY = File.separatorChar + "mapsforge";

	/**
	 * Default background color of the MapView.
	 */
	//private static final int MAP_VIEW_BACKGROUND = Color.rgb(238, 238, 238);
	private static final Color MAP_VIEW_BACKGROUND = new Color(238, 238, 238);

	/**
	 * Message code for the handler to hide the zoom controls.
	 */
	private static final int MSG_ZOOM_CONTROLS_HIDE = 0;
	private static final Paint PAINT_SCALE_BAR = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_SCALE_BAR_STROKE = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_SCALE_BAR_TEXT = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_SCALE_BAR_TEXT_WHITE_STROKE = new Paint(
			Paint.ANTI_ALIAS_FLAG);
	private static final short SCALE_BAR_HEIGHT = 35;
	private static final int[] SCALE_BAR_VALUES = { 10000000, 5000000, 2000000, 1000000,
			500000, 200000, 100000, 50000, 20000, 10000, 5000, 2000, 1000, 500, 200, 100, 50,
			20, 10, 5, 2, 1 };

	private static final short SCALE_BAR_WIDTH = 130;

	/**
	 * Capacity of the RAM cache.
	 */
	private static final int TILE_RAM_CACHE_SIZE = 16;

	/**
	 * Constant move speed factor for trackball events.
	 */
	private static final float TRACKBALL_MOVE_SPEED = 40;

	/**
	 * Delay in milliseconds after which the zoom controls disappear.
	 */
	//private static final long ZOOM_CONTROLS_TIMEOUT = ViewConfiguration
	//		.getZoomControlsTimeout();

	/**
	 * Minimum possible zoom level.
	 */
	private static final byte ZOOM_LEVEL_MIN = 0;

	/**
	 * Maximum possible latitude value of the map.
	 */
	static final double LATITUDE_MAX = 85.05113;

	/**
	 * Minimum possible latitude value of the map.
	 */
	static final double LATITUDE_MIN = -85.05113;

	/**
	 * Maximum possible longitude value of the map.
	 */
	static final double LONGITUDE_MAX = 180;

	/**
	 * Minimum possible longitude value of the map.
	 */
	static final double LONGITUDE_MIN = -180;

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
	 * @param file
	 *            the path to the map file that should be tested.
	 * @return true if the file is a valid map file, false otherwise.
	 */
	public static boolean isValidMapFile(String file) {
		MapDatabase testDatabase = new MapDatabase();
		boolean isValid = testDatabase.openFile(file);
		testDatabase.closeFile();
		return isValid;
	}

	private boolean attachedToWindow;
	private MapGeneratorJob currentJob;
	private Tile currentTile;
	private long currentTime;
	private boolean drawTileCoordinates;
	private boolean drawTileFrames;
	private int fps;
	private Paint fpsPaint;
	private short frame_counter;
	private boolean highlightWaterTiles;
	private double latitude;
	private double longitude;
	private MapActivity mapActivity;
	private MapController mapController;
	private MapDatabase mapDatabase;
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
	private Bitmap mapViewBitmapSwap;
	private Canvas mapViewCanvas;
	private final int mapViewId;
	private MapViewMode mapViewMode;
	private double mapViewPixelX;
	private double mapViewPixelY;
	private long mapViewTileX1;
	private long mapViewTileX2;
	private long mapViewTileY1;
	private long mapViewTileY2;
	private Matrix matrix;
	private float matrixScaleFactor;
	private float matrixTranslateX;
	private float matrixTranslateY;
	private double meterPerPixel;
	private float moveSpeedFactor;
	private int numberOfTiles;
	private boolean persistence;
	private long previousTime;
	private Projection projection;
	private boolean showFpsCounter;
	private boolean showScaleBar;
	private boolean showZoomControls;
	private String text_kilometer;
	private String text_meter;
	private String text_ok;
	private float textScale;
	private Bitmap tileBitmap;
	private ByteBuffer tileBuffer;
	private TileMemoryCardCache tileMemoryCardCache;
	private int tileMemoryCardCacheSize;
	private TileRAMCache tileRAMCache;
	private long tileX;
	private long tileY;
	//private TouchEventHandler touchEventHandler;
	private ZoomAnimator zoomAnimator;
	//private ZoomControls zoomControls;
	private byte zoomLevel;
	private byte zoomLevelMax;
	private byte zoomLevelMin;

	/**
	 * Thread-safe overlay list. It is necessary to manually synchronize on this list when iterating
	 * over it.
	 */
	List<Overlay> overlays;

	/**
	 * Constructs a new MapView with the default {@link MapViewMode}.
	 * 
	 * @param context
	 *            the enclosing MapActivity instance.
	 * @throws IllegalArgumentException
	 *             if the context object is not an instance of {@link MapActivity}.
	 */
	public MapView() {
		//MAP ACTIVITY:this.mapActivity = (MapActivity) context;
		
		this.mapViewMode = DEFAULT_MAP_VIEW_MODE;
		this.mapViewId = 1;//this.mapActivity.getMapViewId();
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
	public synchronized GeoPoint getMapCenter() {
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
		return this.mapDatabase;
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
	 * Returns the maximum zoom level which is supported by the currently selected {@link MapViewMode}
	 * of the MapView.
	 * 
	 * @return the maximum possible zoom level.
	 */
	public int getMaxZoomLevel() {
		return this.mapGenerator.getMaxZoomLevel();
	}

	/**
	 * Returns the move speed of the map, used for trackball and keyboard events.
	 * 
	 * @return the factor by which the move speed of the map will be multiplied.
	 */
	public float getMoveSpeed() {
		return this.moveSpeedFactor;
	}

	/**
	 * Returns the thread-safe list of overlays for this MapView. It is necessary to manually
	 * synchronize on this list when iterating over it.
	 * 
	 * @return the overlay list.
	 */
	public final List<Overlay> getOverlays() {
		return this.overlays;
	}

	/**
	 * Returns the projection that is currently in use to convert pixel coordinates to geographical
	 * coordinates on the map.
	 * 
	 * @return The projection of the MapView. Do not keep this object for a longer time.
	 */
	public Projection getProjection() {
		return this.projection;
	}

	/**
	 * Returns the current zoom level of the map.
	 * 
	 * @return the current zoom level.
	 */
	public byte getZoomLevel() {
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

	/**
	 * Makes a screenshot of the currently visible map and saves it as compressed image. Zoom buttons,
	 * scale bar, overlays, menus and the title bar are not included in the screenshot.
	 * 
	 * @param fileName
	 *            the name of the image file. If the file exists, it will be overwritten.
	 * @param format
	 *            the file format of the compressed image.
	 * @param quality
	 *            value from 0 (low) to 100 (high). Has no effect on some formats like PNG.
	 * @return true if the image was saved successfully, false otherwise.
	 * @throws IOException
	 *             if an error occurs while writing the file.
	 */
	public boolean makeScreenshot(CompressFormat format, int quality, String fileName)
			throws IOException {
		FileOutputStream outputStream = new FileOutputStream(fileName);
		boolean success;
		synchronized (this.matrix) {
			success = this.mapViewBitmap1.compress(format, quality, outputStream);
		}
		outputStream.close();
		return success;
	}

	public boolean onKeyDown() {
		//TODO
		return false;
	}

	public boolean onKeyUp() {
		//TODO
		return false;
	}

	public boolean onTouchEvent() {
		//TODO
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
	 * <p>
	 * This method is for debugging purposes only.
	 * 
	 * @param showFpsCounter
	 *            true if the map frame rate should be visible, false otherwise.
	 */
	public void setFpsCounter(boolean showFpsCounter) {
		this.showFpsCounter = showFpsCounter;
		// invalidate the MapView
		/*getMapActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				invalidate();
			}
		});*/
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
			System.out.println("DRIN");
			throw new UnsupportedOperationException();
		}
		if (newMapFile == null) {
			// no map file is given
			System.out.println("NO MAP");
			return;
		} else if (this.mapFile != null && this.mapFile.equals(newMapFile)) {
			// same map file as before
			System.out.println("SAME MAP");
			return;
		} else if (this.mapDatabase == null) {
			System.out.println("NO DATABASE");
			return;
		}

		this.mapMover.pause();
		this.mapGenerator.pause();

		waitForZoomAnimator();
		waitForMapMover();
		waitForMapGenerator();

		this.mapMover.stopMove();
		this.mapGenerator.clearJobs();

		this.mapMover.unpause();
		this.mapGenerator.unpause();

		this.mapDatabase.closeFile();
		if (this.mapDatabase.openFile(newMapFile)) {
			((DatabaseMapGenerator) this.mapGenerator).onMapFileChange();
			this.mapFile = newMapFile;
			clearMapView();
			setCenter(getDefaultStartPoint());
			handleTiles(true);
		} else {
			this.mapFile = null;
			clearMapView();
			getMapActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					invalidate();
				}
			});
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
	 * Sets the persistence of the memory card cache. If set to true, cached image files will not be
	 * deleted when the MapView gets destroyed. The default value is false.
	 * 
	 * @param persistence
	 *            the new persistence of the memory card cache.
	 */
	public void setMemoryCardCachePersistence(boolean persistence) {
		this.persistence = persistence;
	}

	/**
	 * Sets the new size of the memory card cache. If the cache already contains more items than the new
	 * capacity allows, items are discarded based on the cache policy.
	 * 
	 * @param newCacheSize
	 *            the new capacity of the memory card cache.
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
	 * Sets the move speed of the map, used for trackball and keyboard events.
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
		/*getMapActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				invalidate();
			}
		});*/
	}

	/**
	 * Overrides the specified text field with the given string.
	 * 
	 * @param textField
	 *            the text field to override.
	 * @param value
	 *            the new value of the text field.
	 */
	public void setText(TextField textField, String value) {
		switch (textField) {
			case KILOMETER:
				this.text_kilometer = value;
				break;
			case METER:
				this.text_meter = value;
				break;
			case OKAY:
				this.text_ok = value;
				break;
			default:
				// all cases are covered, the default case should never occur
				break;
		}
	}

	/**
	 * Sets the text scale for the map rendering. Has no effect in downloading mode.
	 * 
	 * @param textScale
	 *            the new text scale for the map rendering.
	 */
	public void setTextScale(float textScale) {
		this.textScale = textScale;
		this.mapGenerator.clearJobs();
		clearMapView();
		handleTiles(true);
	}

	/**
	 * Sets the drawing of tile coordinates for debugging. Has no effect in downloading mode.
	 * <p>
	 * This method is for debugging purposes only.
	 * 
	 * @param drawTileCoordinates
	 *            true if tile coordinates should be drawn, false otherwise.
	 */
	public void setTileCoordinates(boolean drawTileCoordinates) {
		this.drawTileCoordinates = drawTileCoordinates;
		this.mapGenerator.clearJobs();
		clearMapView();
		handleTiles(true);
	}

	/**
	 * Sets the drawing of tile frames for debugging. Has no effect in downloading mode.
	 * <p>
	 * This method is for debugging purposes only.
	 * 
	 * @param drawTileFrames
	 *            true if tile frames should be drawn, false otherwise.
	 */
	public void setTileFrames(boolean drawTileFrames) {
		this.drawTileFrames = drawTileFrames;
		this.mapGenerator.clearJobs();
		clearMapView();
		handleTiles(true);
	}

	/**
	 * Sets the highlighting of water tiles. Has no effect in downloading mode.
	 * <p>
	 * This method is for debugging purposes only.
	 * 
	 * @param highlightWaterTiles
	 *            true if water tiles should be highlighted, false otherwise.
	 */
	public void setWaterTiles(boolean highlightWaterTiles) {
		this.highlightWaterTiles = highlightWaterTiles;
		this.mapGenerator.clearJobs();
		clearMapView();
		handleTiles(true);
	}

	/**
	 * Sets the maximum zoom level of the map to which the user may zoom in.
	 * <p>
	 * The maximum possible zoom level of the MapView depends also on the currently selected
	 * {@link MapViewMode}. For example, downloading map tiles may only be possible up to a certain zoom
	 * level. Setting a higher maximum zoom level has no effect in this case.
	 * 
	 * @param zoomLevelMax
	 *            the maximum zoom level.
	 * @throws IllegalArgumentException
	 *             if the maximum zoom level is smaller than the current minimum zoom level.
	 */
	public void setZoomMax(byte zoomLevelMax) {
		if (zoomLevelMax < this.zoomLevelMin) {
			throw new IllegalArgumentException();
		}
		this.zoomLevelMax = zoomLevelMax;
	}

	/**
	 * Sets the minimum zoom level of the map to which the user may zoom out.
	 * 
	 * @param zoomLevelMin
	 *            the minimum zoom level.
	 * @throws IllegalArgumentException
	 *             if the minimum zoom level is larger than the current maximum zoom level.
	 */
	public void setZoomMin(byte zoomLevelMin) {
		if (zoomLevelMin > this.zoomLevelMax) {
			throw new IllegalArgumentException();
		}
		this.zoomLevelMin = (byte) Math.max(zoomLevelMin, ZOOM_LEVEL_MIN);
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

	/**
	 * Returns the minimum of the maximum zoom level set via {@link #setZoomMax(byte)} and the maximum
	 * zoom level which is supported by the currently selected {@link MapViewMode}.
	 * 
	 * @return the maximum possible zoom level.
	 */
	private byte getMaximumPossibleZoomLevel() {
		return (byte) Math.min(this.zoomLevelMax, this.mapGenerator.getMaxZoomLevel());
	}

	/**
	 * Returns the given zoom level limited to the minimum and maximum possible zoom level.
	 * 
	 * @param zoom
	 *            the zoom level which should be limited.
	 * @return a valid zoom level from the interval [minimum, maximum].
	 */
	private byte getValidZoomLevel(byte zoom) {
		if (zoom < this.zoomLevelMin) {
			return this.zoomLevelMin;
		} else if (zoom > getMaximumPossibleZoomLevel()) {
			return getMaximumPossibleZoomLevel();
		}
		return zoom;
	}

	private void renderScaleBar() {
		synchronized (this) {
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
		}

		for (int i = 0; i < SCALE_BAR_VALUES.length; ++i) {
			this.mapScale = SCALE_BAR_VALUES[i];
			this.mapScaleLength = this.mapScale / (float) this.meterPerPixel;
			if (this.mapScaleLength < (SCALE_BAR_WIDTH - 10)) {
				break;
			}
		}

		// fill the bitmap with transparent color
		//this.mapScaleBitmap.eraseColor(Color.TRANSPARENT);
		this.mapScaleBitmap.eraseColor(Color.TRANSLUCENT);

		// draw the map scale
		this.mapScaleCanvas
				.drawLine(7, 20, this.mapScaleLength + 3, 20, PAINT_SCALE_BAR_STROKE);
		this.mapScaleCanvas.drawLine(5, 10, 5, 30, PAINT_SCALE_BAR_STROKE);
		this.mapScaleCanvas.drawLine(this.mapScaleLength + 5, 10, this.mapScaleLength + 5, 30,
				PAINT_SCALE_BAR_STROKE);
		this.mapScaleCanvas.drawLine(7, 20, this.mapScaleLength + 3, 20, PAINT_SCALE_BAR);
		this.mapScaleCanvas.drawLine(5, 10, 5, 30, PAINT_SCALE_BAR);
		this.mapScaleCanvas.drawLine(this.mapScaleLength + 5, 10, this.mapScaleLength + 5, 30,
				PAINT_SCALE_BAR);

		// draw the scale text
		if (this.mapScale < 1000) {
			this.mapScaleCanvas.drawText(this.mapScale + getText(TextField.METER), 10, 15,
					PAINT_SCALE_BAR_TEXT_WHITE_STROKE);
			this.mapScaleCanvas.drawText(this.mapScale + getText(TextField.METER), 10, 15,
					PAINT_SCALE_BAR_TEXT);
		} else {
			this.mapScaleCanvas.drawText((this.mapScale / 1000) + getText(TextField.KILOMETER),
					10, 15, PAINT_SCALE_BAR_TEXT_WHITE_STROKE);
			this.mapScaleCanvas.drawText((this.mapScale / 1000) + getText(TextField.KILOMETER),
					10, 15, PAINT_SCALE_BAR_TEXT);
		}
	}

	private void setupFpsText() {
		// create the paint1 for drawing the FPS text
		this.fpsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		//this.fpsPaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		this.fpsPaint.setFont(new Font(Font.SERIF, Font.PLAIN, 14));
		this.fpsPaint.setTextSize(20);
	}

	private void setupMapScale() {
		// create the bitmap for the map scale and the canvas to draw on it
		this.mapScaleBitmap = Bitmap.createBitmap(SCALE_BAR_WIDTH, SCALE_BAR_HEIGHT,
				Bitmap.Config.ARGB_4444);
		this.mapScaleCanvas = new Canvas(this.mapScaleBitmap);

		// set the default text fields for the map scale
		setText(TextField.KILOMETER, DEFAULT_TEXT_KILOMETER);
		setText(TextField.METER, DEFAULT_TEXT_METER);
		setText(TextField.OKAY, DEFAULT_TEXT_OK);

		// set up the paints to draw the map scale
		PAINT_SCALE_BAR.setStrokeWidth(2);
		PAINT_SCALE_BAR.setStrokeCap(Paint.Cap.SQUARE);
		PAINT_SCALE_BAR.setColor(Color.BLACK);
		PAINT_SCALE_BAR_STROKE.setStrokeWidth(5);
		PAINT_SCALE_BAR_STROKE.setStrokeCap(Paint.Cap.SQUARE);
		PAINT_SCALE_BAR_STROKE.setColor(Color.WHITE);

		//Typeface.init();
		//PAINT_SCALE_BAR_TEXT.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		PAINT_SCALE_BAR_TEXT.setFont(new Font(Font.SERIF, Font.PLAIN, 14));
		PAINT_SCALE_BAR_TEXT.setTextSize(14);
		PAINT_SCALE_BAR_TEXT.setColor(Color.BLACK);
		
		//PAINT_SCALE_BAR_TEXT_WHITE_STROKE.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		PAINT_SCALE_BAR_TEXT_WHITE_STROKE.setFont(new Font(Font.SERIF, Font.PLAIN, 14));
		//PAINT_SCALE_BAR_TEXT_WHITE_STROKE.setStyle(Paint.Style.STROKE);
		//PAINT_SCALE_BAR_TEXT_WHITE_STROKE.setStrokeWidth(3);
		PAINT_SCALE_BAR_TEXT_WHITE_STROKE.setTextSize(14);
		PAINT_SCALE_BAR_TEXT_WHITE_STROKE.setColor(Color.WHITE);
	}

	private synchronized void setupMapView() {

		this.moveSpeedFactor = DEFAULT_MOVE_SPEED;
		this.textScale = DEFAULT_TEXT_SCALE;

		//TODO VIEW setBackgroundColor(MAP_VIEW_BACKGROUND);
		setWillNotDraw(false);
		//setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);

		setupZoomControls();
		setupMapScale();
		setupFpsText();

		// create the projection
		this.projection = new MercatorProjection(this);

		// create the transformation matrix
		this.matrix = new Matrix();

		// create the tile bitmap and buffer
		this.tileBitmap = Bitmap.createBitmap(Tile.TILE_SIZE, Tile.TILE_SIZE,
				Bitmap.Config.RGB_565);
		this.tileBuffer = ByteBuffer.allocate(Tile.TILE_SIZE_IN_BYTES);

		// create the image bitmap cache
		this.tileRAMCache = new TileRAMCache(TILE_RAM_CACHE_SIZE);

		// create the image file cache with a unique directory
		/*this.tileMemoryCardCache = new TileMemoryCardCache(Environment
				.getExternalStorageDirectory().getAbsolutePath()
				+ EXTERNAL_STORAGE_DIRECTORY + File.separatorChar + this.mapViewId,
				this.tileMemoryCardCacheSize);*/

		// create the MapController for this MapView
		this.mapController = new MapController(this);

		// create the database
		this.mapDatabase = new MapDatabase();

		startMapGeneratorThread();

		// set the default position and zoom level of the map
		/*GeoPoint defaultStartPoint = this.mapGenerator.getDefaultStartPoint();
		this.latitude = defaultStartPoint.getLatitude();
		this.longitude = defaultStartPoint.getLongitude();
		this.zoomLevel = this.mapGenerator.getDefaultZoomLevel();
		this.zoomLevelMin = DEFAULT_ZOOM_LEVEL_MIN;
		this.zoomLevelMax = Byte.MAX_VALUE;

		// create and start the MapMover thread
		this.mapMover = new MapMover();
		this.mapMover.setMapView(this);
		this.mapMover.start();

		// create and start the ZoomAnimator thread
		this.zoomAnimator = new ZoomAnimator();
		this.zoomAnimator.setMapView(this);
		this.zoomAnimator.start();

		// register the MapView in the MapActivity
		this.mapActivity.registerMapView(this);*/
	}



	private void setupZoomControls() {
		//TODO
	}

	/**
	 * Creates and starts the MapGenerator thread.
	 */
	private void startMapGeneratorThread() {
		switch (this.mapViewMode) {
			case CANVAS_RENDERER:
				this.mapGenerator = new CanvasRenderer();
				((DatabaseMapGenerator) this.mapGenerator).setDatabase(this.mapDatabase);
				break;
			case MAPNIK_TILE_DOWNLOAD:
				this.mapGenerator = new MapnikTileDownload();
				break;
			case OPENCYCLEMAP_TILE_DOWNLOAD:
				this.mapGenerator = new OpenCycleMapTileDownload();
				break;
			case OSMARENDER_TILE_DOWNLOAD:
				this.mapGenerator = new OsmarenderTileDownload();
				break;
			default:
				// all cases are covered, the default case should never occur
				throw new RuntimeException("invalid mapViewMode: " + this.mapViewMode);
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

	private void waitForMapGenerator() {
		synchronized (this) {
			while (!this.mapGenerator.isReady()) {
				try {
					wait(50);
				} catch (InterruptedException e) {
					// restore the interrupted status
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	private void waitForMapMover() {
		synchronized (this) {
			while (!this.mapMover.isReady()) {
				try {
					wait(50);
				} catch (InterruptedException e) {
					// restore the interrupted status
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	private void waitForZoomAnimator() {
		synchronized (this) {
			while (this.zoomAnimator.isExecuting()) {
				try {
					wait(50);
				} catch (InterruptedException e) {
					// restore the interrupted status
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	protected void onAttachedToWindow() {
		this.attachedToWindow = true;
		if (this.mapGenerator != null) {
			this.mapGenerator.onAttachedToWindow();
		}
	}

	protected void onDetachedFromWindow() {
		this.attachedToWindow = false;
		if (this.mapGenerator != null) {
			this.mapGenerator.onDetachedFromWindow();
		}
	}

	//@Override
	protected final void onDraw(Canvas canvas) {
		if (this.mapViewBitmap1 == null) {
			return;
		}

		// draw the map
		synchronized (this.matrix) {
			canvas.drawBitmap(this.mapViewBitmap1, this.matrix, null);
			// draw the overlays
			synchronized (this.overlays) {
				for (Overlay overlay : this.overlays) {
					overlay.draw(canvas);
				}
			}
		}

		// draw the scale bar
		if (this.showScaleBar) {
			canvas.drawBitmap(this.mapScaleBitmap, 5, getHeight() - SCALE_BAR_HEIGHT - 5, null);
		}

		// draw the FPS counter
		if (this.showFpsCounter) {
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

	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		//TODO
	}

	protected final void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		//TODO
	}

	protected synchronized void onSizeChanged(int w, int h, int oldw, int oldh) {
		// check if the previous MapView bitmaps must be recycled
		if (this.mapViewBitmap1 != null) {
			this.mapViewBitmap1.recycle();
		}
		if (this.mapViewBitmap2 != null) {
			this.mapViewBitmap2.recycle();
		}

		// check if the new dimensions are positive
		if (w > 0 && h > 0) {
			// calculate how many tiles are needed to fill the MapView completely
			this.numberOfTiles = ((w / Tile.TILE_SIZE) + 1) * ((h / Tile.TILE_SIZE) + 1);

			// create the new MapView bitmaps
			this.mapViewBitmap1 = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
			this.mapViewBitmap2 = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);

			// create the canvas
			this.mapViewBitmap1.eraseColor(MAP_VIEW_BACKGROUND);
			this.mapViewCanvas = new Canvas(this.mapViewBitmap1);
			handleTiles(true);

			// set up the overlays
			synchronized (this.overlays) {
				for (Overlay overlay : this.overlays) {
					overlay.onSizeChanged();
				}
			}
		}
	}

	/**
	 * Called by the enclosing {@link MapActivity} when the MapView is no longer needed.
	 */
	void destroy() {
		// unregister the MapView in the MapActivity
		if (this.mapActivity != null) {
			this.mapActivity.unregisterMapView(this);
			this.mapActivity = null;
		}

		// stop the overlay threads
		if (this.overlays != null) {
			synchronized (this.overlays) {
				for (Overlay overlay : this.overlays) {
					overlay.interrupt();
				}
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

		// stop the ZoomAnimator thread
		if (this.zoomAnimator != null) {
			this.zoomAnimator.interrupt();
			try {
				this.zoomAnimator.join();
			} catch (InterruptedException e) {
				// restore the interrupted status
				Thread.currentThread().interrupt();
			}
			this.zoomAnimator = null;
		}

		stopMapGeneratorThread();

		// destroy the map controller to avoid memory leaks
		this.mapController = null;

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
		this.mapViewBitmapSwap = null;

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
			this.tileMemoryCardCache.destroy(this.persistence);
			this.tileMemoryCardCache = null;
		}

		// close the map file
		if (this.mapDatabase != null) {
			this.mapDatabase.closeFile();
			this.mapDatabase = null;
		}
	}

	/**
	 * Returns the default starting point for the map, which depends on the currently selected operation
	 * mode of the MapView.
	 * 
	 * @return the default starting point.
	 */
	GeoPoint getDefaultStartPoint() {
		return this.mapGenerator.getDefaultStartPoint();
	}

	/**
	 * Returns the default zoom level for the map, which depends on the currently selected operation
	 * mode of the MapView.
	 * 
	 * @return the default zoom level.
	 */
	byte getDefaultZoomLevel() {
		return this.mapGenerator.getDefaultZoomLevel();
	}

	/**
	 * Returns the enclosing MapActivity of the MapView.
	 * 
	 * @return the enclosing MapActivity.
	 */
	MapActivity getMapActivity() {
		return this.mapActivity;
	}

	/**
	 * Returns the current value of the given text field.
	 * 
	 * @param textField
	 *            the text field whose value should be returned.
	 * @return the current value of the text field (may be null).
	 */
	String getText(TextField textField) {
		switch (textField) {
			case KILOMETER:
				return this.text_kilometer;
			case METER:
				return this.text_meter;
			case OKAY:
				return this.text_ok;
			default:
				// all cases are covered, the default case should never occur
				return null;
		}
	}

	/**
	 * Makes sure that the given latitude value is within the possible range.
	 * 
	 * @param lat
	 *            the latitude value that should be checked.
	 * @return a valid latitude value.
	 */
	double getValidLatitude(double lat) {
		if (lat < LATITUDE_MIN) {
			return LATITUDE_MIN;
		} else if (lat > LATITUDE_MAX) {
			return LATITUDE_MAX;
		}
		return lat;
	}

	/**
	 * Returns the ZoomAnimator of this MapView.
	 * 
	 * @return the ZoomAnimator of this MapView.
	 */
	ZoomAnimator getZoomAnimator() {
		return this.zoomAnimator;
	}

	/**
	 * Calculates all necessary tiles and adds jobs accordingly.
	 * 
	 * @param calledByUiThread
	 *            true if called from the UI thread, false otherwise.
	 */
	void handleTiles(boolean calledByUiThread) {
		if (this.getWidth() == 0) {
			return;
		}

		synchronized (this.overlays) {
			for (Overlay overlay : this.overlays) {
				overlay.requestRedraw();
			}
		}

		if (!this.mapViewMode.requiresInternetConnection() && this.mapFile == null) {
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
							this.mapFile, this.textScale, this.drawTileFrames,
							this.drawTileCoordinates, this.highlightWaterTiles);
					if (this.tileRAMCache.containsKey(this.currentJob)) {
						// bitmap cache hit
						putTileOnBitmap(this.currentJob, this.tileRAMCache.get(this.currentJob));
					} else if (this.tileMemoryCardCache.containsKey(this.currentJob)) {
						// memory card cache hit
						if (this.tileMemoryCardCache.get(this.currentJob, this.tileBuffer)) {
							//TODO this.tileBitmap.copyPixelsFromBuffer(this.tileBuffer);
						
							this.tileBitmap = new Bitmap((BufferedImage) Toolkit.getDefaultToolkit().createImage(this.tileBuffer.array()));
							putTileOnBitmap(this.currentJob, this.tileBitmap);
							this.tileRAMCache.put(this.currentJob, this.tileBitmap);
						} else {
							// the image data could not be read from the cache
							this.mapGenerator.addJob(this.currentJob);
						}
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





	/**
	 * Checks if the map currently has a valid center position.
	 * 
	 * @return true if the current center position of the map is valid, false otherwise.
	 */
	synchronized boolean hasValidCenter() {
		if (Double.isNaN(this.latitude) || this.latitude > LATITUDE_MAX
				|| this.latitude < LATITUDE_MIN) {
			return false;
		} else if (Double.isNaN(this.longitude) || this.longitude > LONGITUDE_MAX
				|| this.longitude < LONGITUDE_MIN) {
			return false;
		} else if (!this.mapViewMode.requiresInternetConnection()
				&& (this.mapDatabase == null || this.mapDatabase.getMapBoundary() == null || !this.mapDatabase
						.getMapBoundary().contains(getMapCenter().getLongitudeE6(),
								getMapCenter().getLatitudeE6()))) {
			return false;
		}
		return true;
	}

	/**
	 * Displays the zoom controls for a short time.
	 */
	void hideZoomControlsDelayed() {
		//TODO
	}

	/**
	 * Hides the zoom controls immediately.
	 */
	void hideZoomZontrols() {
		//TODO
	}

	/**
	 * @return true if the matrix is the identity matrix, false otherwise.
	 */
	boolean matrixIsIdentity() {
		synchronized (this.matrix) {
			return this.matrix.isIdentity();
		}
	}

	/**
	 * Scales the matrix of the MapView and all its overlays.
	 * 
	 * @param sx
	 *            the horizontal scale.
	 * @param sy
	 *            the vertical scale.
	 * @param px
	 *            the horizontal pivot point.
	 * @param py
	 *            the vertical pivot point.
	 */
	void matrixPostScale(float sx, float sy, float px, float py) {
		synchronized (this.matrix) {
			this.matrix.postScale(sx, sy, px, py);
			synchronized (MapView.this.overlays) {
				for (Overlay overlay : MapView.this.overlays) {
					overlay.matrixPostScale(sx, sy, px, py);
				}
			}
		}
	}

	/**
	 * Translates the matrix of the MapView and all its overlays.
	 * 
	 * @param dx
	 *            the horizontal translation.
	 * @param dy
	 *            the vertical translation.
	 */
	void matrixPostTranslate(float dx, float dy) {
		synchronized (this.matrix) {
			this.matrix.postTranslate(dx, dy);
			synchronized (MapView.this.overlays) {
				for (Overlay overlay : MapView.this.overlays) {
					overlay.matrixPostTranslate(dx, dy);
				}
			}
		}
	}

	/**
	 * Moves the map by the given amount of pixels.
	 * 
	 * @param moveHorizontal
	 *            the amount of pixels to move the map horizontally.
	 * @param moveVertical
	 *            the amount of pixels to move the map vertically.
	 */
	synchronized void moveMap(float moveHorizontal, float moveVertical) {
		this.longitude = MercatorProjection.pixelXToLongitude(MercatorProjection
				.longitudeToPixelX(this.longitude, this.zoomLevel)
				- moveHorizontal, this.zoomLevel);
		this.latitude = getValidLatitude(MercatorProjection.pixelYToLatitude(MercatorProjection
				.latitudeToPixelY(this.latitude, this.zoomLevel)
				- moveVertical, this.zoomLevel));
	}

	/**
	 * Called by the enclosing activity when {@link MapActivity#onPause()} is executed.
	 */
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

	/**
	 * Called by the enclosing activity when {@link MapActivity#onResume()} is executed.
	 */
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
	 * Draws a tile bitmap at the right position on the MapView bitmap.
	 * 
	 * @param mapGeneratorJob
	 *            the job with the tile.
	 * @param bitmap
	 *            the bitmap to be drawn.
	 */
	synchronized void putTileOnBitmap(MapGeneratorJob mapGeneratorJob, Bitmap bitmap) {
		//TODO
	}

	/**
	 * This method is called by the MapGenerator when its job queue is empty.
	 */
	void requestMoreJobs() {
		if (!this.mapViewMode.requiresInternetConnection() && this.mapFile == null) {
			return;
		//} else if (this.getWidth() == 0) {
			//return;
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
						this.mapFile, this.textScale, this.drawTileFrames,
						this.drawTileCoordinates, this.highlightWaterTiles);
				if (!this.tileMemoryCardCache.containsKey(this.currentJob)) {
					// cache miss
					this.mapGenerator.addJob(this.currentJob);
				}

				this.currentTile = new Tile(this.tileX, this.mapViewTileY1 - 1, this.zoomLevel);
				this.currentJob = new MapGeneratorJob(this.currentTile, this.mapViewMode,
						this.mapFile, this.textScale, this.drawTileFrames,
						this.drawTileCoordinates, this.highlightWaterTiles);
				if (!this.tileMemoryCardCache.containsKey(this.currentJob)) {
					// cache miss
					this.mapGenerator.addJob(this.currentJob);
				}
			}

			// tiles left and right from the visible area
			for (this.tileY = this.mapViewTileY2; this.tileY >= this.mapViewTileY1; --this.tileY) {
				this.currentTile = new Tile(this.mapViewTileX2 + 1, this.tileY, this.zoomLevel);
				this.currentJob = new MapGeneratorJob(this.currentTile, this.mapViewMode,
						this.mapFile, this.textScale, this.drawTileFrames,
						this.drawTileCoordinates, this.highlightWaterTiles);
				if (!this.tileMemoryCardCache.containsKey(this.currentJob)) {
					// cache miss
					this.mapGenerator.addJob(this.currentJob);
				}

				this.currentTile = new Tile(this.mapViewTileX1 - 1, this.tileY, this.zoomLevel);
				this.currentJob = new MapGeneratorJob(this.currentTile, this.mapViewMode,
						this.mapFile, this.textScale, this.drawTileFrames,
						this.drawTileCoordinates, this.highlightWaterTiles);
				if (!this.tileMemoryCardCache.containsKey(this.currentJob)) {
					// cache miss
					this.mapGenerator.addJob(this.currentJob);
				}
			}
		}

		// notify the MapGenerator to process the job list
		this.mapGenerator.requestSchedule(false);
	}

	/**
	 * Sets the center of the MapView and triggers a redraw.
	 * 
	 * @param point
	 *            the new center point of the map.
	 */
	void setCenter(GeoPoint point) {
		setCenterAndZoom(point, this.zoomLevel);
	}

	/**
	 * Sets the center and zoom level of the MapView and triggers a redraw.
	 * 
	 * @param point
	 *            the new center point of the map.
	 * @param zoom
	 *            the new zoom level. This value will be limited by the maximum and minimum possible
	 *            zoom level.
	 */
	void setCenterAndZoom(GeoPoint point, byte zoom) {
		if (point == null) {
			// do nothing
			return;
		}

		if (this.mapViewMode.requiresInternetConnection()
				|| (this.mapDatabase != null && this.mapDatabase.getMapBoundary() != null && this.mapDatabase
						.getMapBoundary().contains(point.getLongitudeE6(),
								point.getLatitudeE6()))) {
			if (hasValidCenter()) {
				// calculate the distance between previous and current position
				synchronized (this) {
					this.matrixTranslateX = (float) (MercatorProjection.longitudeToPixelX(
							this.longitude, this.zoomLevel) - MercatorProjection
							.longitudeToPixelX(point.getLongitude(), this.zoomLevel));
					this.matrixTranslateY = (float) (MercatorProjection.latitudeToPixelY(
							this.latitude, this.zoomLevel) - MercatorProjection
							.latitudeToPixelY(point.getLatitude(), this.zoomLevel));
				}
				matrixPostTranslate(this.matrixTranslateX, this.matrixTranslateY);
			}

			// set the new center coordinates and the zoom level
			synchronized (this) {
				this.latitude = getValidLatitude(point.getLatitude());
				this.longitude = point.getLongitude();
				this.zoomLevel = getValidZoomLevel(zoom);
			}

			// TODO enable or disable the zoom buttons if necessary
			//this.zoomControls
			//		.setIsZoomInEnabled(this.zoomLevel < getMaximumPossibleZoomLevel());
			//this.zoomControls.setIsZoomOutEnabled(this.zoomLevel > this.zoomLevelMin);
			handleTiles(true);
		}
	}

	/**
	 * Calculates the priority for the given job based on the current position and zoom level of the
	 * map.
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
			//TODO calculate the center of the MapView
			//double mapViewCenterX = this.mapViewPixelX + (getWidth() >> 1);
			//double mapViewCenterY = this.mapViewPixelY + (getHeight() >> 1);

			// calculate the center of the tile
			long tileCenterX = mapGeneratorJob.tile.pixelX + (Tile.TILE_SIZE >> 1);
			long tileCenterY = mapGeneratorJob.tile.pixelY + (Tile.TILE_SIZE >> 1);

			//TODO set tile priority to the distance from the MapView center
			//double diffX = mapViewCenterX - tileCenterX;
			//double diffY = mapViewCenterY - tileCenterY;
			//mapGeneratorJob.priority = (int) Math.sqrt(diffX * diffX + diffY * diffY);
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
		if (newMapFile != null && this.mapDatabase != null
				&& this.mapDatabase.openFile(newMapFile)) {
			((DatabaseMapGenerator) this.mapGenerator).onMapFileChange();
			this.mapFile = newMapFile;
		} else {
			this.mapFile = null;
		}
	}

	/**
	 * Displays the zoom controls permanently.
	 */
	void showZoomControls() {
		//TODO
	}

	/**
	 * Zooms in or out by the given amount of zoom levels.
	 * 
	 * @param zoomLevelDiff
	 *            the difference to the current zoom level.
	 * @param zoomStart
	 *            the zoom factor at the begin of the animation.
	 * @return true if the zoom level was changed, false otherwise.
	 */
	boolean zoom(byte zoomLevelDiff, float zoomStart) {
		//TODO
		return true;
	}
	
	private void invalidate() {
		// TODO Auto-generated method stub
		
	}

	private void postInvalidate() {
		// TODO Auto-generated method stub
		
	}
	
	private int getHeight() {
		// TODO Auto-generated method stub
		return 0;
	}

	private int getWidth() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	private void setWillNotDraw(boolean b) {
		// TODO Auto-generated method stub
		
	}
}