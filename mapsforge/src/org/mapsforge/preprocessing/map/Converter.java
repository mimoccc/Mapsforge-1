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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class Converter {
	private static final short BITMAP_AMENITY = 32;
	private static final short BITMAP_BUILDING = 2;
	private static final short BITMAP_HIGHWAY = 1;
	private static final short BITMAP_LANDUSE = 8;
	private static final short BITMAP_LEISURE = 16;
	private static final short BITMAP_NATURAL = 64;
	private static final short BITMAP_RAILWAY = 4;
	private static final short BITMAP_WATERWAY = 128;
	private static int blocksize;
	private static final int DEFAULT_BLOCKSIZE = 15000;
	private static File fileOut;
	private static FileOutputStream fileOutputStream;
	private static GeoRectangle geoRectangle;
	private static int matrixBlocks;
	private static int matrixHeight;
	private static int matrixWidth;
	private static ArrayList<SortedSet<MapElementNode>> nodeBlocks;
	private static Hashtable<Long, MapElementNode> nodes;
	private static Map<String, Byte> nodeTagLookupTable;
	private static TreeMap<String, Byte> nodeTagWhitelist;
	private static NumberFormat numberFormat;
	private static String sourceFile;
	private static String targetFile;
	private static ArrayList<SortedSet<MapElementWay>> wayBlocks;
	private static TreeMap<Long, MapElementWay> ways;
	private static Map<String, Byte> wayTagLookupTable;
	private static TreeMap<String, Byte> wayTagWhitelist;

	public static void main(String[] args) {
		try {
			boolean assertsEnabled = false;
			assert ((assertsEnabled = true) == true);
			if (!assertsEnabled) {
				System.err.println("asserts are disabled");
			}
			if (args.length < 2 || args.length > 3) {
				System.err.println("usage: sourceFile targetFile [blocksize]");
				System.exit(1);
			}
			sourceFile = args[0];
			targetFile = args[1];
			if (args.length > 2) {
				try {
					blocksize = Integer.parseInt(args[2]);
				} catch (NumberFormatException e) {
					System.err.println("invalid blocksize parameter");
					System.exit(1);
				}
			} else {
				blocksize = DEFAULT_BLOCKSIZE;
			}
			XMLReader xmlReader = XMLReaderFactory.createXMLReader();
			nodes = new Hashtable<Long, MapElementNode>();
			ways = new TreeMap<Long, MapElementWay>();
			nodeTagWhitelist = WhiteList.getNodeTagWhitelist();
			wayTagWhitelist = WhiteList.getWayTagWhitelist();

			OsmContentHandler contentHandler = new OsmContentHandler(nodes, ways,
					nodeTagWhitelist, wayTagWhitelist);
			xmlReader.setContentHandler(contentHandler);
			System.out.println("parsing source file: " + sourceFile);
			xmlReader.parse(sourceFile);

			numberFormat = NumberFormat.getInstance();
			numberFormat.setGroupingUsed(true);
			geoRectangle = contentHandler.getBoundingBox();
			System.out.println(numberFormat.format(nodes.size()) + " nodes, "
					+ numberFormat.format(ways.size()) + " ways");

			matrixWidth = ((geoRectangle.longitude2 - geoRectangle.longitude1) / blocksize) + 1;
			matrixHeight = ((geoRectangle.latitude1 - geoRectangle.latitude2) / blocksize) + 1;
			matrixBlocks = matrixWidth * matrixHeight;
			System.out.println("block size: " + blocksize + ", matrix width: " + matrixWidth
					+ ", matrix height: " + matrixHeight);

			nodeBlocks = new ArrayList<SortedSet<MapElementNode>>(matrixBlocks);
			for (int i = 0; i < matrixBlocks; ++i) {
				nodeBlocks.add(i, new TreeSet<MapElementNode>());
			}

			wayBlocks = new ArrayList<SortedSet<MapElementWay>>(matrixBlocks);
			for (int i = 0; i < matrixBlocks; ++i) {
				wayBlocks.add(i, new TreeSet<MapElementWay>());
			}

			MapElementWay currentWay;
			LinkedList<Long> currentNodesSequence;
			long currentNodeId;
			MapElementNode currentNode;
			int column;
			int row;
			int blockNumber;

			Iterator<MapElementNode> nodeIterator = nodes.values().iterator();
			while (nodeIterator.hasNext()) {
				currentNode = nodeIterator.next();
				currentNode.zoomLevel = getNodeZoomLevel(currentNode);
				if (currentNode.tags.size() == 0 || currentNode.zoomLevel == Byte.MAX_VALUE) {
					continue;
				}

				column = getBlockX(currentNode);
				row = getBlockY(currentNode);
				blockNumber = row * matrixWidth + column;
				nodeBlocks.get(blockNumber).add(currentNode);
			}

			Iterator<MapElementWay> wayIterator = ways.values().iterator();
			Integer newWayId = Integer.valueOf(0);
			assert (ways.size() < Integer.MAX_VALUE);

			while (wayIterator.hasNext()) {
				currentWay = wayIterator.next();
				if (currentWay.multipolygonOuterMemberIds != null) {
					if (currentWay.multipolygonOuterMemberIds.size() == 1) {
						MapElementWay outerWay = ways.get(currentWay.multipolygonOuterMemberIds
								.first());
						for (String outerWayTag : outerWay.tags) {
							currentWay.tags.remove(outerWayTag);
						}
					}
				}
				currentWay.zoomLevel = getWayZoomLevel(currentWay);
				if (currentWay.tags.size() == 0 || currentWay.zoomLevel == Byte.MAX_VALUE) {
					continue;
				} else if (currentWay.nodesSequence.size() < 2) {
					continue;
				}

				calculateInnerWaysAmount(currentWay);
				calculateTagBitmap(currentWay);

				currentWay.newId = newWayId.intValue();
				newWayId = Integer.valueOf(newWayId.intValue() + 1);
				currentNodesSequence = currentWay.nodesSequence;
				Iterator<Long> nodeIdIterator = currentNodesSequence.iterator();
				while (nodeIdIterator.hasNext()) {
					currentNodeId = nodeIdIterator.next().longValue();
					currentNode = nodes.get(Long.valueOf(currentNodeId));
					if (currentNode == null) {
						System.err.println("could not find node " + currentNodeId);
					} else {
						column = getBlockX(currentNode);
						row = getBlockY(currentNode);
						blockNumber = row * matrixWidth + column;
						wayBlocks.get(blockNumber).add(currentWay);
					}
				}
			}

			fileOut = new File(targetFile);
			if (fileOut.exists()) {
				if (!fileOut.isFile()) {
					System.err.println("targetFile is a directory");
					return;
				}
				if (!fileOut.delete()) {
					System.err.println("targetFile could not be deleted");
					return;
				}
			}
			if (!fileOut.createNewFile()) {
				System.err.println("targetFile could not be created");
				return;
			}

			System.out.println("writing to output file: " + fileOut);
			fileOutputStream = new FileOutputStream(fileOut);
			writeInitialFileBlock();
			System.out.println("writing block pointers");
			writeBlockPointers();
			writeBlocks();
			fileOutputStream.close();
			System.out.println("output file has " + (fileOut.length() / 1000) + " kB");
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void calculateInnerWaysAmount(MapElementWay way) {
		if (way.multipolygonInnerMemberIds != null && way.multipolygonInnerMemberIds.size() > 0) {
			Iterator<Long> innerWayIterator = way.multipolygonInnerMemberIds.iterator();
			while (innerWayIterator.hasNext()) {
				MapElementWay currentInnerWay = ways.get(innerWayIterator.next());
				if (currentInnerWay != null && currentInnerWay.nodesSequence.size() > 1) {
					++way.innerWays;
				}
			}
		}
	}

	private static void calculateTagBitmap(MapElementWay way) {
		String key;
		for (String tag : way.tags) {
			key = tag.substring(0, tag.indexOf("="));
			if (key.equals("highway")) {
				way.tagBitmap |= BITMAP_HIGHWAY;
			} else if (key.equals("railway")) {
				way.tagBitmap |= BITMAP_RAILWAY;
			} else if (key.equals("building")) {
				way.tagBitmap |= BITMAP_BUILDING;
			} else if (key.equals("landuse")) {
				way.tagBitmap |= BITMAP_LANDUSE;
			} else if (key.equals("leisure")) {
				way.tagBitmap |= BITMAP_LEISURE;
			} else if (key.equals("amenity")) {
				way.tagBitmap |= BITMAP_AMENITY;
			} else if (key.equals("natural")) {
				way.tagBitmap |= BITMAP_NATURAL;
			} else if (key.equals("waterway")) {
				way.tagBitmap |= BITMAP_WATERWAY;
			}
		}
	}

	private static int getBlockSizeInBytes(int blockId) {
		int blockSizeInBytes = 0;
		// 4 bytes for the number of nodes and ways on each of the 19 zoom
		// levels
		blockSizeInBytes += 19 * 4;
		// 4 bytes for the size of all nodes in this block
		blockSizeInBytes += 4;
		// some bytes for the nodes in this block
		blockSizeInBytes += getNodeListSizeInBytes(nodeBlocks.get(blockId));
		// some bytes for the ways in this block
		blockSizeInBytes += getWayListSizeInBytes(wayBlocks.get(blockId));
		return blockSizeInBytes;
	}

	private static int getBlockX(MapElementNode node) {
		return (node.longitude - geoRectangle.longitude1) / blocksize;
	}

	private static int getBlockY(MapElementNode node) {
		return (geoRectangle.latitude1 - node.latitude) / blocksize;
	}

	private static int getNodeListSizeInBytes(SortedSet<MapElementNode> nodeSet) {
		int nodeListSizeInBytes = 0;
		Iterator<MapElementNode> nodeIterator = nodeSet.iterator();
		while (nodeIterator.hasNext()) {
			nodeListSizeInBytes += getNodeSizeInBytes(nodeIterator.next());
		}
		return nodeListSizeInBytes;
	}

	private static int getNodeSizeInBytes(MapElementNode node) {
		int nodeSizeInBytes = 0;
		// 4 bytes for the longitude
		nodeSizeInBytes += 4;
		// 4 bytes for the latitude
		nodeSizeInBytes += 4;
		// 1 byte for the length of the node name
		nodeSizeInBytes += 1;
		// 1 byte for the amount of tags
		nodeSizeInBytes += 1;
		// some bytes for the node name
		if (node.name != null && node.name.length() > 0) {
			try {
				// FIXME: hack for long node names
				if (node.name.getBytes("UTF-8").length >= Byte.MAX_VALUE) {
					node.name = null;
				} else {
					nodeSizeInBytes += node.name.getBytes("UTF-8").length;
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		// 1 byte for each tag id
		nodeSizeInBytes += 1 * node.tags.size();
		return nodeSizeInBytes;
	}

	private static byte getNodeZoomLevel(MapElementNode node) {
		byte nodeZoomLevel = Byte.MAX_VALUE;
		Iterator<String> tagIterator = node.tags.iterator();
		String currentTag;
		byte currentTagZoomLevel;
		while (tagIterator.hasNext()) {
			currentTag = tagIterator.next();
			currentTagZoomLevel = nodeTagWhitelist.get(currentTag).byteValue();
			if (currentTagZoomLevel < nodeZoomLevel) {
				nodeZoomLevel = currentTagZoomLevel;
			}
		}
		return nodeZoomLevel;
	}

	private static int getNumberOfNodesOnLevel(SortedSet<MapElementNode> nodeSet, byte i) {
		Iterator<MapElementNode> nodeSetIterator = nodeSet.iterator();
		MapElementNode currentNode;
		int numberOfNodes = 0;
		while (nodeSetIterator.hasNext()) {
			currentNode = nodeSetIterator.next();
			if (currentNode.zoomLevel <= i) {
				++numberOfNodes;
			}
		}
		return numberOfNodes;
	}

	private static byte getNumberOfRealTags(LinkedList<String> tags) {
		byte realTags = 0;
		Iterator<String> tagIterator = tags.iterator();
		String currentTag;
		byte currentTagZoomLevel;
		while (tagIterator.hasNext()) {
			currentTag = tagIterator.next();
			currentTagZoomLevel = wayTagWhitelist.get(currentTag).byteValue();
			if (currentTagZoomLevel < Byte.MAX_VALUE) {
				++realTags;
			}
		}
		return realTags;
	}

	private static int getNumberOfWaysOnLevel(SortedSet<MapElementWay> waySet, byte i) {
		Iterator<MapElementWay> waySetIterator = waySet.iterator();
		MapElementWay currentWay;
		int numberOfWays = 0;
		while (waySetIterator.hasNext()) {
			currentWay = waySetIterator.next();
			if (currentWay.zoomLevel <= i) {
				++numberOfWays;
			}
		}
		return numberOfWays;
	}

	private static int getWayListSizeInBytes(SortedSet<MapElementWay> waySet) {
		int wayListSizeInBytes = 0;
		Iterator<MapElementWay> wayIterator = waySet.iterator();
		while (wayIterator.hasNext()) {
			wayListSizeInBytes += getWaySizeInBytes(wayIterator.next());
		}
		return wayListSizeInBytes;
	}

	private static int getWaySizeInBytes(MapElementWay way) {
		int waySizeInBytes = 0;
		// 1 byte for the length of the way name
		waySizeInBytes += 1;

		// 1 byte for the multipolygon flag and the amount of tags
		waySizeInBytes += 1;

		// 2 bytes for the amount of nodes
		waySizeInBytes += 2;

		// 4 bytes for the length of the inner node positions
		if (way.innerWays > 0) {
			waySizeInBytes += 4;
		}

		// 4 bytes for the new way id
		waySizeInBytes += 4;

		// 1 byte for the amount of real tags and for the way layer
		waySizeInBytes += 1;

		// some bytes for the way name
		if (way.name != null && way.name.length() > 0) {
			try {
				waySizeInBytes += way.name.getBytes("UTF-8").length;
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}

		// 1 byte for each tag id
		waySizeInBytes += 1 * way.tags.size();

		// 1 byte for the tag bitmap
		waySizeInBytes += 1;

		// 8 bytes for each node position
		waySizeInBytes += 8 * way.nodesSequence.size();

		// some bytes for the inner node positions
		if (way.innerWays > 0) {
			// 1 byte for the amount of inner ways
			waySizeInBytes += 1;
			Iterator<Long> innerWayIterator = way.multipolygonInnerMemberIds.iterator();
			while (innerWayIterator.hasNext()) {
				MapElementWay currentInnerWay = ways.get(innerWayIterator.next());
				if (currentInnerWay != null && currentInnerWay.nodesSequence.size() > 1) {
					// 2 bytes for the amount of nodes
					waySizeInBytes += 2;
					// 8 bytes for each node position
					waySizeInBytes += 8 * currentInnerWay.nodesSequence.size();
				}
			}
		}
		return waySizeInBytes;
	}

	private static byte getWayZoomLevel(MapElementWay way) {
		byte wayZoomLevel = Byte.MAX_VALUE;
		Iterator<String> tagIterator = way.tags.iterator();
		String currentTag;
		byte currentTagZoomLevel;
		while (tagIterator.hasNext()) {
			currentTag = tagIterator.next();
			currentTagZoomLevel = wayTagWhitelist.get(currentTag).byteValue();
			if (currentTagZoomLevel < wayZoomLevel) {
				wayZoomLevel = currentTagZoomLevel;
			}
		}
		return wayZoomLevel;
	}

	private static void writeBlock(int blockId) {
		try {
			SortedSet<MapElementNode> nodeSet = nodeBlocks.get(blockId);
			SortedSet<MapElementWay> waySet = wayBlocks.get(blockId);
			// write the number of nodes and ways on each of the 19 zoom levels
			int numberOfNodes;
			int numberOfWays;
			for (byte i = 0; i < 19; ++i) {
				numberOfNodes = getNumberOfNodesOnLevel(nodeSet, i);
				numberOfWays = getNumberOfWaysOnLevel(waySet, i);
				assert (numberOfNodes >= 0 && numberOfNodes <= Short.MAX_VALUE);
				fileOutputStream.write(Serializer.getBytes((short) numberOfNodes));
				assert (numberOfWays >= 0 && numberOfWays <= Short.MAX_VALUE);
				fileOutputStream.write(Serializer.getBytes((short) numberOfWays));
			}
			// write the size of all nodes in this block
			int nodeListSizeInBytes = getNodeListSizeInBytes(nodeBlocks.get(blockId));
			assert (nodeListSizeInBytes >= 0);
			fileOutputStream.write(Serializer.getBytes(nodeListSizeInBytes));
			// write all nodes in this block
			writeBlockNodes(nodeSet);
			// write all ways in this block
			writeBlockWays(waySet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void writeBlockNodes(SortedSet<MapElementNode> nodeSet) {
		Iterator<MapElementNode> nodeIterator = nodeSet.iterator();
		while (nodeIterator.hasNext()) {
			writeNode(nodeIterator.next());
		}
	}

	private static void writeBlockPointers() {
		try {
			int currentBlockPointer = 0;
			int currentBlockSize;
			for (int i = 0; i < matrixBlocks; ++i) {
				currentBlockSize = getBlockSizeInBytes(i);
				// write the block id
				assert (i >= 0);
				fileOutputStream.write(Serializer.getBytes(i));
				// write the block pointer
				assert (currentBlockPointer >= 0);
				fileOutputStream.write(Serializer.getBytes(currentBlockPointer));
				// write the block size
				assert (currentBlockSize >= 0);
				fileOutputStream.write(Serializer.getBytes(currentBlockSize));
				currentBlockPointer += currentBlockSize;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void writeBlocks() {
		int logIntervall = (int) Math.ceil(matrixBlocks / 4f);
		for (int i = 0; i < matrixBlocks; ++i) {
			if (i % logIntervall == 0) {
				System.out.println("writing block " + i + " ("
						+ (int) ((i / (float) matrixBlocks) * 100) + "%)");
			}
			writeBlock(i);
		}
	}

	private static void writeBlockWays(SortedSet<MapElementWay> waySet) {
		Iterator<MapElementWay> wayIterator = waySet.iterator();
		while (wayIterator.hasNext()) {
			writeWay(wayIterator.next());
		}
	}

	private static void writeInitialFileBlock() {
		try {
			// write bounding box values
			assert (geoRectangle.longitude1 <= 180000000);
			fileOutputStream.write(Serializer.getBytes(geoRectangle.longitude1));
			assert (geoRectangle.latitude1 <= 90000000);
			fileOutputStream.write(Serializer.getBytes(geoRectangle.latitude1));
			assert (geoRectangle.longitude2 >= -180000000);
			fileOutputStream.write(Serializer.getBytes(geoRectangle.longitude2));
			assert (geoRectangle.latitude2 >= -90000000);
			fileOutputStream.write(Serializer.getBytes(geoRectangle.latitude2));

			// write block matrix values
			assert (matrixWidth > 0);
			fileOutputStream.write(Serializer.getBytes(matrixWidth));
			assert (matrixHeight > 0);
			fileOutputStream.write(Serializer.getBytes(matrixHeight));
			assert (blocksize > 0);
			fileOutputStream.write(Serializer.getBytes(blocksize));

			System.out.println(nodeTagWhitelist.size() + " node tags");
			System.out.println(wayTagWhitelist.size() + " way tags");

			nodeTagLookupTable = TagIdsPOIs.getMap();
			wayTagLookupTable = TagIdsWays.getMap();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void writeNode(MapElementNode node) {
		try {
			// write the longitude of the node
			assert (node.longitude <= 180000000 && node.longitude >= -180000000);
			fileOutputStream.write(Serializer.getBytes(node.longitude));

			// write the latitude of the node
			assert (node.latitude <= 90000000 && node.latitude >= -90000000);
			fileOutputStream.write(Serializer.getBytes(node.latitude));

			// write the length of the node name
			if (node.name != null && node.name.length() > 0) {
				// FIXME: dirty hack to remove long names
				if (node.name.getBytes("UTF-8").length >= Byte.MAX_VALUE) {
					node.name = null;
					fileOutputStream.write((byte) 0);
				} else {
					assert (node.name.getBytes("UTF-8").length > 0 && node.name
							.getBytes("UTF-8").length < Byte.MAX_VALUE);
					fileOutputStream.write((byte) node.name.getBytes("UTF-8").length);
				}
			} else {
				fileOutputStream.write((byte) 0);
			}

			// write the amount of tags
			assert (node.tags != null && node.tags.size() > 0 && node.tags.size() < Byte.MAX_VALUE);
			fileOutputStream.write((byte) node.tags.size());

			// write the node name
			if (node.name != null && node.name.length() > 0) {
				fileOutputStream.write(node.name.getBytes("UTF-8"));
			}

			// write the node tags
			if (node.tags != null) {
				Iterator<String> tagIterator = node.tags.iterator();
				String currentTag;
				byte currentTagId = 0;
				while (tagIterator.hasNext()) {
					currentTag = tagIterator.next();
					try {
						currentTagId = nodeTagLookupTable.get(currentTag).byteValue();
					} catch (NullPointerException e) {
						System.err.println("error writing node: could not find tag id for tag "
								+ currentTag);
						System.exit(1);
					}
					// write the current tag id
					assert (currentTagId >= 0);
					fileOutputStream.write(currentTagId);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void writeWay(MapElementWay way) {
		try {
			// write the length of the way name
			if (way.name != null && way.name.length() > 0) {
				assert (way.name.getBytes("UTF-8").length > 0 && way.name.getBytes("UTF-8").length < Byte.MAX_VALUE);
				fileOutputStream.write((byte) way.name.getBytes("UTF-8").length);
			} else {
				fileOutputStream.write((byte) 0);
			}

			// write the multipolygon flag and the amount of tags
			byte multipolygonFlag;
			if (way.innerWays > 0) {
				multipolygonFlag = 1;
			} else {
				multipolygonFlag = 0;
			}
			assert (way.tags != null && way.tags.size() > 0 && way.tags.size() < 64);
			byte byteToWrite = (byte) (multipolygonFlag << 7 | (byte) (way.tags.size()));
			fileOutputStream.write(byteToWrite);

			// write the amount of nodes
			assert (way.nodesSequence != null && way.nodesSequence.size() > 1 && way.nodesSequence
					.size() < Short.MAX_VALUE);
			fileOutputStream.write(Serializer.getBytes((short) way.nodesSequence.size()));

			// write the size of the inner ways and nodes
			if (way.innerWays > 0) {
				int innerBlockSize = 0;
				// 1 byte for the amount of inner ways
				innerBlockSize += 1;
				Iterator<Long> innerWayIterator = way.multipolygonInnerMemberIds.iterator();
				while (innerWayIterator.hasNext()) {
					MapElementWay currentInnerWay = ways.get(innerWayIterator.next());
					if (currentInnerWay != null && currentInnerWay.nodesSequence.size() > 1) {
						// 2 bytes for the amount of nodes
						innerBlockSize += 2;
						// 8 bytes for each node position
						innerBlockSize += 8 * currentInnerWay.nodesSequence.size();
					}
				}
				assert (innerBlockSize > 0 && innerBlockSize < Integer.MAX_VALUE);
				fileOutputStream.write(Serializer.getBytes(innerBlockSize));
			}

			// write the new way id
			assert (way.newId >= 0);
			fileOutputStream.write(Serializer.getBytes(way.newId));

			// write the amount of real tags and the way layer
			byte numberOfRealTags = getNumberOfRealTags(way.tags);
			assert (numberOfRealTags > 0 && numberOfRealTags < 8);
			// FIXME: hack for invalid layers
			if (way.layer < -5) {
				way.layer = -5;
			} else if (way.layer > 5) {
				way.layer = 5;
			}
			assert (way.layer >= -5 && way.layer <= 5);
			byteToWrite = (byte) (numberOfRealTags << 4 | (way.layer + 5));
			fileOutputStream.write(byteToWrite);

			// write the way name
			if (way.name != null && way.name.length() > 0) {
				fileOutputStream.write(way.name.getBytes("UTF-8"));
			}

			// write the way tags
			if (way.tags != null) {
				Iterator<String> tagIterator = way.tags.iterator();
				String currentTag;
				byte currentTagId = 0;
				while (tagIterator.hasNext()) {
					currentTag = tagIterator.next();
					try {
						currentTagId = wayTagLookupTable.get(currentTag).byteValue();
					} catch (NullPointerException e) {
						System.err.println("error writing way: could not find tag id for tag "
								+ currentTag);
						System.exit(1);
					}
					// write the current tag id
					assert (currentTagId >= 0);
					fileOutputStream.write(currentTagId);
				}
			}

			// write the way tag bitmap
			fileOutputStream.write(way.tagBitmap);

			// write the way nodes
			if (way.nodesSequence != null) {
				Iterator<Long> nodeIterator = way.nodesSequence.iterator();
				MapElementNode currentNode;
				while (nodeIterator.hasNext()) {
					currentNode = nodes.get(nodeIterator.next());
					// write the current node longitude
					assert (currentNode.longitude <= 180000000 && currentNode.longitude >= -180000000);
					fileOutputStream.write(Serializer.getBytes(currentNode.longitude));
					// write the current node latitude
					assert (currentNode.latitude <= 90000000 && currentNode.latitude >= -90000000);
					fileOutputStream.write(Serializer.getBytes(currentNode.latitude));
				}
			}

			if (way.innerWays > 0) {
				// write the amount of inner ways
				assert (way.innerWays > 0 && way.innerWays < Byte.MAX_VALUE);
				fileOutputStream.write(way.innerWays);

				Iterator<Long> wayIdIterator = way.multipolygonInnerMemberIds.iterator();
				Iterator<Long> nodeIdIterator;
				while (wayIdIterator.hasNext()) {
					MapElementWay currentInnerWay = ways.get(wayIdIterator.next());
					if (currentInnerWay != null && currentInnerWay.nodesSequence.size() > 1) {
						// write the amount of nodes
						assert (currentInnerWay.nodesSequence.size() > 1 && currentInnerWay.nodesSequence
								.size() < Short.MAX_VALUE);
						fileOutputStream.write(Serializer
								.getBytes((short) currentInnerWay.nodesSequence.size()));

						nodeIdIterator = currentInnerWay.nodesSequence.iterator();
						while (nodeIdIterator.hasNext()) {
							MapElementNode currentInnerNode = nodes.get(nodeIdIterator.next());
							// write the current inner node longitude
							assert (currentInnerNode.longitude <= 180000000 && currentInnerNode.longitude >= -180000000);
							fileOutputStream.write(Serializer
									.getBytes(currentInnerNode.longitude));
							// write the current inner node latitude
							assert (currentInnerNode.latitude <= 90000000 && currentInnerNode.latitude >= -90000000);
							fileOutputStream.write(Serializer
									.getBytes(currentInnerNode.latitude));
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}