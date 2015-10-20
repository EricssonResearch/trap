package com.ericsson.research.trap.spi.tomcat;

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
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import org.apache.catalina.websocket.StreamInbound;
import org.apache.catalina.websocket.WsOutbound;

public class WSSocket extends StreamInbound
{
	
	public interface Delegate
	{
		public void notifyError();
		public void notifyOpen();
		public void notifyClose();
		public void notifyMessage(String string);
		public void notifyMessage(byte[] data);
	}
	
	char[] cb = new char[512];
	byte[] b = new byte[512];
	Delegate d;
	private WsOutbound	out;
	
	public WSSocket(Delegate d)
	{
		this.d = d;
	}

	@Override
	protected void onBinaryData(InputStream is) throws IOException
	{
		int read = 0;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		while((read = is.read(this.b)) > -1)
			bos.write(this.b, 0, read);
		
		this.d.notifyMessage(bos.toByteArray());
		
	}

	@Override
	protected void onTextData(Reader r) throws IOException
	{
		int read = 0;
		StringWriter w = new StringWriter();
		
		while((read = r.read(this.cb)) > -1)
			w.write(this.cb, 0, read);
		
		this.d.notifyMessage(w.toString());
		
	}
	
	public void send(ByteBuffer b) throws IOException
	{
		this.out.writeBinaryMessage(b);
	}
	
	public void send(String s) throws IOException
	{
		this.out.writeTextMessage(CharBuffer.wrap(s));
	}

	@Override
	public void onOpen(WsOutbound out)
	{
		this.out = out;
		this.d.notifyOpen();
	}

	@Override
	protected void onClose(int status)
	{
		super.onClose(status);
		this.d.notifyClose();
	}
	
	public void close() throws IOException
	{
		this.out.close(0, null);
	}
	
}
