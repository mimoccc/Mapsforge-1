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
package org.mapsforge.navigation;

// TODO Create map-matching specific update class.

/**
 * This interface defines a listener for the map-matching service provided through
 * {@link MapMatchingService} . The {@link MapMatchingListener} would typically be implemented by an
 * application that seeks to provide map-matching functionality to the user.
 * 
 * @author thilo ratnaweera
 */
public interface MapMatchingListener {

	/**
	 * Called by a {@link MapMatchingService} to send map-matching updates to this listener.
	 * 
	 * @param mapMatchingUpdate
	 *            Object containing map-matching update information.
	 */
	public void onMapMatchingUpdate(Object mapMatchingUpdate);

}