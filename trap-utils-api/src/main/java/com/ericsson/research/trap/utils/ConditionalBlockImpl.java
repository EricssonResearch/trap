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

import java.util.Arrays;

/**
 * Blocks execution until unblocked. If unblocked, never blocks.
 *
 * @author Vladimir Katardjiev
 * @hide
 * @since 1.0
 */
class ConditionalBlockImpl extends ConditionalBlock
{
	private final String	name;
	private long	timeout	= 10000;

	private boolean	unlocked;
	private byte[]	condition;

	public ConditionalBlockImpl(String name)
	{
		this.name = name;
	}

	public ConditionalBlockImpl(String name, long timeout)
	{
		this(name);
		this.timeout = timeout;
	}

	public synchronized void block(byte[] condition)
	{
		if (this.condition == null)
			this.condition = condition;
		else if (!Arrays.equals(condition, this.condition))
			throw new IllegalArgumentException("Tried to lock a blocked block with a different condition. Bad idea.");

		if (this.unlocked)
			return;
		try
		{
			long startTime = System.currentTimeMillis();
			long endTime = startTime + this.timeout;

			while (!this.unlocked)
			{
				long waitTime = endTime - System.currentTimeMillis();
				if (waitTime <= 0)
					throw new RuntimeException("Timeout during " + this.name);
				this.wait(waitTime);
			}
		}
		catch (InterruptedException e)
		{
		}
		if (!this.unlocked)
			throw new RuntimeException("Timeout during " + this.name);
	}

	public synchronized boolean unblock(byte[] condition)
	{
		// Do not unblock if incorrect
		if (!Arrays.equals(condition, this.condition))
			return false;

		this.doUnblock();

		return true;

	}

	public synchronized void reset()
	{
		this.doUnblock();
		this.unlocked = false;
	}

	private synchronized void doUnblock()
	{
		this.unlocked = true;
		this.notifyAll();
	}

	public long getTimeout()
	{
		return this.timeout;
	}

	public void setTimeout(long timeout)
	{
		this.timeout = timeout;
	}

	public static ConditionalBlockImpl create(String string)
	{
		return new ConditionalBlockImpl(string);
	}

	public synchronized void setCondition(byte[] condition)
	{
		this.condition = condition;
	}
}
