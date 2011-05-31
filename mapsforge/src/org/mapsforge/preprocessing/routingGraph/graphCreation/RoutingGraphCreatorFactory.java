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
package org.mapsforge.preprocessing.routingGraph.graphCreation;

import org.openstreetmap.osmosis.core.pipeline.common.TaskConfiguration;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManager;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.pipeline.v0_6.SinkManager;

class RoutingGraphCreatorFactory extends TaskManagerFactory {

	private final static String PARAM_XML_CONFIG = "xml-config";

	@Override
	protected TaskManager createTaskManagerImpl(TaskConfiguration taskConfig) {
		String xmlConfigPath = getStringArgument(taskConfig, PARAM_XML_CONFIG);
		return new SinkManager(
					taskConfig.getId(),
					new RoutingGraphCreatorTask(xmlConfigPath),
					taskConfig.getPipeArgs());
	}
}
