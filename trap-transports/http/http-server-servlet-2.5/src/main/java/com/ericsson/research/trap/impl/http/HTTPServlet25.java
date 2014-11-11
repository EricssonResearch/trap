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

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HTTPServlet25 extends HttpServlet
{
    
    private static final long serialVersionUID = 1L;
    private String            path             = "/_trap";
    private String            ctxPath;
    private int               off              = 0;
    
    public HTTPServlet25()
    {
        
    }
    
    public HTTPServlet25(int off)
    {
        this.off = off;
    }
    
    @Override
    public void init(ServletConfig arg0) throws ServletException
    {
        super.init(arg0);
        ServletContext ctx = this.getServletContext();
        this.init(ctx);
    }
    
    public void init(ServletContext ctx)
    {
        this.ctxPath = ctx.getContextPath();
        this.ctxPath = this.ctxPath + this.path;
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
    
    public void internalService(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException
    {
        
        final CountDownLatch latch = new CountDownLatch(1);
        
        HTTPSession sess = new HTTPSession() {
            
            @Override
            public HttpServletResponse response()
            {
                return resp;
            }
            
            @Override
            public HttpServletRequest request()
            {
                // TODO Auto-generated method stub
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
        
        HTTPServletAdaptor.handle(sess, ctx[0 + this.off], ctx[1 + this.off], ctx[2 + this.off]);
        
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
    }
    
}
