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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.TreeMap;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.Paint.Align;

/**
 * The MapGenerator reads map data from a database and renders map images.
 */
class MapGenerator extends Thread {
	private static final short BITMAP_AMENITY = 32;
	private static final short BITMAP_BUILDING = 2;
	private static final short BITMAP_HIGHWAY = 1;
	private static final short BITMAP_LANDUSE = 8;
	private static final short BITMAP_LEISURE = 16;
	private static final short BITMAP_NATURAL = 64;
	private static final short BITMAP_RAILWAY = 4;
	private static final short BITMAP_WATERWAY = 128;
	private static final boolean DRAW_TILE_FRAMES = false;
	private static final byte LAYERS = 11;
	private static final byte MIN_ZOOM_LEVEL_AREA_NAMES = 17;
	private static final byte MIN_ZOOM_LEVEL_AREA_SYMBOLS = 17;
	private static final byte MIN_ZOOM_LEVEL_WAY_NAMES = 15;
	private static final byte MODE_AREA_NAME_BLACK = 0;
	private static final byte MODE_AREA_NAME_BLUE = 1;
	private static final byte MODE_AREA_NAME_RED = 2;
	private static final Paint PAINT_AEROWAY_AERODROME_FILL = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_AEROWAY_AERODROME_OUTLINE = new Paint(
			Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_AEROWAY_APRON_FILL = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_AEROWAY_RUNWAY1 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_AEROWAY_RUNWAY2 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_AEROWAY_TAXIWAY1 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_AEROWAY_TAXIWAY2 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_AEROWAY_TERMINAL_FILL = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_AEROWAY_TERMINAL_OUTLINE = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_AMENITY_GRAVE_YARD_FILL = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_AMENITY_HOSPITAL_FILL = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_AMENITY_PARKING_FILL = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_AMENITY_PARKING_OUTLINE = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_AMENITY_SCHOOL_FILL = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_AMENITY_SCHOOL_OUTLINE = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_BARRIER_BOLLARD = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_BARRIER_WALL = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_10 = new Paint(
			Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_2 = new Paint(
			Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_4 = new Paint(
			Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_6 = new Paint(
			Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_8 = new Paint(
			Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_9 = new Paint(
			Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_BUILDING_ROOF_OUTLINE = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_BUILDING_YES_FILL = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_BUILDING_YES_OUTLINE = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_BRIDLEWAY1 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_BRIDLEWAY2 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_CONSTRUCTION = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_CYCLEWAY1 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_CYCLEWAY2 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_FOOTWAY_AREA_FILL = new Paint(
			Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_FOOTWAY_AREA_OUTLINE = new Paint(
			Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_FOOTWAY1 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_FOOTWAY2 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_LIVING_STREET1 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_LIVING_STREET2 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_MOTORWAY_LINK1 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_MOTORWAY_LINK2 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_MOTORWAY1 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_MOTORWAY2 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_PATH1 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_PATH2 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_PEDESTRIAN_AREA_FILL = new Paint(
			Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_PEDESTRIAN_AREA_OUTLINE = new Paint(
			Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_PEDESTRIAN1 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_PEDESTRIAN2 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_PRIMARY_LINK1 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static Paint PAINT_HIGHWAY_PRIMARY_LINK2 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_PRIMARY1 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static Paint PAINT_HIGHWAY_PRIMARY2 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_RESIDENTIAL1 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_RESIDENTIAL2 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_ROAD1 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_ROAD2 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_SECONDARY1 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_SECONDARY2 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_SERVICE_AREA_FILL = new Paint(
			Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_SERVICE_AREA_OUTLINE = new Paint(
			Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_SERVICE1 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_SERVICE2 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_STEPS1 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_STEPS2 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_TERTIARY1 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_TERTIARY2 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_TRACK1 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_TRACK2 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_TRUNK_LINK1 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_TRUNK_LINK2 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_TRUNK1 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_TRUNK2 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_TUNNEL1 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_TUNNEL2 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_UNCLASSIFIED1 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HIGHWAY_UNCLASSIFIED2 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HISTORIC_CIRCLE_INNER = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_HISTORIC_CIRCLE_OUTER = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_INFO_BLACK_13 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_LANDUSE_BASIN_FILL = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_LANDUSE_CEMETERY_FILL = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_LANDUSE_COMMERCIAL_FILL = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_LANDUSE_CONSTRUCTION_FILL = new Paint(
			Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_LANDUSE_FOREST_FILL = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_LANDUSE_GRASS_FILL = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_LANDUSE_GRASS_OUTLINE = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_LANDUSE_INDUSTRIAL_FILL = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_LANDUSE_MILITARY_FILL = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_LANDUSE_RESIDENTIAL_FILL = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_LANDUSE_RETAIL_FILL = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_LEISURE_COMMON_FILL = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_LEISURE_COMMON_OUTLINE = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_LEISURE_STADIUM_FILL = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_LEISURE_STADIUM_OUTLINE = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_MAN_MADE_PIER = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_MAP_SCALE_TEXT = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_MAP_SCALE_TEXT_STROKE = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_MAP_SCALE1 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_MAP_SCALE2 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_MILITARY_BARRACKS_FILL = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_MILITARY_NAVAL_BASE_FILL = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_NAME_BLACK_10 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_NAME_BLACK_12 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_NAME_BLACK_15 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_NAME_BLACK_20 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_NAME_BLACK_25 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_NAME_BLUE_10 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_NAME_PURPLE_10 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_NAME_RED_10 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_NAME_RED_11 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_NAME_RED_13 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_NAME_WHITE_STROKE_10 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_NAME_WHITE_STROKE_11 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_NAME_WHITE_STROKE_13 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_NATURAL_BEACH_FILL = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_NATURAL_COASTLINE_OUTLINE = new Paint(
			Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_NATURAL_HEATH_FILL = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_NATURAL_LAND_FILL = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_NATURAL_WATER_FILL = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_NATURAL_WOOD_FILL = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_RAILWAY_CIRCLE_INNER = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_RAILWAY_CIRCLE_OUTER = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_RAILWAY_LIGHT_RAIL1 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_RAILWAY_LIGHT_RAIL2 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_RAILWAY_RAIL_TUNNEL1 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_RAILWAY_RAIL_TUNNEL2 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_RAILWAY_RAIL1 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_RAILWAY_RAIL2 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_RAILWAY_STATION_FILL = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_RAILWAY_STATION_OUTLINE = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_RAILWAY_SUBWAY_TUNNEL1 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_RAILWAY_SUBWAY_TUNNEL2 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_RAILWAY_SUBWAY1 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_RAILWAY_SUBWAY2 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_RAILWAY_TRAM1 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_RAILWAY_TRAM2 = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_ROUTE_FERRY = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_SPORT_SHOOTING_FILL = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_SPORT_SHOOTING_OUTLINE = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_SPORT_TENNIS_FILL = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_SPORT_TENNIS_OUTLINE = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_TOURISM_ATTRACTION_FILL = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_TOURISM_ZOO_FILL = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_TOURISM_ZOO_OUTLINE = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_WATERWAY_CANAL = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_WATERWAY_RIVER = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_WATERWAY_RIVERBANK_FILL = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final Paint PAINT_WATERWAY_STREAM = new Paint(Paint.ANTI_ALIAS_FLAG);
	private static final String THREAD_NAME = "MapGenerator";
	private static final int TILE_BACKGROUND = Color.rgb(248, 248, 248);
	private float[] areaNamePositions;
	private int arrayListIndex;
	private int bboxLatitude1;
	private int bboxLatitude2;
	private int bboxLongitude1;
	private int bboxLongitude2;
	private Bitmap bitmap;
	private Canvas canvas;
	private TreeMap<GeoPoint, int[]> coastlineEnds;
	private TreeMap<GeoPoint, int[]> coastlineStarts;
	private float currentNodeX;
	private float currentNodeY;
	private Tile currentTile;
	private float currentX;
	private float currentY;
	private Database database;
	private float distanceX;
	private float distanceY;
	private ImageBitmapCache imageBitmapCache;
	private ImageFileCache imageFileCache;
	private Path innerPath;
	private int innerWayLength;
	private ArrayList<ArrayList<PathContainer>> innerWayList;
	private PriorityQueue<Tile> jobQueue1;
	private PriorityQueue<Tile> jobQueue2;
	private byte lastTileZoomLevel;
	private ArrayList<ArrayList<PathContainer>> layer;
	private MapSymbols mapSymbols;
	private MapView mapView;
	private ArrayList<PointContainer> nodes;
	private Path path;
	private PathContainer pathContainer;
	private int pathLengthInPixel;
	private PathTextContainer pathTextContainer;
	private boolean pause;
	private PointContainer pointContainer;
	private float previousX;
	private float previousY;
	private boolean ready;
	private byte remainingTags;
	private HashSet<String> renderedWayNames;
	private boolean scheduleNeeded;
	private byte skipSegments;
	private SymbolContainer symbolContainer;
	private ArrayList<SymbolContainer> symbols;
	private PriorityQueue<Tile> tempQueue;
	private ArrayList<PathContainer> wayList;
	private Path wayNamePath;
	private boolean wayNameRendered;
	private ArrayList<PathTextContainer> wayNames;
	private float wayNameWidth;
	private ArrayList<ArrayList<ArrayList<PathContainer>>> ways;

	/**
	 * Constructs a new map generator thread for rendering images.
	 */
	MapGenerator() {
		// create the bitmap and the canvas
		this.bitmap = Bitmap
				.createBitmap(Tile.TILE_SIZE, Tile.TILE_SIZE, Bitmap.Config.RGB_565);
		this.canvas = new Canvas(this.bitmap);

		// create the map org.mapsforge.android.map.symbols
		this.mapSymbols = new MapSymbols();

		initializePaints();
		this.jobQueue1 = new PriorityQueue<Tile>(64);
		this.jobQueue2 = new PriorityQueue<Tile>(64);
	}

	@Override
	public void run() {
		setName(THREAD_NAME);

		// set up data structures
		this.ways = new ArrayList<ArrayList<ArrayList<PathContainer>>>(LAYERS);
		for (byte i = LAYERS - 1; i >= 0; --i) {
			this.innerWayList = new ArrayList<ArrayList<PathContainer>>(
					LayerIds.LEVELS_PER_LAYER);
			for (byte j = LayerIds.LEVELS_PER_LAYER - 1; j >= 0; --j) {
				this.innerWayList.add(new ArrayList<PathContainer>());
			}
			this.ways.add(this.innerWayList);
		}
		this.wayNames = new ArrayList<PathTextContainer>(64);
		this.renderedWayNames = new HashSet<String>((int) (64 / 0.5f) + 2, 0.5f);
		this.nodes = new ArrayList<PointContainer>(64);
		this.symbols = new ArrayList<SymbolContainer>(64);
		this.coastlineEnds = new TreeMap<GeoPoint, int[]>();
		this.coastlineStarts = new TreeMap<GeoPoint, int[]>();

		while (!isInterrupted()) {
			// reset data structures
			for (byte i = LAYERS - 1; i >= 0; --i) {
				this.innerWayList = this.ways.get(i);
				for (byte j = LayerIds.LEVELS_PER_LAYER - 1; j >= 0; --j) {
					this.innerWayList.get(j).clear();
				}
			}
			this.wayNames.clear();
			this.renderedWayNames.clear();
			this.nodes.clear();
			this.symbols.clear();
			this.coastlineStarts.clear();
			this.coastlineEnds.clear();

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

				// check if the paint parameters need to be set again
				if (this.currentTile.zoomLevel != this.lastTileZoomLevel) {
					setPaintParameters(this.currentTile.zoomLevel);
					this.lastTileZoomLevel = this.currentTile.zoomLevel;
				}

				this.database.executeQuery(this.currentTile,
						this.currentTile.zoomLevel >= MIN_ZOOM_LEVEL_WAY_NAMES, this);

				if (isInterrupted()) {
					break;
				}

				renderCoastlines();

				this.bitmap.eraseColor(TILE_BACKGROUND);

				// draw ways
				for (byte i = 0; i < LAYERS; ++i) {
					this.innerWayList = this.ways.get(i);
					for (byte j = 0; j < LayerIds.LEVELS_PER_LAYER; ++j) {
						this.wayList = this.innerWayList.get(j);
						for (this.arrayListIndex = this.wayList.size() - 1; this.arrayListIndex >= 0; --this.arrayListIndex) {
							this.pathContainer = this.wayList.get(this.arrayListIndex);
							this.canvas.drawPath(this.pathContainer.path,
									this.pathContainer.paint);
						}
					}
				}

				if (isInterrupted()) {
					break;
				}

				// draw way names
				for (this.arrayListIndex = this.wayNames.size() - 1; this.arrayListIndex >= 0; --this.arrayListIndex) {
					this.pathTextContainer = this.wayNames.get(this.arrayListIndex);
					this.canvas.drawTextOnPath(this.pathTextContainer.text,
							this.pathTextContainer.path, 0, 3, this.pathTextContainer.paint);
				}

				if (isInterrupted()) {
					break;
				}

				// draw map symbols
				for (this.arrayListIndex = this.symbols.size() - 1; this.arrayListIndex >= 0; --this.arrayListIndex) {
					this.symbolContainer = this.symbols.get(this.arrayListIndex);
					this.canvas.drawBitmap(this.symbolContainer.symbol, this.symbolContainer.x,
							this.symbolContainer.y, null);

				}

				// draw nodes
				for (this.arrayListIndex = this.nodes.size() - 1; this.arrayListIndex >= 0; --this.arrayListIndex) {
					this.pointContainer = this.nodes.get(this.arrayListIndex);
					this.canvas.drawText(this.pointContainer.text, this.pointContainer.x,
							this.pointContainer.y, this.pointContainer.paint);
				}

				if (DRAW_TILE_FRAMES) {
					// draw tile frames
					this.canvas.drawLines(new float[] { 0, 0, 0, this.canvas.getHeight(), 0,
							this.canvas.getHeight(), this.canvas.getWidth(),
							this.canvas.getHeight(), this.canvas.getWidth(),
							this.canvas.getHeight(), this.canvas.getWidth(), 0 },
							PAINT_INFO_BLACK_13);
				}

				if (isInterrupted()) {
					break;
				}

				this.mapView.putTileOnBitmap(this.currentTile, this.bitmap, true);
				this.mapView.postInvalidate();
				Thread.yield();

				// put the image in the cache
				this.imageFileCache.put(this.currentTile, this.bitmap);
			}

			synchronized (this) {
				// if the job queue is empty, ask the MapView for more jobs
				if (!isInterrupted() && this.jobQueue1.isEmpty()) {
					this.mapView.requestMoreJobs();
				}
			}
		}

		// free the bitmap memory
		if (this.bitmap != null) {
			this.bitmap.recycle();
			this.bitmap = null;
		}

		// free the bitmap memory of the map org.mapsforge.android.map.symbols
		if (this.mapSymbols != null) {
			this.mapSymbols.recycle();
			this.mapSymbols = null;
		}

		// set some fields to null to avoid memory leaks
		this.database = null;
		this.mapView = null;
		this.imageBitmapCache = null;
		this.imageFileCache = null;
	}

	private float[] calculateCenterOfBoundingBox(int currentWayNodes,
			int[] currentWayNodesSequence) {
		// calculate bounding box
		this.bboxLongitude1 = currentWayNodesSequence[0];
		this.bboxLongitude2 = currentWayNodesSequence[0];
		this.bboxLatitude1 = currentWayNodesSequence[1];
		this.bboxLatitude2 = currentWayNodesSequence[1];
		for (int i = 2; i < currentWayNodes; i += 2) {
			if (currentWayNodesSequence[i] < this.bboxLongitude1) {
				this.bboxLongitude1 = currentWayNodesSequence[i];
			} else if (currentWayNodesSequence[i] > this.bboxLongitude2) {
				this.bboxLongitude2 = currentWayNodesSequence[i];
			}
			if (currentWayNodesSequence[i + 1] > this.bboxLatitude1) {
				this.bboxLatitude1 = currentWayNodesSequence[i + 1];
			} else if (currentWayNodesSequence[i + 1] < this.bboxLatitude2) {
				this.bboxLatitude2 = currentWayNodesSequence[i + 1];
			}
		}

		// return center coordinates
		return new float[] {
				scaleLongitude(this.bboxLongitude1
						+ ((this.bboxLongitude2 - this.bboxLongitude1) >> 1)),
				scaleLatitude(this.bboxLatitude2
						+ ((this.bboxLatitude1 - this.bboxLatitude2) >> 1)) };
	}

	/**
	 * Calculate the approximate length in pixel of this way using the Euclidean distance for
	 * each way segment.
	 */
	private int getWayLengthInPixel(short wayNodes, int[] wayNodesSequence) {
		this.previousX = scaleLongitude(wayNodesSequence[0]);
		this.previousY = scaleLatitude(wayNodesSequence[1]);
		this.pathLengthInPixel = 0;
		for (short i = 2; i < wayNodes; i += 2) {
			this.currentX = scaleLongitude(wayNodesSequence[i]);
			this.currentY = scaleLatitude(wayNodesSequence[i + 1]);
			this.distanceX = this.currentX - this.previousX;
			this.distanceY = this.currentY - this.previousY;
			this.pathLengthInPixel += SquareRoot
					.sqrt((int) (this.distanceX * this.distanceX + this.distanceY
							* this.distanceY));
			this.previousX = this.currentX;
			this.previousY = this.currentY;
		}
		return this.pathLengthInPixel;
	}

	/**
	 * Sets the style, color and stroke parameters for all paints.
	 */
	private void initializePaints() {
		PAINT_AEROWAY_AERODROME_FILL.setStyle(Paint.Style.FILL);
		PAINT_AEROWAY_AERODROME_FILL.setStrokeJoin(Paint.Join.ROUND);
		PAINT_AEROWAY_AERODROME_FILL.setStrokeCap(Paint.Cap.ROUND);
		PAINT_AEROWAY_AERODROME_FILL.setColor(Color.rgb(229, 224, 195));
		PAINT_AEROWAY_AERODROME_OUTLINE.setStyle(Paint.Style.STROKE);
		PAINT_AEROWAY_AERODROME_OUTLINE.setStrokeJoin(Paint.Join.ROUND);
		PAINT_AEROWAY_AERODROME_OUTLINE.setStrokeCap(Paint.Cap.ROUND);
		PAINT_AEROWAY_AERODROME_OUTLINE.setColor(Color.rgb(145, 140, 144));
		PAINT_AEROWAY_APRON_FILL.setStyle(Paint.Style.FILL);
		PAINT_AEROWAY_APRON_FILL.setStrokeJoin(Paint.Join.ROUND);
		PAINT_AEROWAY_APRON_FILL.setStrokeCap(Paint.Cap.ROUND);
		PAINT_AEROWAY_APRON_FILL.setColor(Color.rgb(240, 240, 240));
		PAINT_AEROWAY_RUNWAY1.setStyle(Paint.Style.STROKE);
		PAINT_AEROWAY_RUNWAY1.setStrokeJoin(Paint.Join.ROUND);
		PAINT_AEROWAY_RUNWAY1.setStrokeCap(Paint.Cap.SQUARE);
		PAINT_AEROWAY_RUNWAY1.setColor(Color.rgb(0, 0, 0));
		PAINT_AEROWAY_RUNWAY2.setStyle(Paint.Style.STROKE);
		PAINT_AEROWAY_RUNWAY2.setStrokeJoin(Paint.Join.ROUND);
		PAINT_AEROWAY_RUNWAY2.setStrokeCap(Paint.Cap.SQUARE);
		PAINT_AEROWAY_RUNWAY2.setColor(Color.rgb(212, 220, 189));
		PAINT_AEROWAY_TAXIWAY1.setStyle(Paint.Style.STROKE);
		PAINT_AEROWAY_TAXIWAY1.setStrokeJoin(Paint.Join.ROUND);
		PAINT_AEROWAY_TAXIWAY1.setStrokeCap(Paint.Cap.ROUND);
		PAINT_AEROWAY_TAXIWAY1.setColor(Color.rgb(0, 0, 0));
		PAINT_AEROWAY_TAXIWAY2.setStyle(Paint.Style.STROKE);
		PAINT_AEROWAY_TAXIWAY2.setStrokeJoin(Paint.Join.ROUND);
		PAINT_AEROWAY_TAXIWAY2.setStrokeCap(Paint.Cap.ROUND);
		PAINT_AEROWAY_TAXIWAY2.setColor(Color.rgb(212, 220, 189));
		PAINT_AEROWAY_TERMINAL_FILL.setStyle(Paint.Style.FILL);
		PAINT_AEROWAY_TERMINAL_FILL.setStrokeJoin(Paint.Join.ROUND);
		PAINT_AEROWAY_TERMINAL_FILL.setStrokeCap(Paint.Cap.ROUND);
		PAINT_AEROWAY_TERMINAL_FILL.setColor(Color.rgb(243, 214, 182));
		PAINT_AEROWAY_TERMINAL_OUTLINE.setStyle(Paint.Style.STROKE);
		PAINT_AEROWAY_TERMINAL_OUTLINE.setStrokeJoin(Paint.Join.ROUND);
		PAINT_AEROWAY_TERMINAL_OUTLINE.setStrokeCap(Paint.Cap.ROUND);
		PAINT_AEROWAY_TERMINAL_OUTLINE.setColor(Color.rgb(115, 100, 143));

		PAINT_AMENITY_GRAVE_YARD_FILL.setStyle(Paint.Style.FILL);
		PAINT_AMENITY_GRAVE_YARD_FILL.setStrokeJoin(Paint.Join.ROUND);
		PAINT_AMENITY_GRAVE_YARD_FILL.setStrokeCap(Paint.Cap.ROUND);
		PAINT_AMENITY_GRAVE_YARD_FILL.setColor(Color.rgb(189, 227, 203));
		PAINT_AMENITY_HOSPITAL_FILL.setStyle(Paint.Style.FILL);
		PAINT_AMENITY_HOSPITAL_FILL.setStrokeJoin(Paint.Join.ROUND);
		PAINT_AMENITY_HOSPITAL_FILL.setStrokeCap(Paint.Cap.ROUND);
		PAINT_AMENITY_HOSPITAL_FILL.setColor(Color.rgb(248, 248, 248));
		PAINT_AMENITY_PARKING_FILL.setStyle(Paint.Style.FILL);
		PAINT_AMENITY_PARKING_FILL.setStrokeJoin(Paint.Join.ROUND);
		PAINT_AMENITY_PARKING_FILL.setStrokeCap(Paint.Cap.ROUND);
		PAINT_AMENITY_PARKING_FILL.setColor(Color.rgb(255, 255, 192));
		PAINT_AMENITY_PARKING_OUTLINE.setStyle(Paint.Style.STROKE);
		PAINT_AMENITY_PARKING_OUTLINE.setStrokeJoin(Paint.Join.ROUND);
		PAINT_AMENITY_PARKING_OUTLINE.setStrokeCap(Paint.Cap.ROUND);
		PAINT_AMENITY_PARKING_OUTLINE.setColor(Color.rgb(233, 221, 115));
		PAINT_AMENITY_SCHOOL_FILL.setStyle(Paint.Style.FILL);
		PAINT_AMENITY_SCHOOL_FILL.setStrokeJoin(Paint.Join.ROUND);
		PAINT_AMENITY_SCHOOL_FILL.setStrokeCap(Paint.Cap.ROUND);
		PAINT_AMENITY_SCHOOL_FILL.setColor(Color.rgb(205, 171, 222));
		PAINT_AMENITY_SCHOOL_OUTLINE.setStyle(Paint.Style.STROKE);
		PAINT_AMENITY_SCHOOL_OUTLINE.setStrokeJoin(Paint.Join.ROUND);
		PAINT_AMENITY_SCHOOL_OUTLINE.setStrokeCap(Paint.Cap.ROUND);
		PAINT_AMENITY_SCHOOL_OUTLINE.setColor(Color.rgb(233, 221, 115));

		PAINT_BARRIER_BOLLARD.setStyle(Paint.Style.STROKE);
		PAINT_BARRIER_BOLLARD.setColor(Color.rgb(113, 112, 111));
		PAINT_BARRIER_WALL.setStyle(Paint.Style.STROKE);
		PAINT_BARRIER_WALL.setStrokeJoin(Paint.Join.ROUND);
		PAINT_BARRIER_WALL.setStrokeCap(Paint.Cap.ROUND);
		PAINT_BARRIER_WALL.setColor(Color.rgb(0, 0, 0));

		PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_2.setStyle(Paint.Style.STROKE);
		PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_2.setStrokeJoin(Paint.Join.ROUND);
		PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_2.setStrokeCap(Paint.Cap.SQUARE);
		PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_2.setColor(Color.rgb(242, 100, 93));
		PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_4.setStyle(Paint.Style.STROKE);
		PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_4.setStrokeJoin(Paint.Join.ROUND);
		PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_4.setStrokeCap(Paint.Cap.SQUARE);
		PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_4.setColor(Color.rgb(242, 100, 93));
		PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_6.setStyle(Paint.Style.STROKE);
		PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_6.setStrokeJoin(Paint.Join.ROUND);
		PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_6.setStrokeCap(Paint.Cap.SQUARE);
		PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_6.setColor(Color.rgb(242, 100, 93));
		PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_8.setStyle(Paint.Style.STROKE);
		PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_8.setStrokeJoin(Paint.Join.ROUND);
		PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_8.setStrokeCap(Paint.Cap.SQUARE);
		PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_8.setColor(Color.rgb(242, 100, 93));
		PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_9.setStyle(Paint.Style.STROKE);
		PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_9.setStrokeJoin(Paint.Join.ROUND);
		PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_9.setStrokeCap(Paint.Cap.SQUARE);
		PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_9.setColor(Color.rgb(242, 100, 93));
		PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_10.setStyle(Paint.Style.STROKE);
		PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_10.setStrokeJoin(Paint.Join.ROUND);
		PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_10.setStrokeCap(Paint.Cap.SQUARE);
		PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_10.setColor(Color.rgb(242, 100, 93));

		PAINT_BUILDING_ROOF_OUTLINE.setStyle(Paint.Style.STROKE);
		PAINT_BUILDING_ROOF_OUTLINE.setStrokeJoin(Paint.Join.ROUND);
		PAINT_BUILDING_ROOF_OUTLINE.setStrokeCap(Paint.Cap.ROUND);
		PAINT_BUILDING_ROOF_OUTLINE.setColor(Color.rgb(115, 100, 143));
		PAINT_BUILDING_YES_FILL.setStyle(Paint.Style.FILL);
		PAINT_BUILDING_YES_FILL.setStrokeJoin(Paint.Join.ROUND);
		PAINT_BUILDING_YES_FILL.setStrokeCap(Paint.Cap.ROUND);
		PAINT_BUILDING_YES_FILL.setColor(Color.rgb(243, 214, 182));
		PAINT_BUILDING_YES_OUTLINE.setStyle(Paint.Style.STROKE);
		PAINT_BUILDING_YES_OUTLINE.setStrokeJoin(Paint.Join.ROUND);
		PAINT_BUILDING_YES_OUTLINE.setStrokeCap(Paint.Cap.ROUND);
		PAINT_BUILDING_YES_OUTLINE.setColor(Color.rgb(115, 100, 143));

		PAINT_HIGHWAY_BRIDLEWAY1.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_BRIDLEWAY1.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_BRIDLEWAY1.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_BRIDLEWAY1.setColor(Color.rgb(175, 212, 175));
		PAINT_HIGHWAY_BRIDLEWAY2.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_BRIDLEWAY2.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_BRIDLEWAY2.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_BRIDLEWAY2.setColor(Color.rgb(112, 185, 113));
		PAINT_HIGHWAY_CONSTRUCTION.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_CONSTRUCTION.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_CONSTRUCTION.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_CONSTRUCTION.setColor(Color.rgb(208, 208, 209));
		PAINT_HIGHWAY_CYCLEWAY1.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_CYCLEWAY1.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_CYCLEWAY1.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_CYCLEWAY1.setColor(Color.rgb(136, 159, 139));
		PAINT_HIGHWAY_CYCLEWAY2.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_CYCLEWAY2.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_CYCLEWAY2.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_CYCLEWAY2.setColor(Color.rgb(209, 234, 209));
		PAINT_HIGHWAY_FOOTWAY_AREA_FILL.setStyle(Paint.Style.FILL);
		PAINT_HIGHWAY_FOOTWAY_AREA_FILL.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_FOOTWAY_AREA_FILL.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_FOOTWAY_AREA_FILL.setColor(Color.rgb(255, 255, 255));
		PAINT_HIGHWAY_FOOTWAY_AREA_OUTLINE.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_FOOTWAY_AREA_OUTLINE.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_FOOTWAY_AREA_OUTLINE.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_FOOTWAY_AREA_OUTLINE.setColor(Color.rgb(143, 144, 141));
		PAINT_HIGHWAY_FOOTWAY1.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_FOOTWAY1.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_FOOTWAY1.setStrokeCap(Paint.Cap.SQUARE);
		PAINT_HIGHWAY_FOOTWAY1.setColor(Color.rgb(165, 166, 150));
		PAINT_HIGHWAY_FOOTWAY2.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_FOOTWAY2.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_FOOTWAY2.setStrokeCap(Paint.Cap.SQUARE);
		PAINT_HIGHWAY_FOOTWAY2.setColor(Color.rgb(229, 224, 194));
		PAINT_HIGHWAY_LIVING_STREET1.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_LIVING_STREET1.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_LIVING_STREET1.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_LIVING_STREET1.setColor(Color.rgb(194, 194, 194));
		PAINT_HIGHWAY_LIVING_STREET2.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_LIVING_STREET2.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_LIVING_STREET2.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_LIVING_STREET2.setColor(Color.rgb(255, 255, 255));
		PAINT_HIGHWAY_MOTORWAY1.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_MOTORWAY1.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_MOTORWAY1.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_MOTORWAY1.setColor(Color.rgb(80, 96, 119));
		PAINT_HIGHWAY_MOTORWAY2.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_MOTORWAY2.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_MOTORWAY2.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_MOTORWAY2.setColor(Color.rgb(128, 155, 192));
		PAINT_HIGHWAY_MOTORWAY_LINK1.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_MOTORWAY_LINK1.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_MOTORWAY_LINK1.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_MOTORWAY_LINK1.setColor(Color.rgb(80, 96, 119));
		PAINT_HIGHWAY_MOTORWAY_LINK2.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_MOTORWAY_LINK2.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_MOTORWAY_LINK2.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_MOTORWAY_LINK2.setColor(Color.rgb(128, 155, 192));
		PAINT_HIGHWAY_PATH1.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_PATH1.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_PATH1.setStrokeCap(Paint.Cap.SQUARE);
		PAINT_HIGHWAY_PATH1.setColor(Color.rgb(128, 128, 128));
		PAINT_HIGHWAY_PATH2.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_PATH2.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_PATH2.setStrokeCap(Paint.Cap.SQUARE);
		PAINT_HIGHWAY_PATH2.setColor(Color.rgb(208, 208, 208));
		PAINT_HIGHWAY_PEDESTRIAN1.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_PEDESTRIAN1.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_PEDESTRIAN1.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_PEDESTRIAN1.setColor(Color.rgb(128, 128, 128));
		PAINT_HIGHWAY_PEDESTRIAN2.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_PEDESTRIAN2.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_PEDESTRIAN2.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_PEDESTRIAN2.setColor(Color.rgb(237, 237, 237));
		PAINT_HIGHWAY_PEDESTRIAN_AREA_FILL.setStyle(Paint.Style.FILL);
		PAINT_HIGHWAY_PEDESTRIAN_AREA_FILL.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_PEDESTRIAN_AREA_FILL.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_PEDESTRIAN_AREA_FILL.setColor(Color.rgb(229, 224, 195));
		PAINT_HIGHWAY_PEDESTRIAN_AREA_OUTLINE.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_PEDESTRIAN_AREA_OUTLINE.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_PEDESTRIAN_AREA_OUTLINE.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_PEDESTRIAN_AREA_OUTLINE.setColor(Color.rgb(145, 140, 144));
		PAINT_HIGHWAY_PRIMARY1.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_PRIMARY1.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_PRIMARY1.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_PRIMARY1.setColor(Color.rgb(141, 67, 70));
		PAINT_HIGHWAY_PRIMARY2.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_PRIMARY2.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_PRIMARY2.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_PRIMARY2.setColor(Color.rgb(228, 109, 113));
		PAINT_HIGHWAY_PRIMARY_LINK1.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_PRIMARY_LINK1.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_PRIMARY_LINK1.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_PRIMARY_LINK1.setColor(Color.rgb(141, 67, 70));
		PAINT_HIGHWAY_PRIMARY_LINK2.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_PRIMARY_LINK2.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_PRIMARY_LINK2.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_PRIMARY_LINK2.setColor(Color.rgb(228, 109, 113));
		PAINT_HIGHWAY_RESIDENTIAL1.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_RESIDENTIAL1.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_RESIDENTIAL1.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_RESIDENTIAL1.setColor(Color.rgb(153, 153, 153));
		PAINT_HIGHWAY_RESIDENTIAL2.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_RESIDENTIAL2.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_RESIDENTIAL2.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_RESIDENTIAL2.setColor(Color.rgb(255, 255, 255));
		PAINT_HIGHWAY_ROAD1.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_ROAD1.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_ROAD1.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_ROAD1.setColor(Color.rgb(122, 128, 124));
		PAINT_HIGHWAY_ROAD2.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_ROAD2.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_ROAD2.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_ROAD2.setColor(Color.rgb(208, 208, 208));
		PAINT_HIGHWAY_SECONDARY1.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_SECONDARY1.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_SECONDARY1.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_SECONDARY1.setColor(Color.rgb(163, 123, 72));
		PAINT_HIGHWAY_SECONDARY2.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_SECONDARY2.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_SECONDARY2.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_SECONDARY2.setColor(Color.rgb(253, 191, 111));
		PAINT_HIGHWAY_SERVICE_AREA_FILL.setStyle(Paint.Style.FILL);
		PAINT_HIGHWAY_SERVICE_AREA_FILL.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_SERVICE_AREA_FILL.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_SERVICE_AREA_FILL.setColor(Color.rgb(255, 255, 255));
		PAINT_HIGHWAY_SERVICE_AREA_OUTLINE.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_SERVICE_AREA_OUTLINE.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_SERVICE_AREA_OUTLINE.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_SERVICE_AREA_OUTLINE.setColor(Color.rgb(143, 144, 141));
		PAINT_HIGHWAY_SERVICE1.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_SERVICE1.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_SERVICE1.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_SERVICE1.setColor(Color.rgb(126, 126, 126));
		PAINT_HIGHWAY_SERVICE2.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_SERVICE2.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_SERVICE2.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_SERVICE2.setColor(Color.rgb(255, 255, 255));
		PAINT_HIGHWAY_STEPS1.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_STEPS1.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_STEPS1.setStrokeCap(Paint.Cap.SQUARE);
		PAINT_HIGHWAY_STEPS1.setColor(Color.rgb(123, 126, 119));
		PAINT_HIGHWAY_STEPS2.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_STEPS2.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_STEPS2.setStrokeCap(Paint.Cap.SQUARE);
		PAINT_HIGHWAY_STEPS2.setColor(Color.rgb(229, 224, 195));
		PAINT_HIGHWAY_TERTIARY1.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_TERTIARY1.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_TERTIARY1.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_TERTIARY1.setColor(Color.rgb(153, 153, 153));
		PAINT_HIGHWAY_TERTIARY2.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_TERTIARY2.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_TERTIARY2.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_TERTIARY2.setColor(Color.rgb(247, 244, 150));
		PAINT_HIGHWAY_TRACK1.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_TRACK1.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_TRACK1.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_TRACK1.setColor(Color.rgb(177, 188, 126));
		PAINT_HIGHWAY_TRACK2.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_TRACK2.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_TRACK2.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_TRACK2.setColor(Color.rgb(255, 255, 255));
		PAINT_HIGHWAY_TRUNK1.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_TRUNK1.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_TRUNK1.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_TRUNK1.setColor(Color.rgb(71, 113, 71));
		PAINT_HIGHWAY_TRUNK2.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_TRUNK2.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_TRUNK2.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_TRUNK2.setColor(Color.rgb(127, 201, 127));
		PAINT_HIGHWAY_TRUNK_LINK1.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_TRUNK_LINK1.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_TRUNK_LINK1.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_TRUNK_LINK1.setColor(Color.rgb(71, 113, 71));
		PAINT_HIGHWAY_TRUNK_LINK2.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_TRUNK_LINK2.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_TRUNK_LINK2.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_TRUNK_LINK2.setColor(Color.rgb(127, 201, 127));
		PAINT_HIGHWAY_TUNNEL1.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_TUNNEL1.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_TUNNEL1.setStrokeCap(Paint.Cap.BUTT);
		PAINT_HIGHWAY_TUNNEL1.setColor(Color.rgb(112, 112, 112));
		PAINT_HIGHWAY_TUNNEL2.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_TUNNEL2.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_TUNNEL2.setStrokeCap(Paint.Cap.BUTT);
		PAINT_HIGHWAY_TUNNEL2.setColor(Color.rgb(248, 248, 248));
		PAINT_HIGHWAY_UNCLASSIFIED1.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_UNCLASSIFIED1.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_UNCLASSIFIED1.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_UNCLASSIFIED1.setColor(Color.rgb(126, 126, 126));
		PAINT_HIGHWAY_UNCLASSIFIED2.setStyle(Paint.Style.STROKE);
		PAINT_HIGHWAY_UNCLASSIFIED2.setStrokeJoin(Paint.Join.ROUND);
		PAINT_HIGHWAY_UNCLASSIFIED2.setStrokeCap(Paint.Cap.ROUND);
		PAINT_HIGHWAY_UNCLASSIFIED2.setColor(Color.rgb(255, 255, 255));

		PAINT_HISTORIC_CIRCLE_INNER.setStyle(Paint.Style.FILL);
		PAINT_HISTORIC_CIRCLE_INNER.setColor(Color.rgb(64, 64, 254));
		PAINT_HISTORIC_CIRCLE_OUTER.setStyle(Paint.Style.STROKE);
		PAINT_HISTORIC_CIRCLE_OUTER.setColor(Color.rgb(90, 90, 90));
		PAINT_HISTORIC_CIRCLE_OUTER.setStrokeWidth(2);

		PAINT_INFO_BLACK_13.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		PAINT_INFO_BLACK_13.setTextSize(12);

		PAINT_LANDUSE_BASIN_FILL.setStyle(Paint.Style.FILL);
		PAINT_LANDUSE_BASIN_FILL.setStrokeJoin(Paint.Join.ROUND);
		PAINT_LANDUSE_BASIN_FILL.setStrokeCap(Paint.Cap.ROUND);
		PAINT_LANDUSE_BASIN_FILL.setColor(Color.rgb(180, 213, 240));
		PAINT_LANDUSE_CEMETERY_FILL.setStyle(Paint.Style.FILL);
		PAINT_LANDUSE_CEMETERY_FILL.setStrokeJoin(Paint.Join.ROUND);
		PAINT_LANDUSE_CEMETERY_FILL.setStrokeCap(Paint.Cap.ROUND);
		PAINT_LANDUSE_CEMETERY_FILL.setColor(Color.rgb(189, 227, 203));
		PAINT_LANDUSE_COMMERCIAL_FILL.setStyle(Paint.Style.FILL);
		PAINT_LANDUSE_COMMERCIAL_FILL.setStrokeJoin(Paint.Join.ROUND);
		PAINT_LANDUSE_COMMERCIAL_FILL.setStrokeCap(Paint.Cap.ROUND);
		PAINT_LANDUSE_COMMERCIAL_FILL.setColor(Color.rgb(239, 200, 200));
		PAINT_LANDUSE_CONSTRUCTION_FILL.setStyle(Paint.Style.FILL);
		PAINT_LANDUSE_CONSTRUCTION_FILL.setStrokeJoin(Paint.Join.ROUND);
		PAINT_LANDUSE_CONSTRUCTION_FILL.setStrokeCap(Paint.Cap.ROUND);
		PAINT_LANDUSE_CONSTRUCTION_FILL.setColor(Color.rgb(164, 124, 65));
		PAINT_LANDUSE_FOREST_FILL.setStyle(Paint.Style.FILL);
		PAINT_LANDUSE_FOREST_FILL.setStrokeJoin(Paint.Join.ROUND);
		PAINT_LANDUSE_FOREST_FILL.setStrokeCap(Paint.Cap.ROUND);
		PAINT_LANDUSE_FOREST_FILL.setColor(Color.rgb(114, 191, 129));
		PAINT_LANDUSE_GRASS_FILL.setStyle(Paint.Style.FILL);
		PAINT_LANDUSE_GRASS_FILL.setStrokeJoin(Paint.Join.ROUND);
		PAINT_LANDUSE_GRASS_FILL.setStrokeCap(Paint.Cap.ROUND);
		PAINT_LANDUSE_GRASS_FILL.setColor(Color.rgb(189, 227, 203));
		PAINT_LANDUSE_GRASS_OUTLINE.setStyle(Paint.Style.STROKE);
		PAINT_LANDUSE_GRASS_OUTLINE.setStrokeJoin(Paint.Join.ROUND);
		PAINT_LANDUSE_GRASS_OUTLINE.setStrokeCap(Paint.Cap.ROUND);
		PAINT_LANDUSE_GRASS_OUTLINE.setColor(Color.rgb(112, 193, 62));
		PAINT_LANDUSE_INDUSTRIAL_FILL.setStyle(Paint.Style.FILL);
		PAINT_LANDUSE_INDUSTRIAL_FILL.setStrokeJoin(Paint.Join.ROUND);
		PAINT_LANDUSE_INDUSTRIAL_FILL.setStrokeCap(Paint.Cap.ROUND);
		PAINT_LANDUSE_INDUSTRIAL_FILL.setColor(Color.rgb(235, 215, 254));
		PAINT_LANDUSE_MILITARY_FILL.setStyle(Paint.Style.FILL);
		PAINT_LANDUSE_MILITARY_FILL.setStrokeJoin(Paint.Join.ROUND);
		PAINT_LANDUSE_MILITARY_FILL.setStrokeCap(Paint.Cap.ROUND);
		PAINT_LANDUSE_MILITARY_FILL.setColor(Color.rgb(208, 208, 80));
		PAINT_LANDUSE_RESIDENTIAL_FILL.setStyle(Paint.Style.FILL);
		PAINT_LANDUSE_RESIDENTIAL_FILL.setStrokeJoin(Paint.Join.ROUND);
		PAINT_LANDUSE_RESIDENTIAL_FILL.setStrokeCap(Paint.Cap.ROUND);
		PAINT_LANDUSE_RESIDENTIAL_FILL.setColor(Color.rgb(228, 228, 228));
		PAINT_LANDUSE_RETAIL_FILL.setStyle(Paint.Style.FILL);
		PAINT_LANDUSE_RETAIL_FILL.setStrokeJoin(Paint.Join.ROUND);
		PAINT_LANDUSE_RETAIL_FILL.setStrokeCap(Paint.Cap.ROUND);
		PAINT_LANDUSE_RETAIL_FILL.setColor(Color.rgb(254, 234, 234));

		PAINT_LEISURE_COMMON_FILL.setStyle(Paint.Style.FILL);
		PAINT_LEISURE_COMMON_FILL.setStrokeJoin(Paint.Join.ROUND);
		PAINT_LEISURE_COMMON_FILL.setStrokeCap(Paint.Cap.ROUND);
		PAINT_LEISURE_COMMON_FILL.setColor(Color.rgb(199, 241, 163));
		PAINT_LEISURE_COMMON_OUTLINE.setStyle(Paint.Style.STROKE);
		PAINT_LEISURE_COMMON_OUTLINE.setStrokeJoin(Paint.Join.ROUND);
		PAINT_LEISURE_COMMON_OUTLINE.setStrokeCap(Paint.Cap.ROUND);
		PAINT_LEISURE_COMMON_OUTLINE.setColor(Color.rgb(123, 200, 145));
		PAINT_LEISURE_STADIUM_FILL.setStyle(Paint.Style.FILL);
		PAINT_LEISURE_STADIUM_FILL.setStrokeJoin(Paint.Join.ROUND);
		PAINT_LEISURE_STADIUM_FILL.setStrokeCap(Paint.Cap.ROUND);
		PAINT_LEISURE_STADIUM_FILL.setColor(Color.rgb(189, 227, 203));
		PAINT_LEISURE_STADIUM_OUTLINE.setStyle(Paint.Style.STROKE);
		PAINT_LEISURE_STADIUM_OUTLINE.setStrokeJoin(Paint.Join.ROUND);
		PAINT_LEISURE_STADIUM_OUTLINE.setStrokeCap(Paint.Cap.ROUND);
		PAINT_LEISURE_STADIUM_OUTLINE.setColor(Color.rgb(123, 200, 145));

		PAINT_MAN_MADE_PIER.setStyle(Paint.Style.STROKE);
		PAINT_MAN_MADE_PIER.setStrokeJoin(Paint.Join.ROUND);
		PAINT_MAN_MADE_PIER.setStrokeCap(Paint.Cap.ROUND);
		PAINT_MAN_MADE_PIER.setColor(Color.rgb(228, 228, 228));

		PAINT_MAP_SCALE1.setStyle(Paint.Style.STROKE);
		PAINT_MAP_SCALE1.setStrokeWidth(2);
		PAINT_MAP_SCALE2.setStyle(Paint.Style.STROKE);
		PAINT_MAP_SCALE2.setStrokeWidth(4);
		PAINT_MAP_SCALE2.setColor(Color.rgb(255, 255, 255));
		PAINT_MAP_SCALE_TEXT.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		PAINT_MAP_SCALE_TEXT.setTextSize(12);
		PAINT_MAP_SCALE_TEXT.setTextAlign(Align.RIGHT);
		PAINT_MAP_SCALE_TEXT_STROKE.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		PAINT_MAP_SCALE_TEXT_STROKE.setTextSize(12);
		PAINT_MAP_SCALE_TEXT_STROKE.setTextAlign(Align.RIGHT);
		PAINT_MAP_SCALE_TEXT_STROKE.setStyle(Paint.Style.STROKE);
		PAINT_MAP_SCALE_TEXT_STROKE.setStrokeWidth(2);
		PAINT_MAP_SCALE_TEXT_STROKE.setColor(Color.rgb(255, 255, 255));

		PAINT_MILITARY_BARRACKS_FILL.setStyle(Paint.Style.FILL);
		PAINT_MILITARY_BARRACKS_FILL.setStrokeJoin(Paint.Join.ROUND);
		PAINT_MILITARY_BARRACKS_FILL.setStrokeCap(Paint.Cap.ROUND);
		PAINT_MILITARY_BARRACKS_FILL.setColor(Color.rgb(208, 208, 80));
		PAINT_MILITARY_NAVAL_BASE_FILL.setStyle(Paint.Style.FILL);
		PAINT_MILITARY_NAVAL_BASE_FILL.setStrokeJoin(Paint.Join.ROUND);
		PAINT_MILITARY_NAVAL_BASE_FILL.setStrokeCap(Paint.Cap.ROUND);
		PAINT_MILITARY_NAVAL_BASE_FILL.setColor(Color.rgb(181, 214, 241));

		PAINT_NAME_BLACK_10.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		PAINT_NAME_BLACK_10.setTextAlign(Align.CENTER);
		PAINT_NAME_BLACK_10.setTextSize(10);
		PAINT_NAME_BLACK_10.setColor(Color.rgb(0, 0, 0));
		PAINT_NAME_BLACK_12.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		PAINT_NAME_BLACK_12.setTextAlign(Align.CENTER);
		PAINT_NAME_BLACK_12.setTextSize(12);
		PAINT_NAME_BLACK_12.setColor(Color.rgb(0, 0, 0));
		PAINT_NAME_BLACK_15.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		PAINT_NAME_BLACK_15.setTextAlign(Align.CENTER);
		PAINT_NAME_BLACK_15.setTextSize(15);
		PAINT_NAME_BLACK_15.setColor(Color.rgb(0, 0, 0));
		PAINT_NAME_BLACK_20.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		PAINT_NAME_BLACK_20.setTextAlign(Align.CENTER);
		PAINT_NAME_BLACK_20.setTextSize(20);
		PAINT_NAME_BLACK_20.setColor(Color.rgb(0, 0, 0));
		PAINT_NAME_BLACK_25.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		PAINT_NAME_BLACK_25.setTextAlign(Align.CENTER);
		PAINT_NAME_BLACK_25.setTextSize(25);
		PAINT_NAME_BLACK_25.setColor(Color.rgb(0, 0, 0));
		PAINT_NAME_BLUE_10.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		PAINT_NAME_BLUE_10.setTextAlign(Align.CENTER);
		PAINT_NAME_BLUE_10.setTextSize(10);
		PAINT_NAME_BLUE_10.setColor(Color.rgb(64, 64, 254));
		PAINT_NAME_PURPLE_10.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		PAINT_NAME_PURPLE_10.setTextAlign(Align.CENTER);
		PAINT_NAME_PURPLE_10.setTextSize(10);
		PAINT_NAME_PURPLE_10.setColor(Color.rgb(255, 4, 255));
		PAINT_NAME_RED_10.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		PAINT_NAME_RED_10.setTextAlign(Align.CENTER);
		PAINT_NAME_RED_10.setTextSize(10);
		PAINT_NAME_RED_10.setColor(Color.rgb(236, 46, 46));
		PAINT_NAME_RED_11.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		PAINT_NAME_RED_11.setTextAlign(Align.CENTER);
		PAINT_NAME_RED_11.setTextSize(11);
		PAINT_NAME_RED_11.setColor(Color.rgb(236, 46, 46));
		PAINT_NAME_RED_13.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		PAINT_NAME_RED_13.setTextAlign(Align.CENTER);
		PAINT_NAME_RED_13.setTextSize(13);
		PAINT_NAME_RED_13.setColor(Color.rgb(236, 46, 46));
		PAINT_NAME_WHITE_STROKE_10.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		PAINT_NAME_WHITE_STROKE_10.setTextAlign(Align.CENTER);
		PAINT_NAME_WHITE_STROKE_10.setStyle(Paint.Style.STROKE);
		PAINT_NAME_WHITE_STROKE_10.setStrokeWidth(3);
		PAINT_NAME_WHITE_STROKE_10.setTextSize(10);
		PAINT_NAME_WHITE_STROKE_10.setColor(Color.rgb(255, 255, 255));
		PAINT_NAME_WHITE_STROKE_11.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		PAINT_NAME_WHITE_STROKE_11.setTextAlign(Align.CENTER);
		PAINT_NAME_WHITE_STROKE_11.setStyle(Paint.Style.STROKE);
		PAINT_NAME_WHITE_STROKE_11.setStrokeWidth(3);
		PAINT_NAME_WHITE_STROKE_11.setTextSize(11);
		PAINT_NAME_WHITE_STROKE_11.setColor(Color.rgb(255, 255, 255));
		PAINT_NAME_WHITE_STROKE_13.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		PAINT_NAME_WHITE_STROKE_13.setTextAlign(Align.CENTER);
		PAINT_NAME_WHITE_STROKE_13.setStyle(Paint.Style.STROKE);
		PAINT_NAME_WHITE_STROKE_13.setStrokeWidth(3);
		PAINT_NAME_WHITE_STROKE_13.setTextSize(13);
		PAINT_NAME_WHITE_STROKE_13.setColor(Color.rgb(255, 255, 255));

		PAINT_NATURAL_BEACH_FILL.setStyle(Paint.Style.FILL);
		PAINT_NATURAL_BEACH_FILL.setStrokeJoin(Paint.Join.ROUND);
		PAINT_NATURAL_BEACH_FILL.setStrokeCap(Paint.Cap.ROUND);
		PAINT_NATURAL_BEACH_FILL.setColor(Color.rgb(238, 204, 85));
		PAINT_NATURAL_COASTLINE_OUTLINE.setStyle(Paint.Style.STROKE);
		PAINT_NATURAL_COASTLINE_OUTLINE.setStrokeJoin(Paint.Join.ROUND);
		PAINT_NATURAL_COASTLINE_OUTLINE.setStrokeCap(Paint.Cap.ROUND);
		PAINT_NATURAL_COASTLINE_OUTLINE.setColor(Color.rgb(181, 214, 241));
		PAINT_NATURAL_HEATH_FILL.setStyle(Paint.Style.FILL);
		PAINT_NATURAL_HEATH_FILL.setStrokeJoin(Paint.Join.ROUND);
		PAINT_NATURAL_HEATH_FILL.setStrokeCap(Paint.Cap.ROUND);
		PAINT_NATURAL_HEATH_FILL.setColor(Color.rgb(255, 255, 192));
		PAINT_NATURAL_LAND_FILL.setStyle(Paint.Style.FILL);
		PAINT_NATURAL_LAND_FILL.setStrokeJoin(Paint.Join.ROUND);
		PAINT_NATURAL_LAND_FILL.setStrokeCap(Paint.Cap.ROUND);
		PAINT_NATURAL_LAND_FILL.setColor(Color.rgb(248, 248, 248));
		PAINT_NATURAL_WATER_FILL.setStyle(Paint.Style.FILL);
		PAINT_NATURAL_WATER_FILL.setStrokeJoin(Paint.Join.ROUND);
		PAINT_NATURAL_WATER_FILL.setStrokeCap(Paint.Cap.ROUND);
		PAINT_NATURAL_WATER_FILL.setColor(Color.rgb(181, 214, 241));
		PAINT_NATURAL_WOOD_FILL.setStyle(Paint.Style.FILL);
		PAINT_NATURAL_WOOD_FILL.setStrokeJoin(Paint.Join.ROUND);
		PAINT_NATURAL_WOOD_FILL.setStrokeCap(Paint.Cap.ROUND);
		PAINT_NATURAL_WOOD_FILL.setColor(Color.rgb(114, 191, 129));

		PAINT_RAILWAY_CIRCLE_INNER.setStyle(Paint.Style.FILL);
		PAINT_RAILWAY_CIRCLE_INNER.setColor(Color.rgb(236, 46, 46));
		PAINT_RAILWAY_CIRCLE_OUTER.setStyle(Paint.Style.STROKE);
		PAINT_RAILWAY_CIRCLE_OUTER.setColor(Color.rgb(90, 90, 90));
		PAINT_RAILWAY_CIRCLE_OUTER.setStrokeWidth(2);
		PAINT_RAILWAY_LIGHT_RAIL1.setStyle(Paint.Style.STROKE);
		PAINT_RAILWAY_LIGHT_RAIL1.setStrokeJoin(Paint.Join.ROUND);
		PAINT_RAILWAY_LIGHT_RAIL1.setStrokeCap(Paint.Cap.BUTT);
		PAINT_RAILWAY_LIGHT_RAIL1.setColor(Color.rgb(181, 228, 227));
		PAINT_RAILWAY_LIGHT_RAIL2.setStyle(Paint.Style.STROKE);
		PAINT_RAILWAY_LIGHT_RAIL2.setStrokeJoin(Paint.Join.ROUND);
		PAINT_RAILWAY_LIGHT_RAIL2.setStrokeCap(Paint.Cap.BUTT);
		PAINT_RAILWAY_LIGHT_RAIL2.setColor(Color.rgb(16, 77, 17));
		PAINT_RAILWAY_RAIL_TUNNEL1.setStyle(Paint.Style.STROKE);
		PAINT_RAILWAY_RAIL_TUNNEL1.setStrokeJoin(Paint.Join.ROUND);
		PAINT_RAILWAY_RAIL_TUNNEL1.setStrokeCap(Paint.Cap.BUTT);
		PAINT_RAILWAY_RAIL_TUNNEL1.setColor(Color.rgb(248, 248, 248));
		PAINT_RAILWAY_RAIL_TUNNEL2.setStyle(Paint.Style.STROKE);
		PAINT_RAILWAY_RAIL_TUNNEL2.setStrokeJoin(Paint.Join.ROUND);
		PAINT_RAILWAY_RAIL_TUNNEL2.setStrokeCap(Paint.Cap.BUTT);
		PAINT_RAILWAY_RAIL_TUNNEL2.setColor(Color.rgb(153, 156, 153));
		PAINT_RAILWAY_RAIL1.setStyle(Paint.Style.STROKE);
		PAINT_RAILWAY_RAIL1.setStrokeJoin(Paint.Join.ROUND);
		PAINT_RAILWAY_RAIL1.setStrokeCap(Paint.Cap.BUTT);
		PAINT_RAILWAY_RAIL1.setColor(Color.rgb(230, 230, 231));
		PAINT_RAILWAY_RAIL2.setStyle(Paint.Style.STROKE);
		PAINT_RAILWAY_RAIL2.setStrokeJoin(Paint.Join.ROUND);
		PAINT_RAILWAY_RAIL2.setStrokeCap(Paint.Cap.BUTT);
		PAINT_RAILWAY_RAIL2.setColor(Color.rgb(52, 50, 50));
		PAINT_RAILWAY_STATION_FILL.setStyle(Paint.Style.FILL);
		PAINT_RAILWAY_STATION_FILL.setStrokeJoin(Paint.Join.ROUND);
		PAINT_RAILWAY_STATION_FILL.setStrokeCap(Paint.Cap.ROUND);
		PAINT_RAILWAY_STATION_FILL.setColor(Color.rgb(243, 214, 182));
		PAINT_RAILWAY_STATION_OUTLINE.setStyle(Paint.Style.STROKE);
		PAINT_RAILWAY_STATION_OUTLINE.setStrokeJoin(Paint.Join.ROUND);
		PAINT_RAILWAY_STATION_OUTLINE.setStrokeCap(Paint.Cap.ROUND);
		PAINT_RAILWAY_STATION_OUTLINE.setColor(Color.rgb(115, 100, 143));
		PAINT_RAILWAY_SUBWAY1.setStyle(Paint.Style.STROKE);
		PAINT_RAILWAY_SUBWAY1.setStrokeJoin(Paint.Join.ROUND);
		PAINT_RAILWAY_SUBWAY1.setStrokeCap(Paint.Cap.BUTT);
		PAINT_RAILWAY_SUBWAY1.setColor(Color.rgb(183, 183, 229));
		PAINT_RAILWAY_SUBWAY2.setStyle(Paint.Style.STROKE);
		PAINT_RAILWAY_SUBWAY2.setStrokeJoin(Paint.Join.ROUND);
		PAINT_RAILWAY_SUBWAY2.setStrokeCap(Paint.Cap.BUTT);
		PAINT_RAILWAY_SUBWAY2.setColor(Color.rgb(25, 24, 91));
		PAINT_RAILWAY_SUBWAY_TUNNEL1.setStyle(Paint.Style.STROKE);
		PAINT_RAILWAY_SUBWAY_TUNNEL1.setStrokeJoin(Paint.Join.ROUND);
		PAINT_RAILWAY_SUBWAY_TUNNEL1.setStrokeCap(Paint.Cap.BUTT);
		PAINT_RAILWAY_SUBWAY_TUNNEL1.setColor(Color.rgb(161, 153, 159));
		PAINT_RAILWAY_SUBWAY_TUNNEL2.setStyle(Paint.Style.STROKE);
		PAINT_RAILWAY_SUBWAY_TUNNEL2.setStrokeJoin(Paint.Join.ROUND);
		PAINT_RAILWAY_SUBWAY_TUNNEL2.setStrokeCap(Paint.Cap.BUTT);
		PAINT_RAILWAY_SUBWAY_TUNNEL2.setColor(Color.rgb(234, 234, 234));
		PAINT_RAILWAY_TRAM1.setStyle(Paint.Style.STROKE);
		PAINT_RAILWAY_TRAM1.setStrokeJoin(Paint.Join.ROUND);
		PAINT_RAILWAY_TRAM1.setStrokeCap(Paint.Cap.BUTT);
		PAINT_RAILWAY_TRAM1.setColor(Color.rgb(229, 183, 229));
		PAINT_RAILWAY_TRAM2.setStyle(Paint.Style.STROKE);
		PAINT_RAILWAY_TRAM2.setStrokeJoin(Paint.Join.ROUND);
		PAINT_RAILWAY_TRAM2.setStrokeCap(Paint.Cap.BUTT);
		PAINT_RAILWAY_TRAM2.setColor(Color.rgb(77, 16, 76));

		PAINT_ROUTE_FERRY.setStyle(Paint.Style.STROKE);
		PAINT_ROUTE_FERRY.setStrokeJoin(Paint.Join.ROUND);
		PAINT_ROUTE_FERRY.setStrokeCap(Paint.Cap.SQUARE);
		PAINT_ROUTE_FERRY.setColor(Color.rgb(113, 113, 113));

		PAINT_SPORT_SHOOTING_FILL.setStyle(Paint.Style.FILL);
		PAINT_SPORT_SHOOTING_FILL.setStrokeJoin(Paint.Join.ROUND);
		PAINT_SPORT_SHOOTING_FILL.setStrokeCap(Paint.Cap.ROUND);
		PAINT_SPORT_SHOOTING_FILL.setColor(Color.rgb(189, 227, 203));
		PAINT_SPORT_SHOOTING_OUTLINE.setStyle(Paint.Style.STROKE);
		PAINT_SPORT_SHOOTING_OUTLINE.setStrokeJoin(Paint.Join.ROUND);
		PAINT_SPORT_SHOOTING_OUTLINE.setStrokeCap(Paint.Cap.ROUND);
		PAINT_SPORT_SHOOTING_OUTLINE.setColor(Color.rgb(112, 193, 143));
		PAINT_SPORT_TENNIS_FILL.setStyle(Paint.Style.FILL);
		PAINT_SPORT_TENNIS_FILL.setStrokeJoin(Paint.Join.ROUND);
		PAINT_SPORT_TENNIS_FILL.setStrokeCap(Paint.Cap.ROUND);
		PAINT_SPORT_TENNIS_FILL.setColor(Color.rgb(209, 138, 106));
		PAINT_SPORT_TENNIS_OUTLINE.setStyle(Paint.Style.STROKE);
		PAINT_SPORT_TENNIS_OUTLINE.setStrokeJoin(Paint.Join.ROUND);
		PAINT_SPORT_TENNIS_OUTLINE.setStrokeCap(Paint.Cap.ROUND);
		PAINT_SPORT_TENNIS_OUTLINE.setColor(Color.rgb(178, 108, 77));

		PAINT_TOURISM_ATTRACTION_FILL.setStyle(Paint.Style.FILL);
		PAINT_TOURISM_ATTRACTION_FILL.setStrokeJoin(Paint.Join.ROUND);
		PAINT_TOURISM_ATTRACTION_FILL.setStrokeCap(Paint.Cap.ROUND);
		PAINT_TOURISM_ATTRACTION_FILL.setColor(Color.rgb(242, 202, 234));
		PAINT_TOURISM_ZOO_FILL.setStyle(Paint.Style.FILL);
		PAINT_TOURISM_ZOO_FILL.setStrokeJoin(Paint.Join.ROUND);
		PAINT_TOURISM_ZOO_FILL.setStrokeCap(Paint.Cap.ROUND);
		PAINT_TOURISM_ZOO_FILL.setColor(Color.rgb(199, 241, 163));
		PAINT_TOURISM_ZOO_OUTLINE.setStyle(Paint.Style.STROKE);
		PAINT_TOURISM_ZOO_OUTLINE.setStrokeJoin(Paint.Join.ROUND);
		PAINT_TOURISM_ZOO_OUTLINE.setStrokeCap(Paint.Cap.ROUND);
		PAINT_TOURISM_ZOO_OUTLINE.setColor(Color.rgb(123, 200, 145));

		PAINT_WATERWAY_CANAL.setStyle(Paint.Style.STROKE);
		PAINT_WATERWAY_CANAL.setStrokeJoin(Paint.Join.ROUND);
		PAINT_WATERWAY_CANAL.setStrokeCap(Paint.Cap.ROUND);
		PAINT_WATERWAY_CANAL.setColor(Color.rgb(179, 213, 241));
		PAINT_WATERWAY_RIVER.setStyle(Paint.Style.STROKE);
		PAINT_WATERWAY_RIVER.setStrokeJoin(Paint.Join.ROUND);
		PAINT_WATERWAY_RIVER.setStrokeCap(Paint.Cap.ROUND);
		PAINT_WATERWAY_RIVER.setColor(Color.rgb(179, 213, 241));
		PAINT_WATERWAY_RIVERBANK_FILL.setStyle(Paint.Style.FILL);
		PAINT_WATERWAY_RIVERBANK_FILL.setStrokeJoin(Paint.Join.ROUND);
		PAINT_WATERWAY_RIVERBANK_FILL.setStrokeCap(Paint.Cap.ROUND);
		PAINT_WATERWAY_RIVERBANK_FILL.setColor(Color.rgb(179, 213, 241));
		PAINT_WATERWAY_STREAM.setStyle(Paint.Style.STROKE);
		PAINT_WATERWAY_STREAM.setStrokeJoin(Paint.Join.ROUND);
		PAINT_WATERWAY_STREAM.setStrokeCap(Paint.Cap.ROUND);
		PAINT_WATERWAY_STREAM.setColor(Color.rgb(179, 213, 241));
	}

	/**
	 * Draws the name of an area if the zoomLevel level is high enough.
	 */
	private void renderAreaName(String currentWayName, int currentWayNodes,
			int[] currentWayNodesSequence, byte mode, byte nameOffset) {
		if (this.currentTile.zoomLevel >= MIN_ZOOM_LEVEL_AREA_NAMES && currentWayName != null) {
			this.areaNamePositions = calculateCenterOfBoundingBox(currentWayNodes,
					currentWayNodesSequence);
			// choose correct text paint
			if (mode == MODE_AREA_NAME_BLUE) {
				this.nodes.add(new PointContainer(currentWayName, this.areaNamePositions[0],
						this.areaNamePositions[1] - nameOffset, PAINT_NAME_BLUE_10));
				this.nodes.add(new PointContainer(currentWayName, this.areaNamePositions[0],
						this.areaNamePositions[1] - nameOffset, PAINT_NAME_WHITE_STROKE_10));
			} else if (mode == MODE_AREA_NAME_BLACK) {
				this.nodes.add(new PointContainer(currentWayName, this.areaNamePositions[0],
						this.areaNamePositions[1] - nameOffset, PAINT_NAME_BLACK_15));
			} else if (mode == MODE_AREA_NAME_RED) {
				this.nodes.add(new PointContainer(currentWayName, this.areaNamePositions[0],
						this.areaNamePositions[1] - nameOffset, PAINT_NAME_RED_10));
			} else {
				Logger.d("unknown render mode: " + mode);
			}
		}
	}

	/**
	 * Draws the symbol of an area if the zoomLevel level is high enough.
	 */
	private void renderAreaSymbol(int currentWayNodes, int[] currentWayNodesSequence,
			Bitmap symbolBitmap) {
		if (this.currentTile.zoomLevel >= MIN_ZOOM_LEVEL_AREA_SYMBOLS) {
			this.areaNamePositions = calculateCenterOfBoundingBox(currentWayNodes,
					currentWayNodesSequence);
			this.symbols.add((new SymbolContainer(symbolBitmap, this.areaNamePositions[0]
					- (symbolBitmap.getWidth() >> 1), this.areaNamePositions[1]
					- (symbolBitmap.getHeight() >> 1))));
		}
	}

	private void renderCoastlines() {
		if (this.coastlineStarts.size() > 0) {
			Iterator<int[]> coastlinePartsIterator = this.coastlineStarts.values().iterator();
			int[] currentPart;
			int currentPartLength;
			boolean isClosedPolygon;
			Path pathCoast;
			Path pathLand;
			Path pathWater;

			int longitude1;
			int longitude2;
			int latitude1;
			int latitude2;
			int longitudeX;
			int latitudeX;

			while (!isInterrupted() && coastlinePartsIterator.hasNext()) {
				currentPart = coastlinePartsIterator.next();
				currentPartLength = currentPart.length;
				if (currentPart[0] == currentPart[currentPartLength - 2]
						&& currentPart[1] == currentPart[currentPartLength - 1]) {
					isClosedPolygon = true;
				} else {
					isClosedPolygon = false;
				}

				pathCoast = new Path();
				pathCoast.moveTo(scaleLongitude(currentPart[0]), scaleLatitude(currentPart[1]));
				for (int i = 2; i < currentPartLength; i += 2) {
					pathCoast.lineTo(scaleLongitude(currentPart[i]),
							scaleLatitude(currentPart[i + 1]));
				}
				pathLand = new Path(pathCoast);
				if (isClosedPolygon) {
					longitude1 = currentPart[0];
					longitude2 = currentPart[0];
					latitude1 = currentPart[1];
					latitude2 = currentPart[1];
					for (int i = 2; i < currentPartLength; i += 2) {
						if (currentPart[i] < longitude1) {
							longitude1 = currentPart[i];
						} else if (currentPart[i] > longitude2) {
							longitude2 = currentPart[i];
						}
						if (currentPart[i + 1] > latitude1) {
							latitude1 = currentPart[i + 1];
						} else if (currentPart[i + 1] < latitude2) {
							latitude2 = currentPart[i + 1];
						}
					}
					pathWater = new Path();
					pathWater.moveTo(scaleLongitude(longitude1), scaleLatitude(latitude1));
					pathWater.lineTo(scaleLongitude(longitude2), scaleLatitude(latitude1));
					pathWater.lineTo(scaleLongitude(longitude2), scaleLatitude(latitude2));
					pathWater.lineTo(scaleLongitude(longitude1), scaleLatitude(latitude2));
					pathWater.close();
					this.ways.get(0).get(LayerIds.NATURAL$LAND).add(
							new PathContainer(pathLand, PAINT_NATURAL_LAND_FILL));
					this.ways.get(0).get(LayerIds.NATURAL$WATER).add(
							new PathContainer(pathWater, PAINT_NATURAL_WATER_FILL));
				} else {
					pathWater = new Path(pathCoast);
					longitude1 = currentPart[0];
					latitude1 = currentPart[1];
					longitude2 = currentPart[currentPartLength - 2];
					latitude2 = currentPart[currentPartLength - 1];

					if (longitude1 < longitude2) {
						if (latitude1 > latitude2) {
							longitudeX = longitude2;
							latitudeX = latitude1;
							for (int i = 2; i < currentPartLength; i += 2) {
								if (currentPart[i] > longitudeX) {
									longitudeX = currentPart[i];
								}
								if (currentPart[i + 1] > latitudeX) {
									latitudeX = currentPart[i + 1];
								}
							}
							pathLand.lineTo(scaleLongitude(longitudeX),
									scaleLatitude(latitude2));
							pathLand.lineTo(scaleLongitude(longitudeX),
									scaleLatitude(latitudeX));
							pathLand.lineTo(scaleLongitude(longitude1),
									scaleLatitude(latitudeX));

							longitudeX = longitude1;
							latitudeX = latitude2;
							for (int i = 2; i < currentPartLength; i += 2) {
								if (currentPart[i] < longitudeX) {
									longitudeX = currentPart[i];
								}
								if (currentPart[i + 1] < latitudeX) {
									latitudeX = currentPart[i + 1];
								}
							}

							pathWater.lineTo(scaleLongitude(longitude2),
									scaleLatitude(latitudeX));
							pathWater.lineTo(scaleLongitude(longitudeX),
									scaleLatitude(latitudeX));
							pathWater.lineTo(scaleLongitude(longitudeX),
									scaleLatitude(latitude1));
						} else {
							longitudeX = longitude1;
							latitudeX = latitude2;
							for (int i = 2; i < currentPartLength; i += 2) {
								if (currentPart[i] < longitudeX) {
									longitudeX = currentPart[i];
								}
								if (currentPart[i + 1] > latitudeX) {
									latitudeX = currentPart[i + 1];
								}
							}
							pathLand.lineTo(scaleLongitude(longitude2),
									scaleLatitude(latitudeX));
							pathLand.lineTo(scaleLongitude(longitudeX),
									scaleLatitude(latitudeX));
							pathLand.lineTo(scaleLongitude(longitudeX),
									scaleLatitude(latitude1));

							longitudeX = longitude2;
							latitudeX = latitude1;
							for (int i = 2; i < currentPartLength; i += 2) {
								if (currentPart[i] > longitudeX) {
									longitudeX = currentPart[i];
								}
								if (currentPart[i + 1] < latitudeX) {
									latitudeX = currentPart[i + 1];
								}
							}

							pathWater.lineTo(scaleLongitude(longitudeX),
									scaleLatitude(latitude2));
							pathWater.lineTo(scaleLongitude(longitudeX),
									scaleLatitude(latitudeX));
							pathWater.lineTo(scaleLongitude(longitude1),
									scaleLatitude(latitudeX));
						}
					} else {
						if (latitude1 > latitude2) {
							longitudeX = longitude1;
							latitudeX = latitude2;
							for (int i = 2; i < currentPartLength; i += 2) {
								if (currentPart[i] > longitudeX) {
									longitudeX = currentPart[i];
								}
								if (currentPart[i + 1] < latitudeX) {
									latitudeX = currentPart[i + 1];
								}
							}
							pathLand.lineTo(scaleLongitude(longitude2),
									scaleLatitude(latitudeX));
							pathLand.lineTo(scaleLongitude(longitudeX),
									scaleLatitude(latitudeX));
							pathLand.lineTo(scaleLongitude(longitudeX),
									scaleLatitude(latitude1));

							longitudeX = longitude2;
							latitudeX = latitude1;
							for (int i = 2; i < currentPartLength; i += 2) {
								if (currentPart[i] < longitudeX) {
									longitudeX = currentPart[i];
								}
								if (currentPart[i + 1] > latitudeX) {
									latitudeX = currentPart[i + 1];
								}
							}

							pathWater.lineTo(scaleLongitude(longitudeX),
									scaleLatitude(latitude2));
							pathWater.lineTo(scaleLongitude(longitudeX),
									scaleLatitude(latitudeX));
							pathWater.lineTo(scaleLongitude(longitude1),
									scaleLatitude(latitudeX));
						} else {
							longitudeX = longitude2;
							latitudeX = latitude1;
							for (int i = 2; i < currentPartLength; i += 2) {
								if (currentPart[i] < longitudeX) {
									longitudeX = currentPart[i];
								}
								if (currentPart[i + 1] < latitudeX) {
									latitudeX = currentPart[i + 1];
								}
							}
							pathLand.lineTo(scaleLongitude(longitudeX),
									scaleLatitude(latitude2));
							pathLand.lineTo(scaleLongitude(longitudeX),
									scaleLatitude(latitudeX));
							pathLand.lineTo(scaleLongitude(longitude1),
									scaleLatitude(latitudeX));

							longitudeX = longitude1;
							latitudeX = latitude2;
							for (int i = 2; i < currentPartLength; i += 2) {
								if (currentPart[i] > longitudeX) {
									longitudeX = currentPart[i];
								}
								if (currentPart[i + 1] > latitudeX) {
									latitudeX = currentPart[i + 1];
								}
							}

							pathWater.lineTo(scaleLongitude(longitude2),
									scaleLatitude(latitudeX));
							pathWater.lineTo(scaleLongitude(longitudeX),
									scaleLatitude(latitudeX));
							pathWater.lineTo(scaleLongitude(longitudeX),
									scaleLatitude(latitude1));
						}
					}
					pathLand.close();
					pathWater.close();
					this.ways.get(0).get(LayerIds.NATURAL$LAND).add(
							new PathContainer(pathLand, PAINT_NATURAL_LAND_FILL));
					this.ways.get(0).get(LayerIds.NATURAL$WATER).add(
							new PathContainer(pathWater, PAINT_NATURAL_WATER_FILL));
					this.ways.get(0).get(LayerIds.NATURAL$COASTLINE).add(
							new PathContainer(pathCoast, PAINT_NATURAL_COASTLINE_OUTLINE));
				}
			}
		}
	}

	private void renderHighwayName(String wayName, short wayNodes, int[] wayNodesSequence) {
		// calculate the approximate way name length plus some margin of safety
		this.wayNameWidth = PAINT_NAME_BLACK_10.measureText(wayName) + 10;

		this.previousX = scaleLongitude(wayNodesSequence[0]);
		this.previousY = scaleLatitude(wayNodesSequence[1]);

		// flag if the current way name has been rendered at least once
		this.wayNameRendered = false;
		this.skipSegments = 0;

		// find way segments long enough to draw the way name on them
		for (short i = 2; i < wayNodes; i += 2) {
			this.currentX = scaleLongitude(wayNodesSequence[i]);
			this.currentY = scaleLatitude(wayNodesSequence[i + 1]);
			if (this.skipSegments > 0) {
				--this.skipSegments;
			} else {
				// check the length of the current segment by calculating the
				// Euclidian distance of the segment way points
				this.distanceX = this.currentX - this.previousX;
				this.distanceY = this.currentY - this.previousY;
				this.pathLengthInPixel = SquareRoot
						.sqrt((int) (this.distanceX * this.distanceX + this.distanceY
								* this.distanceY));

				if (this.pathLengthInPixel > this.wayNameWidth) {
					this.wayNamePath = new Path();
					// check to prevent inverted way names
					if (this.previousX <= this.currentX) {
						this.wayNamePath.moveTo(this.previousX, this.previousY);
						this.wayNamePath.lineTo(this.currentX, this.currentY);
					} else {
						this.wayNamePath.moveTo(this.currentX, this.currentY);
						this.wayNamePath.lineTo(this.previousX, this.previousY);
					}
					this.wayNames.add(new PathTextContainer(this.wayNamePath,
							PAINT_NAME_BLACK_10, wayName));
					this.wayNameRendered = true;
					this.skipSegments = 3;
				}
			}
			this.previousX = this.currentX;
			this.previousY = this.currentY;
		}

		// if no segment is long enough, test if the name can be drawn on the
		// whole way which may lead to collisions with other way names
		if (!this.wayNameRendered && !this.renderedWayNames.contains(wayName)
				&& getWayLengthInPixel(wayNodes, wayNodesSequence) > this.wayNameWidth) {
			// check to prevent inverted way names
			if (wayNodesSequence[0] > wayNodesSequence[wayNodes - 2]) {
				this.path.rewind();
				this.path.moveTo(scaleLongitude(wayNodesSequence[wayNodes - 2]),
						scaleLatitude(wayNodesSequence[wayNodes - 1]));
				for (short i = (short) (wayNodes - 4); i >= 0; i -= 2) {
					this.path.lineTo(scaleLongitude(wayNodesSequence[i]),
							scaleLatitude(wayNodesSequence[i + 1]));
				}
			}
			this.wayNames.add(new PathTextContainer(this.path, PAINT_NAME_BLACK_10, wayName));
			this.renderedWayNames.add(wayName);
		}
	}

	private void renderPOISymbol(float x, float y, Bitmap symbolBitmap) {
		this.symbols.add((new SymbolContainer(symbolBitmap, x - (symbolBitmap.getWidth() >> 1),
				y - (symbolBitmap.getHeight() >> 1))));
	}

	private float scaleLatitude(int latitude) {
		return (float) (MercatorProjection.latitudeToPixelY(latitude / (double) 1000000,
				this.currentTile.zoomLevel) - this.currentTile.pixelY);
	}

	private float scaleLongitude(int longitude) {
		return (float) (MercatorProjection.longitudeToPixelX(longitude / (double) 1000000,
				this.currentTile.zoomLevel) - this.currentTile.pixelX);
	}

	private void schedule() {
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

	/**
	 * Sets the stroke width of all paints depending on the current zoomLevel level.
	 */
	private void setPaintParameters(byte zoomLevel) {
		float paintScaleFactor;
		switch (zoomLevel) {
			case 25:
				paintScaleFactor = 2048;
				break;
			case 24:
				paintScaleFactor = 1024;
				break;
			case 23:
				paintScaleFactor = 512;
				break;
			case 22:
				paintScaleFactor = 256;
				break;
			case 21:
				paintScaleFactor = 128;
				break;
			case 20:
				paintScaleFactor = 64;
				break;
			case 19:
				paintScaleFactor = 32;
				break;
			case 18:
				paintScaleFactor = 16;
				break;
			case 17:
				paintScaleFactor = 8;
				break;
			case 16:
				paintScaleFactor = 6;
				break;
			case 15:
				paintScaleFactor = 4;
				break;
			case 14:
				paintScaleFactor = 2;
				break;
			case 13:
				paintScaleFactor = 1.5f;
				break;
			default:
				paintScaleFactor = 1;
				break;
		}

		PAINT_HIGHWAY_MOTORWAY1.setStrokeWidth(2.9f * paintScaleFactor);
		PAINT_HIGHWAY_MOTORWAY2.setStrokeWidth(2.6f * paintScaleFactor);
		PAINT_HIGHWAY_MOTORWAY_LINK1.setStrokeWidth(2.6f * paintScaleFactor);
		PAINT_HIGHWAY_MOTORWAY_LINK2.setStrokeWidth(2.3f * paintScaleFactor);
		PAINT_HIGHWAY_TRUNK1.setStrokeWidth(2.6f * paintScaleFactor);
		PAINT_HIGHWAY_TRUNK2.setStrokeWidth(2.3f * paintScaleFactor);
		PAINT_HIGHWAY_TRUNK_LINK1.setStrokeWidth(2.4f * paintScaleFactor);
		PAINT_HIGHWAY_TRUNK_LINK2.setStrokeWidth(2.1f * paintScaleFactor);
		PAINT_HIGHWAY_PRIMARY1.setStrokeWidth(2.1f * paintScaleFactor);
		PAINT_HIGHWAY_PRIMARY2.setStrokeWidth(1.8f * paintScaleFactor);
		PAINT_HIGHWAY_PRIMARY_LINK1.setStrokeWidth(2.1f * paintScaleFactor);
		PAINT_HIGHWAY_PRIMARY_LINK2.setStrokeWidth(1.8f * paintScaleFactor);
		PAINT_HIGHWAY_SECONDARY1.setStrokeWidth(2 * paintScaleFactor);
		PAINT_HIGHWAY_SECONDARY2.setStrokeWidth(1.7f * paintScaleFactor);
		PAINT_HIGHWAY_TERTIARY1.setStrokeWidth(1.8f * paintScaleFactor);
		PAINT_HIGHWAY_TERTIARY2.setStrokeWidth(1.5f * paintScaleFactor);
		PAINT_HIGHWAY_TUNNEL1.setPathEffect(new DashPathEffect(new float[] {
				1.5f * paintScaleFactor, 1.5f * paintScaleFactor }, 0));
		PAINT_HIGHWAY_TUNNEL1.setStrokeWidth(1.3f * paintScaleFactor);
		PAINT_HIGHWAY_TUNNEL2.setStrokeWidth(1 * paintScaleFactor);
		PAINT_HIGHWAY_UNCLASSIFIED1.setStrokeWidth(1.8f * paintScaleFactor);
		PAINT_HIGHWAY_UNCLASSIFIED2.setStrokeWidth(1.5f * paintScaleFactor);
		PAINT_HIGHWAY_ROAD1.setStrokeWidth(1.8f * paintScaleFactor);
		PAINT_HIGHWAY_ROAD2.setStrokeWidth(1.5f * paintScaleFactor);
		PAINT_HIGHWAY_RESIDENTIAL1.setStrokeWidth(1.8f * paintScaleFactor);
		PAINT_HIGHWAY_RESIDENTIAL2.setStrokeWidth(1.5f * paintScaleFactor);
		PAINT_HIGHWAY_LIVING_STREET1.setStrokeWidth(1.8f * paintScaleFactor);
		PAINT_HIGHWAY_LIVING_STREET2.setStrokeWidth(1.5f * paintScaleFactor);
		PAINT_HIGHWAY_SERVICE_AREA_OUTLINE.setStrokeWidth(0.1f * paintScaleFactor);
		PAINT_HIGHWAY_SERVICE1.setStrokeWidth(1.3f * paintScaleFactor);
		PAINT_HIGHWAY_SERVICE2.setStrokeWidth(1 * paintScaleFactor);
		PAINT_HIGHWAY_TRACK1.setStrokeWidth(1.8f * paintScaleFactor);
		PAINT_HIGHWAY_TRACK2.setStrokeWidth(1.5f * paintScaleFactor);
		PAINT_HIGHWAY_PEDESTRIAN_AREA_OUTLINE.setStrokeWidth(0.1f * paintScaleFactor);
		PAINT_HIGHWAY_PEDESTRIAN1.setStrokeWidth(1.8f * paintScaleFactor);
		PAINT_HIGHWAY_PEDESTRIAN2.setStrokeWidth(1.5f * paintScaleFactor);
		PAINT_HIGHWAY_PATH1.setStrokeWidth(0.8f * paintScaleFactor);
		PAINT_HIGHWAY_PATH1.setPathEffect(new DashPathEffect(new float[] {
				1 * paintScaleFactor, 1 * paintScaleFactor }, 0));
		PAINT_HIGHWAY_PATH2.setStrokeWidth(0.5f * paintScaleFactor);
		PAINT_HIGHWAY_CYCLEWAY1.setStrokeWidth(1.8f * paintScaleFactor);
		PAINT_HIGHWAY_CYCLEWAY2.setStrokeWidth(1.5f * paintScaleFactor);
		PAINT_HIGHWAY_FOOTWAY1.setStrokeWidth(0.8f * paintScaleFactor);
		PAINT_HIGHWAY_FOOTWAY1.setPathEffect(new DashPathEffect(new float[] {
				1 * paintScaleFactor, 1 * paintScaleFactor }, 0));
		PAINT_HIGHWAY_FOOTWAY2.setStrokeWidth(0.5f * paintScaleFactor);
		PAINT_HIGHWAY_BRIDLEWAY1.setStrokeWidth(1.8f * paintScaleFactor);
		PAINT_HIGHWAY_BRIDLEWAY2.setStrokeWidth(1.5f * paintScaleFactor);
		PAINT_HIGHWAY_STEPS1.setStrokeWidth(0.8f * paintScaleFactor);
		PAINT_HIGHWAY_STEPS1.setPathEffect(new DashPathEffect(new float[] {
				1 * paintScaleFactor, 1 * paintScaleFactor }, 0));
		PAINT_HIGHWAY_STEPS2.setStrokeWidth(0.5f * paintScaleFactor);
		PAINT_HIGHWAY_STEPS2.setPathEffect(new DashPathEffect(new float[] {
				1 * paintScaleFactor, 1 * paintScaleFactor }, 3));
		PAINT_HIGHWAY_CONSTRUCTION.setStrokeWidth(1.3f * paintScaleFactor);

		PAINT_WATERWAY_CANAL.setStrokeWidth(1.5f * paintScaleFactor);
		PAINT_WATERWAY_RIVER.setStrokeWidth(1 * paintScaleFactor);
		PAINT_WATERWAY_STREAM.setStrokeWidth(0.7f * paintScaleFactor);

		PAINT_RAILWAY_RAIL_TUNNEL1.setPathEffect(new DashPathEffect(new float[] {
				1 * paintScaleFactor, 1 * paintScaleFactor }, 0));
		PAINT_RAILWAY_RAIL_TUNNEL1.setStrokeWidth(0.5f * paintScaleFactor);
		PAINT_RAILWAY_RAIL_TUNNEL2.setStrokeWidth(0.6f * paintScaleFactor);
		PAINT_RAILWAY_RAIL1.setPathEffect(new DashPathEffect(new float[] {
				2 * paintScaleFactor, 2 * paintScaleFactor }, 0));
		PAINT_RAILWAY_RAIL1.setStrokeWidth(0.5f * paintScaleFactor);
		PAINT_RAILWAY_RAIL2.setStrokeWidth(0.6f * paintScaleFactor);
		PAINT_RAILWAY_TRAM1.setStrokeWidth(0.4f * paintScaleFactor);
		PAINT_RAILWAY_TRAM1.setPathEffect(new DashPathEffect(new float[] {
				2 * paintScaleFactor, 2 * paintScaleFactor }, 0));
		PAINT_RAILWAY_TRAM2.setStrokeWidth(0.5f * paintScaleFactor);
		PAINT_RAILWAY_LIGHT_RAIL1.setStrokeWidth(0.4f * paintScaleFactor);
		PAINT_RAILWAY_LIGHT_RAIL1.setPathEffect(new DashPathEffect(new float[] {
				2 * paintScaleFactor, 2 * paintScaleFactor }, 0));
		PAINT_RAILWAY_LIGHT_RAIL2.setStrokeWidth(0.5f * paintScaleFactor);
		PAINT_RAILWAY_SUBWAY1.setPathEffect(new DashPathEffect(new float[] {
				2 * paintScaleFactor, 2 * paintScaleFactor }, 0));
		PAINT_RAILWAY_SUBWAY1.setStrokeWidth(0.4f * paintScaleFactor);
		PAINT_RAILWAY_SUBWAY2.setStrokeWidth(0.5f * paintScaleFactor);
		PAINT_RAILWAY_SUBWAY_TUNNEL1.setPathEffect(new DashPathEffect(new float[] {
				1 * paintScaleFactor, 1 * paintScaleFactor }, 0));
		PAINT_RAILWAY_SUBWAY_TUNNEL1.setStrokeWidth(0.4f * paintScaleFactor);
		PAINT_RAILWAY_SUBWAY_TUNNEL2.setStrokeWidth(0.5f * paintScaleFactor);
		PAINT_RAILWAY_STATION_OUTLINE.setStrokeWidth(0.3f * paintScaleFactor);

		PAINT_AEROWAY_AERODROME_OUTLINE.setStrokeWidth(0.3f * paintScaleFactor);
		PAINT_AEROWAY_RUNWAY1.setStrokeWidth(7.5f * paintScaleFactor);
		PAINT_AEROWAY_RUNWAY2.setStrokeWidth(5 * paintScaleFactor);
		PAINT_AEROWAY_TAXIWAY1.setStrokeWidth(4 * paintScaleFactor);
		PAINT_AEROWAY_TAXIWAY2.setStrokeWidth(3 * paintScaleFactor);
		PAINT_AEROWAY_TERMINAL_OUTLINE.setStrokeWidth(0.3f * paintScaleFactor);

		PAINT_MAN_MADE_PIER.setStrokeWidth(0.8f * paintScaleFactor);

		PAINT_BUILDING_ROOF_OUTLINE.setStrokeWidth(0.1f * paintScaleFactor);
		PAINT_BUILDING_YES_OUTLINE.setStrokeWidth(0.3f * paintScaleFactor);

		PAINT_LEISURE_COMMON_OUTLINE.setStrokeWidth(0.3f * paintScaleFactor);
		PAINT_LEISURE_STADIUM_OUTLINE.setStrokeWidth(0.3f * paintScaleFactor);

		PAINT_AMENITY_SCHOOL_OUTLINE.setStrokeWidth(0.3f * paintScaleFactor);
		PAINT_AMENITY_PARKING_OUTLINE.setStrokeWidth(0.3f * paintScaleFactor);

		PAINT_TOURISM_ZOO_OUTLINE.setStrokeWidth(0.3f * paintScaleFactor);

		PAINT_LANDUSE_GRASS_OUTLINE.setStrokeWidth(0.3f * paintScaleFactor);

		PAINT_ROUTE_FERRY.setPathEffect(new DashPathEffect(new float[] { 3 * paintScaleFactor,
				3 * paintScaleFactor }, 0));
		PAINT_ROUTE_FERRY.setStrokeWidth(1 * paintScaleFactor);

		PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_2.setStrokeWidth(1.5f * paintScaleFactor);
		PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_2.setPathEffect(new DashPathEffect(
				new float[] { 3 * paintScaleFactor, 2 * paintScaleFactor }, 0));
		PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_4.setStrokeWidth(1 * paintScaleFactor);
		PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_4.setPathEffect(new DashPathEffect(
				new float[] { 3 * paintScaleFactor, 2 * paintScaleFactor, 1 * paintScaleFactor,
						2 * paintScaleFactor }, 0));
		PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_6.setStrokeWidth(0.3f * paintScaleFactor);
		PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_6.setPathEffect(new DashPathEffect(
				new float[] { 1 * paintScaleFactor, 4 * paintScaleFactor }, 0));
		PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_8.setStrokeWidth(0.3f * paintScaleFactor);
		PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_8.setPathEffect(new DashPathEffect(
				new float[] { 3 * paintScaleFactor, 2 * paintScaleFactor, 1 * paintScaleFactor,
						2 * paintScaleFactor }, 0));
		PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_9.setStrokeWidth(0.3f * paintScaleFactor);
		PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_9.setPathEffect(new DashPathEffect(
				new float[] { 3 * paintScaleFactor, 2 * paintScaleFactor }, 0));
		PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_10.setStrokeWidth(0.3f * paintScaleFactor);
		PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_10.setPathEffect(new DashPathEffect(
				new float[] { 1 * paintScaleFactor, 4 * paintScaleFactor }, 0));

		PAINT_SPORT_SHOOTING_OUTLINE.setStrokeWidth(0.3f * paintScaleFactor);
		PAINT_SPORT_TENNIS_OUTLINE.setStrokeWidth(0.3f * paintScaleFactor);

		PAINT_NATURAL_COASTLINE_OUTLINE.setStrokeWidth(1 * paintScaleFactor);
	}

	/**
	 * Adds the given tile to the job queue.
	 * 
	 * @param tile
	 *            the tile to be added to the job queue.
	 */
	synchronized void addJob(Tile tile) {
		if (!this.jobQueue1.contains(tile)) {
			this.jobQueue1.offer(tile);
		}
	}

	/**
	 * Clears the job queue.
	 */
	synchronized void clearJobs() {
		this.jobQueue1.clear();
	}

	/**
	 * Returns the status of the MapGenerator.
	 * 
	 * @return true, if the MapGenerator is not working, false otherwise.
	 */
	boolean isReady() {
		return this.ready;
	}

	/**
	 * Request that the MapGenerator should stop working.
	 */
	synchronized void pause() {
		this.pause = true;
	}

	/**
	 * Renders a single POI.
	 * 
	 * @param nodeLayer
	 *            the layer of the node
	 * @param latitude
	 *            the latitude of the node
	 * @param longitude
	 *            the longitude of the node
	 * @param nodeName
	 *            the name of the node
	 * @param nodeTagIds
	 *            the tag id array of the node
	 */
	void renderPointOfInterest(byte nodeLayer, int latitude, int longitude, String nodeName,
			boolean[] nodeTagIds) {
		this.currentNodeX = scaleLongitude(longitude);
		this.currentNodeY = scaleLatitude(latitude);

		/* aeroway */
		if (nodeTagIds[TagIdsPOIs.AEROWAY$HELIPAD]) {
			renderPOISymbol(this.currentNodeX, this.currentNodeY, this.mapSymbols.helipad);
		}

		/* amenity */
		else if (nodeTagIds[TagIdsPOIs.AMENITY$PUB]) {
			if (nodeName != null) {
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 15, PAINT_NAME_RED_10));
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 15, PAINT_NAME_WHITE_STROKE_10));
			}
			renderPOISymbol(this.currentNodeX, this.currentNodeY, this.mapSymbols.pub);
		} else if (nodeTagIds[TagIdsPOIs.AMENITY$CINEMA]) {
			if (nodeName != null) {
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 15, PAINT_NAME_BLUE_10));
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 15, PAINT_NAME_WHITE_STROKE_10));
			}
			renderPOISymbol(this.currentNodeX, this.currentNodeY, this.mapSymbols.cinema);
		} else if (nodeTagIds[TagIdsPOIs.AMENITY$THEATRE]) {
			if (nodeName != null) {
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 15, PAINT_NAME_BLUE_10));
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 15, PAINT_NAME_WHITE_STROKE_10));
			}
			renderPOISymbol(this.currentNodeX, this.currentNodeY, this.mapSymbols.theatre);
		} else if (nodeTagIds[TagIdsPOIs.AMENITY$FIRE_STATION]) {
			if (nodeName != null) {
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 15, PAINT_NAME_BLUE_10));
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 15, PAINT_NAME_WHITE_STROKE_10));
			}
			renderPOISymbol(this.currentNodeX, this.currentNodeY, this.mapSymbols.firebrigade);
		} else if (nodeTagIds[TagIdsPOIs.AMENITY$SHELTER]) {
			if (nodeName != null) {
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 20, PAINT_NAME_BLUE_10));
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 20, PAINT_NAME_WHITE_STROKE_10));
			}
			renderPOISymbol(this.currentNodeX, this.currentNodeY, this.mapSymbols.shelter);
		} else if (nodeTagIds[TagIdsPOIs.AMENITY$SCHOOL]) {
			if (nodeName != null) {
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 15, PAINT_NAME_BLUE_10));
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 15, PAINT_NAME_WHITE_STROKE_10));
			}
			renderPOISymbol(this.currentNodeX, this.currentNodeY, this.mapSymbols.school);
		} else if (nodeTagIds[TagIdsPOIs.AMENITY$UNIVERSITY]) {
			if (nodeName != null) {
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 15, PAINT_NAME_BLUE_10));
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 15, PAINT_NAME_WHITE_STROKE_10));
			}
			renderPOISymbol(this.currentNodeX, this.currentNodeY, this.mapSymbols.university);
		} else if (nodeTagIds[TagIdsPOIs.AMENITY$PLACE_OF_WORSHIP]) {
			if (nodeName != null) {
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 15, PAINT_NAME_BLUE_10));
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 15, PAINT_NAME_WHITE_STROKE_10));
			}
			renderPOISymbol(this.currentNodeX, this.currentNodeY, this.mapSymbols.church);
		} else if (nodeTagIds[TagIdsPOIs.AMENITY$ATM]) {
			if (nodeName != null) {
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 15, PAINT_NAME_BLUE_10));
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 15, PAINT_NAME_WHITE_STROKE_10));
			}
			renderPOISymbol(this.currentNodeX, this.currentNodeY, this.mapSymbols.atm);
		} else if (nodeTagIds[TagIdsPOIs.AMENITY$LIBRARY]) {
			if (nodeName != null) {
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 18, PAINT_NAME_BLUE_10));
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 18, PAINT_NAME_WHITE_STROKE_10));
			}
			renderPOISymbol(this.currentNodeX, this.currentNodeY, this.mapSymbols.library);
		} else if (nodeTagIds[TagIdsPOIs.AMENITY$FAST_FOOD]) {
			if (nodeName != null) {
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 18, PAINT_NAME_BLUE_10));
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 18, PAINT_NAME_WHITE_STROKE_10));
			}
			renderPOISymbol(this.currentNodeX, this.currentNodeY, this.mapSymbols.fastfood);
		} else if (nodeTagIds[TagIdsPOIs.AMENITY$PARKING]) {
			if (nodeName != null) {
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 18, PAINT_NAME_BLUE_10));
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 18, PAINT_NAME_WHITE_STROKE_10));
			}
			renderPOISymbol(this.currentNodeX, this.currentNodeY, this.mapSymbols.parking);
		} else if (nodeTagIds[TagIdsPOIs.AMENITY$HOSPITAL]) {
			if (nodeName != null) {
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 18, PAINT_NAME_BLUE_10));
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 18, PAINT_NAME_WHITE_STROKE_10));
			}
			renderPOISymbol(this.currentNodeX, this.currentNodeY, this.mapSymbols.hospital);
		} else if (nodeTagIds[TagIdsPOIs.AMENITY$RESTAURANT]) {
			if (nodeName != null) {
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 18, PAINT_NAME_BLUE_10));
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 18, PAINT_NAME_WHITE_STROKE_10));
			}
			renderPOISymbol(this.currentNodeX, this.currentNodeY, this.mapSymbols.restaurant);
		} else if (nodeTagIds[TagIdsPOIs.AMENITY$BANK]) {
			if (nodeName != null) {
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 18, PAINT_NAME_BLUE_10));
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 18, PAINT_NAME_WHITE_STROKE_10));
			}
			renderPOISymbol(this.currentNodeX, this.currentNodeY, this.mapSymbols.bank);
		} else if (nodeTagIds[TagIdsPOIs.AMENITY$CAFE]) {
			if (nodeName != null) {
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 18, PAINT_NAME_BLUE_10));
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 18, PAINT_NAME_WHITE_STROKE_10));
			}
			renderPOISymbol(this.currentNodeX, this.currentNodeY, this.mapSymbols.cafe);
		} else if (nodeTagIds[TagIdsPOIs.AMENITY$FUEL]) {
			if (nodeName != null) {
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 18, PAINT_NAME_BLUE_10));
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 18, PAINT_NAME_WHITE_STROKE_10));
			}
			renderPOISymbol(this.currentNodeX, this.currentNodeY, this.mapSymbols.petrolStation);
		} else if (nodeTagIds[TagIdsPOIs.AMENITY$BUS_STATION]) {
			if (nodeName != null) {
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 18, PAINT_NAME_BLUE_10));
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 18, PAINT_NAME_WHITE_STROKE_10));
			}
			renderPOISymbol(this.currentNodeX, this.currentNodeY, this.mapSymbols.bus_sta);
		} else if (nodeTagIds[TagIdsPOIs.AMENITY$POST_BOX]) {
			renderPOISymbol(this.currentNodeX, this.currentNodeY, this.mapSymbols.postbox);
		} else if (nodeTagIds[TagIdsPOIs.AMENITY$POST_OFFICE]) {
			renderPOISymbol(this.currentNodeX, this.currentNodeY, this.mapSymbols.postoffice);
		} else if (nodeTagIds[TagIdsPOIs.AMENITY$PHARMACY]) {
			renderPOISymbol(this.currentNodeX, this.currentNodeY, this.mapSymbols.pharmacy);
		} else if (nodeTagIds[TagIdsPOIs.AMENITY$FOUNTAIN]) {
			renderPOISymbol(this.currentNodeX, this.currentNodeY, this.mapSymbols.fountain);
		} else if (nodeTagIds[TagIdsPOIs.AMENITY$RECYCLING]) {
			renderPOISymbol(this.currentNodeX, this.currentNodeY, this.mapSymbols.recycling);
		} else if (nodeTagIds[TagIdsPOIs.AMENITY$TELEPHONE]) {
			renderPOISymbol(this.currentNodeX, this.currentNodeY, this.mapSymbols.telephone);
		} else if (nodeTagIds[TagIdsPOIs.AMENITY$TOILETS]) {
			renderPOISymbol(this.currentNodeX, this.currentNodeY, this.mapSymbols.toilets);
		} else if (nodeTagIds[TagIdsPOIs.AMENITY$BICYCLE_RENTAL]) {
			renderPOISymbol(this.currentNodeX, this.currentNodeY,
					this.mapSymbols.bicycle_rental);
		}

		/* barrier */
		else if (nodeTagIds[TagIdsPOIs.BARRIER$BOLLARD]) {
			this.path = new Path();
			this.path.addCircle(this.currentNodeX, this.currentNodeY, 1.5f, Path.Direction.CCW);
			this.ways.get(nodeLayer).get(LayerIds.POI_CIRCLE_SYMBOL).add(
					new PathContainer(this.path, PAINT_BARRIER_BOLLARD));
		}

		/* highway */
		else if (nodeTagIds[TagIdsPOIs.HIGHWAY$BUS_STOP]) {
			renderPOISymbol(this.currentNodeX, this.currentNodeY, this.mapSymbols.bus);
		} else if (nodeTagIds[TagIdsPOIs.HIGHWAY$TRAFFIC_SIGNALS]) {
			renderPOISymbol(this.currentNodeX, this.currentNodeY,
					this.mapSymbols.traffic_signal);
		}

		/* historic */
		else if (nodeTagIds[TagIdsPOIs.HISTORIC$MEMORIAL]
				|| nodeTagIds[TagIdsPOIs.HISTORIC$MONUMENT]) {
			this.path = new Path();
			this.path.addCircle(this.currentNodeX, this.currentNodeY, 3, Path.Direction.CCW);
			this.ways.get(nodeLayer).get(LayerIds.POI_CIRCLE_SYMBOL).add(
					new PathContainer(this.path, PAINT_HISTORIC_CIRCLE_INNER));
			this.ways.get(nodeLayer).get(LayerIds.POI_CIRCLE_SYMBOL).add(
					new PathContainer(this.path, PAINT_HISTORIC_CIRCLE_OUTER));
			if (nodeName != null) {
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 15, PAINT_NAME_BLUE_10));
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 15, PAINT_NAME_WHITE_STROKE_10));
			}
		}

		/* leisure */
		else if (nodeTagIds[TagIdsPOIs.LEISURE$PLAYGROUND]) {
			renderPOISymbol(this.currentNodeX, this.currentNodeY, this.mapSymbols.playground);
		}

		/* man_made */
		else if (nodeTagIds[TagIdsPOIs.MAN_MADE$WINDMILL]) {
			renderPOISymbol(this.currentNodeX, this.currentNodeY, this.mapSymbols.windmill);
		}

		/* natural */
		else if (nodeTagIds[TagIdsPOIs.NATURAL$PEAK]) {
			if (nodeName != null) {
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 15, PAINT_NAME_BLACK_12));
			}
			renderPOISymbol(this.currentNodeX, this.currentNodeY, this.mapSymbols.peak);
		}

		/* place */
		else if (nodeTagIds[TagIdsPOIs.PLACE$CITY]) {
			if (nodeName != null) {
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY, PAINT_NAME_BLACK_25));
			}
		} else if (nodeTagIds[TagIdsPOIs.PLACE$ISLAND]) {
			if (nodeName != null) {
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY, PAINT_NAME_BLACK_20));
			}
		} else if (nodeTagIds[TagIdsPOIs.PLACE$SUBURB] || nodeTagIds[TagIdsPOIs.PLACE$TOWN]
				|| nodeTagIds[TagIdsPOIs.PLACE$VILLAGE]) {
			if (nodeName != null) {
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY, PAINT_NAME_BLACK_15));
			}
		}

		/* railway */
		else if (nodeTagIds[TagIdsPOIs.RAILWAY$LEVEL_CROSSING]) {
			renderPOISymbol(this.currentNodeX, this.currentNodeY,
					this.mapSymbols.railway_crossing);
		} else if (nodeTagIds[TagIdsPOIs.RAILWAY$STATION]) {
			if (nodeTagIds[TagIdsPOIs.STATION$LIGHT_RAIL]
					|| nodeTagIds[TagIdsPOIs.STATION$SUBWAY]) {
				this.path = new Path();
				this.path
						.addCircle(this.currentNodeX, this.currentNodeY, 4, Path.Direction.CCW);
				this.ways.get(nodeLayer).get(LayerIds.POI_CIRCLE_SYMBOL).add(
						new PathContainer(this.path, PAINT_RAILWAY_CIRCLE_INNER));
				this.ways.get(nodeLayer).get(LayerIds.POI_CIRCLE_SYMBOL).add(
						new PathContainer(this.path, PAINT_RAILWAY_CIRCLE_OUTER));
				if (nodeName != null) {
					this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
							this.currentNodeY - 10, PAINT_NAME_RED_11));
					this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
							this.currentNodeY - 10, PAINT_NAME_WHITE_STROKE_11));
				}
			} else {
				this.path = new Path();
				this.path
						.addCircle(this.currentNodeX, this.currentNodeY, 6, Path.Direction.CCW);
				this.ways.get(nodeLayer).get(LayerIds.POI_CIRCLE_SYMBOL).add(
						new PathContainer(this.path, PAINT_RAILWAY_CIRCLE_INNER));
				this.ways.get(nodeLayer).get(LayerIds.POI_CIRCLE_SYMBOL).add(
						new PathContainer(this.path, PAINT_RAILWAY_CIRCLE_OUTER));
				if (nodeName != null) {
					this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
							this.currentNodeY - 15, PAINT_NAME_RED_13));
					this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
							this.currentNodeY - 15, PAINT_NAME_WHITE_STROKE_13));
				}
			}
		} else if (nodeTagIds[TagIdsPOIs.RAILWAY$HALT]
				|| nodeTagIds[TagIdsPOIs.RAILWAY$TRAM_STOP]) {
			this.path = new Path();
			this.path.addCircle(this.currentNodeX, this.currentNodeY, 4, Path.Direction.CCW);
			this.ways.get(nodeLayer).get(LayerIds.POI_CIRCLE_SYMBOL).add(
					new PathContainer(this.path, PAINT_RAILWAY_CIRCLE_INNER));
			this.ways.get(nodeLayer).get(LayerIds.POI_CIRCLE_SYMBOL).add(
					new PathContainer(this.path, PAINT_RAILWAY_CIRCLE_OUTER));
			if (nodeName != null) {
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 10, PAINT_NAME_RED_11));
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 10, PAINT_NAME_WHITE_STROKE_11));
			}
		}

		/* shop */
		else if (nodeTagIds[TagIdsPOIs.SHOP$BAKERY]) {
			if (nodeName != null) {
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 15, PAINT_NAME_BLUE_10));
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 15, PAINT_NAME_WHITE_STROKE_10));
			}
			renderPOISymbol(this.currentNodeX, this.currentNodeY, this.mapSymbols.bakery);
		} else if (nodeTagIds[TagIdsPOIs.SHOP$ORGANIC]) {
			if (nodeName != null) {
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 15, PAINT_NAME_BLUE_10));
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 15, PAINT_NAME_WHITE_STROKE_10));
			}

		} else if (nodeTagIds[TagIdsPOIs.SHOP$SUPERMARKET]) {
			if (nodeName != null) {
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 18, PAINT_NAME_BLUE_10));
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 18, PAINT_NAME_WHITE_STROKE_10));
			}
			renderPOISymbol(this.currentNodeX, this.currentNodeY, this.mapSymbols.supermarket);
		}

		/* tourism */
		else if (nodeTagIds[TagIdsPOIs.TOURISM$INFORMATION]) {
			renderPOISymbol(this.currentNodeX, this.currentNodeY, this.mapSymbols.information);
		} else if (nodeTagIds[TagIdsPOIs.TOURISM$MUSEUM]) {
			if (nodeName != null) {
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 15, PAINT_NAME_BLUE_10));
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 15, PAINT_NAME_WHITE_STROKE_10));
			}
		} else if (nodeTagIds[TagIdsPOIs.TOURISM$HOSTEL]) {
			if (nodeName != null) {
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 18, PAINT_NAME_BLUE_10));
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 18, PAINT_NAME_WHITE_STROKE_10));
			}
			renderPOISymbol(this.currentNodeX, this.currentNodeY, this.mapSymbols.hostel);
		} else if (nodeTagIds[TagIdsPOIs.TOURISM$HOTEL]) {
			if (nodeName != null) {
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 18, PAINT_NAME_BLUE_10));
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY - 18, PAINT_NAME_WHITE_STROKE_10));
			}
			renderPOISymbol(this.currentNodeX, this.currentNodeY, this.mapSymbols.hotel);
		} else if (nodeTagIds[TagIdsPOIs.TOURISM$ATTRACTION]) {
			if (nodeName != null) {
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY, PAINT_NAME_PURPLE_10));
				this.nodes.add(new PointContainer(nodeName, this.currentNodeX,
						this.currentNodeY, PAINT_NAME_WHITE_STROKE_10));
			}
		} else if (nodeTagIds[TagIdsPOIs.TOURISM$VIEWPOINT]) {
			renderPOISymbol(this.currentNodeX, this.currentNodeY, this.mapSymbols.viewpoint);
		}

		/* unknown */
		else {
			Logger.d("unknown node");
			return;
		}
	}

	/**
	 * Renders a single way.
	 * 
	 * @param wayLayer
	 *            the layer of the way
	 * @param wayNumberOfRealTags
	 *            the number of real tags
	 * @param wayName
	 *            the name of the way
	 * @param wayTagIds
	 *            the tag id array of the way
	 * @param wayTagBitmap
	 *            the way tag bitmap
	 * @param wayNodes
	 *            the number of node positions
	 * @param wayNodesSequence
	 *            the node positions
	 * @param innerWays
	 *            the inner nodes if this way is a multipolygon
	 */
	void renderWay(byte wayLayer, byte wayNumberOfRealTags, String wayName,
			boolean[] wayTagIds, byte wayTagBitmap, short wayNodes, int[] wayNodesSequence,
			int[][] innerWays) {
		this.remainingTags = wayNumberOfRealTags;
		this.path = new Path();
		this.path.incReserve(wayNodes);
		this.path.moveTo(scaleLongitude(wayNodesSequence[0]),
				scaleLatitude(wayNodesSequence[1]));
		for (short i = 2; i < wayNodes; i += 2) {
			this.path.lineTo(scaleLongitude(wayNodesSequence[i]),
					scaleLatitude(wayNodesSequence[i + 1]));
		}

		if (innerWays != null) {
			for (int[] innerWay : innerWays) {
				this.innerWayLength = innerWay.length;
				this.innerPath = new Path();
				this.innerPath.moveTo(scaleLongitude(innerWay[0]), scaleLatitude(innerWay[1]));
				for (short i = 2; i < this.innerWayLength; i += 2) {
					this.innerPath.lineTo(scaleLongitude(innerWay[i]),
							scaleLatitude(innerWay[i + 1]));
				}
				this.path.addPath(this.innerPath);
			}
			this.path.setFillType(Path.FillType.EVEN_ODD);
		}

		this.layer = this.ways.get(wayLayer);

		/* highway */
		if ((wayTagBitmap & BITMAP_HIGHWAY) != 0) {
			if (wayTagIds[TagIdsWays.TUNNEL$YES]) {
				this.layer.get(LayerIds.HIGHWAY_TUNNEL$YES1).add(
						new PathContainer(this.path, PAINT_HIGHWAY_TUNNEL1));
				this.layer.get(LayerIds.HIGHWAY_TUNNEL$YES2).add(
						new PathContainer(this.path, PAINT_HIGHWAY_TUNNEL2));
			} else if (wayTagIds[TagIdsWays.HIGHWAY$MOTORWAY]) {
				if (wayTagIds[TagIdsWays.BRIDGE$YES]) {
					Paint paint1Bridge = new Paint(PAINT_HIGHWAY_MOTORWAY1);
					paint1Bridge.setStrokeCap(Paint.Cap.BUTT);
					Paint paint2Bridge = new Paint(PAINT_HIGHWAY_MOTORWAY2);
					paint2Bridge.setStrokeCap(Paint.Cap.SQUARE);
					this.layer.get(LayerIds.HIGHWAY$MOTORWAY1).add(
							new PathContainer(this.path, paint1Bridge));
					this.layer.get(LayerIds.HIGHWAY$MOTORWAY2).add(
							new PathContainer(this.path, paint2Bridge));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				} else {
					this.layer.get(LayerIds.HIGHWAY$MOTORWAY1).add(
							new PathContainer(this.path, PAINT_HIGHWAY_MOTORWAY1));
					this.layer.get(LayerIds.HIGHWAY$MOTORWAY2).add(
							new PathContainer(this.path, PAINT_HIGHWAY_MOTORWAY2));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				}
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.HIGHWAY$MOTORWAY_LINK]) {
				if (wayTagIds[TagIdsWays.BRIDGE$YES]) {
					Paint paint1Bridge = new Paint(PAINT_HIGHWAY_MOTORWAY_LINK1);
					paint1Bridge.setStrokeCap(Paint.Cap.BUTT);
					Paint paint2Bridge = new Paint(PAINT_HIGHWAY_MOTORWAY_LINK2);
					paint2Bridge.setStrokeCap(Paint.Cap.SQUARE);
					this.layer.get(LayerIds.HIGHWAY$MOTORWAY_LINK1).add(
							new PathContainer(this.path, paint1Bridge));
					this.layer.get(LayerIds.HIGHWAY$MOTORWAY_LINK2).add(
							new PathContainer(this.path, paint2Bridge));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				} else {
					this.layer.get(LayerIds.HIGHWAY$MOTORWAY_LINK1).add(
							new PathContainer(this.path, PAINT_HIGHWAY_MOTORWAY_LINK1));
					this.layer.get(LayerIds.HIGHWAY$MOTORWAY_LINK2).add(
							new PathContainer(this.path, PAINT_HIGHWAY_MOTORWAY_LINK2));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				}
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.HIGHWAY$TRUNK]) {
				if (wayTagIds[TagIdsWays.BRIDGE$YES]) {
					Paint paint1Bridge = new Paint(PAINT_HIGHWAY_TRUNK1);
					paint1Bridge.setStrokeCap(Paint.Cap.BUTT);
					Paint paint2Bridge = new Paint(PAINT_HIGHWAY_TRUNK2);
					paint2Bridge.setStrokeCap(Paint.Cap.SQUARE);
					this.layer.get(LayerIds.HIGHWAY$TRUNK1).add(
							new PathContainer(this.path, paint1Bridge));
					this.layer.get(LayerIds.HIGHWAY$TRUNK2).add(
							new PathContainer(this.path, paint2Bridge));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				} else {
					this.layer.get(LayerIds.HIGHWAY$TRUNK1).add(
							new PathContainer(this.path, PAINT_HIGHWAY_TRUNK1));
					this.layer.get(LayerIds.HIGHWAY$TRUNK2).add(
							new PathContainer(this.path, PAINT_HIGHWAY_TRUNK2));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				}
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.HIGHWAY$TRUNK_LINK]) {
				if (wayTagIds[TagIdsWays.BRIDGE$YES]) {
					Paint paint1Bridge = new Paint(PAINT_HIGHWAY_TRUNK_LINK1);
					paint1Bridge.setStrokeCap(Paint.Cap.BUTT);
					Paint paint2Bridge = new Paint(PAINT_HIGHWAY_TRUNK_LINK2);
					paint2Bridge.setStrokeCap(Paint.Cap.SQUARE);
					this.layer.get(LayerIds.HIGHWAY$TRUNK_LINK1).add(
							new PathContainer(this.path, paint1Bridge));
					this.layer.get(LayerIds.HIGHWAY$TRUNK_LINK2).add(
							new PathContainer(this.path, paint2Bridge));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				} else {
					this.layer.get(LayerIds.HIGHWAY$TRUNK_LINK1).add(
							new PathContainer(this.path, PAINT_HIGHWAY_TRUNK_LINK1));
					this.layer.get(LayerIds.HIGHWAY$TRUNK_LINK2).add(
							new PathContainer(this.path, PAINT_HIGHWAY_TRUNK_LINK2));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				}
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.HIGHWAY$PRIMARY]) {
				if (wayTagIds[TagIdsWays.BRIDGE$YES]) {
					Paint paint1Bridge = new Paint(PAINT_HIGHWAY_PRIMARY1);
					paint1Bridge.setStrokeCap(Paint.Cap.BUTT);
					Paint paint2Bridge = new Paint(PAINT_HIGHWAY_PRIMARY2);
					paint2Bridge.setStrokeCap(Paint.Cap.SQUARE);
					this.layer.get(LayerIds.HIGHWAY$PRIMARY1).add(
							new PathContainer(this.path, paint1Bridge));
					this.layer.get(LayerIds.HIGHWAY$PRIMARY2).add(
							new PathContainer(this.path, paint2Bridge));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				} else {
					this.layer.get(LayerIds.HIGHWAY$PRIMARY1).add(
							new PathContainer(this.path, PAINT_HIGHWAY_PRIMARY1));
					this.layer.get(LayerIds.HIGHWAY$PRIMARY2).add(
							new PathContainer(this.path, PAINT_HIGHWAY_PRIMARY2));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				}
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.HIGHWAY$PRIMARY_LINK]) {
				if (wayTagIds[TagIdsWays.BRIDGE$YES]) {
					Paint paint1Bridge = new Paint(PAINT_HIGHWAY_PRIMARY_LINK1);
					paint1Bridge.setStrokeCap(Paint.Cap.BUTT);
					Paint paint2Bridge = new Paint(PAINT_HIGHWAY_PRIMARY_LINK2);
					paint2Bridge.setStrokeCap(Paint.Cap.SQUARE);
					this.layer.get(LayerIds.HIGHWAY$PRIMARY_LINK1).add(
							new PathContainer(this.path, paint1Bridge));
					this.layer.get(LayerIds.HIGHWAY$PRIMARY_LINK2).add(
							new PathContainer(this.path, paint2Bridge));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				} else {
					this.layer.get(LayerIds.HIGHWAY$PRIMARY_LINK1).add(
							new PathContainer(this.path, PAINT_HIGHWAY_PRIMARY_LINK1));
					this.layer.get(LayerIds.HIGHWAY$PRIMARY_LINK2).add(
							new PathContainer(this.path, PAINT_HIGHWAY_PRIMARY_LINK2));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				}
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.HIGHWAY$SECONDARY]) {
				if (wayTagIds[TagIdsWays.BRIDGE$YES]) {
					Paint paint1Bridge = new Paint(PAINT_HIGHWAY_SECONDARY1);
					paint1Bridge.setStrokeCap(Paint.Cap.BUTT);
					Paint paint2Bridge = new Paint(PAINT_HIGHWAY_SECONDARY2);
					paint2Bridge.setStrokeCap(Paint.Cap.SQUARE);
					this.layer.get(LayerIds.HIGHWAY$SECONDARY1).add(
							new PathContainer(this.path, paint1Bridge));
					this.layer.get(LayerIds.HIGHWAY$SECONDARY2).add(
							new PathContainer(this.path, paint2Bridge));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				} else {
					this.layer.get(LayerIds.HIGHWAY$SECONDARY1).add(
							new PathContainer(this.path, PAINT_HIGHWAY_SECONDARY1));
					this.layer.get(LayerIds.HIGHWAY$SECONDARY2).add(
							new PathContainer(this.path, PAINT_HIGHWAY_SECONDARY2));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				}
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.HIGHWAY$TERTIARY]) {
				if (wayTagIds[TagIdsWays.BRIDGE$YES]) {
					Paint paint1Bridge = new Paint(PAINT_HIGHWAY_TERTIARY1);
					paint1Bridge.setStrokeCap(Paint.Cap.BUTT);
					Paint paint2Bridge = new Paint(PAINT_HIGHWAY_TERTIARY2);
					paint2Bridge.setStrokeCap(Paint.Cap.SQUARE);
					this.layer.get(LayerIds.HIGHWAY$TERTIARY1).add(
							new PathContainer(this.path, paint1Bridge));
					this.layer.get(LayerIds.HIGHWAY$TERTIARY2).add(
							new PathContainer(this.path, paint2Bridge));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				} else {
					this.layer.get(LayerIds.HIGHWAY$TERTIARY1).add(
							new PathContainer(this.path, PAINT_HIGHWAY_TERTIARY1));
					this.layer.get(LayerIds.HIGHWAY$TERTIARY2).add(
							new PathContainer(this.path, PAINT_HIGHWAY_TERTIARY2));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				}
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.HIGHWAY$UNCLASSIFIED]) {
				if (wayTagIds[TagIdsWays.BRIDGE$YES]) {
					Paint paint1Bridge = new Paint(PAINT_HIGHWAY_UNCLASSIFIED1);
					paint1Bridge.setStrokeCap(Paint.Cap.BUTT);
					Paint paint2Bridge = new Paint(PAINT_HIGHWAY_UNCLASSIFIED2);
					paint2Bridge.setStrokeCap(Paint.Cap.SQUARE);
					this.layer.get(LayerIds.HIGHWAY$UNCLASSIFIED1).add(
							new PathContainer(this.path, paint1Bridge));
					this.layer.get(LayerIds.HIGHWAY$UNCLASSIFIED2).add(
							new PathContainer(this.path, paint2Bridge));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				} else {
					this.layer.get(LayerIds.HIGHWAY$UNCLASSIFIED1).add(
							new PathContainer(this.path, PAINT_HIGHWAY_UNCLASSIFIED1));
					this.layer.get(LayerIds.HIGHWAY$UNCLASSIFIED2).add(
							new PathContainer(this.path, PAINT_HIGHWAY_UNCLASSIFIED2));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				}
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.HIGHWAY$ROAD]) {
				if (wayTagIds[TagIdsWays.BRIDGE$YES]) {
					Paint paint1Bridge = new Paint(PAINT_HIGHWAY_ROAD1);
					paint1Bridge.setStrokeCap(Paint.Cap.BUTT);
					Paint paint2Bridge = new Paint(PAINT_HIGHWAY_ROAD2);
					paint2Bridge.setStrokeCap(Paint.Cap.SQUARE);
					this.layer.get(LayerIds.HIGHWAY$ROAD1).add(
							new PathContainer(this.path, paint1Bridge));
					this.layer.get(LayerIds.HIGHWAY$ROAD2).add(
							new PathContainer(this.path, paint2Bridge));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				} else {
					this.layer.get(LayerIds.HIGHWAY$ROAD1).add(
							new PathContainer(this.path, PAINT_HIGHWAY_ROAD1));
					this.layer.get(LayerIds.HIGHWAY$ROAD2).add(
							new PathContainer(this.path, PAINT_HIGHWAY_ROAD2));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				}
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.HIGHWAY$RESIDENTIAL]) {
				if (wayTagIds[TagIdsWays.BRIDGE$YES]) {
					Paint paint1Bridge = new Paint(PAINT_HIGHWAY_RESIDENTIAL1);
					paint1Bridge.setStrokeCap(Paint.Cap.BUTT);
					Paint paint2Bridge = new Paint(PAINT_HIGHWAY_RESIDENTIAL2);
					paint2Bridge.setStrokeCap(Paint.Cap.SQUARE);
					this.layer.get(LayerIds.HIGHWAY$RESIDENTIAL1).add(
							new PathContainer(this.path, paint1Bridge));
					this.layer.get(LayerIds.HIGHWAY$RESIDENTIAL2).add(
							new PathContainer(this.path, paint2Bridge));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				} else {
					this.layer.get(LayerIds.HIGHWAY$RESIDENTIAL1).add(
							new PathContainer(this.path, PAINT_HIGHWAY_RESIDENTIAL1));
					this.layer.get(LayerIds.HIGHWAY$RESIDENTIAL2).add(
							new PathContainer(this.path, PAINT_HIGHWAY_RESIDENTIAL2));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				}
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.HIGHWAY$LIVING_STREET]) {
				if (wayTagIds[TagIdsWays.BRIDGE$YES]) {
					Paint paint1Bridge = new Paint(PAINT_HIGHWAY_LIVING_STREET1);
					paint1Bridge.setStrokeCap(Paint.Cap.BUTT);
					Paint paint2Bridge = new Paint(PAINT_HIGHWAY_LIVING_STREET2);
					paint2Bridge.setStrokeCap(Paint.Cap.SQUARE);
					this.layer.get(LayerIds.HIGHWAY$LIVING_STREET1).add(
							new PathContainer(this.path, paint1Bridge));
					this.layer.get(LayerIds.HIGHWAY$LIVING_STREET2).add(
							new PathContainer(this.path, paint2Bridge));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				} else {
					this.layer.get(LayerIds.HIGHWAY$LIVING_STREET1).add(
							new PathContainer(this.path, PAINT_HIGHWAY_LIVING_STREET1));
					this.layer.get(LayerIds.HIGHWAY$LIVING_STREET2).add(
							new PathContainer(this.path, PAINT_HIGHWAY_LIVING_STREET2));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				}
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.HIGHWAY$SERVICE]) {
				if (wayTagIds[TagIdsWays.AREA$YES]) {
					this.layer.get(LayerIds.HIGHWAY$SERVICE_AREA$YES).add(
							new PathContainer(this.path, PAINT_HIGHWAY_SERVICE_AREA_OUTLINE));
					this.layer.get(LayerIds.HIGHWAY$SERVICE_AREA$YES).add(
							new PathContainer(this.path, PAINT_HIGHWAY_SERVICE_AREA_FILL));
					renderAreaName(wayName, wayNodes, wayNodesSequence, MODE_AREA_NAME_BLUE,
							(byte) 0);
				} else if (wayTagIds[TagIdsWays.BRIDGE$YES]) {
					Paint paint1Bridge = new Paint(PAINT_HIGHWAY_SERVICE1);
					paint1Bridge.setStrokeCap(Paint.Cap.BUTT);
					Paint paint2Bridge = new Paint(PAINT_HIGHWAY_SERVICE2);
					paint2Bridge.setStrokeCap(Paint.Cap.SQUARE);
					this.layer.get(LayerIds.HIGHWAY$SERVICE1).add(
							new PathContainer(this.path, paint1Bridge));
					this.layer.get(LayerIds.HIGHWAY$SERVICE2).add(
							new PathContainer(this.path, paint2Bridge));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				} else {
					this.layer.get(LayerIds.HIGHWAY$SERVICE1).add(
							new PathContainer(this.path, PAINT_HIGHWAY_SERVICE1));
					this.layer.get(LayerIds.HIGHWAY$SERVICE2).add(
							new PathContainer(this.path, PAINT_HIGHWAY_SERVICE2));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				}
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.HIGHWAY$TRACK]) {
				if (wayTagIds[TagIdsWays.BRIDGE$YES]) {
					Paint paint1Bridge = new Paint(PAINT_HIGHWAY_TRACK1);
					paint1Bridge.setStrokeCap(Paint.Cap.BUTT);
					Paint paint2Bridge = new Paint(PAINT_HIGHWAY_TRACK2);
					paint2Bridge.setStrokeCap(Paint.Cap.SQUARE);
					this.layer.get(LayerIds.HIGHWAY$TRACK1).add(
							new PathContainer(this.path, paint1Bridge));
					this.layer.get(LayerIds.HIGHWAY$TRACK2).add(
							new PathContainer(this.path, paint2Bridge));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				} else {
					this.layer.get(LayerIds.HIGHWAY$TRACK1).add(
							new PathContainer(this.path, PAINT_HIGHWAY_TRACK1));
					this.layer.get(LayerIds.HIGHWAY$TRACK2).add(
							new PathContainer(this.path, PAINT_HIGHWAY_TRACK2));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				}
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.HIGHWAY$PEDESTRIAN]) {
				if (wayTagIds[TagIdsWays.AREA$YES]) {
					this.layer.get(LayerIds.HIGHWAY$PEDESTRIAN_AREA$YES)
							.add(
									new PathContainer(this.path,
											PAINT_HIGHWAY_PEDESTRIAN_AREA_OUTLINE));
					this.layer.get(LayerIds.HIGHWAY$PEDESTRIAN_AREA$YES).add(
							new PathContainer(this.path, PAINT_HIGHWAY_PEDESTRIAN_AREA_FILL));
					renderAreaName(wayName, wayNodes, wayNodesSequence, MODE_AREA_NAME_BLUE,
							(byte) 0);
				} else if (wayTagIds[TagIdsWays.BRIDGE$YES]) {
					Paint paint1Bridge = new Paint(PAINT_HIGHWAY_PEDESTRIAN1);
					paint1Bridge.setStrokeCap(Paint.Cap.BUTT);
					Paint paint2Bridge = new Paint(PAINT_HIGHWAY_PEDESTRIAN2);
					paint2Bridge.setStrokeCap(Paint.Cap.SQUARE);
					this.layer.get(LayerIds.HIGHWAY$PEDESTRIAN1).add(
							new PathContainer(this.path, paint1Bridge));
					this.layer.get(LayerIds.HIGHWAY$PEDESTRIAN2).add(
							new PathContainer(this.path, paint2Bridge));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				} else {
					this.layer.get(LayerIds.HIGHWAY$PEDESTRIAN1).add(
							new PathContainer(this.path, PAINT_HIGHWAY_PEDESTRIAN1));
					this.layer.get(LayerIds.HIGHWAY$PEDESTRIAN2).add(
							new PathContainer(this.path, PAINT_HIGHWAY_PEDESTRIAN2));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				}
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.HIGHWAY$PATH]) {
				if (wayTagIds[TagIdsWays.BRIDGE$YES]) {
					Paint paint1Bridge = new Paint(PAINT_HIGHWAY_PATH1);
					paint1Bridge.setStrokeCap(Paint.Cap.BUTT);
					Paint paint2Bridge = new Paint(PAINT_HIGHWAY_PATH2);
					paint2Bridge.setStrokeCap(Paint.Cap.SQUARE);
					this.layer.get(LayerIds.HIGHWAY$PATH1).add(
							new PathContainer(this.path, paint1Bridge));
					this.layer.get(LayerIds.HIGHWAY$PATH2).add(
							new PathContainer(this.path, paint2Bridge));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				} else {
					this.layer.get(LayerIds.HIGHWAY$PATH1).add(
							new PathContainer(this.path, PAINT_HIGHWAY_PATH1));
					this.layer.get(LayerIds.HIGHWAY$PATH2).add(
							new PathContainer(this.path, PAINT_HIGHWAY_PATH2));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				}
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.HIGHWAY$CYCLEWAY]) {
				if (wayTagIds[TagIdsWays.BRIDGE$YES]) {
					Paint paint1Bridge = new Paint(PAINT_HIGHWAY_CYCLEWAY1);
					paint1Bridge.setStrokeCap(Paint.Cap.BUTT);
					Paint paint2Bridge = new Paint(PAINT_HIGHWAY_CYCLEWAY2);
					paint2Bridge.setStrokeCap(Paint.Cap.SQUARE);
					this.layer.get(LayerIds.HIGHWAY$CYCLEWAY1).add(
							new PathContainer(this.path, paint1Bridge));
					this.layer.get(LayerIds.HIGHWAY$CYCLEWAY2).add(
							new PathContainer(this.path, paint2Bridge));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				} else {
					this.layer.get(LayerIds.HIGHWAY$CYCLEWAY1).add(
							new PathContainer(this.path, PAINT_HIGHWAY_CYCLEWAY1));
					this.layer.get(LayerIds.HIGHWAY$CYCLEWAY2).add(
							new PathContainer(this.path, PAINT_HIGHWAY_CYCLEWAY2));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				}
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.HIGHWAY$FOOTWAY]) {
				if (wayTagIds[TagIdsWays.AREA$YES]) {
					this.layer.get(LayerIds.HIGHWAY$FOOTWAY_AREA$YES).add(
							new PathContainer(this.path, PAINT_HIGHWAY_FOOTWAY_AREA_OUTLINE));
					this.layer.get(LayerIds.HIGHWAY$FOOTWAY_AREA$YES).add(
							new PathContainer(this.path, PAINT_HIGHWAY_FOOTWAY_AREA_FILL));
					renderAreaName(wayName, wayNodes, wayNodesSequence, MODE_AREA_NAME_BLUE,
							(byte) 0);
				} else if (wayTagIds[TagIdsWays.BRIDGE$YES]) {
					Paint paint1Bridge = new Paint(PAINT_HIGHWAY_FOOTWAY1);
					paint1Bridge.setStrokeCap(Paint.Cap.BUTT);
					Paint paint2Bridge = new Paint(PAINT_HIGHWAY_FOOTWAY2);
					paint2Bridge.setStrokeCap(Paint.Cap.SQUARE);
					this.layer.get(LayerIds.HIGHWAY$FOOTWAY1).add(
							new PathContainer(this.path, paint1Bridge));
					this.layer.get(LayerIds.HIGHWAY$FOOTWAY2).add(
							new PathContainer(this.path, paint2Bridge));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				} else {
					this.layer.get(LayerIds.HIGHWAY$FOOTWAY1).add(
							new PathContainer(this.path, PAINT_HIGHWAY_FOOTWAY1));
					this.layer.get(LayerIds.HIGHWAY$FOOTWAY2).add(
							new PathContainer(this.path, PAINT_HIGHWAY_FOOTWAY2));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				}
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.HIGHWAY$BRIDLEWAY]) {
				if (wayTagIds[TagIdsWays.BRIDGE$YES]) {
					Paint paint1Bridge = new Paint(PAINT_HIGHWAY_BRIDLEWAY1);
					paint1Bridge.setStrokeCap(Paint.Cap.BUTT);
					Paint paint2Bridge = new Paint(PAINT_HIGHWAY_BRIDLEWAY2);
					paint2Bridge.setStrokeCap(Paint.Cap.SQUARE);
					this.layer.get(LayerIds.HIGHWAY$BRIDLEWAY1).add(
							new PathContainer(this.path, paint1Bridge));
					this.layer.get(LayerIds.HIGHWAY$BRIDLEWAY2).add(
							new PathContainer(this.path, paint2Bridge));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				} else {
					this.layer.get(LayerIds.HIGHWAY$BRIDLEWAY1).add(
							new PathContainer(this.path, PAINT_HIGHWAY_BRIDLEWAY1));
					this.layer.get(LayerIds.HIGHWAY$BRIDLEWAY2).add(
							new PathContainer(this.path, PAINT_HIGHWAY_BRIDLEWAY2));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				}
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.HIGHWAY$STEPS]) {
				if (wayTagIds[TagIdsWays.BRIDGE$YES]) {
					Paint paint1Bridge = new Paint(PAINT_HIGHWAY_STEPS1);
					paint1Bridge.setStrokeCap(Paint.Cap.BUTT);
					Paint paint2Bridge = new Paint(PAINT_HIGHWAY_STEPS2);
					paint2Bridge.setStrokeCap(Paint.Cap.SQUARE);
					this.layer.get(LayerIds.HIGHWAY$STEPS1).add(
							new PathContainer(this.path, paint1Bridge));
					this.layer.get(LayerIds.HIGHWAY$STEPS2).add(
							new PathContainer(this.path, paint2Bridge));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				} else {
					this.layer.get(LayerIds.HIGHWAY$STEPS1).add(
							new PathContainer(this.path, PAINT_HIGHWAY_STEPS1));
					this.layer.get(LayerIds.HIGHWAY$STEPS2).add(
							new PathContainer(this.path, PAINT_HIGHWAY_STEPS2));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				}
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.HIGHWAY$CONSTRUCTION]) {
				if (wayTagIds[TagIdsWays.BRIDGE$YES]) {
					Paint paint1Bridge = new Paint(PAINT_HIGHWAY_CONSTRUCTION);
					paint1Bridge.setStrokeCap(Paint.Cap.BUTT);
					this.layer.get(LayerIds.HIGHWAY$CONSTRUCTION).add(
							new PathContainer(this.path, paint1Bridge));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				} else {
					this.layer.get(LayerIds.HIGHWAY$CONSTRUCTION).add(
							new PathContainer(this.path, PAINT_HIGHWAY_CONSTRUCTION));
					if (wayName != null && this.currentTile.zoomLevel > 15) {
						renderHighwayName(wayName, wayNodes, wayNodesSequence);
					}
				}
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			}
		}

		/* building */
		if ((wayTagBitmap & BITMAP_BUILDING) != 0) {
			if (wayTagIds[TagIdsWays.BUILDING$ROOF]) {
				this.layer.get(LayerIds.BUILDING$ROOF).add(
						new PathContainer(this.path, PAINT_BUILDING_ROOF_OUTLINE));
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.BUILDING$APARTMENTS]
					|| wayTagIds[TagIdsWays.BUILDING$GOVERNMENT]
					|| wayTagIds[TagIdsWays.BUILDING$GYM]
					|| wayTagIds[TagIdsWays.BUILDING$SPORTS]
					|| wayTagIds[TagIdsWays.BUILDING$TRAIN_STATION]
					|| wayTagIds[TagIdsWays.BUILDING$UNIVERSITY]
					|| wayTagIds[TagIdsWays.BUILDING$YES]) {
				renderAreaName(wayName, wayNodes, wayNodesSequence, MODE_AREA_NAME_BLUE,
						(byte) 0);
				this.layer.get(LayerIds.BUILDING$YES).add(
						new PathContainer(this.path, PAINT_BUILDING_YES_OUTLINE));
				this.layer.get(LayerIds.BUILDING$YES).add(
						new PathContainer(this.path, PAINT_BUILDING_YES_FILL));
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			}
		}

		/* railway */
		if ((wayTagBitmap & BITMAP_RAILWAY) != 0) {
			if (wayTagIds[TagIdsWays.RAILWAY$RAIL]) {
				if (wayTagIds[TagIdsWays.TUNNEL$YES]) {
					this.layer.get(LayerIds.RAILWAY$RAIL_TUNNEL$YES).add(
							new PathContainer(this.path, PAINT_RAILWAY_RAIL_TUNNEL1));
					this.layer.get(LayerIds.RAILWAY$RAIL_TUNNEL$YES).add(
							new PathContainer(this.path, PAINT_RAILWAY_RAIL_TUNNEL2));
				} else {
					this.layer.get(LayerIds.RAILWAY$RAIL).add(
							new PathContainer(this.path, PAINT_RAILWAY_RAIL1));
					this.layer.get(LayerIds.RAILWAY$RAIL).add(
							new PathContainer(this.path, PAINT_RAILWAY_RAIL2));
				}
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.RAILWAY$TRAM]) {
				this.layer.get(LayerIds.RAILWAY$TRAM).add(
						new PathContainer(this.path, PAINT_RAILWAY_TRAM1));
				this.layer.get(LayerIds.RAILWAY$TRAM).add(
						new PathContainer(this.path, PAINT_RAILWAY_TRAM2));
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.RAILWAY$LIGHT_RAIL]) {
				this.layer.get(LayerIds.RAILWAY$LIGHT_RAIL).add(
						new PathContainer(this.path, PAINT_RAILWAY_LIGHT_RAIL1));
				this.layer.get(LayerIds.RAILWAY$LIGHT_RAIL).add(
						new PathContainer(this.path, PAINT_RAILWAY_LIGHT_RAIL2));
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.RAILWAY$SUBWAY]) {
				if (wayTagIds[TagIdsWays.TUNNEL$NO] || wayTagIds[TagIdsWays.BRIDGE$YES]) {
					this.layer.get(LayerIds.RAILWAY$SUBWAY).add(
							new PathContainer(this.path, PAINT_RAILWAY_SUBWAY1));
					this.layer.get(LayerIds.RAILWAY$SUBWAY).add(
							new PathContainer(this.path, PAINT_RAILWAY_SUBWAY2));
				} else {
					this.layer.get(LayerIds.RAILWAY$SUBWAY_TUNNEL).add(
							new PathContainer(this.path, PAINT_RAILWAY_SUBWAY_TUNNEL1));
					this.layer.get(LayerIds.RAILWAY$SUBWAY_TUNNEL).add(
							new PathContainer(this.path, PAINT_RAILWAY_SUBWAY_TUNNEL2));
				}
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.RAILWAY$STATION]) {
				this.layer.get(LayerIds.RAILWAY$STATION).add(
						new PathContainer(this.path, PAINT_RAILWAY_STATION_OUTLINE));
				this.layer.get(LayerIds.RAILWAY$STATION).add(
						new PathContainer(this.path, PAINT_RAILWAY_STATION_FILL));
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			}
		}

		/* landuse */
		if ((wayTagBitmap & BITMAP_LANDUSE) != 0) {
			if (wayTagIds[TagIdsWays.LANDUSE$ALLOTMENTS]
					|| wayTagIds[TagIdsWays.LANDUSE$CEMETERY]
					|| wayTagIds[TagIdsWays.LANDUSE$FARM]
					|| wayTagIds[TagIdsWays.LANDUSE$RECREATION_GROUND]) {
				renderAreaName(wayName, wayNodes, wayNodesSequence, MODE_AREA_NAME_BLUE,
						(byte) 0);
				this.layer.get(LayerIds.LANDUSE$CEMETERY).add(
						new PathContainer(this.path, PAINT_LANDUSE_CEMETERY_FILL));
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.LANDUSE$BASIN]
					|| wayTagIds[TagIdsWays.LANDUSE$RESERVOIR]) {
				this.layer.get(LayerIds.LANDUSE$BASIN).add(
						new PathContainer(this.path, PAINT_LANDUSE_BASIN_FILL));
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.LANDUSE$BROWNFIELD]
					|| wayTagIds[TagIdsWays.LANDUSE$INDUSTRIAL]) {
				renderAreaName(wayName, wayNodes, wayNodesSequence, MODE_AREA_NAME_BLUE,
						(byte) 0);
				this.layer.get(LayerIds.LANDUSE$INDUSTRIAL).add(
						new PathContainer(this.path, PAINT_LANDUSE_INDUSTRIAL_FILL));
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.LANDUSE$COMMERCIAL]) {
				renderAreaName(wayName, wayNodes, wayNodesSequence, MODE_AREA_NAME_BLUE,
						(byte) 0);
				this.layer.get(LayerIds.LANDUSE$COMMERCIAL).add(
						new PathContainer(this.path, PAINT_LANDUSE_COMMERCIAL_FILL));
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.LANDUSE$CONSTRUCTION]) {
				this.layer.get(LayerIds.LANDUSE$CONSTRUCTION).add(
						new PathContainer(this.path, PAINT_LANDUSE_CONSTRUCTION_FILL));
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.LANDUSE$FOREST]
					|| wayTagIds[TagIdsWays.LANDUSE$WOOD]) {
				this.layer.get(LayerIds.LANDUSE$FOREST).add(
						new PathContainer(this.path, PAINT_LANDUSE_FOREST_FILL));
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.LANDUSE$FARMLAND]
					|| wayTagIds[TagIdsWays.LANDUSE$GRASS]
					|| wayTagIds[TagIdsWays.LANDUSE$VILLAGE_GREEN]) {
				this.layer.get(LayerIds.LANDUSE$GRASS).add(
						new PathContainer(this.path, PAINT_LANDUSE_GRASS_FILL));
				this.layer.get(LayerIds.LANDUSE$GRASS).add(
						new PathContainer(this.path, PAINT_LANDUSE_GRASS_OUTLINE));
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.LANDUSE$MILITARY]) {
				this.layer.get(LayerIds.LANDUSE$MILITARY).add(
						new PathContainer(this.path, PAINT_LANDUSE_MILITARY_FILL));
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.LANDUSE$RESIDENTIAL]) {
				this.layer.get(LayerIds.LANDUSE$RESIDENTIAL).add(
						new PathContainer(this.path, PAINT_LANDUSE_RESIDENTIAL_FILL));
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.LANDUSE$RETAIL]) {
				this.layer.get(LayerIds.LANDUSE$RETAIL).add(
						new PathContainer(this.path, PAINT_LANDUSE_RETAIL_FILL));
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			}
		}

		/* leisure */
		if ((wayTagBitmap & BITMAP_LEISURE) != 0) {
			if (wayTagIds[TagIdsWays.LEISURE$COMMON] || wayTagIds[TagIdsWays.LEISURE$GARDEN]
					|| wayTagIds[TagIdsWays.LEISURE$GOLF_COURSE]
					|| wayTagIds[TagIdsWays.LEISURE$PARK]
					|| wayTagIds[TagIdsWays.LEISURE$PITCH]
					|| wayTagIds[TagIdsWays.LEISURE$PLAYGROUND]) {
				renderAreaName(wayName, wayNodes, wayNodesSequence, MODE_AREA_NAME_BLUE,
						(byte) 0);
				this.layer.get(LayerIds.LEISURE$COMMON).add(
						new PathContainer(this.path, PAINT_LEISURE_COMMON_FILL));
				this.layer.get(LayerIds.LEISURE$COMMON).add(
						new PathContainer(this.path, PAINT_LEISURE_COMMON_OUTLINE));
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.LEISURE$SPORTS_CENTRE]
					|| wayTagIds[TagIdsWays.LEISURE$STADIUM]
					|| wayTagIds[TagIdsWays.LEISURE$TRACK]
					|| wayTagIds[TagIdsWays.LEISURE$WATER_PARK]) {
				renderAreaName(wayName, wayNodes, wayNodesSequence, MODE_AREA_NAME_BLUE,
						(byte) 0);
				this.layer.get(LayerIds.LEISURE$STADIUM).add(
						new PathContainer(this.path, PAINT_LEISURE_STADIUM_FILL));
				this.layer.get(LayerIds.LEISURE$STADIUM).add(
						new PathContainer(this.path, PAINT_LEISURE_STADIUM_OUTLINE));
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			}
		}

		/* amenity */
		if ((wayTagBitmap & BITMAP_AMENITY) != 0) {
			if (wayTagIds[TagIdsWays.AMENITY$COLLEGE] || wayTagIds[TagIdsWays.AMENITY$SCHOOL]
					|| wayTagIds[TagIdsWays.AMENITY$UNIVERSITY]) {
				renderAreaName(wayName, wayNodes, wayNodesSequence, MODE_AREA_NAME_BLUE,
						(byte) 0);
				this.layer.get(LayerIds.AMENITY$SCHOOL).add(
						new PathContainer(this.path, PAINT_AMENITY_SCHOOL_FILL));
				this.layer.get(LayerIds.AMENITY$SCHOOL).add(
						new PathContainer(this.path, PAINT_AMENITY_SCHOOL_OUTLINE));
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.AMENITY$GRAVE_YARD]) {
				this.layer.get(LayerIds.AMENITY$GRAVE_YARD).add(
						new PathContainer(this.path, PAINT_AMENITY_GRAVE_YARD_FILL));
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.AMENITY$HOSPITAL]) {
				renderAreaName(wayName, wayNodes, wayNodesSequence, MODE_AREA_NAME_BLUE,
						(byte) 18);
				renderAreaSymbol(wayNodes, wayNodesSequence, this.mapSymbols.hospital);
				this.layer.get(LayerIds.AMENITY$HOSPITAL).add(
						new PathContainer(this.path, PAINT_AMENITY_HOSPITAL_FILL));
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.AMENITY$PARKING]) {
				renderAreaName(wayName, wayNodes, wayNodesSequence, MODE_AREA_NAME_BLUE,
						(byte) 18);
				renderAreaSymbol(wayNodes, wayNodesSequence, this.mapSymbols.parking);
				this.layer.get(LayerIds.AMENITY$PARKING).add(
						new PathContainer(this.path, PAINT_AMENITY_PARKING_FILL));
				this.layer.get(LayerIds.AMENITY$PARKING).add(
						new PathContainer(this.path, PAINT_AMENITY_PARKING_OUTLINE));
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.AMENITY$FOUNTAIN]) {
				renderAreaSymbol(wayNodes, wayNodesSequence, this.mapSymbols.fountain);
				--this.remainingTags;
			}
		}

		/* natural */
		if ((wayTagBitmap & BITMAP_NATURAL) != 0) {
			if (wayTagIds[TagIdsWays.NATURAL$BEACH]) {
				renderAreaName(wayName, wayNodes, wayNodesSequence, MODE_AREA_NAME_BLUE,
						(byte) 0);
				this.layer.get(LayerIds.NATURAL$BEACH).add(
						new PathContainer(this.path, PAINT_NATURAL_BEACH_FILL));
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.NATURAL$HEATH]) {
				this.layer.get(LayerIds.NATURAL$HEATH).add(
						new PathContainer(this.path, PAINT_NATURAL_HEATH_FILL));
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.NATURAL$LAND]) {
				this.layer.get(LayerIds.NATURAL$LAND).add(
						new PathContainer(this.path, PAINT_NATURAL_LAND_FILL));
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.NATURAL$SCRUB]
					|| wayTagIds[TagIdsWays.NATURAL$WOOD]) {
				this.layer.get(LayerIds.NATURAL$WOOD).add(
						new PathContainer(this.path, PAINT_NATURAL_WOOD_FILL));
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.NATURAL$WATER]) {
				renderAreaName(wayName, wayNodes, wayNodesSequence, MODE_AREA_NAME_BLUE,
						(byte) 0);
				this.layer.get(LayerIds.NATURAL$WATER).add(
						new PathContainer(this.path, PAINT_NATURAL_WATER_FILL));
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.NATURAL$COASTLINE]) {
				int[] nodesSequence = wayNodesSequence;
				GeoPoint startPoint = new GeoPoint(nodesSequence[0], nodesSequence[1]);
				GeoPoint endPoint = new GeoPoint(nodesSequence[nodesSequence.length - 2],
						nodesSequence[nodesSequence.length - 1]);
				int[] matchCoastlinePath;
				int[] matchPath;
				int[] newPath;

				if (this.coastlineStarts.containsKey(endPoint)) {
					matchCoastlinePath = this.coastlineStarts.remove(endPoint);
					matchPath = matchCoastlinePath;
					newPath = new int[nodesSequence.length + matchPath.length - 2];
					System.arraycopy(nodesSequence, 0, newPath, 0, nodesSequence.length - 2);
					System.arraycopy(matchPath, 0, newPath, nodesSequence.length - 2,
							matchPath.length);
					nodesSequence = newPath;
					endPoint = new GeoPoint(newPath[nodesSequence.length - 2],
							newPath[nodesSequence.length - 1]);
				}

				if (this.coastlineEnds.containsKey(startPoint)) {
					matchCoastlinePath = this.coastlineEnds.remove(startPoint);
					matchPath = matchCoastlinePath;
					newPath = new int[nodesSequence.length + matchPath.length - 2];
					System.arraycopy(matchPath, 0, newPath, 0, matchPath.length);
					System.arraycopy(nodesSequence, 2, newPath, matchPath.length,
							nodesSequence.length - 2);
					nodesSequence = newPath;
					startPoint = new GeoPoint(newPath[0], newPath[1]);
				}

				int[] coastlinePath = nodesSequence;
				this.coastlineStarts.put(startPoint, coastlinePath);
				this.coastlineEnds.put(endPoint, coastlinePath);
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			}
		}

		/* man_made */
		if (wayTagIds[TagIdsWays.MAN_MADE$PIER]) {
			this.layer.get(LayerIds.MAN_MADE$PIER).add(
					new PathContainer(this.path, PAINT_MAN_MADE_PIER));
			if (this.remainingTags == 1) {
				return;
			}
			--this.remainingTags;
		}

		/* waterway */
		if ((wayTagBitmap & BITMAP_WATERWAY) != 0) {
			if (wayTagIds[TagIdsWays.WATERWAY$CANAL] || wayTagIds[TagIdsWays.WATERWAY$DRAIN]) {
				this.layer.get(LayerIds.WATERWAY$CANAL).add(
						new PathContainer(this.path, PAINT_WATERWAY_CANAL));
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.WATERWAY$RIVER]) {
				this.layer.get(LayerIds.WATERWAY$RIVER).add(
						new PathContainer(this.path, PAINT_WATERWAY_RIVER));
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.WATERWAY$RIVERBANK]) {
				this.layer.get(LayerIds.WATERWAY$RIVERBANK).add(
						new PathContainer(this.path, PAINT_WATERWAY_RIVERBANK_FILL));
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.WATERWAY$STREAM]) {
				this.layer.get(LayerIds.WATERWAY$STREAM).add(
						new PathContainer(this.path, PAINT_WATERWAY_STREAM));
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			}
		}

		/* barrier */
		if (wayTagIds[TagIdsWays.BARRIER$FENCE] || wayTagIds[TagIdsWays.BARRIER$WALL]) {
			if (this.currentTile.zoomLevel > 15) {
				this.layer.get(LayerIds.BARRIER$WALL).add(
						new PathContainer(this.path, PAINT_BARRIER_WALL));
				if (this.remainingTags == 1) {
					return;
				}
			}
			--this.remainingTags;
		}

		/* boundary */
		if (wayTagIds[TagIdsWays.BOUNDARY$ADMINISTRATIVE]) {
			if (wayTagIds[TagIdsWays.ADMIN_LEVEL$2]) {
				this.layer.get(LayerIds.ADMIN_LEVEL$2).add(
						new PathContainer(this.path,
								PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_2));
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.ADMIN_LEVEL$4]) {
				this.layer.get(LayerIds.ADMIN_LEVEL$4).add(
						new PathContainer(this.path,
								PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_4));
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.ADMIN_LEVEL$6]) {
				this.layer.get(LayerIds.ADMIN_LEVEL$6).add(
						new PathContainer(this.path,
								PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_6));
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.ADMIN_LEVEL$8]) {
				this.layer.get(LayerIds.ADMIN_LEVEL$8).add(
						new PathContainer(this.path,
								PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_8));
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.ADMIN_LEVEL$9]) {
				this.layer.get(LayerIds.ADMIN_LEVEL$9).add(
						new PathContainer(this.path,
								PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_9));
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			} else if (wayTagIds[TagIdsWays.ADMIN_LEVEL$10]) {
				this.layer.get(LayerIds.ADMIN_LEVEL$10).add(
						new PathContainer(this.path,
								PAINT_BOUNDARY_ADMINISTRATIVE_ADMIN_LEVEL_10));
				if (this.remainingTags == 1) {
					return;
				}
				--this.remainingTags;
			}
		}

		/* sport */
		if (wayTagIds[TagIdsWays.SPORT$SHOOTING]) {
			this.layer.get(LayerIds.SPORT$SHOOTING).add(
					new PathContainer(this.path, PAINT_SPORT_SHOOTING_FILL));
			this.layer.get(LayerIds.SPORT$SHOOTING).add(
					new PathContainer(this.path, PAINT_SPORT_SHOOTING_OUTLINE));
			if (this.remainingTags == 1) {
				return;
			}
			--this.remainingTags;
		} else if (wayTagIds[TagIdsWays.SPORT$TENNIS]) {
			this.layer.get(LayerIds.SPORT$TENNIS).add(
					new PathContainer(this.path, PAINT_SPORT_TENNIS_FILL));
			this.layer.get(LayerIds.SPORT$TENNIS).add(
					new PathContainer(this.path, PAINT_SPORT_TENNIS_OUTLINE));
			if (this.remainingTags == 1) {
				return;
			}
			--this.remainingTags;
		}

		/* aeroway */
		if (wayTagIds[TagIdsWays.AEROWAY$AERODROME]) {
			this.layer.get(LayerIds.AEROWAY$AERODROME).add(
					new PathContainer(this.path, PAINT_AEROWAY_AERODROME_OUTLINE));
			this.layer.get(LayerIds.AEROWAY$AERODROME).add(
					new PathContainer(this.path, PAINT_AEROWAY_AERODROME_FILL));
			if (this.remainingTags == 1) {
				return;
			}
			--this.remainingTags;
		} else if (wayTagIds[TagIdsWays.AEROWAY$APRON]) {
			this.layer.get(LayerIds.AEROWAY$APRON).add(
					new PathContainer(this.path, PAINT_AEROWAY_APRON_FILL));
			if (this.remainingTags == 1) {
				return;
			}
			--this.remainingTags;
		} else if (wayTagIds[TagIdsWays.AEROWAY$RUNWAY]) {
			this.layer.get(LayerIds.AEROWAY$RUNWAY1).add(
					new PathContainer(this.path, PAINT_AEROWAY_RUNWAY1));
			this.layer.get(LayerIds.AEROWAY$RUNWAY2).add(
					new PathContainer(this.path, PAINT_AEROWAY_RUNWAY2));
			if (this.remainingTags == 1) {
				return;
			}
			--this.remainingTags;
		} else if (wayTagIds[TagIdsWays.AEROWAY$TAXIWAY]) {
			this.layer.get(LayerIds.AEROWAY$TAXIWAY1).add(
					new PathContainer(this.path, PAINT_AEROWAY_TAXIWAY1));
			this.layer.get(LayerIds.AEROWAY$TAXIWAY2).add(
					new PathContainer(this.path, PAINT_AEROWAY_TAXIWAY2));
			if (this.remainingTags == 1) {
				return;
			}
			--this.remainingTags;
		} else if (wayTagIds[TagIdsWays.AEROWAY$TERMINAL]) {
			this.layer.get(LayerIds.AEROWAY$TERMINAL).add(
					new PathContainer(this.path, PAINT_AEROWAY_TERMINAL_OUTLINE));
			this.layer.get(LayerIds.AEROWAY$TERMINAL).add(
					new PathContainer(this.path, PAINT_AEROWAY_TERMINAL_FILL));
			if (this.remainingTags == 1) {
				return;
			}
			--this.remainingTags;
		}

		/* tourism */
		if (wayTagIds[TagIdsWays.TOURISM$ATTRACTION]) {
			renderAreaName(wayName, wayNodes, wayNodesSequence, MODE_AREA_NAME_RED, (byte) 0);
			this.layer.get(LayerIds.TOURISM$ATTRACTION).add(
					new PathContainer(this.path, PAINT_TOURISM_ATTRACTION_FILL));
			if (this.remainingTags == 1) {
				return;
			}
			--this.remainingTags;
		} else if (wayTagIds[TagIdsWays.TOURISM$ZOO]) {
			this.layer.get(LayerIds.TOURISM$ZOO).add(
					new PathContainer(this.path, PAINT_TOURISM_ZOO_FILL));
			this.layer.get(LayerIds.TOURISM$ZOO).add(
					new PathContainer(this.path, PAINT_TOURISM_ZOO_OUTLINE));
			if (this.remainingTags == 1) {
				return;
			}
			--this.remainingTags;
		}

		/* route */
		if (wayTagIds[TagIdsWays.ROUTE$FERRY]) {
			this.layer.get(LayerIds.ROUTE$FERRY).add(
					new PathContainer(this.path, PAINT_ROUTE_FERRY));
			if (this.remainingTags == 1) {
				return;
			}
			--this.remainingTags;
		}

		/* military */
		if (wayTagIds[TagIdsWays.MILITARY$AIRFIELD] || wayTagIds[TagIdsWays.MILITARY$BARRACKS]) {
			this.layer.get(LayerIds.MILITARY$BARRACKS).add(
					new PathContainer(this.path, PAINT_MILITARY_BARRACKS_FILL));
			if (this.remainingTags == 1) {
				return;
			}
			--this.remainingTags;
		} else if (wayTagIds[TagIdsWays.MILITARY$NAVAL_BASE]) {
			this.layer.get(LayerIds.MILITARY$NAVAL_BASE).add(
					new PathContainer(this.path, PAINT_MILITARY_NAVAL_BASE_FILL));
			if (this.remainingTags == 1) {
				return;
			}
			--this.remainingTags;
		}

		/* historic */
		if (wayTagIds[TagIdsWays.HISTORIC$RUINS]) {
			renderAreaName(wayName, wayNodes, wayNodesSequence, MODE_AREA_NAME_BLUE, (byte) 0);
			--this.remainingTags;
		}

		/* place */
		if (wayTagIds[TagIdsWays.PLACE$LOCALITY]) {
			renderAreaName(wayName, wayNodes, wayNodesSequence, MODE_AREA_NAME_BLACK, (byte) 0);
			--this.remainingTags;
		}

	}

	/**
	 * Request a scheduling of all tiles that are currently in the job queue.
	 */
	synchronized void requestSchedule() {
		this.scheduleNeeded = true;
		if (!this.jobQueue1.isEmpty()) {
			this.notify();
		}
	}

	void setDatabase(Database database) {
		this.database = database;
	}

	void setImageCaches(ImageBitmapCache imageBitmapCache, ImageFileCache imageFileCache) {
		this.imageBitmapCache = imageBitmapCache;
		this.imageFileCache = imageFileCache;
	}

	void setMapView(MapView mapView) {
		this.mapView = mapView;
	}

	/**
	 * Request that the MapGenerator should continue working.
	 */
	synchronized void unpause() {
		this.pause = false;
		if (!this.jobQueue1.isEmpty()) {
			this.notify();
		}
	}
}