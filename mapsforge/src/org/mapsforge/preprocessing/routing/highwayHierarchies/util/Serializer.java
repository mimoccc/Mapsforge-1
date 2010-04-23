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
package org.mapsforge.preprocessing.routing.highwayHierarchies.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * @author Frank Viernau
 */
public class Serializer {

	public static void serialize(OutputStream oStream, Serializable s) throws IOException {
		ObjectOutputStream out = new ObjectOutputStream(oStream);
		out.writeObject(s);
		out.flush();
	}

	public static void serialize(File dst, Serializable s) throws IOException {
		FileOutputStream oStream = new FileOutputStream(dst);
		serialize(oStream, s);
		oStream.close();
	}

	public static <S extends Serializable> S deserialize(InputStream iStream)
			throws IOException, ClassNotFoundException {
		ObjectInputStream in = new ObjectInputStream(iStream);
		S s = (S) in.readObject();
		return s;
	}

	public static <S extends Serializable> S deserialize(File src) throws IOException,
			ClassNotFoundException {
		FileInputStream iStream = new FileInputStream(src);
		S s = deserialize(iStream);
		iStream.close();
		return s;
	}

}
