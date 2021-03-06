
/**
 * Defines the methods available for interacting with the various Trap
 * transports.
 * <p>
 * Applications should not need to spend too much effort in configuring the
 * specifics of the Trap transports. At most, applications should suffice with
 * using the enable/disable functionality, and leave Trap to manage the rest.
 * 
 * @author Vladimir Katardjiev
 */
Trap.Transport = function(){};

Trap.Transport.Options = 
{
		Enabled: "enabled",
		ENABLED: "enabled",
		Priority: "priority",
};

Trap.Transport.State =
{
		DISCONNECTED: "trap.transport.state.DISCONNECTED",
		CONNECTING: "trap.transport.state.CONNECTING",
		CONNECTED: "trap.transport.state.CONNECTED",
		AVAILABLE: "trap.transport.state.AVAILABLE",
		UNAVAILABLE: "trap.transport.state.UNAVAILABLE",
		DISCONNECTING: "trap.transport.state.DISCONNECTING",
		ERROR: "trap.transport.state.ERROR",
};

/**
 * Checks whether the given Trap transport is enabled (i.e. it will react to
 * any calls other than configuration).
 * 
 * @return <i>true</i> if the transport is enabled, <i>false</i> otherwise.
 */
Trap.Transport.prototype.isEnabled = function(){};

/**
 * Enables this transport. Does not imply that this transport should
 * connect; {@link #connect()} must be called separately for that to happen.
 */
Trap.Transport.prototype.enable = function(){};

/**
 * Disables this transport, preventing it from participating in the
 * transport abstraction. Unlike {@link #enable()}, disable <b>does imply
 * the transport must close</b> and refuse to carry other messages. Disable
 * may not fail.
 */
Trap.Transport.prototype.disable = function(){};

/**
 * Checks if this transport is currently connected to the other end. Does
 * not imply whether or not it is possible for this transport to connect.
 * 
 * @return <i>true</i> if this transport object represents an active
 *         connection to the other end, <i>false</i> otherwise.
 */
Trap.Transport.prototype.isConnected = function(){};

/**
 * Signals to this transport that it should attempt to connect to the remote
 * endpoint. The transport may attempt to connect synchronously,
 * asynchronously or not at all according to its own configuration.
 * <p>
 * Not all transport instances are able to open an outgoing connection (e.g.
 * server instances) and, as such, some instances may throw an exception
 * when calling this method. If the transport does not support outgoing
 * connections, it must throw an exception immediately.
 * 
 * @throws TrapException
 *             If this transport does not support outgoing connections.
 */
Trap.Transport.prototype.connect = function(){};

/**
 * Signals to this transport that it must disconnect. The transport must
 * immediately take all measures to close the connection, must clean up as
 * much as it can, and may not throw any exceptions while doing so.
 * <p>
 * May NOT block.
 */
Trap.Transport.prototype.disconnect = function(){};

/**
 * Fetches this transport's priority, which is used in the comparable
 * implementation to sort transports, if needed. Currently unused.
 * 
 * @return The transport's priority
 */
Trap.Transport.prototype.getTransportPriority = function(){};

/**
 * Sets this transport's priority.
 * 
 * @param priority
 */
Trap.Transport.prototype.setTransportPriority = function(priority){};

/**
 * Gets this transport's name. The transport name is used for, among other
 * things, log outputs and configuration settings, must be alphanumeric and
 * contain no spaces.
 * 
 * @return The transport's name.
 */
Trap.Transport.prototype.getTransportName = function(){};

/**
 * Configures a specific transport setting (key/value)
 * 
 * @param configurationKey
 * @param configurationValue
 * @throws TrapException
 */
Trap.Transport.prototype.configure = function(configurationKey, configurationValue){};

/**
 * Configures a specific transport setting (key/value)
 * 
 * @param configurationKey
 * @param configurationValue
 * @throws TrapException
 */
Trap.Transport.prototype.configure = function(configurationKey, configurationValue){};

/**
 * Sets the Transport's configuration object. This configuration object is
 * shared with the parent.
 * 
 * @param configuration
 */
Trap.Transport.prototype.setConfiguration = function(configuration){};

/**
 * Returns a configuration string representing this transport's
 * configuration.
 * 
 * @return A String representation of the configuration of this
 *         TrapTransport.
 */
Trap.Transport.prototype.getConfiguration = function(){};

/**
 * Set an authentication instance for this transport, to be used for
 * authenticating any messages that need authentication.
 * 
 * @param authentication
 * @throws TrapException
 *             If the transport and Authentication instances are not
 *             mutually compatible. This transport should then be discarded.
 */
Trap.Transport.prototype.setAuthentication = function(authentication){};

/**
 * Queries if this transport can perform a connection, i.e. if it can act as
 * a client transport.
 * 
 * @return <i>true</i> if this transport can perform an outgoing connection,
 *         <i>false</i> otherwise.
 */
Trap.Transport.prototype.canConnect = function(){};

/**
 * Queries if this transport can accept incoming connections, i.e. if it can
 * act as a server.
 * 
 * @return <i>true</i> if this transport can receive incoming connections,
 *         <i>false</i> otherwise.
 */
Trap.Transport.prototype.canListen = function(){};

/**
 * Attempts to send a message with this transport. If the transport cannot
 * send this message, in full, right now, it MUST throw an exception. The
 * transport MUST NOT buffer messages if the <i>expectMore</i> flag is
 * false. The transport MAY buffer messages if <i>expectMore</i> is
 * <i>true</i> but this is not required.
 * 
 * @param message
 *            The message to send.
 * @param expectMore
 *            A flag signifying to the transport that more messages will be
 *            sent in a short timespan (less than 1ms). Some transports may
 *            wish to buffer these messages before sending to optimise
 *            performance.
 * @throws TrapException
 *             If an error occurred during the sending of the message(s).
 *             The exception MUST contain the message(s) that failed
 *             sending. If it contains more than one message, the order must
 *             be in the same order that send() was called.
 */
Trap.Transport.prototype.send = function(message, expectMore){};

/**
 * Asks if the transport is available for sending. Effectively checks the
 * transport's state for available, but this way is faster.
 * 
 * @return <i>true</i> if the transport can be used to send a message,
 *         <i>false</i> otherwise.
 */
Trap.Transport.prototype.isAvailable = function(){};

/**
 * Sets the Trap ID of this transport.
 * 
 * @param id
 */
Trap.Transport.prototype.setTrapID = function(id){};

/**
 * Returns the Trap ID of this transport.
 * 
 * @return The Trap ID.
 */
Trap.Transport.prototype.getTrapID = function(){};

/**
 * Fetches the transport's current state
 * 
 * @return The transport's current state.
 */
Trap.Transport.prototype.getState = function(){};

/**
 * Fetches the last known liveness timestamp of the transport. This is the
 * last time it received a message from the other end.
 * 
 * @return The timestamp of the last message received from the remote side.
 */
Trap.Transport.prototype.lastAlive = function(){};

/**
 * Attempts to verify if the transport is alive, or has been alive within a
 * certain number of milliseconds. Effectively, this can be used to trigger
 * a keepalive check of the transport if used with a <i>within</i> parameter
 * of 0 and a <i>check</i> parameter of true.
 * <p>
 * This function has a two-part purpose. The first is for the upper layer to
 * be able to check the last known liveness of the transport, to reduce the
 * discovery time of a dead connection. The second is to trigger a check for
 * a dead transport, when the application needs to know that it has active
 * connectivity.
 * <p>
 * Note that in normal operation, the transport itself will report when it
 * has disconnected; the upper layer does not need to concern itself with
 * this detail unless it specifically needs to know that it has connectivity
 * right now.
 * 
 * @param within
 *            Within how many milliseconds the last activity of the
 *            transport should have occurred before the transport should
 *            question whether it is alive.
 * @param check
 *            Whether the transport should attempt to check for liveness, or
 *            simply return false if the last known activity of the
 *            transport is not later than within.
 * @param timeout
 *            If check is true, how many milliseconds at most the liveness
 *            check should take before returning false anyway. The
 *            application can use this value if it has a time constraint on
 *            it.
 * @return <i>true</i> if the connection is currently alive (including if
 *         this function successfully re-established the connection),
 *         <i>false</i> otherwise.
 */
Trap.Transport.prototype.isAlive = function(within, check, timeout, callback){};

/**
 * Called when a Trap Transport has received a TrapMessage.
 * 
 * @param message
 */
Trap.Transport.prototype.onmessage = function(evt){};

/**
 * Called when the Trap Transport changes state.
 * 
 * @param newState
 * @param oldState
 */
Trap.Transport.prototype.onstatechange = function(evt){};

/**
 * Called when the Trap Transport knows that it has failed to send message(s)
 * 
 * @param messages
 */
Trap.Transport.prototype.onfailedsending = function(evt){};