package com.ericsson.research.trap.nhttpd.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import com.ericsson.research.trap.nhttpd.Method;
import com.ericsson.research.trap.nhttpd.Response;
import com.ericsson.research.trap.nhttpd.StatusCodes;
import com.ericsson.research.trap.utils.StringUtil;

/**
 * HTTP response. Return one of these from serve().
 */
public class ResponseImpl implements Response
{
	/**
	 * HTTP status code after processing, e.g. "200 OK", HTTP_OK
	 */
	private int	                status	        = 404;
	/**
	 * MIME type of content, e.g. "text/html"
	 */
	private String	            mimeType;
	/**
	 * Data of the response, may be null.
	 */
	private InputStream	        data;
	/**
	 * Headers for the HTTP response. Use addHeader() to add lines.
	 */
	private Map<String, String>	header	        = new HashMap<String, String>();
	/**
	 * The request method that spawned this response.
	 */
	private Method	            requestMethod;
	/**
	 * Use chunkedTransfer
	 */
	private boolean	            chunkedTransfer	= true;
	private boolean	            async	        = false;
	private byte[]	            responseData;
	private HTTPSession request;

	/**
	 * Uninitialised response
	 */
	public ResponseImpl(HTTPSession request)
	{
		this.request = request;
	}

	/* (non-Javadoc)
	 * @see com.ericsson.research.trap.nhttpd.impl.Response#addHeader(java.lang.String, java.lang.String)
	 */
	@Override
    public ResponseImpl addHeader(String name, String value)
	{
		header.put(name, value);
		return this;
	}

	/* (non-Javadoc)
	 * @see com.ericsson.research.trap.nhttpd.impl.Response#getHeader(java.lang.String)
	 */
	@Override
    public String getHeader(String name)
	{
		return header.get(name);
	}

	/**
	 * Sends given response to the socket.
	 */
	protected void send(OutputStream outputStream)
	{
		String mime = mimeType;
		SimpleDateFormat gmtFrmt = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
		gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));

		try
		{
			if (status == -1)
			{
				throw new Error("sendResponse(): Status can't be null.");
			}
			PrintWriter pw = new PrintWriter(outputStream);
			pw.print("HTTP/1.1 " + StatusCodes.statusText(status) + " \r\n");

			if (mime != null)
			{
				pw.print("Content-Type: " + mime + "\r\n");
			}

			if (header == null || header.get("Date") == null)
			{
				pw.print("Date: " + gmtFrmt.format(new Date()) + "\r\n");
			}

			if (header != null)
			{
				for (String key : header.keySet())
				{
					String value = header.get(key);
					pw.print(key + ": " + value + "\r\n");
				}
			}

			sendConnectionHeaderIfNotAlreadyPresent(pw, header);

			if (requestMethod != Method.HEAD && (chunkedTransfer && responseData == null))
			{
				sendAsChunked(outputStream, pw);
			}
			else
			{
				int pending = responseData != null ? responseData.length : 0;
				sendContentLengthHeaderIfNotAlreadyPresent(pw, header, pending);
				pw.print("\r\n");
				pw.flush();
				sendAsFixedLength(outputStream, pending);
			}
			outputStream.flush();
			NanoHTTPDImpl.safeClose(data);
		}
		catch (IOException ioe)
		{
			// Couldn't write? No can do.
		}
		catch (IllegalStateException ioe)
		{
			// Couldn't write? No can do.
		}
	}

	protected void sendContentLengthHeaderIfNotAlreadyPresent(PrintWriter pw, Map<String, String> header, int size)
	{
		if (!headerAlreadySent(header, "content-length"))
		{
			pw.print("Content-Length: " + size + "\r\n");
		}
	}

	protected void sendConnectionHeaderIfNotAlreadyPresent(PrintWriter pw, Map<String, String> header)
	{
		if (!headerAlreadySent(header, "connection"))
		{
			pw.print("Connection: keep-alive\r\n");
		}
	}

	private boolean headerAlreadySent(Map<String, String> header, String name)
	{
		boolean alreadySent = false;
		for (String headerName : header.keySet())
		{
			alreadySent |= headerName.equalsIgnoreCase(name);
		}
		return alreadySent;
	}

	private void sendAsChunked(OutputStream outputStream, PrintWriter pw) throws IOException
	{

		if (data == null)
		{
			sendContentLengthHeaderIfNotAlreadyPresent(pw, header, 0);
			pw.print("\r\n");
			pw.flush();
			return;
		}

		pw.print("Transfer-Encoding: chunked\r\n");
		pw.print("\r\n");
		pw.flush();
		int BUFFER_SIZE = 16 * 1024;
		byte[] CRLF = "\r\n".getBytes();
		byte[] buff = new byte[BUFFER_SIZE];
		int read;
		while ((read = data.read(buff)) > -1)
		{
			outputStream.write(String.format("%x\r\n", read).getBytes());
			outputStream.write(buff, 0, read);
			outputStream.write(CRLF);
		}
		outputStream.write(String.format("0\r\n\r\n").getBytes());
		outputStream.flush();
	}

	private void sendAsFixedLength(OutputStream outputStream, int pending) throws IOException
	{
		if (requestMethod != Method.HEAD && responseData != null)
		{
			outputStream.write(responseData);
		}
	}

	/* (non-Javadoc)
	 * @see com.ericsson.research.trap.nhttpd.impl.Response#getStatus()
	 */
	@Override
    public int getStatus()
	{
		return status;
	}

	/* (non-Javadoc)
	 * @see com.ericsson.research.trap.nhttpd.impl.Response#setStatus(int)
	 */
	@Override
    public Response setStatus(int status)
	{
		this.status = status;
		return this;
	}

	/* (non-Javadoc)
	 * @see com.ericsson.research.trap.nhttpd.impl.Response#getMimeType()
	 */
	@Override
    public String getMimeType()
	{
		return mimeType;
	}

	/* (non-Javadoc)
	 * @see com.ericsson.research.trap.nhttpd.impl.Response#setMimeType(java.lang.String)
	 */
	@Override
    public Response setMimeType(String mimeType)
	{
		this.mimeType = mimeType;
		return this;
	}

	/* (non-Javadoc)
	 * @see com.ericsson.research.trap.nhttpd.impl.Response#getData()
	 */
    public InputStream getData()
	{
		return data;
	}

	/* (non-Javadoc)
	 * @see com.ericsson.research.trap.nhttpd.impl.Response#setData(java.io.InputStream)
	 */
	@Override
    public Response setData(InputStream data)
	{
		this.data = data;
		return this;
	}

	/* (non-Javadoc)
	 * @see com.ericsson.research.trap.nhttpd.impl.Response#getRequestMethod()
	 */
    public Method getRequestMethod()
	{
		return requestMethod;
	}

	/* (non-Javadoc)
	 * @see com.ericsson.research.trap.nhttpd.impl.Response#setRequestMethod(com.ericsson.research.trap.nhttpd.impl.NanoHTTPD.Method)
	 */
    public Response setRequestMethod(Method requestMethod)
	{
		this.requestMethod = requestMethod;
		return this;
	}

	/* (non-Javadoc)
	 * @see com.ericsson.research.trap.nhttpd.impl.Response#setChunkedTransfer(boolean)
	 */
	@Override
    public Response setChunkedTransfer(boolean chunkedTransfer)
	{
		this.chunkedTransfer = chunkedTransfer;
		return this;
	}

	/* (non-Javadoc)
	 * @see com.ericsson.research.trap.nhttpd.impl.Response#setData(byte[])
	 */
	@Override
    public Response setData(byte[] data)
	{
		responseData = data;
		if (data == null)
		{
			this.data = null;
			return this;
		}
		setData(new ByteArrayInputStream(data));
		return this;
	}

	/* (non-Javadoc)
	 * @see com.ericsson.research.trap.nhttpd.impl.Response#setAsync(boolean)
	 */
	@Override
    public Response setAsync(boolean async)
	{
		this.async = async;
		return this;
	}

	/* (non-Javadoc)
	 * @see com.ericsson.research.trap.nhttpd.impl.Response#getAsync()
	 */
	@Override
    public boolean getAsync()
	{
		return async;
	}

	@Override
    public void sendAsyncResponse()
    {
		request.finish(this);
    }

	@Override
    public Response setData(String data)
    {
	    return setData(StringUtil.toUtfBytes(data));
    }
}
