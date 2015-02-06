package com.ericsson.research.trap.spi.nhttp;

import com.ericsson.research.trap.nhttpd.Response;

import com.ericsson.research.trap.nhttpd.IHTTPSession;

public interface FullRequestHandler
{
    public void handle(IHTTPSession request, Response response);
}
