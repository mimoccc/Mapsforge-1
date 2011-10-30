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
package org.mapsforge.android.maps;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.mapsforge.android.maps.mapdatabase.MapDatabase;
import org.mapsforge.android.maps.mapdatabase.MapDatabaseCallback;
import org.mapsforge.android.maps.mapdatabase.MapFileInfo;
import org.mapsforge.android.maps.rendertheme.RenderCallback;
import org.mapsforge.android.maps.rendertheme.RenderTheme;
import org.mapsforge.android.maps.rendertheme.RenderThemeHandler;
import org.mapsforge.android.maps.rendertheme.Tag;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;

abstract class DatabaseMapGenerator extends MapGenerator implements
		CoastlineAlgorithm.ClosedPolygonHandler, RenderCallback, MapDatabaseCallback {
	private static final byte DEFAULT_ZOOM_LEVEL = 13;
	private static final byte LAYERS = 11;
	private static final Paint PAINT_WATER_TILE_HIGHTLIGHT = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final double STROKE_INCREASE = 1.5;
	private static final byte STROKE_MIN_ZOOM_LEVEL = 12;
	private static final Tag TAG_NATURAL_COASTLINE = new Tag("natural", "coastline");
	private static final Tag TAG_NATURAL_WATER = new Tag("natural", "water");
	private static final float[][] WATER_TILE_COORDINATES = new float[][] { { 0, 0, Tile.TILE_SIZE, 0,
			Tile.TILE_SIZE, Tile.TILE_SIZE, 0, Tile.TILE_SIZE, 0, 0 } };
	private static final byte ZOOM_MAX = 22;

	/**
	 * Calculates the center of the minimum bounding rectangle for the given coordinates.
	 * 
	 * @param coordinates
	 *            the coordinates for which calculation should be done.
	 * @return the center coordinates of the minimum bounding rectangle.
	 */
	private static float[] calculateCenterOfBoundingBox(float[] coordinates) {
		float longitudeMin = coordinates[0];
		float longitudeMax = coordinates[0];
		float latitudeMax = coordinates[1];
		float latitudeMin = coordinates[1];
		for (int i = 2; i < coordinates.length; i += 2) {
			if (coordinates[i] < longitudeMin) {
				longitudeMin = coordinates[i];
			} else if (coordinates[i] > longitudeMax) {
				longitudeMax = coordinates[i];
			}
			if (coordinates[i + 1] < latitudeMin) {
				latitudeMin = coordinates[i + 1];
			} else if (coordinates[i + 1] > latitudeMax) {
				latitudeMax = coordinates[i + 1];
			}
		}

		return new float[] { (longitudeMin + longitudeMax) / 2, (latitudeMax + latitudeMin) / 2 };
	}

	private static byte getValidLayer(byte layer) {
		if (layer < 0) {
			return 0;
		} else if (layer >= LAYERS) {
			return LAYERS - 1;
		} else {
			return layer;
		}
	}

	/**
	 * Returns true if the given way is closed, false otherwise.
	 * 
	 * @param way
	 *            the coordinates of the way.
	 * @return true if the given way is closed, false otherwise.
	 */
	private static boolean isClosedWay(float[] way) {
		return way[0] == way[way.length - 2] && way[1] == way[way.length - 1];
	}

	private List<PointTextContainer> areaLabels;
	private float[] centerPosition;
	private float[][] coordinates;
	private Tile currentTile;
	private float currentX;
	private float currentY;
	private float diffX;
	private float diffY;
	private List<List<ShapePaintContainer>> drawingLayer;
	private List<List<ShapePaintContainer>> innerWayList;
	private LabelPlacement labelPlacement;
	private MapGeneratorJobTheme lastMapGeneratorJobTheme;
	private float lastTileTextScale;
	private byte lastTileZoomLevel;
	private MapDatabase mapDatabase;
	private List<PointTextContainer> nodes;
	private float nodeX;
	private float nodeY;
	private List<SymbolContainer> pointSymbols;
	private float previousX;
	private float previousY;
	private RenderTheme renderTheme;
	private double segmentLengthInPixel;
	private ShapeContainer shapeContainer;
	private int skipPixels;
	private List<Tag> tagList;
	private Bitmap tileBitmap;
	private float[] wayNamePath;
	private List<WayTextContainer> wayNames;
	private List<List<List<ShapePaintContainer>>> ways;
	private List<SymbolContainer> waySymbols;

	/**
	 * Default constructor which must be called by subclasses.
	 */
	DatabaseMapGenerator() {
		super();
		PAINT_WATER_TILE_HIGHTLIGHT.setStyle(Paint.Style.FILL);
		PAINT_WATER_TILE_HIGHTLIGHT.setColor(Color.CYAN);
	}

	@Override
	public void addArea(Paint paint, int level) {
		this.drawingLayer.get(level).add(new ShapePaintContainer(this.shapeContainer, paint));
	}

	@Override
	public void addAreaCaption(String caption, float dy, Paint paint, Paint stroke) {
		this.centerPosition = calculateCenterOfBoundingBox(this.coordinates[0]);
		this.areaLabels.add(new PointTextContainer(caption, this.centerPosition[0],
				this.centerPosition[1], paint, stroke));
	}

	@Override
	public void addAreaSymbol(Bitmap symbol) {
		this.centerPosition = calculateCenterOfBoundingBox(this.coordinates[0]);
		this.pointSymbols.add(new SymbolContainer(symbol, this.centerPosition[0]
				- (symbol.getWidth() >> 1), this.centerPosition[1] - (symbol.getHeight() >> 1)));
	}

	@Override
	public void addNodeCaption(String caption, float dy, Paint paint, Paint stroke) {
		this.nodes.add(new PointTextContainer(caption, this.nodeX, this.nodeY + dy, paint, stroke));
	}

	@Override
	public void addNodeCircle(float radius, Paint outline, int level) {
		this.drawingLayer.get(level).add(
				new ShapePaintContainer(new CircleContainer(this.nodeX, this.nodeY, radius), outline));
	}

	@Override
	public void addNodeSymbol(Bitmap symbol) {
		this.pointSymbols.add(new SymbolContainer(symbol, this.nodeX - (symbol.getWidth() >> 1),
				this.nodeY - (symbol.getHeight() >> 1)));
	}

	@Override
	public void addWay(Paint paint, int level) {
		this.drawingLayer.get(level).add(new ShapePaintContainer(this.shapeContainer, paint));
	}

	@Override
	public void addWaySymbol(Bitmap symbolBitmap, boolean alignCenter, boolean repeatSymbol) {
		/**
		 * Distance in pixels to skip from both ends of a segment.
		 */
		final int segmentSafetyDistance = 30;

		/**
		 * Minimum distance in pixels before the symbol is repeated.
		 */
		final int distanceBetweenSymbols = 200;

		this.skipPixels = segmentSafetyDistance;

		// get the first way point coordinates
		this.previousX = this.coordinates[0][0];
		this.previousY = this.coordinates[0][1];

		// draw the symbol on each way segment
		float segmentLengthRemaining;
		float segmentSkipPercentage;
		float symbolAngle;
		for (int i = 2; i < this.coordinates[0].length; i += 2) {
			// get the current way point coordinates
			this.currentX = this.coordinates[0][i];
			this.currentY = this.coordinates[0][i + 1];

			// calculate the length of the current segment (Euclidian distance)
			this.diffX = this.currentX - this.previousX;
			this.diffY = this.currentY - this.previousY;
			this.segmentLengthInPixel = Math.sqrt(this.diffX * this.diffX + this.diffY * this.diffY);
			segmentLengthRemaining = (float) this.segmentLengthInPixel;

			while (segmentLengthRemaining - this.skipPixels > segmentSafetyDistance) {
				// calculate the percentage of the current segment to skip
				segmentSkipPercentage = this.skipPixels / segmentLengthRemaining;

				// move the previous point forward towards the current point
				this.previousX += this.diffX * segmentSkipPercentage;
				this.previousY += this.diffY * segmentSkipPercentage;
				symbolAngle = (float) Math.toDegrees(Math.atan2(this.currentY - this.previousY,
						this.currentX - this.previousX));

				this.waySymbols.add(new SymbolContainer(symbolBitmap, this.previousX, this.previousY,
						alignCenter, symbolAngle));

				// check if the symbol should only be rendered once
				if (!repeatSymbol) {
					return;
				}

				// recalculate the distances
				this.diffX = this.currentX - this.previousX;
				this.diffY = this.currentY - this.previousY;

				// recalculate the remaining length of the current segment
				segmentLengthRemaining -= this.skipPixels;

				// set the amount of pixels to skip before repeating the symbol
				this.skipPixels = distanceBetweenSymbols;
			}

			this.skipPixels -= segmentLengthRemaining;
			if (this.skipPixels < segmentSafetyDistance) {
				this.skipPixels = segmentSafetyDistance;
			}

			// set the previous way point coordinates for the next loop
			this.previousX = this.currentX;
			this.previousY = this.currentY;
		}
	}

	@Override
	public void addWayText(String textKey, Paint paint, Paint outline) {
		// calculate the way name length plus some margin of safety
		float wayNameWidth = paint.measureText(textKey) + 10;

		this.skipPixels = 0;

		// get the first way point coordinates
		this.previousX = this.coordinates[0][0];
		this.previousY = this.coordinates[0][1];

		// find way segments long enough to draw the way name on them
		for (int i = 2; i < this.coordinates[0].length; i += 2) {
			// get the current way point coordinates
			this.currentX = this.coordinates[0][i];
			this.currentY = this.coordinates[0][i + 1];

			// calculate the length of the current segment (Euclidian distance)
			this.diffX = this.currentX - this.previousX;
			this.diffY = this.currentY - this.previousY;
			this.segmentLengthInPixel = Math.sqrt(this.diffX * this.diffX + this.diffY * this.diffY);

			if (this.skipPixels > 0) {
				this.skipPixels -= this.segmentLengthInPixel;
			} else if (this.segmentLengthInPixel > wayNameWidth) {
				this.wayNamePath = new float[4];
				// check to prevent inverted way names
				if (this.previousX <= this.currentX) {
					this.wayNamePath[0] = this.previousX;
					this.wayNamePath[1] = this.previousY;
					this.wayNamePath[2] = this.currentX;
					this.wayNamePath[3] = this.currentY;
				} else {
					this.wayNamePath[0] = this.currentX;
					this.wayNamePath[1] = this.currentY;
					this.wayNamePath[2] = this.previousX;
					this.wayNamePath[3] = this.previousY;
				}
				this.wayNames.add(new WayTextContainer(this.wayNamePath, textKey, paint));
				if (outline != null) {
					this.wayNames.add(new WayTextContainer(this.wayNamePath, textKey, outline));
				}

				// set the minimum amount of pixels to skip before repeating the way name
				this.skipPixels = 500;
			}

			// store the previous way point coordinates
			this.previousX = this.currentX;
			this.previousY = this.currentY;
		}
	}

	@Override
	public void onInvalidCoastlineSegment(float[] coastline) {
		this.tagList.clear();
		this.tagList.add(TAG_NATURAL_COASTLINE);
		this.coordinates = new float[][] { coastline };
		this.renderTheme.matchLinearWay(this, this.tagList, this.currentTile.zoomLevel);
	}

	@Override
	public void onIslandPolygon(float[] coastline) {
		this.tagList.clear();
		this.tagList.add(TAG_NATURAL_WATER);
		this.tagList.add(TAG_NATURAL_COASTLINE);
		this.coordinates = new float[][] { coastline };
		this.renderTheme.matchClosedWay(this, this.tagList, this.currentTile.zoomLevel);
	}

	@Override
	public void onValidCoastlineSegment(float[] coastline) {
		this.tagList.clear();
		this.tagList.add(TAG_NATURAL_COASTLINE);
		this.coordinates = new float[][] { coastline };
		this.renderTheme.matchClosedWay(this, this.tagList, this.currentTile.zoomLevel);
	}

	@Override
	public void onWaterPolygon(float[] coastline) {
		this.tagList.clear();
		this.tagList.add(TAG_NATURAL_WATER);
		this.coordinates = new float[][] { coastline };
		this.renderTheme.matchClosedWay(this, this.tagList, this.currentTile.zoomLevel);
	}

	@Override
	public void onWaterTile() {
		this.tagList.clear();
		this.tagList.add(TAG_NATURAL_WATER);
		this.coordinates = WATER_TILE_COORDINATES;
		this.renderTheme.matchClosedWay(this, this.tagList, this.currentTile.zoomLevel);
	}

	@Override
	public final void renderPointOfInterest(byte layer, int latitude, int longitude, List<Tag> tags) {
		this.drawingLayer = this.ways.get(getValidLayer(layer));
		this.nodeX = scaleLongitude(longitude);
		this.nodeY = scaleLatitude(latitude);
		this.renderTheme.matchNode(this, tags, this.currentTile.zoomLevel);
	}

	@Override
	public final void renderWaterBackground() {
		this.tagList.clear();
		this.tagList.add(TAG_NATURAL_WATER);
		this.coordinates = WATER_TILE_COORDINATES;
		this.renderTheme.matchClosedWay(this, this.tagList, this.currentTile.zoomLevel);
	}

	@Override
	public final void renderWay(byte layer, float[] labelPosition, List<Tag> tags, float[][] wayNodes) {
		this.drawingLayer = this.ways.get(getValidLayer(layer));
		// TODO what about the label position?

		// convert from longitude/latitude coordinates to XY positions
		this.coordinates = wayNodes;
		for (int i = 0; i < this.coordinates.length; ++i) {
			for (int j = 0; j < this.coordinates[i].length; j += 2) {
				this.coordinates[i][j] = scaleLongitude(this.coordinates[i][j]);
				this.coordinates[i][j + 1] = scaleLatitude(this.coordinates[i][j + 1]);
			}
		}
		this.shapeContainer = new WayContainer(this.coordinates);

		if (isClosedWay(this.coordinates[0])) {
			this.renderTheme.matchClosedWay(this, tags, this.currentTile.zoomLevel);
		} else {
			this.renderTheme.matchLinearWay(this, tags, this.currentTile.zoomLevel);
		}
	}

	private void createWayLists() {
		int levels = this.renderTheme.getLevels();
		this.ways.clear();
		for (byte i = LAYERS - 1; i >= 0; --i) {
			this.innerWayList = new ArrayList<List<ShapePaintContainer>>(levels);
			for (int j = levels - 1; j >= 0; --j) {
				this.innerWayList.add(new ArrayList<ShapePaintContainer>(0));
			}
			this.ways.add(this.innerWayList);
		}
	}

	private RenderTheme getRenderTheme(MapGeneratorJobTheme mapGeneratorJobTheme) {
		InputStream inputStream = null;
		try {
			if (mapGeneratorJobTheme.internal) {
				switch (mapGeneratorJobTheme.internalRenderTheme) {
					case OSMARENDER:
						inputStream = getClass().getResourceAsStream(
								"rendertheme/osmarender/osmarender.xml");
						break;
					default:
						throw new IllegalArgumentException();
				}
			} else {
				inputStream = new FileInputStream(mapGeneratorJobTheme.themePath);
			}

			RenderThemeHandler renderThemeHandler = new RenderThemeHandler();
			XMLReader xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
			xmlReader.setContentHandler(renderThemeHandler);
			xmlReader.parse(new InputSource(inputStream));
			return renderThemeHandler.getRenderTheme();
		} catch (ParserConfigurationException e) {
			Logger.exception(e);
		} catch (SAXException e) {
			Logger.exception(e);
		} catch (IOException e) {
			Logger.exception(e);
		} finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
			} catch (IOException e) {
				Logger.exception(e);
			}
		}
		return null;
	}

	/**
	 * Converts a latitude value into an Y coordinate on the current tile.
	 * 
	 * @param latitude
	 *            the latitude value.
	 * @return the Y coordinate on the current tile.
	 */
	private float scaleLatitude(float latitude) {
		return (float) (MercatorProjection.latitudeToPixelY(latitude / (double) 1000000,
				this.currentTile.zoomLevel) - this.currentTile.pixelY);
	}

	/**
	 * Converts a longitude value into an X coordinate on the current tile.
	 * 
	 * @param longitude
	 *            the longitude value.
	 * @return the X coordinate on the current tile.
	 */
	private float scaleLongitude(float longitude) {
		return (float) (MercatorProjection.longitudeToPixelX(longitude / (double) 1000000,
				this.currentTile.zoomLevel) - this.currentTile.pixelX);
	}

	/**
	 * Sets the scale stroke factor for the given zoom level.
	 * 
	 * @param zoomLevel
	 *            the zoom level for which the scale stroke factor should be set.
	 */
	private void setScaleStrokeWidth(byte zoomLevel) {
		int zoomLevelDiff = Math.max(zoomLevel - STROKE_MIN_ZOOM_LEVEL, 0);
		this.renderTheme.scaleStrokeWidth((float) Math.pow(STROKE_INCREASE, zoomLevelDiff));
	}

	@Override
	final void cleanup() {
		if (this.renderTheme != null) {
			this.renderTheme.destroy();
			this.renderTheme = null;
		}

		this.currentTile = null;
		this.tileBitmap = null;
		this.mapDatabase = null;
	}

	/**
	 * This method is called when the nodes should be rendered.
	 * 
	 * @param drawNodes
	 *            the nodes to be rendered.
	 */
	abstract void drawNodes(List<PointTextContainer> drawNodes);

	/**
	 * This method is called when the map symbols should be rendered.
	 * 
	 * @param drawSymbols
	 *            the symbols to be rendered.
	 */
	abstract void drawSymbols(List<SymbolContainer> drawSymbols);

	/**
	 * This method is called when the tile coordinates should be rendered.
	 * 
	 * @param tile
	 *            the tile whose coordinates should be rendered.
	 */
	abstract void drawTileCoordinates(Tile tile);

	/**
	 * This method is called when the tile frame should be rendered.
	 */
	abstract void drawTileFrame();

	/**
	 * This method is called when the way names should be rendered.
	 * 
	 * @param drawWayNames
	 *            the way names to be rendered.
	 */
	abstract void drawWayNames(List<WayTextContainer> drawWayNames);

	/**
	 * This method is called when the ways should be rendered.
	 * 
	 * @param drawWays
	 *            the ways to be rendered.
	 */
	abstract void drawWays(List<List<List<ShapePaintContainer>>> drawWays);

	@Override
	final boolean executeJob(MapGeneratorJob mapGeneratorJob) {
		MapGeneratorJob currentJob = mapGeneratorJob;
		this.currentTile = mapGeneratorJob.tile;

		// check if the render theme has changed
		if (!currentJob.mapGeneratorJobTheme.equals(this.lastMapGeneratorJobTheme)) {
			this.renderTheme = getRenderTheme(currentJob.mapGeneratorJobTheme);
			if (this.renderTheme == null) {
				this.lastMapGeneratorJobTheme = null;
				return false;
			}
			createWayLists();
			this.lastMapGeneratorJobTheme = currentJob.mapGeneratorJobTheme;
			this.lastTileZoomLevel = Byte.MIN_VALUE;
		}

		// check if the zoom level has changed
		if (this.currentTile.zoomLevel != this.lastTileZoomLevel) {
			setScaleStrokeWidth(this.currentTile.zoomLevel);
			this.lastTileZoomLevel = this.currentTile.zoomLevel;
		}

		// check if the text scale has changed
		if (currentJob.textScale != this.lastTileTextScale) {
			this.renderTheme.scaleTextSize(currentJob.textScale);
			this.lastTileTextScale = currentJob.textScale;
		}

		this.mapDatabase.executeQuery(this.currentTile, this);

		if (isInterrupted()) {
			return false;
		}

		// erase the tileBitmap with the map background color
		this.tileBitmap.eraseColor(this.renderTheme.getMapBackground());

		// draw all map objects
		drawWays(this.ways);
		if (isInterrupted()) {
			return false;
		}

		drawSymbols(this.waySymbols);

		this.nodes = this.labelPlacement.placeLabels(this.nodes, this.pointSymbols, this.areaLabels,
				this.currentTile);
		drawSymbols(this.pointSymbols);
		if (isInterrupted()) {
			return false;
		}

		drawWayNames(this.wayNames);
		if (isInterrupted()) {
			return false;
		}

		drawNodes(this.nodes);
		drawNodes(this.areaLabels);

		if (mapGeneratorJob.drawTileFrames) {
			drawTileFrame();
		}

		if (mapGeneratorJob.drawTileCoordinates) {
			drawTileCoordinates(this.currentTile);
		}

		finishMapGeneration();
		return true;
	}

	/**
	 * This method is called after all map objects have been rendered.
	 */
	abstract void finishMapGeneration();

	@Override
	final GeoPoint getDefaultStartPoint() {
		if (this.mapDatabase != null) {
			MapFileInfo mapFileInfo = this.mapDatabase.getMapFileInfo();
			if (mapFileInfo.getStartPosition() != null) {
				return mapFileInfo.getStartPosition();
			} else if (mapFileInfo.getMapCenter() != null) {
				return mapFileInfo.getMapCenter();
			}
		}
		return super.getDefaultStartPoint();
	}

	@Override
	final byte getDefaultZoomLevel() {
		return DEFAULT_ZOOM_LEVEL;
	}

	@Override
	final byte getMaxZoomLevel() {
		return ZOOM_MAX;
	}

	@Override
	final void prepareMapGeneration() {
		// clear all data structures for the map objects
		for (int i = this.ways.size() - 1; i >= 0; --i) {
			this.innerWayList = this.ways.get(i);
			for (int j = this.innerWayList.size() - 1; j >= 0; --j) {
				this.innerWayList.get(j).clear();
			}
		}
		this.wayNames.clear();
		this.nodes.clear();
		this.areaLabels.clear();
		this.waySymbols.clear();
		this.pointSymbols.clear();
	}

	/**
	 * Sets the MapDatabase from which the map data will be read.
	 * 
	 * @param mapDatabase
	 *            the MapDatabase from which the map data will be read.
	 */
	final void setDatabase(MapDatabase mapDatabase) {
		this.mapDatabase = mapDatabase;
	}

	@Override
	final void setupMapGenerator(Bitmap bitmap) {
		this.tileBitmap = bitmap;

		this.labelPlacement = new LabelPlacement();

		this.ways = new ArrayList<List<List<ShapePaintContainer>>>(LAYERS);
		this.wayNames = new ArrayList<WayTextContainer>(64);
		this.nodes = new ArrayList<PointTextContainer>(64);
		this.areaLabels = new ArrayList<PointTextContainer>(64);
		this.waySymbols = new ArrayList<SymbolContainer>(64);
		this.pointSymbols = new ArrayList<SymbolContainer>(64);
		this.tagList = new ArrayList<Tag>(2);

		setupRenderer(this.tileBitmap);
	}

	/**
	 * This method is called once during the setup process. It can be used to set up internal data
	 * structures that the renderer needs.
	 * 
	 * @param bitmap
	 *            the bitmap on which all future tiles need to be rendered.
	 */
	abstract void setupRenderer(Bitmap bitmap);
}