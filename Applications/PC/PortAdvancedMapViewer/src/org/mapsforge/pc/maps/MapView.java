package org.mapsforge.pc.maps;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;

import javax.swing.JLabel;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Matrix;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.widget.ZoomControls;

public class MapView extends JLabel implements MouseListener, ActionListener {

	private static final long serialVersionUID = -7437435113432268381L;
	public enum TextField { KILOMETER, METER, OKAY; }

	/**
	 * Listener
	 */
	@Override
	public void mouseClicked(MouseEvent arg0) { }

	@Override
	public void mouseEntered(MouseEvent arg0) { }

	@Override
	public void mouseExited(MouseEvent arg0) { }

	@Override
	public void mousePressed(MouseEvent arg0) { }

	@Override
	public void mouseReleased(MouseEvent arg0) { }

	@Override
	public void actionPerformed(ActionEvent arg0) { }

	//Constant
	private static final int TILE_RAM_CACHE_SIZE = 16;
	private static final Color MAP_VIEW_BACKGROUND = new Color(238, 238, 238);
	static final double LATITUDE_MAX = 85.05113;
	static final double LATITUDE_MIN = -85.05113;
	static final double LONGITUDE_MAX = 180;
	static final double LONGITUDE_MIN = -180;
	private static final int[] SCALE_BAR_VALUES = { 10000000, 5000000, 2000000, 1000000,
		500000, 200000, 100000, 50000, 20000, 10000, 5000, 2000, 1000, 500, 200, 100, 50,
		20, 10, 5, 2, 1 };
	private static final Paint PAINT_SCALE_BAR = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_SCALE_BAR_STROKE = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_SCALE_BAR_TEXT = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_SCALE_BAR_TEXT_WHITE_STROKE = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final short SCALE_BAR_WIDTH = 130;
	private static final int DEFAULT_TILE_MEMORY_CARD_CACHE_SIZE = 100;
	private static final byte DEFAULT_ZOOM_LEVEL_MIN = 0;
	
	int mapViewId;
	String mapFile;
	
	//CONTROLLER
	MapController mapController;
	
	//ACTIVITY
	MapActivity mapActivity;
	
	//GENERATOR
	MapGenerator mapGenerator;
	MapGeneratorJob currentJob;

	//DATABASE
	MapDatabase mapDatabase;

	//MOVER
	MapMover mapMover;

	//CACHE
	TileMemoryCardCache tileMemoryCardCache;
	TileRAMCache tileRAMCache;
	int tileMemoryCardCacheSize;
	
	//MATRIX
	Matrix matrix;
	float matrixTranslateX;
	float matrixTranslateY;
	float matrixScaleFactor;
	
	//ZOOM
	byte zoomLevel;
	byte zoomLevelMax;
	byte zoomLevelMin;
	boolean showZoomControls;
	ZoomAnimator zoomAnimator;
	ZoomControls zoomControls;

	//MAPVIEW
	long mapViewTileX1;
	long mapViewTileX2;
	long mapViewTileY1;
	long mapViewTileY2;
	double mapViewPixelX;
	double mapViewPixelY;
	MapViewMode mapViewMode;
	Bitmap mapViewBitmapSwap;
	Bitmap mapViewBitmap1;
	Bitmap mapViewBitmap2;
	public Canvas mapViewCanvas = new Canvas();

	long tileX;
	long tileY;
	
	//MAPSCALE
	Canvas mapScaleCanvas;
	Bitmap mapScaleBitmap;
	float textScale;
	boolean showScaleBar;
	double mapScalePreviousLatitude;
	byte mapScalePreviousZoomLevel;
	int mapScale;
	float mapScaleLength;

	//TILE
	Bitmap tileBitmap;
	ByteBuffer tileBuffer;
	boolean drawTileCoordinates;
	boolean drawTileFrames;
	int numberOfTiles;
	Tile currentTile;

	Projection projection;
	boolean persistence;
	boolean highlightWaterTiles;
	double meterPerPixel;
	float moveSpeedFactor;
	boolean attachedToWindow;
	double longitude;
	double latitude;
	String text_kilometer;
	String text_meter;
	String text_ok;
	boolean showFpsCounter;
	
	/**
	 * Constructor
	 */
	public MapView(int mapViewId) {
		this.mapViewId = mapViewId;
		this.mapActivity  = new MapActivity();
		setupMapView();
	}
	
	/**
	 * Methods
	 */
	private synchronized void setupMapView() {

		this.tileMemoryCardCacheSize = DEFAULT_TILE_MEMORY_CARD_CACHE_SIZE;
		
		this.setBackground(MAP_VIEW_BACKGROUND);
		//setWillNotDraw(false);
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
		this.tileMemoryCardCache = new TileMemoryCardCache("res" + File.separatorChar + "tile" + File.separatorChar + this.mapViewId,
				this.tileMemoryCardCacheSize);

		// create the MapController for this MapView
		this.mapController = new MapController(this);
		
		// create the database
		this.mapDatabase = new MapDatabase();

		startMapGeneratorThread();

		// set the default position and zoom level of the map
		GeoPoint defaultStartPoint = this.mapGenerator.getDefaultStartPoint();
		this.latitude = defaultStartPoint.getLatitude();
		this.longitude = defaultStartPoint.getLongitude();
		this.zoomLevel = this.mapGenerator.getDefaultZoomLevel();
		this.zoomLevelMin = DEFAULT_ZOOM_LEVEL_MIN;
		this.zoomLevelMax = Byte.MAX_VALUE;

		// create and start the MapMover thread
		//this.mapMover = new MapMover();
		//this.mapMover.setMapView(this);
		//this.mapMover.start();

		// create and start the ZoomAnimator thread
		//this.zoomAnimator = new ZoomAnimator();
		//this.zoomAnimator.setMapView(this);
		//this.zoomAnimator.start();
		
		//System.out.println(this.latitude + "<->" + this.longitude);
		// register the MapView in the MapActivity
		
		this.mapActivity.registerMapView(this);
	}
	
	/**
	 * Creates and starts the MapGenerator thread.
	 */
	private void startMapGeneratorThread() {
		this.mapGenerator = new CanvasRenderer();
		((DatabaseMapGenerator) this.mapGenerator).setDatabase(this.mapDatabase);	

		if (this.attachedToWindow) {
			this.mapGenerator.onAttachedToWindow();
		}
		
		this.mapGenerator.setTileCaches(this.tileRAMCache, this.tileMemoryCardCache);
		this.mapGenerator.setMapView(this);
		this.mapGenerator.start();
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
		} else if (this.mapDatabase.getMapBoundary() == null || !this.mapDatabase
						.getMapBoundary().contains(getMapCenter().getLongitudeE6(),
								getMapCenter().getLatitudeE6())) {
			return false;
		}
		return true;
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

		if (this.mapFile == null) {
			return;
		}
		System.out.println("DRAN");

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
							//this.tileBitmap.copyPixelsFromBuffer(this.tileBuffer);
							
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
	
	void matrixPostTranslate(float dx, float dy) {
		synchronized (this.matrix) {
			this.matrix.postTranslate(dx, dy);
		}
	}
	
	void postInvalidate() {
		invalidate();
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

		//if (this.zoomAnimator.isExecuting()) {
			// do not disturb the ongoing animation
		//	return;
		//}

		if (!matrixIsIdentity()) {
			// change the current MapView bitmap
			this.mapViewBitmap2.eraseColor(MAP_VIEW_BACKGROUND);
			this.mapViewCanvas.setBitmap(this.mapViewBitmap2);

			// draw the previous MapView bitmap on the current MapView bitmap
			synchronized (this.matrix) {
				this.mapViewCanvas.drawBitmap(this.mapViewBitmap1, this.matrix, null);
				this.matrix.reset();
			}

			// swap the two MapView bitmaps
			this.mapViewBitmapSwap = this.mapViewBitmap1;
			this.mapViewBitmap1 = this.mapViewBitmap2;
			this.mapViewBitmap2 = this.mapViewBitmapSwap;
		}

		// draw the tile bitmap at the correct position
		this.mapViewCanvas.drawBitmap(bitmap,
				(float) (mapGeneratorJob.tile.pixelX - this.mapViewPixelX),
				(float) (mapGeneratorJob.tile.pixelY - this.mapViewPixelY), null);
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
	
	boolean matrixIsIdentity() {
		synchronized (this.matrix) {
			return this.matrix.isIdentity();
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
		}
	}
	
	/**
	 * This method is called by the MapGenerator when its job queue is empty.
	 */
	void requestMoreJobs() {
		if (this.mapFile == null) {
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
	
	boolean zoom(byte zoomLevelDiff, float zoomStart) {
		if (zoomLevelDiff > 0) {
			// check if zoom in is possible
			if (this.zoomLevel + zoomLevelDiff > getMaximumPossibleZoomLevel()) {
				return false;
			}
			this.matrixScaleFactor = 1 << zoomLevelDiff;
		} else if (zoomLevelDiff < 0) {
			// check if zoom out is possible
			if (this.zoomLevel + zoomLevelDiff < this.zoomLevelMin) {
				return false;
			}
			this.matrixScaleFactor = 1.0f / (1 << -zoomLevelDiff);
		} else {
			// zoom level is unchanged
			this.matrixScaleFactor = 1;
		}

		// change the zoom level
		synchronized (this) {
			this.zoomLevel += zoomLevelDiff;
		}

		// enable or disable the zoom buttons if necessary
		this.zoomControls.setIsZoomInEnabled(this.zoomLevel < getMaximumPossibleZoomLevel());
		this.zoomControls.setIsZoomOutEnabled(this.zoomLevel > this.zoomLevelMin);
		hideZoomControlsDelayed();

		this.zoomAnimator.setParameters(zoomStart, this.matrixScaleFactor, getWidth() >> 1,
				getHeight() >> 1);
		this.zoomAnimator.startAnimation();
		return true;
	}
	
	/**
	 * Displays the zoom controls for a short time.
	 */
	void hideZoomControlsDelayed() {
		//TODO
	}
	
	/**
	 * Called by the enclosing {@link MapActivity} when the MapView is no longer needed.
	 */
	public void destroy() {
		// unregister the MapView in the MapActivity
		if (this.mapActivity != null) {
			this.mapActivity.unregisterMapView(this);
			this.mapActivity = null;
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
	
	/**
	 * Returns the size of a single map tile in bytes.
	 * 
	 * @return the tile size.
	 */
	public static int getTileSizeInBytes() {
		return Tile.TILE_SIZE_IN_BYTES;
	}
	
	/*
	 * GETTERS AND SETTERS
	 */
	
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
	 * Sets the center of the MapView and triggers a redraw.
	 * 
	 * @param point
	 *            the new center point of the map.
	 */
	void setCenter(GeoPoint point) {
		setCenterAndZoom(point, this.zoomLevel);
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
			// calculate the center of the MapView
			double mapViewCenterX = this.mapViewPixelX + (getWidth() >> 1);
			double mapViewCenterY = this.mapViewPixelY + (getHeight() >> 1);

			// calculate the center of the tile
			long tileCenterX = mapGeneratorJob.tile.pixelX + (Tile.TILE_SIZE >> 1);
			long tileCenterY = mapGeneratorJob.tile.pixelY + (Tile.TILE_SIZE >> 1);

			// set tile priority to the distance from the MapView center
			double diffX = mapViewCenterX - tileCenterX;
			double diffY = mapViewCenterY - tileCenterY;
			mapGeneratorJob.priority = (int) Math.sqrt(diffX * diffX + diffY * diffY);
		}
		return mapGeneratorJob;
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
		getMapActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				invalidate();
			}
		});
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

		if ((this.mapDatabase != null && this.mapDatabase.getMapBoundary() != null 
				&& this.mapDatabase.getMapBoundary().contains(point.getLongitudeE6(), point.getLatitudeE6()))) {
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

			//TODO enable or disable the zoom buttons if necessary
			//this.zoomControls.setIsZoomInEnabled(this.zoomLevel < getMaximumPossibleZoomLevel());
			//this.zoomControls.setIsZoomOutEnabled(this.zoomLevel > this.zoomLevelMin);
			handleTiles(true);
		}
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
		getMapActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				invalidate();
			}
		});
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
	 * Sets the map file for this MapView.
	 * 
	 * @param newMapFile
	 *            the path to the new map file.
	 * @throws UnsupportedOperationException
	 *             if the current MapView mode works with an Internet connection.
	 */
	public void setMapFile(String newMapFile) {
		//
		if (newMapFile == null) {
			// no map file is given
			return;
		} else if (this.mapFile != null && this.mapFile.equals(newMapFile)) {
			// same map file as before
			return;
		} else if (this.mapDatabase == null) {
			// no database exists
			return;
		}
		
		//this.mapMover.pause();
		this.mapGenerator.pause();

		//waitForZoomAnimator();
		//waitForMapMover();
		waitForMapGenerator();

		//this.mapMover.stopMove();
		this.mapGenerator.clearJobs();
		
		//this.mapMover.unpause();
		this.mapGenerator.unpause();

		//
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
	 * Returns the MapController for this MapView.
	 * 
	 * @return the MapController.
	 */
	public MapController getController() {
		return this.mapController;
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
	 * Returns the enclosing MapActivity of the MapView.
	 * 
	 * @return the enclosing MapActivity.
	 */
	MapActivity getMapActivity() {
		return this.mapActivity;
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
	 * Returns the currently used map file.
	 * 
	 * @return the map file.
	 * @throws UnsupportedOperationException
	 *             if the current MapView mode works with an Internet connection.
	 */
	public String getMapFile() {
		return this.mapFile;
	}
	
	/**
	 * Returns the database which is currently used for reading the map file.
	 * 
	 * @return the map database.
	 */
	public MapDatabase getMapDatabase() {
		return this.mapDatabase;
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
	 * Returns the move speed of the map, used for trackball and keyboard events.
	 * 
	 * @return the factor by which the move speed of the map will be multiplied.
	 */
	public float getMoveSpeed() {
		return this.moveSpeedFactor;
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
	
	/**
	 * Returns the current zoom level of the map.
	 * 
	 * @return the current zoom level.
	 */
	public byte getZoomLevel() {
		return this.zoomLevel;
	}
	
	public int getWidth() {
		return 100;
	}
	
	public int getHeight() {
		return 100;
	}
}
