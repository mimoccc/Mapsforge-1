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
package org.mapsforge.preprocessing.map;

import java.util.TreeMap;

class Filter {
	static TreeMap<String, Byte> getNodeFilter() {
		TreeMap<String, Byte> filter = new TreeMap<String, Byte>();

		filter.put("aeroway=helipad", Byte.valueOf((byte) 17));

		filter.put("amenity=atm", Byte.valueOf((byte) 17));
		filter.put("amenity=bank", Byte.valueOf((byte) 17));
		filter.put("amenity=bicycle_rental", Byte.valueOf((byte) 17));
		filter.put("amenity=bus_station", Byte.valueOf((byte) 16));
		filter.put("amenity=cafe", Byte.valueOf((byte) 17));
		filter.put("amenity=cinema", Byte.valueOf((byte) 17));
		filter.put("amenity=fast_food", Byte.valueOf((byte) 17));
		filter.put("amenity=fire_station", Byte.valueOf((byte) 17));
		filter.put("amenity=fountain", Byte.valueOf((byte) 16));
		filter.put("amenity=fuel", Byte.valueOf((byte) 17));
		filter.put("amenity=hospital", Byte.valueOf((byte) 16));
		filter.put("amenity=library", Byte.valueOf((byte) 17));
		filter.put("amenity=parking", Byte.valueOf((byte) 17));
		filter.put("amenity=pharmacy", Byte.valueOf((byte) 17));
		filter.put("amenity=place_of_worship", Byte.valueOf((byte) 17));
		filter.put("amenity=post_box", Byte.valueOf((byte) 17));
		filter.put("amenity=post_office", Byte.valueOf((byte) 17));
		filter.put("amenity=pub", Byte.valueOf((byte) 17));
		filter.put("amenity=recycling", Byte.valueOf((byte) 17));
		filter.put("amenity=restaurant", Byte.valueOf((byte) 17));
		filter.put("amenity=school", Byte.valueOf((byte) 17));
		filter.put("amenity=shelter", Byte.valueOf((byte) 16));
		filter.put("amenity=telephone", Byte.valueOf((byte) 17));
		filter.put("amenity=theatre", Byte.valueOf((byte) 17));
		filter.put("amenity=toilets", Byte.valueOf((byte) 17));
		filter.put("amenity=university", Byte.valueOf((byte) 17));

		filter.put("barrier=bollard", Byte.valueOf((byte) 16));

		filter.put("highway=bus_stop", Byte.valueOf((byte) 16));
		filter.put("highway=traffic_signals", Byte.valueOf((byte) 17));

		filter.put("historic=memorial", Byte.valueOf((byte) 17));
		filter.put("historic=monument", Byte.valueOf((byte) 17));

		filter.put("leisure=playground", Byte.valueOf((byte) 17));

		filter.put("man_made=windmill", Byte.valueOf((byte) 17));

		filter.put("natural=peak", Byte.valueOf((byte) 15));

		filter.put("place=city", Byte.valueOf((byte) 8));
		filter.put("place=island", Byte.valueOf((byte) 12));
		filter.put("place=suburb", Byte.valueOf((byte) 14));
		filter.put("place=town", Byte.valueOf((byte) 9));
		filter.put("place=village", Byte.valueOf((byte) 14));

		filter.put("railway=halt", Byte.valueOf((byte) 17));
		filter.put("railway=level_crossing", Byte.valueOf((byte) 16));
		filter.put("railway=station", Byte.valueOf((byte) 15));
		filter.put("railway=tram_stop", Byte.valueOf((byte) 17));

		filter.put("shop=bakery", Byte.valueOf((byte) 17));
		filter.put("shop=organic", Byte.valueOf((byte) 17));
		filter.put("shop=supermarket", Byte.valueOf((byte) 17));

		filter.put("station=light_rail", Byte.valueOf(Byte.MAX_VALUE));
		filter.put("station=subway", Byte.valueOf(Byte.MAX_VALUE));

		filter.put("tourism=attraction", Byte.valueOf((byte) 17));
		filter.put("tourism=hostel", Byte.valueOf((byte) 17));
		filter.put("tourism=hotel", Byte.valueOf((byte) 17));
		filter.put("tourism=information", Byte.valueOf((byte) 17));
		filter.put("tourism=museum", Byte.valueOf((byte) 17));
		filter.put("tourism=viewpoint", Byte.valueOf((byte) 15));

		return filter;
	}

	static TreeMap<String, Byte> getWayFilter() {
		TreeMap<String, Byte> filter = new TreeMap<String, Byte>();

		filter.put("admin_level=2", Byte.valueOf((byte) 6));
		filter.put("admin_level=4", Byte.valueOf((byte) 12)); // like Osmarender
		filter.put("admin_level=6", Byte.valueOf((byte) 15));
		filter.put("admin_level=8", Byte.valueOf((byte) 16));
		filter.put("admin_level=9", Byte.valueOf((byte) 16));
		filter.put("admin_level=10", Byte.valueOf((byte) 16));

		filter.put("aeroway=aerodrome", Byte.valueOf((byte) 13));
		filter.put("aeroway=apron", Byte.valueOf((byte) 13));
		filter.put("aeroway=helipad", Byte.valueOf((byte) 17));
		filter.put("aeroway=runway", Byte.valueOf((byte) 10));
		filter.put("aeroway=taxiway", Byte.valueOf((byte) 10));
		filter.put("aeroway=terminal", Byte.valueOf((byte) 16));

		filter.put("amenity=college", Byte.valueOf((byte) 15));
		filter.put("amenity=fountain", Byte.valueOf((byte) 15));
		filter.put("amenity=grave_yard", Byte.valueOf((byte) 15));
		filter.put("amenity=hospital", Byte.valueOf((byte) 15));
		filter.put("amenity=parking", Byte.valueOf((byte) 15));
		filter.put("amenity=school", Byte.valueOf((byte) 15));
		filter.put("amenity=university", Byte.valueOf((byte) 15));

		filter.put("area=yes", Byte.valueOf(Byte.MAX_VALUE));

		filter.put("barrier=fence", Byte.valueOf((byte) 16));
		filter.put("barrier=wall", Byte.valueOf((byte) 17));

		filter.put("boundary=administrative", Byte.valueOf(Byte.MAX_VALUE));
		filter.put("boundary=national_park", Byte.valueOf((byte) 12));

		filter.put("bridge=yes", Byte.valueOf(Byte.MAX_VALUE));

		filter.put("building=apartments", Byte.valueOf((byte) 16));
		filter.put("building=embassy", Byte.valueOf((byte) 16));
		filter.put("building=government", Byte.valueOf((byte) 16));
		filter.put("building=gym", Byte.valueOf((byte) 16));
		filter.put("building=roof", Byte.valueOf((byte) 16));
		filter.put("building=sports", Byte.valueOf((byte) 16));
		filter.put("building=train_station", Byte.valueOf((byte) 16));
		filter.put("building=university", Byte.valueOf((byte) 16));
		filter.put("building=yes", Byte.valueOf((byte) 16));

		filter.put("highway=bridleway", Byte.valueOf((byte) 13));
		filter.put("highway=construction", Byte.valueOf((byte) 15));
		filter.put("highway=cycleway", Byte.valueOf((byte) 13));
		filter.put("highway=footway", Byte.valueOf((byte) 15));
		filter.put("highway=living_street", Byte.valueOf((byte) 14));
		filter.put("highway=motorway", Byte.valueOf((byte) 8));
		filter.put("highway=motorway_link", Byte.valueOf((byte) 8));
		filter.put("highway=path", Byte.valueOf((byte) 14));
		filter.put("highway=pedestrian", Byte.valueOf((byte) 14));
		filter.put("highway=primary", Byte.valueOf((byte) 8));
		filter.put("highway=primary_link", Byte.valueOf((byte) 8));
		filter.put("highway=residential", Byte.valueOf((byte) 14));
		filter.put("highway=road", Byte.valueOf((byte) 12));
		filter.put("highway=secondary", Byte.valueOf((byte) 9));
		filter.put("highway=service", Byte.valueOf((byte) 14));
		filter.put("highway=steps", Byte.valueOf((byte) 16));
		filter.put("highway=tertiary", Byte.valueOf((byte) 10));
		filter.put("highway=track", Byte.valueOf((byte) 12));
		filter.put("highway=trunk", Byte.valueOf((byte) 8));
		filter.put("highway=trunk_link", Byte.valueOf((byte) 8));
		filter.put("highway=unclassified", Byte.valueOf((byte) 13));

		filter.put("historic=ruins", Byte.valueOf((byte) 17));

		filter.put("landuse=allotments", Byte.valueOf((byte) 12));
		filter.put("landuse=basin", Byte.valueOf((byte) 15));
		filter.put("landuse=brownfield", Byte.valueOf((byte) 12));
		filter.put("landuse=cemetery", Byte.valueOf((byte) 12));
		filter.put("landuse=commercial", Byte.valueOf((byte) 12));
		filter.put("landuse=construction", Byte.valueOf((byte) 15));
		filter.put("landuse=farm", Byte.valueOf((byte) 12));
		filter.put("landuse=farmland", Byte.valueOf((byte) 12));
		filter.put("landuse=forest", Byte.valueOf((byte) 12));
		filter.put("landuse=grass", Byte.valueOf((byte) 15));
		filter.put("landuse=greenfield", Byte.valueOf((byte) 15));
		filter.put("landuse=industrial", Byte.valueOf((byte) 12));
		filter.put("landuse=military", Byte.valueOf((byte) 12));
		filter.put("landuse=recreation_ground", Byte.valueOf((byte) 12));
		filter.put("landuse=reservoir", Byte.valueOf((byte) 12));
		filter.put("landuse=residential", Byte.valueOf((byte) 12));
		filter.put("landuse=retail", Byte.valueOf((byte) 12));
		filter.put("landuse=village_green", Byte.valueOf((byte) 12));
		filter.put("landuse=wood", Byte.valueOf((byte) 12));

		filter.put("leisure=common", Byte.valueOf((byte) 12));
		filter.put("leisure=garden", Byte.valueOf((byte) 15));
		filter.put("leisure=golf_course", Byte.valueOf((byte) 12));
		filter.put("leisure=park", Byte.valueOf((byte) 12));
		filter.put("leisure=pitch", Byte.valueOf((byte) 15));
		filter.put("leisure=playground", Byte.valueOf((byte) 16));
		filter.put("leisure=sports_centre", Byte.valueOf((byte) 12));
		filter.put("leisure=stadium", Byte.valueOf((byte) 12));
		filter.put("leisure=track", Byte.valueOf((byte) 15));
		filter.put("leisure=water_park", Byte.valueOf((byte) 15));

		filter.put("man_made=pier", Byte.valueOf((byte) 15));

		filter.put("military=airfield", Byte.valueOf((byte) 12));
		filter.put("military=barracks", Byte.valueOf((byte) 12));
		filter.put("military=naval_base", Byte.valueOf((byte) 12));

		filter.put("natural=beach", Byte.valueOf((byte) 14));
		filter.put("natural=coastline", Byte.valueOf((byte) 0));
		filter.put("natural=heath", Byte.valueOf((byte) 12));
		filter.put("natural=land", Byte.valueOf((byte) 12));
		filter.put("natural=scrub", Byte.valueOf((byte) 12));
		filter.put("natural=water", Byte.valueOf((byte) 12));
		filter.put("natural=wood", Byte.valueOf((byte) 12));

		filter.put("place=locality", Byte.valueOf((byte) 17));

		filter.put("railway=light_rail", Byte.valueOf((byte) 12));
		filter.put("railway=rail", Byte.valueOf((byte) 10));
		filter.put("railway=station", Byte.valueOf((byte) 13));
		filter.put("railway=subway", Byte.valueOf((byte) 13));
		filter.put("railway=tram", Byte.valueOf((byte) 13));

		filter.put("route=ferry", Byte.valueOf((byte) 12));

		filter.put("sport=shooting", Byte.valueOf((byte) 15));
		filter.put("sport=tennis", Byte.valueOf((byte) 15));

		filter.put("tourism=attraction", Byte.valueOf((byte) 15));
		filter.put("tourism=hostel", Byte.valueOf((byte) 15));
		filter.put("tourism=zoo", Byte.valueOf((byte) 15));

		filter.put("tunnel=no", Byte.valueOf(Byte.MAX_VALUE));
		filter.put("tunnel=yes", Byte.valueOf(Byte.MAX_VALUE));

		filter.put("waterway=canal", Byte.valueOf((byte) 12));
		filter.put("waterway=drain", Byte.valueOf((byte) 12));
		filter.put("waterway=river", Byte.valueOf((byte) 12));
		filter.put("waterway=riverbank", Byte.valueOf((byte) 12));
		filter.put("waterway=stream", Byte.valueOf((byte) 12));

		return filter;
	}
}
