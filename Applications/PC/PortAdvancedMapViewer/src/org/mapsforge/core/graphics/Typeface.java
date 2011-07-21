package org.mapsforge.core.graphics;
/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//import android.content.res.AssetManager;

import java.awt.Font;

/**
 * Re-implementation of Typeface over java.awt
 */
public class Typeface extends Font {

	private static final long serialVersionUID = -6330183324992171615L;
	
	public Typeface(String name, int style, int size) {
		super(name, style, size);
	}

    /** The default NORMAL typeface object */
    public static Typeface DEFAULT = new Typeface(null, Font.PLAIN, 12);    
}