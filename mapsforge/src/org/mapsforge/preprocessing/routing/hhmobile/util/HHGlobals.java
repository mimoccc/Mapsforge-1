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
package org.mapsforge.preprocessing.routing.hhmobile.util;

/**
 * All constants used by preprocessing and by the runtime classes on the mobile device are put
 * here.
 */
public class HHGlobals {

	public static final byte[] RLE_CLUSTER_BLOCKS_HEADER_MAGIC = "#MAPSFORGE_HH_RLE_CLUSTER_BLOCKS#"
			.getBytes();
	public static final int RLE_CLUSTER_BLOCKS_HEADER_LENGTH = 4096;

	public final static byte[] BINARY_FILE_HEADER_MAGIC = "#MAPSFORGE_HH_BINARY#".getBytes();
	public final static int BINARY_FILE_HEADER_LENGTH = 4096;

	public static final byte[] STATIC_RTREE_HEADER_MAGIC = "#MAPSFORE_HH_STATIC_RTREE#"
			.getBytes();

}
