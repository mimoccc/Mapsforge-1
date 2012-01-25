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

import org.mapsforge.map.writer.util.Constants;
import org.openstreetmap.osmosis.core.pipeline.common.TaskConfiguration;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManager;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.pipeline.v0_6.SinkManager;

/**
 * Factory for the mapfile writer osmosis plugin.
 * 
 * @author bross
 */
class MapFileWriterFactory extends TaskManagerFactory {

	private static final String PARAM_OUTFILE = "file";
	private static final String PARAM_BBOX = "bbox";
	private static final String PARAM_ZOOMINTERVAL_CONFIG = "zoom-interval-conf";
	private static final String PARAM_COMMENT = "comment";
	private static final String PARAM_MAP_START_POSITION = "map-start-position";
	private static final String PARAM_MAP_START_ZOOM = "map-start-zoom";
	private static final String PARAM_DEBUG_INFO = "debug-file";
	// private static final String PARAM_WAYNODE_COMPRESSION = "waynode-compression";
	private static final String PARAM_SIMPLIFICATION_FACTOR = "simplification-factor";
	private static final String PARAM_POLYGON_CLIPPING = "polygon-clipping";
	private static final String PARAM_WAY_CLIPPING = "way-clipping";
	private static final String PARAM_TYPE = "type";
	private static final String PARAM_BBOX_ENLARGEMENT = "bbox-enlargement";
	private static final String PARAM_TAG_MAPPING_FILE = "tag-conf-file";
	private static final String PARAM_PREFERRED_LANGUAGE = "language-preference";
	private static final String PARAM_ENCODING = "encoding";

	@Override
	protected TaskManager createTaskManagerImpl(TaskConfiguration taskConfig) {

		String outfile = getStringArgument(taskConfig, PARAM_OUTFILE, Constants.DEFAULT_PARAM_OUTFILE);
		String mapStartPosition = getStringArgument(taskConfig, PARAM_MAP_START_POSITION, null);
		String mapStartZoom = getStringArgument(taskConfig, PARAM_MAP_START_ZOOM, null);
		String bbox = getStringArgument(taskConfig, PARAM_BBOX, null);
		String zoomConf = getStringArgument(taskConfig, PARAM_ZOOMINTERVAL_CONFIG, null);
		String comment = getStringArgument(taskConfig, PARAM_COMMENT, null);
		boolean debug = getBooleanArgument(taskConfig, PARAM_DEBUG_INFO, false);
		// boolean waynodeCompression = getBooleanArgument(taskConfig, PARAM_WAYNODE_COMPRESSION,
		// true);
		double simplificationFactor = getDoubleArgument(taskConfig, PARAM_SIMPLIFICATION_FACTOR,
				Constants.DEFAULT_SIMPLIFICATION_FACTOR);
		boolean polygonClipping = getBooleanArgument(taskConfig, PARAM_POLYGON_CLIPPING, true);
		boolean wayClipping = getBooleanArgument(taskConfig, PARAM_WAY_CLIPPING, true);
		String type = getStringArgument(taskConfig, PARAM_TYPE, Constants.DEFAULT_PARAM_TYPE);
		int bboxEnlargement = getIntegerArgument(taskConfig, PARAM_BBOX_ENLARGEMENT,
				Constants.DEFAULT_PARAM_BBOX_ENLARGEMENT);
		String tagConfFile = getStringArgument(taskConfig, PARAM_TAG_MAPPING_FILE, null);
		String preferredLanguage = getStringArgument(taskConfig, PARAM_PREFERRED_LANGUAGE, null);
		String encoding = getStringArgument(taskConfig, PARAM_ENCODING, Constants.DEFAULT_PARAM_ENCODING);
		MapFileWriterTask task = new MapFileWriterTask(outfile, bbox, mapStartPosition, mapStartZoom, comment,
				zoomConf, debug, simplificationFactor, polygonClipping, wayClipping, type, bboxEnlargement,
				tagConfFile, preferredLanguage, encoding);
		return new SinkManager(taskConfig.getId(), task, taskConfig.getPipeArgs());
	}

}
