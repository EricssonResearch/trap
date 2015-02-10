package com.ericsson.research.trap.nhttpd.impl;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import com.ericsson.research.trap.nhttpd.CookieHandler;
import com.ericsson.research.trap.nhttpd.Method;
import com.ericsson.research.trap.nhttpd.Request;
import com.ericsson.research.trap.nhttpd.ResponseException;
import com.ericsson.research.trap.nhttpd.StatusCodes;
import com.ericsson.research.trap.nio.NioInputStream;
import com.ericsson.research.trap.nio.NioOutputStream;
import com.ericsson.research.trap.nio.Socket;
import com.ericsson.research.trap.nio.Socket.SocketHandler;
import com.ericsson.research.trap.utils.StringUtil;

public class HTTPSession implements Request, SocketHandler, Runnable
{
	public static final int	    BUFSIZE	= 8192;
	private int	                splitbyte;
	private int	                rlen;
	private String	            uri;
	private Method	            method;
	private Map<String, String>	parms;
	private Map<String, String>	headers;
	private CookieHandler	    cookies;
	private String	            queryParameterString;
	private Socket	            sock;
	private NanoHTTPDImpl	        server;
	private byte[]	            buf;

	public HTTPSession(NanoHTTPDImpl server, Socket sock) throws IOException
	{
		this.server = server;
		this.sock = sock;
		sock.setHandler(this);

		InetAddress inetAddress = sock.getRemoteSocketAddress().getAddress();
		String remoteIp = inetAddress.isLoopbackAddress() || inetAddress.isAnyLocalAddress() ? "127.0.0.1" : inetAddress.getHostAddress().toString();
		headers = new HashMap<String, String>();

		headers.put("remote-addr", remoteIp);
		headers.put("http-client-ip", remoteIp);

		// Create an output stream for us
		this.outputStream = new NioOutputStream(sock);
	}

	public void run()
	{

		try
		{
			// Read the first 8192 bytes.
			// The full header should fit in here.
			// Apache's default header limit is 8KB.
			// Do NOT assume that a single read will get the entire header at
			// once!
			this.buf = new byte[BUFSIZE];
			headers.clear();
			splitbyte = 0;
			rlen = 0;
			{
				int read = -1;
				try
				{
					read = inputStream.read(buf, 0, BUFSIZE);
				}
				catch (Exception e)
				{
					e.printStackTrace();
					NanoHTTPDImpl.safeClose(inputStream);
					NanoHTTPDImpl.safeClose(outputStream);
					sock.close();
					return;
				}
				if (read == -1)
				{
					// socket was been closed
					NanoHTTPDImpl.safeClose(inputStream);
					NanoHTTPDImpl.safeClose(outputStream);
					sock.close();
					return;
				}
				while (read >= 0)
				{
					rlen += read;
					splitbyte = findHeaderEnd(buf, rlen);
					if (splitbyte > 0)
						break;
					read = inputStream.read(buf, rlen, BUFSIZE - rlen);
				}
			}

			if (splitbyte < rlen)
			{
				// Flush out all the data from IS into a new IS
				NioInputStream newIs = new NioInputStream();
				newIs.setHandler(this, sock, false);
				ByteBuffer bb = ByteBuffer.allocate(rlen - splitbyte).put(buf, splitbyte, rlen - splitbyte);
				bb.flip();
				newIs.received(bb, sock);
				inputStream.setHandler(newIs, sock, true);
				inputStream = newIs;
			}

			parms = new HashMap<String, String>();
			if (null == headers)
			{
				headers = new HashMap<String, String>();
			}

			// Create a BufferedReader for parsing the header.
			BufferedReader hin = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buf, 0, rlen)));

			// Decode the header into parms and header java properties
			Map<String, String> pre = new HashMap<String, String>();
			decodeHeader(hin, pre, parms, headers);

			method = Method.lookup(pre.get("method"));
			if (method == null)
			{
				throw new ResponseException(StatusCodes.BAD_REQUEST, "BAD REQUEST: Syntax error.");
			}

			uri = pre.get("uri");

			cookies = new CookieHandler(headers);

			int size = -1;

			if (headers.containsKey("content-length"))
			{
				size = Integer.parseInt(headers.get("content-length"));
			}

			// if size == -1, assume chunked

			if (size == -1)
			{
				if ("chunked".equals(headers.get("transfer-encoding")))
				{
					body = new ChunkedInputStream(inputStream);
				}
				else
					body = null;
			}
			else
				body = new LimitedInputStream(inputStream, size);

			ResponseImpl r = new ResponseImpl(this);
			server.getHandler().handleRequest(this, r);
			if (!r.getAsync())
				finish(r);
		}
		catch (SocketException e)
		{
			e.printStackTrace();
		}
		catch (SocketTimeoutException ste)
		{
			ste.printStackTrace();
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
			ResponseImpl r = (ResponseImpl) new ResponseImpl(null).setStatus(StatusCodes.INTERNAL_SERVER_ERROR).setMimeType(NanoHTTPDImpl.MIME_PLAINTEXT)
			        .setData(StringUtil.toUtfBytes("SERVER INTERNAL ERROR: IOException: " + ioe.getMessage()));
			r.send(outputStream);
			NanoHTTPDImpl.safeClose(outputStream);
		}
		catch (ResponseException re)
		{
			ResponseImpl r = (ResponseImpl) new ResponseImpl(null).setStatus(re.getStatus()).setMimeType(NanoHTTPDImpl.MIME_PLAINTEXT)
			        .setData(StringUtil.toUtfBytes(re.getMessage()));
			r.send(outputStream);
			NanoHTTPDImpl.safeClose(outputStream);
		}
		catch (RuntimeException e)
		{
			e.printStackTrace();
		}
		finally
		{

		}
	}

	public void finish(ResponseImpl r)
	{

		try
		{
			// Read any (eventual) message body, to prevent downstream
			// corruption
			if (body != null)
			{
				byte[] buf = new byte[4096];
				while (body.read(buf) > -1)
					;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			sock.close();
		}
		body = null;
		try
		{
			cookies.unloadQueue(r);
			r.setRequestMethod(method);
			r.send(outputStream);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			NioInputStream is = this.inputStream;
			this.inputStream = null;

			if (is != null)
				is.setHandler(this, sock, true);
		}
	}

	static class LimitedInputStream extends InputStream
	{
		private InputStream	src;
		private int		    limit;
		private int		    idx	= 0;

		public LimitedInputStream(InputStream src, int limit)
		{
			this.src = src;
			this.limit = limit;

		}

		@Override
		public int read() throws IOException
		{
			if (idx++ >= limit)
				return -1;

			return src.read();
		}

		@Override
		public int read(byte[] dst, int off, int len) throws IOException
		{

			if (idx >= limit)
				return -1;

			len = Math.min(len, limit - idx);

			int read = src.read(dst, off, len);

			if (read > -1)
				idx += read;

			return read;

		}
	}

	static class ChunkedInputStream extends InputStream
	{
		private InputStream	src;
		byte[]		        header		= new byte[32];
		int		            headerIdx	= 0;
		int		            chunkLength	= 0;
		byte[]		        chunk		= null;
		int		            chunkIdx	= 0;
		private boolean		eof		    = false;

		public ChunkedInputStream(InputStream src)
		{
			this.src = src;

		}

		boolean readChunk() throws IOException
		{

			if (eof)
				return false;

			if (chunkIdx < chunkLength)
				return true;

			headerIdx = 0;

			while (headerIdx < 2 || (header[headerIdx - 2] != '\r' && header[headerIdx - 1] != '\n'))
			{
				int read = src.read();
				if (read == -1)
					return false;
				header[headerIdx++] = (byte) read;
			}

			String num = new String(header, 0, headerIdx - 2);
			chunkLength = Integer.valueOf(num, 16) + 2;
			chunkIdx = 0;
			chunk = new byte[chunkLength];

			while (chunkIdx < chunkLength)
			{
				int read = src.read(chunk, chunkIdx, chunk.length - chunkIdx);
				if (read == -1)
					return false;
				chunkIdx += read;
			}

			chunkIdx = 0;
			chunkLength -= 2;

			// Terminator
			if (chunkLength == 0)
				return false;

			return true;
		}

		@Override
		public int read() throws IOException
		{
			if (!readChunk())
			{
				this.eof = true;
				return -1;
			}
			return chunk[chunkIdx++];
		}
	}

	InputStream	body;

	/**
	 * Decodes the sent headers and loads the data into Key/value pairs
	 */
	private void decodeHeader(BufferedReader in, Map<String, String> pre, Map<String, String> parms, Map<String, String> headers) throws ResponseException
	{
		try
		{
			// Read the request line
			String inLine = in.readLine();
			if (inLine == null)
			{
				return;
			}

			StringTokenizer st = new StringTokenizer(inLine);
			if (!st.hasMoreTokens())
			{
				throw new ResponseException(StatusCodes.BAD_REQUEST, "BAD REQUEST: Syntax error. Usage: GET /example/file.html");
			}

			pre.put("method", st.nextToken());

			if (!st.hasMoreTokens())
			{
				throw new ResponseException(StatusCodes.BAD_REQUEST, "BAD REQUEST: Missing URI. Usage: GET /example/file.html");
			}

			String uri = st.nextToken();

			// Decode parameters from the URI
			int qmi = uri.indexOf('?');
			if (qmi >= 0)
			{
				decodeParms(uri.substring(qmi + 1), parms);
				uri = NanoHTTPDImpl.decodePercent(uri.substring(0, qmi));
			}
			else
			{
				uri = NanoHTTPDImpl.decodePercent(uri);
			}

			// If there's another token, it's protocol version,
			// followed by HTTP headers. Ignore version but parse headers.
			// NOTE: this now forces header names lowercase since they are
			// case insensitive and vary by client.
			if (st.hasMoreTokens())
			{
				String line = in.readLine();
				while (line != null && line.trim().length() > 0)
				{
					int p = line.indexOf(':');
					if (p >= 0)
						headers.put(line.substring(0, p).trim().toLowerCase(Locale.US), line.substring(p + 1).trim());
					line = in.readLine();
				}
			}

			pre.put("uri", uri);
		}
		catch (IOException ioe)
		{
			throw new ResponseException(StatusCodes.INTERNAL_SERVER_ERROR, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage(), ioe);
		}
	}

	/**
	 * Find byte index separating header from body. It must be the last byte of
	 * the first two sequential new lines.
	 */
	private int findHeaderEnd(final byte[] buf, int rlen)
	{
		int splitbyte = 0;
		while (splitbyte + 3 < rlen)
		{
			if (buf[splitbyte] == '\r' && buf[splitbyte + 1] == '\n' && buf[splitbyte + 2] == '\r' && buf[splitbyte + 3] == '\n')
			{
				return splitbyte + 4;
			}
			splitbyte++;
		}
		return 0;
	}

	/**
	 * Decodes parameters in percent-encoded URI-format ( e.g.
	 * "name=Jack%20Daniels&pass=Single%20Malt" ) and adds them to given Map.
	 * NOTE: this doesn't support multiple identical keys due to the simplicity
	 * of Map.
	 */
	private void decodeParms(String parms, Map<String, String> p)
	{
		if (parms == null)
		{
			queryParameterString = "";
			return;
		}

		queryParameterString = parms;
		StringTokenizer st = new StringTokenizer(parms, "&");
		while (st.hasMoreTokens())
		{
			String e = st.nextToken();
			int sep = e.indexOf('=');
			if (sep >= 0)
			{
				p.put(NanoHTTPDImpl.decodePercent(e.substring(0, sep)).trim(), NanoHTTPDImpl.decodePercent(e.substring(sep + 1)));
			}
			else
			{
				p.put(NanoHTTPDImpl.decodePercent(e).trim(), "");
			}
		}
	}

	@Override
	public final Map<String, String> getParms()
	{
		return parms;
	}

	public String getQueryParameterString()
	{
		return queryParameterString;
	}

	@Override
	public final Map<String, String> getHeaders()
	{
		return headers;
	}

	@Override
	public final String getUri()
	{
		return uri;
	}

	@Override
	public final Method getMethod()
	{
		return method;
	}

	@Override
	public final Socket getSocket()
	{
		return sock;
	}

	@Override
	public CookieHandler getCookies()
	{
		return cookies;
	}

	@Override
	public void sent(Socket sock)
	{
		// Handled by the OutputStream primarily
		outputStream.sent(sock);
	}

	boolean	                haveStreams	= false;
	private NioOutputStream	outputStream;
	private NioInputStream	inputStream;

	@Override
	public synchronized void received(ByteBuffer data, Socket sock)
	{
		if (inputStream == null)
		{
			this.inputStream = new NioInputStream(sock);
			inputStream.received(data, sock);
			server.asyncRunner.exec(this);
		}
		else
		{
			inputStream.received(data, sock);
		}
	}

	@Override
	public void opened(Socket sock)
	{
		// Not gonna happen
	}

	@Override
	public void closed(Socket sock)
	{
		server.unRegisterConnection(sock);
	}

	@Override
	public void error(Throwable exc, Socket sock)
	{
		server.unRegisterConnection(sock);
	}

	@Override
	public synchronized InputStream getInputStream()
	{
		return body;
	}

	@Override
	public Socket upgrade(SocketHandler handler, boolean withHeaders)
	{
		if (withHeaders)
			inputStream.received(ByteBuffer.wrap(buf, 0, rlen), sock);
		inputStream.setHandler(handler, sock, true);
		body = null;
		return sock;
	}
}