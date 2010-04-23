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

public class Profil {

	private int pid;
	private String name;
	private String url;
	private Transport transport;
	private String heuristic;

	public Profil(int pid, String name, String url, Transport transport, String heuristic) {
		this.setPid(pid);
		this.name = name;
		this.url = url;
		this.transport = transport;
		this.heuristic = heuristic;
	}

	// Getter
	/**
	 * @return the pid
	 */
	public int getPid() {
		return pid;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @return the transport
	 */
	public Transport getTransport() {
		return transport;
	}

	/**
	 * @return the heuristic
	 */
	public String getHeuristic() {
		return heuristic;
	}

	// Setter
	/**
	 * @param pid
	 *            the pid to set
	 */
	public void setPid(int pid) {
		this.pid = pid;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param url
	 *            the url to set
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * @param transport
	 *            the transport to set
	 */
	public void setTransport(Transport transport) {
		this.transport = transport;
	}

	/**
	 * @param heuristic
	 *            the heuristic to set
	 */
	public void setHeuristik(String heuristic) {
		this.heuristic = heuristic;
	}

}
