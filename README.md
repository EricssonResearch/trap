Transport Abstraction Package (trap)
====

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
* A minimum of 200 kilobytes to load class files.
* Two threads for network & timing. Additional threads as required by the application.
* About 500 kilobytes .jar file (if using all features, including slf4j which Trap depends on)

# Installation
The easiest way to add Trap for Java is to include the full archive in one step. The following code will include Trap as well as the four default, built-in transports. These transports are not suitable in all contexts, but can serve as a quickstart 

		<dependency>
			<groupId>com.ericsson.research.trap.packaging</groupId>
			<artifactId>trap-full</artifactId>
			<version>1.3.0-SNAPSHOT</version>
		</dependency>

The recommended way is to include the specific transports as needed. The full list of transports is available in the [main repo](https://github.com/EricssonResearch/trap/tree/master/trap-transports). Some transports, such as HTTP, have multiple providers. It is recommended to use the best provider available (e.g. Servlet) when applicable.

# Getting Started
Have a look at [the examples](https://github.com/EricssonResearch/trap/tree/master/trap-api/src/main/java/com/ericsson/research/trap/examples). They provide a good Java quickstart. JavaScript is coming soon™.

# Documentation
