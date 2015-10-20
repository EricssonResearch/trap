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
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;

import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;

public class JBossVFSPackageScanner extends PackageScanner
{
    
    @Override
    Class<?>[] performScan(final String packageName, final ClassLoader c) throws IOException
    {
        try
        {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>[]>() {
                public Class<?>[] run() throws Exception
                {
                    
                    HashSet<Class<?>> result = new HashSet<Class<?>>();
                    return JBossVFSPackageScanner.this.recursiveScan(packageName, c, result, new ChildFirstURLClassLoader(new URL[] {}, c)).toArray(new Class[] {});
                }
            });
        }
        catch (PrivilegedActionException e)
        {
            throw new IOException(e);
        }
    }
    
    HashSet<Class<?>> recursiveScan(String packageName, ClassLoader c, HashSet<Class<?>> result, ChildFirstURLClassLoader cfl) throws IOException
    {
        
        try
        {
            final String pkgPath = packageName.replace('.', '/');
            Enumeration<?> resources = c.getResources(pkgPath);
            
            while (resources.hasMoreElements())
            {
                URL resource = (URL) resources.nextElement();
                
                if (resource == null)
                {
                    System.err.println("Not found: " + pkgPath);
                    continue;
                }
                
                VirtualFile file = VFS.getChild(resource.toURI());
                
                List<VirtualFile> children = file.getChildren();
                for (VirtualFile child : children)
                {
                    if (child.isFile())
                    {
                        try
                        {
                            Class<?> clazz = c.loadClass(packageName.concat(".").concat(child.getName().split("\\.")[0]));
                            result.add(clazz);
                        }
                        catch (Throwable e)
                        {
                            // do_nothing_loop()
                        }
                    }
                    else if (child.isDirectory())
                    {
                        this.recursiveScan(packageName + "." + child.getName(), c, result, cfl);
                    }
                }
            }
            
            return result;
        }
        catch (URISyntaxException e)
        {
            throw new IOException(e);
        }
    }
    
}

class SilentClassLoader extends ClassLoader
{
    public SilentClassLoader(ClassLoader parent)
    {
        super(parent);
    }
}
