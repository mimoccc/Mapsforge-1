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
package org.mapsforge.preprocessing.routing.hhmobile.util;

public final class Utils {

	public static int min(int[] arr) {
		if (arr.length == 0) {
			return Integer.MAX_VALUE;
		}
		int min = arr[0];
		for (int val : arr) {
			min = Math.min(min, val);
		}
		return min;
	}

	public static int max(int[] arr) {
		if (arr.length == 0) {
			return Integer.MIN_VALUE;
		}
		int max = arr[0];
		for (int val : arr) {
			max = Math.max(max, val);
		}
		return max;
	}

	public static String arrToString(int[] arr) {
		StringBuilder sb = new StringBuilder();
		for (int i : arr) {
			sb.append(i + ", ");
		}
		sb.replace(Math.max(0, sb.length() - 2), sb.length(), "");
		return sb.toString();
	}

	public static <T> String arrToString(T[] arr) {
		StringBuilder sb = new StringBuilder();
		for (T t : arr) {
			sb.append(t + ", ");
		}
		sb.replace(Math.max(0, sb.length() - 2), sb.length(), "");
		return sb.toString();
	}

	public static void setZero(byte[] b, int offset, int len) {
		for (int i = 0; i < len; i++) {
			b[offset + i] = 0;
		}
	}

	public static <T> void swap(T[] arr, int i, int j) {
		T tmp = arr[i];
		arr[i] = arr[j];
		arr[j] = tmp;
	}

	public static void swap(int[] arr, int i, int j) {
		int tmp = arr[i];
		arr[i] = arr[j];
		arr[j] = tmp;
	}

	public static byte numBitsToEncode(int minVal, int maxVal) {
		int interval = maxVal - minVal + 1;
		return (byte) (Math.floor(Math.log(interval) / Math.log(2)) + 1);
	}

}
