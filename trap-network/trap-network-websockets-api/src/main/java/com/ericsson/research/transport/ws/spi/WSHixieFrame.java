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

import com.ericsson.research.transport.ws.WSException;

public class WSHixieFrame extends WSAbstractFrame
{
    
    private static final byte[] CLOSE_PAYLOAD = new byte[] { (byte) 0xFF, 0 };
    
    protected WSHixieFrame()
    {
    }
    
    public WSHixieFrame(byte type, byte[] payload)
    {
        super(type);
        switch (type)
        {
            case BINARY_FRAME:
                int j = 1;
                for (int i = 28; i >= 0; i = i - 7)
                {
                    byte b = (byte) ((payload.length >>> i) & 0x7F);
                    if (b > 0 || j > 1 || i == 0)
                    {
                        if (i > 0)
                            b |= 0x80;
                        if (j == 1)
                            this.payload = new byte[payload.length + 2 + (i / 7)];
                        this.payload[j++] = b;
                    }
                }
                this.payload[0] = (byte) 0x80;
                System.arraycopy(payload, 0, this.payload, j, payload.length);
                break;
            case TEXT_FRAME:
                this.payload = new byte[payload.length + 2];
                this.payload[0] = 0;
                System.arraycopy(payload, 0, this.payload, 1, payload.length);
                this.payload[payload.length + 1] = (byte) 0xFF;
                break;
            case CLOSE_FRAME:
                this.payload = CLOSE_PAYLOAD;
                break;
                
                default:
                    throw new IllegalStateException("Invalid payload type");
        }
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
                    if ((data[0] & 0x80) == 0)
                        this.type = TEXT_FRAME;
                    else if ((data[0] & 0x7F) == 0)
                        this.type = BINARY_FRAME;
                    else
                        this.type = CLOSE_FRAME;
                    this.pos++;
                    break;
                case 1:
                    if (this.type == CLOSE_FRAME)
                    {
                        this.l1 = -1;
                        return 2;
                    }
                default:
                    switch (this.type)
                    {
                        case BINARY_FRAME:
                            if (this.len == -1)
                            {
                                if ((data[this.pos] & 0x80) == 0)
                                {
                                    this.len = data[1] & 0x7F;
                                    for (int i = 2; i <= this.pos; i++)
                                        this.len = (this.len << 7) | (data[i] & 0x7F);
                                    this.l1 = (byte) (this.pos + 1);
                                    this.pos += this.len;
                                    continue;
                                }
                            }
                            else
                            {
                                this.payload = new byte[this.len];
                                if (this.len > 0)
                                    System.arraycopy(data, this.l1, this.payload, 0, this.len);
                                this.l1 = -1;
                                return this.pos + 1;
                            }
                            break;
                        case TEXT_FRAME:
                            if (data[this.pos] == (byte) 0xFF)
                            {
                                this.payload = new byte[this.pos - 1];
                                if (this.pos > 1)
                                    System.arraycopy(data, 1, this.payload, 0, this.pos - 1);
                                this.l1 = -1;
                                return this.pos + 1;
                            }
                            break;
                    }
                    this.pos++;
            }
        }
    }
    
}
