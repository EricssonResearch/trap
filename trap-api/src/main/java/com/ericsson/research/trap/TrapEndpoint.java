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



import java.util.Map;

import com.ericsson.research.trap.auth.TrapAuthentication;
import com.ericsson.research.trap.auth.TrapContextKeys;
import com.ericsson.research.trap.delegates.OnData;
import com.ericsson.research.trap.delegates.OnObject;
import com.ericsson.research.trap.delegates.TrapDelegate;
import com.ericsson.research.trap.spi.TrapKeepalivePredictor;
import com.ericsson.research.trap.spi.TrapMessage.Format;
import com.ericsson.research.trap.spi.TrapTransport;
import com.ericsson.research.trap.spi.queues.MessageQueue;
import com.ericsson.research.trap.utils.Callback;

/**
 * The main interface to Trap, a TrapEndpoint is the shared interface between servers and clients. It provides most of
 * the methods that should be necessary to configure and use a Trap connection.
 * <p>
 * In general, a TrapEndpoint should be configured either when it has just been created, using a {@link TrapFactory}, or
 * when it has just been provided to a {@link TrapListenerDelegate} using the callback. Reconfiguring a TrapEndpoint
 * while it is in use can have unintended consequences.
 * <p>
 * Common tasks on a TrapEndpoint are:
 * <h3>Configuring Transports</h3>
 * <p>
 * Enabling and disabling transports, as well as configuring each transport, is possible from TrapEndpoint. Adding new
 * transports to an existing endpoint is not possible; for that, instead use
 * {@link TrapTransports#addTransportClass(Class)} before instantiating an endpoint.
 * <h3>Sending and Receiving Messages</h3>
 * <p>
 * To send messages, simply use {@link #send(byte[])} to send binary data, or {@link #send(TrapObject)} to use deferred
 * serialization. When data is received, the appropriate delegate that implements {@link OnData} or {@link OnObject} is
 * called.
 * <h3>Checking Liveness</h3>
 * <p>
 * Trap provides a simple facility to check if the other endpoint is alive, that is, communication is active and the
 * other application layer is responding. The {@link #isAlive(long)} method will perform an active check, that is, ping
 * immediately. The {@link #isAlive(long, boolean, boolean, long)} method on the other hand can be used to check if the
 * endpoint has been alive recently, or a combination of the two.
 * <h3>Configuring Keepalives</h3>
 * <p>
 * By default, Trap will attempt to use a per-transport keepalive policy that strikes a moderate balance between
 * liveness and chattiness. It will take into account traffic only on the current TrapTransport, and without using
 * device integration. This simple implementation can be tweaked to use a static keepalive interval instead (every X
 * milliseconds), or disabled, using the {@link #setKeepaliveInterval(int)} method. More advanced keepalives can be
 * user-supplied on a transport basis using {@link TrapTransport#setKeepalivePredictor(TrapKeepalivePredictor)}.
 * <h3>Customizing the Message Queue</h3>
 * <p>
 * Trap has a number of buffers it uses, and some (such as the message queue) can impact performance significantly under
 * different usage patterns. The message queue is the first front-end that feeds Trap, and plays a large role. An
 * endpoint can be instructed to either choose the "best" buffer available of a given type (see constants) using
 * {@link #setQueueType(String)}, or it can be told to explicitly use a specific queue using
 * {@link #setQueue(MessageQueue)}. User-supplied queues can be use, as long as they fulfil the requirements.
 * 
 * @author Vladimir Katardjiev
 * @since 1.0
 */
public interface TrapEndpoint extends TrapSettings
{
    
    /*
     * Specifies the queue type. There are two types of queues: <p>
     * <b>Blocking</b> queues will block the execution of the send method until
     * more queue space is available. <b>Non-blocking</b> queues are queues
     * without "Blocking" in the name. They will throw an exception if the queue
     * length is exceeded.
     */
    /**
     * Constant representing a regular message queue. Properties follow:
     * <ul>
     * <li>Lowest memory overhead of any kind of queue.
     * <li>No upper limit on number of messages.
     * <li>No view on how much memory is used by the queue.
     * </ul>
     * The regular message queue is good for a generic traffic pattern, that does not have a more specific property, on
     * desktop machines.
     * <p>
     * When supplied to {@link #setQueueType(String)}, allows Trap to automatically select the specific queue
     * implementation from the built-in ones.
     */
    public static final String REGULAR_MESSAGE_QUEUE     = "REGULAR_MESSAGE_QUEUE";
    
    /**
     * Constant representing a blocking message queue. Properties follow:
     * <ul>
     * <li>Low memory overhead
     * <li>Upper limit on number of messages; provides rough prevention against out of memory / buffer overruns.
     * <li>No view on how much memory is used by the queue.
     * </ul>
     * The blocking message queue type is a reasonably low-overhead queue that can prevent rudimentary memory issues. It
     * still provides no explicit checks, but is better than nothing.
     * <p>
     * When supplied to {@link #setQueueType(String)}, allows Trap to automatically select the specific queue
     * implementation from the built-in ones.
     */
    public static final String BLOCKING_MESSAGE_QUEUE    = "BLOCKING_MESSAGE_QUEUE";
    
    /**
     * Constant representing a non-blocking byte queue. Properties follow:
     * <ul>
     * <li>Low memory overhead
     * <li>Moderate CPU overhead to calculate the bytes in/out
     * <li>No upper limit on number of messages.
     * <li>Can inspect how much memory is used
     * </ul>
     * The regular message queue is good for a generic traffic pattern, that does not have a more specific property, on
     * desktop machines.
     * <p>
     * When supplied to {@link #setQueueType(String)}, allows Trap to automatically select the specific queue
     * implementation from the built-in ones.
     */
    public static final String REGULAR_BYTE_QUEUE        = "REGULAR_BYTE_QUEUE";
    
    /**
     * Constant representing a blocking byte queue. Properties follow:
     * <ul>
     * <li>Low memory overhead
     * <li>Moderate CPU overhead to calculate the bytes in/out
     * <li>Upper limit on the RAM usage.
     * </ul>
     * The regular message queue is good for a generic traffic pattern, that does not have a more specific property, on
     * desktop machines.
     * <p>
     * When supplied to {@link #setQueueType(String)}, allows Trap to automatically select the specific queue
     * implementation from the built-in ones.
     */
    public static final String BLOCKING_BYTE_QUEUE       = "BLOCKING_BYTE_QUEUE";
    
    /**
     * Configuration parameter representing the prefix to be prepended to all logging out of this endpoint. This option
     * is useful for integrating this Trap endpoint's logging into a greater structure.
     */
    public static final String OPTION_LOGGERPREFIX       = "trap.loggerprefix";
    
    /**
     * Configuration parameter for the maximum chunk size. Set to [1,Integer.MAX_VALUE] to alter the maximum chunk size
     * of the endpoint. Set to 0 or less to disable chunking entirely.
     */
    public static final String OPTION_MAX_CHUNK_SIZE     = "trap.maxchunksize";
    
    /**
     * Configuration parameter to enable/disable compression. Set to <i>false</i> to disable compression support on the
     * endpoint.
     */
    public static final String OPTION_ENABLE_COMPRESSION = "trap.enablecompression";
    
    /**
     * Hostname for automatic configuration. This option is used by the client to format its Open message, and for the
     * server to ensure the client has the correct transports configured
     */
    public static final String OPTION_AUTO_HOSTNAME      = "trap.auto_hostname";
    
    /**
     * Closes this Trap endpoint, terminating any outstanding Trap transports. Does nothing if the endpoint is already
     * closed, is closing, or is in an error state.
     */
    public abstract void close();
    
    /**
     * Fetches the authentication instance for this Trap endpoint. Can be used by individual transports to challenge
     * and/or authenticate incoming messages, if the transport requires more authentication than the default Trap
     * security policy.
     * 
     * @return The TrapAuthentication instance used by the endpoint.
     */
    public abstract TrapAuthentication getAuthentication();
    
    /**
     * Accessor for the current blocking timeout
     * 
     * @return The current blocking timeout.
     */
    public long getBlockingTimeout();
    
    /**
     * Fetches the channel object associated with the given channel ID. If the channel was not created, creates it,
     * allocating all required buffers.
     * 
     * @param channelID
     *            The channel ID to get the object for.
     * @return The object responsible for maintaining the channel's state.
     */
    public TrapChannel getChannel(int channelID);
    
    /**
     * Retreives the current value of <i>keepaliveInterval</i>. See {@link #setKeepaliveInterval(int)} for information
     * about what the different values mean.
     * 
     * @return The number of seconds between keepalives.
     */
    public int getKeepaliveInterval();
    
    /**
     * Gets the amount of maximum simultaneously active transports.
     * <p>
     * The number of max simultaneously active transports governs the behaviour of Trap Endpoints (both client and
     * server). If the number of active transports equals the number of maximum simultaneous transports, recovery will
     * only take place for transports with higher priority than the currently active transport.
     * <p>
     * If the number of active transports exceeds the maximum, transports will be closed, starting with the lowest
     * priority (largest integer). Note that this is a <i>soft limit</i>. The Trap implementation may exceed the maximum
     * number of transports for a transitional period, in order to ensure a smooth handover.
     * 
     * @return The current maximum simultaneously active transports
     */
    public int getMaxActiveTransports();
    
    /**
     * Fetches the reconnect timeout. See {@link #setReconnectTimeout(long)}
     * 
     * @return The current reconnect timeout, in milliseconds
     */
    public long getReconnectTimeout();
    
    /**
     * Accessor for the current endpoint state.
     * 
     * @return The state the endpoint is in.
     */
    public TrapState getState();
    
    /**
     * Fetches the current message serialization format.
     * 
     * @return the current message serialization format.
     */
    public abstract Format getTrapFormat();
    
    /**
     * Attempts to verify that the endpoint is alive, with a round trip time of at most <i>timeout</i> milliseconds.
     * This function is primarily intended for when an application requires to know it has connectivity <i>right this
     * instant</i>, and should be used sparingly on power-constrained devices.
     * <p>
     * {@link #isAlive(long)} is synonymous to calling <i>isAlive(timeout, true, true, timeout)</i>.
     * <p>
     * Note that isAlive is intended to be used in an asynchronous mode. The callback may be used to request a
     * synchronous answer, but this is strongly discouraged. Further note that isAlive will take longer time to execute
     * than <i>timeout</i> as it will both try to verify transport liveness, and then reconnect the transport if
     * verification fails. Transport reconnection uses a timer longer than <i>timeout</i>, to allow for additional
     * round-trips used when connecting.
     * <p>
     * <b>Warning:</b> Calling <i>isAlive</i> on a Server Trap Endpoint (i.e. when none of the transports can perform
     * the open() function) may cause an endpoint to close on the server, while the client considers it open. The client
     * may not have discovered the underlying transport is dead yet, and may require more time to reconnect. A wakeup
     * mechanism can be used to establish liveness, but the server's <i>timeout</i> value should be significantly more
     * generous in order to accommodate the client's reconnect and/or wakeup procedure!
     * 
     * @param timeout
     *            The timeout (in milliseconds) for the round-trip between the server.
     * @return A callback for <i>true</i> if the connection is currently alive (including if this function successfully
     *         re-established the connection), <i>false</i> otherwise.
     * @since 1.2
     */
    public Callback<Boolean> isAlive(long timeout);
    
    /**
     * Attempts to verify if the endpoint is alive, or has been alive within a certain number of milliseconds.
     * Effectively, this can be used to trigger a keepalive check of the endpoint if used with a <i>within</i> parameter
     * of 0 and a <i>check</i> parameter of true.
     * <p>
     * This function has a two-part purpose. The first is for the application to be able to check the last known
     * liveness of the endpoint, to reduce the discovery time of a dead connection. The second is to trigger a check for
     * a dead endpoint, when the application needs to know that it has active connectivity.
     * <p>
     * Note that in normal operation, the endpoint itself will report when it has disconnected; the application does not
     * need to concern itself with this detail unless it specifically needs to know that it has connectivity right now.
     * <p>
     * <b>Warning:</b> Calling <i>isAlive</i> on a Server Trap Endpoint (i.e. when none of the transports can perform
     * the open() function) may cause a client to lose its connectivity. The client may not have discovered the
     * underlying transport is dead yet, and may require more time to reconnect. A wakeup mechanism can be used to
     * establish liveness, but the server's <i>timeout</i> value should be significantly more generous in order to
     * accommodate the client's reconnect and/or wakeup procedure!
     * 
     * @param within
     *            Within how many milliseconds the last activity of the endpoint should have occurred before the
     *            endpoint should question whether it is alive.
     * @param check
     *            Whether the endpoint should attempt to check for liveness, or simply return false if the last known
     *            activity of the endpoint is not later than within.
     * @param force
     *            Whether to attempt to force reconnection if the transports are not available within the given timeout.
     *            This will ensure available liveness value reflects what is possible right now, although it may mean
     *            disconnecting transports that still may recover.
     * @param timeout
     *            If check is true, how many milliseconds at most the liveness check should take before returning false
     *            anyway. The application can use this value if it has a time constraint on it.
     * @return A callback for <i>true</i> if the connection is currently alive (including if this function successfully
     *         re-established the connection), <i>false</i> otherwise.
     * @since 1.2
     */
    public Callback<Boolean> isAlive(long within, boolean check, boolean force, long timeout);
    
    /**
     * Checks whether this endpoint works in async mode. In async mode, the transport thread is disconnected from the
     * Trap thread. Trap ensures only one thread has concurrent calls from trapData, although it may receive data from
     * several.
     * 
     * @return <i>true</i> if async mode is enabled, <i>false</i> otherwise.
     */
    public boolean isAsync();
    
    /**
     * Fetches the last known liveness timestamp of the endpoint. This is the last time it received a message from the
     * other end. This includes all messages (i.e. also Trap messages such as keepalives, configuration, etc) so must
     * not be confused with the last known activity of the other application. For example, in the case of a JavaScript
     * remote endpoint, this does not guarantee an evaluation error has not rendered the JSApp's main run loop as
     * inoperable.
     * 
     * @see #isAlive(long, boolean, boolean, long) if what is needed is an evaluation of the liveness status.
     * @return The timestamp of the last message received from the remote side.
     */
    public long lastAlive();
    
    /**
     * Alias for {@link #send(byte[], int, boolean)}, with the channel ID set to 1.
     * 
     * @param data
     *            The data to send
     * @throws TrapException
     *             if the queue length is exceeded, or a timeout occurs on a blocking queue
     */
    public abstract void send(byte[] data) throws TrapException;
    
    /**
     * Attempts to queue data for sending. If the queue length is exceeded, it may block or throw an exception, as per
     * the queue type.
     * <p>
     * Please note that while send will accurately send the data over to the other endpoint, it is advisable to instead
     * use {@link #send(TrapObject)} if the data being sent is a serialized object. If the other endpoint is locally
     * deployed, the TrapObject will never be serialized, thus saving on large amounts of processing power.
     * 
     * @param data
     *            The data to send. This should be UTF-8 readable if it is aimed at a browser, for maximum
     *            compatibility. If the remote endpoint is a Java or C client or server, can be any arbitrary data of
     *            less than 2^28-1 bytes length (7-bit encoding) or 2^32-1 bytes (8-bit encoding)
     * @param channel
     *            The channel to send the data on. Recommended in the range [1-63], but will accept ID zero as well.
     * @param useCompression
     *            <i>True</i> if Trap should apply compression algorithm to this message, <i>false</i> otherwise.
     * @throws TrapException
     *             if the queue length is exceeded, or a timeout occurs on a blocking queue
     */
    public abstract void send(byte[] data, int channel, boolean useCompression) throws TrapException;
    
    /**
     * Alias for {@link #send(TrapObject, int, boolean)}, with the channel ID set to 1.
     * 
     * @param object
     *            The object to send
     * @throws TrapException
     *             if the queue length is exceeded, or a timeout occurs on a blocking queue
     */
    public abstract void send(TrapObject object) throws TrapException;
    
    /**
     * Attempts to queue an object for sending. If the queue length is exceeded, it may block or throw an exception, as
     * per the queue type.
     * <p>
     * Sending a TrapObject will serialize it as late as possible, or never. If the object is sent locally, it will
     * never be serialized. If it is to be sent remotely, it will be serialized when needed. Trap may serialize multiple
     * TrapObjects in parallel, so implementations should not assume any order between eventual
     * {@link TrapObject#getSerializedData()} calls.
     * <p>
     * Note that, since the TrapObject is not necessarily serialized in the caller's thread, exceptions caused by object
     * serialization will most probably not be seen in this way. They will be logged, however.
     * 
     * @param object
     *            The object to send.
     * @param channel
     *            The channel to send the data on. Recommended in the range [1-63], but will accept ID zero as well.
     * @param useCompression
     *            <i>True</i> if Trap should apply compression algorithm to this message, <i>false</i> otherwise.
     *            Compression will be applied in the event that the object has to be serialized.
     * @throws TrapException
     *             If the object could not be queued for sending, either due to user configuration or because of an
     *             invalid state of the endpoint.
     */
    public abstract void send(TrapObject object, int channel, boolean useCompression) throws TrapException;
    
    /**
     * In async mode, the endpoint will automatically schedule incoming messages on a separate thread than the one on
     * which they are received. Note that this does not imply there will be multiple threads receiving messages from the
     * TrapEndpoint, only that there is a break in the thread.
     * <p>
     * Async mode is generally preferred over synchronous mode, as it decouples the network layer from the request
     * processing layer. If the application using Trap provides its own thread dispatching with little overhead
     * (computational time) after receiving a message from Trap, it may experience a performance gain from switching off
     * async mode.
     * <p>
     * <b>Note that Asynchronous mode is required for reordering messages</b>! Specifically, messages in Trap may be
     * reordered by the transports due to race- or network conditions. Reordering these messages – and redispatching
     * them – may cause dropped or disconnected network connections, so Trap will NOT reorder incoming messages if in
     * synchronous mode.
     * 
     * @param async
     *            The new mode. True makes the endpoint asynchronous, while false disables it.
     */
    public void setAsync(boolean async);
    
    /**
     * Assigns an authentication instance to this TrapEndpoint. Implicitly, requires that this transport is
     * authenticated from now on, on any incoming connection.
     * <p>
     * Some transports may require more fine-grained authentication than is afforded (i.e. authenticate every message,
     * outgoing or incoming), and so they must individually access the TrapAuthentication instance using the getter.
     * 
     * @param authentication
     *            An instance capable of fulfilling the TrapAuthentication contract.
     * @throws TrapException
     *             If the authentication was not compatible with the transports.
     */
    public abstract void setAuthentication(TrapAuthentication authentication) throws TrapException;
    
    /**
     * Sets the blocking timeout, when using a blocking queue. This determines the amount of time {@link #send(byte[])}
     * will wait for queue space to open up before throwing an exception.
     * 
     * @param timeout
     *            The new timeout, in milliseconds.
     */
    public void setBlockingTimeout(long timeout);
    
    /**
     * Registers an object as a delegate to this endpoint. This delegate MUST implement TrapEndpointDelegate to receive
     * callbacks. Optionally, this delegate may also implement {@link OnObject} to receive TrapObjects.
     * <p>
     * In some cases, Trap can transfer a Java object faster than the serial representation, e.g. when both endpoints
     * are running in the same JVM. In those cases, registering a TrapEndpointObjectDelegate will yield a substantial
     * performance improvement. Note that, depending on how the other side sent the message, there is no guarantee that
     * the object will not be serialised, although if both support TrapObjects (i.e. remote uses .send(TrapObject) and
     * local has a TrapEndpointObjectDelegate), no serialisation will occur.
     * <p>
     * If the remote endpoint sends a TrapObject, but this delegate is not set on the local endpoint, the TrapObject
     * will be serialised on this end and sent to the TrapEndpointDelegate as usual.
     * <p>
     * This delegate is redundant for applications that will never run on the same JVM, as Trap cannot deserialise Java
     * objects.
     * 
     * @param delegate
     *            The delegate object.
     * @param context
     *            An optional object that will be supplied to the listener on every callback.
     * @deprecated Since 1.1, the preferred signature is {@link #setDelegate(TrapDelegate, boolean)}. If a context is
     *             required, use the explicit {@link #setDelegateContext(Object)} method.
     */
    public abstract void setDelegate(com.ericsson.research.trap.TrapEndpointDelegate delegate, Object context);
    
    /**
     * Sets the keepalive expiry timeout. Alias for {@link TrapKeepalivePredictor#setKeepaliveExpiry(long)} on the
     * currently set predictor.
     * 
     * @param newExpiry
     *            The new keepalive expiry time.
     * @see TrapKeepalivePredictor#setKeepaliveExpiry(long)
     */
    public void setKeepaliveExpiry(long newExpiry);
    
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
     * Sets the maximum allowed simultaneous transports. See {@link #getMaxActiveTransports()} for more information.
     * 
     * @param newMax
     *            The new maximum number of transports.
     *            <p>
     *            Note that 0 (zero) is a permitted value for maximum number of transports. This will not close the Trap
     *            connection, but cause a non-deterministic sequence of transport opening and closings, causing an
     *            abnormal load on client, server and network, but making it difficult to predict which transport (and
     *            thus connection) will be used at any given point in time. This can be used to mitigate suspected MITM
     *            attacks, but is not recommended otherwise. This connection sequence is not guaranteed secure. <i>This
     *            (zero) functionality is for experimental purposes, and may be removed at any time!</i>
     */
    public void setMaxActiveTransports(int newMax);
    
    /**
     * Assigns a new queue implementation for the outgoing message queue. This method allows the fine-tuning of outgoing
     * message performance. See {@link MessageQueue} for a discussion on what the queues affect.
     * <p>
     * Setting a queue on the TrapEndpoint will only affect channels that have not been created yet. It will not replace
     * the message queue of channels that have been created (i.e. accessed using {@link #getChannel(int)} or
     * {@link #send(byte[], int, boolean)}).
     * 
     * @param newQueue
     *            The new queue instance to use
     */
    public abstract void setQueue(MessageQueue newQueue);
    
    /**
     * Like {@link #setQueue(MessageQueue)}, except it instantiates one of the built-in queues based on default
     * parameters. Accepted arguments are {@link TrapEndpoint#BLOCKING_BYTE_QUEUE},
     * {@link TrapEndpoint#BLOCKING_MESSAGE_QUEUE}, {@link TrapEndpoint#REGULAR_BYTE_QUEUE} or
     * {@link TrapEndpoint#REGULAR_MESSAGE_QUEUE}.
     * <p>
     * If a specific queue class is not required, this method is recommended, as it will pick up the most efficient
     * built-in queue from the classpath based on the hosting JVM.
     * 
     * @param type
     *            The new queue type to instantiate.
     */
    public void setQueueType(String type);
    
    /**
     * Sets the reconnect timeout. This timeout is the time that an endpoint is allowed to reside in the SLEEPING state
     * without any transports or way to contact the other endpoint. After this time, the endpoint will be forcibly
     * closed, if no transport or wakeup mechanism is present.
     * 
     * @param reconnectTimeout
     *            The max number of milliseconds to sleep for.
     */
    public void setReconnectTimeout(long reconnectTimeout);
    
    /**
     * Sets the Trap message format for this endpoint. This method <i>should only be called on clients</i>, as the
     * server will automatically match the format with the client format.
     * <p>
     * Trap has two message formats available; the regular one, using 8-bit serialization, and a 7-bit safe
     * representation. The 7-bit one is recommended only for usage with pure JS clients that cannot otherwise handle
     * binary data.
     * 
     * @param trapFormat
     *            The new message format for this endpoint.
     */
    public abstract void setTrapFormat(Format trapFormat);
    
    /**
     * Fetches all the available authentication context, for all transports; including, where available, IP numbers,
     * ports, etc. This context will be based on {@link TrapContextKeys}, among other detail information, and consist of
     * a map for each transport, indexed by transport name. This information will provide detail level information on
     * the connection for each transport.
     * <p>
     * Typical output for a single-transport scenario may look as follows:
     * 
     * <pre>
     * {
     *  socket= {
     *      LastAlive=1407198698446,
     *      Format=REGULAR, 
     *      State=AVAILABLE, 
     *      Transport=socket/1/AVAILABLE/704f459c, 
     *      LocalIP=127.0.0.1, 
     *      LocalPort=59277, 
     *      Configuration=trap.enablecompression = true
     *      RemotePort=59278, 
     *      Protocol=tcp, 
     *      RemoteIP=127.0.0.1, 
     *      Priority=-100
     *      }
     *  }
     * </pre>
     * 
     * From this information, we can gather all pertinent information about the transport's internal state. This is a
     * socket transport that is not configured to connect, so was provided by a server endpoint. The state is AVAILABLE,
     * and lastAlive specifies when there was traffic seen. We have the TCP 5-tuple information, which can be used to
     * identify a connection that has not changed.
     * <p>
     * This information is generally available to a {@link TrapAuthentication} instance assigned to the endpoint and/or
     * transports. The authentication will be automatically invoked whenever any changes may occur on the underlying
     * connection, such as a new TCP session, unsecure packet, new HTTP request, transport switchover, or a new
     * transport being introduced. <b>If this information is needed to provide <i>security</i>, TrapAuthentication
     * should be used</b>.
     * <p>
     * This method is intended for <i>informational</i> purposes. The returned values are immediately polled, will
     * include all possible values, and will be requested synchronously; the CPU cost of using this method often is
     * non-negligible. The method is thread safe with no need for synchronization.
     * 
     * @return A map of all authentication context available.
     * @since 1.1.5
     */
    public Map<String, Map<String, Object>> getTransportAuthenticationContexts();
    
}
