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
package org.mapsforge.server.geoCoding;

import java.util.List;

/**
 * Interface for the geocoding service.
 * 
 * @author nemo & bogumil
 * 
 * @see GeoCoderGoogle
 * @see GeoCoderGeoNames
 */
public interface IGeoCoder {

	/**
	 * Returns a list of possible locations, witch match the request String.
	 * 
	 * @param request
	 *            some map informations.
	 * @param max
	 *            maximal number of results
	 * @return List of Points, with 0 <= length(List) <= max.
	 */
	public List<Node> search(String request, int max);
}
