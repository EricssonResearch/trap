package com.ericsson.research.trap.utils;

/*
 * ##_BEGIN_LICENSE_##
 * Transport Abstraction Package (trap)
 * ----------
 * Copyright (C) 2014 Ericsson AB
 * ----------
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Ericsson AB nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * ##_END_LICENSE_##
 */

/**
 * Methods for converting integers to and from bytes, while preserving signs.
 *
 * @author Vladimir Katardjiev
 */
public class ByteConverter
{

	/**
	 * Converts an integer into big endian, 8-bit format, creating a new array.
	 *
	 * @param i
	 *            The integer to convert.
	 * @return A new array with the serialization of i.
	 */
	public static byte[] toBigEndian(int i)
	{
		byte[] arr = new byte[4];

		return toBigEndian(i, arr, 0);
	}

	/**
	 * Converts an integer to big endian, writing the result into a target
	 * array.
	 *
	 * @param src
	 *            The integer to convert.
	 * @param arr
	 *            The array to write to.
	 * @param off
	 *            The offset in the array to start writing to.
	 * @return The array <i>arr</i>
	 * @throws ArrayIndexOutOfBoundsException
	 *             If there are less than four bytes available in <i>arr</i>
	 *             from position <i>off</i>.
	 */
	public static byte[] toBigEndian(int src, byte[] arr, int off)
	{
		arr[off + 0] = (byte) (src >> 24);
		arr[off + 1] = (byte) ((src >> 16) & 0xFF);
		arr[off + 2] = (byte) ((src >> 8) & 0xFF);
		arr[off + 3] = (byte) ((src >> 0) & 0xFF);
		return arr;
	}

	/**
	 * Parses a big endian, 8-bit integer, from a byte array.
	 *
	 * @param arr
	 *            The array to parse from.
	 * @param offset
	 *            The offset in the array to read from.
	 * @return The parsed integer.
	 * @throws ArrayIndexOutOfBoundsException
	 *             If there are less than 4 bytes in the array from offset.
	 */
	public static int fromBigEndian(byte[] arr, int offset)
	{
		int rv = 0;

		rv |= (arr[offset + 0] & 0xFF) << 24;
		rv |= ((arr[offset + 1] & 0xFF) << 16);
		rv |= ((arr[offset + 2] & 0xFF) << 8);
		rv |= ((arr[offset + 3] & 0xFF) << 0);

		return rv;
	}

	/**
	 * Converts an integer to a 7-bit representation of an integer by only
	 * taking the 28 lowest-order values. Does not perform any wrapping, so if
	 * the integer is larger than 2^28, it will become negative. This is used
	 * for serializing Trap's 7-bit format.
	 *
	 * @param src
	 *            The integer to convert.
	 * @return A new array containing the Trap 7-bit-binary representation of
	 *         the integer.
	 */
	public static byte[] toBigEndian7(int src)
	{

		byte[] rv = new byte[4];

		rv[0] = (byte) getBits(src, 4, 11);
		rv[1] = (byte) getBits(src, 11, 18);
		rv[2] = (byte) getBits(src, 18, 25);
		rv[3] = (byte) getBits(src, 25, 32);

		return rv;
	}

	/**
	 * Parses a 7-bit-binary representation of an integer from an array.
	 *
	 * @param arr
	 *            The array to read from
	 * @param offset
	 *            The offset in the array to read from
	 * @return The parsed integer.
	 * @throws ArrayIndexOutOfBoundsException
	 *             If there is not enough data in arr.
	 */
	public static int fromBigEndian7(byte[] arr, int offset)
	{
		int rv = 0;

		rv |= (arr[offset + 0] & 0xFF) << 21;
		rv |= ((arr[offset + 1] & 0xFF) << 14);
		rv |= ((arr[offset + 2] & 0xFF) << 7);
		rv |= ((arr[offset + 3] & 0xFF) << 0);

		if (getBits(rv, 4, 5) == 1)
			rv |= 0xF0000000;

		return rv;
	}

	/**
	 * Fetch a number of bits from an integer.
	 *
	 * @param src
	 *            The source to take from
	 * @param startBit
	 *            The first bit, with starting index 0
	 * @param endBit
	 *            The end index. This index will NOT be included in the return
	 *            value.
	 * @return The endBit-startBit number of bits from index startBit will be in
	 *         the lowest order bits of the returned value.
	 */
	public static int getBits(int src, int startBit, int endBit)
	{
		int mask = (int) (Math.pow(2, endBit - startBit) - 1);
		mask = mask << (32 - endBit);
		int rv = (src & mask) >> (32 - endBit);
		return rv;
	}

	private ByteConverter()
	{
	}
}
