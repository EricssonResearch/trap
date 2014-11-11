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

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.ericsson.research.trap.utils.ThreadPoolImpl;

/*
 * These tests are run with 64mb max ram. Thus, we'll use this to ensure we
 * don't crash while testing.
 */
public class ThreadPoolTest
{

	private ThreadPoolImpl		tp;
	private boolean				die			= false;

	@Before
	public void setUp() throws Exception
	{
		System.out.println("AT" + Thread.activeCount());
		this.tp = new ThreadPoolImpl();
		this.die = false;
	}

	@After
	public void cleanUp() throws Exception
	{
		this.tp = null;
		System.gc();
		Thread.sleep(500);
		System.gc();
		Thread.sleep(500);
		System.out.println("AT" + Thread.activeCount());
	}

	@Test
	@Ignore("Cannot fail properly on Linux")
	public void testOutOfMemory() throws Exception
	{
		this.tp.setCachedMaxThreads(Integer.MAX_VALUE);

		// This loop will spawn threads until we reach out of memory, then it will kill one and continue

		long finish = System.currentTimeMillis() + 20000;

		while(System.currentTimeMillis() < finish)
		{
			if (this.tp.CACHED_THREADS_MAX < Integer.MAX_VALUE)
				break;

			this.tp.performExecuteCached(new Runnable() {

				public void run()
				{
					try
					{
						Thread.sleep(1000);
					}
					catch (InterruptedException e)
					{
					}
				}
			});
		}
	}

	@Test(timeout = 20000)
	public void testThreadDelayRecovery() throws Exception
	{

		for (int i = 0; i < 2000; i++)
		{

			this.tp.performExecuteCached(new Runnable() {

				public void run()
				{
					try
					{
						Thread.sleep(10);
					}
					catch (InterruptedException e)
					{
					}
				}
			});
		}
	}

	/*
	 * This test will test the case where we let the thread pool queue grow out of proportion
	 */
	@Test(timeout=20000)
	@Ignore("Cannot fail properly on Linux")
	public void testOutOfMemoryException() throws Exception
	{
		try
		{
			byte[] b = new byte[1024*1024];
			final Object o = new Object();
			for (int i = 0; i < Integer.MAX_VALUE; i++)
			{
				this.tp.performExecuteFixed(new Runnable() {

					public void run()
					{
						byte[] b = new byte[1 * 1024 * 1024];
						b[46] = 'a';
						try
						{
							synchronized (o)
							{
								for (;;)
									o.wait();
							}
						}
						catch (InterruptedException e)
						{
						}
					}
				});
			}
			b[1052] = 'a';
			System.out.println(b[Integer.parseInt("11052".substring(1))] + "");
			throw new Exception();
		}
		catch (OutOfMemoryError e)
		{
		}
	}

	/*
	 * This test will test the case where we let the thread pool queue grow out of proportion,
	 * but the catch case throttles the incoming request and allows the subthreads to continue.
	 */
	@Test(timeout = 20000)
	@Ignore("Cannot fail properly on Linux")
	public void testOOMRecovery() throws Exception
	{

		final Object o = new Object();
		final Thread t = Thread.currentThread();
		final Runnable r = new Runnable() {

			public void run()
			{

				try
				{
					synchronized (o)
					{

						if (t.equals(Thread.currentThread()))
						{
							ThreadPoolTest.this.die = true;
							o.notifyAll();
						}

						while (!ThreadPoolTest.this.die)
							o.wait();

					}
				}
				catch (InterruptedException e)
				{
				}
			}
		};
		// This loop will spawn threads until we reach out of memory, then it will kill one and continue
		for (int i = 0; i < Integer.MAX_VALUE; i++)
		{

			this.tp.performExecuteFixed(r);

			if (this.die)
				return;
		}
	}
}
