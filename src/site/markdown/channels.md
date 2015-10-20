Channels
====

Trap provides for multiplexed data transfer, in order to allow one physical connection to carry multiple logical flows. These logical flows can be completely independent of each-other, and, as such, may have different requirements. For example, one flow can be the transfer of a 1gb movie, while a separate flow contains short control messages, or user-to-user text messages.

Channels provide a way to separate out these flows.

With channels, the application can specify a channel for "bulk" data, and a channel for prioritised data. When data is sent on the prioritised channel, it will pre-empt the bulk transfer. This allows multiple separate flows to coexist without having to establish separate physical connections.

Channels are instantly available. You can send on any supported channel. Channel configuration can be asymmetric -- the server can have different bandwidths and priorities for a channel than the client. This allows proper prioritisation on asymmetric links, as many internet connections are.

# Using a channel

All channels are instantly available, and can be used at will. The default configuration uses _16kb_ chunks and _128kb_ in flight bytes. Within the same priority level, channels are cycled on a _round robin_ basis. All channels share the same priority, with the exception of the *trap control channel*, with ID 0. This channel should not be used.

Using a channel is as simple as specifying it when sending data. Thus, to send on channel 1:

	client.send(msg, 1, false) // Java
	client.send(msg, 1); // JavaScript
	
All channels can be used instantly.

# Configuring a channel

Channels can be customised as well. It is possible to change the _priority_ of a channel. The priority goes from positive to negative (highest to lowest priority), where the highest priority can pre-empt other traffic. Thus, a channel with priority _100_ will have a possibility to send traffic before a channel with priority _1_. If a link does not have enough bandwidth, it is possible for lower prioritised channels to be starved out.

Within the same priority level, messages will be _interleaved_. If there are five channels at level 0, they will each get to send out one _chunk_. They will not be starved out individually in case of a lack of bandwidth.

Channels are accessed using the _getChannel_ method.

	channel = client.getChannel(1);
	
Once a channel is accessed, several properties can be set. These include:

* **maxInflightBytes**: The number of bytes that can be in transit (=not acknowledged by the receiver) at any time. A larger number can increase throughput, but cause congestion.
* **streaming**: Normally, Trap treats each message as atomic. With streaming mode enabled, this distinction is removed, and Trap can merge or split messages to transport them faster. Media content such as video can greatly benefit from streaming mode.
* **chunkSize**: Trap will divide up larger messages into smaller chunks for transport (reassembling them on the receiver side). The default chunksize is 16kb, so Trap only provides 0.1% overhead. This can be tuned to fit the data format of a channel.
* **priority**: The relative priority of this channel as compared to other channels. Higher values give more priority to this channel.