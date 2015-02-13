package com.ericsson.research.trap.spi.nhttp.handlers;

import java.io.InputStream;

import com.ericsson.research.trap.nhttpd.Request;
import com.ericsson.research.trap.nhttpd.RequestHandler;
import com.ericsson.research.trap.nhttpd.Response;
import com.ericsson.research.trap.spi.TrapHostingTransport.TrapHostable;


public class Resource extends TrapHostable implements RequestHandler
{
    
    private ResourceStreamSource src;
    
    public interface ResourceStreamSource
    {
        InputStream getStream();
    }
    
    public class ClassLoaderStreamSource implements ResourceStreamSource
    {
        
        private ClassLoader c;
        private String      resource;
        
        public ClassLoaderStreamSource(Class<?> c, String resource)
        {
            this.c = c.getClassLoader();
            this.resource = resource;
        }
        
        @Override
        public InputStream getStream()
        {
            return c.getResourceAsStream(resource);
        }
    }
    
    public Resource(ResourceStreamSource src)
    {
        this.src = src;
    }

    @Override
    public void handleRequest(Request request, Response response)
    {
        InputStream is = src.getStream();
        
        if (is == null)
        {
            response.setStatus(404);
            response.setData("Not Found");
            return;
        }
        
        response.setStatus(200);
        response.setData(is);
    }
    
    @Override
    public byte[] getBytes()
    {
        return null;
    }
    
}