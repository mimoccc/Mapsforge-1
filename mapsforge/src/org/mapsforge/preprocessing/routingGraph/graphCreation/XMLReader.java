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
package org.mapsforge.preprocessing.routingGraph.graphCreation;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class parses a given file (in XML-Format) to determine, which tags will be needed for the graph,
 * when extracting data from the pbf-file.
 * 
 * @author Michael Bartel
 * 
 */
public class XMLReader {

	Set<StringPair> wayTagsSet;
	Set<StringPair> nodeTagsSet;
	Set<StringPair> relationTagsSet;

	int vehiclecount = 0;

	/**
	 * The method to parse the XML-Configfile
	 * 
	 * @param file
	 *            the config to define the parsing
	 * @return an Instance of ConfigObject which contains all necessary data
	 * @throws ParserConfigurationException
	 *             an exception to be thrown
	 * @throws SAXException
	 *             an exception to be thrown
	 * @throws IOException
	 *             an exception to be thrown
	 */
	public ConfigObject parseXML(String file) throws ParserConfigurationException, SAXException,
			IOException {
		System.out.println("[RGC] XML-Parse started with File: " + file);
		wayTagsSet = new HashSet<StringPair>();
		nodeTagsSet = new HashSet<StringPair>();
		relationTagsSet = new HashSet<StringPair>();

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(file);
		doc.getDocumentElement().normalize();

		// routing Tags
		NodeList routingTags = doc.getElementsByTagName("routingTags");
		if (routingTags.getLength() != 1) {
			System.out.println("[RGC]: XML-File not in valid format.");
			return null;
		}

		// Traverse the routingTags-Children: wayTags, nodeTags, relationTags
		for (int i = 0; i < routingTags.item(0).getChildNodes().getLength(); i++) {
			Node tmpnode1 = routingTags.item(0).getChildNodes().item(i);

			// Traverse the Children of wayTags, nodeTags, relationTags
			for (int k = 0; k < tmpnode1.getChildNodes().getLength(); k++) {
				Node tmpnode2 = tmpnode1.getChildNodes().item(k);

				if (tmpnode2.hasAttributes()) {
					StringPair sp = getKeyValue(tmpnode2);

					// Add the found pairs to corresponding Sets
					if (tmpnode1.getNodeName().equals("wayTags"))
						wayTagsSet.add(sp);
					if (tmpnode1.getNodeName().equals("nodeTags"))
						nodeTagsSet.add(sp);
					if (tmpnode1.getNodeName().equals("relationTags"))
						relationTagsSet.add(sp);

				}
			}
		}
		// routing Tags End

		// Vehicles
		NodeList vehicles = doc.getElementsByTagName("vehicles");
		if (vehicles.getLength() != 1) {
			System.out.println("[RGC]: XML-File not in valid format. Too much ");
			return null;
		}

		// Traverse the vehicles-children: vehicle
		for (int i = 0; i < vehicles.item(0).getChildNodes().getLength(); i++) {
			Node tmpnode1 = vehicles.item(0).getChildNodes().item(i);
			if (tmpnode1.getNodeName().equals("vehicle")) {
				vehiclecount++;

				// Traverse the children of vehicle: (name), (maxspeed), usableWayTags, restrictions,
				// weightFactors
				for (int k = 0; k < tmpnode1.getChildNodes().getLength(); k++) {
					Node tmpnode2 = tmpnode1.getChildNodes().item(k);

					// Type usableWayTags
					if (tmpnode2.getNodeName().equals("usableWayTags") && (tmpnode2.hasChildNodes())) {

						// Traverse Children: tags
						for (int j = 0; j < tmpnode2.getChildNodes().getLength(); j++) {
							Node tmpnode3 = tmpnode2.getChildNodes().item(j);
							if (tmpnode3.getNodeName().equals("tag"))
								wayTagsSet.add(getKeyValue(tmpnode3));
						}
					}

					// Type restrictions: wayTags, relations
					if (tmpnode2.getNodeName().equals("restrictions") && (tmpnode2.hasChildNodes())) {

						// Traverse Children of Child(1) wayTags: tags
						for (int j = 0; j < tmpnode2.getChildNodes().item(1).getChildNodes()
								.getLength(); j++) {
							Node tmpnode3 = tmpnode2.getChildNodes().item(1).getChildNodes().item(j);
							if (tmpnode3.getNodeName().equals("tag"))
								wayTagsSet.add(getKeyValue(tmpnode3));
						}

						// Traverse Children of Child(3) relations: tags
						for (int j = 0; j < tmpnode2.getChildNodes().item(3).getChildNodes()
								.getLength(); j++) {
							Node tmpnode3 = tmpnode2.getChildNodes().item(3).getChildNodes().item(j);
							if (tmpnode3.getNodeName().equals("tag"))
								relationTagsSet.add(getKeyValue(tmpnode3));
						}

					}

					if (tmpnode2.getNodeName().equals("weightFactors") && (tmpnode2.hasChildNodes())) {

						if (tmpnode2.getChildNodes().getLength() < 4) {
							System.out.println("[RCG] Error Parsing");
							System.out
									.println("[RCG] \"weightFactors\" needs to have children: stopNodeTags, speedreductions, noOSMTags");
							break;
						}

						Node traversNode = tmpnode2.getChildNodes().item(1);

						if (traversNode.hasChildNodes())
							// Traverse Children of Child(1) stopdeNodeTags: tags
							for (int j = 0; j < traversNode.getChildNodes()
									.getLength(); j++) {
								Node tmpnode3 = traversNode.getChildNodes().item(j);
								if (tmpnode3.getNodeName().equals("tag"))
									nodeTagsSet.add(getKeyValue(tmpnode3));
							}

						traversNode = tmpnode2.getChildNodes().item(3);
						if (traversNode.getChildNodes().getLength() < 6) {
							System.out.println("[RCG] Error Parsing");
							System.out
									.println("[RCG] \"speedreductions\" needs to have children: wayTags, nodeTags, dynamicWayTags");
							break;
						}
						// Traverse Children of Child(3,1) speedreductions wayTags: tags
						for (int j = 0; j < traversNode.getChildNodes().item(1)
								.getChildNodes()
								.getLength(); j++) {
							Node tmpnode3 = traversNode.getChildNodes().item(1)
									.getChildNodes().item(j);
							if (tmpnode3.getNodeName().equals("tag"))
								wayTagsSet.add(getKeyValue(tmpnode3));
						}

						// Traverse Children of Child(3,3) speedreductions nodeTags: tags
						for (int j = 0; j < traversNode.getChildNodes().item(3)
								.getChildNodes()
								.getLength(); j++) {
							Node tmpnode3 = traversNode.getChildNodes().item(3)
									.getChildNodes().item(j);
							if (tmpnode3.getNodeName().equals("tag"))
								nodeTagsSet.add(getKeyValue(tmpnode3));
						}

						// Traverse Children of Child(3,5) speedreductions dynamicWayTags: tags
						for (int j = 0; j < traversNode.getChildNodes().item(5)
								.getChildNodes()
								.getLength(); j++) {
							Node tmpnode3 = traversNode.getChildNodes().item(5)
									.getChildNodes().item(j);
							if (tmpnode3.getNodeName().equals("tag"))
								nodeTagsSet.add(getKeyValue(tmpnode3));
						}

					}
				}
			}
		}
		System.out.println("[RGC] finished...");
		System.out.println("[RGC] Vehicles processed: " + vehiclecount);
		System.out.println("[RGC] wayTags found: " + wayTagsSet.size());
		System.out.println("[RGC] nodeTags found: " + nodeTagsSet.size());
		System.out.println("[RGC] relationsTags found: " + relationTagsSet.size());

		return new ConfigObject(wayTagsSet, nodeTagsSet, relationTagsSet);

	}

	/**
	 * For testcases: To be removed...
	 * 
	 * @param args
	 *            - not needed
	 */
	public static void main(String[] args) {
		try {
			new XMLReader().parseXML("D:\\config_new.xml");
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private StringPair getKeyValue(Node n) {

		// no attributes exist
		if (!(n.hasAttributes()))
			return null;

		String val = null;
		String key = null;

		// Check all attributes for key/value
		for (int j = 0; j < n.getAttributes().getLength(); j++) {
			Attr tmpatt = ((Attr) n.getAttributes().item(j));
			if (tmpatt.getName().equals("v"))
				val = tmpatt.getValue();
			if (tmpatt.getName().equals("k"))
				key = tmpatt.getValue();
		}
		return new StringPair(val, key);
	}

	class StringPair {

		String value;
		String key;

		StringPair(String v, String k) {
			value = v;
			key = k;
			// System.out.println("Key: " + k + " Value: " + v);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof StringPair) {
				StringPair o = (StringPair) obj;

				if (o.value == null)
					return ((key.equals(o.key)) && (value == null));
				if (value == null)
					return ((key.equals(o.key)) && (o.value == null));

				return ((value.equals(o.value)) && (key.equals(o.key)));
			}
			return false;
		}

		@Override
		public int hashCode() {
			if (value == null)
				return key.hashCode();
			return value.hashCode() + key.hashCode();
		}

	}
}
