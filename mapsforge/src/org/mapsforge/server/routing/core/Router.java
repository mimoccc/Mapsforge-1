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

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.reflect.Constructor;
import java.sql.Date;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import org.mapsforge.core.conf.IVehicle;
import org.mapsforge.core.conf.Vehicle;
import org.mapsforge.server.core.geoinfo.IPoint;
import org.mapsforge.server.core.geoinfo.Point;

// TODO: IDEE: feasibility check in Router-klasse: danach komponenten in datei
//schreiben und mit hashs versehen, diese hashs sind in den jeweiligen anderen
//Komponenten in einer Liste (nicht in hash-berechnung enthalten) von hashs der
//f�r valide erkl�rten anderen komponenten enthalten. 
public abstract class Router {

	public static enum Message {
		COMPUTED_SECTION_VERTICES, CREATE_POINTINDEX_FINISH, CREATE_POINTINDEX_START, CREATE_POINTMAP_FINISH, CREATE_POINTMAP_START, CREATE_ROUTER, CREATE_ROUTER_START, POINT_TO_WAYPOINT, POINTS_TO_WAYPOINTS, ROUTER_IMPLEMENTATION,
	}

	public static enum Property implements IProperty {
		DB__DATABASE, DB__HOST, DB__JDBC, DB__JDBC_DRIVER, DB__PASSWORD, DB__USE_AS_INPUT, DB__USER, GM__DATAINPUT, GM__DATAOUTPUT, GM__STREETMAP_DATE, GM__STREETMAP_NAME, GM__TYPE, R__TYPE, RG__DATAINPUT, RG__DATAOUTPUT, RG__TYPE, V__VEHICLE_NAME,
	}

	protected static final Logger LOGGER = Logger.getLogger(Router.class.getName());

	private static final ResourceBundle DEFAULTS_BUNDLE = ResourceBundle
			.getBundle("org.mapsforge.server.routing.core.defaults"); //$NON-NLS-1$

	private static final String EMPTY_STRING = ""; //$NON-NLS-1$

	private static final String NEWINSTANCE = "newInstance"; //$NON-NLS-1$

	public static final <R extends Router> Router newInstance(Class<R> routerType,
			IVehicle vehicle, IRoutingGraph routingGraph, IGeoMap geoMap,
			PropertyConfiguration<?> propConf) throws ComponentInitializationException {
		try {
			Constructor<R> c = routerType.getConstructor(IVehicle.class, IRoutingGraph.class,
					IGeoMap.class, PropertyConfiguration.class);

			return c.newInstance(vehicle, routingGraph, geoMap, propConf);
		} catch (Exception exc) {
			throw new ComponentInitializationException(Issue.R__CONSTRUCTOR_PROBLEM_EXC.msg(),
					exc);
		}
	}

	public static Router newInstance(Properties props) throws ComponentInitializationException {
		long timer = -System.currentTimeMillis();
		LOGGER.info(String.format(Messages.getString(Message.CREATE_ROUTER_START)));

		/**
		 * get the default values given through the Defaults class as Properties
		 */
		Properties defProps = new Properties();
		try {
			Enumeration<String> keys = DEFAULTS_BUNDLE.getKeys();
			while (keys.hasMoreElements()) {
				String key = keys.nextElement();
				defProps.setProperty(key, DEFAULTS_BUNDLE.getString(key));
			}
		} catch (Exception exc) {
			throw new ComponentInitializationException(Issue.DEFAULTS_NOT_ACCESSIBLE.msg(), exc);
		}

		/** create a copy of the Properties provided, but use the defaults */
		Properties newProps = new Properties();
		newProps.putAll(defProps);
		newProps.putAll(props);

		Router res = newInstance(PropertyConfiguration.newInstance(newProps, Property.class));

		timer += System.currentTimeMillis();
		LOGGER.info(String.format(Messages.getString(Message.CREATE_ROUTER), (int) Math
				.ceil(timer / 1000.0), res.getClass().getCanonicalName(), res.getRoutingGraph()
				.getNVertices()));

		return res;
	}

	public static Router newInstance(PropertyConfiguration<Property> propConf)
			throws ComponentInitializationException {

		/** create a ConnectionHandler to be used by all database using objects */
		ConnectionHandler conHlr = null;
		if (Boolean.valueOf(propConf.get(Property.DB__USE_AS_INPUT))) {
			/** register Driver for DB connection */
			String driverName = propConf.get(Property.DB__JDBC_DRIVER);
			if (driverName != null && !driverName.equals(EMPTY_STRING))
				try {
					DriverManager.registerDriver((Driver) Class.forName(driverName)
							.newInstance());
				} catch (Exception exc) {
					throw new ComponentInitializationException(
							Issue.DB_DRIVER_NOT_INSTANTIABLE_EXC.msg(), exc);
				}

			/** build connection string to be used for connection setup */
			StringBuilder conSb = new StringBuilder("jdbc:"); //$NON-NLS-1$
			conSb.append(propConf.get(Property.DB__JDBC));
			conSb.append("://"); //$NON-NLS-1$
			conSb.append(propConf.get(Property.DB__HOST));
			conSb.append("/"); //$NON-NLS-1$
			conSb.append(propConf.get(Property.DB__DATABASE));

			/** setup DB connection parameters */
			String conStr = conSb.toString();
			String conUsr = propConf.get(Property.DB__USER);
			String conPwd = propConf.get(Property.DB__PASSWORD);
			conHlr = new ConnectionHandler(conStr, conUsr, conPwd);
		}

		/** create the Vehicle to be used */
		IVehicle vehicle = createVehicle(propConf);

		/** create the RoutingGraph to be used */
		IRoutingGraph routingGraph = createRoutingGraph(vehicle, propConf, conHlr);

		/** create the RoutingGraph to be used */
		IGeoMap geoMap = createGeoMap(routingGraph, propConf, conHlr);

		/** dynamically determine Router's type */
		Class<? extends Router> routerType;
		try {
			Class<?> routerTypeUnchecked = Class.forName(propConf.get(Property.R__TYPE));
			routerType = routerTypeUnchecked.asSubclass(Router.class);
		} catch (Exception exc) {
			throw new ComponentInitializationException(Issue.R__ROUTER_NOT_INSTANTIABLE.msg(),
					exc);
		}

		return newInstance(routerType, vehicle, routingGraph, geoMap, propConf);
	}

	private static IGeoMap createGeoMap(IRoutingGraph routingGraph,
			PropertyConfiguration<Property> pptyConf, ConnectionHandler conHlr)
			throws ComponentInitializationException {

		/** dynamically determine GeoMap's type */
		Class<? extends IGeoMap> geoMapType;
		try {
			Class<?> typeUnchecked = Class.forName(pptyConf.get(Property.GM__TYPE));
			geoMapType = typeUnchecked.asSubclass(IGeoMap.class);
		} catch (Exception exc) {
			throw new ComponentInitializationException(Issue.GM__GEOMAP_INSTANTIATION.msg());
		}

		/** get name of the street map */
		String name = pptyConf.get(Property.GM__STREETMAP_NAME);
		if (name == null || name.equals(EMPTY_STRING))
			throw new ComponentInitializationException(Issue.GM__NAME_NOT_CONFGD.msg());

		/** get the release date of the street map data */
		Date date;
		try {
			date = Date.valueOf(pptyConf.get(Property.GM__STREETMAP_DATE));
		} catch (Exception exc) {
			throw new ComponentInitializationException(Issue.GM__DATE_NOT_CONFGD_PRPLY.msg(),
					exc);
		}
		if (date == null)
			throw new ComponentInitializationException(Issue.GM__DATE_NOT_CONFGD_PRPLY.msg());

		/** get the data input if defined to be used */
		DataInput in = null;
		String inFilename = pptyConf.get(Property.GM__DATAINPUT);
		if (inFilename != null && !inFilename.equals(EMPTY_STRING)) {
			/** if preferred input type is file and the one specified exists */
			try {
				in = new DataInputStream(new FileInputStream(inFilename));
			} catch (FileNotFoundException exc) {
				throw new ComponentInitializationException(Issue.GM__DATAINPUT_NOT_CONFGD_PRPLY
						.msg(), exc);
			}
		}

		/** get the data output if defined to be used */
		DataOutput out = null;
		String outFilename = pptyConf.get(Property.GM__DATAOUTPUT);
		if (outFilename != null && !outFilename.equals(EMPTY_STRING)) {
			/** if preferred input type is file and the one specified exists */
			try {
				out = new DataOutputStream(new FileOutputStream(outFilename));
			} catch (FileNotFoundException exc) {
				throw new ComponentInitializationException(
						Issue.GM__DATAOUTPUT_NOT_CONFGD_PRPLY.msg(), exc);
			}
		}

		/** switch on the type of the GeoMap to be created */
		IGeoMap geoMap = null;
		try {
			if (geoMapType.equals(SqlBackedGeoMap.class))
				geoMap = SqlBackedGeoMap.newInstance(name, date, conHlr, pptyConf);
			else if (geoMapType.equals(ArrayBasedGeoMap.class))
				geoMap = ArrayBasedGeoMap.newInstance(name, date, conHlr, pptyConf, in, out,
						routingGraph);
			else
				/** try creating GeoMap using "newInstance" factory method */
				geoMap = (IGeoMap) geoMapType.getDeclaredMethod(NEWINSTANCE, String.class,
						Date.class, ConnectionHandler.class, PropertyConfiguration.class,
						DataInput.class, DataOutput.class, IRoutingGraph.class).invoke(null,
						name, date, conHlr, pptyConf, in, out, routingGraph);
		} catch (Exception exc) {
			throw new ComponentInitializationException(Issue.GM__CONSTRUCTOR_NOT_APPLICABLE
					.msg(), exc);
		}

		// TODO: use the routingGraph's data to check geoMap (!!!) if assertion
		// is switched on (!!!)

		return geoMap;
	}

	private static IRoutingGraph createRoutingGraph(IVehicle vehicle,
			PropertyConfiguration<Property> pptyConf, ConnectionHandler conHlr)
			throws ComponentInitializationException {

		/** dynamically determine RoutingGraph type */
		Class<? extends IRoutingGraph> rgType;
		try {
			Class<?> typeUnchecked = Class.forName(pptyConf.get(Property.RG__TYPE));
			rgType = typeUnchecked.asSubclass(IRoutingGraph.class);
		} catch (Exception exc) {
			throw new ComponentInitializationException(Issue.RG__ROUTINGGRAPH_INSTANTIATION
					.msg());
		}

		/** get the data input if defined to be used */
		DataInput in = null;
		String inFilename = pptyConf.get(Property.RG__DATAINPUT);
		if (inFilename != null && !inFilename.equals(EMPTY_STRING)) {
			/** if preferred input type is file and the one specified exists */
			try {
				in = new DataInputStream(new FileInputStream(inFilename));
			} catch (FileNotFoundException exc) {
				throw new ComponentInitializationException(Issue.RG__DATAINPUT_NOT_CONFGD_PRPLY
						.msg(), exc);
			}
		}

		/** get the data output if defined to be used */
		DataOutput out = null;
		String outFilename = pptyConf.get(Property.RG__DATAOUTPUT);
		if (outFilename != null && !outFilename.equals(EMPTY_STRING)) {
			/** if preferred input type is file and the one specified exists */
			try {
				out = new DataOutputStream(new FileOutputStream(outFilename));
			} catch (FileNotFoundException exc) {
				throw new ComponentInitializationException(
						Issue.RG__DATAOUTPUT_NOT_CONFGD_PRPLY.msg(), exc);
			}
		}

		/** switch on the type of the GeoMap to be created */
		IRoutingGraph routingGraph = null;
		try {
			/** try creating RoutingGraph using "newInstance"-method */
			routingGraph = (IRoutingGraph) rgType.getDeclaredMethod(NEWINSTANCE,
					PropertyConfiguration.class, ConnectionHandler.class, DataInput.class,
					DataOutput.class, IVehicle.class).invoke(null, pptyConf, conHlr, in, out,
					vehicle);
		} catch (Exception exc) {
			throw new ComponentInitializationException(Issue.RG__VALUEOF_NOT_APPLICABLE.msg(),
					exc);
		}

		return routingGraph;
	}

	private static IVehicle createVehicle(PropertyConfiguration<Property> pptyConf)
			throws ComponentInitializationException {

		/** create reference to a Vehicle as defined in PropertyConfiguration */
		IVehicle vehicle = Vehicle.valueOf(pptyConf.get(Property.V__VEHICLE_NAME));
		if (vehicle == null)
			throw new ComponentInitializationException(Issue.V__NOT_EXISTENT.msg());
		return vehicle;

	}

	private final IGeoMap geoMap;

	private final IVehicle vehicle;

	protected Router(IGeoMap geoMap, IVehicle vehicle) {
		this.geoMap = geoMap;
		this.vehicle = vehicle;
	}

	public abstract String getAlgorithmName();

	public final IGeoMap getGeoMap() {
		return this.geoMap;
	}

	public abstract IRoutingGraph getRoutingGraph();

	public final IVehicle getVehicle() {
		return this.vehicle;
	}

	public final Route route(List<? extends IPoint> initialRouteNodes) {
		int nRouteNodes = initialRouteNodes.size();

		// DEBUG ONLY:
		long timer = -System.currentTimeMillis();

		/** get nearest points on ways */
		List<Point> wayPoints = new ArrayList<Point>(nRouteNodes);
		for (IPoint point : initialRouteNodes) {
			Point wayPoint = getGeoMap().getWayPoint(Point.getInstance(point));
			wayPoints.add(wayPoint);

			// DEBUG ONLY:
			LOGGER.info(String.format(Messages.getString(Message.POINT_TO_WAYPOINT), point,
					wayPoint));
		}
		// DEBUG ONLY:
		LOGGER.info(String.format(Messages.getString(Message.POINTS_TO_WAYPOINTS), timer
				+ System.currentTimeMillis()));

		/** compute the Route */
		Route route = new Route(getGeoMap(), getRoutingGraph());
		for (int i = 0; i < nRouteNodes - 1; i++) {
			Point source = wayPoints.get(i);
			Point destination = wayPoints.get(i + 1);
			// DEBUG ONLY:
			timer = -System.currentTimeMillis();
			int[] routeVtcs = this.route(source, destination);

			// DEBUG ONLY:
			LOGGER.info(String.format(Messages.getString(Message.COMPUTED_SECTION_VERTICES),
					timer + System.currentTimeMillis()));

			/** add new section to the route */
			route.addSection(source, routeVtcs, destination);
		}

		return route;
	}

	public final Route route(Route alternativeRoute) {
		/**
		 * TODO: implement finding alternative routes
		 * */
		return null;
	}

	protected abstract int[] route(Point source, Point destination);

}