package com.ericsson.research.trap.spi.queues;

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



import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.spi.TrapMessage;

/**
 * A FIFO queue for messages. Implementations of MessageQueue must provide a thread-safe, FIFO queue of messages. Some
 * implementations may block when asked to put a message, others may block.
 * <p>
 * The specific properties of the queue affect the performance, throughput and memory footprint of Trap. For example, a
 * <i>blocking</i> queue would wait for messages to be sent before sending new ones. It is useful in a high-throughput
 * environment to prevent the buffer from growing endlessly due to a slight mismatch in processing speed. An
 * array-backed buffer is good for static memory usage but may require too much memory for certain applications.
 * <p>
 * Trap ships with a number of built-in buffer types, described below. All of these classes are defined in the package
 * <i>com.ericsson.research.trap.impl.queues</i> in the <i>trap-core</i> maven artefact.
 * <ul>
 * <li><b>ArrayBlockingMessageQueue</b> is an array-backed, blocking queue, with a limit based on the number of messages
 * in the queue. Useful for static memory usage in a high-throughput environment. The size of an Array-backed queue
 * needs to be chosen with consideration; too low and not enough messages will fit and the queue will block too often.
 * Too high, and the queue uses too much RAM from the JVM.
 * <li><b>LinkedBlockingMessageQueue</b> is a LinkedList-backed, blocking queue, with a limit of number of messages. A
 * good, low-memory, blocking queue type where Trap does not need to monitor byte allocations. Selected by
 * {@link TrapEndpoint#BLOCKING_MESSAGE_QUEUE} if no better linked queue is present.
 * <li><b>LinkedByteMessageQueue</b> is a LinkedList-backed queue that computes its size based on the number of bytes in
 * the queue, as opposed to the number of messages. This is a generally slower queue type from a computational
 * perspective, but can be useful to prevent an application from oversaturating the network on slower networks. The
 * application can then verify how much data is queued for sending. Default implementation for
 * {@link TrapEndpoint#REGULAR_BYTE_QUEUE}.
 * <li><b>LinkedByteBlockingMessageQueue</b> is the same as a LinkedByteMessageQueue except it blocks on the number of
 * bytes as well, providing a throttling mechanism. This provides a way to ceiling the amount of data Trap can and will
 * accept. Default implementation for {@link TrapEndpoint#BLOCKING_BYTE_QUEUE}.
 * <li><b>LinkedMessageQueue</b>. The simplest of queue types, this provides a LinkedList-backed queue with no
 * limitations. This is the lowest-overhead queue, but provides no throttling mechanisms. Default implementation for
 * {@link TrapEndpoint#REGULAR_MESSAGE_QUEUE}.
 * </ul>
 * <p>
 * The following queues are available in the <i>trap-ext-queues-15</i> maven artefact:
 * <ul>
 * <li><b>CLQMessageQueue</b> is a ConcurrentLinkedQueue-based blocking queue, requiring Java 1.5 or later. It provides
 * high-throughput, variable-memory queue based on java.util.concurrent, but has a high number of object
 * allocation/deallocations. Replaces LinkedBlockingMessageQueue as the default implementation of
 * {@link TrapEndpoint#BLOCKING_MESSAGE_QUEUE}.
 * <li><b>CABMessageQueue</b> is an ArrayBlockingQueue-backed queue. It provides the highest sustained throughput of the
 * default Trap message queues, at the expense of requiring a fixed amount of memory.
 * </ul>
 * <p>
 * The default queue chosen by Trap will be either CLQMessageQueue or LinkedBlockingMessageQueue, with a fairly generous
 * limit of 1000 messages. This should provide high throughput on most situations, with latency determined by network.
 * If low latency (or not oversaturating the network) is required, Trap should be reconfigured to use a different
 * message buffer. A custom buffer may also be included, as long as it conforms to this interface, especially the
 * thread-safe part.
 * 
 * @author Vladimir Katardjiev
 */
public interface MessageQueue
{
    
    /**
     * Inserts a TrapMessage at the end of the queue
     * 
     * @param m
     *            The message to insert
     * @throws TrapException
     *             If the message could not be inserted
     */
    public void put(TrapMessage m) throws TrapException;
    
    /**
     * Looks at the first message in the queue
     * 
     * @return The first message of the queue
     */
    public TrapMessage peek();
    
    /**
     * Removes the first message of the queue
     * 
     * @return The removed message
     */
    public TrapMessage pop();
    
    /**
     * Current length of the queue.
     * 
     * @return The current length of the queue
     */
    public int length();
    
    /**
     * Maximum size of the queue
     * 
     * @return The maximum size of the queue.
     */
    public long size();
    
    /**
     * Accessor method for the general message queue type provided by this queue, as defined in {@link TrapEndpoint}.
     * This is no longer used by Trap, instead preferring to use {@link #createNewQueue()}. The method is retained for
     * debugging purposes.
     * 
     * @return A string representing the queue type name.
     */
    public String getQueueType();
    
    /**
     * Method to ask if the queue has more than one elements in it
     * 
     * @return <i>true</i> if the queue has more than one element, <i>false</i> otherwise.
     */
    public boolean hasMoreThanOne();
    
    /**
     * Creates a new queue with the same settings as this queue, but none of the messages.
     * 
     * @return The new queue. It should be of the same class as the old queue, with the same settings.
     * @since 1.1
     */
    public MessageQueue createNewQueue();
    
}
