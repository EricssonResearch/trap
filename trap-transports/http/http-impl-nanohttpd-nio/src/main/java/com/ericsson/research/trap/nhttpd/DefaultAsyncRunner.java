package com.ericsson.research.trap.nhttpd;

import com.ericsson.research.trap.nhttpd.NanoHTTPD.AsyncRunner;
import com.ericsson.research.trap.utils.ThreadPool;

/**
 * Default threading strategy for NanoHttpd.
 * <p/>
 * <p>By default, the server spawns a new Thread for every incoming request.  These are set
 * to <i>daemon</i> status, and named according to the request number.  The name is
 * useful when profiling the application.</p>
 */
public class DefaultAsyncRunner implements AsyncRunner {

    @Override
    public void exec(Runnable code) {
        ThreadPool.executeCached(code);
    }
}