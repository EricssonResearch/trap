package com.ericsson.research.trap.nhttpd;

import java.io.InputStream;

/**
 * General Response interface, used to build a response.
 * 
 * @author Vladimir Katardjiev
 *
 */
public interface Response
{

	/**
	 * Adds given line to the header.
	 */
	public abstract Response addHeader(String name, String value);

	/**
	 * Gets the header with the given name
	 * 
	 * @param name
	 *            The header name
	 * @return The header value
	 */
	public abstract String getHeader(String name);

	/**
	 * Accessor for the status code
	 * 
	 * @return The current status code
	 */
	public abstract int getStatus();

	/**
	 * Sets a new status code
	 * 
	 * @param status
	 *            The new status code
	 */
	public abstract Response setStatus(int status);

	/**
	 * Gets the current mime type
	 * 
	 * @return The current mime type
	 */
	public abstract String getMimeType();

	/**
	 * Sets the mime type
	 * 
	 * @param mimeType
	 * @return
	 */
	public abstract Response setMimeType(String mimeType);

	/**
	 * Sets the given InputStream as the payload for the Response. An
	 * InputStream can be used as a chunked encoding source.
	 * 
	 * @param data
	 * @return
	 */
	public abstract Response setData(InputStream data);

	/**
	 * Enables/disables chunked transfer encoding. Note that if given an
	 * InputStream and no Content-Length, it will default to true.
	 * 
	 * @param chunkedTransfer
	 * @return
	 */
	public abstract Response setChunkedTransfer(boolean chunkedTransfer);

	/**
	 * Sets a byte array as a data source. This will default to Content-Length
	 * encoding.
	 * 
	 * @param data
	 * @return
	 */
	public abstract Response setData(byte[] data);

	/**
	 * Sets a String as a data source. This will default to Content-Length
	 * encoding, and UTF-8.
	 * 
	 * @param data
	 * @return
	 */
	public abstract Response setData(String data);

	/**
	 * Notifies the processor that this response is going to be asynchronous.
	 * The response MUST be paired with a corresponding
	 * {@link #sendAsyncResponse()}. <b>This method will only change the
	 * asynchronous mode until the serve() call has finished</b>
	 * 
	 * @param async
	 *            <i>true</i> to enable async mode, <i>false</i> to disable.
	 * @return
	 */
	public abstract Response setAsync(boolean async);

	/**
	 * Sends an asynchronous Response.
	 */
	public abstract void sendAsyncResponse();

	/**
	 * Accessor for the asynchronous state of a Request.
	 * 
	 * @return
	 */
	public abstract boolean getAsync();

}