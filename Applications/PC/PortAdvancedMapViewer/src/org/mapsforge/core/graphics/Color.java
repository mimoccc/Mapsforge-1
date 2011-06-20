package org.mapsforge.core.graphics;

import java.util.HashMap;

import org.mapsforge.core.MathUtils;

public class Color extends java.awt.Color {

	private static final long serialVersionUID = 4489419131193097797L;

	
	public Color(int arg0) {
		super(arg0);
	}

	//TODO unsafe
	public Color(Color color) {
		super(color.getAlpha());
	}

	public static final Color BLACK       = (Color) java.awt.Color.BLACK;
    public static final Color DKGRAY      = (Color) java.awt.Color.DARK_GRAY;
    public static final Color GRAY        = (Color) java.awt.Color.GRAY;
    public static final Color LTGRAY      = (Color) java.awt.Color.LIGHT_GRAY;
    public static final Color WHITE       = (Color) java.awt.Color.WHITE;
    public static final Color RED         = (Color) java.awt.Color.RED;
    public static final Color GREEN       = (Color) java.awt.Color.GREEN;
    public static final Color BLUE        = (Color) java.awt.Color.BLUE;
    public static final Color YELLOW      = (Color) java.awt.Color.YELLOW;
    public static final Color CYAN        = (Color) java.awt.Color.CYAN;
    public static final Color MAGENTA     = (Color) java.awt.Color.MAGENTA;
    public static final int TRANSPARENT   = java.awt.Color.TRANSLUCENT;

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
    public static Color rgb(int red, int green, int blue) {
        return (Color) new java.awt.Color(red, green, blue);
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
    public static Color argb(int alpha, int red, int green, int blue) {
        return (Color) new java.awt.Color(red, green, blue, alpha);
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

    private static final HashMap<String, Color> sColorNameMap;

    static {
        sColorNameMap = new HashMap<String, Color>();
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
