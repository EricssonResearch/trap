package com.ericsson.research.trap.nhttpd;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.ericsson.research.trap.nhttpd.NanoHTTPD.Method;
import com.ericsson.research.trap.nio.Socket;
import com.ericsson.research.trap.nio.Socket.SocketHandler;

/**
 * Handles one session, i.e. parses the HTTP request and returns the response.
 */
public interface IHTTPSession
{
    Map<String, String> getParms();
    
    Map<String, String> getHeaders();
    
    /**
     * @return the path part of the URL.
     */
    String getUri();
    
    String getQueryParameterString();
    
    Method getMethod();
    
    CookieHandler getCookies();
    
    /**
     * Adds the files in the request body to the files map.
     * 
     * @arg files - map to modify
     */
    void parseBody(Map<String, String> files) throws IOException, ResponseException;
    
    Socket getSocket();

    InputStream getInputStream();
    
    void upgrade(SocketHandler handler);

	void finish(Response r);
}