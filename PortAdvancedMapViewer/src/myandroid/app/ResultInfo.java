
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
package myandroid.app;

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

import myandroid.content.Intent;
import myandroid.os.IBinder;
import myandroid.os.Parcel;
import myandroid.os.Parcelable;
import myandroid.os.Bundle;

import java.util.Map;

/**
 * {@hide}
 */
public class ResultInfo implements Parcelable {
    public final String mResultWho;
    public final int mRequestCode;
    public final int mResultCode;
    public final Intent mData;

    public ResultInfo(String resultWho, int requestCode, int resultCode,
            Intent data) {
        mResultWho = resultWho;
        mRequestCode = requestCode;
        mResultCode = resultCode;
        mData = data;
    }

    public String toString() {
        return "ResultInfo{who=" + mResultWho + ", request=" + mRequestCode
            + ", result=" + mResultCode + ", data=" + mData + "}";
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mResultWho);
        out.writeInt(mRequestCode);
        out.writeInt(mResultCode);
        if (mData != null) {
            out.writeInt(1);
            mData.writeToParcel(out, 0);
        } else {
            out.writeInt(0);
        }
    }

    public static final Parcelable.Creator<ResultInfo> CREATOR
            = new Parcelable.Creator<ResultInfo>() {
        public ResultInfo createFromParcel(Parcel in) {
            return new ResultInfo(in);
        }

        public ResultInfo[] newArray(int size) {
            return new ResultInfo[size];
        }
    };

    public ResultInfo(Parcel in) {
        mResultWho = in.readString();
        mRequestCode = in.readInt();
        mResultCode = in.readInt();
        if (in.readInt() != 0) {
            mData = Intent.CREATOR.createFromParcel(in);
        } else {
            mData = null;
        }
    }
}