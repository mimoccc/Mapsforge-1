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

import java.util.Set;

import org.mapsforge.preprocessing.routingGraph.graphCreation.XMLReader.StringPair;

/**
 * This class bundles given Key/Value-Pairs for OSM-Extraction.
 * 
 * @author Michael Bartel
 * 
 */
public class ConfigObject {

	Set<StringPair> wayTagsSet;
	Set<StringPair> nodeTagsSet;
	Set<StringPair> relationTagsSet;

	/**
	 * Constructor for Creation
	 * 
	 * @param wt
	 *            - The Set including all needed Key/Value-Pairs of OSM.ways
	 * @param nt
	 *            - The Set including all needed Key/Value-Pairs of OSM.nodes
	 * @param rt
	 *            - The Set including all needed Key/Value-Pairs of OSM.relations
	 */
	public ConfigObject(Set<StringPair> wt, Set<StringPair> nt, Set<StringPair> rt) {
		this.wayTagsSet = wt;
		this.nodeTagsSet = nt;
		this.relationTagsSet = rt;
	}

	/**
	 * Returns true if the key/value pair exists in the corresponding set
	 * 
	 * @param key
	 *            The key to be checked
	 * @param value
	 *            The value to be checked
	 * @return true, if the pair exists
	 */
	public boolean containsWayTag(String key, String value) {
		/*
		 * for (StringPair sp : wayTagsSet) // Tags with sp.value==null are not a criteria to chose
		 * wayTags // e.g. the tag name exists for many ways, that are not roads if (sp.value != null) {
		 * if ((sp.key.equals(key)) && sp.value.equals(value)) return true;
		 * 
		 * return false; }
		 */
		return wayTagsSet.contains(new XMLReader().new StringPair(value, key));
	}

	/**
	 * Returns true if the key/value pair exists in the corresponding set
	 * 
	 * @param key
	 *            The key to be checked
	 * @param value
	 *            The value to be checked
	 * @return true, if the pair exists
	 */
	public boolean containsRelationTag(String key, String value) {
		/*
		 * for (StringPair sp : relationTagsSet) if (sp.value != null) if ((sp.key.equals(key)) &&
		 * sp.value.equals(value)) return true; return false;
		 */
		return relationTagsSet.contains(new XMLReader().new StringPair(value, key));
	}

	/**
	 * Returns true if the key/value pair exists in the corresponding set
	 * 
	 * @param key
	 *            The key to be checked
	 * @param value
	 *            The value to be checked
	 * @return true, if the pair exists
	 */
	public boolean containsNodeTag(String key, String value) {
		/*
		 * for (StringPair sp : nodeTagsSet) if (sp.value != null) if ((sp.key.equals(key)) &&
		 * sp.value.equals(value)) return true; return false;
		 */
		return nodeTagsSet.contains(new XMLReader().new StringPair(value, key));
	}

}
