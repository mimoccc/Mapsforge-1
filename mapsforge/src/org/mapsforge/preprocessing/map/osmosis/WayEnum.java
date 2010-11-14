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
package org.mapsforge.preprocessing.map.osmosis;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 
 * @author bross
 * 
 */
enum WayEnum {
	ADMIN_LEVEL_2(WayType.UNCLASSIFIED, (byte) 6),
	ADMIN_LEVEL_4(WayType.UNCLASSIFIED, (byte) 12),
	ADMIN_LEVEL_6(WayType.UNCLASSIFIED, (byte) 15),
	ADMIN_LEVEL_8(WayType.UNCLASSIFIED, (byte) 16),
	ADMIN_LEVEL_9(WayType.UNCLASSIFIED, (byte) 16),
	ADMIN_LEVEL_10(WayType.UNCLASSIFIED, (byte) 16),
	AERIALWAY_CABLE_CAR(WayType.UNCLASSIFIED, (byte) 16),
	AERIALWAY_CHAIR_LIFT(WayType.UNCLASSIFIED, (byte) 16),
	AEROWAY_AERODROME(WayType.UNCLASSIFIED, (byte) 13),
	AEROWAY_APRON(WayType.UNCLASSIFIED, (byte) 13),
	AEROWAY_HELIPAD(WayType.UNCLASSIFIED, (byte) 17),
	AEROWAY_RUNWAY(WayType.UNCLASSIFIED, (byte) 10),
	AEROWAY_TAXIWAY(WayType.UNCLASSIFIED, (byte) 10),
	AEROWAY_TERMINAL(WayType.UNCLASSIFIED, (byte) 16),
	AMENITY_COLLEGE(WayType.AMENITY, (byte) 15),
	AMENITY_EMBASSY(WayType.AMENITY, (byte) 15),
	AMENITY_FOUNTAIN(WayType.AMENITY, (byte) 15),
	AMENITY_GRAVE_YARD(WayType.AMENITY, (byte) 15),
	AMENITY_HOSPITAL(WayType.AMENITY, (byte) 15),
	AMENITY_PARKING(WayType.AMENITY, (byte) 15),
	AMENITY_SCHOOL(WayType.AMENITY, (byte) 15),
	AMENITY_UNIVERSITY(WayType.AMENITY, (byte) 15),
	AREA_YES(WayType.UNCLASSIFIED, (byte) 127),
	BARRIER_FENCE(WayType.UNCLASSIFIED, (byte) 16),
	BARRIER_WALL(WayType.UNCLASSIFIED, (byte) 17),
	BOUNDARY_ADMINISTRATIVE(WayType.UNCLASSIFIED, (byte) 127),
	BOUNDARY_NATIONAL_PARK(WayType.UNCLASSIFIED, (byte) 12),
	BRIDGE_YES(WayType.UNCLASSIFIED, (byte) 127),
	BUILDING_APARTMENTS(WayType.BUILDING, (byte) 16),
	BUILDING_EMBASSY(WayType.BUILDING, (byte) 16),
	BUILDING_GOVERNMENT(WayType.BUILDING, (byte) 16),
	BUILDING_GYM(WayType.BUILDING, (byte) 16),
	BUILDING_ROOF(WayType.BUILDING, (byte) 16),
	BUILDING_SPORTS(WayType.BUILDING, (byte) 16),
	BUILDING_TRAIN_STATION(WayType.BUILDING, (byte) 16),
	BUILDING_UNIVERSITY(WayType.BUILDING, (byte) 16),
	BUILDING_YES(WayType.BUILDING, (byte) 16),
	HIGHWAY_BRIDLEWAY(WayType.HIGHWAY, (byte) 13),
	HIGHWAY_BUS_GUIDEWAY(WayType.HIGHWAY, (byte) 14),
	HIGHWAY_CONSTRUCTION(WayType.HIGHWAY, (byte) 15),
	HIGHWAY_CYCLEWAY(WayType.HIGHWAY, (byte) 13),
	HIGHWAY_FOOTWAY(WayType.HIGHWAY, (byte) 15),
	HIGHWAY_LIVING_STREET(WayType.HIGHWAY, (byte) 14),
	HIGHWAY_MOTORWAY(WayType.HIGHWAY, (byte) 8),
	HIGHWAY_MOTORWAY_LINK(WayType.HIGHWAY, (byte) 8),
	HIGHWAY_PATH(WayType.HIGHWAY, (byte) 14),
	HIGHWAY_PEDESTRIAN(WayType.HIGHWAY, (byte) 14),
	HIGHWAY_PRIMARY(WayType.HIGHWAY, (byte) 8),
	HIGHWAY_PRIMARY_LINK(WayType.HIGHWAY, (byte) 8),
	HIGHWAY_RACEWAY(WayType.HIGHWAY, (byte) 13),
	HIGHWAY_RESIDENTIAL(WayType.HIGHWAY, (byte) 14),
	HIGHWAY_ROAD(WayType.HIGHWAY, (byte) 12),
	HIGHWAY_SECONDARY(WayType.HIGHWAY, (byte) 9),
	HIGHWAY_SERVICE(WayType.HIGHWAY, (byte) 14),
	HIGHWAY_SERVICES(WayType.HIGHWAY, (byte) 14),
	HIGHWAY_STEPS(WayType.HIGHWAY, (byte) 16),
	HIGHWAY_TERTIARY(WayType.HIGHWAY, (byte) 10),
	HIGHWAY_TRACK(WayType.HIGHWAY, (byte) 12),
	HIGHWAY_TRUNK(WayType.HIGHWAY, (byte) 8),
	HIGHWAY_TRUNK_LINK(WayType.HIGHWAY, (byte) 8),
	HIGHWAY_UNCLASSIFIED(WayType.HIGHWAY, (byte) 13),
	HISTORIC_RUINS(WayType.UNCLASSIFIED, (byte) 17),
	LANDUSE_ALLOTMENTS(WayType.LANDUSE, (byte) 12),
	LANDUSE_BASIN(WayType.LANDUSE, (byte) 14),
	LANDUSE_BROWNFIELD(WayType.LANDUSE, (byte) 12),
	LANDUSE_CEMETERY(WayType.LANDUSE, (byte) 12),
	LANDUSE_COMMERCIAL(WayType.LANDUSE, (byte) 12),
	LANDUSE_CONSTRUCTION(WayType.LANDUSE, (byte) 14),
	LANDUSE_FARM(WayType.LANDUSE, (byte) 12),
	LANDUSE_FARMLAND(WayType.LANDUSE, (byte) 12),
	LANDUSE_FOREST(WayType.LANDUSE, (byte) 12),
	LANDUSE_GRASS(WayType.LANDUSE, (byte) 12),
	LANDUSE_GREENFIELD(WayType.LANDUSE, (byte) 12),
	LANDUSE_INDUSTRIAL(WayType.LANDUSE, (byte) 12),
	LANDUSE_MILITARY(WayType.LANDUSE, (byte) 12),
	LANDUSE_QUARRY(WayType.LANDUSE, (byte) 12),
	LANDUSE_RAILWAY(WayType.LANDUSE, (byte) 12),
	LANDUSE_RECREATION_GROUND(WayType.LANDUSE, (byte) 12),
	LANDUSE_RESERVOIR(WayType.LANDUSE, (byte) 12),
	LANDUSE_RESIDENTIAL(WayType.LANDUSE, (byte) 12),
	LANDUSE_RETAIL(WayType.LANDUSE, (byte) 12),
	LANDUSE_VILLAGE_GREEN(WayType.LANDUSE, (byte) 12),
	LANDUSE_VINEYARD(WayType.LANDUSE, (byte) 12),
	LANDUSE_WOOD(WayType.LANDUSE, (byte) 12),
	LEISURE_COMMON(WayType.LEISURE, (byte) 12),
	LEISURE_GARDEN(WayType.LEISURE, (byte) 12),
	LEISURE_GOLF_COURSE(WayType.LEISURE, (byte) 12),
	LEISURE_PARK(WayType.LEISURE, (byte) 12),
	LEISURE_PITCH(WayType.LEISURE, (byte) 15),
	LEISURE_PLAYGROUND(WayType.LEISURE, (byte) 16),
	LEISURE_SPORTS_CENTRE(WayType.LEISURE, (byte) 12),
	LEISURE_STADIUM(WayType.LEISURE, (byte) 12),
	LEISURE_TRACK(WayType.LEISURE, (byte) 15),
	LEISURE_WATER_PARK(WayType.LEISURE, (byte) 15),
	MAN_MADE_PIER(WayType.UNCLASSIFIED, (byte) 15),
	MILITARY_AIRFIELD(WayType.UNCLASSIFIED, (byte) 12),
	MILITARY_BARRACKS(WayType.UNCLASSIFIED, (byte) 12),
	MILITARY_NAVAL_BASE(WayType.UNCLASSIFIED, (byte) 12),
	NATURAL_BEACH(WayType.NATURAL, (byte) 14),
	NATURAL_COASTLINE(WayType.NATURAL, (byte) 0),
	NATURAL_GLACIER(WayType.NATURAL, (byte) 12),
	NATURAL_HEATH(WayType.NATURAL, (byte) 12),
	NATURAL_LAND(WayType.NATURAL, (byte) 12),
	NATURAL_SCRUB(WayType.NATURAL, (byte) 12),
	NATURAL_WATER(WayType.NATURAL, (byte) 12),
	NATURAL_WOOD(WayType.NATURAL, (byte) 12),
	PLACE_LOCALITY(WayType.UNCLASSIFIED, (byte) 17),
	RAILWAY_LIGHT_RAIL(WayType.RAILWAY, (byte) 12),
	RAILWAY_RAIL(WayType.RAILWAY, (byte) 10),
	RAILWAY_STATION(WayType.RAILWAY, (byte) 13),
	RAILWAY_SUBWAY(WayType.RAILWAY, (byte) 13),
	RAILWAY_TRAM(WayType.RAILWAY, (byte) 13),
	ROUTE_FERRY(WayType.UNCLASSIFIED, (byte) 12),
	SPORT_GOLF(WayType.UNCLASSIFIED, (byte) 15),
	SPORT_SHOOTING(WayType.UNCLASSIFIED, (byte) 15),
	SPORT_SOCCER(WayType.UNCLASSIFIED, (byte) 15),
	SPORT_TENNIS(WayType.UNCLASSIFIED, (byte) 15),
	TOURISM_ATTRACTION(WayType.UNCLASSIFIED, (byte) 15),
	TOURISM_HOSTEL(WayType.UNCLASSIFIED, (byte) 15),
	TOURISM_ZOO(WayType.UNCLASSIFIED, (byte) 12),
	TUNNEL_NO(WayType.UNCLASSIFIED, (byte) 127),
	TUNNEL_YES(WayType.UNCLASSIFIED, (byte) 127),
	WATERWAY_CANAL(WayType.WATERWAY, (byte) 12),
	WATERWAY_DAM(WayType.WATERWAY, (byte) 12),
	WATERWAY_DRAIN(WayType.WATERWAY, (byte) 12),
	WATERWAY_RIVER(WayType.WATERWAY, (byte) 12),
	WATERWAY_RIVERBANK(WayType.WATERWAY, (byte) 12),
	WATERWAY_STREAM(WayType.WATERWAY, (byte) 12),
	NUMBER_OF_WAY_TAGS(WayType.UNCLASSIFIED, (byte) 127);

	private final byte zoomlevel;
	private final WayType wayType;
	private static final byte INVALID_ZOOMLEVEL = Byte.MAX_VALUE;

	private WayEnum(WayType wayType, byte zoomlevel) {
		this.zoomlevel = zoomlevel;
		this.wayType = wayType;
	}

	public byte zoomlevel() {
		return zoomlevel;
	}

	public WayType waytype() {
		return wayType;
	}

	public boolean associatedWithValidZoomlevel() {
		return zoomlevel != INVALID_ZOOMLEVEL;
	}

	@Override
	public String toString() {
		String name = name();
		int startValue = name.indexOf("_");
		String key = name.substring(0, startValue).toLowerCase(Locale.US);
		String value = name.substring(startValue + 1, name.length()).toLowerCase(Locale.US);
		return key + "=" + value;
	}

	private static final Map<String, WayEnum> stringToEnum =
			new HashMap<String, WayEnum>();

	static {
		for (WayEnum way : values()) {
			stringToEnum.put(way.toString(), way);
		}
	}

	public static WayEnum fromString(String symbol) {
		return stringToEnum.get(symbol);
	}

	public enum WayType {
		HIGHWAY, RAILWAY, BUILDING, LANDUSE, LEISURE, AMENITY, NATURAL, WATERWAY, UNCLASSIFIED;
	}

}
