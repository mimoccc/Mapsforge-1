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
package org.mapsforge.applications.android.demomapviewer;

import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.MapViewMode;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * A simple application which demonstrates how to use two MapViews at the same time.
 */
public class DualMapViewer extends MapActivity {
	private MapView mapView1;
	private MapView mapView2;

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			finish();
			return true;
		}
		// forward the event to both MapViews for simultaneous movement
		return (this.mapView1.onKeyDown(keyCode, event) | this.mapView2.onKeyDown(keyCode,
				event));
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		// forward the event to both MapViews for simultaneous movement
		return (this.mapView1.onKeyUp(keyCode, event) | this.mapView2.onKeyUp(keyCode, event));
	}

	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		// forward the event to both MapViews for simultaneous movement
		return (this.mapView1.onTrackballEvent(event) | this.mapView2.onTrackballEvent(event));
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.mapView1 = new MapView(this, MapViewMode.CANVAS_RENDERER);
		this.mapView1.setClickable(true);
		this.mapView1.setBuiltInZoomControls(true);
		this.mapView1.setMapFile("/sdcard/germany_30_09.map");
		this.mapView1.setMoveSpeed(3);

		this.mapView2 = new MapView(this, MapViewMode.CANVAS_RENDERER);
		this.mapView2.setClickable(true);
		this.mapView2.setBuiltInZoomControls(true);
		this.mapView2.setMapFile("/sdcard/berlin_z14_17_09.map");
		this.mapView2.setMoveSpeed(3);

		// create a LineaLayout that holds both MapViews
		LinearLayout linearLayout = new LinearLayout(this);

		// if the device orientation is portrait, change the orientation to vertical
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			linearLayout.setOrientation(LinearLayout.VERTICAL);
		}

		this.mapView1.setLayoutParams(new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
		this.mapView2.setLayoutParams(new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

		// add both MapViews to the LinearLayout
		linearLayout.addView(this.mapView1);
		linearLayout.addView(this.mapView2);
		setContentView(linearLayout);
	}
}