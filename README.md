Transport Abstraction Package (trap)
====
<img src="https://travis-ci.org/EricssonResearch/trap.svg?branch=master"/>

This library provides a socket-based interface that abstracts a set of underlying transports (socket, websocket, http, functioncall), while allowing reconnects, network connection changes, compression and multiplexing. Trap can utilise one or more underlying connections to improve performance, is a fully binary protocol, and is designed to handle failures.

# Features
Trap provides

* Transport independent data transfer using a message paradigm.
* Optional, reliable, in-order hop-to-hop transfer, regardless of underlying medium.
* Connection re-establishment, when applicable. This recovers from a network connection change (e.g. wifi to cellular data)
* Multiplexing of up to 255 application streams, at configurable priority levels that allow pre-emption of bulk data.
* Support for Java and JavaScript clients and servers (node.js required for servers).

As an abstraction, Trap applies the following overheads (measured in Java, as of 2014)

* 16 bytes of protocol data per message (excluding transport overhead such as TCP/HTTP)
* Approximately 50 kilobytes of RAM per connection (configurable usage for active connections)
* A minimum of 200 kilobytes of RAM to load class files.
* Two threads for network & timing. Additional threads as required by the application.
* About 500 kilobytes .jar file (if packaging all features, including slf4j which Trap depends on)

# Installation
The easiest way to add Trap for Java is to include the full archive in one step. The following code will include Trap as well as the four default, built-in transports. These transports are not suitable in all contexts, but can serve as an entry point. 

		<dependency>
			<groupId>com.ericsson.research.trap.packaging</groupId>
			<artifactId>trap-full</artifactId>
			<version>${project.version}</version>
		</dependency>

The recommended way is to include the specific transports as needed. The full list of transports is available in the [main repo](https://github.com/EricssonResearch/trap/tree/master/trap-transports). Some transports, such as HTTP, have multiple providers. It is recommended to use the best provider available (e.g. Servlet) when applicable.

# Getting Started
For more detailed information, see the dedicated getting started guide for [Java](./trap-api/quickstart.html) or [JavaScript](./trap-js/index.html), or check out the [Java examples](./apidocs/reference/com/ericsson/research/trap/examples/package-summary.html). The rest of this guide assumes Java for clients and servers.

Listening for incoming connections
---

After you have included the Trap dependency, you can create a listener, which is the equivalent to a server socket.

	listener = TrapFactory.createListener("http://0.0.0.0:8888");
	
This will create and configure a listener with a fixed transport (http) to listen on any IP and port 8888. When the port is unspecified, Trap will automatically allocate available ports. The full list of ports can be accessed via <code>listener.getClientConfiguration()</code>.

The listener, while allocated, is not listening to the sockets yet. In order to start listening, you need to call

	listener.listen(delegate);
	
where _delegate_ is an object that implements at least the [OnAccept](./apidocs/reference/com/ericsson/research/trap/delegates/OnAccept.html) interface from the [Trap Delegate Interfaces](./apidocs/reference/com/ericsson/research/trap/delegates/package-summary.html). This delegate will be called by the listener whenever a new Trap connection is established. 

Whenever an incoming connection is received, the onAccept method will be called with a _TrapEndpoint_ object. The following code exemplifies how this is handled:

	void incomingTrapConnection(TrapEndpoint endpoint, TrapListener listener, Object context) {
		endpoint.setDelegate(myConnectionDelegate);
		myEndpointCollection.put(endpointKey, endpoint);
	}

Connecting to a server
---

Connect to a listener using a TrapClient.

	client = TrapFactory.createClient("http://127.0.0.1:8888");
	client.setDelegate(myDelegate);
	client.open();
	
This will create a client to connect using http to localhost, port 8888. The delegate should implement at least the [OnOpen](./apidocs/reference/com/ericsson/research/trap/delegates/OnOpen.html) interface to be notified when the connection is open. The _trapOpen()_ function will be called when the connection is established.

Sending data
---

Sending data is as simple as calling `TrapEndpoint.send()`.

# Links
* [Home Page](https://ericssonresearch.github.io/trap)
* [Source Code](https://github.com/ericssonresearch/trap)
* [Javadoc](https://ericssonresearch.github.io/trap/trap-api/apidocs/index.html)
* [JSDoc](https://ericssonresearch.github.io/trap/trap-js/jsdoc/index.html)
