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
package org.mapsforge.android.maps;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import org.mapsforge.android.maps.MapView.InternalRenderTheme;

class MapGeneratorJobTheme implements Serializable {
	private static final long serialVersionUID = 1L;

	private transient int hashCode;

	/**
	 * Modification date of the theme file, set to -1 for internal themes.
	 */
	final long fileModificationDate;

	/**
	 * Flag to distinguish internal and external rendering themes.
	 */
	final boolean internal;

	/**
	 * Internal rendering theme (might be null).
	 */
	final InternalRenderTheme internalRenderTheme;

	/**
	 * Path to the external rendering theme (might be null).
	 */
	final String themePath;

	MapGeneratorJobTheme(InternalRenderTheme internalRenderTheme) {
		this.internalRenderTheme = internalRenderTheme;
		this.internal = true;
		this.themePath = null;
		this.fileModificationDate = -1;
		calculateTransientValues();
	}

	MapGeneratorJobTheme(String renderTheme, long fileModificationDate) {
		this.themePath = renderTheme;
		this.fileModificationDate = fileModificationDate;
		this.internal = false;
		this.internalRenderTheme = null;
		calculateTransientValues();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof MapGeneratorJobTheme)) {
			return false;
		}
		MapGeneratorJobTheme other = (MapGeneratorJobTheme) obj;
		if (this.internal != other.internal) {
			return false;
		} else if (this.fileModificationDate != other.fileModificationDate) {
			return false;
		} else if (this.internalRenderTheme == null && other.internalRenderTheme != null) {
			return false;
		} else if (this.internalRenderTheme != null
				&& !this.internalRenderTheme.equals(other.internalRenderTheme)) {
			return false;
		} else if (this.themePath == null && other.themePath != null) {
			return false;
		} else if (this.themePath != null && !this.themePath.equals(other.themePath)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return this.hashCode;
	}

	/**
	 * Calculates the hash value of this object.
	 * 
	 * @return the hash value of this object.
	 */
	private int calculateHashCode() {
		int result = 1;
		result = 31 * result + (int) (this.fileModificationDate ^ (this.fileModificationDate >>> 32));
		result = 31 * result + (this.internal ? 1231 : 1237);
		result = 31 * result
				+ ((this.internalRenderTheme == null) ? 0 : this.internalRenderTheme.hashCode());
		result = 31 * result + ((this.themePath == null) ? 0 : this.themePath.hashCode());
		return result;
	}

	/**
	 * Calculates the values of some transient variables.
	 */
	private void calculateTransientValues() {
		this.hashCode = calculateHashCode();
	}

	private void readObject(ObjectInputStream objectInputStream) throws IOException,
			ClassNotFoundException {
		objectInputStream.defaultReadObject();
		calculateTransientValues();
	}
}