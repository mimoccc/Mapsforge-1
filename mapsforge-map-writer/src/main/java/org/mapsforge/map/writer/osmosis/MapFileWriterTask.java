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
package org.mapsforge.map.writer.osmosis;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.text.NumberFormat;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mapsforge.map.writer.HDTileBasedDataProcessor;
import org.mapsforge.map.writer.MapFileWriter;
import org.mapsforge.map.writer.OSMTagMapping;
import org.mapsforge.map.writer.RAMTileBasedDataProcessor;
import org.mapsforge.map.writer.model.EncodingChoice;
import org.mapsforge.map.writer.model.GeoCoordinate;
import org.mapsforge.map.writer.model.Rect;
import org.mapsforge.map.writer.model.TileBasedDataProcessor;
import org.mapsforge.map.writer.model.ZoomIntervalConfiguration;
import org.mapsforge.map.writer.util.Constants;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Bound;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

/**
 * An Osmosis plugin that reads OpenStreetMap data and converts it to a mapsforge binary file.
 * 
 * @author bross
 */
public class MapFileWriterTask implements Sink {
	private static final Logger LOGGER = Logger.getLogger(MapFileWriterTask.class.getName());

	private TileBasedDataProcessor tileBasedGeoObjectStore;

	// Accounting
	private int amountOfNodesProcessed = 0;
	private int amountOfWaysProcessed = 0;
	private int amountOfRelationsProcessed = 0;

	// configuration parameters
	private final File outFile;
	private final GeoCoordinate mapStartPosition;
	private final byte mapStartZoom;
	private final boolean debugInfo;
	// private boolean waynodeCompression;
	private final double simplificationFactor;
	private final boolean polygonClipping;
	private final boolean wayClipping;
	private final String comment;
	private final ZoomIntervalConfiguration zoomIntervalConfiguration;
	private final String type;
	private final int bboxEnlargement;
	private final String preferredLanguage;
	private final EncodingChoice encoding;

	private final int vSpecification;

	MapFileWriterTask(String outFile, String bboxString, String mapStartPosition, String mapStartZoom, String comment,
			String zoomIntervalConfigurationString, boolean debugInfo, double simplificationFactor,
			boolean polygonClipping, boolean wayClipping, String type, int bboxEnlargement, String tagConfFile,
			String preferredLanguage, String encoding) {

		Properties properties = new Properties();
		try {
			properties.load(MapFileWriterTask.class.getClassLoader().getResourceAsStream("default.properties"));
		} catch (IOException e) {
			throw new RuntimeException("could not find default properties", e); // NOPMD by bross on 25.12.11
																				// 13:36
		}
		String vWriter = properties.getProperty(Constants.PROPERTY_NAME_WRITER_VERSION);
		try {
			this.vSpecification = Integer.parseInt(properties
					.getProperty(Constants.PROPERTY_NAME_FILE_SPECIFICATION_VERSION));
		} catch (NumberFormatException e) {
			throw new RuntimeException("map file specification version is not an integer", e); // NOPMD by bross
																								// on 25.12.11
																								// 13:36
		}
		LOGGER.info("mapfile-writer version " + vWriter);
		LOGGER.info("mapfile format specification version " + this.vSpecification);

		this.outFile = new File(outFile);
		if (this.outFile.isDirectory()) {
			throw new IllegalArgumentException("file parameter points to a directory, must be a file");
		}

		this.debugInfo = debugInfo;
		// this.waynodeCompression = waynodeCompression;
		if (simplificationFactor < 0) {
			throw new RuntimeException("simplification factor must be >= 0");
		}
		this.simplificationFactor = simplificationFactor;
		this.polygonClipping = polygonClipping;
		this.wayClipping = wayClipping;
		this.comment = comment;
		this.bboxEnlargement = bboxEnlargement;
		this.preferredLanguage = preferredLanguage;
		this.encoding = EncodingChoice.fromString(encoding);

		// BOUNDING BOX CONFUGURATION
		Rect bbox = bboxString == null ? null : Rect.fromString(bboxString);

		// START POSITION AND ZOOM
		this.mapStartPosition = mapStartPosition == null ? null : GeoCoordinate.fromString(mapStartPosition);
		if (this.mapStartPosition != null && bbox != null && !validMapStartPosition(bbox, this.mapStartPosition)) {
			throw new RuntimeException(
					"map start position is not valid, must be included in bounding box of the map, bbox: "
							+ bbox.toString() + " - map start position: " + this.mapStartPosition.toString());
		}

		// init with negative value
		byte startZoom = -1;
		if (mapStartZoom != null && !mapStartZoom.isEmpty()) {
			try {
				startZoom = Byte.parseByte(mapStartZoom);
				if (startZoom < 0 || startZoom > 22) {
					LOGGER.warning("map start zoom level is invalid, must be in range [0 - 22], using default of renderer");
					startZoom = -1;
				}
			} catch (NumberFormatException e) {
				LOGGER.warning("map start zoom level is invalid, must be in range [0 - 22], using default of renderer");
			}
		}
		this.mapStartZoom = startZoom;

		// TAG CONFIGURATION
		if (tagConfFile == null) {
			OSMTagMapping.getInstance();
		} else {
			File tagConf = new File(tagConfFile);
			if (tagConf.isDirectory()) {
				throw new IllegalArgumentException("tag-conf-file points to a directory, must be a file");
			}
			try {
				OSMTagMapping.getInstance(tagConf.toURI().toURL());
			} catch (MalformedURLException e) {
				throw new IllegalArgumentException(e);
			}
		}

		this.zoomIntervalConfiguration = zoomIntervalConfigurationString == null ? ZoomIntervalConfiguration
				.getStandardConfiguration() : ZoomIntervalConfiguration.fromString(zoomIntervalConfigurationString);

		this.type = type;
		if (!type.equalsIgnoreCase("ram") && !type.equalsIgnoreCase("hd")) {
			throw new IllegalArgumentException("type argument must equal ram or hd, found: " + type);
		}
		// CREATE DATASTORE IF BBOX IS DEFINED
		if (bbox != null) {
			if (type.equalsIgnoreCase("ram")) {
				this.tileBasedGeoObjectStore = RAMTileBasedDataProcessor.newInstance(bbox,
						this.zoomIntervalConfiguration, bboxEnlargement, preferredLanguage);
			} else {
				this.tileBasedGeoObjectStore = HDTileBasedDataProcessor.newInstance(bbox,
						this.zoomIntervalConfiguration, bboxEnlargement, preferredLanguage);
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * @see org.openstreetmap.osmosis.core.lifecycle.Completable#complete()
	 */
	@Override
	public final void complete() {
		NumberFormat nfMegabyte = NumberFormat.getInstance();
		NumberFormat nfCounts = NumberFormat.getInstance();
		nfCounts.setGroupingUsed(true);
		nfMegabyte.setMaximumFractionDigits(2);

		LOGGER.info("completing read...");
		this.tileBasedGeoObjectStore.complete();

		LOGGER.info("start writing file...");

		try {
			if (this.outFile.exists() && !this.outFile.isDirectory()) {
				LOGGER.info("overwriting file " + this.outFile.getAbsolutePath());
				this.outFile.delete();
			}
			RandomAccessFile file = new RandomAccessFile(this.outFile, "rw");
			MapFileWriter mfw = new MapFileWriter(this.tileBasedGeoObjectStore, file, this.bboxEnlargement);
			// mfw.writeFileWithDebugInfos(System.currentTimeMillis(), 1, (short) 256);
			mfw.writeFile(System.currentTimeMillis(), this.vSpecification,
					(short) 256, // NOPMD by bross on 25.12.11 13:38
					this.comment, // NOPMD by bross on 25.12.11 13:36
					this.debugInfo, this.polygonClipping, this.wayClipping, this.simplificationFactor,
					this.mapStartPosition, this.mapStartZoom, this.preferredLanguage, this.encoding);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "error while writing file", e);
		}

		LOGGER.info("finished...");
		LOGGER.fine("total processed nodes: " + nfCounts.format(this.amountOfNodesProcessed));
		LOGGER.fine("total processed ways: " + nfCounts.format(this.amountOfWaysProcessed));
		LOGGER.fine("total processed relations: " + nfCounts.format(this.amountOfRelationsProcessed));

		System.gc(); // NOPMD by bross on 25.12.11 13:37
		LOGGER.info("estimated memory consumption: "
				+ nfMegabyte.format(+((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / Math
						.pow(1024, 2))) + "MB");
	}

	@Override
	public final void release() {
		this.tileBasedGeoObjectStore.release();
	}

	@Override
	public final void process(EntityContainer entityContainer) {

		Entity entity = entityContainer.getEntity();

		switch (entity.getType()) {

			case Bound:
				Bound bound = (Bound) entity;
				if (this.tileBasedGeoObjectStore == null) {
					Rect bbox = new Rect(bound.getLeft(), bound.getRight(), bound.getBottom(), bound.getTop());
					if (this.mapStartPosition != null && !validMapStartPosition(bbox, this.mapStartPosition)) {
						throw new RuntimeException(
								"map start position is not valid, must be included in bounding box of the map, bbox: "
										+ bbox.toString() + " - map start position: "
										+ this.mapStartPosition.toString());
					}

					if (this.type.equalsIgnoreCase("ram")) {
						this.tileBasedGeoObjectStore = RAMTileBasedDataProcessor.newInstance(bbox,
								this.zoomIntervalConfiguration, this.bboxEnlargement, this.preferredLanguage);
					} else {
						this.tileBasedGeoObjectStore = HDTileBasedDataProcessor.newInstance(bbox,
								this.zoomIntervalConfiguration, this.bboxEnlargement, this.preferredLanguage);
					}
				}
				LOGGER.info("start reading data...");
				break;

			// *******************************************************
			// ****************** NODE PROCESSING*********************
			// *******************************************************
			case Node:

				if (this.tileBasedGeoObjectStore == null) {
					LOGGER.severe("No valid bounding box found in input data.\n"
							+ "Please provide valid bounding box via command "
							+ "line parameter 'bbox=minLat,minLon,maxLat,maxLon'.\n"
							+ "Tile based data store not initialized. Aborting...");
					throw new IllegalStateException("tile based data store not initialized, missing bounding "
							+ "box information in input data");
				}
				this.tileBasedGeoObjectStore.addNode((Node) entity);
				// hint to GC
				entity = null;
				this.amountOfNodesProcessed++;
				break;

			// *******************************************************
			// ******************* WAY PROCESSING*********************
			// *******************************************************
			case Way:
				this.tileBasedGeoObjectStore.addWay((Way) entity);
				entity = null;
				this.amountOfWaysProcessed++;
				break;

			// *******************************************************
			// ****************** RELATION PROCESSING*********************
			// *******************************************************
			case Relation:
				Relation currentRelation = (Relation) entity;
				this.tileBasedGeoObjectStore.addRelation(currentRelation);
				this.amountOfRelationsProcessed++;
				entity = null;
				break;
		}

	}

	private static boolean validMapStartPosition(Rect bbox, GeoCoordinate startPos) {
		return bbox.includes(startPos);
	}
}
