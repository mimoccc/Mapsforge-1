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
package org.mapsforge.preprocessing.graph.model.osmxml;

import java.sql.Timestamp;
import java.util.LinkedList;

/**
 * @author Frank Viernau
 */
public class OsmRelation extends OsmElement {

	private final LinkedList<Member> wayRefs;

	public OsmRelation(long id) {
		super(id);
		this.wayRefs = new LinkedList<Member>();
	}

	public OsmRelation(long id, Timestamp timestamp, String user, boolean visible) {
		super(id, timestamp, user, visible);
		this.wayRefs = new LinkedList<Member>();
	}

	public void addMember(Member ref) {
		wayRefs.add(ref);
	}

	public LinkedList<Member> getMembers() {
		return wayRefs;
	}

	public static class Member {

		public final static String TYPE_WAY = "way";
		public final static String ROLE_OUTER = "outer";
		public final static String ROLE_INNER = "innter";
		public final static String ROLE_FORWARD = "forward";
		public final static String ROLE_BACKWARD = "backward";

		public final String type, role;
		public final long refId;

		public Member(String type, String role, long refId) {
			this.type = type;
			this.role = role;
			this.refId = refId;
		}
	}
}
