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
TestCase('message', {

	test7BitSerialization : function()
	{
		var authMessage = "Authenticate!";
		var messageData = "Foobar";

		// Create a message
		var m = new Trap.Message();
		m.setOp(Trap.Message.Operation.CHALLENGE);
		m.setAuthData(authMessage);
		m.setData(messageData);
		m.setFormat(Trap.Message.Format.SEVEN_BIT_SAFE);

		var data = m.serialize();

		var n = new Trap.Message(data);

		Assert.assertArrayEquals(n.getData(), messageData);
		Assert.assertEquals(n.getAuthData(), authMessage);
	},

	test7BitTwosComplement : function()
	{

		var authMessage = "Authenticate!";
		var messageData = "Foobar";

		// Create a message
		var m = new Trap.Message();
		m.setOp(Trap.Message.Operation.CHALLENGE);
		m.setAuthData(authMessage);
		m.setData(messageData);
		m.setFormat(Trap.Message.Format.SEVEN_BIT_SAFE);

		var data = m.serialize();

		var n = new Trap.Message(data);

		Assert.assertArrayEquals(messageData, n.getData());
		Assert.assertEquals(authMessage, n.getAuthData());
		Assert.assertEquals(m.getFormat(), n.getFormat());
		Assert.assertEquals(m.getOp(), n.getOp());
	},

	// Test 8 bit
	test8BitSerialization : function()
	{
		var authMessage = "Authenticate!";
		var messageData = "Foobar";

		// Create a message
		var m = new Trap.Message();
		m.setOp(Trap.Message.Operation.CHALLENGE);
		m.setAuthData(authMessage);
		m.setData(messageData);
		m.setFormat(Trap.Message.Format.REGULAR);

		var data = m.serialize();

		var n = new Trap.Message(data);

		Assert.assertArrayEquals(messageData, n.getData());
		Assert.assertEquals(authMessage, n.getAuthData());
		Assert.assertEquals(m.getFormat(), n.getFormat());
		Assert.assertEquals(m.getOp(), n.getOp());
	},

	test8BitSerialize : function()
	{
		var authMessage = "Authenticate!";
		var messageData = "Foobar";
		var expected = String.fromCharCode(133, 0, 0, 13, 0, 0, 0, 0, 0, 0, 0, 6, 65, 117, 116, 104, 101, 110, 116, 105, 99, 97, 116, 101, 33, 70, 111, 111, 98, 97, 114);

		var m = new Trap.Message();
		m.setOp(Trap.Message.Operation.CHALLENGE);
		m.setAuthData(authMessage);
		m.setData(messageData);
		m.setFormat(Trap.Message.Format.REGULAR);

		var data = m.serialize();

		Assert.assertArrayEquals(expected, data);
	},

	test8BitDeserialize : function()
	{
		var authMessage = "Authenticate!";
		var messageData = "Foobar";
		var data = String.fromCharCode(133, 0, 0, 13, 0, 0, 0, 0, 0, 0, 0, 6, 65, 117, 116, 104, 101, 110, 116, 105, 99, 97, 116, 101, 33, 70, 111, 111, 98, 97, 114);

		var n = new Trap.Message(data);

		Assert.assertArrayEquals(messageData, n.getData());
		Assert.assertEquals(authMessage, n.getAuthData());
		Assert.assertEquals(Trap.Message.Format.REGULAR, n.getFormat());
		Assert.assertEquals(Trap.Message.Operation.CHALLENGE, n.getOp());
	}, 



	testNullAuth : function()
	{
		var m = new Trap.Message();
		m.setAuthData(null);

		m = new Trap.Message(m.serialize());

		Assert.assertNull(m.getAuthData());
	},

//	@Test(expected = IllegalArgumentException.class)
	testErroneousDeserialisation : function()
	{
		var m = new Trap.Message();
		m.setAuthData("HELLO");

		var bs = m.serialize();

		// Failure due to insufficient data
		var m2 = new Trap.Message();
		var rv = m2.deserialize(bs, 0, 14);

		Assert.assertEquals(rv, -1);

		// Failure due to insufficient header
		m2 = new Trap.Message();
		rv = m2.deserialize(bs, 0, 11);

		Assert.assertEquals(rv, -1);

		try
		{
			// Failure due to insufficient byte buffer
			m2 = new Trap.Message();
			rv = m2.deserialize(bs, bs.length - 5, 11);

			Assert.assertTrue(false);
		}
		catch(e)
		{
			Assert.assertTrue(true);
		}

	},

//	@Test(expected = IllegalArgumentException.class)
	testLongAuthvar : function()
	{
		var sb = new Trap.StringBuffer();

		for (var i = 0; i < 100000; i++)
			sb.append(i);

		try
		{
			new Trap.Message().setAuthData(sb.toString());
			Assert.assertTrue(false);
		}
		catch(e)
		{
			Assert.assertTrue(true);
		}

	},

});
