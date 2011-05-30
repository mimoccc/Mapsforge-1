
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
package myandroid.graphics;

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

public class PorterDuff {

    // these value must match their native equivalents. See SkPorterDuff.h
    public enum Mode {
        /** [0, 0] */
        CLEAR       (0),
        /** [Sa, Sc] */
        SRC         (1),
        /** [Da, Dc] */
        DST         (2),
        /** [Sa + (1 - Sa)*Da, Rc = Sc + (1 - Sa)*Dc] */
        SRC_OVER    (3),
        /** [Sa + (1 - Sa)*Da, Rc = Dc + (1 - Da)*Sc] */
        DST_OVER    (4),
        /** [Sa * Da, Sc * Da] */
        SRC_IN      (5),
        /** [Sa * Da, Sa * Dc] */
        DST_IN      (6),
        /** [Sa * (1 - Da), Sc * (1 - Da)] */
        SRC_OUT     (7),
        /** [Da * (1 - Sa), Dc * (1 - Sa)] */
        DST_OUT     (8),
        /** [Da, Sc * Da + (1 - Sa) * Dc] */
        SRC_ATOP    (9),
        /** [Sa, Sa * Dc + Sc * (1 - Da)] */
        DST_ATOP    (10),
        /** [Sa + Da - 2 * Sa * Da, Sc * (1 - Da) + (1 - Sa) * Dc] */
        XOR         (11),
        /** [Sa + Da - Sa*Da,
             Sc*(1 - Da) + Dc*(1 - Sa) + min(Sc, Dc)] */
        DARKEN      (12),
        /** [Sa + Da - Sa*Da,
             Sc*(1 - Da) + Dc*(1 - Sa) + max(Sc, Dc)] */
        LIGHTEN     (13),
        /** [Sa * Da, Sc * Dc] */
        MULTIPLY    (14),
        /** [Sa + Da - Sa * Da, Sc + Dc - Sc * Dc] */
        SCREEN      (15);

        Mode(int nativeInt) {
            this.nativeInt = nativeInt;
        }
        final int nativeInt;
    }
}