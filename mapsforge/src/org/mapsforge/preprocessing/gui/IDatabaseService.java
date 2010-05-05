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
package org.mapsforge.preprocessing.gui;

import java.util.ArrayList;

public interface IDatabaseService {

	/**
	 * Insert a new transport object into the database.
	 * 
	 * @param transport
	 *            the transport object that should add to the database
	 * @throws Exception
	 */
	public void addTransport(Transport transport);
	
	/**
	 * Update an existing transport object in the database.
	 *  
	 * @param transport 
	 */
	public void updateTransport(Transport transport); 
	
	/**
	 * Delete the transport object with the given name of the database;
	 * 
	 * @param name
	 */
	public void deleteTransport(String name);
	
	/**
	 * @param transportName 
	 * @return transport
	 */
	public Transport getTransport(String transportName);

	/**
	 * Get all transport objects of the database.
	 * 
	 * @return a list of transport objects
	 */
	public ArrayList<Transport> getAllTransports();

	public void addProfil(Profil profil);

	public ArrayList<Profil> getAllProfilsOfTransport(Transport transport);

}
