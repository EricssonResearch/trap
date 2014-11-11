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
import java.util.Random;

import com.ericsson.research.transport.ws.WSException;

public class WSHybiFrame extends WSAbstractFrame
{
    
    protected boolean           masked;
    protected boolean           finalFragment;
    private static final Random random = new Random();
    
    protected WSHybiFrame()
    {
    }
    
    public WSHybiFrame(byte type, byte[] payload, boolean masked) throws IOException
    {
        this(type, payload, masked, true);
    }
    
    public WSHybiFrame(byte type, byte[] payload, boolean masked, boolean finalFragment) throws IOException
    {
        super(type);
        this.masked = masked;
        this.finalFragment = finalFragment;
        int j = 0;
        byte b;
        if (payload == null)
            payload = empty;
        j = payload.length;
        if (masked)
        {
            j += 4;
            b = (byte) 0x80;
        }
        else
            b = 0;
        //FIXME: what if payload is null here?
        if (payload.length < 126)
        {
            this.payload = new byte[j + 2];
            b |= payload.length;
            this.payload[1] = b;
        }
        else if (payload.length <= 0xFFFF)
        {
            this.payload = new byte[j + 4];
            b |= 126;
            this.payload[1] = b;
            this.payload[2] = (byte) ((payload.length >>> 8) & 0xFF);
            this.payload[3] = (byte) (payload.length & 0xFF);
        }
        else
        //if (payload.length <= 0x7FFFFFFF) (Payload is always less than Int.MAX from Java)
        {
            this.payload = new byte[j + 10];
            b |= 127;
            this.payload[1] = b;
            for (int i = 56; i >= 0; i = i - 8)
                this.payload[((56 - i) >>> 3) + 2] = (byte) (((long) payload.length >>> i) & 0xFF);
        }
        j = this.payload.length - j;
        this.payload[0] = type;
        if (finalFragment)
            this.payload[0] |= 0x80;
        if (masked)
        {
            byte[] mask = new byte[4];
            random.nextBytes(mask);
            for (int i = 0; i < 4; i++)
                this.payload[j++] = mask[i];
            for (int i = 0; i < payload.length; i++)
            {
                int k = i % 4;
                this.payload[j++] = (byte) (payload[i] ^ mask[k]);
            }
        }
        else
            System.arraycopy(payload, 0, this.payload, j, payload.length);
    }
    
    public int deserialize(byte[] data, int length) throws WSException
    {
        if (this.l1 == -1)
            throw new IllegalStateException("Already deserialized");
        for (;;)
        {
            if (length <= this.pos)
                return 0;
            switch (this.pos)
            {
                case 0:
                    this.finalFragment = ((data[0] & 0x80) == 0x80);
                    this.type = (byte) (data[0] & 0x0F);
                    this.pos++;
                    break;
                case 1:
                    this.masked = ((data[1] & 0x80) == 0x80);
                    this.l1 = (byte) (data[1] & 0x7F);
                    switch (this.l1)
                    {
                        case 127:
                            this.pos += 8;
                            this.l1 = 10;
                            break;
                        case 126:
                            this.pos += 2;
                            this.l1 = 4;
                            break;
                        default:
                            if (this.len == 0)
                            {
                                this.payload = empty;
                                this.l1 = -1;
                                return 2;
                            }
                            this.len = this.l1;
                            this.pos += this.len;
                            this.l1 = 2;
                    }
                    if (this.masked)
                        this.pos += 4;
                    break;
                default:
                    if (this.len == -1)
                    {
                        switch (this.l1)
                        {
                            case 4:
                                this.len = ((data[2] << 8) & 0xFF00) | (data[3] & 0xFF);
                                break;
                            case 10:
                                this.len = data[2];
                                if ((this.len & 0x80) == 0x80)
                                    throw new WSException("Invalid frame length");
                                for (int i = 3; i < 10; i++)
                                    this.len = ((this.len << 8) & 0xFFFFFF00) | (data[i] & 0xFF);
                        }
                        this.pos += this.len;
                    }
                    else
                    {
                        // here l1 points to the position of the mask or payload in the buffer
                        this.payload = new byte[this.len];
                        System.arraycopy(data, this.l1 + (this.masked ? 4 : 0), this.payload, 0, this.len);
                        if (this.masked)
                            for (int i = 0; i < this.len; i++)
                                this.payload[i] = (byte) (this.payload[i] ^ data[this.l1 + (i % 4)]);
                        this.l1 = -1;
                        return this.pos + 1;
                    }
            }
        }
    }
    
}
