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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.WeakHashMap;

/**
 * Utility class to scan the classes in a certain package. Useful for automatic detection of loaded libraries. There are
 * a number of package scanners included in Trap, and explained below.
 * <p>
 * <ul>
 * <li>JBossVFSPackageScanner -- Allows Package Scanning functionality in JBoss containers.
 * <li>AndroidPackageScanner -- Performs package scanning for Android (Pre 4.4. Post 4.4, this functionality is broken)
 * <li>PackageScannerImpl -- Uses Java File APIs to scan for packages. This generally only works on desktop JVMs, and
 * not in containers.
 * <li>PackageScannerFile -- Uses a trap-packages.txt index file to list all classes known. Use the
 * trap-index-maven-plugin in your workflow to scan for packages at compile time instead of runtime
 * <li>PackageScannerNull -- A null package scanner that will allow Trap to run. Automatic class detection will not
 * work.
 * </ul>
 * 
 * @author Vladimir Katardjiev
 */
public abstract class PackageScanner
{
    
    private static PackageScanner instance;
    
    static
    {
        // Allows forward declaration of multiple package scanners on classpath. Sadly, we don't have any such here, but that's irrelevant right now.
        // @formatter:off
		String[] scanners = new String[] { 
				"com.ericsson.research.trap.utils.JBossVFSPackageScanner", // Package scanner for use with JBoss. Dependency is optional, so classloader will fail to load it if not in JBoss
				"com.ericsson.research.trap.utils.AndroidPackageScanner",
                PackageScanner.class.getName() + "Impl",
                PackageScanner.class.getName() + "File",
                PackageScanner.class.getName() + "Null"  
			};
		// @formatter:on
        
        for (int i = 0; i < scanners.length; i++)
        {
            try
            {
                
                Class<?> c = Class.forName(scanners[i]);
                instance = (PackageScanner) c.newInstance();
                
                // Verify this scanner works by scanning for ourselves
                Class<?>[] arr = instance.performScan(instance.getClass().getPackage().getName(), instance.getClass().getClassLoader());
                
                // Iterate through the array and verify we're in the array
                boolean found = false;
                for (Class<?> c1 : arr)
                    if (instance.getClass().equals(c1))
                    {
                        found = true;
                        break;
                    }
                
                if (!found && !(instance instanceof PackageScannerNull))
                {
                    instance = null;
                }
                else
                    break;
                
            }
            catch (Throwable t)
            {
                //t.printStackTrace();
            }
            
        }
        
        if (instance == null)
            System.err.println("WARNING: Could not initialise PackageScanner. Either no package scanners were found, or none of the scanners could pass the initial test. Bad Things(tm) can happen in this environment. Please ensure anything that scans for classes does not need to do so (or fix the scanner for this environment)");
    }
    
    /**
     * Scan for classes in the given package using the system class loader.
     * 
     * @param packageName
     *            The package to scan
     * @return An array of classes found in the package. May be empty.
     * @throws IOException
     *             If an IO exception pre-empted scanning.
     */
    public static Class<?>[] scan(String packageName) throws IOException
    {
        return instance.performScan(packageName, ClassLoader.getSystemClassLoader());
    }
    
    static WeakHashMap<ClassLoader, HashMap<String, Class<?>[]>> cache = new WeakHashMap<ClassLoader, HashMap<String, Class<?>[]>>();
    
    /**
     * Scans for classes in the given package using the supplied classloader. This method will sort the returned classes
     * by name, ensuring a consistent result every time. Scans will be cached, so subsequent scans for the same
     * 
     * @param packageName
     *            The package to scan. This will include any eventual subpackages, so, e.g. scanning "com" is a very
     *            very bad idea.
     * @param cl
     *            The ClassLoader to use
     * @return An array of classes found in the package. May be empty. Will not be null.
     * @throws IOException
     *             If an IO exception pre-empted scanning.
     */
    public static Class<?>[] scan(String packageName, ClassLoader cl) throws IOException
    {
        HashMap<String, Class<?>[]> clCache;
        Class<?>[] rv;
        synchronized (cache)
        {
            clCache = cache.get(cl);
            if (clCache == null)
            {
                clCache = new HashMap<String, Class<?>[]>();
                cache.put(cl, clCache);
            }
        }
        
        synchronized (clCache)
        {
            rv = clCache.get(packageName);
        }
        
        if (rv == null)
        {
            rv = instance.performScan(packageName, cl);
            Arrays.sort(rv, new Comparator<Class<?>>() {
                
                @Override
                public int compare(Class<?> o1, Class<?> o2)
                {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            clCache.put(packageName, rv);
        }
        
        return rv;
    }
    
    abstract Class<?>[] performScan(String packageName, ClassLoader c) throws IOException;
    
    PackageScanner()
    {
    }
}
