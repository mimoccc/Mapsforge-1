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

//TODO Create guidance specific update class.

/**
 * This interface defines a listener for the guidance service provided through {@link GuidanceService}.
 * The {@link GuidanceListener} would typically be implemented by an application that seeks to provide
 * guidance functionality to the user.
 * 
 * @author thilo ratnaweera
 */
public interface GuidanceListener {

	/**
	 * Called by a {@link GuidanceService} to send guidance updates to this listener.
	 * 
	 * @param guidanceUpdate
	 *            Object containing guidance update information.
	 */
	public void onGuidanceUpdate(Object guidanceUpdate);

}