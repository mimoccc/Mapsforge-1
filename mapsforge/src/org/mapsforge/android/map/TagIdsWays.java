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
 * List of all supported OSM tags for Ways. Each tag has a unique ID and is declared as static
 * final byte to speed up the rendering process.
 */
final class TagIdsWays {
	static final byte ADMIN_LEVEL$10 = 5;
	static final byte ADMIN_LEVEL$2 = 0;
	static final byte ADMIN_LEVEL$4 = 1;
	static final byte ADMIN_LEVEL$6 = 2;
	static final byte ADMIN_LEVEL$8 = 3;
	static final byte ADMIN_LEVEL$9 = 4;
	static final byte AEROWAY$AERODROME = 6;
	static final byte AEROWAY$APRON = 7;
	static final byte AEROWAY$HELIPAD = 8;
	static final byte AEROWAY$RUNWAY = 9;
	static final byte AEROWAY$TAXIWAY = 10;
	static final byte AEROWAY$TERMINAL = 11;
	static final byte AMENITY$COLLEGE = 12;
	static final byte AMENITY$FOUNTAIN = 13;
	static final byte AMENITY$GRAVE_YARD = 14;
	static final byte AMENITY$HOSPITAL = 15;
	static final byte AMENITY$PARKING = 16;
	static final byte AMENITY$SCHOOL = 17;
	static final byte AMENITY$UNIVERSITY = 18;
	static final byte AREA$YES = 19;
	static final byte BARRIER$FENCE = 20;
	static final byte BARRIER$WALL = 21;
	static final byte BOUNDARY$ADMINISTRATIVE = 22;
	static final byte BOUNDARY$NATIONAL_PARK = 23;
	static final byte BRIDGE$YES = 24;
	static final byte BUILDING$APARTMENTS = 25;
	static final byte BUILDING$EMBASSY = 26;
	static final byte BUILDING$GOVERNMENT = 27;
	static final byte BUILDING$GYM = 28;
	static final byte BUILDING$ROOF = 29;
	static final byte BUILDING$SPORTS = 30;
	static final byte BUILDING$TRAIN_STATION = 31;
	static final byte BUILDING$UNIVERSITY = 32;
	static final byte BUILDING$YES = 33;
	static final byte HIGHWAY$BRIDLEWAY = 34;
	static final byte HIGHWAY$CONSTRUCTION = 35;
	static final byte HIGHWAY$CYCLEWAY = 36;
	static final byte HIGHWAY$FOOTWAY = 37;
	static final byte HIGHWAY$LIVING_STREET = 38;
	static final byte HIGHWAY$MOTORWAY = 39;
	static final byte HIGHWAY$MOTORWAY_LINK = 40;
	static final byte HIGHWAY$PATH = 41;
	static final byte HIGHWAY$PEDESTRIAN = 42;
	static final byte HIGHWAY$PRIMARY = 43;
	static final byte HIGHWAY$PRIMARY_LINK = 44;
	static final byte HIGHWAY$RESIDENTIAL = 45;
	static final byte HIGHWAY$ROAD = 46;
	static final byte HIGHWAY$SECONDARY = 47;
	static final byte HIGHWAY$SERVICE = 48;
	static final byte HIGHWAY$STEPS = 49;
	static final byte HIGHWAY$TERTIARY = 50;
	static final byte HIGHWAY$TRACK = 51;
	static final byte HIGHWAY$TRUNK = 52;
	static final byte HIGHWAY$TRUNK_LINK = 53;
	static final byte HIGHWAY$UNCLASSIFIED = 54;
	static final byte HISTORIC$RUINS = 55;
	static final byte LANDUSE$ALLOTMENTS = 56;
	static final byte LANDUSE$BASIN = 57;
	static final byte LANDUSE$BROWNFIELD = 58;
	static final byte LANDUSE$CEMETERY = 59;
	static final byte LANDUSE$COMMERCIAL = 60;
	static final byte LANDUSE$CONSTRUCTION = 61;
	static final byte LANDUSE$FARM = 62;
	static final byte LANDUSE$FARMLAND = 63;
	static final byte LANDUSE$FOREST = 64;
	static final byte LANDUSE$GRASS = 65;
	static final byte LANDUSE$GREENFIELD = 66;
	static final byte LANDUSE$INDUSTRIAL = 67;
	static final byte LANDUSE$MILITARY = 68;
	static final byte LANDUSE$RECREATION_GROUND = 69;
	static final byte LANDUSE$RESERVOIR = 70;
	static final byte LANDUSE$RESIDENTIAL = 71;
	static final byte LANDUSE$RETAIL = 72;
	static final byte LANDUSE$VILLAGE_GREEN = 73;
	static final byte LANDUSE$WOOD = 74;
	static final byte LEISURE$COMMON = 75;
	static final byte LEISURE$GARDEN = 76;
	static final byte LEISURE$GOLF_COURSE = 77;
	static final byte LEISURE$PARK = 78;
	static final byte LEISURE$PITCH = 79;
	static final byte LEISURE$PLAYGROUND = 80;
	static final byte LEISURE$SPORTS_CENTRE = 81;
	static final byte LEISURE$STADIUM = 82;
	static final byte LEISURE$TRACK = 83;
	static final byte LEISURE$WATER_PARK = 84;
	static final byte MAN_MADE$PIER = 85;
	static final byte MILITARY$AIRFIELD = 86;
	static final byte MILITARY$BARRACKS = 87;
	static final byte MILITARY$NAVAL_BASE = 88;
	static final byte NATURAL$BEACH = 89;
	static final byte NATURAL$COASTLINE = 90;
	static final byte NATURAL$HEATH = 91;
	static final byte NATURAL$LAND = 92;
	static final byte NATURAL$SCRUB = 93;
	static final byte NATURAL$WATER = 94;
	static final byte NATURAL$WOOD = 95;
	static final byte PLACE$LOCALITY = 96;
	static final byte RAILWAY$LIGHT_RAIL = 97;
	static final byte RAILWAY$RAIL = 98;
	static final byte RAILWAY$STATION = 99;
	static final byte RAILWAY$SUBWAY = 100;
	static final byte RAILWAY$TRAM = 101;
	static final byte ROUTE$FERRY = 102;
	static final byte SPORT$SHOOTING = 103;
	static final byte SPORT$TENNIS = 104;
	static final byte TOURISM$ATTRACTION = 105;
	static final byte TOURISM$HOSTEL = 106;
	static final byte TOURISM$ZOO = 107;
	static final byte TUNNEL$NO = 108;
	static final byte TUNNEL$YES = 109;
	static final byte WATERWAY$CANAL = 110;
	static final byte WATERWAY$DRAIN = 111;
	static final byte WATERWAY$RIVER = 112;
	static final byte WATERWAY$RIVERBANK = 113;
	static final byte WATERWAY$STREAM = 114;

	static final Map<String, Byte> getMap() {
		Map<String, Byte> map = new HashMap<String, Byte>();
		map.put("admin_level=10", Byte.valueOf((byte) 5));
		map.put("admin_level=2", Byte.valueOf((byte) 0));
		map.put("admin_level=4", Byte.valueOf((byte) 1));
		map.put("admin_level=6", Byte.valueOf((byte) 2));
		map.put("admin_level=8", Byte.valueOf((byte) 3));
		map.put("admin_level=9", Byte.valueOf((byte) 4));
		map.put("aeroway=aerodrome", Byte.valueOf((byte) 6));
		map.put("aeroway=apron", Byte.valueOf((byte) 7));
		map.put("aeroway=helipad", Byte.valueOf((byte) 8));
		map.put("aeroway=runway", Byte.valueOf((byte) 9));
		map.put("aeroway=taxiway", Byte.valueOf((byte) 10));
		map.put("aeroway=terminal", Byte.valueOf((byte) 11));
		map.put("amenity=college", Byte.valueOf((byte) 12));
		map.put("amenity=fountain", Byte.valueOf((byte) 13));
		map.put("amenity=grave_yard", Byte.valueOf((byte) 14));
		map.put("amenity=hospital", Byte.valueOf((byte) 15));
		map.put("amenity=parking", Byte.valueOf((byte) 16));
		map.put("amenity=school", Byte.valueOf((byte) 17));
		map.put("amenity=university", Byte.valueOf((byte) 18));
		map.put("area=yes", Byte.valueOf((byte) 19));
		map.put("barrier=fence", Byte.valueOf((byte) 20));
		map.put("barrier=wall", Byte.valueOf((byte) 21));
		map.put("boundary=administrative", Byte.valueOf((byte) 22));
		map.put("boundary=national_park", Byte.valueOf((byte) 23));
		map.put("bridge=yes", Byte.valueOf((byte) 24));
		map.put("building=apartments", Byte.valueOf((byte) 25));
		map.put("building=embassy", Byte.valueOf((byte) 26));
		map.put("building=government", Byte.valueOf((byte) 27));
		map.put("building=gym", Byte.valueOf((byte) 28));
		map.put("building=roof", Byte.valueOf((byte) 29));
		map.put("building=sports", Byte.valueOf((byte) 30));
		map.put("building=train_station", Byte.valueOf((byte) 31));
		map.put("building=university", Byte.valueOf((byte) 32));
		map.put("building=yes", Byte.valueOf((byte) 33));
		map.put("highway=bridleway", Byte.valueOf((byte) 34));
		map.put("highway=construction", Byte.valueOf((byte) 35));
		map.put("highway=cycleway", Byte.valueOf((byte) 36));
		map.put("highway=footway", Byte.valueOf((byte) 37));
		map.put("highway=living_street", Byte.valueOf((byte) 38));
		map.put("highway=motorway", Byte.valueOf((byte) 39));
		map.put("highway=motorway_link", Byte.valueOf((byte) 40));
		map.put("highway=path", Byte.valueOf((byte) 41));
		map.put("highway=pedestrian", Byte.valueOf((byte) 42));
		map.put("highway=primary", Byte.valueOf((byte) 43));
		map.put("highway=primary_link", Byte.valueOf((byte) 44));
		map.put("highway=residential", Byte.valueOf((byte) 45));
		map.put("highway=road", Byte.valueOf((byte) 46));
		map.put("highway=secondary", Byte.valueOf((byte) 47));
		map.put("highway=service", Byte.valueOf((byte) 48));
		map.put("highway=steps", Byte.valueOf((byte) 49));
		map.put("highway=tertiary", Byte.valueOf((byte) 50));
		map.put("highway=track", Byte.valueOf((byte) 51));
		map.put("highway=trunk", Byte.valueOf((byte) 52));
		map.put("highway=trunk_link", Byte.valueOf((byte) 53));
		map.put("highway=unclassified", Byte.valueOf((byte) 54));
		map.put("historic=ruins", Byte.valueOf((byte) 55));
		map.put("landuse=allotments", Byte.valueOf((byte) 56));
		map.put("landuse=basin", Byte.valueOf((byte) 57));
		map.put("landuse=brownfield", Byte.valueOf((byte) 58));
		map.put("landuse=cemetery", Byte.valueOf((byte) 59));
		map.put("landuse=commercial", Byte.valueOf((byte) 60));
		map.put("landuse=construction", Byte.valueOf((byte) 61));
		map.put("landuse=farm", Byte.valueOf((byte) 62));
		map.put("landuse=farmland", Byte.valueOf((byte) 63));
		map.put("landuse=forest", Byte.valueOf((byte) 64));
		map.put("landuse=grass", Byte.valueOf((byte) 65));
		map.put("landuse=greenfield", Byte.valueOf((byte) 66));
		map.put("landuse=industrial", Byte.valueOf((byte) 67));
		map.put("landuse=military", Byte.valueOf((byte) 68));
		map.put("landuse=recreation_ground", Byte.valueOf((byte) 69));
		map.put("landuse=reservoir", Byte.valueOf((byte) 70));
		map.put("landuse=residential", Byte.valueOf((byte) 71));
		map.put("landuse=retail", Byte.valueOf((byte) 72));
		map.put("landuse=village_green", Byte.valueOf((byte) 73));
		map.put("landuse=wood", Byte.valueOf((byte) 74));
		map.put("leisure=common", Byte.valueOf((byte) 75));
		map.put("leisure=garden", Byte.valueOf((byte) 76));
		map.put("leisure=golf_course", Byte.valueOf((byte) 77));
		map.put("leisure=park", Byte.valueOf((byte) 78));
		map.put("leisure=pitch", Byte.valueOf((byte) 79));
		map.put("leisure=playground", Byte.valueOf((byte) 80));
		map.put("leisure=sports_centre", Byte.valueOf((byte) 81));
		map.put("leisure=stadium", Byte.valueOf((byte) 82));
		map.put("leisure=track", Byte.valueOf((byte) 83));
		map.put("leisure=water_park", Byte.valueOf((byte) 84));
		map.put("man_made=pier", Byte.valueOf((byte) 85));
		map.put("military=airfield", Byte.valueOf((byte) 86));
		map.put("military=barracks", Byte.valueOf((byte) 87));
		map.put("military=naval_base", Byte.valueOf((byte) 88));
		map.put("natural=beach", Byte.valueOf((byte) 89));
		map.put("natural=data", Byte.valueOf((byte) 90));
		map.put("natural=heath", Byte.valueOf((byte) 91));
		map.put("natural=land", Byte.valueOf((byte) 92));
		map.put("natural=scrub", Byte.valueOf((byte) 93));
		map.put("natural=water", Byte.valueOf((byte) 94));
		map.put("natural=wood", Byte.valueOf((byte) 95));
		map.put("place=locality", Byte.valueOf((byte) 96));
		map.put("railway=light_rail", Byte.valueOf((byte) 97));
		map.put("railway=rail", Byte.valueOf((byte) 98));
		map.put("railway=station", Byte.valueOf((byte) 99));
		map.put("railway=subway", Byte.valueOf((byte) 100));
		map.put("railway=tram", Byte.valueOf((byte) 101));
		map.put("route=ferry", Byte.valueOf((byte) 102));
		map.put("sport=shooting", Byte.valueOf((byte) 103));
		map.put("sport=tennis", Byte.valueOf((byte) 104));
		map.put("tourism=attraction", Byte.valueOf((byte) 105));
		map.put("tourism=hostel", Byte.valueOf((byte) 106));
		map.put("tourism=zoo", Byte.valueOf((byte) 107));
		map.put("tunnel=no", Byte.valueOf((byte) 108));
		map.put("tunnel=yes", Byte.valueOf((byte) 109));
		map.put("waterway=canal", Byte.valueOf((byte) 110));
		map.put("waterway=drain", Byte.valueOf((byte) 111));
		map.put("waterway=river", Byte.valueOf((byte) 112));
		map.put("waterway=riverbank", Byte.valueOf((byte) 113));
		map.put("waterway=stream", Byte.valueOf((byte) 114));
		return map;
	}
}