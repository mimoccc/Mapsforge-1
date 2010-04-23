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

/**
 * List of all layers that are used for correct rendering.
 */
final class LayerIds {
	static final byte LANDUSE$MILITARY = 0;
	static final byte MILITARY$AIRFIELD = 1;
	static final byte MILITARY$BARRACKS = 1;
	static final byte MILITARY$NAVAL_BASE = 1;
	static final byte LANDUSE$RESIDENTIAL = 2;
	static final byte LANDUSE$RETAIL = 3;
	static final byte LANDUSE$BROWNFIELD = 4;
	static final byte LANDUSE$INDUSTRIAL = 4;
	static final byte LANDUSE$COMMERCIAL = 5;
	static final byte LANDUSE$CONSTRUCTION = 6;
	static final byte AMENITY$COLLEGE = 7;
	static final byte AMENITY$FOUNTAIN = 7;
	static final byte AMENITY$HOSPITAL = 7;
	static final byte AMENITY$SCHOOL = 7;
	static final byte AMENITY$UNIVERSITY = 7;
	static final byte LANDUSE$CEMETERY = 8;
	static final byte AMENITY$GRAVE_YARD = 9;
	static final byte NATURAL$COASTLINE = 10;
	static final byte NATURAL$LAND = 11;
	static final byte NATURAL$BEACH = 12;
	static final byte LANDUSE$FOREST = 13;
	static final byte LANDUSE$WOOD = 13;
	static final byte NATURAL$WOOD = 13;
	static final byte NATURAL$SCRUB = 13;
	static final byte NATURAL$HEATH = 14;
	static final byte LANDUSE$ALLOTMENTS = 15;
	static final byte LANDUSE$FARM = 15;
	static final byte LANDUSE$FARMLAND = 15;
	static final byte LANDUSE$GRASS = 15;
	static final byte LANDUSE$RECREATION_GROUND = 15;
	static final byte LANDUSE$VILLAGE_GREEN = 15;
	static final byte LEISURE$COMMON = 16;
	static final byte LEISURE$GARDEN = 16;
	static final byte LEISURE$GOLF_COURSE = 16;
	static final byte LEISURE$PARK = 16;
	static final byte LEISURE$PITCH = 17;
	static final byte LEISURE$PLAYGROUND = 17;
	static final byte LEISURE$SPORTS_CENTRE = 18;
	static final byte LEISURE$STADIUM = 18;
	static final byte LEISURE$WATER_PARK = 18;
	static final byte LEISURE$TRACK = 19;
	static final byte AEROWAY$AERODROME = 20;
	static final byte AEROWAY$APRON = 21;
	static final byte SPORT$TENNIS = 22;
	static final byte SPORT$SHOOTING = 23;
	static final byte AMENITY$PARKING = 24;
	static final byte TOURISM$ATTRACTION = 25;
	static final byte TOURISM$ZOO = 26;
	static final byte AEROWAY$RUNWAY1 = 27;
	static final byte AEROWAY$TAXIWAY1 = 28;
	static final byte AEROWAY$RUNWAY2 = 29;
	static final byte AEROWAY$TAXIWAY2 = 30;
	static final byte WATERWAY$RIVER = 31;
	static final byte WATERWAY$STREAM = 32;
	static final byte WATERWAY$CANAL = 33;
	static final byte WATERWAY$DRAIN = 34;
	static final byte NATURAL$WATER = 35;
	static final byte LANDUSE$RESERVOIR = 36;
	static final byte LANDUSE$BASIN = 37;
	static final byte WATERWAY$RIVERBANK = 38;
	static final byte HIGHWAY$FOOTWAY_AREA$YES = 39;
	static final byte HIGHWAY$PEDESTRIAN_AREA$YES = 40;
	static final byte HIGHWAY$SERVICE_AREA$YES = 41;
	static final byte AEROWAY$TERMINAL = 42;
	static final byte BUILDING$APARTMENTS = 43;
	static final byte BUILDING$GYM = 43;
	static final byte BUILDING$ROOF = 43;
	static final byte BUILDING$SPORTS = 43;
	static final byte BUILDING$TRAIN_STATION = 43;
	static final byte BUILDING$UNIVERSITY = 43;
	static final byte BUILDING$YES = 43;
	static final byte MAN_MADE$PIER = 44;
	static final byte ROUTE$FERRY = 45;
	static final byte BARRIER$FENCE = 46;
	static final byte BARRIER$WALL = 46;
	static final byte HIGHWAY_TUNNEL$YES1 = 47;
	static final byte HIGHWAY_TUNNEL$YES2 = 48;
	static final byte HIGHWAY$CONSTRUCTION = 49;
	static final byte HIGHWAY$STEPS1 = 50;
	static final byte HIGHWAY$FOOTWAY1 = 51;
	static final byte HIGHWAY$PEDESTRIAN1 = 52;
	static final byte HIGHWAY$CYCLEWAY1 = 53;
	static final byte HIGHWAY$PATH1 = 54;
	static final byte HIGHWAY$BRIDLEWAY1 = 55;
	static final byte HIGHWAY$TRACK1 = 56;
	static final byte HIGHWAY$SERVICE1 = 57;
	static final byte HIGHWAY$UNCLASSIFIED1 = 58;
	static final byte HIGHWAY$RESIDENTIAL1 = 59;
	static final byte HIGHWAY$LIVING_STREET1 = 60;
	static final byte HIGHWAY$ROAD1 = 61;
	static final byte HIGHWAY$TERTIARY1 = 62;
	static final byte HIGHWAY$SECONDARY1 = 63;
	static final byte HIGHWAY$PRIMARY_LINK1 = 64;
	static final byte HIGHWAY$PRIMARY1 = 65;
	static final byte HIGHWAY$TRUNK_LINK1 = 66;
	static final byte HIGHWAY$TRUNK1 = 67;
	static final byte HIGHWAY$MOTORWAY_LINK1 = 68;
	static final byte HIGHWAY$MOTORWAY1 = 69;
	static final byte HIGHWAY$STEPS2 = 70;
	static final byte HIGHWAY$FOOTWAY2 = 71;
	static final byte HIGHWAY$PEDESTRIAN2 = 72;
	static final byte HIGHWAY$CYCLEWAY2 = 73;
	static final byte HIGHWAY$PATH2 = 74;
	static final byte HIGHWAY$BRIDLEWAY2 = 75;
	static final byte HIGHWAY$TRACK2 = 76;
	static final byte HIGHWAY$SERVICE2 = 77;
	static final byte HIGHWAY$UNCLASSIFIED2 = 78;
	static final byte HIGHWAY$RESIDENTIAL2 = 79;
	static final byte HIGHWAY$LIVING_STREET2 = 80;
	static final byte HIGHWAY$ROAD2 = 81;
	static final byte HIGHWAY$TERTIARY2 = 82;
	static final byte HIGHWAY$SECONDARY2 = 83;
	static final byte HIGHWAY$PRIMARY_LINK2 = 84;
	static final byte HIGHWAY$PRIMARY2 = 85;
	static final byte HIGHWAY$TRUNK_LINK2 = 86;
	static final byte HIGHWAY$TRUNK2 = 87;
	static final byte HIGHWAY$MOTORWAY_LINK2 = 88;
	static final byte HIGHWAY$MOTORWAY2 = 89;
	static final byte RAILWAY$LIGHT_RAIL = 90;
	static final byte RAILWAY$RAIL = 90;
	static final byte RAILWAY$RAIL_TUNNEL$YES = 90;
	static final byte RAILWAY$SUBWAY = 90;
	static final byte RAILWAY$SUBWAY_TUNNEL = 90;
	static final byte RAILWAY$STATION = 90;
	static final byte RAILWAY$TRAM = 90;
	static final byte ADMIN_LEVEL$2 = 91;
	static final byte ADMIN_LEVEL$4 = 91;
	static final byte ADMIN_LEVEL$6 = 91;
	static final byte ADMIN_LEVEL$8 = 91;
	static final byte ADMIN_LEVEL$9 = 91;
	static final byte ADMIN_LEVEL$10 = 91;
	static final byte POI_CIRCLE_SYMBOL = 92;
	static final byte LEVELS_PER_LAYER = 93;
}