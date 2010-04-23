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
package org.mapsforge.preprocessing.graph.osm2rg.routingGraph;

import org.mapsforge.preprocessing.graph.routingGraphInterface.IRgEdge;
import org.mapsforge.preprocessing.model.EHighwayLevel;

/**
 * @author Frank Viernau
 * 
 *         Should be extended to contain all relevant edge osm data.
 */
public class RgEdge implements IRgEdge {

	private final int id, sourceId, targetId;
	private final double[] longitudes, latitudes;
	private final double lengthMeters;
	private final boolean isUndirected, isUrban;
	private final long osmWayId;
	private final String name;
	private final EHighwayLevel hwyLevel;

	public RgEdge(int id, int sourceId, int targetId, double[] longitudes, double[] latitudes,
			boolean isUndirected, boolean isUrban, long osmWayId, String name,
			double lengthMeters, EHighwayLevel hwyLevel) {
		this.id = id;
		this.sourceId = sourceId;
		this.targetId = targetId;
		this.longitudes = longitudes;
		this.latitudes = latitudes;
		this.isUndirected = isUndirected;
		this.isUrban = isUrban;
		this.osmWayId = osmWayId;
		this.name = name;
		this.lengthMeters = lengthMeters;
		this.hwyLevel = hwyLevel;
	}

	public int getId() {
		return id;
	}

	@Override
	public double[] getLatitudes() {
		return latitudes;
	}

	@Override
	public double[] getLongitudes() {
		return longitudes;
	}

	@Override
	public int getSourceId() {
		return sourceId;
	}

	@Override
	public int getTargetId() {
		return targetId;
	}

	@Override
	public boolean isUndirected() {
		return isUndirected;
	}

	public boolean isUrban() {
		return isUrban;
	}

	public long getOsmWayId() {
		return osmWayId;
	}

	public String getName() {
		return name;
	}

	public double getLengthMeters() {
		return lengthMeters;
	}

	public EHighwayLevel getHighwayLevel() {
		return hwyLevel;
	}

	@Override
	public String toString() {
		return "e: " + sourceId + " -> " + targetId + ", length = " + lengthMeters;
	}
}
