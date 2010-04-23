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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;

import org.mapsforge.server.core.geoinfo.IPoint;
import org.mapsforge.server.core.geoinfo.Node;
import org.mapsforge.server.routing.core.Route;

public class RoutingFileWriter {

	private enum Status {
		CLOSED, //
		FINISHED, //
		GPX, //
		TRACK, //
		TRACKPOINT, //
		UNOPENED, //
	}

	public static void writeMapPosition(String filename, double lat, double lon, int zoom)
			throws IOException {

		FileWriter writer = new FileWriter(new File(filename));

		writer.append("var lat = " + new DecimalFormat("###.#####").format(lat) + ";\n");
		writer.append("var lon = " + new DecimalFormat("###.#####").format(lon) + ";\n");
		writer.append("var zoom = " + new DecimalFormat("##").format(zoom) + ";\n");

		writer.flush();
		writer.close();
	}

	private Status status;

	private final FileWriter WRITER;

	public RoutingFileWriter(String filename) throws IOException {
		this.WRITER = new FileWriter(new File(filename));
		this.status = Status.UNOPENED;
	}

	/**
	 * closes writer regardless of current status
	 * 
	 * @throws IOException
	 *             forwarded by the FileWriter used
	 */
	public void close() throws IOException {
		this.WRITER.flush();
		this.WRITER.close();
		this.status = Status.CLOSED;
	}

	public void writeRoute(Route route) throws IOException {
		startGpxFile();

		/** mark points on the route */
		List<Route.Section> sections = route.sections();
		List<List<IPoint>> trackSections = new LinkedList<List<IPoint>>();
		if (!sections.isEmpty()) {
			writeWayPoint(sections.get(0).source());
			for (Route.Section section : sections)
				writeWayPoint(section.destination());
			for (Route.Section section : sections) {
				List<Route.Section.Way> ways = section.ways();
				List<IPoint> points = new LinkedList<IPoint>();
				if (!ways.isEmpty()) {
					points.add(ways.get(0).source());
					for (Route.Section.Way e : ways) {
						for (Node n : e.intermediateNodes())
							points.add(n);
						points.add(e.destination());
					}
				}
				trackSections.add(points);
			}

			writeTrack(trackSections, "Route from " + route.source() + " to "
					+ route.destination());
		}
		endGpxFile();
	}

	public <L extends List<? extends Iterable<? extends IPoint>>> void writeTrack(L nodesList,
			String trackName) throws IOException {
		if (!(this.status == Status.GPX))
			throw new IllegalStateException();

		this.WRITER.append("\t<trk>\n");
		this.WRITER.append("\t\t<name>" + trackName + "</name>\n");
		this.WRITER.append("\t\t<number>" + nodesList.size() + "</number>\n");

		this.status = Status.TRACK;

		for (Iterable<? extends IPoint> nodes : nodesList) {
			this.writeTrackSegment(nodes);
		}

		this.WRITER.append("\t</trk>\n");
		this.status = Status.GPX;
	}

	public <N extends IPoint> void writeTrackPoint(N node) throws IOException {
		if (!(this.status == Status.TRACKPOINT))
			throw new IllegalStateException();
		this.WRITER.append("\t\t\t<trkpt lat=\"" + node.latitudeDegrees() + "\" lon=\""
				+ node.longitudeDegrees() + "\"/>\n");
	}

	public <I extends Iterable<? extends IPoint>> void writeTrackPoints(I nodes)
			throws IOException {
		for (IPoint node : nodes) {
			this.writeTrackPoint(node);
		}
	}

	public <I extends Iterable<? extends IPoint>> void writeTrackSegment(I nodes)
			throws IOException {
		if (!(this.status == Status.TRACK))
			throw new IllegalStateException();

		this.WRITER.append("\t\t<trkseg>\n");
		this.status = Status.TRACKPOINT;

		this.writeTrackPoints(nodes);

		this.WRITER.append("\t\t</trkseg>\n");
		this.status = Status.TRACK;
	}

	public <N extends IPoint> void writeWayPoint(N node) throws IOException {
		if (!(this.status == Status.GPX))
			throw new IllegalStateException();
		this.WRITER.append("\t<wpt lat=\"" + node.latitudeDegrees() + "\" lon=\""
				+ node.longitudeDegrees() + "\"/>\n");
	}

	public <I extends Iterable<? extends IPoint>> void writeWayPoints(I nodes)
			throws IOException {
		for (IPoint node : nodes) {
			this.writeWayPoint(node);
		}
	}

	private void endGpxFile() throws IOException {
		if (!(this.status == Status.GPX))
			throw new IllegalStateException();

		this.WRITER.append("</gpx>\n");
		this.status = Status.FINISHED;
		close();
	}

	private void startGpxFile() throws IOException {
		if (!(this.status == Status.UNOPENED))
			throw new IllegalStateException();

		/** write file intro */
		this.WRITER.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>\n");
		this.WRITER
				.append("<gpx version=\"1.0\" creator=\"Turbo GPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.topografix.com/GPX/1/0\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd\">\n");
		this.status = Status.GPX;
	}

}
