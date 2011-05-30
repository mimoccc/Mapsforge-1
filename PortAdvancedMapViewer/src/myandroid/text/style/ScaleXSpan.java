
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

import myandroid.os.Parcel;
import myandroid.text.ParcelableSpan;
import myandroid.text.TextPaint;
import myandroid.text.TextUtils;

public class ScaleXSpan extends MetricAffectingSpan implements ParcelableSpan {

        private final float mProportion;

        public ScaleXSpan(float proportion) {
                mProportion = proportion;
        }

    public ScaleXSpan(Parcel src) {
        mProportion = src.readFloat();
    }

    public int getSpanTypeId() {
        return TextUtils.SCALE_X_SPAN;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(mProportion);
    }

        public float getScaleX() {
                return mProportion;
        }

        @Override
        public void updateDrawState(TextPaint ds) {
                ds.setTextScaleX(ds.getTextScaleX() * mProportion);
        }

        @Override
        public void updateMeasureState(TextPaint ds) {
                ds.setTextScaleX(ds.getTextScaleX() * mProportion);
        }
}