package com.ericsson.research.trap.spi.nhttp.handlers;

import com.ericsson.research.trap.nhttpd.Request;
import com.ericsson.research.trap.nhttpd.RequestHandler;
import com.ericsson.research.trap.nhttpd.Response;
import com.ericsson.research.trap.nhttpd.StatusCodes;
import com.ericsson.research.trap.spi.TrapHostingTransport.TrapHostable;


public class Redirect extends TrapHostable implements RequestHandler
{
    
    private String location;
    
    public Redirect(String location)
    {
        this.location = location;
        
    }
    
    @Override
    public void handleRequest(Request request, Response response)
    {
        response.setStatus(StatusCodes.FOUND);
        response.addHeader("Location", location);
    }
    
    @Override
    public byte[] getBytes()
    {
        // TODO Auto-generated method stub
        return null;
    }
    
}