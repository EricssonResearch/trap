package com.ericsson.research.trap.impl;

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

import java.io.IOException;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.ericsson.research.trap.spi.TrapMessage;
import com.ericsson.research.trap.spi.TrapMessage.Format;
import com.ericsson.research.trap.spi.TrapMessage.Operation;

public class TrapMessageTest
{
	@Test
	public void test7BitSerialization() throws IOException
	{
		String authMessage = "Authenticate!";
		byte[] messageData = "Foobar".getBytes();
		
		// Create a message
		TrapMessageImpl m = new TrapMessageImpl();
		m.setOp(Operation.CHALLENGE);
		m.setAuthData(authMessage);
		m.setData(messageData);
		m.setFormat(Format.SEVEN_BIT_SAFE);
		
		byte[] data = m.serialize();
		
		TrapMessageImpl n = new TrapMessageImpl(data);
		
		assert (Arrays.equals(n.getData(), messageData));
		Assert.assertEquals(n.getAuthData(), authMessage);
	}
	
	@Test
	public void test7BitTwosComplement() throws Exception
	{
		
		String authMessage = "Authenticate!";
		byte[] messageData = "Foobar".getBytes();
		
		// Create a message
		TrapMessageImpl m = new TrapMessageImpl();
		m.setOp(Operation.CHALLENGE);
		m.setAuthData(authMessage);
		m.setData(messageData);
		m.setFormat(Format.SEVEN_BIT_SAFE);
		
		byte[] data = m.serialize();
		
		TrapMessageImpl n = new TrapMessageImpl(data);
		
		Assert.assertArrayEquals(messageData, n.getData());
		Assert.assertEquals(authMessage, n.getAuthData());
		Assert.assertEquals(m.getFormat(), n.getFormat());
		Assert.assertEquals(m.getOp(), n.getOp());
	}
	
	// Test 8 bit
	@Test()
	public void test8BitSerialization() throws IOException
	{
		String authMessage = "Authenticate!";
		byte[] messageData = "Foobar".getBytes();
		
		// Create a message
		TrapMessageImpl m = new TrapMessageImpl();
		m.setOp(Operation.CHALLENGE);
		m.setAuthData(authMessage);
		m.setData(messageData);
		m.setFormat(Format.REGULAR);
		
		byte[] data = m.serialize();
		
		TrapMessageImpl n = new TrapMessageImpl(data);
		
		Assert.assertArrayEquals(messageData, n.getData());
		Assert.assertEquals(authMessage, n.getAuthData());
		Assert.assertEquals(m.getFormat(), n.getFormat());
		Assert.assertEquals(m.getOp(), n.getOp());
	}
	
	@Test
	public void test8BitSerialize() throws Exception
	{
		String authMessage = "Authenticate!";
		byte[] messageData = "Foobar".getBytes();
		byte[] expected = new byte[] { -123, 0, 0, 13, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 6, 65, 117, 116, 104, 101, 110, 116, 105, 99, 97, 116, 101, 33, 70, 111, 111, 98, 97, 114 };
		
		TrapMessageImpl m = new TrapMessageImpl();
		m.setOp(Operation.CHALLENGE);
		m.setAuthData(authMessage);
		m.setData(messageData);
		m.setFormat(Format.REGULAR);
		m.setChannel(1);
		
		byte[] data = m.serialize();
		
		Assert.assertArrayEquals(expected, data);
	}
	
	@Test
	public void test8BitSerializeCompressed() throws Exception
	{
		String authMessage = "Authenticate!";
		byte[] messageData = "Foobar".getBytes();
		byte[] expected = new byte[] { -123, -128, 0, 13, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 14, 65, 117, 116, 104, 101, 110, 116, 105, 99, 97, 116, 101, 33, 120, -100, 115, -53, -49, 79, 74, 44, 2, 0, 7, -21, 2, 90 };

		TrapMessageImpl m = new TrapMessageImpl();
		m.setOp(Operation.CHALLENGE);
		m.setAuthData(authMessage);
		m.setData(messageData);
		m.setFormat(Format.REGULAR);
		m.setCompressed(true);
		m.setChannel(1);
		
		byte[] data = m.serialize();
		
		Assert.assertArrayEquals(expected, data);
	}
	
	@Test
	public void test8BitDeserialize() throws Exception
	{
		String authMessage = "Authenticate!";
		byte[] messageData = "Foobar".getBytes();
		byte[] data = new byte[] { -123, 0, 0, 13, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 65, 117, 116, 104, 101, 110, 116, 105, 99, 97, 116, 101, 33, 70, 111, 111, 98, 97, 114 };
		
		TrapMessageImpl n = new TrapMessageImpl(data);
		
		Assert.assertArrayEquals(messageData, n.getData());
		Assert.assertEquals(authMessage, n.getAuthData());
		Assert.assertEquals(Format.REGULAR, n.getFormat());
		Assert.assertEquals(Operation.CHALLENGE, n.getOp());
	}
	
	@Test
	public void test8BitDeserializeCompressed() throws Exception
	{
		String authMessage = "Authenticate!";
		byte[] messageData = "Foobar".getBytes();
		byte[] data = new byte[] { -123, -127, 0, 13, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 14, 65, 117, 116, 104, 101, 110, 116, 105, 99, 97, 116, 101, 33, 120, -100, 115, -53, -49, 79, 74, 44, 2, 0, 7, -21, 2, 90 };
		
		TrapMessageImpl n = new TrapMessageImpl(data);
		
		Assert.assertArrayEquals(messageData, n.getData());
		Assert.assertEquals(authMessage, n.getAuthData());
		Assert.assertEquals(Format.REGULAR, n.getFormat());
		Assert.assertEquals(Operation.CHALLENGE, n.getOp());
	}
	
	@Test
	public void testNullAuth() throws Exception
	{
		TrapMessage m = new TrapMessageImpl();
		m.setAuthData(null);
		
		m = new TrapMessageImpl(m.serialize());
		
		Assert.assertNull(m.getAuthData());
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testErroneousDeserialisation() throws Exception
	{
		TrapMessageImpl m = new TrapMessageImpl();
		m.setAuthData("HELLO");
		
		byte[] bs = m.serialize();
		
		// Failure due to insufficient data
		TrapMessageImpl m2 = new TrapMessageImpl();
		int rv = m2.deserialize(bs, 0, 14);
		
		Assert.assertEquals(rv, -1);
		
		// Failure due to insufficient header
		m2 = new TrapMessageImpl();
		rv = m2.deserialize(bs, 0, 11);
		
		Assert.assertEquals(rv, -1);
		
		// Failure due to insufficient byte buffer
		m2 = new TrapMessageImpl();
		rv = m2.deserialize(bs, bs.length - 5, 11);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testLongAuthString()
	{
		StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i < 100000; i++)
			sb.append((char) i);
		new TrapMessageImpl().setAuthData(sb.toString());
	}
}
