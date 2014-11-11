package com.ericsson.research.transport.ws;

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

import com.ericsson.research.transport.ws.spi.util.PrettyPrinter;

class WSPrintingListener implements WSListener {
	
	private final String name;
	WSInterface socket;

	public WSPrintingListener(String name) {
		this.name = name;
	}
	
	public void notifyOpen(WSInterface socket) {
		System.out.println("["+name+"] notifyOpen - "+socket);
		this.socket = socket;
	}
	
	public void notifyMessage(byte[] data) {
		System.out.println("["+name+"] byte[] - "+PrettyPrinter.toHexString(data, data.length));
	}
	
	public void notifyMessage(String string) {
		System.out.println("["+name+"] String - "+string);
	}
	
	public void notifyClose() {
		System.out.println("["+name+"] notifyClose");
	}

	public void notifyPong(byte[] payload) {
		System.out.println("["+name+"] notifyPong - "+PrettyPrinter.toHexString(payload, payload.length));
	}

	public void notifyError(Throwable t) {
		System.out.println("["+name+"] notifyError - "+t.getMessage());
	}
	
}