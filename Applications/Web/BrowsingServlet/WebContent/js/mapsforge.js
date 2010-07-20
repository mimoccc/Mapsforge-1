/**
 * Entry Point and zoom level of the initial map
 * Coordinates of Brandenburger Tor
 */
var initMap = {
  lon : 8.8,
  lat : 53.1,
  zoom : 14
};

/**
 * Initialization of the OpenStreetMap map.
 */
function init() {
  // set map div size once
  resizeMapWindow();

	map = new OpenLayers.Map("map", {
		controls : [ new OpenLayers.Control.Navigation(),
				new OpenLayers.Control.PanZoomBar(),
				new OpenLayers.Control.LayerSwitcher(),
				new OpenLayers.Control.MousePosition(),
				new OpenLayers.Control.Attribution() ],
		projection : projSpheMe,
		displayProjection : projWSG84
	});
  // disable regular right click menu, call other function in stead
	map.div.oncontextmenu = function cm(e) {
		mf_contextmenu(e);
		return false;
	};

	layerMapnik = new OpenLayers.Layer.OSM();
	map.addLayer(layerMapnik);
	layerMarkers = new OpenLayers.Layer.Markers("Markers");
	map.addLayer(layerMarkers);
	
	var styleMap = new OpenLayers.StyleMap(OpenLayers.Util.applyDefaults({
      fillColor: "#FFBBBB",
      fillOpacity: 0.5,
      strokeColor: "#FF0000",
      strokeOpacity: 0.7,
      strokeWidth: 3
    },OpenLayers.Feature.Vector.style["default"]));

	layerVectors = new OpenLayers.Layer.Vector("Vectors", {styleMap:styleMap});
	map.addLayer(layerVectors);
	
	// Super awesome drag controler allows dragging of start and end points
  mf_dragcontrol = new OpenLayers.Control.DragFeature(layerVectors, {
      geometryTypes: ["OpenLayers.Geometry.Point"],
      onDrag: mf_dragpoint,
      onStart: mf_dragstart,
      onComplete: mf_dragcomplete
    });
	map.addControl(mf_dragcontrol);
	mf_dragcontrol.activate();

  var centerLonLat = new OpenLayers.LonLat(initMap.lon, initMap.lat).tf();
  map.setCenter(centerLonLat, initMap.zoom);
}

var map; // complex object of type OpenLayers.Map
var projWSG84 = new OpenLayers.Projection("EPSG:4326");
var projSpheMe = new OpenLayers.Projection("EPSG:900913");

/**
 * Helper function for not writing the same transformation over and over again
 * 
 * @return this LonLat object in the projection of the OpenStreetMap layer
 */
OpenLayers.LonLat.prototype.tf = function() {
  if (this.tf_result == undefined) {
    this.tf_result = this.clone().transform(projWSG84, projSpheMe);
    this.tf_result.rtf_result = this;
  }
  return this.tf_result;
};

/**
 * Helper function for not writing the same transformation over and over again
 * 
 * @return this LonLat object in the WSG 1984 projection 
 */
OpenLayers.LonLat.prototype.rtf = function() {
  if (this.rtf_result == undefined) {
    this.rtf_result = this.clone().transform(projSpheMe, projWSG84);
    this.rtf_result.tf_result = this;
  }
  return this.rtf_result;
};

// The route object contains all the info relevant to the routing
var route = {
  start: {},
  via: {},
  end: {}
};

/**
 * Right click menu is created by this function
 */
var menupopup;
function mf_contextmenu(e) {

  clickX = e.pageX - leftDivWidth + 10; // 10 because of padding
  clickY = e.pageY - 5; // 5 also because of padding
  clickLatLon = map.getLonLatFromPixel(new OpenLayers.Pixel(clickX, clickY)).rtf();
  newValues = clickLatLon.lon
              + ',' + clickLatLon.lat
              + ',\'' + clickLatLon.lat
              + ', ' + clickLatLon.lon+ '\',';
  menuhtml = '<div id="popupmenu"><a onclick="setValues(' + newValues + '\'start\')">Set Start</a><br>'
           + '<a onclick="setValues(' + newValues + '\'via\')">Set Via</a><br>'
           + '<a onclick="setValues(' + newValues + '\'end\')">Set End</a></div>';
  
  menupopup = new OpenLayers.Popup("menu",
                               clickLatLon.tf(),
                               new OpenLayers.Size(80,60),
                               menuhtml,
                               false);
  map.addPopup(menupopup, true);
};


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
  route[status].lon = lon;
  route[status].lat = lat;
  route[status].text = desc;
	document.search[status].value = desc;
	for ( var i = 0, len = map.popups.length; i < len; i++) {
		map.removePopup(map.popups[i]);
	}
	hhRoute();
}

/**
 * Clean all input fields and OpenLayers layers.
 */
function newRequest() {
  //remove popups
  for ( var i = 0, len = map.popups.length; i < len; i++) {
		map.removePopup(map.popups[i]);
	}
	//remove vectors
  if(route.line) layerVectors.removeFeatures(route.line);
  if(startCircle) layerVectors.removeFeatures(startCircle);
  if(endCircle) layerVectors.removeFeatures(endCircle);
  // Clear all fields in the html
  document.getElementById("resultAreaStart").innerHTML = "";
	document.getElementById("resultAreaEnd").innerHTML = "";
	document.getElementById("resultAreaStop").innerHTML = "";
	document.getElementById("resultAreaInfo").innerHTML = "";
	document.search.start.value = "";
	document.search.via.value = "";
	document.search.end.value = "";
}

route.drag = {}; // encapsulate the dragging stuff
route.drag.isdragging = false;
//route.drag.date = new Date;
//route.drag.lastDrag = route.drag.date.getTime();
function mf_dragpoint(feature, pixel) {
  route.drag.date = new Date;
  // Enforce a minimum delay of 300 seconds between routing requests
  // if ((route.drag.date.getTime() - route.drag.lastDrag > 300) && !(route.waitingForResponse) ) {
  if (!(route.waitingForResponse) ) {
    //route.drag.lastDrag = route.drag.date.getTime();
  	route.drag.LonLat = map.getLonLatFromPixel(new OpenLayers.Pixel(pixel.x, pixel.y));
    setValues(route.drag.LonLat.rtf().lon, route.drag.LonLat.rtf().lat, route.drag.LonLat.rtf().lon +", "+ route.drag.LonLat.rtf().lat, feature.attributes.mf);
  }
}
function mf_dragstart(feature, pixel) {
  route.drag.isdragging = true;
  route.drag.which = feature.attributes.mf;
}
function mf_dragcomplete(feature, pixel) {
  route.drag.which = null;
  route.drag.isdragging = false;
  redrawEndPoints();
  //setTimeout("redrawEndPoints()", 50);
}

/**
 * Here the routing request is sent 
 */
function hhRoute() {
  if (route.start.lon === undefined || route.start.lon === "" || route.end.lon === undefined || route.end.lon === "" ) return ;
	route.url = "/HHRoutingWebservice/?format=json&points=" + route.start.lon + "," + route.start.lat + ";" + route.end.lon + "," + route.end.lat;
  route.waitingForResponse = true;
	OpenLayers.Request.GET( {
		url : route.url,
		success : hhRouteResponseHandler,
		scope : this
	});
}

/**
 * Here the routing request response is dealt with
 */
function hhRouteResponseHandler(response) {
  //console.log(response.responseText);
  if (response.responseText != "Error") {
    data = eval('('+response.responseText+')');
    if (data.type != "Error") {
      if(route.line) layerVectors.removeFeatures(route.line);
      geoJSONParser = new OpenLayers.Format.GeoJSON();
      route.line = geoJSONParser.read(response.responseText);
      // We need to manualy convert all points to sperical mercator
      // While iterating over the whole thing, I'll collect the streetnames too
      var directions = "";
      for (i in route.line) {
        for (j in route.line[i].geometry.components) {
          route.line[i].geometry.components[j].transform(projWSG84, projSpheMe); // I actually do want to transform the object itself
        }
        streetName = route.line[i].attributes.Name;
        length = Math.round(route.line[i].attributes.Length/10)*10;
        unit = "m";
        if (length > 1000) {
        	length = Math.round(length/100)/10;
        	unit = "km";
        }
        angle = route.line[i].attributes.Angle;
    	if (angle == -360 || isNaN(angle) || angle == undefined) {
    		arrow = "";
    	} else if (angle > 337 || angle < 22) {
    		arrow = '<div class="Straight"></div>';
    	} else if (angle > 22 && angle < 67) {
    		arrow = '<div class="R45"></div>';
    	} else if (angle > 67 && angle < 112) {
    		arrow = '<div class="R90"></div>';
    	} else if (angle > 112 && angle < 157) {
    		arrow = '<div class="R135"></div>';
    	} else if (angle > 157 && angle < 202) {
    		arrow = '<div class="UTurn"></div>';
    	} else if (angle > 202 && angle < 247 ) {
    		arrow = '<div class="L135"></div>';
    	} else if (angle > 247 && angle < 292 ) {
    		arrow = '<div class="L90"></div>';
    	} else if (angle > 292 && angle < 337 ) {
    		arrow = '<div class="L45"></div>';
    	}
        angle = Math.round(angle);
      	directions += '<tr><td class="direction">' + arrow + '</td><td class="street">' + streetName + '</td><td class="length">' +  length + '&nbsp;' + unit + '</td></tr>';
      }
      document.getElementById("turnTable").innerHTML = directions;
      layerVectors.addFeatures(route.line);
      route.start.point = route.line[0].geometry.components[0];
      route.end.point = route.line[route.line.length-1].geometry.components[route.line[route.line.length-1].geometry.components.length-1];
      if (!route.drag.isdragging) redrawEndPoints();
    }
  }
  route.waitingForResponse = false;
}

var startCircle;
var endCircle;
function redrawEndPoints() {
    layerVectors.removeFeatures(startCircle);
    layerVectors.removeFeatures(endCircle);
    startCircle = new OpenLayers.Feature.Vector(route.start.point);
    startCircle.attributes.mf = "start";
    layerVectors.addFeatures(startCircle);
    endCircle = new OpenLayers.Feature.Vector(route.end.point);
    endCircle.attributes.mf = "end";
    layerVectors.addFeatures(endCircle);
}

// This part takes care of the resizing of the map div, it's not really related to routing or openlayers
var leftDivWidth;

var resizeMapWindow = function resizeMap() {
	windowWidth = window.innerWidth || document.documentElement.clientWidth || document.body.clientWidth;
	windowHeight = window.innerHeight || document.documentElement.clientHeight || document.body.clientHeight;
	document.getElementById("map").style.height = windowHeight - 10; // 10 because body padding
	document.getElementById("leftDiv").style.height = windowHeight - 20; // 10 because body padding
	document.getElementById("turnByTurn").style.height = windowHeight - 20 - document.getElementById('searchForm').offsetHeight; // 10 because body padding
	leftDivWidth = parseInt(document.getElementById("leftDiv").offsetWidth);
	document.getElementById("map").style.width = windowWidth - leftDivWidth;
}

window.onresize = resizeMapWindow;

function isEmpty(obj) {
  for(var prop in obj) {
    if(obj.hasOwnProperty(prop))
      return false;
  }
  return true;
}
