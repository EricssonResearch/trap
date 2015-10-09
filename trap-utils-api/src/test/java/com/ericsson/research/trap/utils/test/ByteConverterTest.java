package com.ericsson.research.trap.utils.test;

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

import junit.framework.Assert;

import org.junit.Test;

import com.ericsson.research.trap.utils.ByteConverter;

public class ByteConverterTest
{
	@Test
	public void test7Bit()
	{
		// Iterate over a couple of potentially disastrous sequences
		for (int i = -257; i < 257; i++)
			this.doAssert7(i);

		// Iterate over the overflow value
		int k = Integer.MAX_VALUE - 10;
		for (long l = Integer.MAX_VALUE - 10; l < (Integer.MAX_VALUE + 100); l++)
			this.doAssert7(k++);

		// Do some random values
		int max = 0x1000000 - 2;
		int min = (0 - 0x1000000) + 1;

		for (int i = 0; i < 11165535; i++)
			this.doAssert7((int) ((Math.random() * (max - min)) + min));

	}

	private void doAssert7(int i)
	{
		Assert.assertEquals(i, ByteConverter.fromBigEndian7(ByteConverter.toBigEndian7(i), 0));
	}

	@Test
	public void test8Bit()
	{
		// Iterate over a couple of potentially disastrous sequences
		for (int i = -258; i < 258; i++)
			this.doAssert8(i);

		// Iterate over the overflow value
		int k = Integer.MAX_VALUE - 10;
		for (long l = Integer.MAX_VALUE - 10; l < (Integer.MAX_VALUE + 100); l++)
			this.doAssert8(k++);

		// Do some random values
		int max = 0x1000000 - 2;
		int min = (0 - 0x1000000) + 1;

		for (int i = 0; i < 11165535; i++)
			this.doAssert8((int) ((Math.random() * (max - min)) + min));

	}

	private void doAssert8(int i)
	{
		Assert.assertEquals(i, ByteConverter.fromBigEndian(ByteConverter.toBigEndian(i), 0));
	}
}
