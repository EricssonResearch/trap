package com.ericsson.research.trap;

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



import com.ericsson.research.trap.spi.TrapMessageBuffer;
import com.ericsson.research.trap.spi.queues.MessageQueue;

/**
 * A channel is a logical stream of Trap messages, multiplexed on the same Trap connection. Essentially, this allows
 * sending multiple streams over the same connection. This is useful when multiple forms of data may need to be
 * transported over a Trap session (e.g. short and long messages mixed), where a long/large message should not hold up a
 * short/small message.
 * <p>
 * In the default case, there are two channels on every TrapEndpoint. Channel ID 0 will consist of control traffic,
 * ensuring the endpoint is alive, managing transports, etc. It will have the highest priority, ensuring the endpoint
 * can manage itself. Channel ID 1 will consist of application data. It will yield to Channel ID 0, ensuring that the
 * application sending large messages will not cause control traffic to time out.
 * <p>
 * Trap version 1.2 supports up to 256 different channels. It is not recommended that Channel ID 0 is used for
 * application data, leaving 255 channels for the application to use. Each channel can have its
 * features individually configured.
 * <p>
 * When instantiated, channels have certain default settings. Trap's default implementation will use a chunk size of
 * 16KB, and limit to 128KB in-flight bytes per channel. The channels will not operate in streaming mode by default. The
 * default priority will be 0, except for Channel ID 0 which has the maximum priority.
 * <p>
 * The in-flight window will limit the throughput on fast links, while preventing us from oversaturating slow links. As
 * an example, assuming 100ms latency and 128kb window size, we will at most process 10 windows per second, or 1280kb/s.
 * 10ms latency yields 12800kb/s. Increasing the window size on a faster link will yield more throughput, but may risk
 * oversaturating a slower link.
 * 
 * @author Vladimir Katardjiev
 * @since 1.1
 */
public interface TrapChannel
{
    
    public static final int ID_MIN   = 0;
    public static final int ID_MAX   = 255;
    
    /**
     * Controls the <i>streaming</i> flag of the channel. When a channel works in streaming mode, it will dispatch
     * trapData events as data is received, although always in the correct order. With streaming mode disabled, each
     * trapData event will represent a single send() event on the other side.
     * <p>
     * Streaming mode is useful for when Trap is used to transfer larger chunks of data, whose framing is internal to
     * the data transferred. For example, an image, a song, or a video stream. Streaming mode will reduce – but not
     * eliminate – the amount of buffering done in Trap.
     * 
     * @param streamingEnabled
     *            <i>true</i> to enable streaming mode on this channel, <i>false</i> otherwise.
     * @return This channel object, for chaining.
     */
    public abstract TrapChannel setStreamingMode(boolean streamingEnabled);
    
    /**
     * Queries the streaming mode of the channel.
     * 
     * @return <i>true</i> if streaming is enabled, <i>false</i> otherwise.
     */
    public abstract boolean getStreamingMode();
    
    /**
     * Set the chunk size – the maximum number of bytes allowed in each message. Note that the chunk size includes the
     * Trap message header, and this will be automatically subtracted from <i>numBytes</i>, unless numBytes is in the
     * range of [1, TRAP_HEADER_SIZE]. If numBytes is zero or negative, chunking will be disabled.
     * <p>
     * Note that a chunkSize of Integer.MAX_VALUE will disable chunking. A channel will have that value set if the
     * remote endpoint is suspected of not supporting chunking. Excepting that, chunkSize will automatically be reduced
     * to the trap config option {@link TrapEndpoint#OPTION_MAX_CHUNK_SIZE}, which is automatically negotiated between
     * the peers.
     * 
     * @param numBytes
     *            The new limit. This limit will be applied to subsequent messages, but will NOT apply retroactively.
     * @return This object, for chaining.
     */
    public abstract TrapChannel setChunkSize(int numBytes);
    
    /**
     * Accessor for the current chunk size, in bytes. This value will reflect the chunk size <i>without</i> the Trap
     * Message Headers. This leads to the peculiar behaviour that setChunkSize(x) will often not lead to getChunkSize()
     * == x
     * 
     * @return The current chunk size.
     */
    public abstract int getChunkSize();
    
    /**
     * Sets the maximum number of in-flight bytes. Combined with the chunk size, this limits the number of messages that
     * the channel will allow to be in transit at any given time.
     * <p>
     * Increasing the number of in flight bytes will increase the required buffer sizes on both the local and remote
     * ends, as well as the system's network buffers. It may also increase throughput, especially on congested links or
     * when using multiple transports.
     * <p>
     * Note that in-flight bytes differs from the queue size. The queue denotes how many messages/bytes this channel can
     * buffer, while in-flight bytes denotes how many messages/bytes we allow on the network.
     * 
     * @param bytes
     *            The maximum number of in-flight bytes.
     * @return This instance, for chaining.
     * @throws IllegalArgumentException
     *             If the maximum number of bytes is less than the chunk size (including the Trap message header).
     */
    public abstract TrapChannel setInFlightBytes(int bytes);
    
    /**
     * Accessor for the maximum number of in-flight bytes.
     * 
     * @return The current maximum bytes.
     */
    public abstract int getInFlightBytes();
    
    /**
     * Sets the message queue used for outgoing messages. See {@link MessageQueue} for details on the performance
     * implication of queues. This method should not be called once messages have been sent on this channel, but that
     * will not be verified.
     * 
     * @param queue
     *            The new queue instance
     * @return The TrapChannel, for chaining.
     */
    public abstract TrapChannel setOutgoingMessageQueue(MessageQueue queue);
    
    /**
     * Retrieves the outgoing message queue used by this channel. Whether any properties can be changed on the queue
     * depends on the queue type.
     * 
     * @return The queue used by the channel.
     */
    public abstract MessageQueue getOutgoingMessageQueue();
    
    /**
     * Sets the incoming message buffer. This buffer is used to ensure that incoming Trap messages maintain their proper
     * order while being dispatched. The buffer will also have certain size parameters, and can affect channel
     * performance.
     * <p>
     * Like MessageQueues, buffers should not be changed once messages start flowing. Like MessageQueues, this will not
     * be verified.
     * 
     * @param buffer
     *            The new buffer to use.
     * @return The TrapChannel, for chaining.
     */
    public abstract TrapChannel setIncomingMessageBuffer(TrapMessageBuffer buffer);
    
    /**
     * Accessing the currently in use message buffer allows changing of the size parameters. The actual parameters that
     * can be tweaked depend on the buffer instance.
     * 
     * @return The current incoming buffer in use.
     */
    public abstract TrapMessageBuffer getIncomingMessageBuffer();
    
    /**
     * Sets the new channel priority, relative to the other channels. Channel ID 0 has priority
     * {@link Integer#MAX_VALUE}, meaning any traffic on 0 takes precedence of any other traffic, for any reason.
     * <p>
     * Priority is byte based. A channel with priority <i>n</i> will be allowed to send up to <i>n</i> bytes before
     * ceding transmission rights to a transport with lower priority. Note that if <i>chunkSize</i> exceeds priority,
     * the transport will nevertheless be allowed to send <i>chunkSize</i> number of bytes.
     * <p>
     * Priority only affects the scheduling order of messages, and not the throughput. For the exact buffering, one must
     * consider the channel's in-flight limit, the endpoint's in-flight limit (if any), as well as the transports'
     * in-flight limit.
     * 
     * @param newPriority
     *            The new priority value to set. May be any number.
     * @return The TrapChannel, for chaining.
     */
    public abstract TrapChannel setPriority(int newPriority);
    
    /**
     * Accessor for the channel's byte priority.
     * 
     * @return The current byte priority.
     */
    public abstract int getPriority();
    
}
