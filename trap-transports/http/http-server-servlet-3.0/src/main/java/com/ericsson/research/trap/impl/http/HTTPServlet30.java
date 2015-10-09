package com.ericsson.research.trap.impl.http;

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

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.AsyncContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Define the servlet. 3.0 spec works with annotations. Wohoo!
@WebServlet(value = "/_trap30/*", asyncSupported = true, loadOnStartup = 1)
public class HTTPServlet30 extends HttpServlet
{
    
    private static final long      serialVersionUID = 1L;
    private final transient Logger logger           = LoggerFactory.getLogger(this.getClass().getName());
    private String                 path             = "/_trap30";
    private String                 ctxPath;
    
    // Async is developed, but has a race condition. We'll disable it.
    private static boolean         useAsync         = false;
    
    @Override
    public void init(ServletConfig arg0) throws ServletException
    {
        super.init(arg0);
        ServletContext ctx = this.getServletContext();
        this.ctxPath = ctx.getContextPath() + this.path;
        HTTPServletAdaptor.addServletContext(this.ctxPath);
    }
    
    @Override
    public void destroy()
    {
        HTTPServletAdaptor.removeServletContext(this.ctxPath);
    }
    
    @Override
    protected void service(HttpServletRequest arg0, HttpServletResponse arg1) throws ServletException, IOException
    {
        this.internalService(arg0, arg1);
    }
    
    @Override
    protected void doDelete(HttpServletRequest arg0, HttpServletResponse arg1) throws ServletException, IOException
    {
        this.internalService(arg0, arg1);
    }
    
    @Override
    protected void doGet(HttpServletRequest arg0, HttpServletResponse arg1) throws ServletException, IOException
    {
        this.internalService(arg0, arg1);
    }
    
    @Override
    protected void doPost(HttpServletRequest arg0, HttpServletResponse arg1) throws ServletException, IOException
    {
        this.internalService(arg0, arg1);
    }
    
    @Override
    protected void doPut(HttpServletRequest arg0, HttpServletResponse arg1) throws ServletException, IOException
    {
        this.internalService(arg0, arg1);
    }
    
    void internalService(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException
    {
        this.logger.debug("Beginning request for method {}", req.getMethod());
        if (resp.isCommitted())
            return;
        
        AsyncSession asess;
        final CountDownLatch latch = new CountDownLatch(1);
        
        if (useAsync && "GET".equalsIgnoreCase(req.getMethod()))
        {
            asess = new AsyncSession(req, resp);
        }
        else
        {
            // Fall back to 2.5 handling on non-GET methods.
            
            HTTPSession sess = new HTTPSession() {
                
                @Override
                public HttpServletResponse response()
                {
                    return resp;
                }
                
                @Override
                public HttpServletRequest request()
                {
                    return req;
                }
                
                @Override
                public void finish()
                {
                    latch.countDown();
                }
                
                @Override
                public boolean isFinished()
                {
                    return latch.getCount() <= 0;
                }
            };
            
            // Add CORS headers before we hand over session
            HTTPServletUtil.addCorsHeaders(req, resp);
            String[] ctx = HTTPServletUtil.getContexts(req, this.path);
            HTTPServletAdaptor.handle(sess, ctx[0], ctx[1], ctx[2]);
            
            try
            {
                latch.await(30, TimeUnit.SECONDS);
            }
            catch (InterruptedException e)
            {
                // Do nothing. This is HTTP timeout. Protocol will handle it.
                e.printStackTrace();
                resp.setStatus(200);
            }
            return;
            
        }
        
        // Add CORS headers before we hand over session
        HTTPServletUtil.addCorsHeaders(req, resp);
        String[] ctx = HTTPServletUtil.getContexts(req, this.path);
        asess.asyncContext = req.startAsync();
        asess.asyncContext.setTimeout(30000);
        HTTPServletAdaptor.handle(asess, ctx[0], ctx[1], ctx[2]);
        
    }
    
    class AsyncSession implements HTTPSession
    {
        
        AsyncContext        asyncContext = null;
        AtomicInteger       latch        = new AtomicInteger(1);
        HttpServletRequest  req          = null;
        HttpServletResponse resp         = null;
        
        public AsyncSession(HttpServletRequest req, HttpServletResponse resp)
        {
            this.req = req;
            this.resp = resp;
        }
        
        @Override
        public HttpServletResponse response()
        {
            if (this.asyncContext != null)
                return (HttpServletResponse) this.asyncContext.getResponse();
            return this.resp;
        }
        
        @Override
        public HttpServletRequest request()
        {
            if (this.asyncContext != null)
                return (HttpServletRequest) this.asyncContext.getRequest();
            return this.req;
        }
        
        @Override
        public void finish()
        {
            try
            {
                if (this.latch.decrementAndGet() == 0)
                {
                    if (this.asyncContext != null)
                        this.asyncContext.complete();
                    HTTPServlet30.this.logger.trace("AsyncContext complete...");
                }
                else
                {
                    HTTPServlet30.this.logger.trace("Duplicated finish called on async. Someone's calling excessive finishes...");
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        
        @Override
        public boolean isFinished()
        {
            return this.latch.get() <= 0;
        }
        
    }
}
