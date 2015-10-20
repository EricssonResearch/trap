package com.ericsson.research.trap.nhttpd;

import java.io.InputStream;
import java.util.Map;

import com.ericsson.research.trap.nio.Socket;
import com.ericsson.research.trap.nio.Socket.SocketHandler;

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

	Socket getSocket();

	InputStream getInputStream();

	/**
	 * Performs an upgrade. Control of the socket will be transferred to the
	 * given SocketHandler. The socket is returned to the caller. The HTTP
	 * server will discontinue any further interaction with this session. It is
	 * up to the caller to send any applicable replies.
	 * <p>
	 * The optional parameter <i>passHeaders</i> will also send along any bytes
	 * that were transmitted as part of the header handshake.
	 * 
	 * @param handler
	 *            The new SocketHandler
	 * @param passHeaders
	 *            Whether or not to also send the socket handler the data from
	 *            the HTTP headers
	 * @return The raw socket object.
	 */
	Socket upgrade(SocketHandler handler, boolean passHeaders);

}