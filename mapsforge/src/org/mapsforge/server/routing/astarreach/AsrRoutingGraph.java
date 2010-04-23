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
package org.mapsforge.server.routing.astarreach;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.mapsforge.core.conf.IVehicle;
import org.mapsforge.server.routing.core.ComponentInitializationException;
import org.mapsforge.server.routing.core.ConnectionHandler;
import org.mapsforge.server.routing.core.IProperty;
import org.mapsforge.server.routing.core.Issue;
import org.mapsforge.server.routing.core.PropertyConfiguration;
import org.mapsforge.server.routing.core.ConnectionHandler.ICallback;

public class AsrRoutingGraph implements IAsrRoutingGraph, Serializable {

	public static enum SqlCommand implements IProperty {
		GET_EDGES, GET_RULES, GET_VERTICES,
	}

	private static class Rule {
		final int cost;
		final int dstId;
		final int pdcId;

		Rule(int pdcId, int dstId, int cost) {
			this.pdcId = pdcId;
			this.dstId = dstId;
			this.cost = cost;
		}
	}

	private static final int[] EMPTY_INT_ARRAY = new int[0];
	private static final String EMPTY_STRING = ""; //$NON-NLS-1$

	private static final long serialVersionUID = -9130994753234063579L;

	public static AsrRoutingGraph newInstance(PropertyConfiguration<?> propConf,
			ConnectionHandler conHlr, DataInput in, DataOutput out, IVehicle vehicle)
			throws ComponentInitializationException {

		AsrRoutingGraph res;

		if (in != null) {
			try {
				/** use DataInput; check UID first */
				if (serialVersionUID != in.readLong())
					throw new ComponentInitializationException(Issue.RG__BAD_DATAINPUT.msg());

				/** the number of vertices is essential to be known early */
				int nVertices = in.readInt();
				if (nVertices <= 2 || nVertices == Integer.MAX_VALUE)
					throw new ComponentInitializationException(Issue.RG__BAD_DATAINPUT.msg());

				int[] reachValues = new int[nVertices];
				for (int vtxId = 0; vtxId < nVertices; vtxId++) {
					reachValues[vtxId] = in.readInt();
					/** assert for correct reach values; 0 means none defined */
					if (reachValues[vtxId] == 0)
						reachValues[vtxId] = Integer.MAX_VALUE;
					else if (reachValues[vtxId] < 0)
						throw new ComponentInitializationException(Issue.RG__BAD_DATAINPUT
								.msg());
				}

				int nEdges = 0;
				int[][] edgeDsts = new int[nVertices][];
				int[][] edgeCosts = new int[nVertices][];
				for (int vtxId = 0; vtxId < nVertices; vtxId++) {
					int nEdgesOut = in.readInt();
					nEdges += nEdgesOut;
					if (nEdgesOut == 0) {
						/**
						 * vertices having no out edges get assigned the UNMODIFIABLE empty
						 * array
						 */
						edgeDsts[vtxId] = EMPTY_INT_ARRAY;
						edgeCosts[vtxId] = EMPTY_INT_ARRAY;
						continue;
					}
					if (nEdgesOut < 0)
						throw new ComponentInitializationException(Issue.RG__BAD_DATAINPUT
								.msg());
					edgeDsts[vtxId] = new int[nEdgesOut];
					edgeCosts[vtxId] = new int[nEdgesOut];
					for (int dstId = 0; dstId < nEdgesOut; dstId++) {
						edgeDsts[vtxId][dstId] = in.readInt();
						edgeCosts[vtxId][dstId] = in.readInt();
					}
				}

				TIntObjectHashMap<LinkedList<Rule>> ruleCosts = new TIntObjectHashMap<LinkedList<Rule>>();
				/** retrieve the rules one by one */
				int lastSrcId = -1;
				int srcId = -1;
				int pdcId = -1;
				int dstId = -1;
				int cost = -1;
				boolean readCmplt = false;
				LinkedList<Rule> rList = new LinkedList<Rule>();
				while (!readCmplt) {
					lastSrcId = srcId;

					readCmplt = true;
					try {
						srcId = in.readInt();
						readCmplt = false;
						pdcId = in.readInt();
						dstId = in.readInt();
						cost = in.readInt();
					} catch (EOFException exc) {
						if (!readCmplt)
							throw new ComponentInitializationException(Issue.RG__BAD_DATAINPUT
									.msg(), exc);
						break;
					}

					// TODO assertion of values read

					if (lastSrcId != srcId && lastSrcId != -1) {
						/**
						 * begin a new list after putting the old one into the hashmap
						 */
						ruleCosts.put(lastSrcId, rList);
						rList = new LinkedList<Rule>();
					}
					rList.add(new Rule(pdcId, dstId, cost));
				}
				/** the last rules list hasn't been inserted, so do it now */
				ruleCosts.put(lastSrcId, rList);

				/** create routing graph from the data read */
				res = new AsrRoutingGraph(nVertices, reachValues, nEdges, edgeDsts, edgeCosts,
						ruleCosts);

			} catch (IOException exc) {
				throw new ComponentInitializationException(Issue.RG__BAD_DATAINPUT.msg(), exc);
			}
		} else {

			/** use SQL database, so read query statements to be used first */
			final PropertyConfiguration<SqlCommand> sqlConf = PropertyConfiguration
					.newInstance(propConf, SqlCommand.class);

			res = conHlr.handle(new ICallback<AsrRoutingGraph>() {
				@Override
				public AsrRoutingGraph callback(Connection con, Object... objects)
						throws SQLException {
					/**
					 * get vertex properties (reach values, etc.)
					 * 
					 * SQL Parameter Expectations: <br/>
					 * none. <br/>
					 * 
					 * SQL ResultSet Expectations: <br/>
					 * 1st column is the vertex' ID <br/>
					 * 2nd column is the vertex' reach value
					 */
					PreparedStatement stmt = con.prepareStatement(sqlConf
							.get(SqlCommand.GET_VERTICES));
					ResultSet rs = stmt.executeQuery();
					// /** get number of vertices in graph and jump back */
					// rs.last();
					// final int nVertices = rs.getRow() + 1;
					// rs.beforeFirst();
					/** get vertices now */
					TIntIntHashMap reachValuesMap = new TIntIntHashMap();
					while (rs.next()) {
						int vtxId = rs.getInt(1);
						if (rs.wasNull())
							/** jump over bad rows */
							continue;
						int reachValue = rs.getInt(2);
						if (rs.wasNull())
							/** jump over bad rows */
							continue;
						reachValuesMap.put(vtxId, reachValue);
					}
					int nVertices = reachValuesMap.size();
					int[] reachValues = new int[nVertices];
					/** make int[] from reach values */
					for (int vtxId = 0; vtxId < nVertices; vtxId++) {
						/** non-existing keys result in 0 */
						int reachValue = reachValuesMap.get(vtxId);
						if (reachValue < 0)
							throw new SQLException(Issue.RG__BAD_DATAINPUT.msg());
						if (reachValue == 0)
							reachValue = Integer.MAX_VALUE;
						reachValues[vtxId] = reachValue;
					}

					/**
					 * get edge properties (destinations, costs, etc.)
					 * 
					 * SQL Parameter Expectations: <br/>
					 * none
					 * 
					 * SQL ResultSet Expectations: <br/>
					 * 1st column is the edge's source vertex ID <br/>
					 * 2nd column is the edge's destination ID <br/>
					 * 3rd column is the edge's cost
					 */
					stmt = con.prepareStatement(sqlConf.get(SqlCommand.GET_EDGES));
					stmt.setFetchSize(nVertices * 3);
					int[][] edgeDsts = new int[nVertices][];
					int[][] edgeCosts = new int[nVertices][];
					int nEdges = 0;
					TIntArrayList edgeDstsList = new TIntArrayList();
					TIntArrayList edgeCostsList = new TIntArrayList();
					/** retrieve the edges all at once */
					rs = stmt.executeQuery();
					int recentId = -1;
					while (rs.next()) {
						int vtxId = rs.getInt(1);
						if (vtxId != recentId) {
							if (recentId >= 0) {
								edgeDsts[recentId] = edgeDstsList.toNativeArray();
								edgeCosts[recentId] = edgeCostsList.toNativeArray();
							}
							edgeDstsList = new TIntArrayList();
							edgeCostsList = new TIntArrayList();
							recentId = vtxId;
						}
						edgeDstsList.add(rs.getInt(2));
						edgeCostsList.add(rs.getInt(3));
						nEdges++;
					}
					if (recentId >= 0) {
						edgeDsts[recentId] = edgeDstsList.toNativeArray();
						edgeCosts[recentId] = edgeCostsList.toNativeArray();
					}

					/**
					 * get rules
					 * 
					 * SQL Parameter Expectations: <br/>
					 * none
					 * 
					 * SQL ResultSet Expectations: <br/>
					 * 1st column is the edge's predecessor vertex ID <br/>
					 * 2nd column is the edge's source vertex ID <br/>
					 * 3rd column is the edge's destination ID <br/>
					 * 4th column is the edge's cost
					 */
					String sql = sqlConf.get(SqlCommand.GET_RULES);
					stmt = con.prepareStatement(sql);
					stmt.setFetchSize(nEdges);
					TIntObjectHashMap<LinkedList<Rule>> ruleCosts = new TIntObjectHashMap<LinkedList<Rule>>();
					/** retrieve the rules all at once */
					if (sql != null && !sql.equals(EMPTY_STRING)) {
						rs = stmt.executeQuery();
						while (rs.next()) {
							int pdcId = rs.getInt(1);
							int srcId = rs.getInt(2);
							int dstId = rs.getInt(3);
							int cost = rs.getInt(4);
							LinkedList<Rule> rList = ruleCosts.get(srcId);
							if (rList == null) {
								rList = new LinkedList<Rule>();
								ruleCosts.put(srcId, rList);
							}
							rList.add(new Rule(pdcId, dstId, cost));
						}
					}

					/** create routing graph from the data read */
					return new AsrRoutingGraph(nVertices, reachValues, nEdges, edgeDsts,
							edgeCosts, ruleCosts);
				}
			});
		}
		if (out != null)
			try {
				res.write(out);
			} catch (IOException exc) {
				throw new ComponentInitializationException(Issue.RG__BAD_DATAOUTPUT.msg(), exc);
			}

		return res;
	}

	/** edges: [srcId][outNo] */
	private final int[][] edgeCosts;

	/** edges: [srcId][outNo] */
	private final int[][] edgeDsts;

	/** edges: [srcId][outNo] */
	private final int[][] edgeSrcs;

	private final long nEdges;

	private final int nVertices;

	private final int[] reachValues;
	/** srcId->List<(pdcId,dstId,cost)> */
	private final TIntObjectHashMap<LinkedList<Rule>> ruleCosts;

	private AsrRoutingGraph(int nVertices, int[] reachValues, int nEdges, int[][] edgeDsts,
			int[][] edgeCosts, TIntObjectHashMap<LinkedList<Rule>> ruleCosts) {
		this.nVertices = nVertices;
		this.reachValues = reachValues;
		this.nEdges = nEdges;
		this.edgeDsts = edgeDsts;
		this.edgeCosts = edgeCosts;

		/** create EMPTY arrays for the sources to each edge */
		this.edgeSrcs = new int[nVertices][0];

		for (int srcId = 0; srcId < nVertices; srcId++)
			if (edgeDsts[srcId] != null)
				for (int dstNo = 0; dstNo < edgeDsts[srcId].length; dstNo++) {
					int dstId = edgeDsts[srcId][dstNo];
					/** create array of length + 1 */
					int srcNo = this.edgeSrcs[dstId].length;
					this.edgeSrcs[dstId] = Arrays.copyOf(this.edgeSrcs[dstId], srcNo + 1);
					/** put srcId in there as new last element */
					this.edgeSrcs[dstId][srcNo] = srcId;
					/**
					 * sort (providing possibility of faster processing of queries)
					 */
					Arrays.sort(this.edgeSrcs[dstId]);
				}

		this.ruleCosts = ruleCosts;
	}

	@Override
	public int getEdgeCost(int srcId, int dstId) {
		/** find edge number and return corresponding cost, if that edge exists */

		for (int edgeNo = 0; edgeNo < this.edgeDsts[srcId].length; edgeNo++)
			if (this.edgeDsts[srcId][edgeNo] == dstId)
				return this.edgeCosts[srcId][edgeNo];

		/** ... or return INFINITE_COST otherwise */
		return INFINITE_COST;
	}

	@Override
	public int[] getInNeighbors(int vertexId) {
		if (this.edgeSrcs[vertexId] == null)
			return EMPTY_INT_ARRAY;
		return this.edgeSrcs[vertexId].clone();
	}

	@Override
	public long getNEdges() {
		return this.nEdges;
	}

	@Override
	public int getNVertices() {
		return this.nVertices;
	}

	@Override
	public int[] getOutNeighbors(int vertexId) {
		if (this.edgeDsts[vertexId] == null)
			return EMPTY_INT_ARRAY;
		return this.edgeDsts[vertexId].clone();
	}

	@Override
	public int getReachValue(int vtxId) {
		return this.reachValues[vtxId];
	}

	@Override
	public int getRuleCost(int pdcId, int srcId, int dstId) {
		List<Rule> rules = this.ruleCosts.get(srcId);
		if (rules != null)
			for (Rule rule : rules)
				if (rule.pdcId == pdcId && rule.dstId == dstId)
					return rule.cost;
		return 0;
	}

	@Override
	public boolean isBidirectional(int sourceId, int destinationId) {
		return Arrays.binarySearch(this.edgeDsts[sourceId], destinationId) >= 0
				&& Arrays.binarySearch(this.edgeDsts[destinationId], sourceId) >= 0;
	}

	@Override
	public void write(DataOutput out) throws IOException {
		// TODO Auto-generated method stub
	}
}
