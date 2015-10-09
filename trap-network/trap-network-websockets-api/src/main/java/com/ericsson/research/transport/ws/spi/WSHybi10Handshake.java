package com.ericsson.research.transport.ws.spi;

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
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Random;

import com.ericsson.research.transport.ws.WSException;
import com.ericsson.research.trap.utils.Base64;

public class WSHybi10Handshake extends WSAbstractHandshake implements WSConstants
{
    
    protected static final String SERVER_PREAMBLE = HTTP11 + " 101 Switching Protocols";
    protected String              key;
    protected static final Random random          = new Random();
    
    public WSHybi10Handshake(WSAbstractProtocol protocol)
    {
        super(protocol);
    }
    
    public void sendHandshake(OutputStream os) throws IOException
    {
        StringBuffer h = new StringBuffer();
        if (this.protocol.client)
        {
            h.append(GET);
            h.append(" ");
            h.append(this.protocol.resource);
            h.append(" ");
            h.append(HTTP11);
            h.append(WSHixie75Handshake.COMMON_PART);
            h.append(HOST_HEADER);
            h.append(COLON_SPACE);
            h.append(this.protocol.host);
            if ((this.protocol.securityContext == null && this.protocol.port != 80) || (this.protocol.securityContext != null && this.protocol.port != 443))
            {
                h.append(":");
                h.append(this.protocol.port);
            }
            h.append(CRLF);
            h.append(SEC_WEBSOCKET_ORIGIN_HEADER);
            h.append(COLON_SPACE);
            h.append(this.protocol.origin);
            h.append(CRLF);
            h.append(SEC_WEBSOCKET_VERSION_HEADER);
            h.append(COLON_SPACE);
            h.append(8);
            h.append(CRLF);
            h.append(SEC_WEBSOCKET_KEY_HEADER);
            h.append(COLON_SPACE);
            byte[] randomBytes = new byte[16];
            random.nextBytes(randomBytes);
            this.key = new String(Base64.encode(randomBytes));
            h.append(this.key);
            h.append(CRLFCRLF);
        }
        else
        {
            h.append(SERVER_PREAMBLE);
            h.append(WSHixie75Handshake.COMMON_PART);
            h.append(SEC_WEBSOCKET_ACCEPT_HEADER);
            h.append(COLON_SPACE);
            String nounceAndGUID = this.key + GUID;
            try
            {
                byte[] sha1 = this.computeSHA1(nounceAndGUID);
                h.append(Base64.encode(sha1));
                h.append(CRLFCRLF);
            }
            catch (WSException e)
            {
                throw new IOException(e);
            }
        }
        synchronized (os)
        {
            os.write(h.toString().getBytes(ISO_8859_1));
            os.flush();
        }
    }
    
    public void headersRead() throws WSException
    {
        if (this.protocol.client)
        {
            byte[] expected = this.computeSHA1(this.key + GUID);
            if (this.getHeader(SEC_WEBSOCKET_ACCEPT_HEADER) == null)
                throw new WSException("Missing " + SEC_WEBSOCKET_ACCEPT_HEADER + " header");
            byte[] actual = Base64.decode(this.getHeader(SEC_WEBSOCKET_ACCEPT_HEADER));
            if (!Arrays.equals(expected, actual))
                throw new WSException("Failed to validate " + SEC_WEBSOCKET_ACCEPT_HEADER + " header value");
            this.protocol.protocol = this.getHeader(SEC_WEBSOCKET_PROTOCOL_HEADER);
        }
        else
        {
            if (this.getHeader(HOST_HEADER) == null)
                throw new WSException("Missing " + HOST_HEADER + " header");
            if (!"8".equals(this.getHeader(SEC_WEBSOCKET_VERSION_HEADER)))
                throw new WSException("Unsupported version " + this.getHeader(SEC_WEBSOCKET_VERSION_HEADER));
            this.key = this.getHeader(SEC_WEBSOCKET_KEY_HEADER);
            if (this.key == null)
                throw new WSException("Missing " + SEC_WEBSOCKET_KEY_HEADER + " header");
        }
    }
    
}
