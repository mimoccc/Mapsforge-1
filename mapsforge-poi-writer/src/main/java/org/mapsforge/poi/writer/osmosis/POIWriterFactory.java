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
package org.mapsforge.poi.writer.osmosis;

import org.mapsforge.mapmaker.logging.DummyProgressManager;
import org.mapsforge.mapmaker.logging.ProgressManager;
import org.openstreetmap.osmosis.core.pipeline.common.TaskConfiguration;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManager;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.pipeline.v0_6.SinkManager;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

/**
 * This class is used for creating {@link POIWriterTask}s.
 * 
 * @author Karsten Groll
 */
public class POIWriterFactory extends TaskManagerFactory {

	/**
	 * Default constructor.
	 */
	public POIWriterFactory() {
	}

	@Override
	protected TaskManager createTaskManagerImpl(TaskConfiguration taskConfig) {
		// Output file
		String outputFilePath = getDefaultStringArgument(taskConfig, System.getProperty("user.home") + "/map.pbf");

		// XML-file containing a POI category configuration
		String categoryConfigFilePath = getStringArgument(taskConfig, "categoryConfigPath", "POICategoriesOsmosis.xml");

		// If set to true, progress messages will forwarded to a GUI message handler
		boolean guiMode = getBooleanArgument(taskConfig, "gui-mode", false);

		ProgressManager progressManager = new DummyProgressManager();

		// The creation task
		Sink task = new POIWriterTask(outputFilePath, categoryConfigFilePath, progressManager);

		return new SinkManager(taskConfig.getId(), task, taskConfig.getPipeArgs());
	}

}
