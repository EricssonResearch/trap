Trap Programming Model
====

This page details how Trap for Java deals with elements such as memory management, threading and thread safety, performance, and other platform-specific considerations a developer may need to know.

# Memory Management

While the allocation and freeing of memory is managed by the JVM, the specific triggers vary from application to application, and their respective assumptions. The Trap library for Java is based on a socket model of thinking when it comes to memory management and object deallocation. What that means is it basically boils down to two fundamental rules.

- The Trap library will allocate and initialise objects, using TrapFactory
- The Trap library will not prevent Trap objects from being garbage collected

In other words, any TrapEndpoint that the application does not reference using a strong reference will be garbage collected, any transports disconnected and cleared, and the session terminated. This includes TrapEndpoints spawned from a ListenerTrapEndpoint; Trap will not prevent any endpoint from being garbage collected.

	
Due to the interaction between keepalives & timers (below) and garbage collection, TrapEndpoints may not be immediately garbage collected when the last user-made reference to the endpoint is removed. A corollary of this is that an application can exhibit behaviour conflicting will be above statement. This behaviour must not be relied upon as it depends on library-internal timers. Trap users should keep a reference of an endpoint as long as they intend to use it, and not expect any more feedback once they discard the last reference. Preferably, before the reference is discarded, close the Trap Endpoint.

# Keepalives & Timers

Trap maintains a number of keepalives and timers for its objects. We posit that the actual computational power involved in verifying a keepalive is less than rescheduling the timers and, thus, will fire timers more often than absolutely necessary, instead of rescheduling them. For the same reason, if Trap is forced to reschedule a timer (e.g., shortened keepalive timer), it will reschedule by cancelling the future task and scheduling a near-term one. Eventually, futures will be purged and cleaned up, but this may cause short-term memory issues if Trap is allowed to schedule long keepalives that are manually overridden to short keepalives. For that reason, configure a client before opening it, and a listener before setting it to listen, and avoid re-configuring open endpoints when possible.

# Thread Management

Trap is designed from the ground up to rely on multiple threads via Thread Pools. The actual Thread Pool implementation is provided by the crap-utils package, and may be reused by other applications. Pooling may also be disabled by a sufficiently basic crap-utils package. In normal usage, Trap has the following thread characteristics:

- trap-core must not block any API calls that do not specify they are synchronous
- trap-core will not block any internal threads, preferring instead to use thread pool scheduled timers
- trap-core must block synchronous API calls (e.g. isAlive())
- transports may use threads internally as needed, but must not block calls from trap-core except such calls where this is required (e.g. isAlive())
- transports should deliver messages to trap-core without spawning additional threads; they should assume trap-core handles the message quickly and efficiently

In practice, this means that most Trap usage will spawn at least two threads, with three being more common. The threads are as follows:

- A sending thread, used for outgoing messages. This thread allows Trap to not block the application's send() calls.
- A receiving thread, used to dispatch messages to the app. This thread allows the application to take its time processing a message without affecting the transports.
- A nio thread, used for socket IO. This functionality is provided by the ER NIO library.

These thread separations are extremely important, as they may otherwise cause operations to block. Specifically, some Trap API functions, especially if called from a different thread compared to the one where the incoming message is received, would deadlock. To reiterate, Trap assumes fast operating, non-blocking callbacks internally. This thread setup can have a negative impact on received message performance, and can be slightly adjusted to compensate, see "Disabling the Receiving Thread" below.

# Thread Safety
The general rule-of-thumb on thread safety in Trap is as follows:

- The interfaces defined in com.ericsson.research.trap are protected from being corrupted from access by multiple threads, although the exact state of the object (configuration, actions, etc) may be difficult to predict.
- The interfaces defined in com.ericsson.research.trap.spi are not protected from multiple thread access, and assume a specific access model (noted above). Access to these is provided for advanced use-cases, but is more difficult to use than the general API.

TrapEndpoints are generally thread-safe constructs, in that calling a TrapEndpoint from multiple threads will not deadlock. Note, however, that this is not recommended as the outcome of calling a TrapEndpoint from multiple threads simultaneously is non-deterministic. For example, if TrapEndpoint.send() is called from multiple threads simultaneously, the ordering of the messages is undefined, although all messages will be transported to the other side. Depending on the use-case this may or may not be desired.

TrapTransport objects are not thread-safe and should be handled with care. It is recommended that TrapTransports are modified either before the TrapEndpoint they are associated with is opened, or, if absolutely necessary, the transport disabled before being modified. It may be possible to modify a transport while it is in use, but this is not a required mode of operation. Caveat emptor!

Finally, delegates. Delegate objects (TrapEndpointDelegate; TrapListenerDelegate) must not block, and must return quickly in all cases except for TrapEndpointDelegate.trapData() which, in the default configuration, may take a while to process its data. In all other cases, blocking or taking time in a delegate method will negatively impact Trap performance, and may even cause a connection to fail. Trap delegate callbacks are synchronised and synchronous, meaning that a delegate will only be called from one thread at a time. Trap makes no guarantees that it will be the same thread for every function call, however.

# Disabling the Receiving Thread
The receiving thread prevents deadlocks by adding a thread disconnect between the incoming data, which may come from the socket (or nio) thread, and the application. In order to prevent deadlocks, this break is necessary. In some circumstances, however, the application may be layering its own dispatching and thread break mechanism right on top of Trap – for example, a container with multiple parallel processing threads – and the thread break can cause a greater performance loss than a plain function call. Before disabling the receiving thread, note the following:

As noted above, but worth reiterating, a thread change is mandatory. If it does not exist, deadlocks may occur!
Trap delegate callbacks must execute very fast without a thread break. The callback will block the actual socket thread which will impact not only the active TrapEndpoint but eventual other endpoints as well!
The throughput gain is real, assuming careful coding, and this functionality is therefore exposed. The receiving thread can be enabled or disabled by calling TrapEndpoint.setAsync(), and the current value introspected by TrapEndpoint.isAsync(). As with most changes, it is recommended, for optimum performance, that the async mode is only modified before the endpoint is opened.