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
package org.mapsforge.preprocessing.routingGraph.graphCreation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

/**
 * Class to create and read a binary file.
 * 
 * @author Robert Fels
 * 
 */
public class BinaryFileCreator {
	/**
	 * Write an object into a binary file
	 * 
	 * @param fileName
	 *            Path and file name in a string
	 * @param objects
	 *            objects you want to store in the binary file
	 */

	public void saveToBinary(String fileName, ArrayList<Object> objects) {
		try {

			File file;
			file = new File(fileName);
			if (!file.exists())
				file.createNewFile();

			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file));
			out.writeObject(objects);
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Read the objects stored in a binary file into a ArrayList
	 * 
	 * @param fileName
	 *            Path and file name in a string
	 * @return
	 */
	public ArrayList<Object> readBinary(String fileName) {
		ArrayList<Object> result = null;
		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(fileName));
			result = (ArrayList<Object>) in.readObject();
			in.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;

	}
}
