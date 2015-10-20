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

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @hide
 * @author Vladimir Katardjiev
 * @since 1.0
 */
class PackageScannerImpl extends PackageScanner
{
    
    static class CacheKey
    {
        
        private int code;
        
        public CacheKey(String packageName, ClassLoader c)
        {
            this.code = packageName.hashCode() + c.hashCode();
        }
        
        @Override
        public int hashCode()
        {
            // TODO Auto-generated method stub
            return this.code;
        }
        
        public boolean equals(Object other)
        {
            return (other instanceof CacheKey) && (other.hashCode() == this.code);
        }

    }
    
    LRUCache<CacheKey, Class<?>[]> cache = LRUCache.createCache();
    
    public Class<?>[] performScan(String packageName, ClassLoader c) throws IOException
    {
        CacheKey key = new CacheKey(packageName, c);
        Class<?>[] value = this.cache.get(key);
        if (value != null)
            return value;
        
        HashSet<Class<?>> classes = new HashSet<Class<?>>();
        // Get a File object for the package
        String relPath = packageName.replace('.', '/');
        Enumeration<URL> resources = c.getResources(relPath);
        
        while (resources.hasMoreElements())
            this.scanPackage(classes, resources.nextElement(), packageName);
        
        value = classes.toArray(new Class[0]);
        this.cache.put(key, value);
        
        return value;
        
    }
    
    public void scanPackage(HashSet<Class<?>> outSet, URL packageUrl, String packageName) throws MalformedURLException
    {
        //System.err.println("Now scanning [" + packageUrl + "] [" + packageName + "]");
        File directory;
        String packagePath = packageName.replace('.', '/');
        String path = packageUrl.getFile();
        
        try
        {
            // this is shit
            try
            {
                directory = new File(new URI(packageUrl.toString()));
            }
            catch (URISyntaxException e)
            {
                directory = new File(packageUrl.getPath());
            }
            catch (IllegalArgumentException e)
            {
                directory = new File(URLDecoder.decode(packageUrl.getFile(), "UTF-8"));
            }
        }
        catch (IllegalArgumentException e)
        {
            directory = null;
        }
        catch (UnsupportedEncodingException e)
        {
            directory = null;
        }
        
        if ((directory != null) && directory.exists())
        {
            // Get the list of the files contained in the package
            String[] files = directory.list();
            if (files == null)
                return;
            for (int i = 0; i < files.length; i++)
            {
                // we are only interested in .class files
                if (files[i].endsWith(".class"))
                {
                    // removes the .class extension
                    String className = packageName + '.' + files[i].substring(0, files[i].length() - 6);
                    try
                    {
                        outSet.add(Class.forName(className));
                    }
                    catch (Throwable e)
                    {
                        //System.err.println("ClassNotFoundException loading " + className);
                    }
                }
                else
                {
                    // Recurse!
                    this.scanPackage(outSet, new URL(directory.toURI().toURL().toString() + "/" + files[i]), packageName + "." + files[i]);
                }
            }
        }
        else
        {
            try
            {
                String jarPath = path.replaceFirst("[.]jar[!].*", ".jar").replaceFirst("file:", "");
                jarPath = jarPath.replaceAll("%20", " ");
                JarFile jarFile = new JarFile(jarPath);
                try
                {
                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements())
                    {
                        JarEntry entry = entries.nextElement();
                        String entryName = entry.getName();
                        
                        if (entryName.endsWith("/"))
                            continue; // Package
                            
                        if (entryName.startsWith(packagePath) && (entryName.length() > (packagePath.length() + "/".length())))
                        {
                            String className = entryName.replace('/', '.').replace('\\', '.');
                            if (className.endsWith(".class"))
                                className = className.substring(0, className.length() - 6);
                            try
                            {
                                outSet.add(Class.forName(className));
                            }
                            catch (ClassNotFoundException e)
                            {
                                // throw new
                                // RuntimeException("ClassNotFoundException loading "
                                // + className);
                            }
                            catch (NoClassDefFoundError e)
                            {
                                // A class was found, but it depends on a class that isn't found.
                            }
                        }
                    }
                }
                finally
                {
                    jarFile.close();
                }
            }
            catch (IOException e)
            {
                System.err.println(packageName + " (" + directory + ") does not appear to be a valid package");
            }
        }
    }
}
