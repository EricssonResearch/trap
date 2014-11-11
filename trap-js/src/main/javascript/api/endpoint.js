
Trap.Endpoint = function(){};
Trap.Endpoint.prototype = new Trap.Settings;
Trap.Endpoint.prototype.constructor = Trap.Endpoint;

Trap.Endpoint.State = 
{
		CLOSED : "Trap.Endpoint.State.CLOSING",
		OPENING : "Trap.Endpoint.State.OPENING",
		OPEN : "Trap.Endpoint.State.OPEN",
		SLEEPING : "Trap.Endpoint.State.SLEEPING",
		ERROR : "Trap.Endpoint.State.ERROR",
		CLOSING : "Trap.Endpoint.State.CLOSING"
};

/**
 * Closes this Trap endpoint, terminating any outstanding Trap transports.
 */
Trap.Endpoint.prototype.close = function(){};

/**
 * Attempts to queue data for sending. If the queue length is exceeded, it
 * may block or throw an exception, as per the queue type.
 * <p>
 * Please note that while send will accurately send the data over to the
 * other endpoint, it is advisable to instead use {@link #send(TrapObject)}
 * if the data being sent is a serialized object. If the other endpoint is
 * locally deployed, the TrapObject will never be serialized, thus saving on
 * large amounts of processing power.
 * 
 * @param data
 * @throws TrapException
 *             if the queue length is exceeded, or a timeout occurs on a
 *             blocking queue
 */
Trap.Endpoint.prototype.send = function(string){};

/**
 * Alters the queue length. The queue length determines how many messages or
 * bytes to buffer before throwing. The default behaviour is to denote how
 * many messages to buffer, with an optional behaviour specifying bytes, for
 * memory-sensitive devices.
 * <p>
 * If the length is stated as Long.MAX_VALUE, no maximum queue length will
 * be enforced.
 * 
 * @param length
 */
Trap.Endpoint.prototype.setQueueLength = function(length){};

/**
 * Retrieves the current queue length.
 * 
 * @return the current queue length.
 */
Trap.Endpoint.prototype.getQueueLength = function(){};

/**
 * Sets the current queue type.
 * 
 * @param type
 */
Trap.Endpoint.prototype.setQueueType = function(type){};

/**
 * Fetches the current active queue type.
 */
Trap.Endpoint.prototype.getQueueType = function(){};

/**
 * Assigns an authentication instance to this TrapEndpoint. Implicitly,
 * requires that this transport is authenticated from now on, on any
 * incoming connection.
 * <p>
 * Some transports may require more fine-grained authentication than is
 * afforded (i.e. authenticate every message, outgoing or incoming), and so
 * they must individually access the TrapAuthentication instance using the
 * getter.
 * 
 * @param authentication
 * @throws TrapException
 *             If the authentication was not compatible with the transports.
 */
Trap.Endpoint.prototype.setAuthentication = function(authenticationObject){};

/**
 * Fetches the authentication instance for this Trap endpoint. Can be used
 * by individual transports to challenge and/or authenticate incoming
 * messages, if the transport requires more authentication than the default
 * Trap security policy.
 * 
 * @return The TrapAuthentication instance used by the endpoint.
 */
Trap.Endpoint.prototype.getAuthentication = function(){};

/**
 * Fetches the last known liveness timestamp of the endpoint. This is the
 * last time it received a message from the other end. This includes all
 * messages (i.e. also Trap messages such as keepalives, configuration, etc)
 * so must not be confused with the last known activity of the other
 * application. For example, in the case of a JavaScript remote endpoint,
 * this does not guarantee an evaluation error has not rendered the JSApp's
 * main run loop as inoperable.
 * 
 * @see #isAlive(long, boolean, boolean, long) if all that is needed is an
 *      evaluation of the liveness status.
 * @return The timestamp of the last message received from the remote side.
 */
Trap.Endpoint.prototype.lastAlive = function(){};

/**
 * Attempts to verify that the endpoint is alive, with a round trip time of
 * at most <i>timeout</i> milliseconds. This function is primarily intended
 * for when an application requires to know it has connectivity <i>right
 * this instant</i>, and should be used sparingly on power-constrained
 * devices.
 * <p>
 * {@link #isAlive(long)} is synonymous to calling <i>isAlive(timeout, true,
 * true, timeout)</i>.
 * <p>
 * Note that isAlive is synchronous. Further note that isAlive will take
 * longer time to execute than <i>timeout</i> as it will both try to verify
 * transport liveness, and then reconnect the transport if verification
 * fails. Transport reconnection uses a timer longer than <i>timeout</i>, to
 * allow for additional round-trips used when connecting.
 * <p>
 * <b>Warning:</b> Calling <i>isAlive</i> on a Server Trap Endpoint (i.e.
 * when none of the transports can perform the open() function) may cause a
 * client to lose its connectivity. The client may not have discovered the
 * underlying transport is dead yet, and may require more time to reconnect.
 * A wakeup mechanism can be used to establish liveness, but the server's
 * <i>timeout</i> value should be significantly more generous in order to
 * accommodate the client's reconnect and/or wakeup procedure!
 * 
 * @param timeout
 *            The timeout (in milliseconds) for the round-trip between the
 *            server.
 * @return <i>true</i> if the connection is currently alive (including if
 *         this function successfully re-established the connection),
 *         <i>false</i> otherwise.
 */
Trap.Endpoint.prototype.isAlive = function(timeout, callback){};

/**
 * Attempts to verify if the endpoint is alive, or has been alive within a
 * certain number of milliseconds. Effectively, this can be used to trigger
 * a keepalive check of the endpoint if used with a <i>within</i> parameter
 * of 0 and a <i>check</i> parameter of true.
 * <p>
 * This function has a two-part purpose. The first is for the application to
 * be able to check the last known liveness of the endpoint, to reduce the
 * discovery time of a dead connection. The second is to trigger a check for
 * a dead endpoint, when the application needs to know that it has active
 * connectivity.
 * <p>
 * Note that in normal operation, the endpoint itself will report when it
 * has disconnected; the application does not need to concern itself with
 * this detail unless it specifically needs to know that it has connectivity
 * right now.
 * <p>
 * <b>Warning:</b> Calling <i>isAlive</i> on a Server Trap Endpoint (i.e.
 * when none of the transports can perform the open() function) may cause a
 * client to lose its connectivity. The client may not have discovered the
 * underlying transport is dead yet, and may require more time to reconnect.
 * A wakeup mechanism can be used to establish liveness, but the server's
 * <i>timeout</i> value should be significantly more generous in order to
 * accommodate the client's reconnect and/or wakeup procedure!
 * 
 * @param within
 *            Within how many milliseconds the last activity of the endpoint
 *            should have occurred before the endpoint should question
 *            whether it is alive.
 * @param check
 *            Whether the endpoint should attempt to check for liveness, or
 *            simply return false if the last known activity of the endpoint
 *            is not later than within.
 * @param force
 *            Whether to attempt to force reconnection if the transports are
 *            not available within the given timeout. This will ensure
 *            available liveness value reflects what is possible right now,
 *            although it may mean disconnecting transports that still may
 *            recover.
 * @param timeout
 *            If check is true, how many milliseconds at most the liveness
 *            check should take before returning false anyway. The
 *            application can use this value if it has a time constraint on
 *            it.
 * @return <i>true</i> if the connection is currently alive (including if
 *         this function successfully re-established the connection),
 *         <i>false</i> otherwise.
 */
Trap.Endpoint.prototype.isAlive = function(within, check, force, timeout, callback){};

/*
 * Declared Fields
 */
Trap.Endpoint.prototype.state = Trap.Endpoint.State.CLOSED;

// These callbacks replace the Delegate pattern used in Java.
/**
 * Called when the Trap endpoint has received byte data from the other end.
 * This method executes in a Trap thread, so it should only perform minimal
 * operations before returning, in order to allow for maximum throughput.
 * 
 * @param data
 *            The data received.
 */
Trap.Endpoint.prototype.onmessage = function(evt){};

/**
 * Called when Trap changes state. Includes both the new state, and the
 * previous one.
 * 
 * @param newState
 *            The state Trap changed to.
 * @param oldState
 *            The previous state.
 */
Trap.Endpoint.prototype.onstatechange = function(evt){};

/**
 * Called when a Trap Endpoint knows it has failed to send some messages.
 * This can occur when the Trap Endpoint is killed forcibly, loses all its
 * transports while still having an outgoing buffer, or fails to wake up a
 * client that has disconnected all its transports normally.
 * <p>
 * Note that there are conditions when Trap may unwittingly lose data (such
 * as data sent during a switch from unauthenticated -> authenticated
 * session, when the authentication is triggered from the remote side), so
 * the sum of data received by the other end, and called on this method, may
 * be different. Nevertheless, any data returned by this method definitely
 * failed to send.
 * 
 * @param datas
 *            A collection of transportable objects that failed sending.
 *            Usually byte arrays, but may contain TrapObject instances.
 */
Trap.Endpoint.prototype.onfailedsending = function(evt){};

/**
 * Gets the amount of maximum simultaneously active transports.
 * <p>
 * The number of max simultaneously active transports governs the behaviour
 * of Trap Endpoints (both client and server). If the number of active
 * transports equals the number of maximum simultaneous transports, recovery
 * will only take place for transports with higher priority than the
 * currently active transport.
 * <p>
 * If the number of active transports exceeds the maximum, transports will
 * be closed, starting with the lowest priority (largest integer). Note that
 * this is a <i>soft limit</i>. The Trap implementation may exceed the
 * maximum number of transports for a transitional period, in order to
 * ensure a smooth handover.
 * 
 * @return The current maximum simultaneously active transports
 */
Trap.Endpoint.prototype.getMaxActiveTransports = function(){};

/**
 * Sets the maximum allowed simultaneous transports. See
 * {@link #getMaxActiveTransports()} for more information.
 * 
 * @param newMax
 *            The new maximum number of transports.
 *            <p>
 *            Note that 0 (zero) is a permitted value for maximum number of
 *            transports. This will not close the Trap connection, but cause
 *            a non-deterministic sequence of transport opening and
 *            closings, causing an abnormal load on client, server and
 *            network, but making it difficult to predict which transport
 *            (and thus connection) will be used at any given point in time.
 *            This can be used to mitigate suspected MITM attacks, but is
 *            not recommended otherwise. This connection sequence is not
 *            guaranteed secure. <i>This (zero) functionality is for
 *            experimental purposes, and may be removed at any time!</i>
 */
Trap.Endpoint.prototype.setMaxActiveTransports = function(newMax){};