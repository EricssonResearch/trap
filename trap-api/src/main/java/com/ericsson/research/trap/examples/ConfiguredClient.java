package com.ericsson.research.trap.examples;

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



import com.ericsson.research.trap.TrapChannel;
import com.ericsson.research.trap.TrapClient;
import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.TrapFactory;
import com.ericsson.research.trap.delegates.OnData;
import com.ericsson.research.trap.delegates.OnError;
import com.ericsson.research.trap.delegates.OnOpen;
import com.ericsson.research.trap.spi.TrapTransportPriority;
import com.ericsson.research.trap.utils.StringUtil;

/**
 * Similar to the configured server, this sample shows how to tweak a client's configuration.
 * <p>
 * {@sample ../../../src/main/java/com/ericsson/research/trap/examples/ConfiguredClient.java sample}
 * 
 * @author Vladimir Katardjiev
 * @since 1.1
 */
//BEGIN_INCLUDE(sample)
public class ConfiguredClient implements OnOpen, OnData
{
    
    /** A reference to the client, to prevent GC */
    public TrapClient client;
    
    /** Counter on the sent messages */
    public int        counter = 0;
    
    /**
     * Creates a new ConfiguredClient to connect to trapCfg
     * 
     * @param trapCfg
     *            Trap CFG to connect to
     * @throws TrapException
     *             Hopefully not.
     */
    public ConfiguredClient(String trapCfg) throws TrapException
    {
        // Create a new Trap Client to the specified host.
        // The second parameter of true tells the client to attempt to autodiscover other transports.
        // If it was set to false, the client can only ever use the manually configured transports.
        this.client = TrapFactory.createClient("http://trap.example.com:4000", true);
        
        // While the above would work if we had a server at trap.example.com, we don't have that luxury.
        // For testing purposes, we'll use the provided trapCfg.
        this.client = TrapFactory.createClient(trapCfg, true);
        
        // The same enable/disable commands as in ConfiguredServer work just as well on the client
        this.client.disableTransport("loopback");
        
        // We also have the same configuration parameters in setOption/configureTransport
        this.client.setOption(TrapEndpoint.OPTION_LOGGERPREFIX, "example");
        
        // Tell the client to use us as a delegate.
        this.client.setDelegate(this, true);
        
        // We may need to handle a different event by another object.
        // To that end, the second argument to setDelegate is false. This will add this delegate
        // without removing any delegates that don't conflict with it.
        this.client.setDelegate(new OnError() {
            
            public void trapError(TrapEndpoint endpoint, Object context)
            {
                System.err.println("Error occurred!");
            }
        }, false);
        
        /*
         * Keepalives are important. By default, Trap uses conservative timeouts for both clients and servers.
         * If a client requires something else, they can reset it on-the-fly. (These methods work just as well on servers)
         * In the example above, the worst-case scenario is that it may take up to 30 seconds to close an endpoint. 
         * A keepalive every 15 seconds, plus 5 seconds for the server responds, allows for 20 seconds before the endpoint discovers
         * all transports are dead and moves to SLEEPING. It is then given 10 seconds in SLEEPING to connect. Failing that, it moves to ERROR.
         */
        
        // Send a keepalive roughly every 15 seconds. The exact timing may be tweaked by Trap.
        // This value is communicated over the network (hence truncated to an int) and must be within the documented range.
        // Note this is counted in whole seconds.
        this.client.setKeepaliveInterval(15);
        
        // Allow a client up to 5000 milliseconds to receive a keepalive response. If it doesn't, it will assume the transport(s) are dead and attempt to reconnect.
        this.client.setKeepaliveExpiry(5000);
        
        // Allow a client up to 15 milliseconds to reconnect at least one transport.
        this.client.setReconnectTimeout(10000);
        
        // Asynchronous processing is generally faster and recommended. It also defaults to on.
        // The following line disables it, but should be avoided.
        this.client.setAsync(false);
        
        // By default, Trap allows all transports to be connected. This may be overkill for some clients as they don't saturate even a single one.
        // Setting maxActive to one will disconnect all but the most optimum transport, as determined by the transport priority.
        this.client.setMaxActiveTransports(1);
        
        // We can always reorder the priority, of course. This makes HTTP preferable to WebSocket
        this.client.getTransport("http").setTransportPriority(TrapTransportPriority.WEBSOCKET - 1);
        
        // Trap also has 64 channels. Channel 0 is the system channel, though, so avoid it. With the rest, we can have some fun
        // Channels are multiplexed. This means that we can send data on multiple channels simultaneously, i.e. transfer a large file
        // while sending small messages. Each channel is individually in-order.
        
        // Channels are defined as-needed. We can send on any channel, even if we haven't configured it.
        TrapChannel channel = this.client.getChannel(5);
        
        // Chunk size is the largest single Trap packet that should be sent over. Don't worry; they're reassembled on the other side!
        // Smaller values allow more multiplexing, but increase protocol overhead. Remember, each Trap packet is 16 bytes!
        channel.setChunkSize(1200);
        
        // Sets an upper limit on the number of bytes that the channel may be transmitting at any one time. Smaller values prevent this channel
        // from crowding lower priority channels, while larger values provide more throughput especially with latency.
        channel.setInFlightBytes(32000);
        
        // Trap will send from higher priority channels (up to in flight max bytes) before lower priority ones. Within the same priority group
        // it employs round-robin queueing. Default priority is 0 (=lowest), while maximum priority of Integer.MAX_VALUE is reserved for the control channel.
        channel.setPriority(26);
        
        // Channels can also be used as C-sockets. BEWARE HOWEVER! Once streaming is enabled, it MUST NOT be disabled for the channel
        // Streaming will trigger OnData events whenever data arrives on the network, with as little buffering as possible.
        // However, this also means Trap may (and will) chunk the data, so it may not arrive as one blob.
        // Use with caution (e.g. video streaming)
        channel.setStreamingMode(true);
        
        // Start connecting.
        this.client.open();
    }
    
    // Called when the client is open (=ready to send)
    public void trapOpen(TrapEndpoint endpoint, Object context)
    {
        try
        {
            // Sending on a channel is easy. Just use the channel number in the send command
            this.client.send(new byte[] {}, 5, false);
            
            // If you have a large message, it can be compressed! Compression is enabled/disabled on a per-message basis.
            // By sending it on a different channel than our stream channel, we ensure it isn't chunked.
            this.client.send(new byte[65535], 1, true);
        }
        catch (TrapException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    // Called when the client receives data. Since we connect to the echo server, the same message should come back.
    public void trapData(byte[] data, int channel, TrapEndpoint endpoint, Object context)
    {
        System.out.println("Got message: " + StringUtil.toUtfString(data));
        this.sendMessage();
    }
    
    private void sendMessage()
    {
        String message = "Message {" + this.counter++ + "}";
        System.out.println("Now sending: " + message);
        
        try
        {
            this.client.send(StringUtil.toUtfBytes(message));
        }
        catch (TrapException e)
        {
            e.printStackTrace();
        }
    }
    
}
//END_INCLUDE(sample)
