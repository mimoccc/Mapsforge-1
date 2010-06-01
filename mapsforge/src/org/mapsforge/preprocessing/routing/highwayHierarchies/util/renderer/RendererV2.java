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

import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Iterator;

import javax.swing.JFrame;

import org.mapsforge.preprocessing.routing.highwayHierarchies.datastructures.InRamCoordinateIndex;
import org.mapsforge.preprocessing.util.GeoCoordinate;
import org.mapsforge.server.routing.IEdge;
import org.mapsforge.server.routing.IRouter;
import org.mapsforge.server.routing.IVertex;
import org.mapsforge.server.routing.RouterFactory;

import com.jhlabs.map.proj.Projection;

public class RendererV2 {

	private static final double[] zoomLevels = getZoomLevels(20);

	private final BufferedCanvas canvas;

	private final Projection proj;
	private final IRouter router;

	private int zoomLevel;
	private GeoCoordinate center;
	private double screenW, screenH, metersPerPixel;
	private double minX, minY;
	private int minLon, minLat, maxLon, maxLat;

	private Color bgColor, fgColor;

	public RendererV2(int width, int height, IRouter router, Color bgColor, Color fgColor) {
		this.screenW = width;
		this.screenH = height;
		this.proj = InRamCoordinateIndex.DEFAULT_PROJECTION_GERMANY;
		this.router = router;

		this.bgColor = bgColor;
		this.fgColor = fgColor;

		this.canvas = new BufferedCanvas(width, height);
		canvas.clear(bgColor);

		setRenderParam(new GeoCoordinate(
				(router.getMaxLatitude() + router.getMinLatitude()) / 2, (router
						.getMaxLongitude() + router.getMinLongitude()) / 2), 3);

		new RendererFrame();
	}

	private static double[] getZoomLevels(int n) {
		double[] tmp = new double[n];
		tmp[0] = 4;
		for (int i = 1; i < tmp.length; i++) {
			tmp[i] = tmp[i - 1] + (tmp[i - 1] / 2.2);
		}
		return tmp;
	}

	private synchronized void zoomOut() {
		if (zoomLevel < zoomLevels.length - 1) {
			setRenderParam(center, zoomLevel + 1);
		}
	}

	private synchronized void zoomInt() {
		if (zoomLevel > 0) {
			setRenderParam(center, zoomLevel - 1);
		}
	}

	private void drawGraph() {
		for (Iterator<? extends IVertex> iter = router.getVerticesWithinBox(minLon, minLat,
				maxLon, maxLat); iter.hasNext();) {
			IVertex v = iter.next();
			for (IEdge e : v.getOutboundEdges()) {
				ScreenCoordinate sc1 = geoToScreen(v.getCoordinate().getLongitudeInt(), v
						.getCoordinate().getLatitudeInt());
				ScreenCoordinate sc2 = geoToScreen(e.getTarget().getCoordinate()
						.getLongitudeInt(), e.getTarget().getCoordinate().getLatitudeInt());

				canvas.drawLine(sc1.x, sc1.y, sc2.x, sc2.y, fgColor);
			}
		}
	}

	private ScreenCoordinate geoToScreen(int lon, int lat) {
		// projection to carthesian coordinate (x, y)
		double[] tmp = new double[] { GeoCoordinate.itod(lon), GeoCoordinate.itod(lat) };
		proj.transform(tmp, 0, tmp, 0, 1);
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
		proj.inverseTransform(tmp, 0, tmp, 0, 1);

		return new GeoCoordinate(tmp[1], tmp[0]);
	}

	private void setRenderParam(GeoCoordinate center, int zoomLevel) {
		this.center = center;
		this.zoomLevel = zoomLevel;
		this.metersPerPixel = zoomLevels[zoomLevel];

		// project center
		double[] tmp = new double[] { GeoCoordinate.itod(center.getLongitudeInt()),
				GeoCoordinate.itod(center.getLatitudeInt()) };
		proj.transform(tmp, 0, tmp, 0, 1);
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
		canvas.clear(bgColor);
		drawGraph();
		canvas.update();
	}

	private class ScreenCoordinate {

		private final int x, y;

		public ScreenCoordinate(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}

	private class RendererFrame extends JFrame {

		private static final long serialVersionUID = -7699248454662433016L;
		private Point lastDragPoint = null;
		private RendererFrame lock = this;

		public RendererFrame() {
			super("Graph Renderer v0.1a");
			getContentPane().add(canvas);
			pack();
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			setResizable(false);
			setVisible(true);

			addMouseWheelListener(new MouseWheelListener() {

				@Override
				public void mouseWheelMoved(MouseWheelEvent e) {
					if (e.getUnitsToScroll() > 0) {
						zoomInt();
					} else {
						zoomOut();
					}
				}
			});

			addMouseListener(new MouseAdapter() {
				@Override
				public void mouseReleased(MouseEvent e) {
					synchronized (lock) {
						lastDragPoint = null;
					}
				}
			});

			addMouseMotionListener(new MouseMotionListener() {

				@Override
				public void mouseMoved(MouseEvent e) {
					// TODO Auto-generated method stub

				}

				@Override
				public void mouseDragged(MouseEvent e) {
					synchronized (lock) {
						if (lastDragPoint == null) {
							lastDragPoint = e.getPoint();
						} else {
							int dx = lastDragPoint.x - e.getPoint().x;
							int dy = e.getPoint().y - lastDragPoint.y;
							GeoCoordinate coord = screenToGeo((int) (screenW / 2) + dx,
									(int) (screenH / 2) + dy);
							setRenderParam(coord, zoomLevel);
							lastDragPoint = e.getPoint();
						}
					}
				}
			});

		}
	}

	public static void main(String[] args) {
		RendererV2 renderer = new RendererV2(1024, 768, RouterFactory.getRouter(), Color.WHITE,
				Color.BLACK);
		System.out.println("rendering center coord : " + renderer.center);
	}
}
