package com.ericsson.research.trap.nhttpd;

import com.ericsson.research.trap.nhttpd.NanoHTTPD.AsyncRunner;

/**
 * Default threading strategy for NanoHttpd.
 * <p/>
 * <p>By default, the server spawns a new Thread for every incoming request.  These are set
 * to <i>daemon</i> status, and named according to the request number.  The name is
 * useful when profiling the application.</p>
 */
public class DefaultAsyncRunner implements AsyncRunner {
    private long requestCount;

    @Override
    public void exec(Runnable code) {
        ++requestCount;
        Thread t = new Thread(code);
        t.setDaemon(true);
        t.setName("NanoHttpd Request Processor (#" + requestCount + ")");
        t.start();
    }
}