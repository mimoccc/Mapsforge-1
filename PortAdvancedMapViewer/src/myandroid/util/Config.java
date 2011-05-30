
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
package myandroid.util;
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

/**
 * Build configuration.  The constants in this class vary depending
 * on release vs. debug build.
 * {@more}
 */
public final class Config
{
    /**
     * If this is a debug build, this field will be true.
     */
    //public static final boolean DEBUG = ConfigBuildFlags.DEBUG;	MARTINUS
    public static final boolean DEBUG = true;

    /*
     * Deprecated fields
     * TODO: Remove platform references to these and @hide them.
     */

    /**
     * @deprecated Use {@link #DEBUG} instead.
     */
    @Deprecated
    public static final boolean RELEASE = !DEBUG;

    /**
     * @deprecated Always false.
     */
    @Deprecated
    public static final boolean PROFILE = false;

    /**
     * @deprecated Always false.
     */
    @Deprecated
    public static final boolean LOGV = false;

    /**
     * @deprecated Always true.
     */
    @Deprecated
    public static final boolean LOGD = true;
}