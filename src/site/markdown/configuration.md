Trap Configuration
====

Trap employs a simple, hierarchical, key-value configuration system. Each key corresponds to one value exactly. There is one key pair per line, separated by an equals sign. The line is terminated by a newline character. A typical configuration could look as follows

	trap.host = 127.0.0.1
	trap.keepalive.interval = 15
	trap.transport.http.port = 8080

Trap Configuration can be supplied anywhere a "config" string is used, even if that same field can also accept a prefix. For example, when creating `new Trap.ClientEndpoint` in JavaScript, a configuration string can be included.

Global Configuration Options
====
The following options apply globally.

option | type | default | meaning
-------|------|---------|--------
trap.keepalive.interval | integer | 15 | Number of seconds (approximate) between each static keepalive
trap.keepalive.expiry | long | 5000 | Number of milliseconds to wait before concluding a keepalive has expired.
trap.concurrent-connection-window | long | 30000 | Number of milliseconds to use additional server-side resources for speeding up handshakes.
trap.max-active-transports | int | 1 | The number of transports that shall be kept open once negotiation has concluded.
trap.max-connecting-transports | int | 4 | The number of transports to attempt to connect at any one time. A larger number speeds up negotiation time, but requires more CPU and bandwidth.
trap.host | String | `none` | The hostname or IP number to bind to. This value will be inherited by all transports.
trap.loggerprefix | String | `none` | A prefix to prepend to the Trap loggers. This allows users to channel Trap data into separate loggers.
trap.enablecompression | bool | true | Enables/disables compression support. This is used in negotiation, whether or not to even accept compression. If disabled, overrides the sender to not compress data even if requested by the API.
trap.maxchunksize | int | -1 | Sets the maximum chunk size globally. Channels cannot override this. Set to 0 or less to disable chunking.
trap.auto_hostname | String | `none` | Hostname for automatic configuration. Set this to the client-visible hostname, if different from the bind hostname.

Transport Configuration Options
====

Transports can be configured independently. Each transport has a number of configurable parameters. The default ones are included below.

Loopback Transport
----

The loopback transport has very few options. Note that the "loopback" transport provides two transports: the asynchronous one, and a synchronous one for debug purposes.

option | type | default | meaning
-------|------|---------|--------
trap.transport.asyncloopback.enabled | bool | true | Enables/Disables this transport.
trap.transport.asyncloopback.priority | int | -1100 | The relative transport priority.
trap.transport.loopback.enabled | bool | false | Enables/Disables this transport.
trap.transport.loopback.priority | int | -1000 | The relative transport priority.

Socket Transport
----

option | type | default | meaning
-------|------|---------|--------
trap.transport.socket.enabled | bool | true | Enables/Disables this transport.
trap.transport.socket.priority | int | -100 | The relative transport priority.
trap.transport.socket.host	| String 	| `0.0.0.0`	| The host (IP address) to bind to on the server, and the hostname or IP address to connect to on the client.
trap.transport.socket.port	| int	| 0 | The port to bind to on the server, and the port to connect to on the client
trap.transport.socket.autoconfig.host	| String | `none` |	The hostname or ip address to provide for getClientConfiguration() purposes. This must be an IP number that can be accessed for the intended clients of the Trap server, though it must not be a public IP number or hostname. No validation will be performed on this value.
trap.transport.socket.autoconfig.port	| int	| `none` |The port to provide for getClientConfiguration() purposes. This must be the port that clients can connect to. No validation will be performed on this.

Websocket Transport
----

option | type | default | meaning
-------|------|---------|--------
trap.transport.websocket.enabled | bool | true | Enables/Disables this transport.
trap.transport.websocket.priority | int | 0 | The relative transport priority.
trap.transport.websocket.wsuri	| String | `none` |	The URI to connect the WebSocket to. Used to send configuration to clients. Automatically generated on the server, specified on the client
trap.transport.websocket.host	| String 	| `0.0.0.0`	| The host (IP address) to bind to on the server
trap.transport.websocket.port	| int	| 0 | The port to bind to on the server
trap.transport.websocket.autoconfig.host| String | `none` |	The hostname or ip address to provide for getClientConfiguration() purposes. This must be an IP number that can be accessed for the intended clients of the Trap server, though it must not be a public IP number or hostname. No validation will be performed on this value.
trap.transport.websocket.autoconfig.port	| int	| `none` |The port to provide for getClientConfiguration() purposes. This must be the port that clients can connect to. No validation will be performed on this.

HTTP Transport
----

option | type | default | meaning
-------|------|---------|--------
trap.transport.http.enabled | bool | true | Enables/Disables this transport.
trap.transport.http.priority | int | 1000 | The relative transport priority.
trap.transport.http.url	| String | `none` |	The URL to connect the WebSocket to. Used to send configuration to clients. Automatically generated on the server, specified on the client
trap.transport.http.host	| String 	| `0.0.0.0`	| The host (IP address) to bind to on the server
trap.transport.http.port	| int	| 0 | The port to bind to on the server
trap.transport.http.autoconfig.host	| String | `none` |	The hostname or ip address to provide for getClientConfiguration() purposes. This must be an IP number that can be accessed for the intended clients of the Trap server, though it must not be a public IP number or hostname. No validation will be performed on this value. This parameter is required when using HttpServlet transport (automatic in Servlet 3.0 containers).
trap.transport.http.autoconfig.port	| int	| 0 | The port to provide for getClientConfiguration() purposes. This must be the port that clients can connect to. No validation will be performed on this. This parameter is required when using HttpServlet transport (automatic in Servlet 3.0 containers).
trap.transport.http.autoconfig.scheme	| String | `http` or `http` |	The scheme to use for connecting. Generally, http or https. This parameter is recommended for servlet containers, but not required.
trap.transport.http.autoconfig.path	| String | `autodetected` |	The path to use for connecting. This can generally be automatically derived, and should not be overriden.
trap.transport.http.expirationDelay	| int	| 30000 | The number of milliseconds to leave the long polling connection open. This value can be set both on the server and on the client. Only the shortest value will matter; the client's value will be propagated to the server.

Prefix Shortening
====

Prefixes in the configuration can be shortened, and preceding whitespace ignored. Thus, a longer configuration can be shortened to this:

	trap.
		.loggerprefix = myapp
		.keepalive.interval = 15
		
	trap.transport.http.
		.enabled = true
		.url = http://trapserver.com:8888
