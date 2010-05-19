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

import gnu.trove.TIntArrayList;
import gnu.trove.TIntObjectHashMap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Stack;

import org.mapsforge.server.core.geoinfo.BoundingBox;
import org.mapsforge.server.core.geoinfo.Node;
import org.mapsforge.server.core.geoinfo.Point;
import org.mapsforge.server.routing.core.ConnectionHandler.ICallback;

public final class ArrayBasedGeoMap implements IGeoMap, Serializable {

	public static final class Builder {
		private Date date;
		private String name;
		private TIntArrayList srcList = new TIntArrayList();
		private TIntArrayList dstList = new TIntArrayList();
		private TIntArrayList latList = new TIntArrayList();
		private TIntArrayList lonList = new TIntArrayList();
		private TIntArrayList vertexLatList = new TIntArrayList();
		private TIntArrayList vertexLonList = new TIntArrayList();
		private TIntObjectHashMap<TIntObjectHashMap<int[][]>> wayImNodes = new TIntObjectHashMap<TIntObjectHashMap<int[][]>>();

		/**
		 * Implicitly adds the corresponding way point, too.
		 * 
		 * @param id
		 * @param latitude
		 * @param longitude
		 */
		public void addVertex(int id, int latitude, int longitude) {
			// TODO: arbitrary IDs allowed
			assert id == this.vertexLatList.size();
			this.vertexLatList.add(latitude);
			this.vertexLonList.add(longitude);
			/**
			 * add way point using vertex ID as source and destination of the way, which is
			 * legitimated by the fact that self-loops are discouraged by definition of routing
			 * graphs
			 */
			addWayPoint(id, id, latitude, longitude);
		}

		public void addWayPoint(int srcId, int dstId, int latitude, int longitude) {
			this.srcList.add(srcId);
			this.dstList.add(dstId);
			this.latList.add(latitude);
			this.lonList.add(longitude);
		}

		public ArrayBasedGeoMap newInstance() {
			if (this.name == null || this.date == null)
				throw new IllegalStateException();
			// TODO: assert other values
			return new ArrayBasedGeoMap(
					this.name, 
					this.date, 
					this.latList.toNativeArray(),
					this.lonList.toNativeArray(), 
					this.srcList.toNativeArray(), 
					this.dstList.toNativeArray(), 
					this.vertexLatList.toNativeArray(),
					this.vertexLonList.toNativeArray(), 
					this.wayImNodes
				);
		}

		public void putWayImNodes(int srcId, int dstId, TIntArrayList[] edgeNodesList) {
			if (dstId < srcId) {
				/** reverse nodes order direction */
				edgeNodesList[0].reverse();
				edgeNodesList[1].reverse();
				/** exchange src and dst vertex IDs */
				int h = srcId;
				srcId = dstId;
				dstId = h;
			}
			TIntObjectHashMap<int[][]> nodesMap = this.wayImNodes.get(srcId);
			if (nodesMap == null) {
				/** add new map for that vertex */
				nodesMap = new TIntObjectHashMap<int[][]>(1);
				this.wayImNodes.put(srcId, nodesMap);
			}
			/** now add way in forward direction */
			this.wayImNodes.get(srcId).put(dstId, new int[][] { edgeNodesList[0].toNativeArray(), edgeNodesList[1].toNativeArray() });
			for (int i = 0; i < edgeNodesList[0].size(); i++) {
				addWayPoint(srcId, dstId, edgeNodesList[0].get(i), edgeNodesList[1].get(i));
			}
		}

		public void setDate(Date date) {
			this.date = date;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public enum SqlCommand implements IProperty {
		GET_INTERMEDIATE_NODES_FOR_ALL_WAYS, GET_POINTS_FOR_ALL_VERTICES,
	}

	private class IndexEntry {
		final int idxFrom;
		final int idxTo;
		final int latMax;
		final int latMin;
		final int lonMax;
		final int lonMin;
		final IndexEntry one;
		final IndexEntry two;

		IndexEntry(int idxFrom, int idxTo, int latMin, int latMax, int lonMin, int lonMax,
				IndexEntry left, IndexEntry right) {
			this.idxFrom = idxFrom;
			this.idxTo = idxTo;
			this.latMin = latMin;
			this.latMax = latMax;
			this.lonMin = lonMin;
			this.lonMax = lonMax;
			this.one = left;
			this.two = right;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder('(');
			sb.append(this.idxFrom);
			sb.append(':');
			sb.append(this.idxTo);
			sb.append(';');
			sb.append(BoundingBox.getInstance(this.latMin, this.lonMin, this.latMax,
					this.lonMax));
			sb.append(')');
			return sb.toString();
		}

		BoundingBox boundingBox() {
			return BoundingBox.getInstance(this.latMin, this.lonMin, this.latMax, this.lonMax);
		}

		private List<IndexEntry> overlappingBlocks(BoundingBox bb) {
			/**
			 * create List of all blocks which have BoundingBoxes overlapping with the
			 * BoundingBox found for the point
			 */
			List<IndexEntry> res = new ArrayList<IndexEntry>();
			res.add(this);
			for (int i = 0; i < res.size(); i++) {
				IndexEntry e = res.get(i);
				if (e.one != null && bb.overlaps(e.one.boundingBox()))
					res.add(e.one);
				if (e.two != null && bb.overlaps(e.two.boundingBox()))
					res.add(e.two);
				if (e.one != null || e.two != null) {
					res.remove(i);
					i--;
				}
			}
			return res;
		}
	}

	private class PointMap {

		final int[] lat;

		private final int[] dst;

		private final int[] lon;

		private final Random rnd = new Random();

		private final int[] src;

		private final int[] vtxLats;

		private final int[] vtxLons;

		/**
		 * HashMap: srcId <br/>
		 * HashMap: dstId <br/>
		 * int[][]: 0=lats/1=lons <br/>
		 * int[]: lat / lon
		 */
		private final TIntObjectHashMap<TIntObjectHashMap<int[][]>> wayImNodes;

		PointMap(int[] lat, int[] lon, int[] src, int[] dst, int[] vtxLats, int[] vtxLons,
				TIntObjectHashMap<TIntObjectHashMap<int[][]>> wayImNodes) {
			this.lat = lat;
			this.lon = lon;
			this.src = src;
			this.dst = dst;
			this.vtxLats = vtxLats;
			this.vtxLons = vtxLons;
			this.wayImNodes = wayImNodes;

			createBlockStructure();
		}

		void createBlockStructure() {
			/**
			 * create block structure: alternating sort order by latitude / longitude
			 */
			Stack<Integer> starts = new Stack<Integer>();
			Stack<Integer> stops = new Stack<Integer>();
			Stack<Boolean> sortOrderLats = new Stack<Boolean>();

			starts.push(0);
			stops.push(this.lat.length);
			sortOrderLats.push(true);

			while (!starts.empty()) {
				int startPos = starts.pop();
				int stopPos = stops.pop();
				boolean byLatitudeFirst = sortOrderLats.pop();

				int size = stopPos - startPos;
				int nBlocks = nBlocks(size);
				int splitPos = startPos + (1 << largestPowerOfKInN(2, nBlocks - 1)) * BLOCKSIZE;

				assert nBlocks >= 1;

				if (nBlocks == 1) {
					/**
					 * stop recursion at block size level, sort each block by latitude first
					 */
					sort(startPos, stopPos, true);
				} else {
					/** split this part and do "recursion" on both sub parts */
					split(startPos, stopPos, byLatitudeFirst, splitPos);

					starts.push(startPos);
					stops.push(splitPos);
					sortOrderLats.push(!byLatitudeFirst);

					starts.push(splitPos);
					stops.push(stopPos);
					sortOrderLats.push(!byLatitudeFirst);
				}
			}
		}

		int partition(int start, int stop, boolean byLatitudeFirst) {
			/** randomized QuickSort randomly changes first element */
			swap(this.rnd.nextInt(stop - start) + start, start);
			/** now the standard QuickSort procedure */
			int splitPos = start;
			for (int pos = start + 1; pos < stop; pos++)
				if (larger(byLatitudeFirst, start, pos))
					swap(++splitPos, pos);
			swap(start, splitPos);
			return splitPos;
		}

		private boolean larger(boolean byLatitudeFirst, int pos1, int pos2) {
			return byLatitudeFirst
					&& (this.lat[pos1] > this.lat[pos2] || this.lat[pos1] == this.lat[pos2]
							&& this.lon[pos1] >= this.lon[pos2])
					|| !byLatitudeFirst
					&& (this.lon[pos1] > this.lon[pos2] || this.lon[pos1] == this.lon[pos2]
							&& this.lat[pos1] >= this.lat[pos2]);
		}

		private BoundingBox minRect(Point point, IndexEntry e) {
			int minDist = Integer.MAX_VALUE;
			for (int i = e.idxFrom; i < e.idxTo; i++) {
				minDist = Math.min(minDist, point.distanceTo(this.lat[i], this.lon[i]));
			}
			return BoundingBox.getInstance(point, minDist);
		}

		/**
		 * A QuickSort implementation.
		 * 
		 * @param start
		 * @param stop
		 * @param byLatitudeFirst
		 */
		private void sort(int start, int stop, boolean byLatitudeFirst) {
			if (start >= stop - 1)
				return;
			int splitPos = partition(start, stop, byLatitudeFirst);
			sort(start, splitPos, byLatitudeFirst);
			sort(splitPos + 1, stop, byLatitudeFirst);
		}

		/**
		 * A QuickSort implementation which stops sorting when the position searched was
		 * reached.
		 * 
		 * @param start
		 * @param stop
		 * @param byLatitudeFirst
		 * @param searchPos
		 *            the position where the recursion stops if it is outside the start to stop
		 *            range.
		 */
		private void split(int start, int stop, boolean byLatitudeFirst, int searchPos) {
			if (start >= stop - 1 || start > searchPos || stop < searchPos)
				return;
			int splitPos = partition(start, stop, byLatitudeFirst);
			split(start, splitPos, byLatitudeFirst, searchPos);
			split(splitPos + 1, stop, byLatitudeFirst, searchPos);
		}

		private void swap(int pos1, int pos2) {
			if (pos1 == pos2)
				return;

			/** swap lat */
			int h = this.lat[pos1];
			this.lat[pos1] = this.lat[pos2];
			this.lat[pos2] = h;

			/** swap lon */
			h = this.lon[pos1];
			this.lon[pos1] = this.lon[pos2];
			this.lon[pos2] = h;

			/** swap src */
			h = this.src[pos1];
			this.src[pos1] = this.src[pos2];
			this.src[pos2] = h;

			/** swap dst */
			h = this.dst[pos1];
			this.dst[pos1] = this.dst[pos2];
			this.dst[pos2] = h;
		}
	}

	private static final int BLOCKSIZE = 256;

	private static final long serialVersionUID = 1854164642830114731L;

	public static final ArrayBasedGeoMap newInstance(final String name, final Date date,
			ConnectionHandler conHlr, PropertyConfiguration<?> anyConf, DataInput in,
			DataOutput out, final IRoutingGraph graph) throws ComponentInitializationException {
		final PropertyConfiguration<SqlCommand> sqlConf = PropertyConfiguration.newInstance(
				anyConf, SqlCommand.class);

		/** construct latitude/longitude arrays for vertices */
		final int nVertices = graph.getNVertices();

		ArrayBasedGeoMap res = null;
		if (in == null) {
			/**
			 * read from SQL database: <br/>
			 * use the ConnectionHandler for retrieving all vertex coordinates
			 */
			res = conHlr.handle(new ICallback<ArrayBasedGeoMap>() {
				@Override
				public ArrayBasedGeoMap callback(Connection con, Object... objects)
						throws SQLException {
					Builder pmBuilder = new Builder();
					pmBuilder.setName(name);
					pmBuilder.setDate(date);

					/**
					 * get points for all vertices
					 * 
					 * SQL Parameter Expectations: <br/>
					 * none
					 * 
					 * SQL ResultSet Expectations: <br/>
					 * 1st column is the vertex ID <br/>
					 * 2nd column is the vertex latitude <br/>
					 * 3rd column is the vertex longitude
					 */
					PreparedStatement stmt = con.prepareStatement(sqlConf
							.get(SqlCommand.GET_POINTS_FOR_ALL_VERTICES));
					stmt.setFetchSize(nVertices);
					/** retrieve the vertices all at once */
					ResultSet rs = stmt.executeQuery();
					while (rs.next()) {
						int vtxId = rs.getInt(1);
						int lat = rs.getInt(2);
						int lon = rs.getInt(3);
						/** add vertex (and way point implicitly) */
						pmBuilder.addVertex(vtxId, lat, lon);
					}

					/**
					 * get nodes for all ways
					 * 
					 * SQL Parameter Expectations: <br/>
					 * none
					 * 
					 * SQL ResultSet Expectations: <br/>
					 * 1st column is the edge's source vertex ID <br/>
					 * 2nd column is the edge's destination ID <br/>
					 * 3rd column is the node's latitude <br/>
					 * 4th column is the node's longitude <br/>
					 * the ResultSet is ordered by column 1 and 2 and the node's sequence number
					 * within the edge <br/>
					 */
					stmt = con.prepareStatement(sqlConf
							.get(SqlCommand.GET_INTERMEDIATE_NODES_FOR_ALL_WAYS));
					stmt.setFetchSize(nVertices);
					/** retrieve the edges all at once */
					rs = stmt.executeQuery();
					TIntArrayList[] edgeNodesList = new TIntArrayList[] {
							new TIntArrayList(),
							new TIntArrayList() 
						};
					int recentSrcId = -1;
					int recentDstId = -1;
					while (rs.next()) {
						int srcId = rs.getInt(1);
						int dstId = rs.getInt(2);
						int lat = rs.getInt(3);
						int lon = rs.getInt(4);
						// TODO : assert values read !!

						if (recentSrcId != -1 && (dstId != recentDstId || srcId != recentSrcId)) {
							/** add way to recent vertex' map */
							pmBuilder.putWayImNodes(recentSrcId, recentDstId, edgeNodesList);

							/** reset the lat/lon lists */
							edgeNodesList[0].clear();
							edgeNodesList[1].clear();
						}
						/**
						 * alternately add lat and lon, so lats get positions 0 mod 2 and lons 1
						 * mod 2
						 */
						edgeNodesList[0].add(lat);
						edgeNodesList[1].add(lon);
						recentSrcId = srcId;
						recentDstId = dstId;
					}
					if (recentSrcId != 0) {
						/** put last way read into map */
						pmBuilder.putWayImNodes(recentSrcId, recentDstId, edgeNodesList);
					}

					/** build PointMap from the points given to the Builder */
					return pmBuilder.newInstance();
				}
			});
		} else {
			/** read from DataInput */
			try {
				// TODO: read
				/** use DataInput; check UID first */
				if (serialVersionUID != in.readLong())
					throw new ComponentInitializationException(Issue.GM__BAD_DATAINPUT.msg());
				// pmBuilder.setName(name);
				// pmBuilder.setDate(date);

				/** get number of vertices in file and assert it */
				if (nVertices != in.readInt())
					throw new IOException();

				/** read the vertices' geographical coordinates */
				for (int vtxId = 0; vtxId < nVertices; vtxId++) {
					// TODO: read
				}

				/** read the ways intermediate nodes */
				// TODO: read

			} catch (IOException exc) {
				throw new ComponentInitializationException(Messages
						.getString(ArrayBasedGeoMap.class.getCanonicalName() + ".1"), exc); //$NON-NLS-1$
			}
		}

		if (res == null)
			throw new IllegalStateException();

		/** if an output filename was specified, use it now */
		if (out != null)
			try {
				res.write(out);
			} catch (IOException exc) {
				throw new ComponentInitializationException(
						Issue.GM__DATAOUTPUT_NOT_CONFGD_PRPLY.msg(), exc);
			}

		return res;
	}

	private static int largestPowerOfKInN(int k, long n) {
		assert n > 0;
		for (int p = 0; p < 63; p++) {
			if (Math.pow(k, p + 1) > n)
				return p;
		}
		throw new AssertionError();
	}

	private static int max(int[] arr, int start, int end) {
		assert end < arr.length;
		assert start >= 0;

		int res = Integer.MIN_VALUE;
		for (int pos = start; pos < end; pos++)
			if (res < arr[pos])
				res = arr[pos];
		return res;
	}

	private static int min(int[] arr, int start, int end) {
		assert end < arr.length;
		assert start >= 0;

		int res = Integer.MAX_VALUE;
		for (int pos = start; pos < end; pos++)
			if (res > arr[pos])
				res = arr[pos];
		return res;
	}

	private static int nBlocks(int length) {
		return length / BLOCKSIZE + (length % BLOCKSIZE == 0 ? 0 : 1);
	}

	private final Date date;

	private final IndexEntry index;

	private final String name;

	private final PointMap pointMap;

	private ArrayBasedGeoMap(String name, Date date, int[] lat, int[] lon, int[] src,
			int[] dst, int[] vtxLats, int[] vtxLons,
			TIntObjectHashMap<TIntObjectHashMap<int[][]>> wayImNodes) {
		this.name = name;
		this.date = date;
		this.pointMap = new PointMap(lat, lon, src, dst, vtxLats, vtxLons, wayImNodes);
		this.index = createIndex(this.pointMap);
	}

	@Override
	public VertexDistance[] getAdjacentVertices(Point wayPoint) throws IllegalArgumentException {
		/** get corresponding index entry to point */
		IndexEntry e = indexEntryOf(wayPoint);

		if (e.idxTo - e.idxFrom > BLOCKSIZE)
			throw new IllegalArgumentException("wayPoint illegal"); // TODO

		/** try to find way point index by linear search within block */
		// TODO: binary search
		int i = e.idxFrom;
		while (i < e.idxTo) {
			if (this.pointMap.lat[i] == wayPoint.getLat()
					&& this.pointMap.lon[i] == wayPoint.getLon())
				break;
			i++;
		}

		if (i == e.idxTo)
			throw new IllegalArgumentException("wayPoint illegal"); // TODO

		/** index of way point was found, construct result */
		if (this.pointMap.src[i] == this.pointMap.dst[i])
			/** way point is a vertex, and distance zero */
			return new VertexDistance[] { new VertexDistance(this.pointMap.src[i], 0) };

		/**
		 * TODO : change distance calc. to usage of internal distance structure, which uses all
		 * intermediate waypoints to determine distances
		 */
		return new VertexDistance[] {
				new VertexDistance(this.pointMap.src[i], getVertexPoint(this.pointMap.src[i])
						.distanceTo(wayPoint)),
				new VertexDistance(this.pointMap.dst[i], getVertexPoint(this.pointMap.dst[i])
						.distanceTo(wayPoint)) };
	}

	@Override
	public BoundingBox getBoundingBox() {
		return this.index.boundingBox();
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Date getReleaseDate() {
		return this.date;
	}

	@Override
	public Point getVertexPoint(int vtxId) {
		return Point.newInstance(this.pointMap.vtxLats[vtxId], this.pointMap.vtxLons[vtxId]);
	}

	@Override
	public Point getWayPoint(Point point) {
		/** find a block matching the point very good */
		IndexEntry nearestBlock = nearestBlock(point);

		/**
		 * bounding box around the point using the minimal distance to a point within the block
		 * found
		 */
		BoundingBox searchArea = this.pointMap.minRect(point, nearestBlock);

		/** get the blocks to search in */
		List<IndexEntry> overlappingBlocks = this.index.overlappingBlocks(searchArea);

		/**
		 * process the List and compute ALL distances to way points in blocks of the list
		 */
		int[] minDist = null;
		for (IndexEntry e : overlappingBlocks)
			for (int i = e.idxFrom; i < e.idxTo; i++) {
				/** calculate values to be stored */
				int distance = point.distanceTo(this.pointMap.lat[i], this.pointMap.lon[i]);

				/** decide which value to keep */
				if (minDist == null || distance < minDist[0])
					/** keep new value, replace old one if exists */
					minDist = new int[] { distance, i };
			}

		if (minDist == null)
			throw new IllegalStateException();

		return Point.newInstance(this.pointMap.lat[minDist[1]], this.pointMap.lon[minDist[1]]);
	}

	@Override
	public List<Point> getWayPoints(Point point, int nPointsRequested,
			BoundingBox designatedSearchArea) {
		/** find a block matching the point very good */
		IndexEntry nearestBlock = nearestBlock(point);

		/**
		 * bounding box around the point using the minimal distance to a point within the block
		 * found
		 */
		BoundingBox searchArea = this.pointMap.minRect(point, nearestBlock);

		/** apply designated search area if possible */
		if (designatedSearchArea != null) {
			searchArea = searchArea.intersect(designatedSearchArea);
			if (searchArea == null)
				searchArea = designatedSearchArea;
		}

		/** get the blocks to search in */
		List<IndexEntry> overlappingBlocks = this.index.overlappingBlocks(searchArea);

		/**
		 * process the List and compute ALL distances to way points in blocks of the list
		 */
		TIntObjectHashMap<TIntObjectHashMap<int[]>> wayDists = new TIntObjectHashMap<TIntObjectHashMap<int[]>>(
				overlappingBlocks.size() * BLOCKSIZE / 2);
		for (IndexEntry e : overlappingBlocks)
			for (int i = e.idxFrom; i < e.idxTo; i++) {
				/** calculate values to be stored */
				final int distance = point.distanceTo(this.pointMap.lat[i],
						this.pointMap.lon[i]);

				/** get values stored previously */
				TIntObjectHashMap<int[]> dstMap = wayDists.get(this.pointMap.src[i]);
				if (dstMap == null) {
					dstMap = new TIntObjectHashMap<int[]>(3);
					wayDists.put(this.pointMap.src[i], dstMap);
				}
				int[] distEntry = dstMap.get(this.pointMap.dst[i]);

				/** decide which values to keep */
				if (distEntry == null || distance < distEntry[0])
					/** keep new value, replace old one if exists */
					dstMap.put(this.pointMap.dst[i], new int[] { distance, i });
			}

		/** select the part of the results to be returned */
		PriorityQueue<int[]> resQueue = new PriorityQueue<int[]>(nPointsRequested,
				new Comparator<int[]>() {
					@Override
					public int compare(int[] o1, int[] o2) {
						return Integer.valueOf(o1[0]).compareTo(o2[0]);
					}
				});
		for (Object o : wayDists.getValues()) {
			TIntObjectHashMap<int[]> map = (TIntObjectHashMap<int[]>) o;
			for (Object entry : map.getValues())
				resQueue.add((int[]) entry);
		}

		/** build a List of the queued points */
		List<Point> res = new LinkedList<Point>();
		for (int i = 0; i < nPointsRequested; i++) {
			int[] p = resQueue.poll();
			res.add(Point.newInstance(this.pointMap.lat[p[1]], this.pointMap.lon[p[1]]));
		}

		return res;
	}

	@Override
	public List<Node> getNoneVertexNodes(int srcId, int dstId) {
		// TODO : replace HashMap structure by better implementation

		/** determine way's direction */
		boolean backwards = false;
		if (dstId < srcId) {
			backwards = true;
			/** change src and dst */
			int h = srcId;
			srcId = dstId;
			dstId = h;
		}
		/** construct FORWARD way */
		List<Node> res = new LinkedList<Node>();
		TIntObjectHashMap<int[][]> imNodesMap = this.pointMap.wayImNodes.get(srcId);
		if (imNodesMap != null) {
			int[][] imNodes = imNodesMap.get(dstId);
			if (imNodes != null)
				for (int n = 0; n < imNodes[0].length; n++) {
					res.add(Node.newNode(imNodes[0][n], imNodes[1][n]));
				}
		}
		/** change list direction if constructed backwards */
		if (backwards)
			Collections.reverse(res);
		return res;
	}

	public int size() {
		return this.pointMap.lat.length;
	}

	@Override
	public Node vertexNode(int vtxId) {
		// TODO: Eike, insert meaningful attributes!
		//HashMap<String, String> attributes = new HashMap<String, String>();
		//attributes.put("node-id", Integer.toString(vtxId));
		Node returnNode = Node.newNode(this.pointMap.vtxLats[vtxId], this.pointMap.vtxLons[vtxId]);
		returnNode.setId(vtxId);
		return returnNode;
	}

	@Override
	public void write(DataOutput out) throws IOException, UnsupportedOperationException {
		// TODO do writing
	}

	private IndexEntry createIndex(PointMap pm) {
		/** create index over blocks */
		List<IndexEntry> blockEntries = new ArrayList<IndexEntry>(nBlocks());
		for (int block = 0; block < nBlocks(); block++) {
			int from = block * BLOCKSIZE;
			int to = Math.min(size(), from + BLOCKSIZE);
			blockEntries.add(block, new IndexEntry(from, to, min(pm.lat, from, to), max(pm.lat,
					from, to), min(pm.lon, from, to), max(pm.lon, from, to), null, null));
		}

		/** consolidate entries as long as possible */
		while (blockEntries.size() > 1) {
			for (int i = 0; i + 1 < blockEntries.size(); i++) {
				IndexEntry one = blockEntries.remove(i);
				IndexEntry two = blockEntries.remove(i);

				assert one.idxTo == two.idxFrom;

				blockEntries.add(i, new IndexEntry(one.idxFrom, two.idxTo, Math.min(one.latMin,
						two.latMin), Math.max(one.latMax, two.latMax), Math.min(one.lonMin,
						two.lonMin), Math.max(one.lonMax, two.lonMax), one, two));
			}
		}
		return blockEntries.get(0);
	}

	private IndexEntry indexEntryOf(Point point) {
		IndexEntry e = this.index;

		assert e != null;

		while (e.one != null || e.two != null) {
			/** as long as sub entries exist, set i to the one which matches */
			boolean a = e.one != null && e.one.boundingBox().surrounds(point);
			boolean b = e.two != null && e.two.boundingBox().surrounds(point);
			if (a && b || !a && !b)
				/** both branches are wrong or both match, so choose parent */
				break;
			else if (a && !b)
				/** choose branch one */
				e = e.one;
			else if (!a && b)
				/** choose second branch */
				e = e.two;
		}

		/** do final assertion */
		if (!e.boundingBox().surrounds(point))
			throw new IllegalArgumentException("Point not in area."); // TODO
		// message

		return e;
	}

	private int nBlocks() {
		return nBlocks(size());
	}

	private IndexEntry nearestBlock(Point point) {
		/**
		 * find the corresponding IndexEntry, the point does not lie within any sub IndexEntry
		 * by definition
		 */
		IndexEntry res = indexEntryOf(point);

		/**
		 * find the nearest IndexEntry for a block within e, this computation has no need for
		 * absolute correctness, choosing better blocks just speeds up computation
		 */
		boolean useOne, useTwo;
		while ((useOne = (res.one != null)) | (useTwo = (res.two != null))) {
			/** compute Hamming distances */
			int diffOne = Integer.MAX_VALUE;
			if (useOne && res.one.latMin <= point.getLat() && point.getLat() <= res.one.latMax)
				diffOne = Math.min(diffOne, Math.min(point.distanceTo(Point.newInstance(point
						.getLat(), res.one.latMin)), point.distanceTo(Point.newInstance(point
						.getLat(), res.one.latMax))));
			if (useOne && res.one.lonMin <= point.getLon() && point.getLon() <= res.one.lonMax)
				diffOne = Math.min(diffOne, Math.min(point.distanceTo(Point.newInstance(
						res.one.lonMin, point.getLon())), point.distanceTo(Point.newInstance(
						res.one.lonMax, point.getLon()))));
			int diffTwo = Integer.MAX_VALUE;
			if (useTwo && res.two.latMin <= point.getLat() && point.getLat() <= res.two.latMax)
				diffTwo = Math.min(diffTwo, Math.min(point.distanceTo(Point.newInstance(point
						.getLat(), res.two.latMin)), point.distanceTo(Point.newInstance(point
						.getLat(), res.two.latMax))));
			if (useTwo && res.two.lonMin <= point.getLon() && point.getLon() <= res.two.lonMax)
				diffTwo = Math.min(diffTwo, Math.min(point.distanceTo(Point.newInstance(
						res.two.lonMin, point.getLon())), point.distanceTo(Point.newInstance(
						res.two.lonMax, point.getLon()))));
			res = (diffOne < diffTwo) ? res.one : res.two;
		}

		return res;
	}

}
