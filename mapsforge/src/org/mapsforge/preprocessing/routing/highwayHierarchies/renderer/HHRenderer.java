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
package org.mapsforge.preprocessing.routing.highwayHierarchies.renderer;

import java.awt.Color;
import java.awt.Dimension;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

import org.mapsforge.preprocessing.routing.highwayHierarchies.datastructures.HHStaticGraph;
import org.mapsforge.preprocessing.routing.highwayHierarchies.datastructures.InRamCoordinateIndex;
import org.mapsforge.preprocessing.routing.highwayHierarchies.datastructures.HHStaticGraph.HHStaticEdge;
import org.mapsforge.preprocessing.routing.highwayHierarchies.datastructures.HHStaticGraph.HHStaticVertex;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.geo.CarthesianPoint;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.geo.PolarCoordinate;
import org.mapsforge.server.routing.highwayHierarchies.HHAlgorithm;
import org.mapsforge.server.routing.highwayHierarchies.HHRouter;
import org.mapsforge.server.routing.highwayHierarchies.HHRouterFactory;

import com.jhlabs.map.proj.Projection;

/**
 * @author Frank Viernau
 */
public class HHRenderer extends JFrame {

	private static final long serialVersionUID = 6150126433610297912L;

	private final static double FAC = 10;

	private final static int WINDOW_WIDTH = 1024;
	private final static int WINDOW_HEIGHT = 768;

	private final double width, height;
	private final BufferedCanvas canvas;
	private final Projection proj;
	private final HHStaticGraph graph;
	private final InRamCoordinateIndex coordinateIndex;

	private double metersPerPixel;
	private double minX, maxX, minY, maxY;
	private Color bgColor = Color.WHITE;

	public HHRenderer(int width, int height, HHStaticGraph graph,
			InRamCoordinateIndex coordinateIndex, double metersPerPixel) {
		super();
		this.width = width;
		this.height = height;
		this.graph = graph;
		this.coordinateIndex = coordinateIndex;
		this.metersPerPixel = metersPerPixel;
		canvas = new BufferedCanvas(width, height);

		setSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		getContentPane().add(new JScrollPane(canvas));
		canvas.clear(bgColor);

		proj = InRamCoordinateIndex.DEFAULT_PROJECTION_GERMANY;

		CarthesianPoint cMax = polarToCarthesian(coordinateIndex.getMaxCoordinate(), proj);
		CarthesianPoint cMin = polarToCarthesian(coordinateIndex.getMinCoordinate(), proj);
		CarthesianPoint c = new CarthesianPoint((cMax.x + cMin.x) / 2, (cMax.y + cMin.y) / 2);

		setCenter(
		// carthesianToPolar(c, proj)
		new PolarCoordinate(13d, 52d));
		setVisible(true);
		clear();
		drawGraph();
		update();
	}

	public void setCenter(PolarCoordinate center) {
		System.out.println("center : " + center);
		CarthesianPoint center_ = polarToCarthesian(center, proj);
		minX = center_.x - (metersPerPixel * (width / 2) * FAC);
		maxX = center_.x + (metersPerPixel * (width / 2) * FAC);
		minY = center_.y - (metersPerPixel * (height / 2) * FAC);
		maxY = center_.y + (metersPerPixel * (height / 2) * FAC);
	}

	public void drawLine(PolarCoordinate c1, PolarCoordinate c2, Color c, int width) {
		CarthesianPoint c1_ = polarToScreen(c1);
		CarthesianPoint c2_ = polarToScreen(c2);
		canvas.drawLine(c1_.x, c1_.y, c2_.x, c2_.y, c, width);
	}

	public void clear() {
		canvas.clear(bgColor);
	}

	public void drawGraph() {
		for (int i = 0; i < graph.numVertices(); i++) {
			HHStaticVertex v = graph.getVertex(i);
			for (HHStaticEdge e : v.getAdjacentEdges(0)) {
				if (!e.isShortcut()) {
					drawEdge(e, Color.black, 0);
				}
			}
		}
	}

	public void update() {
		canvas.repaint();
	}

	public void drawVertex(int id, Color c) {
		CarthesianPoint c_ = polarToScreen(coordinateIndex.getPolarCoordinate(id));
		canvas.drawCircle(c_.x, c_.y, c, 4);

	}

	public void drawPathEdges(LinkedList<HHStaticEdge> edges, Color c, int width) {
		for (HHStaticEdge e : edges) {
			drawEdge(e, c, width);
		}
	}

	private void drawEdge(HHStaticEdge e, Color c, int width) {
		drawLine(coordinateIndex.getPolarCoordinate(e.getSource().getId()), coordinateIndex
				.getPolarCoordinate(e.getTarget().getId()), c, width);
	}

	public CarthesianPoint polarToScreen(PolarCoordinate pc) {
		CarthesianPoint c = polarToCarthesian(pc, proj);
		c.x = (int) Math.rint(((c.x - minX) / (maxX - minX)) * width);
		c.y = (int) Math.rint(((c.y - minY) / (maxY - minY)) * height);
		return c;
	}

	public PolarCoordinate screenToPolar(CarthesianPoint screenCoord) {
		double x = ((screenCoord.x / width) * (maxX - minX)) + minX;
		double y = ((screenCoord.y / height) * (maxY - minY)) + minY;
		CarthesianPoint c = new CarthesianPoint((int) Math.rint(x), (int) Math.rint(y));
		return carthesianToPolar(c, proj);
	}

	private CarthesianPoint polarToCarthesian(PolarCoordinate c, Projection proj) {
		double[] tmp = new double[] { c.getLongitudeDouble(), c.getLatitudeDouble() };
		proj.transform(tmp, 0, tmp, 0, 1);
		return new CarthesianPoint((int) Math.rint(tmp[0] * FAC), (int) Math.rint(tmp[1] * FAC)

		);
	}

	private PolarCoordinate carthesianToPolar(CarthesianPoint c, Projection proj) {
		double[] tmp = new double[] { c.x / FAC, c.y / FAC };
		proj.inverseTransform(tmp, 0, tmp, 0, 1);
		return new PolarCoordinate(tmp[0], tmp[1]);
	}

	public static void main(String[] args) throws SQLException, FileNotFoundException,
			IOException {
		// Connection conn = DbConnection.getBerlinDbConn();
		// HHStaticGraph graph = HHStaticGraph.getFromHHDb(conn);
		// System.out.println(graph.getGraphPropterties());
		// HHEdgeExpanderRecursive edeIndex = HHEdgeExpanderRecursive.createIndex(graph,
		// HHEdgeExpanderRecursive.getEMinLvl(conn));
		// InRamCoordinateIndex coordinateIndex = InRamCoordinateIndex.getFromHHDb(conn,
		// InRamCoordinateIndex.DEFAULT_PROJECTION_GERMANY);
		// DistanceTable dt = DistanceTable.getFromHHDb(conn);
		// HHEdgeReverser reverser = new HHEdgeReverser(graph);
		HHRouter router = HHRouterFactory.getHHRouterInstance();

		Random rnd = new Random(12);
		System.out.println("a");
		HHRenderer renderer = new HHRenderer(3200, 2400, router.routingGraph.graph,
				router.routingGraph.coordinateIndex, 1700);
		System.out.println("b");
		HHAlgorithm algo = new HHAlgorithm();

		for (int i = 0; i < 100; i++) {

			int s = rnd.nextInt(router.routingGraph.coordinateIndex
					.getNearestNeighborIdx(new PolarCoordinate(11, 50)));
			int t = rnd.nextInt(router.routingGraph.coordinateIndex
					.getNearestNeighborIdx(new PolarCoordinate(32, 52)));
			System.out.println(router.routingGraph.coordinateIndex.getPolarCoordinate(s) + " "
					+ router.routingGraph.coordinateIndex.getPolarCoordinate(t));

			LinkedList<HHStaticEdge> searchSpace = new LinkedList<HHStaticEdge>();
			LinkedList<HHStaticEdge> fwd = new LinkedList<HHStaticEdge>();
			LinkedList<HHStaticEdge> bwd = new LinkedList<HHStaticEdge>();
			LinkedList<HHStaticEdge> expandedBwd = new LinkedList<HHStaticEdge>();
			System.out.println("find route");
			int d = algo.shortestPath(router.routingGraph.graph, s, t,
					router.routingGraph.distanceTable, fwd, bwd, searchSpace);
			if (d != Integer.MAX_VALUE) {
				System.out.println("found route");
				LinkedList<HHStaticEdge> searchSpace_ = new LinkedList<HHStaticEdge>();
				router.routingGraph.edgeExpander.expandShortestPath(searchSpace, searchSpace_);
				LinkedList<HHStaticEdge> sp = new LinkedList<HHStaticEdge>();

				router.routingGraph.edgeExpander.expandShortestPath(fwd, sp);
				router.routingGraph.edgeExpander.expandShortestPath(bwd, expandedBwd);
				router.routingGraph.edgeReverser.reverseEdges(expandedBwd, sp);
				System.out.println("numEdges on shortestPath=" + sp.size());
				renderer.drawPathEdges(searchSpace_, Color.BLUE, 2);
				renderer.drawPathEdges(sp, Color.RED, 2);
				renderer.drawVertex(s, Color.GREEN);
				renderer.drawVertex(t, Color.GREEN);
				renderer.update();
			} else {
				System.out.println("no route found");
			}
		}
		System.out.println("ready");

		// long start = System.currentTimeMillis();
		// for(int i=0;i<1000;i++) {
		// int s = rnd.nextInt(router.routingGraph.graph.numVertices());
		// int t = rnd.nextInt(router.routingGraph.graph.numVertices());
		// System.out.println("route " + s + " -> " + t);
		// System.out.println("edges : " + router.getShortestPath(s, t).size());
		// }
		// long time = System.currentTimeMillis() - start;
		// System.out.println(time);

		// long start = System.currentTimeMillis();
		// for(int i=0;i<100000;i++){
		// PolarCoordinate c = router.getCoordinate(i);
		// router.routingGraph.coordinateIndex.getNearestNeighborIdx(c);
		// }
		// long time = System.currentTimeMillis() - start;
		// System.out.println(time);
	}
}
