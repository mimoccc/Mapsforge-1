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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.mapsforge.core.FontLoader;

/**
 * Re-implementation of Typeface over java.awt
 */
public class Typeface {
    private static final String DEFAULT_FAMILY = "sans-serif";
    private static final int[] styleBuffer = new int[1];

    /** The default NORMAL typeface object */
    public static Typeface DEFAULT;
    /**
     * The default BOLD typeface object. Note: this may be not actually be
     * bold, depending on what fonts are installed. Call getStyle() to know
     * for sure.
     */
    public static Typeface DEFAULT_BOLD;
    /** The NORMAL style of the default sans serif typeface. */
    public static Typeface SANS_SERIF;
    /** The NORMAL style of the default serif typeface. */
    public static Typeface SERIF;
    /** The NORMAL style of the default monospace typeface. */
    public static Typeface MONOSPACE;

    private static Typeface[] sDefaults;
    //private static FontLoader mFontLoader;

    private final int mStyle;
    private final List<Font> mFonts;
    private final String mFamily;
    private static FontLoader mFontLoader;

    // Style
    public static final int NORMAL = 0;
    public static final int BOLD = 1;
    public static final int ITALIC = 2;
    public static final int BOLD_ITALIC = 3;

    /**
     * Returns the underlying {@link Font} objects. The first item in the list is the real
     * font. Any other items are fallback fonts for characters not found in the first one.
     */
    public List<Font> getFonts() {
        return mFonts;
    }

    /** Returns the typeface's intrinsic style attributes */
    public int getStyle() {
        return mStyle;
    }

    /** Returns true if getStyle() has the BOLD bit set. */
    public final boolean isBold() {
        return (getStyle() & BOLD) != 0;
    }

    /** Returns true if getStyle() has the ITALIC bit set. */
    public final boolean isItalic() {
        return (getStyle() & ITALIC) != 0;
    }

    /**
     * Create a typeface object given a family name, and option style information.
     * If null is passed for the name, then the "default" font will be chosen.
     * The resulting typeface object can be queried (getStyle()) to discover what
     * its "real" style characteristics are.
     *
     * @param familyName May be null. The name of the font family.
     * @param style  The style (normal, bold, italic) of the typeface.
     *               e.g. NORMAL, BOLD, ITALIC, BOLD_ITALIC
     * @return The best matching typeface.
     */
    public static Typeface create(String familyName, int style) {
        styleBuffer[0] = style;
        Font font = mFontLoader.getFont(familyName, styleBuffer);
        if (font != null) {
            ArrayList<Font> list = new ArrayList<Font>();
            list.add(font);
            list.addAll(mFontLoader.getFallBackFonts());
            return new Typeface(familyName, styleBuffer[0], list);
        }

        return null;
    }

    /**
     * Create a typeface object that best matches the specified existing
     * typeface and the specified Style. Use this call if you want to pick a new
     * style from the same family of an existing typeface object. If family is
     * null, this selects from the default font's family.
     *
     * @param family May be null. The name of the existing type face.
     * @param style  The style (normal, bold, italic) of the typeface.
     *               e.g. NORMAL, BOLD, ITALIC, BOLD_ITALIC
     * @return The best matching typeface.
     */
    public static Typeface create(Typeface family, int style) {
        styleBuffer[0] = style;
        Font font = mFontLoader.getFont(family.mFamily, styleBuffer);
        if (font != null) {
            ArrayList<Font> list = new ArrayList<Font>();
            list.add(font);
            list.addAll(mFontLoader.getFallBackFonts());
            return new Typeface(family.mFamily, styleBuffer[0], list);
        }

        return null;
    }

    /**
     * Returns one of the default typeface objects, based on the specified style
     *
     * @return the default typeface that corresponds to the style
     */
    public static Typeface defaultFromStyle(int style) {
        return sDefaults[style];
    }


    // don't allow clients to call this directly
    private Typeface(String family, int style, List<Font> fonts) {
        mFamily = family;
        mFonts = Collections.unmodifiableList(fonts);
        mStyle = style;
    }

    public static void init(FontLoader fontLoader) {
        mFontLoader = fontLoader;

        DEFAULT = create(DEFAULT_FAMILY, NORMAL);
        DEFAULT_BOLD = create(DEFAULT_FAMILY, BOLD);
        SANS_SERIF = create("sans-serif", NORMAL);
        SERIF = create("serif", NORMAL);
        MONOSPACE = create("monospace", NORMAL);
        sDefaults = new Typeface[] {
                DEFAULT,
                DEFAULT_BOLD,
                create(DEFAULT_FAMILY, ITALIC),
                create(DEFAULT_FAMILY, BOLD_ITALIC),
        };
    }
}