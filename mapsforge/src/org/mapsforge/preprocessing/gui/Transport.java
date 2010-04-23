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

import java.util.Iterator;
import java.util.List;

public class Transport {

	int id;
	String Name;
	int maxSpeed;
	List<Tag> useableWays;

	public Transport(String name, int speed, List<Tag> ways) {
		this.id = -1;
		this.Name = name;
		this.maxSpeed = speed;
		this.useableWays = ways;
	}

	public Transport(int id, String name, int speed, List<Tag> ways) {
		this.id = id;
		this.Name = name;
		this.maxSpeed = speed;
		this.useableWays = ways;
	}

	// Getter
	/**
	 * @return the name
	 */
	public String getName() {
		return Name;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return the maxSpeed
	 */
	public int getMaxSpeed() {
		return maxSpeed;
	}

	/**
	 * @return the useableWays
	 */
	public List<Tag> getUseableWays() {
		return useableWays;
	}

	public String getUseableWaysSerialized() {
		// TODO Auto-generated method stub
		String result = "";
		Iterator<Tag> it = useableWays.iterator();
		while (it.hasNext()) {
			result.concat(it.next().toString() + ";");

		}

		return result;
	}

	// Setter
	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @param useableWays
	 *            the useableWays to set
	 */
	public void setUseableWays(List<Tag> useableWays) {
		this.useableWays = useableWays;
	}

}
