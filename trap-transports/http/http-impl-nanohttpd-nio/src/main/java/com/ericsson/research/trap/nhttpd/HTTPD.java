package com.ericsson.research.trap.nhttpd;

import com.ericsson.research.trap.nhttpd.impl.NanoHTTPDImpl;

/**
 * Trap port of NanoHTTPD. Feature list:
 * 
 * <ul>
 * <li> HTTP 1.1
 * <li> Upgrade (and WebSockets)
 * <li> TLS
 * <li> Asynchronous Operation
 * <li> Java NIO (and NIO2 on later versions)
 * </ul>
 * 
 * This version does not include any implementation of 
 * @author Vladimir Katardjiev
 *
 */
public class HTTPD extends NanoHTTPDImpl
{

	public HTTPD(String hostname, int port)
    {
	    super(hostname, port);
    }

	public HTTPD(int port)
    {
	    super(port);
    }

}
