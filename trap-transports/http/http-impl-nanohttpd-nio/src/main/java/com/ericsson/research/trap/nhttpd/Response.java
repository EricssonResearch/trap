package com.ericsson.research.trap.nhttpd;

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

import com.ericsson.research.trap.nhttpd.NanoHTTPD.Method;

/**
 * HTTP response. Return one of these from serve().
 */
public class Response
{
	/**
	 * HTTP status code after processing, e.g. "200 OK", HTTP_OK
	 */
	private Response.IStatus	status;
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
	private Map<String, String>	header	= new HashMap<String, String>();
	/**
	 * The request method that spawned this response.
	 */
	private Method	            requestMethod;
	/**
	 * Use chunkedTransfer
	 */
	private boolean	            chunkedTransfer;
	
	/**
	 * Uninitialised response
	 */
	public Response()
	{
	}

	/**
	 * Default constructor: response = HTTP_OK, mime = MIME_HTML and your
	 * supplied message
	 */
	public Response(String msg)
	{
		this(Status.OK, NanoHTTPD.MIME_HTML, msg);
	}

	/**
	 * Basic constructor.
	 */
	public Response(Response.IStatus status, String mimeType, InputStream data)
	{
		this.status = status;
		this.mimeType = mimeType;
		this.data = data;
	}

	/**
	 * Convenience method that makes an InputStream out of given text.
	 */
	public Response(Response.IStatus status, String mimeType, String txt)
	{
		this.status = status;
		this.mimeType = mimeType;
		try
		{
			this.data = txt != null ? new ByteArrayInputStream(txt.getBytes("UTF-8")) : null;
		}
		catch (java.io.UnsupportedEncodingException uee)
		{
			uee.printStackTrace();
		}
	}

	/**
	 * Adds given line to the header.
	 */
	public void addHeader(String name, String value)
	{
		header.put(name, value);
	}

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
			if (status == null)
			{
				throw new Error("sendResponse(): Status can't be null.");
			}
			PrintWriter pw = new PrintWriter(outputStream);
			pw.print("HTTP/1.1 " + status.getDescription() + " \r\n");

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

			if (requestMethod != Method.HEAD && chunkedTransfer)
			{
				sendAsChunked(outputStream, pw);
			}
			else
			{
				int pending = data != null ? data.available() : 0;
				sendContentLengthHeaderIfNotAlreadyPresent(pw, header, pending);
				pw.print("\r\n");
				pw.flush();
				sendAsFixedLength(outputStream, pending);
			}
			outputStream.flush();
			NanoHTTPD.safeClose(data);
		}
		catch (IOException ioe)
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
		pw.print("Transfer-Encoding: chunked\r\n");
		pw.print("\r\n");
		pw.flush();
		int BUFFER_SIZE = 16 * 1024;
		byte[] CRLF = "\r\n".getBytes();
		byte[] buff = new byte[BUFFER_SIZE];
		int read;
		while ((read = data.read(buff)) > 0)
		{
			outputStream.write(String.format("%x\r\n", read).getBytes());
			outputStream.write(buff, 0, read);
			outputStream.write(CRLF);
		}
		outputStream.write(String.format("0\r\n\r\n").getBytes());
	}

	private void sendAsFixedLength(OutputStream outputStream, int pending) throws IOException
	{
		if (requestMethod != Method.HEAD && data != null)
		{
			int BUFFER_SIZE = 16 * 1024;
			byte[] buff = new byte[BUFFER_SIZE];
			while (pending > 0)
			{
				int read = data.read(buff, 0, ((pending > BUFFER_SIZE) ? BUFFER_SIZE : pending));
				if (read <= 0)
				{
					break;
				}
				outputStream.write(buff, 0, read);
				pending -= read;
			}
		}
	}

	public Response.IStatus getStatus()
	{
		return status;
	}

	public void setStatus(Response.Status status)
	{
		this.status = status;
	}

	public String getMimeType()
	{
		return mimeType;
	}

	public void setMimeType(String mimeType)
	{
		this.mimeType = mimeType;
	}

	public InputStream getData()
	{
		return data;
	}

	public void setData(InputStream data)
	{
		this.data = data;
	}

	public Method getRequestMethod()
	{
		return requestMethod;
	}

	public void setRequestMethod(Method requestMethod)
	{
		this.requestMethod = requestMethod;
	}

	public void setChunkedTransfer(boolean chunkedTransfer)
	{
		this.chunkedTransfer = chunkedTransfer;
	}

	public interface IStatus
	{
		int getRequestStatus();

		String getDescription();
	}

	/**
	 * Some HTTP response status codes
	 */
	public enum Status implements Response.IStatus
	{
		SWITCH_PROTOCOL(101, "Switching Protocols"), OK(200, "OK"), CREATED(201, "Created"), ACCEPTED(202, "Accepted"), NO_CONTENT(204, "No Content"), PARTIAL_CONTENT(
		        206, "Partial Content"), REDIRECT(301, "Moved Permanently"), NOT_MODIFIED(304, "Not Modified"), BAD_REQUEST(400, "Bad Request"), UNAUTHORIZED(
		        401, "Unauthorized"), FORBIDDEN(403, "Forbidden"), NOT_FOUND(404, "Not Found"), METHOD_NOT_ALLOWED(405, "Method Not Allowed"), RANGE_NOT_SATISFIABLE(
		        416, "Requested Range Not Satisfiable"), INTERNAL_ERROR(500, "Internal Server Error");
		private final int		requestStatus;
		private final String	description;

		Status(int requestStatus, String description)
		{
			this.requestStatus = requestStatus;
			this.description = description;
		}

		@Override
		public int getRequestStatus()
		{
			return this.requestStatus;
		}

		@Override
		public String getDescription()
		{
			return "" + this.requestStatus + " " + description;
		}
	}

	public void setData(byte[] data)
	{
		setData(new ByteArrayInputStream(data));
	}
}