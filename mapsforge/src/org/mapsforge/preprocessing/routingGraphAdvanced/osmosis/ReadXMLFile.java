package org.mapsforge.preprocessing.routingGraphAdvanced.osmosis;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 
 * @author Robert
 * 
 *         Class to get information out of the XML-config file
 */
public class ReadXMLFile {

	/**
	 * @param vehicle
	 *            vehicle type you want
	 * @param xmlFilePath
	 *            path of XML config file
	 * @return maxspeed in km/h as Integer
	 */
	public static Integer getMaxSpeedbyVehicle(String vehicle, String xmlFilePath) {
		Document doc = getXmlDocument(xmlFilePath);
		XPath xpath = XPathFactory.newInstance().newXPath();
		try {
			XPathExpression xe = xpath
					.compile("//vehicle[name='" + vehicle + "']/maxspeed[1]");

			Node node = (Node) xe.evaluate(doc, XPathConstants.NODE);
			return Integer.parseInt(node.getTextContent());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}

	/**
	 * @param vehicle
	 *            vehicle type you want
	 * @param xmlFilePath
	 *            path of XML config file
	 * @return a list of tags represented as XML-Nodes
	 */
	public static NodeList getUsableWaysbyVehicle(String vehicle, String xmlFilePath) {
		Document doc = getXmlDocument(xmlFilePath);
		XPath xpath = XPathFactory.newInstance().newXPath();
		try {
			XPathExpression xe = xpath
					.compile("//vehicle[name='" + vehicle + "']/usebleWayTags/child::tag");

			NodeList nList = (NodeList) xe.evaluate(doc, XPathConstants.NODESET);
			return nList;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 
	 * @param vehicle
	 *            vehicle type you want
	 * @param xmlFilePath
	 *            path of XML config file
	 * @return a list of tags represented as XML-Nodes
	 */
	public static NodeList getWayRestrictions(String vehicle, String xmlFilePath) {

		Document doc = getXmlDocument(xmlFilePath);
		XPath xpath = XPathFactory.newInstance().newXPath();
		try {
			XPathExpression xe = xpath.compile("//vehicle[name='" + vehicle
					+ "']/restrictions/wayTags/*");
			NodeList nList = (NodeList) xe.evaluate(doc, XPathConstants.NODESET);
			return nList;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param vehicle
	 *            vehicle type you want
	 * @param xmlFilePath
	 *            path of XML config file
	 * @return a list of tags represented as XML-Nodes
	 */
	public static NodeList getRelationRestrictions(String vehicle, String xmlFilePath) {

		Document doc = getXmlDocument(xmlFilePath);
		XPath xpath = XPathFactory.newInstance().newXPath();
		try {
			XPathExpression xe = xpath.compile("//vehicle[name='" + vehicle
					+ "']/restrictions/relations/*");
			NodeList nList = (NodeList) xe.evaluate(doc, XPathConstants.NODESET);
			return nList;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param vehicle
	 *            vehicle type you want
	 * @param xmlFilePath
	 *            path of XML config file
	 * @return a list of tags represented as XML-Nodes
	 */
	public static NodeList getStopWeightFactors(String vehicle, String xmlFilePath) {

		Document doc = getXmlDocument(xmlFilePath);
		XPath xpath = XPathFactory.newInstance().newXPath();
		try {
			XPathExpression xe = xpath.compile("//vehicle[name='" + vehicle
					+ "']/weightFactors/stopNodeTags/*");
			NodeList nList = (NodeList) xe.evaluate(doc, XPathConstants.NODESET);
			return nList;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param vehicle
	 *            vehicle type you want
	 * @param xmlFilePath
	 *            path of XML config file
	 * @return a list of tags represented as XML-Nodes
	 */
	public static NodeList getSpeedRedWayTags(String vehicle, String xmlFilePath) {

		Document doc = getXmlDocument(xmlFilePath);
		XPath xpath = XPathFactory.newInstance().newXPath();
		try {
			XPathExpression xe = xpath.compile("//vehicle[name='" + vehicle
					+ "']/weightFactors/speedreductions/wayTags/*");
			NodeList nList = (NodeList) xe.evaluate(doc, XPathConstants.NODESET);
			return nList;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param vehicle
	 *            vehicle type you want
	 * @param xmlFilePath
	 *            path of XML config file
	 * @return a list of tags represented as XML-Nodes
	 */
	public static NodeList getSpeedRedNodeTags(String vehicle, String xmlFilePath) {

		Document doc = getXmlDocument(xmlFilePath);
		XPath xpath = XPathFactory.newInstance().newXPath();
		try {
			XPathExpression xe = xpath.compile("//vehicle[name='" + vehicle
					+ "']/weightFactors/speedreductions/nodeTags/*");
			NodeList nList = (NodeList) xe.evaluate(doc, XPathConstants.NODESET);
			return nList;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param vehicle
	 *            vehicle type you want
	 * @param xmlFilePath
	 *            path of XML config file
	 * @return a list of tags represented as XML-Nodes
	 */
	public static NodeList getdynValueTags(String vehicle, String xmlFilePath) {

		Document doc = getXmlDocument(xmlFilePath);
		XPath xpath = XPathFactory.newInstance().newXPath();
		try {
			XPathExpression xe = xpath.compile("//vehicle[name='" + vehicle
					+ "']/weightFactors/speedreductions/dynamicWayTags/*");
			NodeList nList = (NodeList) xe.evaluate(doc, XPathConstants.NODESET);
			return nList;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param vehicle
	 *            vehicle type you want
	 * @param xmlFilePath
	 *            path of XML config file
	 * @return a list of tags represented as XML-Nodes
	 */
	public static NodeList getNoOSMTags(String vehicle, String xmlFilePath) {

		Document doc = getXmlDocument(xmlFilePath);
		XPath xpath = XPathFactory.newInstance().newXPath();
		try {
			XPathExpression xe = xpath.compile("//vehicle[name='" + vehicle
					+ "']/weightFactors/noOSMTags/*");
			NodeList nList = (NodeList) xe.evaluate(doc, XPathConstants.NODESET);
			return nList;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param xmlFilePath
	 *            path of XML config file
	 * @return return the xml document to modify or just read it
	 */
	private static Document getXmlDocument(String xmlFilePath) {
		try {
			File xmlFile = new File(xmlFilePath);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(xmlFile);
			doc.getDocumentElement().normalize();
			return doc;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 
	 * @param nList
	 *            listWith nodes
	 * @return String with all nodes in readable format
	 */
	public static String nodeListtoString(NodeList nList) {
		String result = null;
		for (int i = 0; i < nList.getLength(); i++) {
			Node node = nList.item(i);

			if (node.getNodeType() == Node.ELEMENT_NODE) {
				String line;
				Element eElement = (Element) node;
				line = "<" + eElement.getTagName() + " ";
				NamedNodeMap attributes = eElement.getAttributes();
				/* go trough all attributes */
				for (int j = 0; j < attributes.getLength(); j++) {
					Node nodeAttribute = attributes.item(j);
					line += nodeAttribute.getNodeName() + "=" + nodeAttribute.getNodeValue() + " ";
					if (j == attributes.getLength() - 1) {
						line += "/>";
					}
				}
				if (result == null)
					result = line + "\n";
				else
					result += line + "\n";
			}
		}
		return result;

	}
}