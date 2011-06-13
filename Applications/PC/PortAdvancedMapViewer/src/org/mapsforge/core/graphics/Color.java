package org.mapsforge.core.graphics;

import java.util.HashMap;

import org.mapsforge.core.MathUtils;

public class Color {

	public static final java.awt.Color BLACK       = java.awt.Color.BLACK;
    public static final java.awt.Color DKGRAY      = java.awt.Color.DARK_GRAY;
    public static final java.awt.Color GRAY        = java.awt.Color.GRAY;
    public static final java.awt.Color LTGRAY      = java.awt.Color.LIGHT_GRAY;
    public static final java.awt.Color WHITE       = java.awt.Color.WHITE;
    public static final java.awt.Color RED         = java.awt.Color.RED;
    public static final java.awt.Color GREEN       = java.awt.Color.GREEN;
    public static final java.awt.Color BLUE        = java.awt.Color.BLUE;
    public static final java.awt.Color YELLOW      = java.awt.Color.YELLOW;
    public static final java.awt.Color CYAN        = java.awt.Color.CYAN;
    public static final java.awt.Color MAGENTA     = java.awt.Color.MAGENTA;
    //public static final java.awt.Color TRANSPARENT = java.awt.Color;
	
    /**
     * Return the alpha component of a color int. This is the same as saying
     * color >>> 24
     */
    public static int alpha(int color) {
        return color >>> 24;
    }

    /**
     * Return the red component of a color int. This is the same as saying
     * (color >> 16) & 0xFF
     */
    public static int red(int color) {
        return (color >> 16) & 0xFF;
    }

    /**
     * Return the green component of a color int. This is the same as saying
     * (color >> 8) & 0xFF
     */
    public static int green(int color) {
        return (color >> 8) & 0xFF;
    }

    /**
     * Return the blue component of a color int. This is the same as saying
     * color & 0xFF
     */
    public static int blue(int color) {
        return color & 0xFF;
    }

    /**
     * Return a color-int from red, green, blue components.
     * The alpha component is implicity 255 (fully opaque).
     * These component values should be [0..255], but there is no
     * range check performed, so if they are out of range, the
     * returned color is undefined.
     * @param red  Red component [0..255] of the color
     * @param green Green component [0..255] of the color
     * @param blue  Blue component [0..255] of the color
     */
    public static int rgb(int red, int green, int blue) {
        return (0xFF << 24) | (red << 16) | (green << 8) | blue;
    }

    /**
     * Return a color-int from alpha, red, green, blue components.
     * These component values should be [0..255], but there is no
     * range check performed, so if they are out of range, the
     * returned color is undefined.
     * @param alpha Alpha component [0..255] of the color
     * @param red   Red component [0..255] of the color
     * @param green Green component [0..255] of the color
     * @param blue  Blue component [0..255] of the color
     */
    public static int argb(int alpha, int red, int green, int blue) {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    /**
     * Returns the hue component of a color int.
     *
     * @return A value between 0.0f and 1.0f
     *
     * @hide Pending API council
     */
    public static float hue(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        int V = Math.max(b, Math.max(r, g));
        int temp = Math.min(b, Math.min(r, g));

        float H;

        if (V == temp) {
            H = 0;
        } else {
            final float vtemp = (float) (V - temp);
            final float cr = (V - r) / vtemp;
            final float cg = (V - g) / vtemp;
            final float cb = (V - b) / vtemp;

            if (r == V) {
                H = cb - cg;
            } else if (g == V) {
                H = 2 + cr - cb;
            } else {
                H = 4 + cg - cr;
            }

            H /= 6.f;
            if (H < 0) {
                H++;
            }
        }

        return H;
    }

    /**
     * Returns the saturation component of a color int.
     *
     * @return A value between 0.0f and 1.0f
     *
     * @hide Pending API council
     */
    public static float saturation(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;


        int V = Math.max(b, Math.max(r, g));
        int temp = Math.min(b, Math.min(r, g));

        float S;

        if (V == temp) {
            S = 0;
        } else {
            S = (V - temp) / (float) V;
        }

        return S;
    }

    /**
     * Returns the brightness component of a color int.
     *
     * @return A value between 0.0f and 1.0f
     *
     * @hide Pending API council
     */
    public static float brightness(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        int V = Math.max(b, Math.max(r, g));

        return (V / 255.f);
    }

    /**
     * GESPERRT
     * Parse the color string, and return the corresponding color-int.
     * If the string cannot be parsed, throws an IllegalArgumentException
     * exception. Supported formats are:
     * #RRGGBB
     * #AARRGGBB
     * 'red', 'blue', 'green', 'black', 'white', 'gray', 'cyan', 'magenta',
     * 'yellow', 'lightgray', 'darkgray'
     */
    /*
     public static int parseColor(String colorString) {
        if (colorString.charAt(0) == '#') {
            // Use a long to avoid rollovers on #ffXXXXXX
            long color = Long.parseLong(colorString.substring(1), 16);
            if (colorString.length() == 7) {
                // Set the alpha value
                color |= 0x00000000ff000000;
            } else if (colorString.length() != 9) {
                throw new IllegalArgumentException("Unknown color");
            }
            return (int)color;
        } else {
            Integer color = sColorNameMap.get(colorString.toLowerCase(Locale.US));
            if (color != null) {
                return color;
            }
        }
        throw new IllegalArgumentException("Unknown color");
    }
     */

    /**
     * Convert HSB components to an ARGB color. Alpha set to 0xFF.
     *     hsv[0] is Hue [0 .. 1)
     *     hsv[1] is Saturation [0...1]
     *     hsv[2] is Value [0...1]
     * If hsv values are out of range, they are pinned.
     * @param hsb  3 element array which holds the input HSB components.
     * @return the resulting argb color
     *
     * @hide Pending API council
     */
    public static int HSBtoColor(float[] hsb) {
        return HSBtoColor(hsb[0], hsb[1], hsb[2]);
    }

    /**
     * Convert HSB components to an ARGB color. Alpha set to 0xFF.
     *     hsv[0] is Hue [0 .. 1)
     *     hsv[1] is Saturation [0...1]
     *     hsv[2] is Value [0...1]
     * If hsv values are out of range, they are pinned.
     * @param h Hue component
     * @param s Saturation component
     * @param b Brightness component
     * @return the resulting argb color
     *
     * @hide Pending API council
     */
    public static int HSBtoColor(float h, float s, float b) {
        h = MathUtils.constrain(h, 0.0f, 1.0f);
        s = MathUtils.constrain(s, 0.0f, 1.0f);
        b = MathUtils.constrain(b, 0.0f, 1.0f);

        float red = 0.0f;
        float green = 0.0f;
        float blue = 0.0f;

        final float hf = (h - (int) h) * 6.0f;
        final int ihf = (int) hf;
        final float f = hf - ihf;
        final float pv = b * (1.0f - s);
        final float qv = b * (1.0f - s * f);
        final float tv = b * (1.0f - s * (1.0f - f));

        switch (ihf) {
            case 0:         // Red is the dominant color
                red = b;
                green = tv;
                blue = pv;
                break;
            case 1:         // Green is the dominant color
                red = qv;
                green = b;
                blue = pv;
                break;
            case 2:
                red = pv;
                green = b;
                blue = tv;
                break;
            case 3:         // Blue is the dominant color
                red = pv;
                green = qv;
                blue = b;
                break;
            case 4:
                red = tv;
                green = pv;
                blue = b;
                break;
            case 5:         // Red is the dominant color
                red = b;
                green = pv;
                blue = qv;
                break;
        }

        return 0xFF000000 | (((int) (red * 255.0f)) << 16) |
                (((int) (green * 255.0f)) << 8) | ((int) (blue * 255.0f));
    }

    /**
     * Convert RGB components to HSV.
     *     hsv[0] is Hue [0 .. 360)
     *     hsv[1] is Saturation [0...1]
     *     hsv[2] is Value [0...1]
     * @param red  red component value [0..255]
     * @param green  green component value [0..255]
     * @param blue  blue component value [0..255]
     * @param hsv  3 element array which holds the resulting HSV components.
     */
    public static void RGBToHSV(int red, int green, int blue, float hsv[]) {
        if (hsv.length < 3) {
            throw new RuntimeException("3 components required for hsv");
        }
        nativeRGBToHSV(red, green, blue, hsv);
    }

    /**
     * Convert the argb color to its HSV components.
     *     hsv[0] is Hue [0 .. 360)
     *     hsv[1] is Saturation [0...1]
     *     hsv[2] is Value [0...1]
     * @param color the argb color to convert. The alpha component is ignored.
     * @param hsv  3 element array which holds the resulting HSV components.
     */
    public static void colorToHSV(int color, float hsv[]) {
        RGBToHSV((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, hsv);
    }

    /**
     * Convert HSV components to an ARGB color. Alpha set to 0xFF.
     *     hsv[0] is Hue [0 .. 360)
     *     hsv[1] is Saturation [0...1]
     *     hsv[2] is Value [0...1]
     * If hsv values are out of range, they are pinned.
     * @param hsv  3 element array which holds the input HSV components.
     * @return the resulting argb color
    */
    public static int HSVToColor(float hsv[]) {
        return HSVToColor(0xFF, hsv);
    }

    /**
     * Convert HSV components to an ARGB color. The alpha component is passed
     * through unchanged.
     *     hsv[0] is Hue [0 .. 360)
     *     hsv[1] is Saturation [0...1]
     *     hsv[2] is Value [0...1]
     * If hsv values are out of range, they are pinned.
     * @param alpha the alpha component of the returned argb color.
     * @param hsv  3 element array which holds the input HSV components.
     * @return the resulting argb color
    */
    public static int HSVToColor(int alpha, float hsv[]) {
        if (hsv.length < 3) {
            throw new RuntimeException("3 components required for hsv");
        }
        return nativeHSVToColor(alpha, hsv);
    }

    private static native void nativeRGBToHSV(int red, int greed, int blue, float hsv[]);
    private static native int nativeHSVToColor(int alpha, float hsv[]);

    private static final HashMap<String, java.awt.Color> sColorNameMap;

    static {
        sColorNameMap = new HashMap<String, java.awt.Color>();
        sColorNameMap.put("black", BLACK);
        sColorNameMap.put("darkgray", DKGRAY);
        sColorNameMap.put("gray", GRAY);
        sColorNameMap.put("lightgray", LTGRAY);
        sColorNameMap.put("white", WHITE);
        sColorNameMap.put("red", RED);
        sColorNameMap.put("green", GREEN);
        sColorNameMap.put("blue", BLUE);
        sColorNameMap.put("yellow", YELLOW);
        sColorNameMap.put("cyan", CYAN);
        sColorNameMap.put("magenta", MAGENTA);
    }
    
}
