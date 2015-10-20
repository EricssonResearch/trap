package com.ericsson.research.trap;

/*
 * ##_BEGIN_LICENSE_##
 * Transport Abstraction Package (trap)
 * ----------
 * Copyright (C) 2014 Ericsson AB
 * ----------
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Ericsson AB nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * ##_END_LICENSE_##
 */



import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import com.ericsson.research.trap.delegates.OnAccept;
import com.ericsson.research.trap.utils.StringUtil;

/**
 * TrapFactory is used as an entry point to the underlying Trap implementation. It can create listener (for servers) or
 * client instances.
 * 
 * @author Vladimir Katardjiev
 * @since 1.0
 */
public class TrapFactory
{
    
    /**
     * Creates a listener endpoint with default configuration. <b>It is up to the caller to ensure proper configuration
     * of the listener before calling {@link TrapListener#listen(OnAccept)} </b>.
     * 
     * @return A newly created TrapListener instance.
     * @throws TrapException
     *             If an error occurred during instantiation; either if the configuration was invalid, or if the Trap
     *             implementation was not present.
     * @since 1.1
     */
    public static TrapListener createListener() throws TrapException
    {
        return createListener(null);
    }
    
    /**
     * Creates a listener endpoint instance, capable of accepting new incoming connections. The listener will be
     * initialised with some default values, but will not listen for incoming connections until
     * {@link TrapListener#listen(OnAccept)} is called.
     * 
     * @param configuration
     *            The configuration string to use when creating the listener. It is strongly recommended you always have
     *            a configuration string, even though Trap can supply default values.
     * @return A newly created TrapListener instance.
     * @throws TrapException
     *             If an error occurred during instantiation; either if the configuration was invalid, or if the Trap
     *             implementation was not present.
     */
    public static TrapListener createListener(String configuration) throws TrapException
    {
        try
        {
            @SuppressWarnings("unchecked")
            Class<TrapListener> c = (Class<TrapListener>) Class.forName("com.ericsson.research.trap.impl.ListenerTrapEndpoint");
            TrapListener l = c.newInstance();
            l.configure(resolveConfiguration(configuration));
            return l;
        }
        catch (Exception e)
        {
            throw new TrapException("Error while allocating a Listener Trap Endpoint", e);
        }
    }
    
    /**
     * Creates a client endpoint instance, capable of creating an outgoing connection. Unlike the listener object, the
     * client <i>must</i> be configured with proper configuration before it can connect. The listener endpoint instance
     * is capable of creating a complete configuration profile out of the box, which can just be supplied to the client.
     * 
     * @param configuration
     *            A configuration representing, at the very least, one transport address to connect to a Listener Trap
     *            Endpoint.
     * @param autoConfigure
     *            Enables/disables automatic client configuration. When <i>true</i>, the client can learn of additional
     *            transports and configuration parameters from the server. <i>false</i> makes the client ignore updated
     *            configuration.
     * @return A TrapEndpoint capable of opening an outgoing connection to a Listener Trap Endpoint.
     * @throws TrapException
     *             If an error occurred during instantiation; either if the configuration was invalid, or if the Trap
     *             implementation was not present.
     */
    public static TrapClient createClient(String configuration, boolean autoConfigure) throws TrapException
    {
        try
        {
            Class<?> c = Class.forName("com.ericsson.research.trap.impl.ClientTrapEndpoint");
            TrapClient l = (TrapClient) c.getConstructor(new Class[] { String.class, Boolean.class }).newInstance(new Object[] { resolveConfiguration(configuration), autoConfigure });
            return l;
        }
        catch (Exception e)
        {
            throw new TrapException("Error while allocating a Client Trap Endpoint", e);
        }
    }
    
    /**
     * Accessor for the current Trap API version. It is possible, <i>though not recommended</i> to use a different
     * version of trap-api and trap-core. When using Maven, the versions should be matched automatically. This method
     * has two primary purposes:
     * <p>
     * First, a trap-core implementation <i>should</i> verify that it matches this API version.
     * <p>
     * Second, someone with an unknown trap-full.jar can use this method to determine the version number.
     * 
     * @return The Trap API version, or "unknown" if not known. The version corresponds to the Maven version, so can be,
     *         e.g., 1.0 or 1.1-SNAPSHOT.
     */
    public static String getVersion()
    {
        try
        {
            Class<?> c = Class.forName("com.ericsson.research.trap.Version");
            Field field = c.getField("VERSION");
            return (String) field.get(null);
        }
        catch (Exception e)
        {
            return "unknown";
        }
    }
    
    /**
     * Applies configuration resolution heuristics to the configuration. Scans line-by-line to detect common patterns.
     * 
     * @param config
     *            The configuration string to resolve.
     * @throws TrapException
     */
    protected static String resolveConfiguration(String config) throws TrapException
    {
        
        if (config == null)
            return config;
        
        StringBuilder rv = new StringBuilder();
        try
        {
            BufferedReader br = new BufferedReader(new StringReader(config));
            String line;
            while ((line = br.readLine()) != null)
            {
                
                try
                {
                    
                    if (line.startsWith("trap"))
                    {
                        rv.append(line);
                        rv.append("\n");
                        continue;
                    }
                    
                    URI uri = URI.create(line);
                    String scheme = uri.getScheme();
                    
                    if ("http".equals(scheme) || "https".equals(scheme))
                    {
                        
                        // The common pattern of http[s]://.../trap.txt indicates we should download the config.
                        // Query params are permitted (ignored by our code).
                        if (uri.getPath().endsWith("trap.txt"))
                        {
                            
                            try
                            {
                                URL url = new URL(line);
                                URLConnection connection = url.openConnection();
                                connection.setConnectTimeout(3000);
                                connection.setReadTimeout(3000);
                                InputStream is = connection.getInputStream();
                                
                                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                
                                int c;
                                
                                while ((c = is.read()) > -1)
                                    bos.write(c);
                                
                                String cfg = StringUtil.toUtfString(bos.toByteArray());
                                
                                // Once we've read the new config, we don't need to do anything else.
                                // A side-effect of this is that a single config can include multiple lines (failover-ish)
                                // Also, we will recurse over the config to permit resolution thereof.
                                return resolveConfiguration(cfg);
                            }
                            catch (Exception e)
                            {
                                // TODO: This is a non-fatal error, but we should handle it in a more graceful manner.
                                e.printStackTrace();
                                continue;
                            }
                        }
                        else
                        {
                            append("trap.transport.http.url=", line, rv);
                            append("trap.transport.http.host=", uri.getHost(), rv);
                            
                            int port = uri.getPort();
                            if (port == -1)
                                port = "http".equals(uri.getScheme()) ? 80 : 443;
                            
                            append("trap.transport.http.port=", Integer.toString(port), rv);
                            
                        }
                    }
                    else if ("ws".equals(scheme) || "wss".equals(scheme))
                    {
                        append("trap.transport.websocket.wsuri=", line, rv);
                        append("trap.transport.websocket.host=", uri.getHost(), rv);
                        
                        int port = uri.getPort();
                        if (port == -1)
                            port = "ws".equals(uri.getScheme()) ? 80 : 443;
                        
                        append("trap.transport.websocket.port=", Integer.toString(port), rv);
                        append("trap.transport.websocket.path=", uri.getPath(), rv);
                    }
                    else if ("socket".equals(scheme) || "sockets".equals(scheme))
                    {
                        append("trap.transport.socket.host=", uri.getHost(), rv);
                        append("trap.transport.socket.port=", Integer.toString(uri.getPort()), rv);
                        
                        if ("sockets".equals(scheme))
                            append("trap.transport.socket.secure=", "true", rv);
                    }
                }
                catch (RuntimeException e)
                {
                    rv.append(line);
                    rv.append("\n");
                }
                catch (Exception e)
                {
                    rv.append(line);
                    rv.append("\n");
                }
                
            }
        }
        catch (IOException e)
        {
            throw new TrapException("Error while parsing configuration", e);
        }
        
        return rv.toString();
    }
    
    private static void append(String prefix, String value, StringBuilder out)
    {
        if (prefix == null || value == null || out == null)
            return;
        out.append(prefix);
        out.append(value);
        out.append("\n");
    }
    
    // Prevent it from being documented.
    private TrapFactory()
    {
    }
}
