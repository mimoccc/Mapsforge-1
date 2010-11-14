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
enum PoiEnum {
	AEROWAY_GATE((byte) 17),
	AEROWAY_HELIPAD((byte) 17),
	AMENITY_ATM((byte) 17),
	AMENITY_BANK((byte) 17),
	AMENITY_BICYCLE_RENTAL((byte) 17),
	AMENITY_BUS_STATION((byte) 16),
	AMENITY_CAFE((byte) 17),
	AMENITY_CINEMA((byte) 17),
	AMENITY_FAST_FOOD((byte) 17),
	AMENITY_FOUNTAIN((byte) 16),
	AMENITY_FIRE_STATION((byte) 17),
	AMENITY_FUEL((byte) 17),
	AMENITY_HOSPITAL((byte) 16),
	AMENITY_LIBRARY((byte) 17),
	AMENITY_PARKING((byte) 17),
	AMENITY_PHARMACY((byte) 17),
	AMENITY_PLACE_OF_WORSHIP((byte) 17),
	AMENITY_POLICE((byte) 17),
	AMENITY_POST_BOX((byte) 17),
	AMENITY_POST_OFFICE((byte) 17),
	AMENITY_PUB((byte) 17),
	AMENITY_RECYCLING((byte) 17),
	AMENITY_RESTAURANT((byte) 17),
	AMENITY_SCHOOL((byte) 17),
	AMENITY_SHELTER((byte) 16),
	AMENITY_TELEPHONE((byte) 17),
	AMENITY_THEATRE((byte) 17),
	AMENITY_TOILETS((byte) 17),
	AMENITY_UNIVERSITY((byte) 17),
	BARRIER_BOLLARD((byte) 16),
	BARRIER_CYCLE_BARRIER((byte) 17),
	EMERGENCY_PHONE((byte) 17),
	HIGHWAY_BUS_STOP((byte) 16),
	HIGHWAY_MINI_ROUNDABOUT((byte) 17),
	HIGHWAY_TRAFFIC_SIGNALS((byte) 17),
	HIGHWAY_TURNING_CIRCLE((byte) 17),
	HISTORIC_MEMORIAL((byte) 17),
	HISTORIC_MONUMENT((byte) 17),
	LEISURE_PLAYGROUND((byte) 17),
	LEISURE_SLIPWAY((byte) 17),
	MAN_MADE_LIGHTHOUSE((byte) 17),
	MAN_MADE_SURVEILLANCE((byte) 17),
	MAN_MADE_TOWER((byte) 17),
	MAN_MADE_WINDMILL((byte) 17),
	NATURAL_PEAK((byte) 15),
	NATURAL_SPRING((byte) 17),
	NATURAL_TREE((byte) 17),
	PLACE_CITY((byte) 8),
	PLACE_ISLAND((byte) 12),
	PLACE_SUBURB((byte) 14),
	PLACE_TOWN((byte) 9),
	PLACE_VILLAGE((byte) 14),
	POWER_GENERATOR((byte) 17),
	POWER_TOWER((byte) 17),
	RAILWAY_HALT((byte) 17),
	RAILWAY_LEVEL_CROSSING((byte) 16),
	RAILWAY_STATION((byte) 15),
	RAILWAY_TRAM_STOP((byte) 17),
	SHOP_BAKERY((byte) 17),
	SHOP_HAIRDRESSER((byte) 17),
	SHOP_ORGANIC((byte) 17),
	SHOP_SUPERMARKET((byte) 17),
	STATION_LIGHT_RAIL((byte) 17),
	STATION_SUBWAY((byte) 17),
	TOURISM_ATTRACTION((byte) 17),
	TOURISM_HOSTEL((byte) 17),
	TOURISM_HOTEL((byte) 17),
	TOURISM_INFORMATION((byte) 17),
	TOURISM_MUSEUM((byte) 17),
	TOURISM_VIEWPOINT((byte) 15);

	private final byte zoomlevel;

	private PoiEnum(byte zoomlevel) {
		this.zoomlevel = zoomlevel;
	}

	public byte zoomlevel() {
		return zoomlevel;
	}

	@Override
	public String toString() {
		String name = name();
		int startValue = name.indexOf("_");
		String key = name.substring(0, startValue).toLowerCase(Locale.US);
		String value = name.substring(startValue + 1, name.length()).toLowerCase(Locale.US);
		return key + "=" + value;
	}

	private static final Map<String, PoiEnum> stringToEnum =
			new HashMap<String, PoiEnum>();

	static {
		for (PoiEnum poi : values()) {
			stringToEnum.put(poi.toString(), poi);
		}
	}

	public static PoiEnum fromString(String symbol) {
		return stringToEnum.get(symbol);
	}

}
