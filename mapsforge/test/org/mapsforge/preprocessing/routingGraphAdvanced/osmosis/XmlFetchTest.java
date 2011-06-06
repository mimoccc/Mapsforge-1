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
package org.mapsforge.preprocessing.routingGraphAdvanced.osmosis;

import org.junit.Test;
import org.w3c.dom.NodeList;

/**
 * 
 * Test for getting infos out of xml
 * 
 * @author rob
 * 
 */
public class XmlFetchTest {
	/**
	 * Test
	 */
	@Test
	public void testXmlFatching() {
		// toDo Path relativ machen
		String PATH = "/media/sda6/Uni/8.Semester/Ba-Arbeit/projects/mapsforge/src/org/mapsforge/preprocessing/routingGraphAdvanced/osmosis/config.xml";

		int speed = ReadXMLFile.getMaxSpeedbyVehicle("motorcar", PATH);
		System.out.println(String.format("maxspeed:\n %s", "------------"));
		System.out.println(speed);

		NodeList usableWays = ReadXMLFile.getUsableWaysbyVehicle("motorcar", PATH);
		System.out.println(String.format("all usable Ways:\n %s", "------------"));
		System.out.println(ReadXMLFile.nodeListtoString(usableWays));

		NodeList wayRestrictions = ReadXMLFile.getWayRestrictions("motorcar", PATH);
		System.out.println(String.format("all way restrictions:\n %s", "------------"));
		System.out.println(ReadXMLFile.nodeListtoString(wayRestrictions));

		NodeList relationRestrictions = ReadXMLFile.getRelationRestrictions("motorcar", PATH);
		System.out.println(String.format("all relation restrictions:\n %s", "------------"));
		System.out.println(ReadXMLFile.nodeListtoString(relationRestrictions));

		NodeList stopFactors = ReadXMLFile.getStopWeightFactors("motorcar", PATH);
		System.out.println(String.format("all stop factors:\n %s", "------------"));
		System.out.println(ReadXMLFile.nodeListtoString(stopFactors));

		NodeList reductionWays = ReadXMLFile.getSpeedRedWayTags("motorcar", PATH);
		System.out.println(String.format("all way speed reductions:\n %s", "------------"));
		System.out.println(ReadXMLFile.nodeListtoString(reductionWays));

		NodeList reductionNodes = ReadXMLFile.getSpeedRedNodeTags("motorcar", PATH);
		System.out.println(String.format("all node speed reductions:\n %s", "------------"));
		System.out.println(ReadXMLFile.nodeListtoString(reductionNodes));

		NodeList reductionDynamic = ReadXMLFile.getdynValueTags("motorcar", PATH);
		System.out.println(String.format("all dynamic speed reductions:\n %s", "------------"));
		System.out.println(ReadXMLFile.nodeListtoString(reductionDynamic));

		NodeList noOSM = ReadXMLFile.getNoOSMTags("motorcar", PATH);
		System.out.println(String.format("all noOSM:\n %s", "------------"));
		System.out.println(ReadXMLFile.nodeListtoString(noOSM));
	}
}
