
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