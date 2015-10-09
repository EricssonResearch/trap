package com.ericsson.research.trap.spi;

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



import java.net.URI;

/**
 * Defines interfaces for transports that are capable of hosting files. This allows an application to sideload
 * information on transports, instead of having to set up its own hosting setup. The primary use-case for hosting
 * transports is when the main application wants to provide some file/data (e.g. setup JavaScript) without having to
 * provide its own hosting interface. HTTP transports should implement this interface.
 * <p>
 * This interface is NOT intended as a replacement to the Servlet API or similar containers, and should only be used
 * when it is too expensive to set up a proper container. Additionally, it is not intended for dynamic content, or
 * anything advanced really.
 * 
 * @author Vladimir Katardjiev
 * @since 1.1
 */
public interface TrapHostingTransport extends TrapTransport
{
    
    /**
     * Interface for objects hosted by a transport. This interface simply asks for the data, with no context.
     * 
     * @author Vladimir Katardjiev
     */
    public abstract class TrapHostable
    {
        
        protected String mimeType = "application/octet-stream";
        private URI uri;
        
        /**
         * Creates a new TrapHostable instance with the default mime type of <code>application/octet-stream</code>.
         */
        public TrapHostable()
        {
        }
        
        /**
         * Creates a new TrapHostable instance with the mime type supplied.
         * @param mimeType The mime type to use for requests.
         */
        public TrapHostable(String mimeType)
        {
            this.mimeType = mimeType;
        }
        
        /**
         * Get the content type that should annotate the transfer.
         * 
         * @return A string representing the MIME/Content type.
         */
        public String getContentType()
        {
            return mimeType;
        };
        
        /**
         * Get the bytes that should be delivered to the client.
         * 
         * @return The hosted object's bytes.
         */
        public abstract byte[] getBytes();
        
        /**
         * Called when this object is removed from the hosting transport. Generally, this means the transport has gone
         * away.
         */
        public void notifyRemoved()
        {
        };
        
        public URI uri() {
            return this.uri;
        }
        
        public void setURI(URI uri) {
            this.uri = uri;
        }
    }
    
    /**
     * Adds an object to be hosted. The hosted object will be accessible from the URI returned. The URI type is
     * transport dependent; an HTTP transport would return an HTTP URI, other transports may have other types.
     * <p>
     * The URI returned will be pseudo-random. It is impossible to guarantee any specific URI, format or path, as all
     * depend on the specific transport quantities.
     * <p>
     * The hosted object will be weakly referenced. To remove the object, simply let it get GC'd.
     * 
     * @param hosted
     *            The object to host.
     * @param preferredName
     *            A human readable name at which this object would prefer to be hosted. The path is not guaranteed, but
     *            it may be more predictable than purely random. May be <i>null</i>, in which case a random path is
     *            used.
     * @return The URI at which the object's data can be accessed remotely.
     */
    public URI addHostedObject(TrapHostable hosted, String preferredName);
    
    /**
     * Accessor for the protocol implemented by the hosting transport.
     * @return A value from {@link TrapTransportProtocol}.
     */
    public String getProtocolName();
    
}
