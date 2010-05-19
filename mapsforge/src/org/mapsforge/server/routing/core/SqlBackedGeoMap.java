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
package org.mapsforge.server.routing.core;

import java.io.DataOutput;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;

import org.mapsforge.server.core.geoinfo.BoundingBox;
import org.mapsforge.server.core.geoinfo.Node;
import org.mapsforge.server.core.geoinfo.Point;
import org.mapsforge.server.routing.core.ConnectionHandler.ICallback;

/** package private */
public final class SqlBackedGeoMap implements IGeoMap {

	public static enum SqlCommand implements IProperty {
		GET_BOUNDINGBOX, GET_POINT_FOR_VERTEX_ID, GET_VERTICES_FOR_WAYPOINT, GET_WAYPOINTS_FOR_POINT, GET_INTERMEDIATE_NODES_FOR_WAY,
	}

	public static SqlBackedGeoMap newInstance(String name, Date date, ConnectionHandler conHlr,
			PropertyConfiguration<?> anyConf) throws ComponentInitializationException {
		return new SqlBackedGeoMap(name, date, conHlr, anyConf);
	}

	protected final EnumMap<SqlCommand, ICallback<?>> callbacks;

	protected final ConnectionHandler conHlr;

	private final BoundingBox bbox;

	private final Date date;

	private final String name;

	/**
	 * (package private constructor enforces overriding classes to be in same package)
	 * 
	 * @param name
	 * @param date
	 * @param conHlr
	 * @param anyConf
	 * @throws ComponentInitializationException
	 */
	SqlBackedGeoMap(String name, Date date, ConnectionHandler conHlr,
			PropertyConfiguration<?> anyConf) throws ComponentInitializationException {

		final PropertyConfiguration<SqlCommand> sqlConf = PropertyConfiguration.newInstance(
				anyConf, SqlCommand.class);

		this.name = name;
		this.date = date;
		this.conHlr = conHlr;
		this.bbox = conHlr.handle(new ICallback<BoundingBox>() {

			private final String stmt = sqlConf.get(SqlCommand.GET_BOUNDINGBOX);

			@Override
			public BoundingBox callback(Connection con, Object... objects) throws SQLException {
				/**
				 * Bounding box
				 * 
				 * SQL Parameter Expectations: <br/>
				 * none
				 * 
				 * SQL ResultSet Expectations: <br/>
				 * 1st column is the left lower point's latitude <br/>
				 * 2nd column is the left lower point's longitude <br/>
				 * 3rd column is the right upper point's latitude <br/>
				 * 4th column is the right upper point's longitude
				 */

				/** use PreparedStatement and execute query */
				PreparedStatement pStmt = con.prepareStatement(this.stmt);
				ResultSet rs = pStmt.executeQuery();

				/** read database results */
				rs.next();
				return BoundingBox.getInstance(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs
						.getInt(4));
			}
		});
		this.callbacks = new EnumMap<SqlCommand, ICallback<?>>(SqlCommand.class);
		this.callbacks.put(SqlCommand.GET_VERTICES_FOR_WAYPOINT,
				new ICallback<VertexDistance[]>() {

					private final String stmt = sqlConf
							.get(SqlCommand.GET_VERTICES_FOR_WAYPOINT);

					@Override
					public VertexDistance[] callback(Connection con, Object... objects)
							throws SQLException {
						/**
						 * vertices with distances
						 * 
						 * SQL Parameter Expectations: <br/>
						 * 1st is the point's latitude <br/>
						 * 2nd is the point's longitude <br/>
						 * 
						 * SQL ResultSet Expectations: <br/>
						 * 1st column is the vertex ID <br/>
						 * 2nd column is the cost to reach that vertex
						 */
						if (!(objects[0] instanceof Point))
							throw new IllegalArgumentException(Issue.CALLBACK_TYPE_ASSERTION
									.msg());
						Point wayPoint = (Point) objects[0];

						/** use PreparedStatement and execute query */
						PreparedStatement pStmt = con.prepareStatement(this.stmt);
						pStmt.setInt(1, wayPoint.getLat());
						pStmt.setInt(2, wayPoint.getLon());
						ResultSet rs = pStmt.executeQuery();

						/** read database results */
						if (rs.next()) {
							int vtxId1 = rs.getInt(1);
							int distance = rs.getInt(2);
							if (rs.next()) {
								return new VertexDistance[] {
										new VertexDistance(vtxId1, distance),
										new VertexDistance(rs.getInt(1), rs.getInt(2)) };
							}
							/**
							 * if the result is a single vertex, only examine the first result
							 * ID
							 */
							return new VertexDistance[] { new VertexDistance(vtxId1, distance) };
						}
						return EMPTY_VERTEXDISTANCE_ARRAY;
					}
				});
		this.callbacks.put(SqlCommand.GET_INTERMEDIATE_NODES_FOR_WAY,
				new ICallback<List<Node>>() {

					private final String stmt = sqlConf
							.get(SqlCommand.GET_INTERMEDIATE_NODES_FOR_WAY);

					@Override
					public List<Node> callback(Connection con, Object... objects)
							throws SQLException {
						/**
						 * way points for a way specified by its source and destination vertices
						 * 
						 * SQL Parameter Expectations: <br/>
						 * 1st is the source's ID <br/>
						 * 2nd is the destination's ID <br/>
						 * 
						 * SQL ResultSet Expectations: <br/>
						 * 1st column is the point's latitude <br/>
						 * 2nd column is the point's longitude <br/>
						 */
						if (!(objects[0] instanceof Integer)
								|| !(objects[1] instanceof Integer))
							throw new IllegalArgumentException(Issue.CALLBACK_TYPE_ASSERTION
									.msg());
						Integer srcId = (Integer) objects[0];
						Integer dstId = (Integer) objects[1];

						/** use PreparedStatement and execute query */
						PreparedStatement pStmt = con.prepareStatement(this.stmt);
						pStmt.setInt(1, srcId);
						pStmt.setInt(2, dstId);
						ResultSet rs = pStmt.executeQuery();

						/** read database results */
						List<Node> res = new ArrayList<Node>();
						while (rs.next()) {
							// TODO : map of attributes
							res.add(Node.newNode(rs.getInt(1), rs.getInt(2)));
						}
						// TODO: if way was provided backward: return backward
						// list of nodes
						return res;
					}
				});
		this.callbacks.put(SqlCommand.GET_POINT_FOR_VERTEX_ID, new ICallback<Point>() {

			private final String stmt = sqlConf.get(SqlCommand.GET_POINT_FOR_VERTEX_ID);

			@Override
			public Point callback(Connection con, Object... objects) throws SQLException {
				/**
				 * point for a specified vertex ID
				 * 
				 * SQL Parameter Expectations: <br/>
				 * 1st is the vertex ID <br/>
				 * 
				 * SQL ResultSet Expectations: <br/>
				 * 1st column is the vertex' latitude <br/>
				 * 2nd column is the vertex' longitude
				 */
				if (!(objects[0] instanceof Integer))
					throw new IllegalArgumentException(Issue.CALLBACK_TYPE_ASSERTION.msg());
				Integer vtxId = (Integer) objects[0];

				/** use PreparedStatement and execute query */
				PreparedStatement pStmt = con.prepareStatement(this.stmt);
				pStmt.setInt(1, vtxId);
				ResultSet rs = pStmt.executeQuery();

				/** read database results */
				rs.next();
				return Point.newInstance(rs.getInt(1), rs.getInt(2));
			}
		});
		this.callbacks.put(SqlCommand.GET_WAYPOINTS_FOR_POINT,
				new ICallback<Iterable<Point>>() {

					private final String stmt = sqlConf.get(SqlCommand.GET_WAYPOINTS_FOR_POINT);

					@Override
					public Iterable<Point> callback(Connection con, Object... objects)
							throws SQLException {
						/**
						 * way points for an arbitrary point, each way point belongs to a
						 * different way
						 * 
						 * TODO SQL Parameter Expectations: <br/>
						 * 1st is the point's latitude <br/>
						 * 2nd is the point's longitude <br/>
						 * 3rd is the number of result points requested <br/>
						 * 4th is the bounding box' lower left point's latitude <br/>
						 * 5th is the bounding box' lower left point's longitude <br/>
						 * 6th is the bounding box' upper right point's latitude <br/>
						 * 7th is the bounding box' upper right point's longitude <br/>
						 * 
						 * SQL ResultSet Expectations: <br/>
						 * 1st column is the way point's latitude <br/>
						 * 2nd column is the way point's longitude
						 */
						if (!(objects[0] instanceof Point) || !(objects[1] instanceof Integer)
								|| !(objects[2] instanceof BoundingBox))
							throw new IllegalArgumentException(Issue.CALLBACK_TYPE_ASSERTION
									.msg());
						Point arbitraryPosition = (Point) objects[0];
						Integer nPointsRequested = (Integer) objects[1];
						BoundingBox centralSearchArea = (BoundingBox) objects[2];

						List<Point> res = new ArrayList<Point>(nPointsRequested);

						/** do exponential search */
						int minLat = arbitraryPosition.getLat();
						int maxLat = arbitraryPosition.getLat();
						int minLon = arbitraryPosition.getLon();
						int maxLon = arbitraryPosition.getLon();
						int expand = 512;
						while (res.size() < nPointsRequested
								&& (centralSearchArea.getMinGeo().getLat() < minLat
										|| centralSearchArea.getMaxGeo().getLat() > maxLat
										|| centralSearchArea.getMinGeo().getLon() < minLon || centralSearchArea
										.getMaxGeo().getLon() > maxLon)) {
							// DEBUG ONLY:
							Router.LOGGER
									.info(String
											.format(
													SqlBackedGeoMap.class.getName()
															+ ": searching point (%d,%d)\t\twithin area (%d, %d),(%d, %d)",
													arbitraryPosition.getLat(),
													arbitraryPosition.getLon(), minLat, minLon,
													maxLat, maxLon));

							/** expand search area */
							minLat -= expand;
							maxLat += expand;
							minLon -= expand;
							maxLon += expand;
							expand *= 2;

							/** reset result set */
							res.clear();

							/** use PreparedStatement and execute query */
							PreparedStatement pStmt = con.prepareStatement(this.stmt);

							pStmt.setInt(1, arbitraryPosition.getLat());
							pStmt.setInt(2, arbitraryPosition.getLon());

							pStmt.setInt(3, minLat);
							pStmt.setInt(4, maxLat);
							pStmt.setInt(5, minLon);
							pStmt.setInt(6, maxLon);

							pStmt.setInt(7, arbitraryPosition.getLat());
							pStmt.setInt(8, arbitraryPosition.getLon());
							pStmt.setInt(9, nPointsRequested);

							pStmt.setFetchSize(nPointsRequested);
							ResultSet rs = pStmt.executeQuery();

							/** read database results */
							while (rs.next())
								res.add(Point.newInstance(rs.getInt(1), rs.getInt(2)));
						}

						return res;
					}
				});
	}

	@Override
	public VertexDistance[] getAdjacentVertices(Point wayPoint) throws IllegalArgumentException {
		try {
			return (VertexDistance[]) this.conHlr.handle(this.callbacks
					.get(SqlCommand.GET_VERTICES_FOR_WAYPOINT), wayPoint);
		} catch (ComponentInitializationException e) {
			/** write out the problem, but do not break down */
			e.printStackTrace();
			return EMPTY_VERTEXDISTANCE_ARRAY;
		}
	}

	@Override
	public final BoundingBox getBoundingBox() {
		return this.bbox;
	}

	@Override
	public final String getName() {
		return this.name;
	}

	@Override
	public List<Node> getNoneVertexNodes(int srcId, int dstId) {
		try {
			return (List<Node>) this.conHlr.handle(this.callbacks
					.get(SqlCommand.GET_INTERMEDIATE_NODES_FOR_WAY), srcId, dstId);
		} catch (ComponentInitializationException e) {
			/** write out the problem, but do not break down */
			e.printStackTrace();
			return Collections.EMPTY_LIST;
		}
	}

	@Override
	public final Date getReleaseDate() {
		/** Date is mutable */
		return (Date) this.date.clone();
	}

	@Override
	public Point getVertexPoint(int vtxId) {
		try {
			return (Point) this.conHlr.handle(this.callbacks
					.get(SqlCommand.GET_POINT_FOR_VERTEX_ID), vtxId);
		} catch (ComponentInitializationException e) {
			/** write out the problem, but do not break down */
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public final Point getWayPoint(Point arbitraryPosition) {
		return getWayPoints(arbitraryPosition, 1, this.bbox).iterator().next();
	}

	@Override
	public Iterable<Point> getWayPoints(Point arbitraryPosition, int nPointsRequested,
			BoundingBox centralSearchArea) {
		try {
			return (Iterable<Point>) this.conHlr.handle(this.callbacks
					.get(SqlCommand.GET_WAYPOINTS_FOR_POINT), arbitraryPosition,
					nPointsRequested, centralSearchArea);
		} catch (ComponentInitializationException e) {
			/** write out the problem, but do not break down */
			e.printStackTrace();
			return Collections.EMPTY_LIST;
		}
	}

	@Override
	public void write(DataOutput out) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node vertexNode(int vtxId) {
		// TODO: get attributes map
		Point p = this.getVertexPoint(vtxId);
		return Node.newNode(p.getLat(), p.getLon());
	}
}
