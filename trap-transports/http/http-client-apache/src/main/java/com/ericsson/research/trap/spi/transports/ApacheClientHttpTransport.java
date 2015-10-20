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
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.spi.TrapMessage;
import com.ericsson.research.trap.spi.TrapTransportException;
import com.ericsson.research.trap.spi.TrapTransportPriority;
import com.ericsson.research.trap.spi.TrapTransportProtocol;
import com.ericsson.research.trap.spi.TrapTransportState;

public class ApacheClientHttpTransport extends AbstractTransport implements Runnable
{
    
    protected URI                    connectUrl;
    protected URI                    activeUrl;
    protected boolean                running         = true;
    LinkedBlockingQueue<TrapMessage> messagesToSend  = new LinkedBlockingQueue<TrapMessage>();
    protected long                   expirationDelay = 28000;
    private DefaultHttpClient        postclient;
    
    public ApacheClientHttpTransport()
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
                    this.logger.trace("No URL specified for HTTP transport.");
                }
                else
                {
                    URI uri = URI.create(url);
                    this.connectUrl = uri;
                }
            }
            else
                this.logger.debug("Updating HTTP configuration while open; changes will not take effect until HTTP is reconnected");
        }
        catch (IllegalArgumentException e)
        {
            if (this.isEnabled())
            {
                this.logger.debug("Invalid configuration for HTTP transport. The transport is disabled");
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
    }
    
    @Override
    public void fillAuthenticationKeys(HashSet<String> keys)
    {
        super.fillAuthenticationKeys(keys);
    }
    
    @Override
    public void internalSend(TrapMessage message, boolean expectMore) throws TrapTransportException
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
            this.logger.debug("HTTP Transport not properly configured... Unless autoconfigure is enabled (and another transport succeeds) this transport will not be available.");
            this.setState(TrapTransportState.ERROR);
            return;
        }
        
        try
        {
            this.running = true;
            HttpClient httpclient = new DefaultHttpClient();
            HttpGet httpget = this.openGet(this.connectUrl);
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            if (entity != null)
            {
                InputStream is = null;
                InputStreamReader isr = null;
                try
                {
                    is = entity.getContent();
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
                    
                    this.activeUrl = URI.create(urlBase);
                    
                    // Start a new thread for polling
                    Thread t = new Thread(this);
                    t.setDaemon(true);
                    t.start();
                    
                    this.postclient = new DefaultHttpClient();
                    
                    // Change state to connected
                    this.setState(TrapTransportState.CONNECTED);
                }
                finally
                {
                    if (is != null)
                        is.close();
                    
                    if (isr != null)
                        isr.close();
                }
            }
            
        }
        catch (Exception e)
        {
            this.setState(TrapTransportState.ERROR);
            throw new TrapException(e);
        }
        
    }
    
    @Override
    protected void internalDisconnect()
    {
        
        synchronized (this.messagesToSend)
        {
            if (this.messagesToSend.size() > 0)
                try
                {
                    this.internalSend(null, false);
                }
                catch (TrapTransportException e)
                {
                    e.printStackTrace();
                }
        }
        
        this.running = false;
        try
        {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost post = new HttpPost(this.activeUrl);
            HttpResponse response = httpclient.execute(post);
            response.getEntity();
            InputStream is = response.getEntity().getContent();
            
            @SuppressWarnings("unused")
            int read = 0;
            while ((read = is.read()) != -1)
                ;
            is.close();
        }
        catch (Exception e)
        {
        }
        this.setState(TrapTransportState.DISCONNECTED);
    }
    
    @Override
    public boolean canConnect()
    {
        return true;
    }
    
    protected byte[] readBuf = new byte[4096];
    
    @Override
    public void run()
    {
        
        this.logger.trace("HTTP Transport entering polling loop");
        HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = null;
        HttpResponse response = null;
        int responseCode = -1;
        while (this.running)
        {
            try
            {
                
                httpget = this.openGet(this.activeUrl);
                response = httpclient.execute(httpget);
                
                responseCode = response.getStatusLine().getStatusCode();
                
                if (responseCode >= 300)
                {
                    this.logger.debug("Closing due to non-200 code; Transport status is {}", this.getState());
                    
                    if ((this.getState() != TrapTransportState.DISCONNECTED) && (this.getState() != TrapTransportState.ERROR) && (this.getState() != TrapTransportState.DISCONNECTING))
                        this.setState(TrapTransportState.ERROR);
                    return;
                }
                
                if (responseCode == 201)
                {
                    InputStream is = response.getEntity().getContent();
                    
                    int read = 0;
                    while ((read = is.read(this.readBuf)) != -1)
                    {
                        this.receive(this.readBuf, 0, read);
                    }
                    
                    is.close();
                }
                else
                {
                    httpget.abort();
                }
            }
            catch (Exception e)
            {
                //e.printStackTrace();
                this.logger.debug("Moving to state ERROR due to exception", e);
                this.setState(TrapTransportState.ERROR);
            }
        }
        this.logger.trace("HTTP Transport exiting polling loop. Running is false.");
    }
    
    protected HttpGet openGet(URI cUrl) throws IOException
    {
        /*
         * When the URL is formatted as
         *
         * 	http://localhost:8088
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
        return new HttpGet(uri);
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
    
    @Override
    public void flushTransport()
    {
        synchronized (this.messagesToSend)
        {
            try
            {
                if (this.logger.isTraceEnabled())
                    this.logger.trace("[HTTP] Flushing {} messages", this.messagesToSend.size());
                
                // Don't bother if we have no messages to send...
                if (this.messagesToSend.size() == 0)
                    return;
                
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                
                for (TrapMessage m : this.messagesToSend)
                {
                    bos.write(m.serialize());
                }
                
                byte[] body = bos.toByteArray();
                
                HttpPost post = new HttpPost(this.activeUrl);
                
                ByteArrayEntity bae = new org.apache.http.entity.ByteArrayEntity(body);
                post.setEntity(bae);
                
                HttpResponse response = this.postclient.execute(post);
                int responseCode = response.getStatusLine().getStatusCode();
                HttpEntity entity = response.getEntity();
                if (entity != null)
                {
                    InputStream is = entity.getContent();
                    
                    @SuppressWarnings("unused")
                    int read = 0;
                    while ((read = is.read()) != -1)
                        ;
                    is.close();
                }
                
                if (responseCode >= 400)
                    throw new IOException("Failed sending due to response code: " + responseCode);
                this.messagesToSend.clear();
            }
            catch (IOException e)
            {
                e.printStackTrace();
                //throw new TrapTransportException(message, this.state);
                this.delegate.ttMessagesFailedSending(this.messagesToSend, this, this.delegateContext);
                this.messagesToSend.clear();
            }
            finally
            {
            }
        }
    }
    
}
