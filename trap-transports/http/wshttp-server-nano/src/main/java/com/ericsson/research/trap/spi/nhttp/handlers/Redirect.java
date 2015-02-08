package com.ericsson.research.trap.spi.nhttp.handlers;

import com.ericsson.research.trap.nhttpd.IHTTPSession;
import com.ericsson.research.trap.nhttpd.Response;
import com.ericsson.research.trap.nhttpd.Response.Status;
import com.ericsson.research.trap.spi.TrapHostingTransport.TrapHostable;
import com.ericsson.research.trap.spi.nhttp.FullRequestHandler;


public class Redirect extends TrapHostable implements FullRequestHandler
{
    
    private String location;
    
    public Redirect(String location)
    {
        this.location = location;
        
    }
    
    @Override
    public void handle(IHTTPSession request, Response response)
    {
        response.setStatus(Status.REDIRECT);
        response.addHeader("Location", location);
    }
    
    @Override
    public byte[] getBytes()
    {
        // TODO Auto-generated method stub
        return null;
    }
    
}