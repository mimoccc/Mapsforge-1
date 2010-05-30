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
package org.mapsforge.preprocessing.routing.highwayHierarchies.datastructures;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;

import org.mapsforge.preprocessing.routing.highwayHierarchies.HHDbReader;
import org.mapsforge.preprocessing.routing.highwayHierarchies.HHDbReader.HHVertex;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.Serializer;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.geo.CarthesianPoint;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.geo.PolarCoordinate;

import com.jhlabs.map.proj.Projection;
import com.jhlabs.map.proj.ProjectionFactory;

/**
 * Index and storage structure using projection and kd tree. This is like a secondary index
 * since it keeps memory location of coordinates thus allows for finding coordinate by vertex id
 * and vice versa via the kd tree.
 * 
 * @author Frank Viernau
 */
public class InRamCoordinateIndex {

	public final static Projection DEFAULT_PROJECTION_GERMANY = ProjectionFactory
			.fromPROJ4Specification(new String[] { "+proj=cass", "+lat_0=52.41864827777778",
					"+lon_0=13.62720366666667", "+x_0=40000", "+y_0=10000", "+ellps=bessel",
					"+datum=potsdam", "+units=m", "+no_defs" });

	private final NearestNeighbor2DTree tree;
	private final Projection projection;
	private final int size;
	private final PolarCoordinate coordMin, coordMax;

	private final static double FAC = 10;

	public InRamCoordinateIndex(int[] lon, int[] lat, Projection proj) {
		this.size = lon.length;
		this.projection = proj;

		for (int i = 0; i < lat.length; i++) {
			PolarCoordinate c = new PolarCoordinate(lon[i], lat[i]);
			CarthesianPoint c_ = polarToCarthesian(c, proj);
			// replace with carthesian coordinates
			lon[i] = c_.x;
			lat[i] = c_.y;
		}
		tree = new NearestNeighbor2DTree(lon, lat);

		// bounding rectangle
		coordMin = carthesianToPolar(tree.minCoordinate(), proj);
		coordMax = carthesianToPolar(tree.maxCoordinate(), proj);
	}

	private InRamCoordinateIndex(NearestNeighbor2DTree tree, Projection proj, int size,
			PolarCoordinate cMin, PolarCoordinate cMax) {
		this.tree = tree;
		this.projection = proj;
		this.size = size;
		this.coordMin = cMin;
		this.coordMax = cMax;
	}

	public static InRamCoordinateIndex readFrom(InputStream iStream) throws IOException,
			ClassNotFoundException {
		NearestNeighbor2DTree tree = Serializer.deserialize(iStream);
		String[] tmp = Serializer.deserialize(iStream);
		Projection proj = ProjectionFactory.fromPROJ4Specification(tmp);
		Integer size = Serializer.deserialize(iStream);
		PolarCoordinate cMin = Serializer.deserialize(iStream);
		PolarCoordinate cMax = Serializer.deserialize(iStream);
		return new InRamCoordinateIndex(tree, proj, size, cMin, cMax);
	}

	public void writeTo(OutputStream oStream) throws IOException {
		Serializer.serialize(oStream, tree);
		Serializer.serialize(oStream, projection.getPROJ4Description().split(" "));
		Serializer.serialize(oStream, new Integer(size));
		Serializer.serialize(oStream, coordMin);
		Serializer.serialize(oStream, coordMax);
	}

	public PolarCoordinate getMaxCoordinate() {
		return coordMax;
	}

	public PolarCoordinate getMinCoordinate() {
		return coordMin;
	}

	public int getNearestNeighborIdx(PolarCoordinate c) {
		return tree.nearestNeighborIdx(polarToCarthesian(c, projection));
	}

	public PolarCoordinate getPolarCoordinate(int idx) {
		return carthesianToPolar(tree.getPoint(idx), projection);
	}

	public LinkedList<Integer> getCoordianteIndicesWithinRectangle(PolarCoordinate cMin,
			PolarCoordinate cMax) {
		return tree.getPointsWithinRectangle(polarToCarthesian(cMin, projection),
				polarToCarthesian(cMax, projection));
	}

	public int size() {
		return size;
	}

	private CarthesianPoint polarToCarthesian(PolarCoordinate c, Projection proj) {
		double[] tmp = new double[] { c.getLatitudeDouble(), c.getLongitudeDouble() };
		proj.transform(tmp, 0, tmp, 0, 1);
		return new CarthesianPoint((int) Math.rint(tmp[1] * FAC), (int) Math.rint(tmp[0] * FAC));
	}

	private PolarCoordinate carthesianToPolar(CarthesianPoint c, Projection proj) {
		double[] tmp = new double[] { c.y / FAC, c.x / FAC };
		proj.inverseTransform(tmp, 0, tmp, 0, 1);
		return new PolarCoordinate(tmp[1], tmp[0]);
	}

	public static InRamCoordinateIndex getFromHHDb(Connection conn, Projection proj)
			throws SQLException {
		HHDbReader reader = new HHDbReader(conn);
		int[] lon = new int[reader.numVertices()];
		int[] lat = new int[reader.numVertices()];

		for (Iterator<HHVertex> iter = reader.getVertices(); iter.hasNext();) {
			HHVertex v = iter.next();
			lon[v.id] = PolarCoordinate.double2Int(v.longitude);
			lat[v.id] = PolarCoordinate.double2Int(v.latitude);
		}

		InRamCoordinateIndex index = new InRamCoordinateIndex(lon, lat, proj);
		return index;
	}
}
