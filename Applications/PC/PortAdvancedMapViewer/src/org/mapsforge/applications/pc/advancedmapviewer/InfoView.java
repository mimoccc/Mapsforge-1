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

import android.app.Activity;
//import org.mapsforge.core.app.Activity;
//import android.os.Bundle;
//import org.mapsforge.core.os.Bundle;
import android.preference.PreferenceManager;
//import org.mapsforge.core.preference.PreferenceManager;
import android.view.WindowManager;
//import org.mapsforge.core.view.WindowManager;
//import android.webkit.WebView;
//import org.mapsforge.core.webkit.WebView;

/**
 * Simple activity to display the info web page from the assets folder.
 */
public class InfoView extends Activity {
	//TODO @Override
	//protected void onCreate(Bundle savedInstanceState) {
		//super.onCreate(savedInstanceState);
		//WebView webView = new WebView(this);
		//webView.loadUrl("file:///android_asset/info.xml");
		//setContentView(webView);
	//}

	@Override
	protected void onResume() {
		super.onResume();
		// check if the full screen mode should be activated
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("fullscreen", false)) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		} else {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		}
	}
}