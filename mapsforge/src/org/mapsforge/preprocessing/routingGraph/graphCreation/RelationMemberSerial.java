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

import java.io.Serializable;

import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;

/**
 * A osmosis RelationMember class serializable.
 * 
 * @author rob
 * 
 */
public class RelationMemberSerial implements Serializable {

	long memberId;
	EntityType memberType;
	String memberRole;

	public long getMemberId() {
		return memberId;
	}

	public EntityType getMemberType() {
		return memberType;
	}

	public String getMemberRole() {
		return memberRole;
	}

	/**
	 * @param memberId
	 *            ID of a member
	 * @param memberType
	 *            Type (node, way or relation) of a member
	 * @param memberRole
	 *            role name of a member
	 */
	public RelationMemberSerial(long memberId, EntityType memberType, String memberRole) {
		this.memberId = memberId;
		this.memberRole = memberRole;
		this.memberType = memberType;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
