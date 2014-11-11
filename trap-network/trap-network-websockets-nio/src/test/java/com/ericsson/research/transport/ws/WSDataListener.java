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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class WSDataListener implements WSListener {
	
	protected WSInterface socket;
	private boolean open = false;
	private final List<String> strings = Collections.synchronizedList(new ArrayList<String>());
	private final List<byte[]> datas =  Collections.synchronizedList(new ArrayList<byte[]>());
	private final List<byte[]> pongs =  Collections.synchronizedList(new ArrayList<byte[]>());
	
	public synchronized void notifyOpen(WSInterface socket) {
		this.socket = socket;
		open  = true;
		notifyAll();
	}
	
	public synchronized void waitForOpen(long timeout) throws TimeoutException {
		long expiry = System.currentTimeMillis() + timeout;
		while(!open) {
			long waitTime = expiry - System.currentTimeMillis();
			if (waitTime <= 0 && !open)
				throw new TimeoutException();
			try {
				wait(waitTime);
			} catch (InterruptedException e) {}
		}
	}
	
	public synchronized void notifyMessage(byte[] data) {
		datas.add(data);
		notifyAll();
	}

	public synchronized byte[] waitForBytes(long timeout) throws TimeoutException {
		long expiry = System.currentTimeMillis() + timeout;
		while(datas.isEmpty()) {
			long waitTime = expiry - System.currentTimeMillis();
			if (waitTime <= 0)
				throw new TimeoutException();
			try {
				wait(waitTime);
			} catch (InterruptedException e) {}
		}
		return datas.remove(0);
	}
	
	public synchronized void notifyMessage(String string) {
		strings.add(string);
		notifyAll();
	}
	
	public synchronized String waitForString(long timeout) throws TimeoutException {
		long expiry = System.currentTimeMillis() + timeout;
		while(strings.isEmpty()) {
			long waitTime = expiry - System.currentTimeMillis();
			if (waitTime <= 0)
				throw new TimeoutException();
			try {
				wait(waitTime);
			} catch (InterruptedException e) {}
		}
		return strings.remove(0);
	}
	
	public synchronized void notifyClose() {
		open = false;
		notifyAll();
	}
	
	public synchronized void notifyError(Throwable t) {
		t.printStackTrace();
		open = false;
		notifyAll();
	}
	
	public synchronized void waitForClose(long timeout) throws TimeoutException {
		long expiry = System.currentTimeMillis() + timeout;
		while(open) {
			long waitTime = expiry - System.currentTimeMillis();
			if (waitTime <= 0)
				throw new TimeoutException();
			try {
				wait(waitTime);
			} catch (InterruptedException e) {}
		}
	}
	
	public void notifyPong(byte[] payload) {
		pongs.add(payload);
		notifyAll();
	}

	public synchronized byte[] waitForPong(long timeout) throws TimeoutException {
		long expiry = System.currentTimeMillis() + timeout;
		while(pongs.isEmpty()) {
			long waitTime = expiry - System.currentTimeMillis();
			if (waitTime <= 0)
				throw new TimeoutException();
			try {
				wait(waitTime);
			} catch (InterruptedException e) {}
		}
		return pongs.remove(0);
	}
	
}