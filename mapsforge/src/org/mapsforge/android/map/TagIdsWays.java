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
	static final short ADMIN_LEVEL$2 = 0;
	static final short ADMIN_LEVEL$4 = 1;
	static final short ADMIN_LEVEL$6 = 2;
	static final short ADMIN_LEVEL$8 = 3;
	static final short ADMIN_LEVEL$9 = 4;
	static final short ADMIN_LEVEL$10 = 5;
	static final short AERIALWAY$CABLE_CAR = 6;
	static final short AERIALWAY$CHAIR_LIFT = 7;
	static final short AEROWAY$AERODROME = 8;
	static final short AEROWAY$APRON = 9;
	static final short AEROWAY$HELIPAD = 10;
	static final short AEROWAY$RUNWAY = 11;
	static final short AEROWAY$TAXIWAY = 12;
	static final short AEROWAY$TERMINAL = 13;
	static final short AMENITY$COLLEGE = 14;
	static final short AMENITY$EMBASSY = 15;
	static final short AMENITY$FOUNTAIN = 16;
	static final short AMENITY$GRAVE_YARD = 17;
	static final short AMENITY$HOSPITAL = 18;
	static final short AMENITY$PARKING = 19;
	static final short AMENITY$SCHOOL = 20;
	static final short AMENITY$UNIVERSITY = 21;
	static final short AREA$YES = 22;
	static final short BARRIER$FENCE = 23;
	static final short BARRIER$WALL = 24;
	static final short BOUNDARY$ADMINISTRATIVE = 25;
	static final short BOUNDARY$NATIONAL_PARK = 26;
	static final short BRIDGE$YES = 27;
	static final short BUILDING$APARTMENTS = 28;
	static final short BUILDING$EMBASSY = 29;
	static final short BUILDING$GOVERNMENT = 30;
	static final short BUILDING$GYM = 31;
	static final short BUILDING$ROOF = 32;
	static final short BUILDING$SPORTS = 33;
	static final short BUILDING$TRAIN_STATION = 34;
	static final short BUILDING$UNIVERSITY = 35;
	static final short BUILDING$YES = 36;
	static final short HIGHWAY$BRIDLEWAY = 37;
	static final short HIGHWAY$BUS_GUIDEWAY = 38;
	static final short HIGHWAY$CONSTRUCTION = 39;
	static final short HIGHWAY$CYCLEWAY = 40;
	static final short HIGHWAY$FOOTWAY = 41;
	static final short HIGHWAY$LIVING_STREET = 42;
	static final short HIGHWAY$MOTORWAY = 43;
	static final short HIGHWAY$MOTORWAY_LINK = 44;
	static final short HIGHWAY$PATH = 45;
	static final short HIGHWAY$PEDESTRIAN = 46;
	static final short HIGHWAY$PRIMARY = 47;
	static final short HIGHWAY$PRIMARY_LINK = 48;
	static final short HIGHWAY$RESIDENTIAL = 49;
	static final short HIGHWAY$RACEWAY = 50;
	static final short HIGHWAY$ROAD = 51;
	static final short HIGHWAY$SECONDARY = 52;
	static final short HIGHWAY$SERVICE = 53;
	static final short HIGHWAY$SERVICES = 54;
	static final short HIGHWAY$STEPS = 55;
	static final short HIGHWAY$TERTIARY = 56;
	static final short HIGHWAY$TRACK = 57;
	static final short HIGHWAY$TRUNK = 58;
	static final short HIGHWAY$TRUNK_LINK = 59;
	static final short HIGHWAY$UNCLASSIFIED = 60;
	static final short HISTORIC$RUINS = 61;
	static final short LANDUSE$ALLOTMENTS = 62;
	static final short LANDUSE$BASIN = 63;
	static final short LANDUSE$BROWNFIELD = 64;
	static final short LANDUSE$CEMETERY = 65;
	static final short LANDUSE$COMMERCIAL = 66;
	static final short LANDUSE$CONSTRUCTION = 67;
	static final short LANDUSE$FARM = 68;
	static final short LANDUSE$FARMLAND = 69;
	static final short LANDUSE$FOREST = 70;
	static final short LANDUSE$GRASS = 71;
	static final short LANDUSE$GREENFIELD = 72;
	static final short LANDUSE$INDUSTRIAL = 73;
	static final short LANDUSE$MILITARY = 74;
	static final short LANDUSE$QUARRY = 75;
	static final short LANDUSE$RAILWAY = 76;
	static final short LANDUSE$RECREATION_GROUND = 77;
	static final short LANDUSE$RESERVOIR = 78;
	static final short LANDUSE$RESIDENTIAL = 79;
	static final short LANDUSE$RETAIL = 80;
	static final short LANDUSE$VILLAGE_GREEN = 81;
	static final short LANDUSE$VINEYARD = 82;
	static final short LANDUSE$WOOD = 83;
	static final short LEISURE$COMMON = 84;
	static final short LEISURE$GARDEN = 85;
	static final short LEISURE$GOLF_COURSE = 86;
	static final short LEISURE$PARK = 87;
	static final short LEISURE$PITCH = 88;
	static final short LEISURE$PLAYGROUND = 89;
	static final short LEISURE$SPORTS_CENTRE = 90;
	static final short LEISURE$STADIUM = 91;
	static final short LEISURE$TRACK = 92;
	static final short LEISURE$WATER_PARK = 93;
	static final short MAN_MADE$PIER = 94;
	static final short MILITARY$AIRFIELD = 95;
	static final short MILITARY$BARRACKS = 96;
	static final short MILITARY$NAVAL_BASE = 97;
	static final short NATURAL$BEACH = 98;
	static final short NATURAL$COASTLINE = 99;
	static final short NATURAL$GLACIER = 100;
	static final short NATURAL$HEATH = 101;
	static final short NATURAL$LAND = 102;
	static final short NATURAL$SCRUB = 103;
	static final short NATURAL$WATER = 104;
	static final short NATURAL$WOOD = 105;
	static final short PLACE$LOCALITY = 106;
	static final short POWER$STATION = 107;
	static final short POWER$SUB_STATION = 108;
	static final short RAILWAY$LIGHT_RAIL = 109;
	static final short RAILWAY$RAIL = 110;
	static final short RAILWAY$STATION = 111;
	static final short RAILWAY$SUBWAY = 112;
	static final short RAILWAY$TRAM = 113;
	static final short ROUTE$FERRY = 114;
	static final short SPORT$GOLF = 115;
	static final short SPORT$SHOOTING = 116;
	static final short SPORT$SOCCER = 117;
	static final short SPORT$TENNIS = 118;
	static final short TOURISM$ATTRACTION = 119;
	static final short TOURISM$HOSTEL = 120;
	static final short TOURISM$ZOO = 121;
	static final short TRACKTYPE$GRADE1 = 122;
	static final short TRACKTYPE$GRADE2 = 123;
	static final short TRACKTYPE$GRADE3 = 124;
	static final short TRACKTYPE$GRADE4 = 125;
	static final short TRACKTYPE$GRADE5 = 126;
	static final short TUNNEL$NO = 127;
	static final short TUNNEL$YES = 128;
	static final short WATERWAY$CANAL = 129;
	static final short WATERWAY$DAM = 130;
	static final short WATERWAY$DRAIN = 131;
	static final short WATERWAY$RIVER = 132;
	static final short WATERWAY$RIVERBANK = 133;
	static final short WATERWAY$STREAM = 134;

	static final Map<String, Short> getMap() {
		Map<String, Short> map = new HashMap<String, Short>();
		map.put("admin_level=10", Short.valueOf((short) 5));
		map.put("admin_level=2", Short.valueOf((short) 0));
		map.put("admin_level=4", Short.valueOf((short) 1));
		map.put("admin_level=6", Short.valueOf((short) 2));
		map.put("admin_level=8", Short.valueOf((short) 3));
		map.put("admin_level=9", Short.valueOf((short) 4));
		map.put("aerialway=cable_car", Short.valueOf((short) 6));
		map.put("aerialway=chair_lift", Short.valueOf((short) 7));
		map.put("aeroway=aerodrome", Short.valueOf((short) 8));
		map.put("aeroway=apron", Short.valueOf((short) 9));
		map.put("aeroway=helipad", Short.valueOf((short) 10));
		map.put("aeroway=runway", Short.valueOf((short) 11));
		map.put("aeroway=taxiway", Short.valueOf((short) 12));
		map.put("aeroway=terminal", Short.valueOf((short) 13));
		map.put("amenity=college", Short.valueOf((short) 14));
		map.put("amenity=embassy", Short.valueOf((short) 15));
		map.put("amenity=fountain", Short.valueOf((short) 16));
		map.put("amenity=grave_yard", Short.valueOf((short) 17));
		map.put("amenity=hospital", Short.valueOf((short) 18));
		map.put("amenity=parking", Short.valueOf((short) 19));
		map.put("amenity=school", Short.valueOf((short) 20));
		map.put("amenity=university", Short.valueOf((short) 21));
		map.put("area=yes", Short.valueOf((short) 22));
		map.put("barrier=fence", Short.valueOf((short) 23));
		map.put("barrier=wall", Short.valueOf((short) 24));
		map.put("boundary=administrative", Short.valueOf((short) 25));
		map.put("boundary=national_park", Short.valueOf((short) 26));
		map.put("bridge=yes", Short.valueOf((short) 27));
		map.put("building=apartments", Short.valueOf((short) 28));
		map.put("building=embassy", Short.valueOf((short) 29));
		map.put("building=government", Short.valueOf((short) 30));
		map.put("building=gym", Short.valueOf((short) 31));
		map.put("building=roof", Short.valueOf((short) 32));
		map.put("building=sports", Short.valueOf((short) 33));
		map.put("building=train_station", Short.valueOf((short) 34));
		map.put("building=university", Short.valueOf((short) 35));
		map.put("building=yes", Short.valueOf((short) 36));
		map.put("highway=bridleway", Short.valueOf((short) 37));
		map.put("highway=bus_guideway", Short.valueOf((short) 38));
		map.put("highway=construction", Short.valueOf((short) 39));
		map.put("highway=cycleway", Short.valueOf((short) 40));
		map.put("highway=footway", Short.valueOf((short) 41));
		map.put("highway=living_street", Short.valueOf((short) 42));
		map.put("highway=motorway", Short.valueOf((short) 43));
		map.put("highway=motorway_link", Short.valueOf((short) 44));
		map.put("highway=path", Short.valueOf((short) 45));
		map.put("highway=pedestrian", Short.valueOf((short) 46));
		map.put("highway=primary", Short.valueOf((short) 47));
		map.put("highway=primary_link", Short.valueOf((short) 48));
		map.put("highway=raceway", Short.valueOf((short) 50));
		map.put("highway=residential", Short.valueOf((short) 49));
		map.put("highway=road", Short.valueOf((short) 51));
		map.put("highway=secondary", Short.valueOf((short) 52));
		map.put("highway=service", Short.valueOf((short) 53));
		map.put("highway=services", Short.valueOf((short) 54));
		map.put("highway=steps", Short.valueOf((short) 55));
		map.put("highway=tertiary", Short.valueOf((short) 56));
		map.put("highway=track", Short.valueOf((short) 57));
		map.put("highway=trunk", Short.valueOf((short) 58));
		map.put("highway=trunk_link", Short.valueOf((short) 59));
		map.put("highway=unclassified", Short.valueOf((short) 60));
		map.put("historic=ruins", Short.valueOf((short) 61));
		map.put("landuse=allotments", Short.valueOf((short) 62));
		map.put("landuse=basin", Short.valueOf((short) 63));
		map.put("landuse=brownfield", Short.valueOf((short) 64));
		map.put("landuse=cemetery", Short.valueOf((short) 65));
		map.put("landuse=commercial", Short.valueOf((short) 66));
		map.put("landuse=construction", Short.valueOf((short) 67));
		map.put("landuse=farm", Short.valueOf((short) 68));
		map.put("landuse=farmland", Short.valueOf((short) 69));
		map.put("landuse=forest", Short.valueOf((short) 70));
		map.put("landuse=grass", Short.valueOf((short) 71));
		map.put("landuse=greenfield", Short.valueOf((short) 72));
		map.put("landuse=industrial", Short.valueOf((short) 73));
		map.put("landuse=military", Short.valueOf((short) 74));
		map.put("landuse=quarry", Short.valueOf((short) 75));
		map.put("landuse=railway", Short.valueOf((short) 76));
		map.put("landuse=recreation_ground", Short.valueOf((short) 77));
		map.put("landuse=reservoir", Short.valueOf((short) 78));
		map.put("landuse=residential", Short.valueOf((short) 79));
		map.put("landuse=retail", Short.valueOf((short) 80));
		map.put("landuse=village_green", Short.valueOf((short) 81));
		map.put("landuse=vineyard", Short.valueOf((short) 82));
		map.put("landuse=wood", Short.valueOf((short) 83));
		map.put("leisure=common", Short.valueOf((short) 84));
		map.put("leisure=garden", Short.valueOf((short) 85));
		map.put("leisure=golf_course", Short.valueOf((short) 86));
		map.put("leisure=park", Short.valueOf((short) 87));
		map.put("leisure=pitch", Short.valueOf((short) 88));
		map.put("leisure=playground", Short.valueOf((short) 89));
		map.put("leisure=sports_centre", Short.valueOf((short) 90));
		map.put("leisure=stadium", Short.valueOf((short) 91));
		map.put("leisure=track", Short.valueOf((short) 92));
		map.put("leisure=water_park", Short.valueOf((short) 93));
		map.put("man_made=pier", Short.valueOf((short) 94));
		map.put("military=airfield", Short.valueOf((short) 95));
		map.put("military=barracks", Short.valueOf((short) 96));
		map.put("military=naval_base", Short.valueOf((short) 97));
		map.put("natural=beach", Short.valueOf((short) 98));
		map.put("natural=coastline", Short.valueOf((short) 99));
		map.put("natural=glacier", Short.valueOf((short) 100));
		map.put("natural=heath", Short.valueOf((short) 101));
		map.put("natural=land", Short.valueOf((short) 102));
		map.put("natural=scrub", Short.valueOf((short) 103));
		map.put("natural=water", Short.valueOf((short) 104));
		map.put("natural=wood", Short.valueOf((short) 105));
		map.put("place=locality", Short.valueOf((short) 106));
		map.put("power=station", Short.valueOf((short) 107));
		map.put("power=sub_station", Short.valueOf((short) 108));
		map.put("railway=light_rail", Short.valueOf((short) 109));
		map.put("railway=rail", Short.valueOf((short) 110));
		map.put("railway=station", Short.valueOf((short) 111));
		map.put("railway=subway", Short.valueOf((short) 112));
		map.put("railway=tram", Short.valueOf((short) 113));
		map.put("route=ferry", Short.valueOf((short) 114));
		map.put("sport=golf", Short.valueOf((short) 115));
		map.put("sport=shooting", Short.valueOf((short) 116));
		map.put("sport=soccer", Short.valueOf((short) 117));
		map.put("sport=tennis", Short.valueOf((short) 118));
		map.put("tourism=attraction", Short.valueOf((short) 119));
		map.put("tourism=hostel", Short.valueOf((short) 120));
		map.put("tourism=zoo", Short.valueOf((short) 121));
		map.put("tracktype=grade1", Short.valueOf((short) 122));
		map.put("tracktype=grade2", Short.valueOf((short) 123));
		map.put("tracktype=grade3", Short.valueOf((short) 124));
		map.put("tracktype=grade4", Short.valueOf((short) 125));
		map.put("tracktype=grade5", Short.valueOf((short) 126));
		map.put("tunnel=no", Short.valueOf((short) 127));
		map.put("tunnel=yes", Short.valueOf((short) 128));
		map.put("waterway=canal", Short.valueOf((short) 129));
		map.put("waterway=dam", Short.valueOf((short) 130));
		map.put("waterway=drain", Short.valueOf((short) 131));
		map.put("waterway=river", Short.valueOf((short) 132));
		map.put("waterway=riverbank", Short.valueOf((short) 133));
		map.put("waterway=stream", Short.valueOf((short) 134));
		return map;
	}
}