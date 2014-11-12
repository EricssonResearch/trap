Trap Quickstart (Java)
====

In general, Trap attempts to expose an asynchronous socket API. The basic operations (and the ones covered in this document) are:

- *Listen* for incoming connections
- *Accept* an incoming connection
- *Open* an outgoing connection
- *Send* data
- *Receive* data

This document will exemplify how to do all of the above. In addition the API docs provide [full examples](./apidocs) on how to use Trap.

# Installation

Add Trap to your project either as a fully-blown, self-contained archive, or piecemeal as Maven Artifacts. Trap depends on slf4j for its logging; if embedded, slf4j will be included.

## Single Dependency Model

Trap provides a basic package that includes four transports in a single package. These four basic transports should work in most circumstances and serve as a starting point on using Trap. These can be included by adding the following Maven dependency

		<dependency>
			<groupId>com.ericsson.research.trap.packaging</groupId>
			<artifactId>trap-full</artifactId>
			<version>${project.version}</version>
		</dependency>
		
## Selective Transport Model

The other way to include Trap is to explicitly select which transports to use. All transports depend on the Trap API, so it will be included by default in any dependency. See the [Trap Transports](../trap-transports) project for a list of the current Trap transports and their options. 

# Listening for Incoming Connections
The first step in having a connection is opening a socket for listening. In Trap, this is done by creating a Trap listener. For this full example, see the [ConfiguredServer example](./apidocs/reference/com/ericsson/research/trap/examples/ConfiguredServer.html).

		// Create a new listener
        listener = TrapFactory.createListener();
        
        // At this point, we might want to configure it. For now, we'll go with default config.
        
        // Start the server.
        // The listener will retain a reference to our delegate (the TrapEchoServer object).
        listener.listen(new EchoServer());
        
This will start a Trap listener on any available port. To specify some ports, configure the Trap Listener to assign ports to each transport.

		// Create a new listener
        listener = TrapFactory.createListener();
        
        // At this point, we might want to configure it. For now, we'll go with default config.
        listener.configureTransport("http", "port", "4000");
        listener.configureTransport("websocket", "port", "4001");
        listener.configureTransport("socket", "port", "4002");
        
        // Start the server.
        // The listener will retain a reference to our delegate (the TrapEchoServer object).
        listener.listen(new EchoServer());
        
With the above, we configure the ports of the above transports; however, they will bind to 0.0.0.0 and may not properly react to being NAT-ed.

As the caller, we must maintain a permanent reference to the _listener_ object. Failure to do so will cause it to be garbage collected, and all resources released.

# Accepting an Incoming Connection
When the TrapListener object detects an incoming connection, it will call the [OnAccept](./apidocs/reference/com/ericsson/research/trap/delegates/OnAccept.html) interface on the object provided to the _listen()_ method. In this case, our EchoServer class implements it.

    public void incomingTrapConnection(TrapEndpoint endpoint, TrapListener listener, Object context)
    {
        // Endpoints received by this method are NOT strongly referenced, and may be GC'd. Thus, we must retain
        clients.add(endpoint);
        
        // We also want feedback from the endpoint.
        endpoint.setDelegate(this, true);
    }

The first line stores the new connection (TrapEndpoint) in a set, to prevent it from being garbage collected. Like the listener, if the endpoint object becomes garbage collected, the connection is closed automatically. The second line sets the _delegate_ object to receive callbacks from the endpoint. See [Delegates](./delegates.html) for more description on delegates.

# Opening an Outgoing Connection
Once we have a listener, we are able to connect to it. For this full example, see the [ConfiguredClient example](./apidocs/reference/com/ericsson/research/trap/examples/ConfiguredClient.html).

	// Create a new Trap Client to the specified host.
    // The second parameter of true tells the client to attempt to autodiscover other transports.
    // If it was set to false, the client can only ever use the manually configured transports.
    this.client = TrapFactory.createClient("http://trap.example.com:4000", true);
    
    // Tell the client to use us as a delegate.
    this.client.setDelegate(this, true);
    
    // Start connecting.
    this.client.open();
    
There is at least one delegate method that needs to be implemented by the client for this to work properly, and that is OnOpen. That delegate will be called when the connection has been opened.

    // Called when the client is open (=ready to send)
    public void trapOpen(TrapEndpoint endpoint, Object context)
    {
        try
        {
            // Send some bytes
            this.client.send(new byte[65535], 1, true);
        }
        catch (TrapException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
# Sending Data

As shown above, data is sent by using the _send()_ method. The data will be serialised and sent to the other side. Trap includes built-in deflate compression. It can be enabled and disabled on a per-message basis, and is activated by the third parameter of the send method. The second argument is the Channel ID to send on. See the APIdocs for [TrapChannel](./apidocs/reference/com/ericsson/research/trap/TrapChannel.html).

# Receiving Data

When an endpoint receives data, it will call the OnData or OnObject delegate, as applicable. OnData will only be called if an object that implements the TrapObject interface was provided to send. Otherwise, OnData will be called as follows

    // Called when the client receives data. Since we connect to the echo server, the same message should come back.
    public void trapData(byte[] data, int channel, TrapEndpoint endpoint, Object context)
    {
        System.out.println("Got message: " + StringUtil.toUtfString(data));
        this.sendMessage();
    }
    
# Next Steps
The above functionality provides the basics of Trap. The next topics are:

- The [Delegate Model](./delegates.html) for how to handle errors and other events.
- The [Channel Model](../channels.html) for how Trap multiplexing works.
- The [Configuration Model](../configuration.html) on how Trap is configured.
