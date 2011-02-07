/*
 * Copyright 2010, 2011 mapsforge.org
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
package org.mapsforge.android.maps;

import java.util.HashMap;

class TagIDsWays {
	Integer admin_level$10;
	Integer admin_level$2;
	Integer admin_level$4;
	Integer admin_level$6;
	Integer admin_level$8;
	Integer admin_level$9;
	Integer aeroway$aerodrome;
	Integer aeroway$apron;
	Integer aeroway$runway;
	Integer aeroway$taxiway;
	Integer aeroway$terminal;
	Integer amenity$college;
	Integer amenity$fountain;
	Integer amenity$grave_yard;
	Integer amenity$hospital;
	Integer amenity$parking;
	Integer amenity$school;
	Integer amenity$university;
	Integer area$yes;
	Integer barrier$fence;
	Integer barrier$wall;
	Integer boundary$administrative;
	Integer boundary$national_park;
	Integer bridge$yes;
	Integer building$apartments;
	Integer building$embassy;
	Integer building$government;
	Integer building$gym;
	Integer building$roof;
	Integer building$sports;
	Integer building$train_station;
	Integer building$university;
	Integer building$yes;
	Integer highway$bridleway;
	Integer highway$construction;
	Integer highway$cycleway;
	Integer highway$footway;
	Integer highway$living_street;
	Integer highway$motorway;
	Integer highway$motorway_link;
	Integer highway$path;
	Integer highway$pedestrian;
	Integer highway$primary;
	Integer highway$primary_link;
	Integer highway$residential;
	Integer highway$road;
	Integer highway$secondary;
	Integer highway$service;
	Integer highway$steps;
	Integer highway$tertiary;
	Integer highway$track;
	Integer highway$trunk;
	Integer highway$trunk_link;
	Integer highway$unclassified;
	Integer historic$ruins;
	Integer landuse$allotments;
	Integer landuse$basin;
	Integer landuse$brownfield;
	Integer landuse$cemetery;
	Integer landuse$commercial;
	Integer landuse$construction;
	Integer landuse$farm;
	Integer landuse$farmland;
	Integer landuse$forest;
	Integer landuse$grass;
	Integer landuse$greenfield;
	Integer landuse$industrial;
	Integer landuse$military;
	Integer landuse$recreation_ground;
	Integer landuse$reservoir;
	Integer landuse$residential;
	Integer landuse$retail;
	Integer landuse$village_green;
	Integer landuse$wood;
	Integer leisure$common;
	Integer leisure$garden;
	Integer leisure$golf_course;
	Integer leisure$park;
	Integer leisure$pitch;
	Integer leisure$playground;
	Integer leisure$sports_centre;
	Integer leisure$stadium;
	Integer leisure$track;
	Integer leisure$water_park;
	Integer man_made$pier;
	Integer military$airfield;
	Integer military$barracks;
	Integer military$naval_base;
	Integer natural$beach;
	Integer natural$coastline;
	Integer natural$glacier;
	Integer natural$heath;
	Integer natural$land;
	Integer natural$scrub;
	Integer natural$water;
	Integer natural$wood;
	Integer place$locality;
	Integer railway$light_rail;
	Integer railway$rail;
	Integer railway$station;
	Integer railway$subway;
	Integer railway$tram;
	Integer route$ferry;
	Integer sport$shooting;
	Integer sport$swimming;
	Integer sport$tennis;
	Integer tourism$attraction;
	Integer tourism$zoo;
	Integer tunnel$no;
	Integer tunnel$yes;
	Integer waterway$canal;
	Integer waterway$drain;
	Integer waterway$river;
	Integer waterway$riverbank;
	Integer waterway$stream;

	void update(HashMap<String, Integer> nodeTags) {
		this.admin_level$10 = nodeTags.get("admin_level=10");
		this.admin_level$2 = nodeTags.get("admin_level=2");
		this.admin_level$4 = nodeTags.get("admin_level=4");
		this.admin_level$6 = nodeTags.get("admin_level=6");
		this.admin_level$8 = nodeTags.get("admin_level=8");
		this.admin_level$9 = nodeTags.get("admin_level=9");

		this.aeroway$aerodrome = nodeTags.get("aeroway=aerodrome");
		this.aeroway$apron = nodeTags.get("aeroway=apron");
		this.aeroway$runway = nodeTags.get("aeroway=runway");
		this.aeroway$taxiway = nodeTags.get("aeroway=taxiway");
		this.aeroway$terminal = nodeTags.get("aeroway=terminal");

		this.amenity$college = nodeTags.get("amenity=college");
		this.amenity$fountain = nodeTags.get("amenity=fountain");
		this.amenity$grave_yard = nodeTags.get("amenity=grave_yard");
		this.amenity$hospital = nodeTags.get("amenity=hospital");
		this.amenity$parking = nodeTags.get("amenity=parking");
		this.amenity$school = nodeTags.get("amenity=school");
		this.amenity$university = nodeTags.get("amenity=university");

		this.area$yes = nodeTags.get("area=yes");

		this.barrier$fence = nodeTags.get("barrier=fence");
		this.barrier$wall = nodeTags.get("barrier=wall");

		this.boundary$administrative = nodeTags.get("boundary=administrative");
		this.boundary$national_park = nodeTags.get("boundary=national_park");

		this.bridge$yes = nodeTags.get("bridge=yes");

		this.building$apartments = nodeTags.get("building=apartments");
		this.building$embassy = nodeTags.get("building=embassy");
		this.building$government = nodeTags.get("building=government");
		this.building$gym = nodeTags.get("building=gym");
		this.building$roof = nodeTags.get("building=roof");
		this.building$sports = nodeTags.get("building=sports");
		this.building$train_station = nodeTags.get("building=train_station");
		this.building$university = nodeTags.get("building=university");
		this.building$yes = nodeTags.get("building=yes");

		this.highway$bridleway = nodeTags.get("highway=bridleway");
		this.highway$construction = nodeTags.get("highway=construction");
		this.highway$cycleway = nodeTags.get("highway=cycleway");
		this.highway$footway = nodeTags.get("highway=footway");
		this.highway$living_street = nodeTags.get("highway=living_street");
		this.highway$motorway = nodeTags.get("highway=motorway");
		this.highway$motorway_link = nodeTags.get("highway=motorway_link");
		this.highway$path = nodeTags.get("highway=path");
		this.highway$pedestrian = nodeTags.get("highway=pedestrian");
		this.highway$primary = nodeTags.get("highway=primary");
		this.highway$primary_link = nodeTags.get("highway=primary_link");
		this.highway$residential = nodeTags.get("highway=residential");
		this.highway$road = nodeTags.get("highway=road");
		this.highway$secondary = nodeTags.get("highway=secondary");
		this.highway$service = nodeTags.get("highway=service");
		this.highway$steps = nodeTags.get("highway=steps");
		this.highway$tertiary = nodeTags.get("highway=tertiary");
		this.highway$track = nodeTags.get("highway=track");
		this.highway$trunk = nodeTags.get("highway=trunk");
		this.highway$trunk_link = nodeTags.get("highway=trunk_link");
		this.highway$unclassified = nodeTags.get("highway=unclassified");

		this.historic$ruins = nodeTags.get("historic=ruins");

		this.landuse$allotments = nodeTags.get("landuse=allotments");
		this.landuse$basin = nodeTags.get("landuse=basin");
		this.landuse$brownfield = nodeTags.get("landuse=brownfield");
		this.landuse$cemetery = nodeTags.get("landuse=cemetery");
		this.landuse$commercial = nodeTags.get("landuse=commercial");
		this.landuse$construction = nodeTags.get("landuse=construction");
		this.landuse$farm = nodeTags.get("landuse=farm");
		this.landuse$farmland = nodeTags.get("landuse=farmland");
		this.landuse$forest = nodeTags.get("landuse=forest");
		this.landuse$grass = nodeTags.get("landuse=grass");
		this.landuse$greenfield = nodeTags.get("landuse=greenfield");
		this.landuse$industrial = nodeTags.get("landuse=industrial");
		this.landuse$military = nodeTags.get("landuse=military");
		this.landuse$recreation_ground = nodeTags.get("landuse=recreation_ground");
		this.landuse$reservoir = nodeTags.get("landuse=reservoir");
		this.landuse$residential = nodeTags.get("landuse=residential");
		this.landuse$retail = nodeTags.get("landuse=retail");
		this.landuse$village_green = nodeTags.get("landuse=village_green");
		this.landuse$wood = nodeTags.get("landuse=wood");

		this.leisure$common = nodeTags.get("leisure=common");
		this.leisure$garden = nodeTags.get("leisure=garden");
		this.leisure$golf_course = nodeTags.get("leisure=golf_course");
		this.leisure$park = nodeTags.get("leisure=park");
		this.leisure$pitch = nodeTags.get("leisure=pitch");
		this.leisure$playground = nodeTags.get("leisure=playground");
		this.leisure$sports_centre = nodeTags.get("leisure=sports_centre");
		this.leisure$stadium = nodeTags.get("leisure=stadium");
		this.leisure$track = nodeTags.get("leisure=track");
		this.leisure$water_park = nodeTags.get("leisure=water_park");

		this.man_made$pier = nodeTags.get("man_made=pier");

		this.military$airfield = nodeTags.get("military=airfield");
		this.military$barracks = nodeTags.get("military=barracks");
		this.military$naval_base = nodeTags.get("military=naval_base");

		this.natural$beach = nodeTags.get("natural=beach");
		this.natural$coastline = nodeTags.get("natural=coastline");
		this.natural$glacier = nodeTags.get("natural=glacier");
		this.natural$heath = nodeTags.get("natural=heath");
		this.natural$land = nodeTags.get("natural=land");
		this.natural$scrub = nodeTags.get("natural=scrub");
		this.natural$water = nodeTags.get("natural=water");
		this.natural$wood = nodeTags.get("natural=wood");

		this.place$locality = nodeTags.get("place=locality");

		this.railway$light_rail = nodeTags.get("railway=light_rail");
		this.railway$rail = nodeTags.get("railway=rail");
		this.railway$station = nodeTags.get("railway=station");
		this.railway$subway = nodeTags.get("railway=subway");
		this.railway$tram = nodeTags.get("railway=tram");

		this.route$ferry = nodeTags.get("route=ferry");

		this.sport$shooting = nodeTags.get("sport=shooting");
		this.sport$swimming = nodeTags.get("sport=swimming");
		this.sport$tennis = nodeTags.get("sport=tennis");

		this.tourism$attraction = nodeTags.get("tourism=attraction");
		this.tourism$zoo = nodeTags.get("tourism=zoo");

		this.tunnel$no = nodeTags.get("tunnel=no");
		this.tunnel$yes = nodeTags.get("tunnel=yes");

		this.waterway$canal = nodeTags.get("waterway=canal");
		this.waterway$drain = nodeTags.get("waterway=drain");
		this.waterway$river = nodeTags.get("waterway=river");
		this.waterway$riverbank = nodeTags.get("waterway=riverbank");
		this.waterway$stream = nodeTags.get("waterway=stream");
	}
}