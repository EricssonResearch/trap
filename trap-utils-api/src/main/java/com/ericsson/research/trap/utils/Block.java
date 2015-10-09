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
 * Blocks execution until unblocked. If unblocked, never blocks. A block is a
 * simple synchronization tool in which one thread can "block" on this object.
 * Another thread will unblock it. It is similar to Object.wait, except spurious
 * unlocks are handled by the block.
 *
 * @author Leonid Mokrushin
 */
public abstract class Block
{

	/**
	 * Blocks execution until unblock is called. If unblock was already called,
	 * merely flips the state of the block.
	 */
	public abstract void block();

	/**
	 * Unblocks a blocked thread.
	 */
	public abstract void unblock();

	/**
	 * Resets the block back to ready-to-block. Unblocks any existing blocked
	 * threads.
	 */
	public abstract void reset();

	/**
	 * Creates a block with a given name. A block is a java.lang.Object-based
	 * implementation of Lock.
	 *
	 * @param name
	 *            The name of the block
	 * @return A new Block instance.
	 */
	public static Block create(String name)
	{
		try
		{
			Class<?> c = Class.forName(Block.class.getName() + "Impl");
			return (Block) c.getConstructor(String.class).newInstance(name );
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates a block with a given name. A block is a java.lang.Object-based
	 * implementation of Lock.
	 *
	 * @param name
	 *            The name of the block
	 * @param timeout
	 *            The number of milliseconds a block can wait to block.
	 * @return A new Block instance.
	 */
	public static Block create(String name, long timeout)
	{
		try
		{
			Class<?> c = Class.forName(Block.class.getName() + "Impl");
			return (Block) c.getConstructor(String.class, Long.class).newInstance(name, timeout);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	// Prevent javadoc
	Block()
	{
	}
}
