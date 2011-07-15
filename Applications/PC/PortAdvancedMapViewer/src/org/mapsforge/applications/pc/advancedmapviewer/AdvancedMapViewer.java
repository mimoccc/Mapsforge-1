/*
 * Copyright 2010, 2011 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.applications.pc.advancedmapviewer;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

import org.mapsforge.pc.maps.ArrayCircleOverlay;
import org.mapsforge.pc.maps.GeoPoint;
import org.mapsforge.pc.maps.MapActivity;
import org.mapsforge.pc.maps.MapController;
import org.mapsforge.pc.maps.MapDatabase;
import org.mapsforge.pc.maps.MapView;
import org.mapsforge.pc.maps.MapViewMode;
import org.mapsforge.pc.maps.OverlayCircle;
import org.mapsforge.pc.maps.MapView.TextField;

/* APP */
import android.app.AlertDialog;
//import org.mapsforge.core.app.AlertDialog;
import android.app.Dialog;
//import org.mapsforge.core.app.Dialog;

/* CONTENT */
import android.content.Context;
//import org.mapsforge.core.content.Context;
import android.content.DialogInterface;
//import org.mapsforge.core.content.DialogInterface;
import android.content.Intent;
//import org.mapsforge.core.content.Intent;
import android.content.SharedPreferences;
//import org.mapsforge.core.content.SharedPreferences;

/* GRAPHICS */
//import android.graphics.Color;
import java.awt.Color;
//import android.graphics.Paint;
import org.mapsforge.core.graphics.Paint;
//import android.graphics.Rect;
import org.mapsforge.applications.android.advancedmapviewer.R;
import org.mapsforge.core.graphics.Rect;
//import android.graphics.Bitmap.CompressFormat;
import org.mapsforge.core.graphics.Bitmap.CompressFormat;
//import android.graphics.drawable.AnimationDrawable;
//import org.mapsforge.core.graphics.drawable.AnimationDrawable;

/* LOCATION */
import android.location.Location;
//import org.mapsforge.core.location.Location;
import android.location.LocationListener;
//import org.mapsforge.core.location.LocationListener;
import android.location.LocationManager;
//import org.mapsforge.core.location.LocationManager;
import android.location.LocationProvider;
//import org.mapsforge.core.location.LocationProvider;

/* OS */
import android.os.Bundle;
//import org.mapsforge.core.os.Bundle;
import android.os.Environment;
//import org.mapsforge.core.os.Environment;
//import android.os.PowerManager;
//import android.os.PowerManager.WakeLock;

/* PREFERENCES */
import android.preference.PreferenceManager;

/* VIEW */
import android.view.LayoutInflater;
//import org.mapsforge.core.view.LayoutInflater;
import android.view.Menu;
//import org.mapsforge.core.view.Menu;
import android.view.MenuItem;
//import org.mapsforge.core.view.MenuItem;
import android.view.MotionEvent;
//import org.mapsforge.core.view.MotionEvent;
import android.view.View;
//import org.mapsforge.core.view.View;
import android.view.WindowManager;
//import org.mapsforge.core.view.WindowManager;
import android.view.View.OnClickListener;
//import org.mapsforge.core.view.OnClickListener;

/* WIDGET */
import android.widget.EditText;
//import org.mapsforge.core.widget.EditText;
import android.widget.ImageView;
//import org.mapsforge.core.widget.ImageView;
import android.widget.SeekBar;
//import org.mapsforge.core.widget.SeekBar;
import android.widget.TextView;
//import org.mapsforge.core.widget.TextView;
import android.widget.Toast;
//import org.mapsforge.core.widget.Toast;

/**
 * A map application which uses the features from the mapsforge library. The map can be centered
 * to the current GPS coordinate. A simple file browser for selecting the map file is also
 * included. Some preferences can be adjusted via the EditPreferences activity and screenshots
 * of the map may be taken in different image formats.
 */
public class AdvancedMapViewer extends MapActivity {
	
}