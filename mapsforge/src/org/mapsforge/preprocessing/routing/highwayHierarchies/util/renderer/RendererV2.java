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
package org.mapsforge.preprocessing.routing.highwayHierarchies.util.renderer;

import gnu.trove.set.hash.THashSet;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.mapsforge.preprocessing.routing.hhmobile.clustering.ICluster;
import org.mapsforge.preprocessing.routing.hhmobile.clustering.IClustering;
import org.mapsforge.preprocessing.util.GeoCoordinate;
import org.mapsforge.server.routing.IEdge;
import org.mapsforge.server.routing.IRouter;
import org.mapsforge.server.routing.IVertex;
import org.mapsforge.server.routing.RouterFactory;

import com.jhlabs.map.proj.Projection;
import com.jhlabs.map.proj.ProjectionFactory;

public class RendererV2 {

	private static Color[] CLUSTER_COLORS = { Color.GREEN, Color.BLUE, Color.RED, Color.YELLOW,
			Color.MAGENTA, Color.CYAN, Color.ORANGE, Color.PINK, Color.MAGENTA };

	private static final double[] ZOOM_LEVELS = getZoomLevels(20);

	public final static Projection PROJ = ProjectionFactory
			.fromPROJ4Specification(new String[] { "+proj=cass", "+lat_0=52.41864827777778",
					"+lon_0=13.62720366666667", "+x_0=40000", "+y_0=10000", "+ellps=bessel",
					"+datum=potsdam", "+units=m", "+no_defs" });

	private static double[] getZoomLevels(int n) {
		double[] tmp = new double[n];
		tmp[0] = 4;
		for (int i = 1; i < tmp.length; i++) {
			tmp[i] = tmp[i - 1] + (tmp[i - 1] / 2.2);
		}
		return tmp;
	}

	private final BufferedCanvas canvas;

	private final IRouter router;
	private final HashMap<IEdge[], Color> routes;
	private IClustering clustering;
	private HashMap<ICluster, Color> clusterColors;

	private int zoomLevel;
	private GeoCoordinate center;
	private double screenW, screenH, metersPerPixel;
	private double minX, minY;
	private int minLon, minLat, maxLon, maxLat;

	private Color bgColor, fgColor;

	public RendererV2(int width, int height, IRouter router, Color bgColor, Color fgColor) {
		this.clustering = null;
		this.screenW = width;
		this.screenH = height;
		this.router = router;

		this.bgColor = bgColor;
		this.fgColor = fgColor;

		this.routes = new HashMap<IEdge[], Color>();
		this.canvas = new BufferedCanvas(width, height);

		canvas.clear(bgColor);
		setRenderParam(router.getMapCenter(), 3);

		new RendererGui();
	}

	public void setRenderParam(GeoCoordinate center, int zoomLevel) {
		this.center = center;
		this.zoomLevel = zoomLevel;
		this.metersPerPixel = ZOOM_LEVELS[zoomLevel];

		// project center
		double[] tmp = new double[] { GeoCoordinate.itod(center.getLongitudeInt()),
				GeoCoordinate.itod(center.getLatitudeInt()) };
		PROJ.transform(tmp, 0, tmp, 0, 1);
		double cx = tmp[0];
		double cy = tmp[1];

		// set bounding coordinates
		minX = cx - (metersPerPixel * (screenW / 2));
		// maxX = cx + (metersPerPixel * (screenW / 2));
		minY = cy - (metersPerPixel * (screenH / 2));
		// maxY = cy + (metersPerPixel * (screenH / 2));

		GeoCoordinate minC = screenToGeo(0, 0);
		GeoCoordinate maxC = screenToGeo((int) screenW, (int) screenH);

		this.minLon = minC.getLongitudeInt();
		this.minLat = minC.getLatitudeInt();
		this.maxLon = maxC.getLongitudeInt();
		this.maxLat = maxC.getLatitudeInt();

		// update screen
		drawRenderContent();
	}

	private void drawRenderContent() {
		canvas.clear(bgColor);
		drawGraph();
		drawRoutes();
		canvas.update();
	}

	public int getNumZoomLevels() {
		return ZOOM_LEVELS.length;
	}

	public int getZoomLevel() {
		return zoomLevel;
	}

	public boolean setZoomLevel(int zl) {
		if (zl >= 0 && zl < ZOOM_LEVELS.length && zl != zoomLevel) {
			zoomLevel = zl;
			setRenderParam(center, zoomLevel);
			return true;
		}
		return false;
	}

	public void addRoute(IEdge[] route, Color c) {
		if (route != null && c != null) {
			synchronized (routes) {
				routes.put(route, c);
				drawRoute(route, c);
				canvas.update();
			}
		}
	}

	public void clearRoutes() {
		synchronized (routes) {
			routes.clear();
			drawRenderContent();
		}
	}

	public void setClustering(IClustering clustering) {
		this.clustering = clustering;
		if (clustering != null) {
			clusterColors = getClusterColors();
		}
		drawRenderContent();
		canvas.update();
	}

	// private void drawClustering() {
	// if (clustering != null) {
	// HashMap<ICluster, Color> colors = getClusterColors();
	// for (ICluster c : colors.keySet()) {
	// for (int v : c.getVertices()) {
	// for (IEdge e : router.getVertex(v).getOutboundEdges()) {
	// if (clustering.getCluster(e.getSource().getId()) == clustering
	// .getCluster(e.getTarget().getId()))
	// drawEdge(e, colors.get(c), 1);
	// }
	// }
	// }
	// }
	// }

	private HashMap<ICluster, Color> getClusterColors() {
		HashMap<ICluster, Color> colors = new HashMap<ICluster, Color>();

		for (ICluster cluster : clustering.getClusters()) {
			HashSet<Color> adjColors = new HashSet<Color>();
			for (ICluster adj : getAdjClusters(cluster)) {
				Color cAdj = colors.get(adj);
				if (cAdj != null) {
					adjColors.add(cAdj);
				}
			}
			for (int i = 0; i < CLUSTER_COLORS.length; i++) {
				if (!adjColors.contains(CLUSTER_COLORS[i])) {
					colors.put(cluster, CLUSTER_COLORS[i]);
					break;
				}
			}
		}
		return colors;
	}

	private ICluster[] getAdjClusters(ICluster cluster) {
		THashSet<ICluster> set = new THashSet<ICluster>();
		for (int v : cluster.getVertices()) {
			for (IEdge e : router.getVertex(v).getOutboundEdges()) {
				ICluster c = clustering.getCluster(e.getTarget().getId());
				if (c != null && !c.equals(cluster)) {
					set.add(c);
				}
			}
		}
		ICluster[] adjClusters = new ICluster[set.size()];
		set.toArray(adjClusters);
		return adjClusters;
	}

	private void drawGraph() {
		for (Iterator<? extends IVertex> iter = router.getVerticesWithinBox(minLon, minLat,
				maxLon, maxLat); iter.hasNext();) {
			IVertex v = iter.next();
			for (IEdge e : v.getOutboundEdges()) {
				drawEdge(e, fgColor);
				if (clustering != null) {
					ICluster c1 = clustering.getCluster(e.getSource().getId());
					ICluster c2 = clustering.getCluster(e.getTarget().getId());
					if (c1 != null && c1 == c2) {
						drawEdge(e, clusterColors.get(c1), 1);
					} else {
						drawEdge(e, fgColor);
					}
				} else {
					drawEdge(e, fgColor);
				}
			}
		}
	}

	private void drawEdge(IEdge e, Color c) {
		ScreenCoordinate sc1 = geoToScreen(e.getSource().getCoordinate().getLongitudeInt(), e
				.getSource().getCoordinate().getLatitudeInt());
		ScreenCoordinate sc2 = geoToScreen(e.getTarget().getCoordinate().getLongitudeInt(), e
				.getTarget().getCoordinate().getLatitudeInt());
		canvas.drawLine(sc1.x, sc1.y, sc2.x, sc2.y, c);
	}

	private void drawEdge(IEdge e, Color c, int width) {
		ScreenCoordinate sc1 = geoToScreen(e.getSource().getCoordinate());
		ScreenCoordinate sc2 = geoToScreen(e.getTarget().getCoordinate());
		canvas.drawLine(sc1.x, sc1.y, sc2.x, sc2.y, c, width);
	}

	private void drawRoute(IEdge[] route, Color c) {
		for (IEdge e : route) {
			drawEdge(e, c, 2);
		}
	}

	private void drawRoutes() {
		for (IEdge[] route : routes.keySet()) {
			Color c = routes.get(route);
			drawRoute(route, c);
		}
	}

	private ScreenCoordinate geoToScreen(GeoCoordinate c) {
		return geoToScreen(c.getLongitudeInt(), c.getLatitudeInt());
	}

	private ScreenCoordinate geoToScreen(int lon, int lat) {
		// projection to carthesian coordinate (x, y)
		double[] tmp = new double[] { GeoCoordinate.itod(lon), GeoCoordinate.itod(lat) };
		PROJ.transform(tmp, 0, tmp, 0, 1);
		double x = tmp[0];
		double y = tmp[1];

		// from carthesian coordinate (meter units) to screenCoordinate
		int scrX = (int) Math.rint((x - minX) / metersPerPixel);
		int scrY = (int) Math.rint((y - minY) / metersPerPixel);

		return new ScreenCoordinate(scrX, scrY);
	}

	private GeoCoordinate screenToGeo(int scrX, int scrY) {
		// carthesian coordinate in meter units
		double x = minX + (scrX * metersPerPixel);
		double y = minY + (scrY * metersPerPixel);

		// inverse projection
		double[] tmp = new double[] { x, y };
		PROJ.inverseTransform(tmp, 0, tmp, 0, 1);

		return new GeoCoordinate(tmp[1], tmp[0]);
	}

	private class ScreenCoordinate {

		private final int x, y;

		public ScreenCoordinate(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}

	private class RendererGui extends JFrame {

		private static final long serialVersionUID = -7699248454662433016L;

		private MapMouseAdapter mouseAdapter;
		private JSlider zoomSlider;

		private GeoCoordinate routeSource, routeDestination;
		private Object lockSrcTgt = new Boolean(true);

		private final double SCROLL_FAC = 0.20;

		public RendererGui() {
			super("Graph Renderer v0.1a");

			// temporary for selection of source and target
			this.routeSource = null;
			this.routeDestination = null;

			// add root all components
			getContentPane().setLayout(new BorderLayout());
			getContentPane().add(canvas, BorderLayout.CENTER);
			this.zoomSlider = getMapZoomSlider();
			getContentPane().add(zoomSlider, BorderLayout.EAST);

			// set up display
			pack();
			setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			setResizable(false);
			setVisible(true);

			// menu
			PopMenu menu = new PopMenu();
			canvas.addMouseListener(menu.getMouseAdapter());

			// map mouse handling
			mouseAdapter = new MapMouseAdapter();
			canvas.addMouseListener(mouseAdapter);
			canvas.addMouseMotionListener(mouseAdapter);
			canvas.addMouseWheelListener(mouseAdapter);

			// map keyboard handling
			this.addKeyListener(getMapKeyAdapter());

		}

		private void setRouteSource(Point p) {
			GeoCoordinate c = screenToGeo(p.x, (int) screenH - p.y);
			synchronized (lockSrcTgt) {
				routeSource = c;
				triggerRouteComputation();
			}
		}

		private void setRouteTarget(Point p) {
			GeoCoordinate c = screenToGeo(p.x, (int) screenH - p.y);
			synchronized (lockSrcTgt) {
				routeDestination = c;
				triggerRouteComputation();
			}
		}

		private void triggerRouteComputation() {
			synchronized (lockSrcTgt) {
				if (routeSource != null && routeDestination != null) {
					IVertex s = router.getNearestVertex(routeSource);
					IVertex t = router.getNearestVertex(routeDestination);
					IEdge[] route = router.getShortestPath(s.getId(), t.getId());
					addRoute(route, Color.BLUE);
					routeSource = null;
					routeDestination = null;
				}
			}
		}

		private JSlider getMapZoomSlider() {
			final JSlider slider = new JSlider(SwingConstants.VERTICAL, 0, getNumZoomLevels(),
					getZoomLevel());
			slider.setMajorTickSpacing(5);
			slider.setMinorTickSpacing(1);
			slider.setPaintTicks(true);
			slider.setPaintLabels(true);
			slider.setSnapToTicks(true);
			slider.setPreferredSize(new Dimension(slider.getPreferredSize().width, 150));
			slider.addChangeListener(new ChangeListener() {

				@Override
				public void stateChanged(ChangeEvent e) {
					if (!slider.getValueIsAdjusting()) {
						setRenderParam(center, slider.getValue());
					}
				}
			});

			return slider;
		}

		private KeyAdapter getMapKeyAdapter() {
			return new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					switch (e.getKeyChar()) {
						case '-': {
							setZoomLevel(getZoomLevel() - 1);
							return;
						}
						case '+': {
							setZoomLevel(getZoomLevel() + 1);
							return;
						}
					}

					switch (e.getKeyCode()) {

						case KeyEvent.VK_DOWN: {
							int x = (int) (screenW / 2);
							int y = (int) ((screenH / 2) - (SCROLL_FAC * screenH));
							setRenderParam(screenToGeo(x, y), zoomLevel);
							return;
						}
						case KeyEvent.VK_UP: {
							int x = (int) (screenW / 2);
							int y = (int) ((screenH / 2) + (SCROLL_FAC * screenH));
							setRenderParam(screenToGeo(x, y), zoomLevel);
							return;
						}
						case KeyEvent.VK_LEFT: {
							int x = (int) ((screenW / 2) - (SCROLL_FAC * screenH));
							int y = (int) (screenH / 2);
							setRenderParam(screenToGeo(x, y), zoomLevel);
							return;
						}
						case KeyEvent.VK_RIGHT: {
							int x = (int) ((screenW / 2) + (SCROLL_FAC * screenH));
							int y = (int) (screenH / 2);
							setRenderParam(screenToGeo(x, y), zoomLevel);
							return;
						}

					}
				}
			};
		}

		private class MapMouseAdapter extends MouseAdapter {

			private Point lastDragPoint;
			private final Object lockDragPoint;
			private final DecimalFormat df;

			public MapMouseAdapter() {
				super();
				this.df = new DecimalFormat("#.#####");
				df.setMinimumFractionDigits(5);
				df.setMaximumFractionDigits(5);
				lastDragPoint = null;
				lockDragPoint = new Object();
			}

			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				int zl = getZoomLevel();
				if (e.getUnitsToScroll() < 0) {
					zl--;
				} else {
					zl++;
				}
				if (zl > 0 && zl < getNumZoomLevels()) {
					zoomSlider.setValue(zl);
				}
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				synchronized (lockDragPoint) {
					if (lastDragPoint != null) {
						int dx = lastDragPoint.x - e.getPoint().x;
						int dy = e.getPoint().y - lastDragPoint.y;
						GeoCoordinate coord = screenToGeo((int) (screenW / 2) + dx,
								(int) (screenH / 2) + dy);
						setRenderParam(coord, zoomLevel);
						lastDragPoint = e.getPoint();
						lastDragPoint = e.getPoint();
					}
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					synchronized (lockDragPoint) {
						lastDragPoint = null;
					}
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					synchronized (lockDragPoint) {
						lastDragPoint = e.getPoint();
					}
				}
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				Point p = e.getPoint();
				GeoCoordinate c = screenToGeo(p.x, (int) screenH - p.y);
				canvas.setToolTipText("(" + df.format(c.getLatitude().getDegree()) + ","
						+ df.format(c.getLongitude().getDegree()) + ")");
			}
		}

		private class PopMenu extends JPopupMenu {

			private static final long serialVersionUID = 1L;

			private MouseAdapter mouseListener;
			private Color cSelected = Color.green.darker().darker();
			private Point position;

			public PopMenu() {
				super();
				mouseListener = new MouseAdapter() {

					@Override
					public void mouseReleased(MouseEvent e) {
						if (e.isPopupTrigger()) {
							show(e.getComponent(), e.getX(), e.getY());
							position = e.getPoint();
						}
					}
				};
				add(getRouterMenu());
				add(getMapMenu());
			}

			public MouseAdapter getMouseAdapter() {
				return mouseListener;
			}

			private JMenu getMapMenu() {
				JMenu menu = new JMenu("map");
				JMenuItem miCenter = new JMenuItem("go to center");
				miCenter.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						setRenderParam(router.getMapCenter(), zoomLevel);
					}
				});
				menu.add(miCenter);
				return menu;
			}

			private JMenu getRouterMenu() {
				JMenu menu = new JMenu("routing");
				JMenuItem miSrc = new JMenuItem("choose as source") {
					private static final long serialVersionUID = 1L;

					@Override
					public Color getForeground() {
						if (routeSource == null) {
							return super.getForeground();
						}
						return cSelected;
					}
				};
				miSrc.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						setRouteSource(position);
					}
				});
				menu.add(miSrc);
				JMenuItem miDst = new JMenuItem("choose as destination") {
					private static final long serialVersionUID = 1L;

					@Override
					public Color getForeground() {
						if (routeDestination == null) {
							return super.getForeground();
						}
						return cSelected;
					}
				};
				miDst.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						setRouteTarget(position);
					}
				});
				menu.add(miDst);

				JMenuItem miClear = new JMenuItem("clear Routes");
				miClear.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						clearRoutes();
					}
				});
				menu.add(miClear);

				return menu;
			}
		}
	}

	public static void main(String[] args) {
		RendererV2 renderer = new RendererV2(1024, 768, RouterFactory.getRouter(), Color.WHITE,
				Color.BLACK);
		System.out.println("rendering center coord : " + renderer.center);
	}
}
