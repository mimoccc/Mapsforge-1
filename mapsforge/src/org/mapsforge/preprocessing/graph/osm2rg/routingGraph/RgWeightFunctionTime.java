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

import org.mapsforge.preprocessing.graph.routingGraphInterface.IRgWeightFunction;
import org.mapsforge.preprocessing.model.IHighwayLevel2Speed;

/**
 * No good heuristcs until now, should be extended along with edge data.
 * 
 * @author Frank Viernau
 */
public class RgWeightFunctionTime implements IRgWeightFunction<RgEdge> {

	private final IHighwayLevel2Speed hl2s;

	public RgWeightFunctionTime(IHighwayLevel2Speed hl2s) {
		this.hl2s = hl2s;
	}

	@Override
	public double getWeightDouble(RgEdge edge) {
		double m_per_s = hl2s.speed(edge.getHighwayLevel()) / 3.6d;
		return (edge.getLengthMeters() / m_per_s) * 10;
	}

	@Override
	public int getWeightInt(RgEdge edge) {
		return (int) Math.rint(getWeightDouble(edge));
	}

}
