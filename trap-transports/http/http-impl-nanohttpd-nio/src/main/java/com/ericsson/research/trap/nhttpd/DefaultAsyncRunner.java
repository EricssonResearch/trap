package com.ericsson.research.trap.nhttpd;

import com.ericsson.research.trap.nhttpd.NanoHTTPD.AsyncRunner;
import com.ericsson.research.trap.utils.ThreadPool;

/**
 * Default threading strategy for NanoHttpd.
 * <p/>
 * Integrated with Trap, it will leverage a thread pool to do its business.
 */
public class DefaultAsyncRunner implements AsyncRunner
{

	@Override
	public void exec(Runnable code)
	{
		ThreadPool.executeCached(code);
	}
}