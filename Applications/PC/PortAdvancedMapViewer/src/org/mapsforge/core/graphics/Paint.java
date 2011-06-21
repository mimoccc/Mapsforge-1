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

/*import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.SpannedString;
import android.text.TextUtils;*/

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//import android.graphics.Shader;
import org.mapsforge.core.graphics.Shader;
/**
 * A paint implementation overridden by the LayoutLib bridge.
 */
public class Paint {

    /**
     * Class associating a {@link Font} and it's {@link java.awt.FontMetrics}.
     */
    public static final class FontInfo {
        Font mFont;
        java.awt.FontMetrics mMetrics;
    }

    public static final Integer ANTI_ALIAS_FLAG       = 0x01;
    public static final Integer FILTER_BITMAP_FLAG    = 0x02; //TODO
    public static final Integer DITHER_FLAG   		  = 0x03;
    public static final Integer DEV_KERN_TEXT_FLAG    = 0x04;
    
    private static final int DEFAULT_PAINT_FLAGS = DEV_KERN_TEXT_FLAG;

    
    /**
     * Class that describes the various metrics for a font at a given text size.
     * Remember, Y values increase going down, so those values will be positive,
     * and values that measure distances going up will be negative. This class
     * is returned by getFontMetrics().
     */
    public static class FontMetrics {
        /**
         * The maximum distance above the baseline for the tallest glyph in
         * the font at a given text size.
         */
        public float   top;
        /**
         * The recommended distance above the baseline for singled spaced text.
         */
        public float   ascent;
        /**
         * The recommended distance below the baseline for singled spaced text.
         */
        public float   descent;
        /**
         * The maximum distance below the baseline for the lowest glyph in
         * the font at a given text size.
         */
        public float   bottom;
        /**
         * The recommended additional space to add between lines of text.
         */
        public float   leading;
    }
    
    /**
     * Convenience method for callers that want to have FontMetrics values as
     * integers.
     */
    public static class FontMetricsInt {
        public int   top;
        public int   ascent;
        public int   descent;
        public int   bottom;
        public int   leading;

        // Override
        public String toString() {
            return "FontMetricsInt: top=" + top + " ascent=" + ascent + " descent=" + descent + " bottom=" + bottom +
                    " leading=" + leading;
        }
    }
    
    /**
     * The Style specifies if the primitive being drawn is filled,
     * stroked, or both (in the same color). The default is FILL.
     */
    public enum Style {
        /**
         * Geometry and text drawn with this style will be filled, ignoring all
         * stroke-related settings in the paint.
         */
        FILL            (0),
        /**
         * Geometry and text drawn with this style will be stroked, respecting
         * the stroke-related fields on the paint.
         */
        STROKE          (1),
        /**
         * Geometry and text drawn with this style will be both filled and
         * stroked at the same time, respecting the stroke-related fields on
         * the paint.
         */
        FILL_AND_STROKE (2);

        Style(int nativeInt) {
            this.nativeInt = nativeInt;
        }
        final int nativeInt;
    }

    /**
     * The Cap specifies the treatment for the beginning and ending of
     * stroked lines and paths. The default is BUTT.
     */
    public enum Cap {
        /**
         * The stroke ends with the path, and does not project beyond it.
         */
        BUTT    (0),
        /**
         * The stroke projects out as a square, with the center at the end
         * of the path.
         */
        ROUND   (1),
        /**
         * The stroke projects out as a semicircle, with the center at the
         * end of the path.
         */
        SQUARE  (2);

        private Cap(int nativeInt) {
            this.nativeInt = nativeInt;
        }
        final int nativeInt;

        /** custom for layoutlib */
        public int getJavaCap() {
            switch (this) {
                case BUTT:
                    return BasicStroke.CAP_BUTT;
                case ROUND:
                    return BasicStroke.CAP_ROUND;
                default:
                case SQUARE:
                    return BasicStroke.CAP_SQUARE;
            }
        }
    }

    /**
     * The Join specifies the treatment where lines and curve segments
     * join on a stroked path. The default is MITER.
     */
    public enum Join {
        /**
         * The outer edges of a join meet at a sharp angle
         */
        MITER   (0),
        /**
         * The outer edges of a join meet in a circular arc.
         */
        ROUND   (1),
        /**
         * The outer edges of a join meet with a straight line
         */
        BEVEL   (2);

        private Join(int nativeInt) {
            this.nativeInt = nativeInt;
        }
        final int nativeInt;

        /** custom for layoutlib */
        public int getJavaJoin() {
            switch (this) {
                default:
                case MITER:
                    return BasicStroke.JOIN_MITER;
                case ROUND:
                    return BasicStroke.JOIN_ROUND;
                case BEVEL:
                    return BasicStroke.JOIN_BEVEL;
            }
        }
    }

    /**
     * Align specifies how drawText aligns its text relative to the
     * [x,y] coordinates. The default is LEFT.
     */
    public enum Align {
        /**
         * The text is drawn to the right of the x,y origin
         */
        LEFT    (0),
        /**
         * The text is drawn centered horizontally on the x,y origin
         */
        CENTER  (1),
        /**
         * The text is drawn to the left of the x,y origin
         */
        RIGHT   (2);

        private Align(int nativeInt) {
            this.nativeInt = nativeInt;
        }
        final int nativeInt;
    }

    /* GLOBAL VARIABLE */
    private Color color = null;
    private PathEffect pathEffect;
    private Shader strokeShader;
    private Cap strokeCap = Cap.BUTT;
    private Join strokeJoin = Join.MITER;
    private float strokeMiter = 4.0f;
    private float strokeWidth = 1.f;
    private Style style = Style.FILL;
    private Align align = Align.LEFT;
    private float mScaleX = 1;
    private float mSkewX = 0;
    private int mFlags = 0;
    private float textSize;
    private Typeface typeface;
    
    private List<FontInfo> mFonts;
    private final FontRenderContext mFontContext = new FontRenderContext(new AffineTransform(), true, true);
    
    /* CONSTRUCTOR */
    public Paint(int flag) {
    	setFlags(flag | DEFAULT_PAINT_FLAGS);
        initFont();
    }

	private void initFont() {
		typeface = Typeface.DEFAULT;
		updateFontObject();		
	}

	public Paint(Paint src) {
		if (this != src) {
            color = src.color;
            textSize = src.textSize;
            mScaleX = src.mScaleX;
            mSkewX = src.mSkewX;
            align = src.align;
            style = src.style;
            strokeShader = src.strokeShader;
            pathEffect = src.pathEffect;
            
            updateFontObject();

            //super.set(src);
        }
	}
	
	/* GETTER AND SETTER */


	public void setColor(java.awt.Color color) {
		this.color = color;
	}
    
	public Color getColor() {
		return color;
	}
	
	public void setFlags(int flags) {
        mFlags = flags;
    }
	
	private int getFlags() {
        return mFlags;
	}
	
	public List<FontInfo> getFonts() {
		return mFonts;
	}
	
	public void setPathEffect(PathEffect pathEffect) {
		this.pathEffect = pathEffect;
	}
	
	public PathEffect getPathEffect() {
		return this.pathEffect;
	}
	
	public void setShader(Shader shader) {
		this.strokeShader = shader;
	}
	
	public Shader getShader() {
		return this.strokeShader;
	}

	public void setStrokeCap(Cap cap) {
		this.strokeCap = cap;
	}
	
	public Cap getStrokeCap() {
		return this.strokeCap;
	}
	
	public void setStrokeJoin(Join join) {
		this.strokeJoin = join;
	}
	public Join getStrokeJoin() {
		return this.strokeJoin;
	}
	
	public void setStrokeMiter(float miter) {
        this.strokeMiter = miter;
    }
	
	public float getStrokeMiter() {
		return this.strokeMiter;
	}
	
	public void setStrokeWidth(float f) {
		this.strokeWidth = f;	
	}

	public float getStrokeWidth() {
		return this.strokeWidth;
	}
	
	public void setStyle(Style style) {
		this.style = style;
	}
	
	public Style getStyle() {
		return this.style;
	}
	
	public void setTextAlign(Align align) {
		this.align = align;
	}
	
	public Align getTextAlign() {
		return this.align;
	}
	
	public void getTextBounds(String text, int start, int end, Rect boundary) {
		 if ((start | end | (end - start) | (text.length() - end)) < 0) {
	            throw new IndexOutOfBoundsException();
	        }
	        if (boundary == null) {
	            throw new NullPointerException("need bounds Rect");
	        }

	        getTextBounds(text.toCharArray(), start, end - start, boundary);
	}
	
	//TODO Test
	public void getTextBounds(char[] text, int index, int count, Rect bounds) {
        if (mFonts.size() > 0) {
            if ((index | count) < 0 || index + count > text.length) {
                throw new ArrayIndexOutOfBoundsException();
            }
            if (bounds == null) {
                throw new NullPointerException("need bounds Rect");
            }

            FontInfo mainInfo = mFonts.get(0);

            Rectangle2D rect = mainInfo.mFont.getStringBounds(text, index, index + count, mFontContext);
            bounds.set(0, 0, (int)rect.getWidth(), (int)rect.getHeight());
        }
    }
	
	public float setTextSize(float f) {
		this.textSize = f;
		return this.textSize;
	}
	
	public Typeface setTypeface(Typeface typeface) {
		if (typeface != null) {
            this.typeface = typeface;
        } else {
        	this.typeface = Typeface.DEFAULT;
        }

        updateFontObject();

        return typeface;	
	}
	

	/* OTHER */

	@SuppressWarnings("deprecation")
	private void updateFontObject() {
		if (typeface != null) {
            // Get the fonts from the TypeFace object.
            List<Font> fonts = typeface.getFonts();

            // create new font objects as well as FontMetrics, based on the current text size
            // and skew info.
            ArrayList<FontInfo> infoList = new ArrayList<FontInfo>(fonts.size());
            for (Font font : fonts) {
                FontInfo info = new FontInfo();
                info.mFont = font.deriveFont(textSize);
                if (mScaleX != 1.0 || mSkewX != 0) {
                    // TODO: support skew TEST
                    info.mFont = info.mFont.deriveFont(new AffineTransform(
                            mScaleX, mSkewX, 0, 0, 1, 0));
                }
                info.mMetrics = Toolkit.getDefaultToolkit().getFontMetrics(info.mFont);

                infoList.add(info);
            }

            mFonts = Collections.unmodifiableList(infoList);
        }
	}

	public float measureText(String text) {
        return measureText(text.toCharArray(), 0, text.length());
    }
	
	public float measureText(char[] text, int index, int count) {
        // WARNING: the logic in this method is similar to Canvas.drawText.
        // Any change to this method should be reflected in Canvas.drawText
        if (mFonts.size() > 0) {
            FontInfo mainFont = mFonts.get(0);
            int i = index;
            int lastIndex = index + count;
            float total = 0f;
            while (i < lastIndex) {
                // always start with the main font.
                int upTo = mainFont.mFont.canDisplayUpTo(text, i, lastIndex);
                if (upTo == -1) {
                    // shortcut to exit
                    return total + mainFont.mMetrics.charsWidth(text, i, lastIndex - i);
                } else if (upTo > 0) {
                    total += mainFont.mMetrics.charsWidth(text, i, upTo - i);
                    i = upTo;
                    // don't call continue at this point. Since it is certain the main font
                    // cannot display the font a index upTo (now ==i), we move on to the
                    // fallback fonts directly.
                }

                // no char supported, attempt to read the next char(s) with the
                // fallback font. In this case we only test the first character
                // and then go back to test with the main font.
                // Special test for 2-char characters.
                boolean foundFont = false;
                for (int f = 1 ; f < mFonts.size() ; f++) {
                    FontInfo fontInfo = mFonts.get(f);

                    // need to check that the font can display the character. We test
                    // differently if the char is a high surrogate.
                    int charCount = Character.isHighSurrogate(text[i]) ? 2 : 1;
                    upTo = fontInfo.mFont.canDisplayUpTo(text, i, i + charCount);
                    if (upTo == -1) {
                        total += fontInfo.mMetrics.charsWidth(text, i, charCount);
                        i += charCount;
                        foundFont = true;
                        break;

                    }
                }

                // in case no font can display the char, measure it with the main font.
                if (foundFont == false) {
                    int size = Character.isHighSurrogate(text[i]) ? 2 : 1;
                    total += mainFont.mMetrics.charsWidth(text, i, size);
                    i += size;
                }
            }
        }

        return 0;
    }

	public boolean isFilterBitmap() {		
		return (getFlags() & FILTER_BITMAP_FLAG) != 0;
	}

	public int getAlpha() {
		return color.getAlpha();
	}

	public void setAlpha(int i) {
		color = new Color(color.getRed(), color.getGreen(), color.getBlue(), i);		
	}

}