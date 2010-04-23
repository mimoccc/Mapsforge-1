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
package org.mapsforge.preprocessing.model;

import org.mapsforge.core.conf.IVehicle;

public interface ITransportConfigurationPreprocessing extends IVehicle {

	/**
	 * Calculates the weight of a given edge. This typically includes the use of a heuristic
	 * which estimates the traveltime on the edge considering informations such as number of
	 * traffic signals, crossings or max speed restrictions, etc.
	 * 
	 * @param edge
	 *            the edge for which the weight is to be calculated
	 * @return the estimated weight for this edge or Double.POSITIVE_INFINITY if this Transport
	 *         cannot use this edge.
	 */
	public float getWeight(Edge edge);
}
