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
package org.mapsforge.server.routing.highwayHierarchies;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.mapsforge.core.conf.IVehicle;
import org.mapsforge.preprocessing.routing.highwayHierarchies.datastructures.DistanceTable;
import org.mapsforge.preprocessing.routing.highwayHierarchies.datastructures.GeoCoordinateKDTree;
import org.mapsforge.preprocessing.routing.highwayHierarchies.datastructures.HHEdgeExpanderRecursive;
import org.mapsforge.preprocessing.routing.highwayHierarchies.datastructures.HHEdgeReverser;
import org.mapsforge.preprocessing.routing.highwayHierarchies.datastructures.HHStaticGraph;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.Serializer;
import org.mapsforge.preprocessing.util.GeoCoordinate;
import org.mapsforge.server.core.geoinfo.BoundingBox;
import org.mapsforge.server.core.geoinfo.Node;
import org.mapsforge.server.core.geoinfo.Point;
import org.mapsforge.server.routing.core.ConnectionHandler;
import org.mapsforge.server.routing.core.IGeoMap;
import org.mapsforge.server.routing.core.IRoutingGraph;
import org.mapsforge.server.routing.core.PropertyConfiguration;

/**
 * @author Frank Viernau
 * 
 *         Bundles all datastructures needed for routing. Namely the graph, an index and storage
 *         structure for vertex coordinate, A datasctructure for translating shortcuts to level
 *         yero edges, a distance table holding all pars shortest path distances for the top
 *         level core and a datastructure for fast retrieval of reverse edges.
 * 
 */
public class HHCompleteRoutingGraph implements IRoutingGraph, IGeoMap {

	private static String FILE_NAME_GRAPH = "hhGraph.dat";
	private static String FILE_NAME_DISTANCE_TABLE = "hhDistanceTable.dat";
	private static String FILE_NAME_COORDINATE_INDEX = "hhCoordinateIdx.dat";
	private static String FILE_NAME_EDGE_EXPANDER = "hhEdgeExpander.dat";
	private static String FILE_NAME_EDGE_REVERSER = "hhEdgeReverser.dat";

	public final HHStaticGraph graph;
	public final DistanceTable distanceTable;
	public final GeoCoordinateKDTree vertexIndex;
	public final HHEdgeExpanderRecursive edgeExpander;
	public final HHEdgeReverser edgeReverser;
	private final BoundingBox bbox;

	/**
	 * method to create geoMap
	 */
	public static HHCompleteRoutingGraph newInstance(String name, Date date,
			ConnectionHandler conHdr, PropertyConfiguration propConf, DataInput in,
			DataOutput out, IRoutingGraph routingGraph) {
		return (HHCompleteRoutingGraph) routingGraph;
	}

	public static HHCompleteRoutingGraph newInstance(PropertyConfiguration propConf,
			ConnectionHandler conHdr, DataInput in, DataOutput out, IVehicle vehicle) {
		try {
			return HHRouterFactory.getHHRouterInstance().routingGraph;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;

	}

	public HHCompleteRoutingGraph(HHStaticGraph graph, DistanceTable distanceTable,
			GeoCoordinateKDTree vertexIndex, HHEdgeExpanderRecursive edgeExpander,
			HHEdgeReverser edgeReverser) {
		this.graph = graph;
		this.distanceTable = distanceTable;
		this.vertexIndex = vertexIndex;
		this.edgeExpander = edgeExpander;
		this.edgeReverser = edgeReverser;
		this.bbox = BoundingBox
				.getInstance(vertexIndex.getMinLatitude(), vertexIndex.getMinLongitude(),
						vertexIndex.getMaxLatitude(), vertexIndex.getMaxLongitude());
	}

	public static HHCompleteRoutingGraph importGraphFromDb(Connection conn) throws SQLException {

		HHStaticGraph graph = HHStaticGraph.getFromHHDb(conn);
		DistanceTable distanceTable = DistanceTable.getFromHHDb(conn);
		GeoCoordinateKDTree vertexIndex = GeoCoordinateKDTree.buildHHVertexIndex(conn);
		HHEdgeExpanderRecursive edgeExpander = HHEdgeExpanderRecursive.createIndex(graph,
				HHEdgeExpanderRecursive.getEMinLvl(conn));
		HHEdgeReverser edgeReverser = new HHEdgeReverser(graph);
		HHCompleteRoutingGraph routingGraph = new HHCompleteRoutingGraph(graph, distanceTable,
				vertexIndex, edgeExpander, edgeReverser);

		return routingGraph;
	}

	public static HHCompleteRoutingGraph importGraphFromFile(File inDir) throws IOException,
			ClassNotFoundException {
		HHStaticGraph graph = Serializer.deserialize(getFile(inDir, FILE_NAME_GRAPH));
		DistanceTable distanceTable = Serializer.deserialize(getFile(inDir,
				FILE_NAME_DISTANCE_TABLE));
		HHEdgeExpanderRecursive edgeExpander = Serializer.deserialize(getFile(inDir,
				FILE_NAME_EDGE_EXPANDER));
		HHEdgeReverser edgeReverser = Serializer.deserialize(getFile(inDir,
				FILE_NAME_EDGE_REVERSER));
		GeoCoordinateKDTree coordinateIndex = Serializer.deserialize(new FileInputStream(
				getFile(inDir, FILE_NAME_COORDINATE_INDEX)));
		return new HHCompleteRoutingGraph(graph, distanceTable, coordinateIndex, edgeExpander,
				edgeReverser);
	}

	public static void exportGraphToDirectory(File outDir, HHCompleteRoutingGraph graph)
			throws IOException {
		if (!outDir.exists()) {
			outDir.mkdirs();
		}
		Serializer.serialize(getFile(outDir, FILE_NAME_GRAPH), graph.graph);
		Serializer.serialize(getFile(outDir, FILE_NAME_DISTANCE_TABLE), graph.distanceTable);
		Serializer.serialize(getFile(outDir, FILE_NAME_EDGE_EXPANDER), graph.edgeExpander);
		Serializer.serialize(getFile(outDir, FILE_NAME_EDGE_REVERSER), graph.edgeReverser);
		Serializer.serialize(getFile(outDir, FILE_NAME_COORDINATE_INDEX), graph.vertexIndex);
	}

	public static File getFile(File directory, String fileName) {
		return new File(directory.getAbsolutePath() + File.separatorChar + fileName);
	}

	@Override
	public long getNEdges() {
		return graph.getGraphPropterties().levelStats[0].numEdges;
	}

	@Override
	public int getNVertices() {
		return graph.numVertices();
	}

	@Override
	public boolean isBidirectional(int sourceId, int destinationId) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void write(DataOutput out) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public VertexDistance[] getAdjacentVertices(Point wayPoint) throws IllegalArgumentException {
		// TODO no way-points detected !!
		// TODO distance zero is wrong !!
		return new VertexDistance[] { new VertexDistance(vertexIndex.getNearestNeighborId(
				wayPoint.getLon(), wayPoint.getLat()), 0) };
	}

	@Override
	public BoundingBox getBoundingBox() {
		return bbox;
	}

	@Override
	public String getName() {
		return "TODO";
	}

	@Override
	public Date getReleaseDate() {
		return new Date(graph.getGraphPropterties().creationDate.getTime());
	}

	@Override
	public Point getVertexPoint(int vtxId) {
		GeoCoordinate c = vertexIndex.getCoordinate(vtxId);
		return Point.newInstance(c.getLatitudeInt(), c.getLongitudeInt());
	}

	@Override
	public Point getWayPoint(Point arbitraryPosition) {
		int id = vertexIndex.getNearestNeighborId(arbitraryPosition.getLon(), arbitraryPosition
				.getLat());
		GeoCoordinate c = vertexIndex.getCoordinate(id);
		return Point.newInstance(c.getLatitudeInt(), c.getLongitudeInt());
	}

	@Override
	public Iterable<Point> getWayPoints(Point arbitraryPosition, int nPointsRequested,
			BoundingBox centralSearchArea) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Node> getNoneVertexNodes(int srcId, int dstId) {
		// TODO:fix
		return Collections.emptyList();
	}

	@Override
	public Node vertexNode(int vtxId) {
		GeoCoordinate c = vertexIndex.getCoordinate(vtxId);
		return Node.newNode(c.getLatitudeInt(), c.getLongitudeInt());
	}
}
