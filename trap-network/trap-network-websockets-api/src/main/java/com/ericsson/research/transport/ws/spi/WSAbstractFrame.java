package com.ericsson.research.transport.ws.spi;

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
import java.io.OutputStream;

import com.ericsson.research.transport.ws.WSException;


public abstract class WSAbstractFrame implements WSConstants {

	public static final byte CONTINUATION_FRAME = 0;
	public static final byte TEXT_FRAME = 1;
	public static final byte BINARY_FRAME = 2;
	public static final byte CLOSE_FRAME = 8;
	public static final byte PING_FRAME = 9;
	public static final byte PONG_FRAME = 10;
	
	protected byte type;
	protected byte[] payload;
	
	protected int pos = 0;
	protected int len = -1;
	protected byte l1 = 0;
	
	WSAbstractFrame() {}
			
	WSAbstractFrame(byte type) {
		this.type = (byte)(type & 0x0F);
	}
	
	public byte getType() {
		return type;
	}

	public byte[] getPayload() {
		return payload;
	}
	
	public void serialize(OutputStream os) throws IOException {
		synchronized(os) {
			os.write(payload);
			os.flush();
		}
	}
	
	public abstract int deserialize(byte[] data, int length) throws WSException;

}
