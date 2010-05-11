// Start position for the map (hardcoded here for simplicity)

//var lat = 52.45579; // Koordinaten vom FB Inf
//var lon = 13.29751;
var leftDivWidth;

var resizeMapWindow = function resizeMap() {
	windowWidth = window.innerWidth || document.documentElement.clientWidth || document.body.clientWidth;
	windowHeight = window.innerHeight || document.documentElement.clientHeight || document.body.clientHeight;
	document.getElementById("map").style.height = windowHeight - 10; // 10 because body padding
	leftDivWidth = parseInt(document.getElementById("leftDiv").offsetWidth);
	document.getElementById("map").style.width = windowWidth - leftDivWidth;
}

window.onresize = resizeMapWindow;
/**
 * Entry Point and zoom level of the OpenStreetMap map
 */
var lon = 13.377655; // Koordinaten vom Brandenburger Tor
var lat = 52.516471;

var zoom = 11;

var routelayer;
var marker;
var menupopup;

/**
 * Base URL of the Web Service.
 */
var host = "/axis2/services/OSMService/";

var map; // complex object of type OpenLayers.Map

function mf_contextmenu(e) {
  
	clickX = e.pageX - leftDivWidth + 10; // 10 because of padding
	clickY = e.pageY - 5; // 5 also because of padding
	clickLatLonMapProj = map.getLonLatFromPixel(new OpenLayers.Pixel(clickX, clickY));
	//console.log(clickLatLonMapProj);
  clickLatLonWSG84Proj = clickLatLonMapProj.clone().transform(map.getProjectionObject(), new OpenLayers.Projection("EPSG:4326"));
  menuhtml = '<div id="popupmenu"><a onclick="setValues(' + Math.round(clickLatLonWSG84Proj.lon*1000000) + ',' + Math.round(clickLatLonWSG84Proj.lat*1000000) + ',\'' + clickLatLonWSG84Proj.lat + ', ' + clickLatLonWSG84Proj.lon + '\', \'start\')">Set Start</a><br>'
           + '<a onclick="setValues(' + Math.round(clickLatLonWSG84Proj.lon*1000000) + ',' + Math.round(clickLatLonWSG84Proj.lat*1000000) + ',\'' + clickLatLonWSG84Proj.lat + ', ' + clickLatLonWSG84Proj.lon + '\', \'stop\')">Set Via</a><br>'
           + '<a onclick="setValues(' + Math.round(clickLatLonWSG84Proj.lon*1000000) + ',' + Math.round(clickLatLonWSG84Proj.lat*1000000) + ',\'' + clickLatLonWSG84Proj.lat + ', ' + clickLatLonWSG84Proj.lon + '\', \'end\')">Set End</a></div>';
  
  menupopup = new OpenLayers.Popup("menu",
                               clickLatLonMapProj,
                               new OpenLayers.Size(80,60),
                               menuhtml,
                               false);
  map.addPopup(menupopup, true);
};


/**
 * Initialization of the OpenStreetMap map.
 */
function init() {
	
	resizeMapWindow();
	
	//clearFields();
	map = new OpenLayers.Map("map", {
		controls : [ new OpenLayers.Control.Navigation(),
				new OpenLayers.Control.PanZoomBar(),
				new OpenLayers.Control.LayerSwitcher(),
				new OpenLayers.Control.Attribution() ],
		maxExtent : new OpenLayers.Bounds(-20037508.34, -20037508.34,
				20037508.34, 20037508.34),
		maxResolution : 156543.0399,
		numZoomLevels : 19,
		units : 'm',
		projection : new OpenLayers.Projection("EPSG:900913"),
		displayProjection : new OpenLayers.Projection("EPSG:4326")
	});
	
	map.div.oncontextmenu = function cm(e) {
		mf_contextmenu(e);
		return false;
	};
	
	layerMapnik = new OpenLayers.Layer.OSM.Mapnik("Mapnik");
	map.addLayer(layerMapnik);
	layerTilesAtHome = new OpenLayers.Layer.OSM.Osmarender("Osmarender");
	map.addLayer(layerTilesAtHome);
	layerCycleMap = new OpenLayers.Layer.OSM.CycleMap("CycleMap");
	map.addLayer(layerCycleMap);
	layerMarkers = new OpenLayers.Layer.Markers("Markers");
	map.addLayer(layerMarkers);

	var lonLat = new OpenLayers.LonLat(lon, lat).transform(
			new OpenLayers.Projection("EPSG:4326"), map.getProjectionObject());
	map.setCenter(lonLat, zoom);
}

/**
 * Hands over the parameters to a HTTP GET request function to call the
 * corresponding WebService for calculating the route.
 */
function getroute() {

	clearMap();
	document.getElementById("resultAreaInfo").innerHTML = "";
	document.getElementById("resultAreaStart").innerHTML = "";
	document.getElementById("resultAreaStop").innerHTML = "";
	document.getElementById("resultAreaEnd").innerHTML = "";
	
	route = {
    start : {
    	lon : document.search.startlon.value,
    	lat : document.search.startlat.value,
    	text : document.search.start.value
    },
    via : {
    	lon : document.search.stoplon.value,
    	lat : document.search.stoplat.value,
    	text : document.search.stop.value
    },
  	end : {
    	lon : document.search.endlon.value,
    	lat : document.search.endlat.value,
    	text : document.search.end.value
    }
  }

  // origin:
  route.markup = "<h3>Route:</h3><b>Von:</b><br/><img src=\"http://www.openlayers.org/dev/img/marker-green.png\">" + route.start.text;
  route.requestString = host + "getRoute?points=" + route.start.lon + "," + route.start.lat + ";";
  // via (if applicable):
  if (route.via.text != "" && route.via.lon != "" && route.via.lat != "") {
  	route.markup += "<br/><b>&Uuml;ber:</b><br/><img src=\"http://www.openlayers.org/dev/img/marker-gold.png\">" + route.via.text;
  	route.requestString += route.via.lon + "," + route.via.lat + ";";
  }
  // destination:
  route.markup += "<br/><b>Nach:</b><br/><img src=\"http://www.openlayers.org/dev/img/marker.png\">" + route.end.text;
  route.requestString += route.end.lon + "," + route.end.lat;

  // find and show route
  if (route.start.lon != "" && route.start.lat != "" && route.end.lon != "" && route.end.lat != "" ) {
    // route
    route.layer = new OpenLayers.Layer.GPX(route.start.text + " -> " + route.end.text, route.requestString, "#FF0000");
    map.addLayer(route.layer);

  	document.getElementById("resultAreaInfo").innerHTML = route.markup;
	}
}

/**
 * Calls the getpois function as start point extraction
 * 
 * @param ajaxRequest
 *            XMLHttpRequest object
 */
function getpoisStart(ajaxRequest) {
	getpois(ajaxRequest, "start");
}

/**
 * Calls the getpois function as end point extraction
 * 
 * @param ajaxRequest
 *            XMLHttpRequest object
 */
function getpoisEnd(ajaxRequest) {
	getpois(ajaxRequest, "end");
}

/**
 * Calls the getpois function as intermediate stop extraction
 * 
 * @param ajaxRequest
 *            XMLHttpRequest object
 */
function getpoisStop(ajaxRequest) {
	getpois(ajaxRequest, "stop");
}

/**
 * Extracts the start and end points from the XMLHttpRequest and adds a layer
 * 
 * @param ajaxRequest
 *            XMLHttpRequest object
 * @param isStart
 *            true, if given point is a start point; false, otherwise
 */
function getpois(ajaxRequest, status) {

	if (marker != undefined) {
		for ( var i = 0, len = map.popups.length; i < len; i++) {
			map.removePopup(map.popups[i]);
		}
		map.removeLayer(marker);
		marker = undefined;
	}
	var doc = ajaxRequest.responseXML;
	if (!doc || ajaxRequest.fileType != "XML") {
		doc = OpenLayers.parseXMLString(ajaxRequest.responseText);
	}
	if (typeof doc == "string") {
		doc = OpenLayers.parseXMLString(doc);
	}
	var pois = doc.childNodes[0].childNodes;
	var pois_text = "lat\tlon\ttitle\tdescription\ticon\ticonSize\ticonOffset\n";
	var points = "";
	var result = "";
	if (pois.length == 0) {
		result = "<h3>Kein Ergebnis</h3>Bitte Suche verfeinern.";
	}
	if (pois.length == 1) {
		result = "<h3>1 Ergebnis:</h3>";
	}
	if (pois.length > 1) {
		result = "<h3>" + pois.length + " Ergebnisse:</h3>";
	}
	for ( var i = 0; i < pois.length; i++) {
		var p = pois[i].childNodes;
		for ( var j = 0; j < (p.length); j++) {
			if (j < p.length - 3) {
				if (p[j].childNodes[0].childNodes[0].nodeValue == "name") {
					if (status == "start") {
						result = result
								+ "<img src=\"http://www.openlayers.org/dev/img/marker-green.png\">"
								+ p[j].childNodes[1].childNodes[0].nodeValue
								+ "<br/>";
					}
					if (status == "end") {
						result = result
								+ "<img src=\"http://www.openlayers.org/dev/img/marker.png\">"
								+ p[j].childNodes[1].childNodes[0].nodeValue
								+ "<br/>";
					}
					if (status == "stop") {
						result = result
								+ "<img src=\"http://www.openlayers.org/dev/img/marker-gold.png\">"
								+ p[j].childNodes[1].childNodes[0].nodeValue
								+ "<br/>";
					}
					var title = "\t"
							+ p[j].childNodes[1].childNodes[0].nodeValue;
				}
			}
			if (p[j].nodeName.substr(p[j].nodeName.length - 3,
					p[j].nodeName.length) == "lon") {
				var lat = (p[j].childNodes[0].nodeValue);
			}
			if (p[j].nodeName.substr(p[j].nodeName.length - 3,
					p[j].nodeName.length) == "lat") {
				var lon = (p[j].childNodes[0].nodeValue);
			}
			if (j == p.length - 1) {
				if (pois.length == 1) {
					if (status == "start") {
						setValues(lon, lat, title.substring(1), "start");
						var desc = "\t ";
						result = result + desc;
					}
					if (status == "end") {
						setValues(lon, lat, title.substring(1), "end");
						var desc = "\t ";
						result = result + desc;
					}
					if (status == "stop") {
						setValues(lon, lat, title.substring(1), "stop");
						var desc = "\t ";
						result = result + desc;
					}
				} else {
					if (status == "start") {
						var desc = "\t<form name=\"setPoint\">"
								+ "<input type=\"button\" value=\"Startpunkt setzen\" "
								+ "onclick=\"setValues(" + lon + "," + lat
								+ ",'" + title.substring(1) + "','start');\""
								+ " />" + "</form>";
						result = result + desc;
						if (i < (pois.length - 1)) {
							result = result
									+ "<hr align=\"left\" width=150px />";
						}
					}
					if (status == "end") {
						var desc = "\t<form name=\"setPoint\">"
								+ "<input type=\"button\" value=\"Endpunkt setzen\" "
								+ "onclick=\"setValues(" + lon + "," + lat
								+ ",'" + title.substring(1) + "','end');\""
								+ " />" + "</form>";
						result = result + desc;
						if (i < (pois.length - 1)) {
							result = result
									+ "<hr align=\"left\" width=150px />";
						}
					}
					if (status == "stop") {
						var desc = "\t<form name=\"setPoint\">"
								+ "<input type=\"button\" value=\"Halt setzen\" "
								+ "onclick=\"setValues(" + lon + "," + lat
								+ ",'" + title.substring(1) + "','stop');\""
								+ " />" + "</form>";
						result = result + desc;
						if (i < (pois.length - 1)) {
							result = result
									+ "<hr align=\"left\" width=150px />";
						}
					}
				}
			}
		}
		if (status == "start") {
			points = points
					+ (lat / 1000000)
					+ "\t"
					+ (lon / 1000000)
					+ title
					+ desc
					+ "\thttp://www.openlayers.org/dev/img/marker-green.png\t21,25\t-10,-25\n";
		}
		if (status == "end") {
			points = points
					+ (lat / 1000000)
					+ "\t"
					+ (lon / 1000000)
					+ title
					+ desc
					+ "\thttp://www.openlayers.org/dev/img/marker.png\t21,25\t-10,-25\n";
		}
		if (status == "stop") {
			points = points
					+ (lat / 1000000)
					+ "\t"
					+ (lon / 1000000)
					+ title
					+ desc
					+ "\thttp://www.openlayers.org/dev/img/marker-gold.png\t21,25\t-10,-25\n";
		}
	}

	// http://www.openlayers.org/dev/img/marker-blue.png -> Blau
	// http://www.openlayers.org/dev/img/marker-gold.png -> Gelb
	// http://www.openlayers.org/dev/img/marker-green.png -> Grün
	// http://www.openlayers.org/dev/img/marker.png -> Rot

	var loc = pois_text + points;

	if (status == "start") {
		marker1 = new OpenLayers.Layer.Textmod("StartPoints", {
			location : loc,
			projection : map.displayProjection
		});
		map.addLayer(marker1);
		document.getElementById("resultAreaStart").innerHTML = result;
	}
	if (status == "end") {
		marker2 = new OpenLayers.Layer.Textmod("EndPoints", {
			location : loc,
			projection : map.displayProjection
		});
		map.addLayer(marker2);
		document.getElementById("resultAreaEnd").innerHTML = result;
	}
	if (status == "stop") {
		marker3 = new OpenLayers.Layer.Textmod("StopPoints", {
			location : loc,
			projection : map.displayProjection
		});
		map.addLayer(marker3);
		document.getElementById("resultAreaStop").innerHTML = result;
	}
}

/**
 * Sets the form parameter at the index.html
 * 
 * @param lon
 *            longitude of the given coordinate
 * @param lat
 *            latitude of the given coordinate
 * @param desc
 *            textual description shown in the text field
 * @param status
 *            declares, which form parameters will be set
 */
function setValues(lon, lat, desc, status) {
	if (status == "start") {
		document.search.startlon.value = lon;
		document.search.startlat.value = lat;
		document.search.start.value = desc;
	}
	if (status == "end") {
		document.search.endlon.value = lon;
		document.search.endlat.value = lat;
		document.search.end.value = desc;
	}
	if (status == "stop") {
		document.search.stoplon.value = lon;
		document.search.stoplat.value = lat;
		document.search.stop.value = desc;
	}
	for ( var i = 0, len = map.popups.length; i < len; i++) {
		map.removePopup(map.popups[i]);
	}
	getroute();
}

/**
 * Hands over the search parameters to a HTTP GET request function to call the
 * corresponding WebService.
 * 
 * @param start
 *            search string for start point
 * @param stop
 *            search string for intermediate stop point
 * @param end
 *            search string for end point
 */
function getStartEndPoints(start, stop, end) {

	newRequest();

	document.search.start.value = start;
	document.search.stop.value = stop;
	document.search.end.value = end;

	// Start Points
	document.getElementById("resultAreaStart").innerHTML = "";
	var poi_url = host + "getGeoLocation?searchString=" + start
			+ "&wanted=0&max=3";
	OpenLayers.Request.GET( {
		url : poi_url,
		success : getpoisStart,
		scope : this
	});

	// End Points
	document.getElementById("resultAreaEnd").innerHTML = "";
	var poi_url = host + "getGeoLocation?searchString=" + end
			+ "&wanted=0&max=3";
	OpenLayers.Request.GET( {
		url : poi_url,
		success : getpoisEnd,
		scope : this
	});

	// intermediate stop
	if (stop != "") {
		document.getElementById("resultAreaStop").innerHTML = "";
		var poi_url = host + "getGeoLocation?searchString=" + stop
				+ "&wanted=0&max=3";
		OpenLayers.Request.GET( {
			url : poi_url,
			success : getpoisStop,
			scope : this
		});
	}
}

/**
 * Clears all input fields
 */
function clearFields() {
	document.getElementById("resultAreaStart").innerHTML = "";
	document.getElementById("resultAreaEnd").innerHTML = "";
	document.getElementById("resultAreaStop").innerHTML = "";
	document.getElementById("resultAreaInfo").innerHTML = "";

	document.search.startlon.value = "";
	document.search.startlat.value = "";
	document.search.start.value = "";

	document.search.stoplon.value = "";
	document.search.stoplat.value = "";
	document.search.stop.value = "";

	document.search.endlon.value = "";
	document.search.endlat.value = "";
	document.search.end.value = "";
}

/**
 * Removes all layers from Map
 */
function clearMap() {
	for ( var i = 0, len = map.popups.length; i < len; i++) {
		map.removePopup(map.popups[i]);
	}
	for ( var i = 0; i < (map.layers.length); i++) {
		if (map.layers[i].name != "Mapnik"
				&& map.layers[i].name != "Osmarender"
				&& map.layers[i].name != "Markers"
				&& map.layers[i].name != "CycleMap") {
			map.removeLayer(map.layers[i]);
			i--;
		}
	}
}

/**
 * Clean all input fields and OpenLayers layers.
 */
function newRequest() {
	// neue Anfrage
	clearMap();
	clearFields();
}

/**
 * Requests the WebService to retrieve the feature info 
 */
function info() {
	var url = host + "getFeatures";
	OpenLayers.Request.GET( {
		url : url,
		success : infosucc,
		scope : this
	});
}

/**
 * extracts the feature informations from the XMLHttpRequest
 * 
 * @param ajaxRequest
 *            XMLHttpRequest object
 */
function infosucc(ajaxRequest) {
	var desc;
	var transport;
	var algorithm;
	var doc = ajaxRequest.responseXML;
	if (!doc || ajaxRequest.fileType != "XML") {
		doc = OpenLayers.parseXMLString(ajaxRequest.responseText);
	}
	if (typeof doc == "string") {
		doc = OpenLayers.parseXMLString(doc);
	}
	var infos = doc.childNodes[0].childNodes[0].childNodes;
	for ( var i = 0; i < infos.length; i++) {
		if (infos[i].nodeName.split(":")[1] == "algorithm") {
			algorithm = infos[i].childNodes[0].nodeValue;
		}
		if (infos[i].nodeName.split(":")[1] == "description") {
			desc = infos[i].childNodes[0].nodeValue;
		}
		if (infos[i].nodeName.split(":")[1] == "transportType") {
			transport = infos[i].childNodes[0].nodeValue;
		}
	}
	alert("Informationen zum verwendeten Routing:\n\n" + "Beschreibung: \t\t"
			+ desc + "\n" + "Algorithmus: \t\t" + algorithm + "\n"
			+ "Transportmittel: \t" + transport);
}
