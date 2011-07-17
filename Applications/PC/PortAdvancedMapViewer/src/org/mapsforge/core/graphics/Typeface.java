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
import java.util.List;


/**
 * Re-implementation of Typeface over java.awt
 */
public class Typeface extends Font {

	private static final long serialVersionUID = -6330183324992171615L;

	
	public Typeface(String name, int style, int size) {
		super(name, style, size);
	}

    /** The default NORMAL typeface object */
    public static Typeface DEFAULT = new Typeface(null, Font.PLAIN, 8);
    
    /**
     * The default BOLD typeface object. Note: this may be not actually be
     * bold, depending on what fonts are installed. Call getStyle() to know
     * for sure.
     */
    public static final Typeface DEFAULT_BOLD = new Typeface(null, Font.BOLD, 8);
    /** The NORMAL style of the default sans serif typeface. */
    public static final Typeface SANS_SERIF  = new Typeface(Font.SANS_SERIF, Font.PLAIN, 8);
    /** The NORMAL style of the default serif typeface. */
    public static final Typeface SERIF = new Typeface(Font.SERIF, Font.PLAIN, 8);
    /** The NORMAL style of the default monospace typeface. */
    public static final Typeface MONOSPACE = new Typeface(Font.MONOSPACED, Font.PLAIN, 8);

    private List<Font> mFonts;	//final

    /**
     * Returns the underlying {@link Font} objects. The first item in the list is the real
     * font. Any other items are fallback fonts for characters not found in the first one.
     */
    public List<Font> getFonts() {
        return mFonts;
    }

	public String getName() {
		return super.getName();
	}

	public void setName(String name) {
		super.name = name;
	}

	public int getStyle() {
		return super.getStyle();
	}

	public void setStyle(int style) {
		super.style = style;
	}

	public int getSize() {
		return super.getSize();
	}

	public void setSize(int size) {
		super.size = size;
	}

}