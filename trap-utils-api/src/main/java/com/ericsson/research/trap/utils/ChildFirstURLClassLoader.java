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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Constructs a class loader that, when asked to load a class, attempts to load it from the provided URLs before it
 * attempts to go to the parent.
 * 
 * @author Vladimir Katardjiev
 * @since 1.1
 */
class ChildFirstURLClassLoader extends URLClassLoader
{
    public ChildFirstURLClassLoader(URL[] urls, ClassLoader parent)
    {
        super(urls, parent);
    }
    
    /**
     * Queries whether the given class should be loaded child first or parent first
     * 
     * @param name
     *            The class name to load @
     */
    protected boolean shouldLoadChildFirst(String name)
    {
        return true;
    }
    
    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
    {
        
        // We have a very specialised ClassLoader.
        // Essentially, we must ONLY load BCU or BC with this ClassLoader. All other classes MUST be parent ones
        // to prevent clashes.
        if (!this.shouldLoadChildFirst(name))
            return super.loadClass(name, resolve);
        
        try
        {
            
            Class<?> findClass;
            ClassLoader current = this;
            ClassLoader parent = this;
            Method m = ClassLoader.class.getDeclaredMethod("findLoadedClass", new Class[] { String.class });
            m.setAccessible(true);
            
            do
            {
                current = parent;
                findClass = (Class<?>) m.invoke(current, name);
                
                if (findClass != null)
                    return findClass;
                
                parent = this.getParent();
                
            } while (parent != null && parent != current);
            
            findClass = this.loadClassFromBytes(name);
            
            if (resolve)
                this.resolveClass(findClass);
            
            return findClass;
        }
        catch (Throwable e)
        {
            return super.loadClass(name, resolve);
        }
    }
    
    Class<?> loadClassFromBytes(String name) throws ClassNotFoundException
    {
        String resName = name.replace('.', '/') + ".class";
        InputStream resStream = this.getResourceAsStream(resName);
        
        ClassLoader current = this;
        ClassLoader parent = this;
        
        do
        {
            current = parent;
            resStream = current.getResourceAsStream(resName);
            parent = this.getParent();
        } while (resStream == null && parent != null && current != parent);
        
        if (resStream == null)
            resStream = ClassLoader.getSystemResourceAsStream(resName);
        
        // Read out the class bytes
        try
        {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(resStream.available());
            byte[] buf = new byte[4096];
            int read;
            
            while ((read = resStream.read(buf)) > -1)
                bos.write(buf, 0, read);
            
            resStream.close();
            
            buf = bos.toByteArray();
            
            Class<?> defined = this.defineClass(name, buf, 0, buf.length);
            return defined;
        }
        catch (IOException e)
        {
            return this.findClass(name);
        }
        
    }
}
