package com.ericsson.research.trap.spi;

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
import java.util.Map;

import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.TrapKeepalivePolicy;
import com.ericsson.research.trap.auth.TrapAuthentication;
import com.ericsson.research.trap.auth.TrapContextKeys;
import com.ericsson.research.trap.spi.TrapMessage.Format;
import com.ericsson.research.trap.utils.Callback;
import com.ericsson.research.trap.utils.SSLUtil.SSLMaterial;

/**
 * Defines the methods available for interacting with the various Trap transports.
 * <p>
 * Applications should not need to spend too much effort in configuring the specifics of the Trap transports. At most,
 * applications should suffice with using the enable/disable functionality, and leave Trap to manage the rest.
 * 
 * @author Vladimir Katardjiev
 */
public interface TrapTransport
{
    
    /**
     * The package to search for transports
     */
    public static final String TRAP_TRANSPORT_PACKAGE = "com.ericsson.research.trap.spi.transports";
    
    /**
     * Option to enable a transport. Set to true/false to enable/disable respectively.
     */
    public static final String OPTION_ENABLED         = "enabled";
    
    /**
     * The transport priority is a numeric value that determines the order in which Trap should prefer transports. Lower
     * values are preferred.
     */
    public static final String OPTION_PRIORITY        = "priority";
    
    /**
     * Sets a string that will be used as a prefix to logger calls. Use this to redirect Trap logger traffic into
     * specific logging parent contexts.
     */
    public static final String OPTION_LOGGERPREFIX    = "loggerprefix";
    
    /**
     * Option string to configure the transport to warn about (server) transports that have not been configured to
     * listen to a specific IP number.
     */
    public static final String OPTION_WARN_ADDRESS    = "warnAddressConfiguration";
    
    /**
     * Uses <b>insecure, untrusted</b> default certificates. Set to <i>true</i> to enable. This enables TLS on
     * transports that support it (http, socket, ws), but uses an invalid and insecure certificate.
     */
    public static final String CERT_USE_INSECURE_TEST = "certInsecureDefault";
    
    /**
     * Corresponding flag to {@link #CERT_USE_INSECURE_TEST} to tell client transports to ignore invalid server
     * certificates.
     */
    public static final String CERT_IGNORE_INVALID    = "certIgnoreInvalid";
    
    /**
     * The type of the certificate keystore. Usually "jks" or "pkcs12".
     */
    public static final String CERT_KEYSTORE_TYPE     = "certKeystoreType";
    
    /**
     * The name (in case of a .jar embedded resource) or path to the file containing the keystore material. The keystore
     * is used for the server (or client, where applicable) certificate.
     */
    public static final String CERT_KEYSTORE_NAME     = "certKeystoreName";
    
    /**
     * The password/passphrase used for the keystore file.
     */
    public static final String CERT_KEYSTORE_PASS     = "certKeystorePass";
    
    /**
     * The type of the certificate truststore. Usually {@link SSLMaterial#JKS_TYPE} or {@link SSLMaterial#PKCS12_TYPE}.
     */
    public static final String CERT_TRUSTSTORE_TYPE   = "certTruststoreType";
    
    /**
     * The name (in case of a .jar embedded resource) or path to the file containing the keystore material. The
     * truststore should contain any and all root certificates that the transport should accept. May be null, in which
     * case the system-wide defaults are used.
     */
    public static final String CERT_TRUSTSTORE_NAME   = "certTruststoreName";
    
    /**
     * The password/passphrase used for the truststore file.
     */
    public static final String CERT_TRUSTSTORE_PASS   = "certTruststorePass";
    
    /**
     * Checks whether the given Trap transport is enabled (i.e. it will react to any calls other than configuration).
     * 
     * @return <i>true</i> if the transport is enabled, <i>false</i> otherwise.
     */
    public boolean isEnabled();
    
    /**
     * Enables this transport. Does not imply that this transport should connect; {@link #connect()} must be called
     * separately for that to happen.
     */
    public void enable();
    
    /**
     * Disables this transport, preventing it from participating in the transport abstraction. Unlike {@link #enable()},
     * disable <b>does imply the transport must close</b> and refuse to carry other messages. Disable may not fail.
     */
    public void disable();
    
    /**
     * Checks if this transport is currently connected to the other end. Does not imply whether or not it is possible
     * for this transport to connect.
     * 
     * @return <i>true</i> if this transport object represents an active connection to the other end, <i>false</i>
     *         otherwise.
     */
    public boolean isConnected();
    
    /**
     * Signals to this transport that it should attempt to connect to the remote endpoint. The transport may attempt to
     * connect synchronously, asynchronously or not at all according to its own configuration.
     * <p>
     * Not all transport instances are able to open an outgoing connection (e.g. server instances) and, as such, some
     * instances may throw an exception when calling this method. If the transport does not support outgoing
     * connections, it must throw an exception immediately.
     * 
     * @throws TrapException
     *             If this transport does not support outgoing connections.
     */
    public void connect() throws TrapException;
    
    /**
     * Signals to this transport that it must disconnect. The transport must immediately take all measures to close the
     * connection, must clean up as much as it can, and may not throw any exceptions while doing so.
     * <p>
     * May NOT block.
     */
    public void disconnect();
    
    /**
     * Fetches this transport's priority, which is used in the comparable implementation to sort transports, if needed.
     * Currently unused.
     * 
     * @return The transport's priority
     */
    public int getTransportPriority();
    
    /**
     * Sets this transport's priority. This is a relative priority that determines the order-of-preference between
     * transports.
     * 
     * @param priority
     *            The new priority. May be positive or negative.
     */
    public void setTransportPriority(int priority);
    
    /**
     * Gets this transport's name. The transport name is used for, among other things, log outputs and configuration
     * settings, must be alphanumeric and contain no spaces.
     * 
     * @return The transport's name.
     */
    public String getTransportName();
    
    /**
     * Configures a specific transport setting (key/value)
     * 
     * @param configurationKey
     *            A string key representing the configuration parameter.
     * @param configurationValue
     *            The new value of the configuration parameter.
     * @throws TrapException
     *             If the configuration is invalid, or could not be executed.
     */
    public void configure(String configurationKey, String configurationValue);
    
    /**
     * Configures a specific transport setting (key/value) using integer values.
     * 
     * @param configurationKey
     *            A string key representing the configuration parameter.
     * @param configurationValue
     *            The new value of the configuration parameter.
     * @throws TrapException
     *             If the configuration is invalid, or could not be executed.
     */
    public void configure(String configurationKey, int configurationValue) throws TrapException;
    
    /**
     * Sets the Transport's configuration object. This configuration object is shared with the parent.
     * 
     * @param configuration
     *            The new configuration object.
     */
    public void setConfiguration(TrapConfiguration configuration);
    
    /**
     * Returns a configuration string representing this transport's configuration.
     * 
     * @return A String representation of the configuration of this TrapTransport.
     */
    public String getConfiguration();
    
    /**
     * Sets the listener of this transport. The delegate will be notified of this transport's state changes, as well as
     * incoming messages.
     * 
     * @param delegate
     *            The new delegate to the transport, which will receive all transport delegate calls.
     * @param context
     *            An arbitrary object that will be passed along with all calls to the delegate.
     */
    public void setTransportDelegate(TrapTransportDelegate delegate, Object context);
    
    /**
     * Set an authentication instance for this transport, to be used for authenticating any messages that need
     * authentication.
     * 
     * @param authentication
     *            An instance ot TrapAuthentication capable of performing all authentication-related tasks.
     * @throws TrapException
     *             If the transport and Authentication instances are not mutually compatible. This transport should then
     *             be discarded.
     */
    public void setAuthentication(TrapAuthentication authentication) throws TrapException;
    
    /**
     * Queries if this transport can perform a connection, i.e. if it can act as a client transport.
     * 
     * @return <i>true</i> if this transport can perform an outgoing connection, <i>false</i> otherwise.
     */
    public boolean canConnect();
    
    /**
     * Queries if this transport can accept incoming connections, i.e. if it can act as a server.
     * 
     * @return <i>true</i> if this transport can receive incoming connections, <i>false</i> otherwise.
     */
    public boolean canListen();
    
    /**
     * Attempts to send a message with this transport. If the transport cannot send this message, in full, right now, it
     * MUST throw an exception. The transport MUST NOT buffer messages if the <i>expectMore</i> flag is false. The
     * transport MAY buffer messages if <i>expectMore</i> is <i>true</i> but this is not required.
     * 
     * @param message
     *            The message to send.
     * @param expectMore
     *            A flag signifying to the transport that more messages will be sent in a short timespan (less than
     *            1ms). Some transports may wish to buffer these messages before sending to optimise performance.
     * @throws TrapException
     *             If an error occurred during the sending of the message(s). The exception MUST contain the message(s)
     *             that failed sending. If it contains more than one message, the order must be in the same order that
     *             send() was called.
     */
    public void send(TrapMessage message, boolean expectMore) throws TrapTransportException;
    
    /**
     * Asks if the transport is available for sending. Effectively checks the transport's state for available, but this
     * way is faster.
     * 
     * @return <i>true</i> if the transport can be used to send a message, <i>false</i> otherwise.
     */
    public boolean isAvailable();
    
    /**
     * Fetches the transport's current state
     * 
     * @return The transport's current state.
     */
    public TrapTransportState getState();
    
    /**
     * Fetches the last known liveness timestamp of the transport. This is the last time it received a message from the
     * other end.
     * 
     * @return The timestamp of the last message received from the remote side.
     */
    public long lastAlive();
    
    /**
     * Attempts to verify if the transport is alive, or has been alive within a certain number of milliseconds.
     * Effectively, this can be used to trigger a keepalive check of the transport if used with a <i>within</i>
     * parameter of 0 and a <i>check</i> parameter of true.
     * <p>
     * This function has a two-part purpose. The first is for the upper layer to be able to check the last known
     * liveness of the transport, to reduce the discovery time of a dead connection. The second is to trigger a check
     * for a dead transport, when the application needs to know that it has active connectivity.
     * <p>
     * Note that in normal operation, the transport itself will report when it has disconnected; the upper layer does
     * not need to concern itself with this detail unless it specifically needs to know that it has connectivity right
     * now.
     * 
     * @param within
     *            Within how many milliseconds the last activity of the transport should have occurred before the
     *            transport should question whether it is alive.
     * @param check
     *            Whether the transport should attempt to check for liveness, or simply return false if the last known
     *            activity of the transport is not later than within.
     * @param timeout
     *            If check is true, how many milliseconds at most the liveness check should take before returning false
     *            anyway. The application can use this value if it has a time constraint on it.
     * @return <i>true</i> if the connection is currently alive (including if this function successfully re-established
     *         the connection), <i>false</i> otherwise.
     */
    public Callback<Boolean> isAlive(long within, boolean check, long timeout);
    
    /**
     * Initialises the transport, making it able to connect again. This is, in effect, reconstructing the object, but
     * does not need to use reflection. This method MUST properly clean up the old connection(s) and state(s) if
     * applicable, and make the object like new again. It may and should keep its existing configuration, however.
     */
    public void init();
    
    /**
     * Retreives the current value of <i>keepaliveInterval</i>. See {@link #setKeepaliveInterval(int)} for information
     * about what the different values mean.
     * 
     * @return The number of seconds between keepalives.
     */
    public int getKeepaliveInterval();
    
    /**
     * Sets a new keepalive interval for the trap endpoint. The keepalive interval has one of three possible meanings:
     * <ul>
     * <li>A value of {@link TrapKeepalivePolicy#DISABLED} will disable the keepalives.
     * <li>A value of {@link TrapKeepalivePolicy#DEFAULT} will cause each transport to use its internal estimate of what
     * a good keepalive is.
     * <li>A value of 1 &lt;= n &lt;= 999999 will specify the number of seconds between keepalive messages.
     * </ul>
     * Any change on the TrapEndpoint level will affect all transports associated with this endpoint, overwriting any
     * individual configuration the transports may have had. The inverse does not apply.
     * <p>
     * See <a href= "http://www.cf.ericsson.net/confluence/display/warp/Trap+Keepalives">the Trap Keepalive
     * documentation</a> for details on the keepalives.
     * 
     * @param newInterval
     *            The new keepalive interval or policy.
     */
    public void setKeepaliveInterval(int newInterval);
    
    /**
     * Retrieves the currently-in-use keepalive predictor. See {@link TrapKeepalivePredictor} for details.
     * 
     * @return the currently-in-use predictor.
     */
    public TrapKeepalivePredictor getKeepalivePredictor();
    
    /**
     * Assigns a new predictor. See {@link TrapKeepalivePredictor}. Before assigning a predictor to a transport, ensure
     * the predictor supports the transport. Some transports, e.g. http, do not support or require keepalive prediction
     * and thus disable it.
     * 
     * @param newPredictor
     *            the new predictor.
     */
    public void setKeepalivePredictor(TrapKeepalivePredictor newPredictor);
    
    /**
     * Alias to set the expiry of the currently configured predictor.
     * 
     * @see TrapKeepalivePredictor#setKeepaliveExpiry(long)
     * @param newExpiry
     *            The new keepalive expiry.
     */
    public void setKeepaliveExpiry(long newExpiry);
    
    /**
     * Queries whether this transport is configured for a particular role. This method can be used to ask a transport if
     * it is capable of performing the requisite task (Listen for a Server transport, or connect for a client transport)
     * if called upon.
     * <p>
     * This method is strict and, i.e. if a transport is asked both client=true and server=true it must only return true
     * if it can act as both client and server AND the configuration is appropriate.
     * <p>
     * The method is mainly intended for introspection.
     * 
     * @param client
     *            Whether the transport's configuration as a client is necessary.
     * @param server
     *            Whether the transport's configuration as a server is necessary.
     * @return <i>true</i> if the client can act as, and is configured for, the roles asked. <i>false</i> otherwise.
     */
    public boolean isConfigured(boolean client, boolean server);
    
    /**
     * Forces the transport to enter an ERROR state, closing the transport ungracefully. This is generally invoked when
     * a transport
     */
    public void forceError();
    
    /**
     * Sets the message format for this transport. This method is intended for Endpoint use, as configuring transports
     * to have a different format than their respective endpoints can have unintended consequences.
     * 
     * @param format
     *            The new message format to use.
     */
    public abstract void setFormat(Format format);
    
    /**
     * Accesses the current format for this transport.
     * 
     * @return The current message format.
     */
    public abstract Format getFormat();
    
    /**
     * Sends a transport-specific message, bypassing the regular message queue. This method can only be used on
     * unordered messages that must arrive on the other side out-of-order. Ping/pong, notifications, etc, should use
     * this method. Notably, regularly scheduled CLOSE and END must not use sendTransportSpecific, as they must remain
     * in the message buffer in order to allow all messages to transfer.
     * 
     * @param message
     *            The message to send.
     */
    public void sendTransportSpecific(TrapMessage message);
    
    /**
     * Queries whether this transport is capable of transporting TrapObjectMessages. These transports are essentially a
     * short circuit around much of Trap's functionality, and will thus skip the message queue.
     * 
     * @return <i>true</i> if this is an object/loopback transport, <i>false</i> otherwise.
     */
    public abstract boolean isObjectTransport();
    
    /**
     * Flushes the transport buffers. Any buffered messages should be sent.
     */
    public abstract void flushTransport();
    
    /**
     * Fetches the associated callback context object.
     */
    public abstract Object getContext();
    
    /**
     * Receives a transported message, sent from the other side using
     * {@link TrapTransportDelegate#ttNeedTransport(TrapMessage, TrapTransport, Object)}
     * 
     * @since 1.2
     */
    public abstract void receiveTransportedMessage(TrapMessage msg);
    
    /**
     * Fetches the authentication keys available; the keys are string options that specify which context data is
     * available. Some common keys are specified in {@link TrapContextKeys}, and may differ from transport to transport.
     * This context can be used to identify and authenticate a remote endpoint.
     * 
     * @return A collection with all the keys that the transport can provide values for.
     */
    public Collection<String> getAuthenticationKeys();
    
    /**
     * Fetches all available authentication context keys and values. This may be computationally intensive, and is a
     * synchronous operation. If not all values are necessary, consider {@link #getAuthenticationContext(Collection)}
     * 
     * @return A map containing all the current keys and values. Note that values may change over time; that change will
     *         not be represented here.
     */
    public Map<String, Object> getAuthenticationContext();
    
    /**
     * Fetches the available authentication context values for the specified keys. Only values with the corresponding
     * keys will be returned. If a key cannot be mapped to a value, the key will not be represented in the returned map.
     * 
     * @param authenticationKeys
     *            The keys which to fetch.
     * @return A map containing the subset of <i>authenticationKeys</i> that could be mapped to a value.
     */
    public Map<String, Object> getAuthenticationContext(Collection<String> authenticationKeys);
    
}
