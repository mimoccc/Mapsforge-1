/* Copyright (c) 2006 MetaCarta, Inc., published under the BSD license.
 * See http://svn.openlayers.org/trunk/openlayers/release-license.txt 
 * for the full text of the license. */

/**
 * @class This class calls a web service via HTTP GET and extracts a list of
 *        points from the XMLHttpRequest object. Finally it constructs a route
 *        and put it on a layer.
 * 
 * @requires OpenLayers/Layer/Markers.js
 * @requires OpenLayers/Ajax.js
 */
OpenLayers.Layer.GPX = OpenLayers.Class.create();
OpenLayers.Layer.GPX.prototype = OpenLayers.Class
		.inherit(
				OpenLayers.Layer.Markers,
				OpenLayers.Layer.Vector,
				{

					/**
					 * store url of text file
					 * 
					 * @type str
					 */
					url : null,
					icolor : null,
					/** @type Array(OpenLayers.Feature) */
					features : null,

					/** @type OpenLayers.Feature */
					selectedFeature : null,

					/**
					 * @constructor
					 * 
					 * @param {String}
					 *            name
					 * @param {String}
					 *            location
					 */
					initialize : function(name, url, icolor, options) {
						// alert(url);
						var newArguments = new Array();
						newArguments.push(name, options);
						OpenLayers.Layer.Vector.prototype.initialize.apply(
								this, newArguments);
						OpenLayers.Layer.Markers.prototype.initialize.apply(
								this, [ name ]);
						this.url = url;
						this.icolor = icolor;
						this.features = new Array();
						var results = OpenLayers.loadURL(this.url, null, this,
								this.requestSuccess, this.requestFailure);
					},

					/**
					 * 
					 */

					destroy : function() {
						this.clearFeatures();
						this.features = null;
						OpenLayers.Layer.Markers.prototype.destroy.apply(this,
								arguments);
					},

					/**
					 * @param {XMLHttpRequest}
					 *            ajaxRequest
					 */
					requestSuccess : function(request) {
						// var gpxns = "http://www.topografix.com/GPX/1/0";
						var doc = request.responseXML;
						if (!doc || request.fileType != "XML") {
							doc = OpenLayers
									.parseXMLString(request.responseText);
						}
						if (typeof doc == "string") {
							doc = OpenLayers.parseXMLString(doc);
						}

						var rte = doc.childNodes;
						// var rte =
						// doc.getElementsByTagName("ns:getRouteResponse");
						var featureRTE = [];
						for ( var i = 0; i < rte.length; i++) {
							var color = this.icolor;
							// var color = this.randomColor();
							var style_green = {
								strokeColor : color,
								strokeOpacity : 1,
								strokeWidth : 4,
								pointRadius : 6,
								pointerEvents : "visiblePainted"
							};
							var pointList = [];
							for ( var j = 0; j < rte[i].childNodes.length; j++) {
								switch (rte[i].childNodes[j].nodeName) {
								case 'ns:return':
									var feature = this.parseFeature(
											rte[i].childNodes[j], j,
											rte[i].childNodes.length);
									if (feature) {
										pointList.push(feature);
									}
									break;
								default:
									break;
								}
							}
							featureRTE.push(new OpenLayers.Feature.Vector(
									new OpenLayers.Geometry.LineString(
											pointList), null, style_green));
						}
						;
						this.addFeatures(featureRTE);
					},

					/**
					 * @param {Event}
					 *            evt
					 */
					randomColor : function() {
						var hex = new Array("0", "1", "2", "3", "4", "5", "6",
								"7", "8", "9", "A", "B", "C", "D", "E", "F");
						var color = '#';
						for (i = 0; i < 6; i++) {
							color += hex[Math.floor(Math.random() * hex.length)];
						}
						return color;
					},

					/**
					 * This function is the core of the GPX parsing code in
					 * OpenLayers. It creates the geometries that are then
					 * attached to the returned feature, and calls
					 * parseAttributes() to get attribute data out.
					 * 
					 * @param DOMElement
					 *            xmlNode
					 */
					parseFeature : function(xmlNode, j, length) {
			            node_lat = xmlNode.childNodes[0].childNodes[0].nodeValue;
			            node_lon = xmlNode.childNodes[1].childNodes[0].nodeValue;
			            if (node_lat && node_lon) {
							var point = this.setToMercator((node_lon / 1000000), (node_lat / 1000000));
/*
 * Preparation for adding a marker at start and end point
 */
							
//							var size = new OpenLayers.Size(21, 25);
//							var offset = new OpenLayers.Pixel(-(size.w / 2),
//									-size.h);
//							startEndLayerMarkers = new OpenLayers.Layer.Markers(
//									"StartEndMarkers");
//							map.addLayer(layerMarkers);
//							if (j == 0) {
//								var slonLat = new OpenLayers.LonLat(
//										((xmlNode.childNodes[3].childNodes[0].nodeValue) / 1000000),
//										((xmlNode.childNodes[2].childNodes[0].nodeValue) / 1000000))
//										.transform(new OpenLayers.Projection(
//												"EPSG:4326"), map
//												.getProjectionObject());
//								startEndLayerMarkers
//										.addMarker(new OpenLayers.Marker(
//												slonLat,
//												new OpenLayers.Icon(
//														'http://www.openstreetmap.org/openlayers/img/marker-green.png',
//														size, offset)));
//							}
//							if (j == (length - 1)) {
//								var elonLat = new OpenLayers.LonLat(
//										((xmlNode.childNodes[3].childNodes[0].nodeValue) / 1000000),
//										((xmlNode.childNodes[2].childNodes[0].nodeValue) / 1000000))
//										.transform(new OpenLayers.Projection(
//												"EPSG:4326"), map
//												.getProjectionObject());
//								startEndLayerMarkers
//										.addMarker(new OpenLayers.Marker(
//												elonLat,
//												new OpenLayers.Icon(
//														'http://www.openstreetmap.org/openlayers/img/marker.png',
//														size, offset)));
//							}
							return new OpenLayers.Geometry.Point(point[0],
									point[1]);
						}
						return false;
					},
					setToMercator : function(lon, lat) {
						x = parseFloat(lon);
						y = parseFloat(lat);
						var PI = 3.14159265358979323846;
						x = x * 20037508.34 / 180;
						y = Math.log(Math.tan((90 + y) * PI / 360))
								/ (PI / 180);
						y = y * 20037508.34 / 180;
						return new Array(x, y);
					},
					/**
					 * 
					 */
					clearFeatures : function() {
						if (this.features != null) {
							while (this.features.length > 0) {
								var feature = this.features[0];
								OpenLayers.Util.removeItem(this.features,
										feature);
								feature.destroy();
							}
						}
					},
					requestFailure : function(request) {
					},
					moveTo : function(bounds, zoomChanged, dragging) {
						OpenLayers.Layer.Vector.prototype.moveTo.apply(this,
								arguments);
						// OpenLayers.Layer.Markers.prototype.moveTo.apply(this,
						// arguments);
						if (!dragging) {
							this.div.style.left = -parseInt(this.map.layerContainerDiv.style.left)
									+ "px";
							this.div.style.top = -parseInt(this.map.layerContainerDiv.style.top)
									+ "px";
							var extent = this.map.getExtent();
							this.renderer.setExtent(extent);
							for (i = 0; i < this.markers.length; i++) {
								marker = this.markers[i];
								lonlat = this.map
										.getLayerPxFromLonLat(marker.lonlat);
								if (marker.icon.calculateOffset) {
									marker.icon.offset = marker.icon
											.calculateOffset(marker.icon.size);
								}
								var offsetPx = lonlat
										.offset(marker.icon.offset);
								marker.icon.imageDiv.style.left = offsetPx.x
										+ parseInt(this.map.layerContainerDiv.style.left)
										+ "px";
								marker.icon.imageDiv.style.top = offsetPx.y
										+ parseInt(this.map.layerContainerDiv.style.top)
										+ "px";
							}

						}
						if (!this.drawn || zoomChanged) {
							this.drawn = true;
							for ( var i = 0; i < this.features.length; i++) {
								var feature = this.features[i];
								this.drawFeature(feature);
							}
						}

					},
					setMap : function(map) {
						// OpenLayers.Layer.Markers.prototype.setMap.apply(this,
					// arguments);
					OpenLayers.Layer.prototype.setMap.apply(this, arguments);

					if (!this.renderer) {
						this.map.removeLayer(this);
					} else {
						this.renderer.map = this.map;
						this.renderer.setSize(this.map.getSize());
					}

				},
				/**
				 * @final
				 * @type String
				 */
				CLASS_NAME : "OpenLayers.Layer.GPX"
				});
