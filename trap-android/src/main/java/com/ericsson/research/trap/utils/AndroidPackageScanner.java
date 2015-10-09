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

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.HashSet;

import dalvik.system.BaseDexClassLoader;
import dalvik.system.DexFile;
import dalvik.system.PathClassLoader;

public class AndroidPackageScanner extends PackageScanner
{
    
    private static Field dexField;
    private static Field pathField;
    
    static
    {
        
        try
        {
            dexField = PathClassLoader.class.getDeclaredField("mDexs");
            dexField.setAccessible(true);
        }
        catch (Exception e)
        {
            try
            {
                pathField = BaseDexClassLoader.class.getDeclaredField("originalPath");
                pathField.setAccessible(true);
            }
            catch (Exception e1)
            {
                pathField = null;
                dexField = null;
            }
        }
    }
    
    public AndroidPackageScanner() throws Exception
    {
        if (pathField == null && dexField == null)
            throw new NullPointerException("Class not inited");
    }
    
    @Override
    Class<?>[] performScan(String packageName, ClassLoader c) throws IOException
    {
        
        HashSet<Class<?>> rv = new HashSet<Class<?>>();
        
        try
        {
            ClassLoader cl = c;
            
            while (!(cl instanceof PathClassLoader) && cl.getParent() != null && cl.getParent() != cl)
                cl = cl.getParent();
            
            PathClassLoader classLoader = (PathClassLoader) cl;
            
            DexFile[] dexs = new DexFile[] {};
            
            if (dexField != null)
            {
                dexs = (DexFile[]) dexField.get(classLoader);
            }
            
            if (AndroidPackageScanner.pathField != null)
            {
                String path = (String) AndroidPackageScanner.pathField.get(classLoader);
                dexs = new DexFile[] { new DexFile(path) };
            }
            for (DexFile dex : dexs)
            {
                Enumeration<String> entries = dex.entries();
                while (entries.hasMoreElements())
                {
                    String entry = entries.nextElement();
                    
                    if (entry.startsWith(packageName))
                    {
                        
                        try
                        {
                            Class<?> entryClass = cl.loadClass(entry);
                            
                            if (entryClass != null)
                            {
                                rv.add(entryClass);
                            }
                        }
                        catch (Throwable t)
                        {
                        }
                    }
                }
            }
        }
        catch (Throwable e)
        {
            throw new IOException(e);
        }
        
        return rv.toArray(new Class<?>[] {});
    }
    
}
