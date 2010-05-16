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
package org.mapsforge.server.routing.debug;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import org.mapsforge.server.core.geoinfo.Point;
import org.mapsforge.server.routing.core.Route;
import org.mapsforge.server.routing.core.Router;

public class TestRouter {
	private static final Logger LOGGER = Logger.getLogger(TestRouter.class.getName());

	public static void main(String[] args) throws Exception {
		// if (args.length != 3) {
		// System.err
		//					.println("Paramters: Properties-File OutputGPX-File mapPositionJS-File"); //$NON-NLS-1$
		// System.exit(-1);
		// }
		Properties props = new Properties();
		// FileInputStream fis = new FileInputStream(new File(args[0]));
		// props.load(fis);
		// fis.close();

		long start = System.currentTimeMillis();
		Router router = Router.newInstance(props);
		long stop = System.currentTimeMillis();
		start = System.currentTimeMillis();
		List<Point> points = new LinkedList<Point>();
		String p1 = "a";
		// while (true) {
		// p1 = javax.swing.JOptionPane
		// .showInputDialog("Point coordinates on route?");
		// if (p1.equals(""))
		// break;
		// String[] coords = p1.split(",");
		// points.add(Point.newInstance(Double.valueOf(coords[0]), Double
		// .valueOf(coords[1])));
		// }
		if (points.size() < 2) {
			System.err.println("minimum 2 Points must be specified to route.\nusing predefined points."); //$NON-NLS-1$
			points = Arrays.asList( //
					 Point.newInstance(52.487, 13.480),  // Insel der Jugend
					 Point.newInstance(52.487, 13.425),  // Hermannplatz
					 Point.newInstance(52.527, 13.412),  // Volksbuehne
					 Point.newInstance(52.456, 13.297)); // inf.fu-berlin.de
		}
		Route route = router.route(points);

		// for (Node n : route.intermediateNodes()) {
		// System.out.println(n.getLat() + " : " + n.getLon());
		// }

		stop = System.currentTimeMillis();
		LOGGER.info((stop - start) / 1000.0 + "sec needed to compute route."); //$NON-NLS-1$

		start = System.currentTimeMillis();
		RoutingFileWriter rWriter = new RoutingFileWriter(args[1]);
		rWriter.writeRoute(route);
		Point center = Point.center(route.source(), route.destination());
		// RoutingFileWriter.writeMapPosition(args[2], center.latitudeDegrees(),
		// center.longitudeDegrees(), 16);
		stop = System.currentTimeMillis();
		LOGGER.info((stop - start) / 1000.0 + "sec needed to write route to file."); //$NON-NLS-1$
	}
}
