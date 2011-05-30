
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
package myandroid.text.style;

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

import myandroid.graphics.Paint;
import myandroid.graphics.Typeface;
import myandroid.os.Parcel;
import myandroid.text.ParcelableSpan;
import myandroid.text.TextPaint;
import myandroid.text.TextUtils;

/**
 *
 * Describes a style in a span.
 * Note that styles are cumulative -- if both bold and italic are set in
 * separate spans, or if the base style is bold and a span calls for italic,
 * you get bold italic.  You can't turn off a style from the base style.
 *
 */
public class StyleSpan extends MetricAffectingSpan implements ParcelableSpan {

        private final int mStyle;

        /**
         *
         * @param style An integer constant describing the style for this span. Examples
         * include bold, italic, and normal. Values are constants defined
         * in {@link android.graphics.Typeface}.
         */
        public StyleSpan(int style) {
                mStyle = style;
        }

    public StyleSpan(Parcel src) {
        mStyle = src.readInt();
    }

    public int getSpanTypeId() {
        return TextUtils.STYLE_SPAN;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mStyle);
    }

        /**
         * Returns the style constant defined in {@link android.graphics.Typeface}.
         */
        public int getStyle() {
                return mStyle;
        }

        @Override
    public void updateDrawState(TextPaint ds) {
        apply(ds, mStyle);
    }

        @Override
    public void updateMeasureState(TextPaint paint) {
        apply(paint, mStyle);
    }

    private static void apply(Paint paint, int style) {
        int oldStyle;

        Typeface old = paint.getTypeface();
        if (old == null) {
            oldStyle = 0;
        } else {
            oldStyle = old.getStyle();
        }

        int want = oldStyle | style;

        Typeface tf;
        if (old == null) {
            tf = Typeface.defaultFromStyle(want);
        } else {
            tf = Typeface.create(old, want);
        }

        int fake = want & ~tf.getStyle();

        if ((fake & Typeface.BOLD) != 0) {
            paint.setFakeBoldText(true);
        }

        if ((fake & Typeface.ITALIC) != 0) {
            paint.setTextSkewX(-0.25f);
        }

        paint.setTypeface(tf);
    }
}