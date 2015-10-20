package com.ericsson.research.trap.utils;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;

class PackageScannerFile extends PackageScanner
{
    
    HashSet<String> classNames = new HashSet<String>();
    
    public PackageScannerFile() throws IOException
    {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("trap-packages.txt");
        InputStreamReader isr = null;
        BufferedReader br = null;
        
        if (is == null)
            throw new InstantiationError("No packages index file included in the classpath");
        try
        {
            isr = new InputStreamReader(is, Charset.forName("UTF-8"));
            br = new BufferedReader(isr);
            
            String line;
            while ((line = br.readLine()) != null)
                this.classNames.add(line);
        }
        finally
        {
            is.close();
            
            if (br != null)
                br.close();
            
            if (isr != null)
                isr.close();
        }
    }
    
    @Override
    Class<?>[] performScan(String packageName, ClassLoader c) throws IOException
    {
        
        ArrayList<Class<?>> rv = new ArrayList<Class<?>>();
        
        for (String name : this.classNames)
        {
            try
            {
                Class<?> cs = c.loadClass(name);
                rv.add(cs);
            }
            catch (Throwable t)
            {
            }
        }
        
        return rv.toArray(new Class[] {});
    }
    
}
