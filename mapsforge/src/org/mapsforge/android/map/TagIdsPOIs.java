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
package org.mapsforge.android.map;

import java.util.HashMap;
import java.util.Map;

/**
 * List of all supported OSM tags for POIs. Each tag has a unique ID and is declared as static
 * final byte to speed up the rendering process.
 */
final class TagIdsPOIs {
	static final byte AEROWAY$HELIPAD = 0;
	static final byte AMENITY$ATM = 1;
	static final byte AMENITY$BANK = 2;
	static final byte AMENITY$BICYCLE_RENTAL = 3;
	static final byte AMENITY$BUS_STATION = 4;
	static final byte AMENITY$CAFE = 5;
	static final byte AMENITY$CINEMA = 6;
	static final byte AMENITY$FAST_FOOD = 7;
	static final byte AMENITY$FOUNTAIN = 8;
	static final byte AMENITY$FIRE_STATION = 9;
	static final byte AMENITY$FUEL = 10;
	static final byte AMENITY$HOSPITAL = 11;
	static final byte AMENITY$LIBRARY = 12;
	static final byte AMENITY$PARKING = 13;
	static final byte AMENITY$PHARMACY = 14;
	static final byte AMENITY$PLACE_OF_WORSHIP = 15;
	static final byte AMENITY$POST_BOX = 16;
	static final byte AMENITY$POST_OFFICE = 17;
	static final byte AMENITY$PUB = 18;
	static final byte AMENITY$RECYCLING = 19;
	static final byte AMENITY$RESTAURANT = 20;
	static final byte AMENITY$SCHOOL = 21;
	static final byte AMENITY$SHELTER = 22;
	static final byte AMENITY$TELEPHONE = 23;
	static final byte AMENITY$THEATRE = 24;
	static final byte AMENITY$TOILETS = 25;
	static final byte AMENITY$UNIVERSITY = 26;
	static final byte BARRIER$BOLLARD = 27;
	static final byte HIGHWAY$BUS_STOP = 28;
	static final byte HIGHWAY$TRAFFIC_SIGNALS = 29;
	static final byte HISTORIC$MEMORIAL = 30;
	static final byte HISTORIC$MONUMENT = 31;
	static final byte LEISURE$PLAYGROUND = 32;
	static final byte MAN_MADE$WINDMILL = 33;
	static final byte NATURAL$PEAK = 34;
	static final byte PLACE$CITY = 35;
	static final byte PLACE$ISLAND = 36;
	static final byte PLACE$SUBURB = 37;
	static final byte PLACE$TOWN = 38;
	static final byte PLACE$VILLAGE = 39;
	static final byte RAILWAY$HALT = 40;
	static final byte RAILWAY$LEVEL_CROSSING = 41;
	static final byte RAILWAY$STATION = 42;
	static final byte RAILWAY$TRAM_STOP = 43;
	static final byte SHOP$BAKERY = 44;
	static final byte SHOP$ORGANIC = 45;
	static final byte SHOP$SUPERMARKET = 46;
	static final byte STATION$LIGHT_RAIL = 47;
	static final byte STATION$SUBWAY = 48;
	static final byte TOURISM$ATTRACTION = 49;
	static final byte TOURISM$HOSTEL = 50;
	static final byte TOURISM$HOTEL = 51;
	static final byte TOURISM$INFORMATION = 52;
	static final byte TOURISM$MUSEUM = 53;
	static final byte TOURISM$VIEWPOINT = 54;

	static final Map<String, Byte> getMap() {
		Map<String, Byte> map = new HashMap<String, Byte>();
		map.put("aeroway=helipad", Byte.valueOf((byte) 0));
		map.put("amenity=atm", Byte.valueOf((byte) 1));
		map.put("amenity=bank", Byte.valueOf((byte) 2));
		map.put("amenity=bicycle_rental", Byte.valueOf((byte) 3));
		map.put("amenity=bus_station", Byte.valueOf((byte) 4));
		map.put("amenity=cafe", Byte.valueOf((byte) 5));
		map.put("amenity=cinema", Byte.valueOf((byte) 6));
		map.put("amenity=fast_food", Byte.valueOf((byte) 7));
		map.put("amenity=fire_station", Byte.valueOf((byte) 9));
		map.put("amenity=fountain", Byte.valueOf((byte) 8));
		map.put("amenity=fuel", Byte.valueOf((byte) 10));
		map.put("amenity=hospital", Byte.valueOf((byte) 11));
		map.put("amenity=library", Byte.valueOf((byte) 12));
		map.put("amenity=parking", Byte.valueOf((byte) 13));
		map.put("amenity=pharmacy", Byte.valueOf((byte) 14));
		map.put("amenity=place_of_worship", Byte.valueOf((byte) 15));
		map.put("amenity=post_box", Byte.valueOf((byte) 16));
		map.put("amenity=post_office", Byte.valueOf((byte) 17));
		map.put("amenity=pub", Byte.valueOf((byte) 18));
		map.put("amenity=recycling", Byte.valueOf((byte) 19));
		map.put("amenity=restaurant", Byte.valueOf((byte) 20));
		map.put("amenity=school", Byte.valueOf((byte) 21));
		map.put("amenity=shelter", Byte.valueOf((byte) 22));
		map.put("amenity=telephone", Byte.valueOf((byte) 23));
		map.put("amenity=theatre", Byte.valueOf((byte) 24));
		map.put("amenity=toilets", Byte.valueOf((byte) 25));
		map.put("amenity=university", Byte.valueOf((byte) 26));
		map.put("barrier=bollard", Byte.valueOf((byte) 27));
		map.put("highway=bus_stop", Byte.valueOf((byte) 28));
		map.put("highway=traffic_signals", Byte.valueOf((byte) 29));
		map.put("historic=memorial", Byte.valueOf((byte) 30));
		map.put("historic=monument", Byte.valueOf((byte) 31));
		map.put("leisure=playground", Byte.valueOf((byte) 32));
		map.put("man_made=windmill", Byte.valueOf((byte) 33));
		map.put("natural=peak", Byte.valueOf((byte) 34));
		map.put("place=city", Byte.valueOf((byte) 35));
		map.put("place=island", Byte.valueOf((byte) 36));
		map.put("place=suburb", Byte.valueOf((byte) 37));
		map.put("place=town", Byte.valueOf((byte) 38));
		map.put("place=village", Byte.valueOf((byte) 39));
		map.put("railway=halt", Byte.valueOf((byte) 40));
		map.put("railway=level_crossing", Byte.valueOf((byte) 41));
		map.put("railway=station", Byte.valueOf((byte) 42));
		map.put("railway=tram_stop", Byte.valueOf((byte) 43));
		map.put("shop=bakery", Byte.valueOf((byte) 44));
		map.put("shop=organic", Byte.valueOf((byte) 45));
		map.put("shop=supermarket", Byte.valueOf((byte) 46));
		map.put("station=light_rail", Byte.valueOf((byte) 47));
		map.put("station=subway", Byte.valueOf((byte) 48));
		map.put("tourism=attraction", Byte.valueOf((byte) 49));
		map.put("tourism=hostel", Byte.valueOf((byte) 50));
		map.put("tourism=hotel", Byte.valueOf((byte) 51));
		map.put("tourism=information", Byte.valueOf((byte) 52));
		map.put("tourism=museum", Byte.valueOf((byte) 53));
		map.put("tourism=viewpoint", Byte.valueOf((byte) 54));
		return map;
	}
}