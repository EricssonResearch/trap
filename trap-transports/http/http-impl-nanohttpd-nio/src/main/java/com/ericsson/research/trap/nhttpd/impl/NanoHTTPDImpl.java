package com.ericsson.research.trap.nhttpd.impl;

import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.net.ssl.SSLContext;

import com.ericsson.research.trap.nhttpd.Request;
import com.ericsson.research.trap.nhttpd.RequestHandler;
import com.ericsson.research.trap.nhttpd.Response;
import com.ericsson.research.trap.nhttpd.options.DefaultAsyncRunner;
import com.ericsson.research.trap.nio.Nio;
import com.ericsson.research.trap.nio.ServerSocket;
import com.ericsson.research.trap.nio.ServerSocket.ServerSocketHandler;
import com.ericsson.research.trap.nio.Socket;

/**
 * A simple, tiny, nicely embeddable HTTP server in Java
 * <p/>
 * <p/>
 * NanoHTTPD
 * <p>
 * </p>
 * Copyright (c) 2012-2013 by Paul S. Hawke, 2001,2005-2013 by Jarno Elonen, 2010 by Konstantinos Togias</p>
 * <p>
 * See the separate "LICENSE.md" file for the distribution license (Modified BSD licence)
 */
public abstract class NanoHTTPDImpl implements ServerSocketHandler
{
    /**
     * Maximum time to wait on Socket.getInputStream().read() (in milliseconds) This is required as the Keep-Alive HTTP
     * connections would otherwise block the socket reading thread forever (or as long the browser is open).
     */
    public static final int        SOCKET_READ_TIMEOUT    = 5000;
    /**
     * Common mime type for dynamic content: plain text
     */
    public static final String     MIME_PLAINTEXT         = "text/plain";
    /**
     * Common mime type for dynamic content: html
     */
    public static final String     MIME_HTML              = "text/html";
    /**
     * Pseudo-Parameter to use to store the actual query string in the parameters map for later re-processing.
     */
    private static final String    QUERY_STRING_PARAMETER = "NanoHttpd.QUERY_STRING";
    private final String           hostname;
    private final int              myPort;
    private ServerSocket           myServerSocket;
    private Set<Socket>            openConnections        = new HashSet<Socket>();
    /**
     * Pluggable strategy for asynchronously executing requests.
     */
    AsyncRunner                    asyncRunner;
    private SSLContext             sslc;
    
    private RequestHandler handler = new RequestHandler()
	{
		@Override
		public void handleRequest(Request request, Response response)
		{
			// Do_nothing. The response will return 404.
		}
	};
    
    /**
     * Constructs an HTTP server on given port.
     */
    public NanoHTTPDImpl(int port)
    {
        this(null, port);
    }
    
    /**
     * Constructs an HTTP server on given hostname and port.
     */
    public NanoHTTPDImpl(String hostname, int port)
    {
        this.hostname = hostname;
        this.myPort = port;
        setAsyncRunner(new DefaultAsyncRunner());
    }
    
    static final void safeClose(Closeable closeable)
    {
        if (closeable != null)
        {
            try
            {
                closeable.close();
            }
            catch (IOException e)
            {
            }
        }
    }
    
    private static final void safeClose(Socket closeable)
    {
        if (closeable != null)
        {
            closeable.close();
        }
    }
    
    private static final void safeClose(ServerSocket closeable)
    {
        if (closeable != null)
        {
            closeable.close();
        }
    }
    
    /**
     * Start the server.
     *
     * @throws IOException
     *             if the socket is in use.
     */
    public void start() throws IOException
    {
        if (getSslc() != null)
            myServerSocket = Nio.factory().sslServer(getSslc(), this);
        else
            myServerSocket = Nio.factory().server(this);
        myServerSocket.listen((hostname != null) ? new InetSocketAddress(hostname, myPort) : new InetSocketAddress(myPort));
        
    }
    
    /**
     * Stop the server.
     */
    public void stop()
    {
        try
        {
            safeClose(myServerSocket);
            closeAllConnections();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    /**
     * Registers that a new connection has been set up.
     *
     * @param socket
     *            the {@link Socket} for the connection.
     */
    public void registerConnection(Socket socket)
    {
        synchronized (openConnections)
        {
            openConnections.add(socket);
        }
    }
    
    /**
     * Registers that a connection has been closed
     *
     * @param socket
     *            the {@link Socket} for the connection.
     */
    public void unRegisterConnection(Socket socket)
    {
        synchronized (openConnections)
        {
            
            openConnections.remove(socket);
        }
    }
    
    /**
     * Forcibly closes all connections that are open.
     */
    public void closeAllConnections()
    {
        synchronized (openConnections)
        {
            Socket[] socks = openConnections.toArray(new Socket[]{});
            for (Socket socket : socks)
            {
                safeClose(socket);
            }
        }
    }
    
    public final int getListeningPort()
    {
        try
        {
            return myServerSocket == null ? -1 : myServerSocket.getInetAddress().getPort();
        }
        catch (IOException e)
        {
            return -1;
        }
    }
    
    public final boolean wasStarted()
    {
        return myServerSocket != null;
    }
    
    public final boolean isAlive()
    {
        return wasStarted() && !myServerSocket.isClosed();
    }
    
    /**
     * Decode percent encoded <code>String</code> values.
     *
     * @param str
     *            the percent encoded <code>String</code>
     * @return expanded form of the input, for example "foo%20bar" becomes "foo bar"
     */
    protected static String decodePercent(String str)
    {
        String decoded = null;
        try
        {
            decoded = URLDecoder.decode(str, "UTF8");
        }
        catch (UnsupportedEncodingException ignored)
        {
        }
        return decoded;
    }
    
    /**
     * Decode parameters from a URL, handing the case where a single parameter name might have been supplied several
     * times, by return lists of values. In general these lists will contain a single element.
     *
     * @param parms
     *            original <b>NanoHttpd</b> parameters values, as passed to the <code>serve()</code> method.
     * @return a map of <code>String</code> (parameter name) to <code>List&lt;String&gt;</code> (a list of the values
     *         supplied).
     */
    protected Map<String, List<String>> decodeParameters(Map<String, String> parms)
    {
        return this.decodeParameters(parms.get(QUERY_STRING_PARAMETER));
    }
    
    /**
     * Decode parameters from a URL, handing the case where a single parameter name might have been supplied several
     * times, by return lists of values. In general these lists will contain a single element.
     *
     * @param queryString
     *            a query string pulled from the URL.
     * @return a map of <code>String</code> (parameter name) to <code>List&lt;String&gt;</code> (a list of the values
     *         supplied).
     */
    protected Map<String, List<String>> decodeParameters(String queryString)
    {
        Map<String, List<String>> parms = new HashMap<String, List<String>>();
        if (queryString != null)
        {
            StringTokenizer st = new StringTokenizer(queryString, "&");
            while (st.hasMoreTokens())
            {
                String e = st.nextToken();
                int sep = e.indexOf('=');
                String propertyName = (sep >= 0) ? decodePercent(e.substring(0, sep)).trim() : decodePercent(e).trim();
                if (!parms.containsKey(propertyName))
                {
                    parms.put(propertyName, new ArrayList<String>());
                }
                String propertyValue = (sep >= 0) ? decodePercent(e.substring(sep + 1)) : null;
                if (propertyValue != null)
                {
                    parms.get(propertyName).add(propertyValue);
                }
            }
        }
        return parms;
    }
    
    // ------------------------------------------------------------------------------- //
    //
    // Threading Strategy.
    //
    // ------------------------------------------------------------------------------- //
    
    /**
     * Pluggable strategy for asynchronously executing requests.
     *
     * @param asyncRunner
     *            new strategy for handling threads.
     */
    public void setAsyncRunner(AsyncRunner asyncRunner)
    {
        this.asyncRunner = asyncRunner;
    }
    
    /**
     * Pluggable strategy for asynchronously executing requests.
     */
    public interface AsyncRunner
    {
        void exec(Runnable code);
    }
    
    @Override
    public void accept(Socket sock, ServerSocket ss)
    {
        registerConnection(sock);
        try
        {
            new HTTPSession(this, sock);
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    @Override
    public void error(Throwable exc, ServerSocket ss)
    {
        // TODO Auto-generated method stub
        
    }
    
    public SSLContext getSslc()
    {
        return sslc;
    }
    
    public void setSslc(SSLContext sslc)
    {
        this.sslc = sslc;
    }
    
    public InetSocketAddress getAddress() throws IOException
    {
        return myServerSocket.getInetAddress();
    }

	public RequestHandler getHandler()
    {
	    return handler;
    }

	public NanoHTTPDImpl setHandler(RequestHandler handler)
    {
		this.handler = handler;
		return this;
    }
    
    // ------------------------------------------------------------------------------- //
    
}