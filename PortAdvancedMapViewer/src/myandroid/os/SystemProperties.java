
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
package myandroid.os;

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
 * Gives access to the system properties store.  The system properties
 * store contains a list of string key-value pairs.
 *
 * {@hide}
 */
public class SystemProperties
{
    public static final int PROP_NAME_MAX = 31;
    public static final int PROP_VALUE_MAX = 91;

    private static native String native_get(String key);
    private static native String native_get(String key, String def);
    private static native int native_get_int(String key, int def);
    private static native long native_get_long(String key, long def);
    private static native boolean native_get_boolean(String key, boolean def);
    private static native void native_set(String key, String def);

    /**
     * Get the value for the given key.
     * @return an empty string if the key isn't found
     * @throws IllegalArgumentException if the key exceeds 32 characters
     */
    public static String get(String key) {
        if (key.length() > PROP_NAME_MAX) {
            throw new IllegalArgumentException("key.length > " + PROP_NAME_MAX);
        }
        return native_get(key);
    }

    /**
     * Get the value for the given key.
     * @return if the key isn't found, return def if it isn't null, or an empty string otherwise
     * @throws IllegalArgumentException if the key exceeds 32 characters
     */
    public static String get(String key, String def) {
        if (key.length() > PROP_NAME_MAX) {
            throw new IllegalArgumentException("key.length > " + PROP_NAME_MAX);
        }
        return native_get(key, def);
    }

    /**
     * Get the value for the given key, and return as an integer.
     * @param key the key to lookup
     * @param def a default value to return
     * @return the key parsed as an integer, or def if the key isn't found or
     *         cannot be parsed
     * @throws IllegalArgumentException if the key exceeds 32 characters
     */
    public static int getInt(String key, int def) {
        if (key.length() > PROP_NAME_MAX) {
            throw new IllegalArgumentException("key.length > " + PROP_NAME_MAX);
        }
        return native_get_int(key, def);
    }

    /**
     * Get the value for the given key, and return as a long.
     * @param key the key to lookup
     * @param def a default value to return
     * @return the key parsed as a long, or def if the key isn't found or
     *         cannot be parsed
     * @throws IllegalArgumentException if the key exceeds 32 characters
     */
    public static long getLong(String key, long def) {
        if (key.length() > PROP_NAME_MAX) {
            throw new IllegalArgumentException("key.length > " + PROP_NAME_MAX);
        }
        return native_get_long(key, def);
    }

    /**
     * Get the value for the given key, returned as a boolean.
     * Values 'n', 'no', '0', 'false' or 'off' are considered false.
     * Values 'y', 'yes', '1', 'true' or 'on' are considered true.
     * (case insensitive).
     * If the key does not exist, or has any other value, then the default
     * result is returned.
     * @param key the key to lookup
     * @param def a default value to return
     * @return the key parsed as a boolean, or def if the key isn't found or is
     *         not able to be parsed as a boolean.
     * @throws IllegalArgumentException if the key exceeds 32 characters
     */
    public static boolean getBoolean(String key, boolean def) {
        if (key.length() > PROP_NAME_MAX) {
            throw new IllegalArgumentException("key.length > " + PROP_NAME_MAX);
        }
        return native_get_boolean(key, def);
    }

    /**
     * Set the value for the given key.
     * @throws IllegalArgumentException if the key exceeds 32 characters
     * @throws IllegalArgumentException if the value exceeds 92 characters
     */
    public static void set(String key, String val) {
        if (key.length() > PROP_NAME_MAX) {
            throw new IllegalArgumentException("key.length > " + PROP_NAME_MAX);
        }
        if (val != null && val.length() > PROP_VALUE_MAX) {
            throw new IllegalArgumentException("val.length > " +
                PROP_VALUE_MAX);
        }
        native_set(key, val);
    }
}