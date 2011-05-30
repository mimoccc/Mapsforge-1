
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
package myandroid.text;

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

import myandroid.os.Parcel;

/**
 * Annotations are simple key-value pairs that are preserved across
 * TextView save/restore cycles and can be used to keep application-specific
 * data that needs to be maintained for regions of text.
 */
public class Annotation implements ParcelableSpan {
    private final String mKey;
    private final String mValue;

    public Annotation(String key, String value) {
        mKey = key;
        mValue = value;
    }

    public Annotation(Parcel src) {
        mKey = src.readString();
        mValue = src.readString();
    }

    public int getSpanTypeId() {
        return TextUtils.ANNOTATION;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mKey);
        dest.writeString(mValue);
    }

    public String getKey() {
        return mKey;
    }

    public String getValue() {
        return mValue;
    }
}