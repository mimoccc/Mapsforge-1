package org.mapsforge.android.maps;

import org.mapsforge.preprocessing.map.osmosis.Serializer;

abstract class AsyncTester extends Thread {
	private long max;
	private long min;
	private long number;
	private long oldNumber;
	private long skip;
	private long timeEnd;
	private long timeStart;

	@Override
	public final void run() {
		this.min = getMinValue();
		this.max = getMaxValue();
		this.skip = getSkip();

		if (this.skip < 1) {
			System.out.println(getDescription() + ": invalid interval " + this.skip);
			return;
		}

		this.number = this.min;
		this.timeStart = System.currentTimeMillis();
		while (true) {
			try {
				if (decode(encode(this.number)) != this.number) {
					System.out.println(getDescription() + ": error " + this.number + " <> "
							+ decode(encode(this.number)));
					return;
				}
			} catch (ArrayIndexOutOfBoundsException e) {
				System.out.println(getDescription() + ": ArrayIndexOutOfBoundsException "
						+ this.number);
				return;
			}

			this.oldNumber = this.number;
			this.number += this.skip;
			if (this.number > this.max) {
				// maximum reached
				break;
			} else if (this.number < this.oldNumber) {
				// overflow
				break;
			}
		}

		if (this.skip > 1) {
			// check upper boundary case separately
			this.number = this.max;
			try {
				if (decode(encode(this.number)) != this.number) {
					System.out.println(getDescription() + ": error " + this.number + " <> "
							+ decode(encode(this.number)));
					return;
				}
			} catch (ArrayIndexOutOfBoundsException e) {
				System.out.println(getDescription() + ": ArrayIndexOutOfBoundsException "
						+ this.number);
				return;
			}
		}

		this.timeEnd = System.currentTimeMillis();
		System.out.println(getDescription() + ": finished in "
				+ ((this.timeEnd - this.timeStart) / 1000f) + " seconds, skip: " + this.skip);
	}

	abstract long decode(byte[] buffer);

	abstract byte[] encode(long value);

	abstract String getDescription();

	abstract long getMaxValue();

	abstract long getMinValue();

	abstract long getSkip();
}

class EncodingTest {
	/**
	 * @param args
	 *            no arguments expected
	 */
	public static void main(String[] args) {
		AsyncTester asyncTester;
		// TODO
		// asyncTester = new TestShortEncoding();
		// asyncTester.start();
		//
		// asyncTester = new TestVBUEncoding();
		// asyncTester.start();
		//
		// asyncTester = new TestIntEncoding();
		// asyncTester.start();
		//
		// asyncTester = new TestVBSEncoding();
		// asyncTester.start();

		asyncTester = new Test5BytesEncoding();
		asyncTester.start();

		// asyncTester = new TestLongEncoding();
		// asyncTester.start();
	}
}

class Test5BytesEncoding extends AsyncTester {
	@Override
	long decode(byte[] buffer) {
		return org.mapsforge.android.maps.Deserializer.getFiveBytesLong(buffer, 0);
	}

	@Override
	byte[] encode(long value) {
		return Serializer.getFiveBytes(value);
	}

	@Override
	String getDescription() {
		return "5BytesEncoding";
	}

	@Override
	long getMaxValue() {
		return 1099511627775L; // 2^40 - 1
	}

	@Override
	long getMinValue() {
		return 0;
	}

	@Override
	long getSkip() {
		return 1;
	}
}

class TestIntEncoding extends AsyncTester {
	@Override
	long decode(byte[] buffer) {
		return Deserializer.getInt(buffer, 0);
	}

	@Override
	byte[] encode(long value) {
		return Serializer.getBytes((int) value);
	}

	@Override
	String getDescription() {
		return "IntEncoding";
	}

	@Override
	long getMaxValue() {
		return Integer.MAX_VALUE;
	}

	@Override
	long getMinValue() {
		return Integer.MIN_VALUE;
	}

	@Override
	long getSkip() {
		return 1;
	}
}

class TestLongEncoding extends AsyncTester {
	@Override
	long decode(byte[] buffer) {
		return Deserializer.getLong(buffer, 0);
	}

	@Override
	byte[] encode(long value) {
		return Serializer.getBytes(value);
	}

	@Override
	String getDescription() {
		return "LongEncoding";
	}

	@Override
	long getMaxValue() {
		return Long.MAX_VALUE;
	}

	@Override
	long getMinValue() {
		return Long.MIN_VALUE;
	}

	@Override
	long getSkip() {
		return Integer.MAX_VALUE >> 1;
	}
}

class TestShortEncoding extends AsyncTester {
	@Override
	long decode(byte[] buffer) {
		return Deserializer.getShort(buffer, 0);
	}

	@Override
	byte[] encode(long value) {
		return Serializer.getBytes((short) value);
	}

	@Override
	String getDescription() {
		return "ShortEncoding";
	}

	@Override
	long getMaxValue() {
		return Short.MAX_VALUE;
	}

	@Override
	long getMinValue() {
		return Short.MIN_VALUE;
	}

	@Override
	long getSkip() {
		return 1;
	}
}

class TestVBSEncoding extends AsyncTester {
	private int bufferPosition;
	private byte[] readBuffer;
	private int variableByteDecode;
	private int variableByteShift;

	/**
	 * Converts a variable amount of bytes to a signed int.
	 * <p>
	 * The first bit is for continuation info, the other six (last byte) or seven (all other
	 * bytes) bits for data. The second bit in the last byte indicates the sign of the number.
	 * 
	 * @return the int value.
	 */
	private int getVariableByteEncodedSignedInt() {
		this.variableByteDecode = 0;
		this.variableByteShift = 0;

		while ((this.readBuffer[this.bufferPosition] & 0x80) != 0) {
			this.variableByteDecode |= (this.readBuffer[this.bufferPosition] & 0x7f) << this.variableByteShift;
			++this.bufferPosition;
			this.variableByteShift += 7;
		}

		// read the six data bits from the last byte
		if ((this.readBuffer[this.bufferPosition] & 0x40) != 0) {
			// negative
			return -(this.variableByteDecode | ((this.readBuffer[this.bufferPosition] & 0x3f) << this.variableByteShift));
		}
		// positive
		return this.variableByteDecode
				| ((this.readBuffer[this.bufferPosition] & 0x3f) << this.variableByteShift);
	}

	@Override
	long decode(byte[] buffer) {
		this.readBuffer = buffer;
		this.bufferPosition = 0;
		return getVariableByteEncodedSignedInt();
	}

	@Override
	byte[] encode(long value) {
		return Serializer.getVariableByteSigned((int) value);
	}

	@Override
	String getDescription() {
		return "VBSEncoding";
	}

	@Override
	long getMaxValue() {
		return Integer.MAX_VALUE;
	}

	@Override
	long getMinValue() {
		return Integer.MIN_VALUE;
	}

	@Override
	long getSkip() {
		return 1;
	}
}

class TestVBUEncoding extends AsyncTester {
	private int bufferPosition;
	private byte[] readBuffer;
	private int variableByteDecode;
	private int variableByteShift;

	/**
	 * Converts a variable amount of bytes to an unsigned int.
	 * <p>
	 * The first bit is for continuation info, the other seven bits for data.
	 * 
	 * @return the int value.
	 */
	private int getVariableByteEncodedUnsignedInt() {
		this.variableByteDecode = 0;
		this.variableByteShift = 0;

		while ((this.readBuffer[this.bufferPosition] & 0x80) != 0) {
			this.variableByteDecode |= (this.readBuffer[this.bufferPosition] & 0x7f) << this.variableByteShift;
			++this.bufferPosition;
			this.variableByteShift += 7;
		}

		// read the seven data bits from the last byte
		return this.variableByteDecode
				| (this.readBuffer[this.bufferPosition] << this.variableByteShift);
	}

	@Override
	long decode(byte[] buffer) {
		this.readBuffer = buffer;
		this.bufferPosition = 0;
		return getVariableByteEncodedUnsignedInt();
	}

	@Override
	byte[] encode(long value) {
		return Serializer.getVariableByteUnsigned((int) value);
	}

	@Override
	String getDescription() {
		return "VBUEncoding";
	}

	@Override
	long getMaxValue() {
		return Integer.MAX_VALUE;
	}

	@Override
	long getMinValue() {
		return 0;
	}

	@Override
	long getSkip() {
		return 1;
	}
}