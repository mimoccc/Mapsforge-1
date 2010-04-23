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
package org.mapsforge.preprocessing.model.impl;

import java.util.HashSet;
import java.util.Set;

import org.mapsforge.core.conf.IVehicle;
import org.mapsforge.core.conf.Vehicle;
import org.mapsforge.preprocessing.model.EHighwayLevel;
import org.mapsforge.preprocessing.model.Edge;
import org.mapsforge.preprocessing.model.IHighwayLevel2Speed;
import org.mapsforge.preprocessing.model.ITransportConfigurationPreprocessing;
import org.mapsforge.preprocessing.util.MaxSpeedMapper;

/**
 * <h2>Implementation details</h2> <i>(no unchangeability guarantee)</i><br/>
 * This enum uses the Decorator Pattern to achieve augmented functionality used only by
 * preprocessing methods.
 * 
 * @author till
 * 
 */
public enum TransportConfigurationPreprocessing implements ITransportConfigurationPreprocessing {
	STANDARD_CAR__SIMPLE_HEURISTIC(Vehicle.STANDARD_CAR__SIMPLE_HEURISTIC) {
		@Override
		public float getWeight(Edge edge) {

			// this transport cannot use this edge
			if (!this.validHighwayLevels.contains(edge.level))
				return Float.POSITIVE_INFINITY;

			// calculate travel time over edge in seconds
			float speed = calcSpeed(edge);
			float traveltime = (float) (edge.length / (speed / 3.6f));

			// for each traffic light add 5 seconds of traveltime (just a
			// guess...)
			traveltime += 5 * edge.trafficLights;

			// for each stop sign add 10 seconds of traveltime (just a guess...)
			traveltime += 10 * edge.stopSigns;

			// for each crossing add 5 seconds of traveltime (just a guess...)
			traveltime += 5 * edge.crossings;

			return traveltime;
		}
	}, //
	;
	protected final IVehicle base;

	protected final Set<EHighwayLevel> validHighwayLevels;
	protected final MaxSpeedMapper maxSpeedMapper;
	protected final IHighwayLevel2Speed highwayLevel2Speed;

	private TransportConfigurationPreprocessing(IVehicle base) {

		this.base = base;

		validHighwayLevels = new HashSet<EHighwayLevel>();
		validHighwayLevels.add(EHighwayLevel.motorway);
		validHighwayLevels.add(EHighwayLevel.motorway_link);
		validHighwayLevels.add(EHighwayLevel.trunk);
		validHighwayLevels.add(EHighwayLevel.trunk_link);
		validHighwayLevels.add(EHighwayLevel.primary);
		validHighwayLevels.add(EHighwayLevel.primary_link);
		validHighwayLevels.add(EHighwayLevel.secondary);
		validHighwayLevels.add(EHighwayLevel.tertiary);
		validHighwayLevels.add(EHighwayLevel.residential);
		validHighwayLevels.add(EHighwayLevel.living_street);
		validHighwayLevels.add(EHighwayLevel.road);

		this.maxSpeedMapper = new MaxSpeedMapper();
		this.highwayLevel2Speed = new DEHighwayLevel2Speed();
	}

	// private TransportConfigurationPreprocessing(String configfile){
	// //TODO load valid highways from configfile
	// throw new UnsupportedOperationException();
	// }
	//	

	// @Override
	// public abstract float getWeight(Way edge);

	protected final float calcSpeed(Edge edge) {
		return Math.min(calcEdgeSpeed(edge), this.getMaxSpeed());
	}

	private float calcEdgeSpeed(Edge edge) {
		float maxspeed = maxSpeedMapper.translate(edge.maxspeed);
		if (maxspeed > 0)
			return maxspeed;

		// if(edge.urban != null && edge.urban)
		// return 50;

		return highwayLevel2Speed.speed(edge.level);
	}

	@Override
	public int getAvgSpeed() {
		return this.base.getAvgSpeed();
	}

	@Override
	public int getMaxSpeed() {
		return this.base.getMaxSpeed();
	}

	@Override
	public int getCost(int distance) {
		return this.base.getCost(distance);
	}

	@Override
	public float getWeight(Edge edge) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isValidHighwayLevel(String level) {

		return true;
	}

}
