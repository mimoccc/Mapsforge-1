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
/**
 * 
 */
package org.mapsforge.navigation;

import org.mapsforge.core.GeoCoordinate;

/**
 * This interface describes a personal navigation assistant. It is meant to be a high level API for
 * applications that want to use the guidance functionality of the mapsforge library. An application can
 * register itself to an instance of an implementing class and receives updates regarding current
 * location, distance, turn-by-turn directions, etc.
 * 
 * @author thilo ratnaweera
 * 
 */
public interface GuidanceService {
	/**
	 * Registers a {@link GuidanceListener}.
	 * 
	 * @param guidanceListener
	 *            The listener that should be added.
	 */
	public void addGuidanceListener(GuidanceListener guidanceListener);

	/**
	 * Removes a {@link GuidanceListener}.
	 * 
	 * @param guidanceListener
	 *            The listener that should be removed.
	 */
	public void removeGuidanceListener(GuidanceListener guidanceListener);

	/**
	 * Sets the destination represented as a {@link GeoCoordinate} where the user wants to be guided to.
	 * 
	 * @param destination
	 *            The destination.
	 */
	public void setDestination(GeoCoordinate destination);

	/**
	 * Sets the destination represented as a {@link PostalAddress} where the user wants to be guided to.
	 * 
	 * @param destination
	 *            The destination.
	 */
	public void setDestination(PostalAddress destination);

	/**
	 * Starts the guidance system. After invocation the system sends guidance updates to the registered
	 * {@link GuidanceListener}s.
	 */
	public void start();

	/**
	 * Pauses the guidance system. As long it is not resumed or restarted the system doesn't send any
	 * updates.
	 */
	public void pause();

	/**
	 * Resumes the guidance system after it has been paused. Otherwise does nothing.
	 */
	public void resume();

	/**
	 * Stops the guidance system.
	 */
	public void stop();
}
