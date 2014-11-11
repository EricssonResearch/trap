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
 * Provides a set of thread pools, supplying implementations for JVMs that do
 * not ship with Thread Pools. The number of pools are limited to three: a
 * cached, a fixed and a scheduled.
 *
 * @author Vladimir Katardjiev
 */
public abstract class ThreadPool
{
	private static ThreadPool	instance;

	static
	{
		try
		{
			Class<?> c = Class.forName(ThreadPool.class.getName() + "Impl");
			instance = (ThreadPool) c.newInstance();
		}
		catch (Throwable t)
		{
			System.err.println("Could not initialise ThreadPool Impl");
		}
	}

	/**
	 * Schedules a task to be executed after a certain delay. The execution
	 * takes place in a thread pool for timed tasks, allowing several tasks to
	 * perform concurrently (thus better individual performance than a Timer).
	 *
	 * @param task
	 *            The task to schedule
	 * @param delay
	 *            The delay (in milliseconds) to wait before the task should
	 *            execute. This delay is a lower bound, not an upper bound.
	 * @return A {@link Future} instance that can be used to cancel the task
	 */
	public static Future executeAfter(Runnable task, long delay)
	{
		return instance.performSchedule(task, delay);
	}

	abstract Future performSchedule(Runnable task, long delay);

	/**
	 * Schedules a task to be executed after a certain delay, weakly referencing
	 * the task. Should the Future returned by this function be garbage
	 * collected, the task will be garbage collected. Otherwise acts as
	 * {@link #executeAfter(Runnable, long)}.
	 * <p>
	 * The advantage of this method is significantly reduced memory footprint
	 * for cancelled delayed tasks (though the footprint is NOT eliminated).
	 *
	 * @param task
	 *            The task to execute. The runnable will only be weakly
	 *            referenced by the thread pool.
	 * @param delay
	 *            The delay after which to execute.
	 * @return A Future representing the task and its eventual result. The
	 *         Future holds the only strong reference to the task in the queue
	 *         and MUST be retained until the task has executed.
	 */
	public static Future weakExecuteAfter(Runnable task, long delay)
	{
		return instance.performWeakExecuteAfter(task, delay);
	}

	abstract Future performWeakExecuteAfter(Runnable task, long delay);

	/**
	 * Schedules a task to be executed at a given time. The execution performs
	 * in a pooled environment, so it may be delayed slightly if necessary
	 *
	 * @param task
	 *            The task to run
	 * @param timestamp
	 *            A UNIX timestamp on when to run it
	 * @return
	 */
	public static Future executeAt(Runnable task, long timestamp)
	{
		long delay = timestamp - System.currentTimeMillis();

		if (delay < 0)
			delay = 0;

		return executeAfter(task, delay);
	}

	/**
	 * Executes a task in the fixed size thread pool. Suitable for tasks that
	 * need to complete on a best-effort basis, in a separate thread.
	 *
	 * @param task
	 *            The task to execute.
	 */
	public static void executeFixed(Runnable task)
	{
		instance.performExecuteFixed(task);
	}

	abstract void performExecuteFixed(Runnable task);

	/**
	 * Executes a task in a cached thread pool, reusing a thread if available
	 * and creating a new one otherwise. Useful for tasks that need to split off
	 * in a thread immediately, and execute for an extended period of time.
	 *
	 * @param task
	 *            The task to execute.
	 */
	public static void executeCached(Runnable task)
	{
		instance.performExecuteCached(task);
	}

	abstract void performExecuteCached(Runnable task);

	ThreadPool()
	{
	}

}
