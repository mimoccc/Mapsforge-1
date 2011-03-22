/*
 * Copyright 2010, 2011 mapsforge.org
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
package org.mapsforge.preprocessing.automization;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FileOperation {

	public static File createDirectory(File absolutePath, String directory)
			throws FileNotFoundException, IOException {
		File result = null;

		if (directory.startsWith(File.separator))
			result = new File(directory);
		else {

			if (!absolutePath.exists())
				throw new FileNotFoundException("No such directory: "
						+ absolutePath.getAbsolutePath());
			if (!absolutePath.isDirectory())
				throw new IOException("File is no directory: " + absolutePath.getAbsolutePath());

			result = new File(absolutePath, directory);
		}

		if (!result.exists()) {
			if (!result.mkdirs())
				throw new IOException("Can not create directory: " + result.getAbsolutePath());
		} else if (!result.isDirectory())
			throw new IOException("File is no directory: " + result.getAbsolutePath());
		if (!result.canRead())
			throw new IOException("Can not read from directory: " + result.getAbsolutePath());
		if (!result.canWrite())
			throw new IOException("Can not write to directory: " + result.getAbsolutePath());

		return result;
	}

	public static File createReadFile(String absolutePath, String file)
			throws FileNotFoundException, IOException {

		File result = null;

		if (file.startsWith(File.separator))
			result = new File(file);
		else
			result = new File(absolutePath, file);

		if (!result.exists())
			throw new FileNotFoundException("No such file or directory: "
					+ result.getAbsolutePath());
		if (!result.isFile())
			throw new IOException("File is directory: " + result.getAbsolutePath());
		if (!result.canRead())
			throw new IOException("Can not read file: " + result.getAbsolutePath());

		return result;
	}

	public static File createWriteFile(String absolutePath, String file) throws IOException {

		File result = null;

		if (file.startsWith(File.separator))
			result = new File(file);
		else
			result = new File(absolutePath, file);

		File parent = result.getParentFile();
		if (!parent.exists()) {
			if (!parent.mkdirs())
				throw new IOException("Can not create directory: " + parent.getAbsolutePath());
			else if (!parent.canWrite())
				throw new IOException("Can not write to directory: "
						+ parent.getAbsolutePath());
		}

		return result;
	}

	public static File createReadWriteFile(String absolutePath, String file)
			throws FileNotFoundException, IOException {

		File result = null;

		if (file.startsWith(File.separator))
			result = new File(file);
		else
			result = new File(absolutePath, file);

		if (!result.exists())
			throw new FileNotFoundException("No such file or directory: "
					+ result.getAbsolutePath());
		if (!result.isFile())
			throw new IOException("File is directory: " + result.getAbsolutePath());
		if (!result.canRead())
			throw new IOException("Can not read file: " + result.getAbsolutePath());
		if (!result.canWrite())
			throw new IOException("Can not write to file: " + result.getAbsolutePath());

		return result;
	}

}
