package com.ericsson.research.trap.nhttpd;

import java.io.InputStream;
import java.util.Map;

/**
 * Handles one session, i.e. parses the HTTP request and returns the response.
 */
public interface Request
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

	InputStream getInputStream();

}