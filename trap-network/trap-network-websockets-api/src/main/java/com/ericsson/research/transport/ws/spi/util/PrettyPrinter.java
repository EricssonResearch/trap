package com.ericsson.research.transport.ws.spi.util;

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


import com.ericsson.research.trap.utils.StringUtil;

public class PrettyPrinter {
	
	public static String toHexString(byte[] bytes, int length) {
	    StringBuffer sb = new StringBuffer();
	    
	    boolean binary = false;
	    for(int i=0;i<length;i++)
			if(bytes[i]<0 || Character.isISOControl(bytes[i]) && bytes[i]!=0x0d && bytes[i]!=0x0a) {
				binary = true;
	    		break;
			}

	    if(binary) {
		    for(int i=0;i<length;i++) {
		    	String hex = Integer.toHexString(bytes[i]).toUpperCase();
				if(hex.length()>2)
					hex = hex.substring(hex.length()-2);
		    	if(hex.length()<2)
		    		hex = "0"+hex;
		    	sb.append("[0x").append(hex).append("]");
			}
	    } else
	    	sb.append(StringUtil.toUtfString(bytes, 0, length));
	    return sb.toString();
	}

}
