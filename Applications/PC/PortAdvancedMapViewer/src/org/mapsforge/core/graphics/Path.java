package org.mapsforge.core.graphics;
/*
 * Copyright (C) 2006 The Android Open Source Project
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

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;

/**
 * The Path class encapsulates compound (multiple contour) geometric paths
 * consisting of straight line segments, quadratic curves, and cubic curves.
 * It can be drawn with canvas.drawPath(path, paint), either filled or stroked
 * (based on the paint's Style), or it can be used for clipping or to draw
 * text on a path.
 */
public class Path {

    private FillType mFillType = FillType.WINDING;
    private GeneralPath mPath = new GeneralPath();

    private float mLastX = 0;
    private float mLastY = 0;

    //---------- Custom methods ----------

    public Shape getAwtShape() {
        return mPath;
    }

    //----------

    /**
     * Create an empty path
     */
    public Path() {
    }

    /**
     * Create a new path, copying the contents from the src path.
     *
     * @param src The path to copy from when initializing the new path
     */
    public Path(Path src) {
        mPath.append(src.mPath, false /* connect */);
    }

    /**
     * Clear any lines and curves from the path, making it empty.
     * This does NOT change the fill-type setting.
     */
    public void reset() {
        mPath = new GeneralPath();
    }

    /**
     * Rewinds the path: clears any lines and curves from the path but
     * keeps the internal data structure for faster reuse.
     */
    /*Not tested*/
    public void rewind() {
        mPath.reset();
    }

    /** Replace the contents of this with the contents of src.
    */
    public void set(Path src) {
        mPath.append(src.mPath, false /* connect */);
    }

    /** Enum for the ways a path may be filled
    */
    public enum FillType {
        // these must match the values in SkPath.h
        WINDING         (GeneralPath.WIND_NON_ZERO, false),
        EVEN_ODD        (GeneralPath.WIND_EVEN_ODD, false),
        INVERSE_WINDING (GeneralPath.WIND_NON_ZERO, true),
        INVERSE_EVEN_ODD(GeneralPath.WIND_EVEN_ODD, true);

        FillType(int rule, boolean inverse) {
            this.rule = rule;
            this.inverse = inverse;
        }

        final int rule;
        final boolean inverse;
    }

    /**
     * Return the path's fill type. This defines how "inside" is
     * computed. The default value is WINDING.
     *
     * @return the path's fill type
     */
    public FillType getFillType() {
        return mFillType;
    }

    /**
     * Set the path's fill type. This defines how "inside" is computed.
     *
     * @param ft The new fill type for this path
     */
    public void setFillType(FillType ft) {
        mFillType = ft;
        mPath.setWindingRule(ft.rule);
    }

    /**
     * Returns true if the filltype is one of the INVERSE variants
     *
     * @return true if the filltype is one of the INVERSE variants
     */
    public boolean isInverseFillType() {
        return mFillType.inverse;
    }

    /**
     * Toggles the INVERSE state of the filltype
     */
    public void toggleInverseFillType() {
        switch (mFillType) {
            case WINDING:
                mFillType = FillType.INVERSE_WINDING;
                break;
            case EVEN_ODD:
                mFillType = FillType.INVERSE_EVEN_ODD;
                break;
            case INVERSE_WINDING:
                mFillType = FillType.WINDING;
                break;
            case INVERSE_EVEN_ODD:
                mFillType = FillType.EVEN_ODD;
                break;
        }
    }

    /**
     * Returns true if the path is empty (contains no lines or curves)
     *
     * @return true if the path is empty (contains no lines or curves)
     */
    public boolean isEmpty() {
        return mPath.getCurrentPoint() == null;
    }

    /**
     * Set the beginning of the next contour to the point (x,y).
     *
     * @param x The x-coordinate of the start of a new contour
     * @param y The y-coordinate of the start of a new contour
     */
    public void moveTo(float x, float y) {
        mPath.moveTo(mLastX = x, mLastY = y);
    }

    /**
     * Set the beginning of the next contour relative to the last point on the
     * previous contour. If there is no previous contour, this is treated the
     * same as moveTo().
     *
     * @param dx The amount to add to the x-coordinate of the end of the
     *           previous contour, to specify the start of a new contour
     * @param dy The amount to add to the y-coordinate of the end of the
     *           previous contour, to specify the start of a new contour
     */
    public void rMoveTo(float dx, float dy) {
        dx += mLastX;
        dy += mLastY;
        mPath.moveTo(mLastX = dx, mLastY = dy);
    }

    /**
     * Add a line from the last point to the specified point (x,y).
     * If no moveTo() call has been made for this contour, the first point is
     * automatically set to (0,0).
     *
     * @param x The x-coordinate of the end of a line
     * @param y The y-coordinate of the end of a line
     */
    public void lineTo(float x, float y) {
        mPath.lineTo(mLastX = x, mLastY = y);
    }

    /**
     * Same as lineTo, but the coordinates are considered relative to the last
     * point on this contour. If there is no previous point, then a moveTo(0,0)
     * is inserted automatically.
     *
     * @param dx The amount to add to the x-coordinate of the previous point on
     *           this contour, to specify a line
     * @param dy The amount to add to the y-coordinate of the previous point on
     *           this contour, to specify a line
     */
    public void rLineTo(float dx, float dy) {
        if (isEmpty()) {
            mPath.moveTo(mLastX = 0, mLastY = 0);
        }
        dx += mLastX;
        dy += mLastY;
        mPath.lineTo(mLastX = dx, mLastY = dy);
    }

    /**
     * Add a quadratic bezier from the last point, approaching control point
     * (x1,y1), and ending at (x2,y2). If no moveTo() call has been made for
     * this contour, the first point is automatically set to (0,0).
     *
     * @param x1 The x-coordinate of the control point on a quadratic curve
     * @param y1 The y-coordinate of the control point on a quadratic curve
     * @param x2 The x-coordinate of the end point on a quadratic curve
     * @param y2 The y-coordinate of the end point on a quadratic curve
     */
    public void quadTo(float x1, float y1, float x2, float y2) {
        mPath.quadTo(x1, y1, mLastX = x2, mLastY = y2);
    }

    /**
     * Same as quadTo, but the coordinates are considered relative to the last
     * point on this contour. If there is no previous point, then a moveTo(0,0)
     * is inserted automatically.
     *
     * @param dx1 The amount to add to the x-coordinate of the last point on
     *            this contour, for the control point of a quadratic curve
     * @param dy1 The amount to add to the y-coordinate of the last point on
     *            this contour, for the control point of a quadratic curve
     * @param dx2 The amount to add to the x-coordinate of the last point on
     *            this contour, for the end point of a quadratic curve
     * @param dy2 The amount to add to the y-coordinate of the last point on
     *            this contour, for the end point of a quadratic curve
     */
    public void rQuadTo(float dx1, float dy1, float dx2, float dy2) {
        if (isEmpty()) {
            mPath.moveTo(mLastX = 0, mLastY = 0);
        }
        dx1 += mLastX;
        dy1 += mLastY;
        dx2 += mLastX;
        dy2 += mLastY;
        mPath.quadTo(dx1, dy1, mLastX = dx2, mLastY = dy2);
    }

    /**
     * Add a cubic bezier from the last point, approaching control points
     * (x1,y1) and (x2,y2), and ending at (x3,y3). If no moveTo() call has been
     * made for this contour, the first point is automatically set to (0,0).
     *
     * @param x1 The x-coordinate of the 1st control point on a cubic curve
     * @param y1 The y-coordinate of the 1st control point on a cubic curve
     * @param x2 The x-coordinate of the 2nd control point on a cubic curve
     * @param y2 The y-coordinate of the 2nd control point on a cubic curve
     * @param x3 The x-coordinate of the end point on a cubic curve
     * @param y3 The y-coordinate of the end point on a cubic curve
     */
    public void cubicTo(float x1, float y1, float x2, float y2,
                        float x3, float y3) {
        mPath.curveTo(x1, y1, x2, y2, mLastX = x3, mLastY = y3);
    }

    /**
     * Same as cubicTo, but the coordinates are considered relative to the
     * current point on this contour. If there is no previous point, then a
     * moveTo(0,0) is inserted automatically.
     */
    public void rCubicTo(float dx1, float dy1, float dx2, float dy2,
                         float dx3, float dy3) {
        if (isEmpty()) {
            mPath.moveTo(mLastX = 0, mLastY = 0);
        }
        dx1 += mLastX;
        dy1 += mLastY;
        dx2 += mLastX;
        dy2 += mLastY;
        dx3 += mLastX;
        dy3 += mLastY;
        mPath.curveTo(dx1, dy1, dx2, dy2, mLastX = dx3, mLastY = dy3);
    }

    /**
     * Close the current contour. If the current point is not equal to the
     * first point of the contour, a line segment is automatically added.
     */
    public void close() {
        mPath.closePath();
    }

    /**
     * Specifies how closed shapes (e.g. rects, ovals) are oriented when they
     * are added to a path.
     */
    public enum Direction {
        /** clockwise */
        CW  (0),    // must match enum in SkPath.h
        /** counter-clockwise */
        CCW (1);    // must match enum in SkPath.h

        Direction(int ni) {
            nativeInt = ni;
        }
        final int nativeInt;
    }

    /**
     * Add a closed rectangle contour to the path
     *
     * @param left   The left side of a rectangle to add to the path
     * @param top    The top of a rectangle to add to the path
     * @param right  The right side of a rectangle to add to the path
     * @param bottom The bottom of a rectangle to add to the path
     * @param dir    The direction to wind the rectangle's contour
     */
    public void addRect(float left, float top, float right, float bottom,
                        Direction dir) {
        moveTo(left, top);

        switch (dir) {
            case CW:
                lineTo(right, top);
                lineTo(right, bottom);
                lineTo(left, bottom);
                break;
            case CCW:
                lineTo(left, bottom);
                lineTo(right, bottom);
                lineTo(right, top);
                break;
        }

        close();
    }

    /**
     * Add a closed circle contour to the path
     *
     * @param x   The x-coordinate of the center of a circle to add to the path
     * @param y   The y-coordinate of the center of a circle to add to the path
     * @param radius The radius of a circle to add to the path
     * @param dir    The direction to wind the circle's contour
     */
    /* Not Tested */
    public void addCircle(float x, float y, float radius, Direction dir) {
    	mPath.append(new Ellipse2D.Float(x - radius, y - radius, radius * 2, radius * 2), false);
    }
}