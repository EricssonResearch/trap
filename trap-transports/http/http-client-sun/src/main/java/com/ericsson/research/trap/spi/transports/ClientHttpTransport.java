package com.ericsson.research.trap.spi.transports;

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.spi.TrapMessage;
import com.ericsson.research.trap.spi.TrapTransportException;
import com.ericsson.research.trap.spi.TrapTransportPriority;
import com.ericsson.research.trap.spi.TrapTransportProtocol;
import com.ericsson.research.trap.spi.TrapTransportState;
import com.ericsson.research.trap.utils.SSLUtil;
import com.ericsson.research.trap.utils.ThreadPool;

public class ClientHttpTransport extends AbstractTransport
{
    
    protected URL                    connectUrl;
    protected URL                    activeUrl;
    protected boolean                running         = true;
    LinkedBlockingQueue<TrapMessage> messagesToSend  = new LinkedBlockingQueue<TrapMessage>();
    protected boolean                sending         = false;
    protected long                   expirationDelay = 28000;
    protected HTTPPoller             poller          = null;
    protected Object                 sendingLock     = new Object();
    private boolean                  ignoreCertificates;
    
    public ClientHttpTransport()
    {
        this.transportPriority = TrapTransportPriority.HTTP_SUN;
    }
    
    @Override
    public String getTransportName()
    {
        return "http";
    }
    
    @Override
    public String getProtocolName()
    {
        return TrapTransportProtocol.HTTP;
    }
    
    @Override
    public void init()
    {
        super.init();
        this.messagesToSend = new LinkedBlockingQueue<TrapMessage>();
    }
    
    @Override
    protected void updateConfig()
    {
        super.updateConfig();
        try
        {
            if ((this.getState() == TrapTransportState.DISCONNECTED) || (this.getState() == TrapTransportState.CONNECTING))
            {
                String url = this.getOption("url");
                
                if (url == null)
                {
                    this.logger.trace("No URL specified for {} transport.", this.getTransportName());
                }
                else
                {
                    URI uri = URI.create(url);
                    this.connectUrl = uri.toURL();
                }
            }
            else
                this.logger.debug("Updating {} configuration while open; changes will not take effect until HTTP is reconnected", this.getTransportName());
        }
        catch (IllegalArgumentException e)
        {
            if (this.isEnabled())
            {
                this.logger.debug("Invalid configuration for {} transport. The transport is disabled", this.getTransportName());
                this.connectUrl = null;
            }
        }
        catch (MalformedURLException e)
        {
            if (this.isEnabled())
            {
                this.logger.debug("Invalid configuration for {} transport. The transport is disabled", this.getTransportName());
                this.connectUrl = null;
            }
        }
        catch (NullPointerException e)
        {
            this.logger.warn("Null Pointer Exception while updating transport config. Affected config was [" + this.getConfiguration() + "]");
        }
        
        String newexpirationDelay = this.getOption("expirationDelay");
        try
        {
            this.expirationDelay = Long.parseLong(newexpirationDelay);
        }
        catch (Exception e)
        {
        }
        
        this.ignoreCertificates = this.getBooleanOption(CERT_IGNORE_INVALID, false);
    }
    
    @Override
    public void fillAuthenticationKeys(HashSet<String> keys)
    {
        super.fillAuthenticationKeys(keys);
    }
    
    @Override
    public void internalSend(TrapMessage message, boolean expectMore) throws TrapTransportException
    {
        try
        {
            synchronized (this.messagesToSend)
            {
                
                if ((this.getState() != TrapTransportState.CONNECTED) && (this.getState() != TrapTransportState.AVAILABLE) && (this.getState() != TrapTransportState.UNAVAILABLE) && (message.getOp() != TrapMessage.Operation.CLOSE))
                    throw new TrapTransportException(message, this.getState());
                
                if (message != null)
                {
                    if (this.logger.isTraceEnabled())
                        this.logger.trace("[HTTP] Scheduling message {} with id {}", message.getOp(), message.getMessageId());
                    // Don't slam messages yet
                    
                    this.messagesToSend.add(message);
                    
                }
                
                if (expectMore)
                    return;
                
                this.flushTransport();
            }
        }
        catch (Exception e)
        {
            throw new TrapTransportException(message, this.state);
        }
    }
    
    @Override
    protected boolean isClientConfigured()
    {
        return this.connectUrl != null;
    }
    
    @Override
    protected void internalConnect() throws TrapException
    {
        
        this.updateConfig();
        
        // Make a request to get a new TransportID
        if (!this.isClientConfigured())
        {
            this.logger.debug("{} Transport not properly configured... Unless autoconfigure is enabled (and another transport succeeds) this transport will not be available.", this.getTransportName());
            this.setState(TrapTransportState.ERROR);
            return;
        }
        
        InputStream is = null;
        InputStreamReader isr = null;
        
        try
        {
            this.running = true;
            HttpURLConnection c = this.openConnection(this.connectUrl);
            c.setConnectTimeout(5000);
            c.setReadTimeout(5000);
            c.setRequestMethod("GET");
            is = c.getInputStream();
            isr = new InputStreamReader(is, Charset.forName("UTF-8"));
            
            StringWriter sr = new StringWriter();
            int ch = 0;
            while ((ch = isr.read()) != -1)
                sr.write(ch);
            
            String rawUrl = sr.toString();
            
            String urlBase = this.connectUrl.toString();
            
            if (urlBase.endsWith("/"))
                urlBase += rawUrl;
            else
                urlBase += "/" + rawUrl;
            
            this.activeUrl = URI.create(urlBase).toURL();
            
            if (this.poller != null)
                this.poller.running = false;
            
            this.poller = new HTTPPoller();
            
            // Start a new thread for polling
            Thread t = new Thread(this.poller);
            t.setDaemon(true);
            t.start();
            
            // Change state to connected
            this.setState(TrapTransportState.CONNECTED);
            
        }
        catch (Exception e)
        {
            this.setState(TrapTransportState.ERROR);
            throw new TrapException(e);
        }
        finally
        {
            if (is != null)
                try
                {
                    is.close();
                }
                catch (IOException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            
            if (isr != null)
                try
                {
                    isr.close();
                }
                catch (IOException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
        }
        
    }
    
    @Override
    protected void internalDisconnect()
    {
        this.running = false;
    }
    
    @Override
    public boolean canConnect()
    {
        return true;
    }
    
    protected byte[] readBuf = new byte[4096];
    
    class HTTPPoller implements Runnable
    {
        
        public boolean running = true;
        public Thread  pollThread;
        
        public void run()
        {
            this.pollThread = Thread.currentThread();
            try
            {
                ClientHttpTransport.this.logger.trace("{} Transport entering polling loop", ClientHttpTransport.this.getTransportName());
                while (this.running && ClientHttpTransport.this.running)
                {
                    try
                    {
                        HttpURLConnection c = ClientHttpTransport.this.openConnection(ClientHttpTransport.this.activeUrl);
                        c.setRequestMethod("GET");
                        
                        int responseCode = c.getResponseCode();
                        
                        if (responseCode >= 300)
                        {
                            ClientHttpTransport.this.logger.debug("Closing due to non-200 code; Transport status is {}", ClientHttpTransport.this.getState());
                            
                            if ((ClientHttpTransport.this.getState() != TrapTransportState.DISCONNECTED) && (ClientHttpTransport.this.getState() != TrapTransportState.ERROR) && (ClientHttpTransport.this.getState() != TrapTransportState.DISCONNECTING))
                                ClientHttpTransport.this.setState(TrapTransportState.ERROR);
                            return;
                        }
                        
                        InputStream is = c.getInputStream();
                        
                        // In some circumstances, the HTTPUrlConnection's input stream appears to contain an HTTP body. This is pretty interesting (and slightly wrong).
                        
                        if (c.getContentLength() > 0)
                        {
                            
                            String clStr = c.getHeaderField("Content-Length");
                            int contentLength = Integer.parseInt(clStr);
                            
                            if (contentLength > 0)
                            {
                                
                                // We have a known content length we can verify.
                                byte[] content = new byte[contentLength];
                                
                                int read = is.read(content);
                                
                                if (read != content.length)
                                {
                                    // TODO: This is a bug!
                                    ClientHttpTransport.this.logger.error("Content read does not equal content length. Aborting...");
                                    ClientHttpTransport.this.forceError();
                                    return;
                                }
                                
                                if (is.read() >= 0)
                                {
                                    // This is another bug. We have more content than was reported!
                                    ClientHttpTransport.this.logger.error("Excess content detected! Something is VERY wrong with this HTTP connection. Aborting...");
                                    ClientHttpTransport.this.forceError();
                                    return;
                                }
                                
                                ClientHttpTransport.this.receive(content, 0, content.length);
                                continue;
                            }
                        }
                        
                        int read = 0;
                        while ((read = is.read(ClientHttpTransport.this.readBuf)) != -1)
                            ClientHttpTransport.this.receive(ClientHttpTransport.this.readBuf, 0, read);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        ClientHttpTransport.this.logger.debug("Moving to state ERROR due to exception", e);
                        ClientHttpTransport.this.setState(TrapTransportState.ERROR);
                        return;
                    }
                }
            }
            finally
            {
                this.running = false;
                ClientHttpTransport.this.running = false;
                this.pollThread = null;
                ClientHttpTransport.this.logger.trace("HTTP Transport exiting polling loop. Running is false.");
                ClientHttpTransport.this.setState(TrapTransportState.DISCONNECTED);
            }
        }
        
    }
    
    protected HttpURLConnection openConnection(URL cUrl) throws IOException
    {
        /*
         * When the URL is formatted as
         *
         *  http://localhost:8088
         *
         * Then this automatic syntax will create a non-valid url. The code
         * below will detect if there is no path of the url; only if there is
         * no path will it append a slash.
         */
        String urlStr = cUrl.toString();
        if ((cUrl.getPath() == null) || (cUrl.getPath().trim().length() == 0))
            urlStr = urlStr + "/";
        
        urlStr += "?expires=" + this.expirationDelay;
        
        URI uri = URI.create(urlStr);
        URL url = uri.toURL();
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setConnectTimeout(5000);
        c.setReadTimeout(30000);
        this.checkInsecure(c);
        
        return c;
    }
    
    protected void checkInsecure(HttpURLConnection c)
    {
        if (this.ignoreCertificates && c instanceof HttpsURLConnection)
        {
            
            try
            {
                SSLContext sslc = SSLUtil.getInsecure();
                ((HttpsURLConnection) c).setSSLSocketFactory(sslc.getSocketFactory());
            }
            catch (Exception e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            ((HttpsURLConnection) c).setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String arg0, SSLSession arg1)
                {
                    return true;
                }
            });
        }
    }
    
    /*
     * Disable transit messages
     *
     * (non-Javadoc)
     * @see com.ericsson.research.trap.spi.transports.AbstractTransport#addTransitMessage(com.ericsson.research.trap.spi.TrapMessage)
     */
    protected void addTransitMessage(TrapMessage m)
    {
    }
    
    public void flushTransport()
    {
        synchronized (this.sendingLock)
        {
            if (!this.sending)
            {
                
                if (this.messagesToSend.size() == 0)
                    return;
                
                this.sending = true;
                if (this.getState() == TrapTransportState.AVAILABLE)
                    this.setState(TrapTransportState.UNAVAILABLE);
                
                ThreadPool.executeCached(new Runnable() {
                    
                    public void run()
                    {
                        try
                        {
                            synchronized (ClientHttpTransport.this.messagesToSend)
                            {
                                
                                try
                                {
                                    if (ClientHttpTransport.this.logger.isTraceEnabled())
                                        ClientHttpTransport.this.logger.trace("[HTTP] Flushing {} messages", ClientHttpTransport.this.messagesToSend.size());
                                    
                                    // Don't bother if we have no messages to send...
                                    if (ClientHttpTransport.this.messagesToSend.size() == 0)
                                        return;
                                    
                                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                    
                                    for (TrapMessage m : ClientHttpTransport.this.messagesToSend)
                                    {
                                        bos.write(m.serialize());
                                        ClientHttpTransport.this.delegate.ttMessageSent(m, ClientHttpTransport.this, ClientHttpTransport.this.delegateContext);
                                    }
                                    
                                    byte[] body = bos.toByteArray();
                                    
                                    HttpURLConnection c = (HttpURLConnection) ClientHttpTransport.this.activeUrl.openConnection();
                                    ClientHttpTransport.this.checkInsecure(c);
                                    c.setRequestMethod("POST");
                                    c.setDoOutput(true);
                                    c.setFixedLengthStreamingMode(body.length);
                                    OutputStream os = c.getOutputStream();
                                    os.write(body);
                                    os.flush();
                                    os.close();
                                    int responseCode = c.getResponseCode();
                                    
                                    if (responseCode >= 400)
                                        throw new IOException("Failed sending due to response code: " + responseCode);
                                    ClientHttpTransport.this.messagesToSend.clear();
                                }
                                catch (IOException e)
                                {
                                    LinkedList<TrapMessage> faileds = new LinkedList<TrapMessage>(ClientHttpTransport.this.messagesToSend);
                                    
                                    // Clear the queue. This will cause a NullPointerException 
                                    ClientHttpTransport.this.messagesToSend = null;
                                    throw new TrapTransportException(faileds, ClientHttpTransport.this.state);
                                    
                                }
                            }
                        }
                        catch (TrapTransportException e)
                        {
                            ClientHttpTransport.this.delegate.ttMessagesFailedSending(e.failedMessages, ClientHttpTransport.this, ClientHttpTransport.this.delegateContext);
                            ClientHttpTransport.this.forceError();
                        }
                        finally
                        {
                            
                            synchronized (ClientHttpTransport.this.sendingLock)
                            {
                                if (ClientHttpTransport.this.getState() == TrapTransportState.UNAVAILABLE)
                                    ClientHttpTransport.this.setState(TrapTransportState.AVAILABLE);
                                
                                ClientHttpTransport.this.sending = false;
                            }
                            
                            synchronized (ClientHttpTransport.this.messagesToSend)
                            {
                                if (!ClientHttpTransport.this.messagesToSend.isEmpty())
                                    ClientHttpTransport.this.flushTransport();
                            }
                        }
                    }
                });
            }
        }
    }
    
    protected void setState(TrapTransportState newState)
    {
        if (this.getState() == TrapTransportState.DISCONNECTED || this.getState() == TrapTransportState.ERROR)
            this.running = false;
        super.setState(newState);
    }
    
}
