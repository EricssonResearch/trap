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



import java.util.Collection;

import com.ericsson.research.trap.delegates.TrapDelegate;
import com.ericsson.research.trap.spi.TrapHostingTransport;
import com.ericsson.research.trap.spi.TrapTransport;
import com.ericsson.research.trap.spi.TrapTransportProtocol;

/**
 * A common ancestor interface for {@link TrapEndpoint} and {@link TrapListener}. Methods common to TrapEndpoints and
 * TrapListeners will be found here, generally on managing the transports themselves.
 * 
 * @author Vladimir Katardjiev
 * @since 1.0
 */
public interface TrapSettings
{
    /**
     * Enables a transport with a given name. Enabling a transport merely switches the enabled flag of the transport in
     * the resident Trap configuration. It does not necessarily connect it.
     * <p>
     * Enabling a transport after a client endpoint has been asked to connect will cause the transport to be ignored
     * until the reconnect procedure is triggered. For this reason, it is recommended that enabling/disabling transports
     * is done before an endpoint is connected.
     * 
     * @param transportName
     *            The transport to enable.
     * @throws TrapException
     *             If the transport does not exist. This exception is thrown to prevent applications that rely on a
     *             certain transport from being surprised when the transport never connects. A typical case is a client
     *             disabling the http transport when websockets is not available.
     */
    public void enableTransport(String transportName) throws TrapException;
    
    /**
     * Disables a transport. Unlike {@link #enableTransport(String)}, this method will always succeed, even if the
     * transport does not exist. This allows applications to safely remove transports they do not wish to use – if the
     * transport does not exist, it will not be used.
     * 
     * @param transportName
     *            The transport to disable
     */
    public void disableTransport(String transportName);
    
    /**
     * Convenience method to disable all transports at a given configurable object. This is primarily used to create
     * controlled-environment Trap connections, where the available transports are not automatically set. Primarily
     * targeted towards tests or other controlled environments, or when all transports need to be clamped down.
     */
    public void disableAllTransports();
    
    /**
     * Queries if a transport is enabled. Preferable to calling {@link #getTransport(String)} and
     * {@link TrapTransport#isEnabled()} as this method will not throw if a transport does not exist.
     * 
     * @param transportName
     *            The transport whose state to query.
     * @return <i>true</i> if a transport exists, and is enabled, <i>false</i> otherwise.
     */
    public boolean isTransportEnabled(String transportName);
    
    /**
     * Gets the configuration of this endpoint. This is the configuration as it applies for this node; it does not
     * represent the configuration another endpoint needs to have to connect here. See
     * {@link TrapListener#getClientConfiguration()} for that. This method is useful for debugging.
     * 
     * @return A string representing the current configuration of this TrapEndpoint.
     */
    public String getConfiguration();
    
    /**
     * Configures this TrapEndpoint, overwriting any previous configuration, and setting the new string to the new
     * configuration. This will also reconfigure any constituent transports. This method should be used before using any
     * of the programmatic configuration methods, as it may override them.
     * 
     * @param configuration
     *            A string representing the new configuration.
     */
    public void configure(String configuration);
    
    /**
     * Changes the configuration of a given transport. This is an alias to accessing the transport and configuring it
     * directly. After configuration, the transport settings will be updated.
     * <p>
     * Care should be taken when using this method to not conflict with Trap's general management. In most cases, an
     * endpoint will manage transport settings automatically, although some tweaks can be done on a per-transport basis.
     * <p>
     * General configuration keys can be found as static properties in {@link TrapTransport}. Specific options are
     * relevant only to the given transport.
     * 
     * @param transportName
     *            The name of the transport to configure.
     * @param configurationKey
     *            The <i>unprefixed</i> configuration key
     * @param configurationValue
     *            The new value of the key.
     * @throws TrapException
     *             If the transport does not exist (and thus cannot be configured).
     */
    public void configureTransport(String transportName, String configurationKey, String configurationValue) throws TrapException;
    
    /**
     * Attempts to fetch a {@link TrapHostingTransport} instance for the specific protocol. This method effectively
     * fetches the hosting transports and attempts to find the one with the highest priority that conforms to
     * <i>protocol</i>. Not all protocols may have hosting transports. No verification will be performed that the query
     * can ever return a non-null value.
     * 
     * @param protocol
     *            The protocol of the transport that should host. See {@link TrapTransportProtocol}.
     * @return The hosting transport for the given protocol with the highest priority. Else, <i>null</i>.
     * @see #getHostingTransports()
     * @since 1.1
     */
    public TrapHostingTransport getHostingTransport(String protocol);
    
    /**
     * Ask the endpoint to make a list of the currently available {@link TrapHostingTransport}s associated with it. The
     * number of transports may be zero when called from, for example, client endpoints. See the TrapHostingTransport
     * documentation for discussion on the uses of these.
     * 
     * @return A collection of all transports that can potentially host objects. The collection may be empty.
     * @since 1.1
     */
    public Collection<TrapHostingTransport> getHostingTransports();
    
    /**
     * Gets the current set of transports associated with this TrapEndpoint. These transports represent all the
     * instances available to the endpoint, not necessarily the ones that are currently in use. Each transport has an
     * individual state that determines if it is connected or not.
     * 
     * @return A collection of transports associated with this endpoint.
     */
    public Collection<TrapTransport> getTransports();
    
    /**
     * Accesses a single transport by name. This is useful for advanced configuration of transports, debugging or highly
     * specialised tweaking.
     * 
     * @param transportName
     *            The name of the transport.
     * @return The {@link TrapTransport} instance representing the transport
     * @throws TrapException
     *             If the transport does not exist.
     */
    public TrapTransport getTransport(String transportName) throws TrapException;
    
    /**
     * Sets a configuration option.
     * 
     * @param optionName
     *            The full option name (including prefix, if applicable).
     * @param value
     *            The new value.
     */
    public abstract void setOption(String optionName, String value);
    
    /**
     * Sets the object <i>delegate</i> as the listener for all the implemented {@link TrapDelegate} sub-interfaces. If
     * <i>replaceAllExisting</i> is true, then all callbacks are reset, even the ones not implemented by <i>delegate</i>
     * (they will instead log a warning if applicable). Otherwise, only the implemented callbacks are overwritten.
     * <p>
     * Multiple invocations to setDelegate can be used to set different object as delegates for different actions. For
     * example, <i>setDelegate(openDelegate, false)</i> and <i>setDelegate(dataDelegate, false)</i> will retain
     * <b>both</b> objects as delegates. The actions a delegate is retained for depend on the interfaces implemented.
     * <p>
     * All delegates can be cleared by invoking <i>setDelegate(null, true)</i>.
     * 
     * @param delegate
     *            The delegate to add. It must implement at least one sub-interface of TrapDelegate, or the call will
     *            have no effect.
     * @param replaceAllExisting
     *            <i>true</i> to clear all delegates before assigning this delegate, <i>false</i> to keep any delegates
     *            that do not conflict with this one.
     * @since 1.1
     */
    public abstract void setDelegate(TrapDelegate delegate, boolean replaceAllExisting);
    
    /**
     * Trap delegates may have an additional context object provided at callback time. This is a single object shared
     * between all delegates, and may be used for server dispatching purposes.
     * 
     * @param delegateContext
     *            The context object to provide to delegates.
     * @since 1.1
     */
    public abstract void setDelegateContext(Object delegateContext);
}
