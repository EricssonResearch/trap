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



import com.ericsson.research.trap.TrapClient;
import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.TrapFactory;
import com.ericsson.research.trap.delegates.OnData;
import com.ericsson.research.trap.delegates.OnOpen;

/**
 * Showcases how to use Trap Channels to send multiple data flows at the same time.
 * <p>
 * This demo highlights how multiple priority levels can be used to designate some data as "more important", how in
 * flight bytes can be used to distribute bandwidth (when available), and how sending is interleaved.
 * <p>
 * We establish a HIGH PRIORITY channel, which will be scheduled immediately for transmission. We establish a STREAM
 * channel, which dispatches data as it is received (instead of message chunks). And we show how they interact with a
 * normal DATA channel.
 * <h3>Stream vs Message Performance</h3>
 * <p>
 * It is worth noting that the stream will have managed to echo, if one looks at the timestamp, almost 40000 bytes by
 * the time the data channel manages to transfer 32768 bytes. This appears to be at odds with the claim that those flows
 * are interleaved; however, one must consider that the stream is dispatched on <i>every</i> data packet, while the DATA
 * message is dispatched once the entire transfer completes. As such, the server will not begin to echo the DATA message
 * until the client finishes sending it. In contrast, the server can echo the STREAM data as soon as it is received.
 * <p>
 * If we had only done half the loop (i.e. print out the messages on the server), both the STREAM and DATA channel would
 * reach 32768 bytes at almost the same moment. There is no inherent performance penalty on a single hop for STREAM vs
 * MESSAGE. On a multi-hop basis there is, however. Additionally, STREAM does not need to buffer the entire message to
 * dispatch either, which may be desirable in some circumstances.
 * <p>
 * {@sample ../../../src/main/java/com/ericsson/research/trap/examples/MultiplexedClient.java sample}
 * 
 * @author Vladimir Katardjiev
 * @since 1.1
 */
//BEGIN_INCLUDE(sample)
public class MultiplexedClient implements OnOpen, OnData
{
    
    public static MultiplexedClient multiplexedClient;
    
    // To run the demo, you must have at least one transport on the classpath.
    public static void main(String[] args) throws Throwable
    {
        // Set up a server
        MultiplexedServer.main(args);
        
        // Create and run our client.
        multiplexedClient = new MultiplexedClient(MultiplexedServer.clientConfig);
    }
    
    public TrapClient       client;
    
    // This field is only necessary for the automated tests.
    public boolean          allDone           = false;
    
    public static final int HIGH_PRIO_CHANNEL = 10;
    public static final int DATA_CHANNEL      = 1;
    public static final int STREAM_CHANNEL    = 2;
    
    public MultiplexedClient(String trapCfg) throws TrapException
    {
        // Create a new Trap Client to the specified host.
        this.client = TrapFactory.createClient(trapCfg, true);
        
        // Tell the client to use us as a delegate.
        // We'll only handle two events this time: open and data
        this.client.setDelegate(this, true);
        
        // Time to configure the channels.
        
        // High priority channel should get a higher priority level.
        this.client.getChannel(HIGH_PRIO_CHANNEL).setPriority(10);
        
        // The two general data transfer channels will stay on the default priority (0), but we'll configure them differently.
        // For our example, we'll use a low chunk size to force chunking on relatively small messages.
        this.client.getChannel(DATA_CHANNEL).setChunkSize(116); // Note the chunk size includes the Trap header. So to get 100 byte data chunks, we use chunk size 116
        this.client.getChannel(STREAM_CHANNEL).setChunkSize(116);
        this.client.getChannel(HIGH_PRIO_CHANNEL).setChunkSize(116);
        
        // Limiting the in-flight bytes will also allow us to simulate somewhat network traffic.
        // Once a channel has this number of bytes in flight, it will not dispatch any more messages to the scheduler.
        // The scheduler will attempt to schedule the highest priority channel first, and then go down in a FCFS basis, until the transport is filled up.
        // The scheduler resets the starting position every second, or once all channels are scheduled, whichever comes first.
        this.client.getChannel(DATA_CHANNEL).setInFlightBytes(400);
        this.client.getChannel(STREAM_CHANNEL).setInFlightBytes(400);
        
        // We'll allocate more scheduling window for the high priority channel.
        this.client.getChannel(HIGH_PRIO_CHANNEL).setInFlightBytes(1600);
        
        // Finally, we'll change the STREAM channel to use Trap's streaming mode, to exemplify how that works.
        this.client.getChannel(STREAM_CHANNEL).setStreamingMode(true);
        
        // This next part is mainly for simulation purposes. We need to apply a transport that provides asynchronous latency
        // to highlight how the multiplexing works. To that end, we'll restrict all traffic to HTTP only.
        this.client.disableAllTransports();
        this.client.enableTransport("http");
        
        // Start connecting.
        this.client.open();
    }
    
    // Called when the client is open (=ready to send)
    public void trapOpen(TrapEndpoint endpoint, Object context)
    {
        // We'll fire off all three of our transfers!
        try
        {
            // No compression for ANY of them. We need to be inefficient for the simulation :-)
            
            // Send off 32768 bytes on the DATA channel. This will arrive as one trapData callback
            // The large chunk will arrive in the middle of the stream (=interleaved).
            this.client.send(new byte[32768], DATA_CHANNEL, false);
            
            // However, 65535 bytes on the STREAM channel will arrive as 656 trapData callbacks.
            // 655 with 100 bytes each, and one with 35 bytes.
            this.client.send(new byte[65535], STREAM_CHANNEL, false);
            
        }
        catch (TrapException e)
        {
            e.printStackTrace();
        }
    }
    
    int     streamMessages = 0;
    int     streamBytes    = 0;
    boolean gotHighPrio    = false;
    boolean gotData        = false;
    
    // Called when the client receives data. Since we connect to the echo server, the same message should come back.
    public void trapData(byte[] data, int channel, TrapEndpoint endpoint, Object context)
    {
        switch (channel)
        {
            case HIGH_PRIO_CHANNEL:
                this.out("Got " + data.length + " bytes on the HIGH PRIORITY channel");
                this.gotHighPrio = true;
                break;
            
            case DATA_CHANNEL:
                this.out("Got " + data.length + " bytes on the DATA channel");
                this.gotData = true;
                
                this.out("###");
                this.out("We'll now send off a HIGH PRIORITY message. This message will be large and throttle the number of STREAM messages that may be sent");
                this.out("Note, then, that this high priority message will be echoed in significantly shorter time than the DATA channel");
                this.out("###");
                try
                {
                    endpoint.send(new byte[65535], HIGH_PRIO_CHANNEL, false);
                }
                catch (TrapException e)
                {
                    e.printStackTrace();
                }
                break;
            
            // We'll throttle the stream channel to print some log messages.
            case STREAM_CHANNEL:
                this.streamMessages++;
                this.streamBytes += data.length;
                if ((this.streamMessages % 40) == 0 || this.streamBytes == 65535)
                    this.out("I have now received " + this.streamBytes + " bytes over " + this.streamMessages + " messages on the STREAM channel");
                break;
            
            default:
                this.out("Got unknown message on channel " + channel);
        }
        
        if (this.gotData && this.gotHighPrio && this.streamBytes == 65535)
        {
            this.out("All Done");
            this.allDone = true;
        }
    }
    
    long start = System.nanoTime();
    
    void out(String str)
    {
        long curr = (System.nanoTime() - this.start) / 1000;
        System.out.println(curr + ": " + str);
    }
    
}
//END_INCLUDE(sample)
