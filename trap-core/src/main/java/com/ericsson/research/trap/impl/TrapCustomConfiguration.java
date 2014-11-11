package com.ericsson.research.trap.impl;

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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

public class TrapCustomConfiguration extends TrapConfigurationImpl
{
    
    protected TrapConfigurationImpl staticConfig;
    
    public TrapCustomConfiguration(String configuration)
    {
        this.setStaticConfiguration(configuration);
    }
    
    void setStaticConfiguration(String configuration)
    {
        this.staticConfig = new TrapConfigurationImpl(configuration);
    }
    
    public Map<String, String> getOptions(final String optionsPrefix, final boolean cutPrefixes)
    {
        Map<String, String> options = this.createPuttableGettableMap(optionsPrefix, cutPrefixes);
        options.putAll(this.staticConfig.getOptions(optionsPrefix, cutPrefixes));
        options.putAll(super.getOptions(optionsPrefix, cutPrefixes));
        return options;
    }
    
    public String getOption(String option)
    {
        String val = super.getOption(option);
        if (val == null)
            val = this.staticConfig.getOption(option);
        return val;
    }
    
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        HashSet<String> join = new HashSet<String>(this.staticConfig.getConfig().keySet());
        Map<String, String> cfg = this.getConfig();
        synchronized (cfg)
        {
            join.addAll(cfg.keySet());
        }
        Object[] keys = join.toArray();
        Arrays.sort(keys);
        for (int i = 0; i < keys.length; i++)
        {
            String key = keys[i].toString();
            sb.append(key);
            sb.append(" = ");
            sb.append(this.getOption(key));
            sb.append("\n");
        }
        return sb.toString();
    }
    
}
