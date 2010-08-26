/*
 * Copyright 2010 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.server.poi.persistence;

import org.garret.perst.Assert;

/**
 * Provides for methods for computing the hilbert value in two and three dimensional space.
 * 
 * @author weise
 * 
 */
class Hilbert {

	private static final HilbertState state0 = new HilbertState(new String[] { "000", "001",
			"011", "010", "111", "110", "100", "101" }, new int[] { 1, 2, 3, 2, 4, 5, 3, 5 });
	private static final HilbertState state1 = new HilbertState(new String[] { "000", "111",
			"001", "110", "011", "100", "010", "101" }, new int[] { 2, 6, 0, 7, 8, 8, 0, 7 });
	private static final HilbertState state2 = new HilbertState(new String[] { "000", "011",
			"111", "100", "001", "010", "110", "101" }, new int[] { 0, 9, 10, 9, 1, 1, 11, 11 });
	private static final HilbertState state3 = new HilbertState(new String[] { "010", "011",
			"001", "000", "101", "100", "110", "111" }, new int[] { 6, 0, 6, 11, 9, 0, 9, 8, });
	private static final HilbertState state4 = new HilbertState(new String[] { "100", "011",
			"101", "010", "111", "000", "110", "001" }, new int[] { 11, 11, 0, 7, 5, 9, 0, 7 });
	private static final HilbertState state5 = new HilbertState(new String[] { "110", "101",
			"001", "010", "111", "100", "000", "011" }, new int[] { 4, 4, 8, 8, 0, 6, 10, 6 });
	private static final HilbertState state6 = new HilbertState(new String[] { "100", "111",
			"011", "000", "101", "110", "010", "001" }, new int[] { 5, 7, 5, 3, 1, 1, 11, 11 });
	private static final HilbertState state7 = new HilbertState(new String[] { "110", "111",
			"101", "100", "001", "000", "010", "011" }, new int[] { 6, 1, 6, 10, 9, 4, 9, 10 });
	private static final HilbertState state8 = new HilbertState(new String[] { "010", "101",
			"011", "100", "001", "110", "000", "111" }, new int[] { 10, 3, 1, 1, 10, 3, 5, 9 });
	private static final HilbertState state9 = new HilbertState(new String[] { "010", "001",
			"101", "110", "011", "000", "100", "111" }, new int[] { 4, 4, 8, 8, 2, 7, 2, 3 });
	private static final HilbertState state10 = new HilbertState(new String[] { "100", "101",
			"111", "110", "011", "010", "000", "001" }, new int[] { 7, 2, 11, 2, 7, 5, 8, 5 });
	private static final HilbertState state11 = new HilbertState(new String[] { "110", "001",
			"111", "000", "101", "010", "100", "011" }, new int[] { 10, 3, 2, 6, 10, 3, 4, 4 });

	private static final HilbertState[] states3D = new HilbertState[] { state0, state1, state2,
			state3, state4, state5, state6, state7, state8, state9, state10, state11 };

	private static class HilbertStateMachine {

		private final HilbertState[] states;
		private final int order = 21;

		public HilbertStateMachine(HilbertState[] states) {
			super();
			this.states = states;
		}

		Long computeValue(int startingState, int[] values) {
			String[] data = interleaveAndChop(values);

			HilbertState state = this.states[startingState];
			for (int i = 0; i < data.length; i++) {
				state = this.states[state.transition(data, i)];
			}

			return Long.parseLong(concat(data), 2);
		}

		private String[] interleaveAndChop(int[] values) {
			String[] binaries = new String[values.length];
			for (int i = 0; i < values.length; i++) {
				binaries[i] = normalize(Integer.toBinaryString(values[i]), order);
			}
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < order; i++) {
				for (int j = 0; j < binaries.length; j++) {
					sb.append(binaries[j].charAt(i));
				}
			}

			return sb.toString().split("(?<=\\G(.{" + values.length + "}))");
		}

		private String concat(String[] data) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < data.length; i++) {
				sb.append(data[i]);
			}

			String binaryString = sb.toString();

			return binaryString.substring(binaryString.indexOf("1"), binaryString.length());
		}

	}

	private static class HilbertState {

		private final String[] results;
		private final int[] states;

		public HilbertState(String[] results, int[] states) {
			super();
			this.results = results;
			this.states = states;
		}

		int transition(String[] data, int pos) {
			int input = new Long(Long.parseLong(data[pos], 2)).intValue();
			data[pos] = results[input];
			return states[input];
		}

	}

	public static Long computeValue3D(int x, int y, int z) {
		return new HilbertStateMachine(states3D).computeValue(0, new int[] { x / 100, y / 100,
				z / 100 });
	}

	public static long computeValue(int latitude, int longitude) {
		String latString = normalize(Integer.toBinaryString(latitude), 32);
		String lngString = normalize(Integer.toBinaryString(longitude), 32);

		String interleaved = interleave(lngString, latString);
		String[] chunks = interleaved.split("(?<=\\G(.{2}))");
		int[] values = new int[chunks.length];

		for (int i = 0; i < chunks.length; i++) {
			if (chunks[i].equals("00")) {
				values[i] = 0;
			} else if (chunks[i].equals("01")) {
				values[i] = 1;
			} else if (chunks[i].equals("10")) {
				values[i] = 3;
			} else {
				values[i] = 2;
			}
		}

		for (int i = 0; i < values.length; i++) {
			if (values[i] == 0) {
				switchValues(values, 1, 3, i);
			} else if (values[i] == 3) {
				switchValues(values, 2, 0, i);
			}
		}

		return Long.parseLong(valueArrayToBinaryString(values), 2);
	}

	static String normalize(String binaryString, int order) {
		Assert.that(binaryString.length() <= order);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < (order - binaryString.length()); i++) {
			sb.append('0');
		}
		sb.append(binaryString);
		return sb.toString();
	}

	private static String interleave(String x, String y) {
		Assert.that(x.length() == y.length());
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < x.length(); i++) {
			sb.append(x.charAt(i));
			sb.append(y.charAt(i));
		}
		return sb.toString();
	}

	private static void switchValues(int[] array, int a, int b, int startAt) {
		for (int i = startAt; i < array.length; i++) {
			if (array[i] == a) {
				array[i] = b;
			} else if (array[i] == b) {
				array[i] = a;
			}
		}
	}

	private static String valueArrayToBinaryString(int[] values) {
		StringBuilder sb = new StringBuilder();
		for (int c : values) {
			if (c == 0) {
				sb.append("00");
			} else if (c == 1) {
				sb.append("01");
			} else if (c == 2) {
				sb.append("10");
			} else {
				sb.append("11");
			}
		}
		return sb.toString();
	}

}
