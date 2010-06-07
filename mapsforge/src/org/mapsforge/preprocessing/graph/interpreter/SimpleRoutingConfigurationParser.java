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
package org.mapsforge.preprocessing.graph.interpreter;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.mapsforge.preprocessing.graph.model.gui.DatabaseProperties;
import org.mapsforge.preprocessing.graph.model.gui.Profil;
import org.mapsforge.preprocessing.graph.model.gui.Transport;
import org.mapsforge.preprocessing.model.EHighwayLevel;
import org.mapsforge.preprocessing.util.HighwayLevelExtractor;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SimpleRoutingConfigurationParser extends DefaultHandler {

	private Profil profil;
	private Transport transport;
	private DatabaseProperties dbprops;
	private String osmUrl;

	private String currentObject, characters;
	String transportName, profilName, key, value, username, password, dbname, host;
	int maxSpeed, port;
	HashSet<EHighwayLevel> highways = new HashSet<EHighwayLevel>();

	public SimpleRoutingConfigurationParser(File file) {

		createParser(file);
	}

	private void createParser(File file) {
		DefaultHandler saxParser = this;

		// get a factory
		SAXParserFactory spf = SAXParserFactory.newInstance();
		try {

			// get a new instance of parser
			SAXParser sp = spf.newSAXParser();

			// parse the file and also register this class for call backs
			sp.parse(file.getAbsolutePath(), saxParser);

		} catch (SAXException se) {
			se.printStackTrace();
		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (IOException ie) {
			ie.printStackTrace();
		}
	}

	@Override
	public void startDocument() throws SAXException {
		super.startDocument();
		currentObject = null;
		transportName = profilName = key = value = username = password = dbname = host = "";
		maxSpeed = port = -1;
	}

	@Override
	public void endDocument() throws SAXException {
		super.endDocument();
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes)
			throws SAXException {

		System.out.println("<" + qName + ">");
		if (qName.toLowerCase().equals("profil") || qName.toLowerCase().equals("transport")) {
			currentObject = qName.toLowerCase();
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		EHighwayLevel currenHwyLvl = null;

		if (qName.toLowerCase().equals("profil")) {
			if (profilName == "" || transport == null || dbprops == null) {
				System.err.println("This profil has some illegal values.");
				System.exit(-1);
			} else {
				profil = new Profil(profilName, null, transport, null, dbprops);
			}
			currentObject = null;
		} else if (qName.toLowerCase().equals("transport")) {
			if (transportName == "" || maxSpeed <= 0) {
				System.err.println("There are missing some arguments. Can't create transport.");
				System.exit(-1);
			} else {
				transport = new Transport(transportName, maxSpeed, highways);
			}
			currentObject = "profil";

		} else if (qName.toLowerCase().equals("dbprops")) {
			if (host == "" || dbname == "" || username == "" || password == "" || port < 0) {
				System.err
						.println("Can't create a database connection, because the values are invalid.");
				System.exit(-1);
			} else {
				dbprops = new DatabaseProperties(host, port, dbname, username, password);
			}

		} else if (qName.toLowerCase().equals("name")) {
			if (currentObject.equals("profil")) {
				profilName = characters;
			} else if (currentObject.equals("transport")) {
				System.out.print(characters);
				transportName = characters;
			}
		} else if (qName.toLowerCase().equals("highway")) {

			currenHwyLvl = HighwayLevelExtractor.getLevel(characters);
			if (currenHwyLvl != null)
				highways.add(currenHwyLvl);
		} else if (qName.toLowerCase().equals("maxspeed")) {
			maxSpeed = Integer.parseInt(characters);
		} else if (qName.toLowerCase().equals("key")) {
			key = characters;
		} else if (qName.toLowerCase().equals("value")) {
			value = characters;
		} else if (qName.toLowerCase().equals("host")) {
			host = characters;
		} else if (qName.toLowerCase().equals("username")) {
			username = characters;
		} else if (qName.toLowerCase().equals("password")) {
			password = characters;
		} else if (qName.toLowerCase().equals("dbname")) {
			dbname = characters;
		} else if (qName.toLowerCase().equals("port")) {
			port = Integer.parseInt(characters);
		}

	}

	@Override
	public void characters(char ch[], int start, int length) {
		characters = "";

		for (int i = start; i < start + length; i++) {
			switch (ch[i]) {
				case '\\':
					System.out.println("\\\\");
					break;
				case '"':
					System.out.println("\\\"");
					break;
				case '\n':
					System.out.println("\\n");
					break;
				case '\r':
					System.out.println("\\r");
					break;
				case '\t':
					System.out.println("\\t");
					break;
				default:
					characters += ch[i];
					break;
			}
		}
	}

	public void parse() {
		// TODO soll der parser per konstruktor oder per parse methode aufgerufen werden.
		// je nach dem muss der konstruktor und diese methode angepasst werden
	}

	public void setTransport(Transport transport) {
		this.transport = transport;
	}

	public Transport getTransport() {
		return transport;
	}

	public void setOsmUrl(String osmUrl) {
		this.osmUrl = osmUrl;
	}

	public String getOsmUrl() {
		return osmUrl;
	}

	public void setProfil(Profil profil) {
		this.profil = profil;
	}

	public Profil getProfil() {
		return profil;
	}

	public static void main(String[] args) {
		File file = new File("U:\\berlin.osm\\testprofil.profil");
		if (!file.exists() || !file.isFile() || !file.canRead()) {
			System.out.println("Can not read file. Maybe istn't one.");
			System.exit(-1);
		}
		SimpleRoutingConfigurationParser parser = new SimpleRoutingConfigurationParser(file);
		System.out.println("profil name: " + parser.profil.getName());
		System.out.println("transport name: " + parser.profil.getTransport().getName());
		System.out.println("transport ways: "
				+ parser.profil.getTransport().getUseableWaysSerialized());

	}

	public void setDbprops(DatabaseProperties dbprops) {
		this.dbprops = dbprops;
	}

	public DatabaseProperties getDbprops() {
		return dbprops;
	}
}
