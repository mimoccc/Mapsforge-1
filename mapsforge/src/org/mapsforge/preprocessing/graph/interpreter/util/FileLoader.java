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
package org.mapsforge.preprocessing.graph.interpreter.util;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * The FileLoader to load the osm file of the file system.
 * 
 * @author kunis
 */
public class FileLoader {

	public FileLoader() {
	}

	public File getOsmFile(String url) throws FileNotFoundException {

		if (url == "") {
			throw new FileNotFoundException("No value for osm file.");
		}
		url = "U:\\berlin.osm\\berlin.osm";

		// TODO have to check the file and do a good exception handling

		File file = new File(url);
		System.out.println(file.getAbsolutePath());
		if (!file.exists()) {
			throw new FileNotFoundException("OSM file does not exits.");
		}
		System.out.println("Test");
		return file;
	}
}
