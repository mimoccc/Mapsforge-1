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
package org.mapsforge.preprocessing.graph.gui.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.mapsforge.preprocessing.graph.model.gui.Profil;
import org.mapsforge.preprocessing.model.EHighwayLevel;

import com.thoughtworks.xstream.XStream;

public class SimpleRoutingConfigurationWriter {

	private Profil profil;

	public SimpleRoutingConfigurationWriter(Profil p) {

		this.profil = p;
	}

	public void createFile() throws Exception {
		File configFile = new File("U:\\berlin.osm\\testprofil.profil");

		if (!configFile.exists()) {
			try {
				configFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			// TODO file existiert bereits, es müsste abgefragt werden, ob die bestehende datei
			// überschrieben werden soll oder nicht
		}

		writeProfil2File(configFile);
	}

	private void writeProfil2File(File configFile) throws Exception {

		final XStream xs = new XStream();

		xs.alias("profil", Profil.class);
		xs.alias("highway", EHighwayLevel.class);

		String xml = xs.toXML(profil);

		try {
			FileWriter fw = new FileWriter(configFile);
			fw.write(xml);
			fw.flush();
			fw.close();
		} catch (IOException e) {
			throw new Exception("Can not write configuration file.");
		}
	}
}
