
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
 * When an object of this type is attached to a Spannable, its methods
 * will be called to notify it that other markup objects have been
 * added, changed, or removed.
 */
public interface SpanWatcher extends NoCopySpan {
    /**
     * This method is called to notify you that the specified object
     * has been attached to the specified range of the text.
     */
    public void onSpanAdded(Spannable text, Object what, int start, int end);
    /**
     * This method is called to notify you that the specified object
     * has been detached from the specified range of the text.
     */
    public void onSpanRemoved(Spannable text, Object what, int start, int end);
    /**
     * This method is called to notify you that the specified object
     * has been relocated from the range <code>ostart&hellip;oend</code>
     * to the new range <code>nstart&hellip;nend</code> of the text.
     */
    public void onSpanChanged(Spannable text, Object what, int ostart, int oend,
                              int nstart, int nend);
}