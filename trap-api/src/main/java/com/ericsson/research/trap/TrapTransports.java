package com.ericsson.research.trap;

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
import java.util.HashSet;

import com.ericsson.research.trap.spi.TrapTransport;
import com.ericsson.research.trap.utils.PackageScanner;

/**
 * Container for the automatic detection of transports. This class is primarily exposed for compatibility with platforms
 * where automatic detection fails, e.g. Android. In such cases, applications can manually add the transport classes.
 * 
 * @author Vladimir Katardjiev
 * @since 1.0
 */
public final class TrapTransports
{
    
    // Prevent instantiation. This is a true static class.
    private TrapTransports()
    {
    }
    
    private static HashSet<Class<? extends TrapTransport>> transportClasses   = new HashSet<Class<? extends TrapTransport>>();
    private static HashSet<Class<?>>                       blacklistedClasses = new HashSet<Class<?>>();
    private static boolean                                 useAutodiscover    = true;
    
    /**
     * Generates an array of transport classes for the given classloader, plus the classes added using
     * {@link #addTransportClass(Class)}. <i>Note that this may cause classes loaded with multiple classloaders to be
     * present, if the classes added manually do not correspond with the given classloader</i>.
     * 
     * @param cl
     *            The classloader to use to scan packages.
     * @return An array of classes detected using automatic scanning, plus the classes added manually.
     * @throws IOException
     *             If scanning was not possible.
     */
    @SuppressWarnings("unchecked")
    public static Class<TrapTransport>[] getTransportClasses(ClassLoader cl) throws IOException
    {
        HashSet<Class<? extends TrapTransport>> rv = new HashSet<Class<? extends TrapTransport>>();
        rv.addAll(transportClasses);
        
        // Autodiscover, if applicable.
        if (useAutodiscover)
        {
            Class<?>[] transports = PackageScanner.scan(TrapTransport.TRAP_TRANSPORT_PACKAGE, cl);
            for (int i = 0; i < transports.length; i++)
                if (TrapTransport.class.isAssignableFrom(transports[i]))
                    rv.add((Class<TrapTransport>) transports[i]);
        }
        
        // Apply the blacklist
        rv.removeAll(blacklistedClasses);
        
        return rv.toArray(new Class[] {});
    }
    
    /**
     * Adds a class to the available transport classes. Multiple invocations for the same class are ignored.
     * 
     * @param transportClass
     *            The class to add.
     */
    public static void addTransportClass(Class<? extends TrapTransport> transportClass)
    {
        transportClasses.add(transportClass);
        blacklistedClasses.remove(transportClass);
    }
    
    /**
     * Marks a transport class as blacklisted, i.e. it will not be returned by {@link #getTransportClasses(ClassLoader)}
     * . This is regardless of whether or not autodiscovery would discover the transport on its own. Call
     * {@link #addTransportClass(Class)} to negate this effect.
     * 
     * @param removedClass
     *            The class to blacklist.
     * @since 1.1
     */
    public static void removeTransportClass(Class<?> removedClass)
    {
        blacklistedClasses.add(removedClass);
    }
    
    /**
     * Sets whether to use automatically discovered transports. Set this to <i>false</i> to disable the package scanning
     * step of Trap and only use transports explicitly configured using {@link TrapTransports#addTransportClass(Class)}.
     * <p>
     * The primary use case for this method is to allow the application full control over which transports are in use.
     * Depending on the distribution of TrAP used, it may ship with one or more built-in transports. Furthermore, these
     * built-in transports may or may not conflict with each-other and, as such, it may become difficult to determine
     * which specific implementations are used. If full control is required, autodiscovery should be disabled.
     * 
     * @param useAutodiscover
     *            <i>true</i> to enable automatic transport discovery, <i>false</i> otherwise.
     * @since 1.1
     */
    public static void setUseAutodiscoveredTransports(boolean useAutodiscover)
    {
        TrapTransports.useAutodiscover = useAutodiscover;
    }
}
