package com.ericsson.research.trap.nhttpd;

public interface RequestHandler
{
	public void handleRequest(Request request, Response response);
}
