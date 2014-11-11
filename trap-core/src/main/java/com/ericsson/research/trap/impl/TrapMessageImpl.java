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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

import com.ericsson.research.trap.spi.TrapConstants;
import com.ericsson.research.trap.spi.TrapMessage;
import com.ericsson.research.trap.utils.StringUtil;

public class TrapMessageImpl implements TrapMessage
{
	
	protected byte[]		data			= new byte[] {};
	protected String		authString		= null;
	protected Format		format			= TrapConstants.MESSAGE_FORMAT_DEFAULT;
	protected Operation		op				= Operation.OK;
	private int				messageId		= 0;
	private boolean			compressed		= false;
	private int				channelId		= 0;
	
	public static final int	HEADER_SIZE		= 16;
	
	public TrapMessageImpl()
	{
	}
	
	public TrapMessageImpl(byte[] data) throws UnsupportedEncodingException
	{
		this.deserialize(data, 0, data.length);
	}
	
	/* (non-Javadoc)
	 * @see com.ericsson.research.trap.impl.TrapMessage#serialize()
	 */
	
	public byte[] serialize() throws IOException
	{
		// Make sure the data we have is serialized.
		// Yeah, I know this looks really odd
		
		if (this.format == Format.SEVEN_BIT_SAFE)
		{
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			this.serialize7bit(bos);
			return bos.toByteArray();
		}
		else
			return this.serialize8bit();
		
	}
	
	protected byte[] serialize8bit() throws UnsupportedEncodingException, IOException
	{
		
		// Make 8-bit assertions
		if (this.data.length >= Math.pow(2, 32))
			throw new IllegalStateException("Asked to serialize more than 2^32 bytes data into a 8-bit Trap message");
		
		byte b1 = 0, b2 = 0;
		
		byte[] mData = this.getCompressedData();
		
		// First byte: |1|0| MESSAGEOP |
		b1 |= this.op.getOp() | 0x80;
		
		// Second byte: compressed | RSV1
		b2 = 0;
		if (this.compressed)
			b2 |= 0x80;
		
		int authLen = (this.authString != null ? this.authString.length() : 0);
		
		ByteBuffer buf = ByteBuffer.allocate(HEADER_SIZE + authLen + mData.length);
		buf.put(b1);
		buf.put(b2);
		
		// Bytes 3-4 : AuthLen
		buf.putShort((short) authLen);
		
		// Bytes 5-8: MessageID
		buf.putInt(this.getMessageId());
		
		// Byte 9: RSV2
		buf.put((byte) 0);
		
		// Byte 10: ChannelID
		buf.put((byte) this.channelId);
		
		// Byte 11-12: RSV3
		buf.putShort((short) 0);
		
		buf.putInt(mData.length);
		
		if (authLen > 0)
			buf.put(this.authString.getBytes("UTF-8"));
		
		buf.put(mData);
		
		/*// Third byte: Bits 3 - 9 of authLen
		bos.write((authLen >>> 8) & 0xFF);
		
		// Fourth byte: Bits 10 - 16 of authLen
		bos.write(authLen & 0xFF);
		
		// Transport ID!
		this.writeInt8(this.getMessageId(), bos, true);
		this.writeInt8(this.transportId, bos, true);
		this.writeInt8(mData.length, bos, false);
		
		if (authLen > 0)
			bos.write(this.authString.getBytes("UTF-8"));
		
		bos.write(mData);*/
		
		return buf.array();
	}
	
	protected void serialize7bit(ByteArrayOutputStream bos) throws IOException
	{
		
		// Make 7-bit assertions
		if (this.data.length >= Math.pow(2, 28))
			throw new IllegalStateException("Asked to serialize more than 2^28 bytes data into a 7-bit Trap message");
		
		byte b = 0;
		
		// First byte: |0|0| MESSAGEOP |
		b |= this.op.getOp();
		bos.write(b);
		
		int authLen = (this.authString != null ? this.authString.length() : 0);
		
		// Second byte: First two bits of authLen
		// Compatibility note: 7-bit mode does not gain channels/compression.
		bos.write(this.getBits(authLen, 17, 18));
		
		// Third byte: Bits 3 - 9 of authLen
		bos.write(this.getBits(authLen, 19, 25));
		
		// Fourth byte: Bits 10 - 16 of authLen
		bos.write(this.getBits(authLen, 26, 32));
		
		// Transport ID!
		this.writeInt7(this.getMessageId(), bos, true);
		this.writeInt7(0, bos, true);
		this.writeInt7(this.data.length, bos, false);
		
		if (authLen > 0)
			bos.write(this.authString.getBytes("UTF-8"));
		
		bos.write(this.data);
	}
	
	private void writeInt7(int src, ByteArrayOutputStream bos, boolean signed)
	{
		bos.write(this.getBits(src, 5, 11));
		bos.write(this.getBits(src, 12, 18));
		bos.write(this.getBits(src, 19, 25));
		bos.write(this.getBits(src, 26, 32));
	}
	
	/* (non-Javadoc)
	 * @see com.ericsson.research.trap.impl.TrapMessage#deserialize(byte[])
	 */
	
	public int deserialize(byte[] rawData, int offset, int length) throws UnsupportedEncodingException
	{
		
		if ((offset + length) > rawData.length)
			throw new IllegalArgumentException("Offset and length specified exceed the buffer");
		
		if (length < 16)
			return -1;
		
		int authLen;
		int contentLen;
		
		if ((rawData[offset + 0] & 0x80) != 0)
		{
			// 8-bit
			this.format = Format.REGULAR;
			this.op = Operation.getType(rawData[offset + 0] & 0x3F);
			this.compressed = (rawData[offset + 1] & 0x80) != 0;
			
			/*
			 * Byte values are sign extended to 32 bits before any any bitwise operations are performed on the value. Thus, if b[0] contains the value 0xff, and x is initially 0, then the code ((x << 8) | b[0]) will sign extend 0xff to get 0xffffffff, and thus give the value 0xffffffff as the result.

				In particular, the following code for packing a byte array into an int is badly wrong:

			int result = 0;
			for(int i = 0; i < 4; i++)
			result = ((result << 8) | b[i]);

			The following idiom will work instead:

			int result = 0;
			for(int i = 0; i < 4; i++)
			result = ((result << 8) | (b[i] & 0xff));
			 */
			
			/*
			authLen = ((rawData[offset + 2] & 0xFF) << 8) | (rawData[offset + 3] & 0xFF);
			this.messageId = ((rawData[offset + 4] & 0xFF) << 24) | ((rawData[offset + 5] & 0xFF) << 16) | ((rawData[offset + 6] & 0xFF) << 8) | (rawData[offset + 7] & 0xFF);
			this.transportId = ((rawData[offset + 8] & 0xFF) << 24) | ((rawData[offset + 9] & 0xFF) << 16) | ((rawData[offset + 10] & 0xFF) << 8) | (rawData[offset + 11] & 0xFF);
			contentLen = ((rawData[offset + 12] & 0xFF) << 24) | ((rawData[offset + 13] & 0xFF) << 16) | ((rawData[offset + 14] & 0xFF) << 8) | (rawData[offset + 15] & 0xFF);
			*/
			
			ByteBuffer hs = ByteBuffer.wrap(rawData, offset + 2, TrapMessageImpl.HEADER_SIZE - 2);
			authLen = hs.getShort() & 0xFFFF;
			this.messageId = hs.getInt();
			
			// Skip RSV2 (8 bits)
			hs.get();
			this.channelId = hs.get();
			
			// Skip RSV3 (16 bits)
			hs.getShort();
			
			contentLen = hs.getInt();
			//throw new IllegalArgumentException();
			
		}
		else
		{
			// 7-bit
			this.format = Format.SEVEN_BIT_SAFE;
			this.op = Operation.getType(rawData[offset + 0] & 0x3F);
			
			authLen = ((rawData[offset + 1] & 0x03) << 14) | ((rawData[offset + 2] & 0x7F) << 7) | ((rawData[offset + 3] & 0x7F) << 0);
			this.messageId = ((rawData[offset + 4] & 0x7F) << 21) | ((rawData[offset + 5] & 0x7F) << 14) | ((rawData[offset + 6] & 0x7F) << 7) | ((rawData[offset + 7] & 0x7F) << 0);
			//this.transportId = ((rawData[offset + 8] & 0x7F) << 21) | ((rawData[offset + 9] & 0x7F) << 14) | ((rawData[offset + 10] & 0x7F) << 7) | ((rawData[offset + 11] & 0x7F) << 0);
			
			contentLen = ((rawData[offset + 12] & 0x7F) << 21) | ((rawData[offset + 13] & 0x7F) << 14) | ((rawData[offset + 14] & 0x7F) << 7) | ((rawData[offset + 15] & 0x7F) << 0);
			
			this.compressed = false;
			this.channelId = 0;
		}
		
		// Verify that there's enough remaining content to read the message.
		int messageSize = 16 + authLen + contentLen;
		
		if (length < messageSize)
			return -1; // Cannot successfully read the remaining values.
			
		// Range of authHeader = (16, authLen)
		int startByte = offset + 16;
		
		// We have an authentication header!
		if (authLen > 0)
		{
			
			// Make a new string of the appropriate bytes
			this.authString = new String(rawData, startByte, authLen, Charset.forName("UTF-8"));
			startByte += authLen;
		}
		else
		{
			this.authString = null;
		}
		
		if (!this.compressed)
		{
			// Copy the data
			//Arrays.copyOfRange(rawData, startByte, startByte+contentLen);
			this.data = new byte[contentLen];
			System.arraycopy(rawData, startByte, this.data, 0, contentLen);
		}
		else
		{
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			InflaterOutputStream ios = new InflaterOutputStream(bos);
			
			try
			{
				ios.write(rawData, startByte, contentLen);
				ios.flush();
				this.data = bos.toByteArray();
				
				ios.close();
				bos.close();
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		// The number of bytes consumed. This allows multiple messages to be parsed from the same data block.
		return messageSize;
	}
	
	private int getBits(int src, int startBit, int endBit)
	{
		int mask = (int) (Math.pow(2, (endBit - startBit) + 1) - 1);
		mask = mask << (32 - endBit);
		int rv = (src & mask) >> (32 - endBit);
		return rv;
	}
	
	/* (non-Javadoc)
	 * @see com.ericsson.research.trap.impl.TrapMessage#getData()
	 */
	
	public byte[] getData()
	{
		return this.data;
	}
	
	/* (non-Javadoc)
	 * @see com.ericsson.research.trap.impl.TrapMessage#setData(byte[])
	 */
	
	public TrapMessage setData(byte[] data)
	{
		this.data = data;
		return this;
	}
	
	/* (non-Javadoc)
	 * @see com.ericsson.research.trap.impl.TrapMessage#getAuthData()
	 */
	
	public String getAuthData()
	{
		return this.authString;
	}
	
	/* (non-Javadoc)
	 * @see com.ericsson.research.trap.impl.TrapMessage#setAuthData(java.lang.String)
	 */
	
	public TrapMessage setAuthData(String authData)
	{
		if ((authData != null) && (authData.length() > 65535))
			throw new IllegalArgumentException("Cannot have an AuthString more than 65535 bytes");
		
		if (authData == null || authData.length() == 0)
			this.authString = null;
		else
			this.authString = authData;
		
		return this;
	}
	
	/* (non-Javadoc)
	 * @see com.ericsson.research.trap.impl.TrapMessage#getFormat()
	 */
	
	public Format getFormat()
	{
		return this.format;
	}
	
	/* (non-Javadoc)
	 * @see com.ericsson.research.trap.impl.TrapMessage#setFormat(com.ericsson.research.trap.impl.TrapMessageImpl.Format)
	 */
	
	public TrapMessage setFormat(Format format)
	{
		this.format = format;
		return this;
	}
	
	/* (non-Javadoc)
	 * @see com.ericsson.research.trap.impl.TrapMessage#getOp()
	 */
	
	public Operation getOp()
	{
		return this.op;
	}
	
	/* (non-Javadoc)
	 * @see com.ericsson.research.trap.impl.TrapMessage#setOp(com.ericsson.research.trap.impl.TrapMessageImpl.Operation)
	 */
	
	public TrapMessage setOp(Operation op)
	{
		this.op = op;
		return this;
	}
	
	public int getMessageId()
	{
		return this.messageId;
	}
	
	public TrapMessage setMessageId(int messageId)
	{
		this.messageId = messageId;
		return this;
	}
	
	public long length()
	{
		int l = HEADER_SIZE;
		if (this.authString != null)
			l += StringUtil.toUtfBytes(this.authString).length;
		
		if (this.getCompressedData() != null)
			l += this.getCompressedData().length;
		return l;
	}
	
	public String toString()
	{
		return this.getOp() + "/C" + this.getChannel() + "/" + this.getMessageId() + (this.data != null ? "/" + this.data.length : "");
	}
	
	public TrapMessage setCompressed(boolean isCompressed)
	{
		this.compressed = isCompressed;
		return this;
	}
	
	public boolean isCompressed()
	{
		return this.compressed;
	}
	
	public TrapMessage setChannel(int channelID)
	{
		if (channelID < 0 || channelID > 63)
			throw new IllegalArgumentException("Channel ID " + channelID + " outside the allowed range.");
		
		this.channelId = channelID;
		return this;
	}
	
	public int getChannel()
	{
		return this.getFormat() == Format.REGULAR ? this.channelId : 0;
	}
	
	public byte[] getCompressedData()
	{
		
		if (!this.isCompressed())
			return this.getData();
		
		try
		{
			ByteArrayOutputStream tos = new ByteArrayOutputStream();
			DeflaterOutputStream dos = new DeflaterOutputStream(tos);
			
			dos.write(this.getData());
			dos.finish();
			dos.flush();
			
			byte[] mData = tos.toByteArray();
			
			dos.close();
			tos.close();
			dos = null;
			tos = null;
			
			return mData;
		}
		catch (IOException e)
		{
			return this.getData();
		}
	}
}
