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
package org.mapsforge.android.maps.mapgenerator;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import org.mapsforge.android.maps.DebugSettings;
import org.mapsforge.android.maps.MapViewMode;
import org.mapsforge.android.maps.Tile;

/**
 * A MapGeneratorJob holds all immutable rendering parameters for a single map image together with a mutable
 * priority field, which indicates the importance of this job.
 */
public class MapGeneratorJob implements Comparable<MapGeneratorJob>, Serializable {
	private static final long serialVersionUID = 1L;

	public final boolean drawTileCoordinates;
	public final boolean drawTileFrames;
	public final JobParameters jobParameters;

	/**
	 * The tile which should be generated.
	 */
	public final Tile tile;

	private transient int hashCodeValue;
	private transient double priority;
	final boolean highlightWater;
	final String mapFile;
	final MapViewMode mapViewMode;

	/**
	 * Creates a new job for a MapGenerator with the given parameters.
	 */
	public MapGeneratorJob(Tile tile, MapViewMode mapViewMode, String mapFile, JobParameters jobParameters,
			DebugSettings debugSettings) {
		this.tile = tile;
		this.mapViewMode = mapViewMode;
		this.mapFile = mapFile;
		this.jobParameters = jobParameters;
		this.drawTileFrames = debugSettings.isDrawTileFrames();
		this.drawTileCoordinates = debugSettings.isDrawTileCoordinates();
		this.highlightWater = debugSettings.isHighlightWaterTiles();
		calculateTransientValues();
	}

	@Override
	public int compareTo(MapGeneratorJob otherMapGeneratorJob) {
		if (this.priority < otherMapGeneratorJob.priority) {
			return -1;
		} else if (this.priority > otherMapGeneratorJob.priority) {
			return 1;
		}
		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof MapGeneratorJob)) {
			return false;
		}
		MapGeneratorJob other = (MapGeneratorJob) obj;
		if (!this.tile.equals(other.tile)) {
			return false;
		} else if (this.mapViewMode != other.mapViewMode) {
			return false;
		} else if (this.mapFile == null && other.mapFile != null) {
			return false;
		} else if (this.mapFile != null && !this.mapFile.equals(other.mapFile)) {
			return false;
		} else if (this.jobParameters == null && other.jobParameters != null) {
			return false;
		} else if (this.jobParameters != null && !this.jobParameters.equals(other.jobParameters)) {
			return false;
		} else if (this.drawTileFrames != other.drawTileFrames) {
			return false;
		} else if (this.drawTileCoordinates != other.drawTileCoordinates) {
			return false;
		} else if (this.highlightWater != other.highlightWater) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return this.hashCodeValue;
	}

	/**
	 * @return the hash code of this object.
	 */
	private int calculateHashCode() {
		int result = 7;
		result = 31 * result + ((this.tile == null) ? 0 : this.tile.hashCode());
		result = 31 * result + ((this.mapViewMode == null) ? 0 : this.mapViewMode.hashCode());
		result = 31 * result + ((this.mapFile == null) ? 0 : this.mapFile.hashCode());
		result = 31 * result + ((this.jobParameters == null) ? 0 : this.jobParameters.hashCode());
		result = 31 * result + (this.drawTileFrames ? 1231 : 1237);
		result = 31 * result + (this.drawTileCoordinates ? 1231 : 1237);
		result = 31 * result + (this.highlightWater ? 1231 : 1237);
		return result;
	}

	/**
	 * Calculates the values of some transient variables.
	 */
	private void calculateTransientValues() {
		this.hashCodeValue = calculateHashCode();
	}

	private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
		objectInputStream.defaultReadObject();
		calculateTransientValues();
	}

	void setPriority(double priority) {
		this.priority = priority;
	}
}
