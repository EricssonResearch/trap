package com.ericsson.research.trap.impl;

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.zip.InflaterOutputStream;

import com.ericsson.research.trap.TrapChannel;
import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.impl.buffers.TrapMessageBufferImpl;
import com.ericsson.research.trap.impl.queues.LinkedMessageQueue;
import com.ericsson.research.trap.spi.TrapConstants;
import com.ericsson.research.trap.spi.TrapEndpointMessage;
import com.ericsson.research.trap.spi.TrapMessage;
import com.ericsson.research.trap.spi.TrapMessage.Operation;
import com.ericsson.research.trap.spi.TrapMessageBuffer;
import com.ericsson.research.trap.spi.TrapTransport;
import com.ericsson.research.trap.spi.queues.BlockingMessageQueue;
import com.ericsson.research.trap.spi.queues.MessageQueue;
import com.ericsson.research.trap.utils.ThreadPool;

public class TrapChannelImpl implements TrapChannel, Runnable
{
	
	private boolean				streamingEnabled	= false;
	private int					chunkSize;
	private int					maxInFlightBytes;
	private MessageQueue		outQueue;
	private TrapMessageBuffer	inBuf;
	private int					channelID			= 0;
	private TrapEndpointImpl	parentEP;
	private int					bytesInFlight		= 0;
	private Object				availabilityLock	= new Object();
	private boolean				available			= false;
	private boolean				running				= false;
	
	protected int				messageId			= 1;
	protected int				maxMessageId		= 0x8000000;	// Even number means we can slide the buffer evenly, without incurring buffer loop costs.
																	
	public TrapChannelImpl(TrapEndpointImpl trapEndpointImpl, int channelID)
	{
		this.channelID = channelID;
		this.parentEP = trapEndpointImpl;
		this.setChunkSize(TrapConstants.DEFAULT_CHUNK_SIZE);
		this.setInFlightBytes(this.getChunkSize() * 8);
		this.outQueue = this.parentEP.templateMessageQueue.createNewQueue();
		this.inBuf = new TrapMessageBufferImpl(8, 65535, 1, 1, this.maxMessageId);
	}
	
	public TrapChannel setStreamingMode(boolean streamingEnabled)
	{
		this.streamingEnabled = streamingEnabled;
		return this;
	}
	
	public boolean getStreamingMode()
	{
		return this.streamingEnabled;
	}
	
	public TrapChannel setChunkSize(int numBytes)
	{
		int newSize = numBytes;
		
		if (newSize > TrapMessageImpl.HEADER_SIZE)
			newSize -= TrapMessageImpl.HEADER_SIZE;
		
		if (newSize > this.parentEP.getMaxChunkSize())
			newSize = this.parentEP.getMaxChunkSize();
		
		if (newSize <= 0)
			newSize = Integer.MAX_VALUE;
		
		this.chunkSize = newSize;
		return this;
	}
	
	public int getChunkSize()
	{
		return this.chunkSize;
	}
	
	public TrapChannel setInFlightBytes(int maxBytes)
	{
		int chunkWithHeaders = this.chunkSize + TrapMessageImpl.HEADER_SIZE;
		if (maxBytes < chunkWithHeaders)
			throw new IllegalArgumentException("The number of in flight bytes requested (" + maxBytes + ") is less than the current chunk size of " + chunkWithHeaders);
		
		this.maxInFlightBytes = maxBytes;
		return this;
	}
	
	public int getInFlightBytes()
	{
		return this.maxInFlightBytes;
	}
	
	public TrapChannel setOutgoingMessageQueue(MessageQueue queue)
	{
		this.outQueue = queue;
		return this;
	}
	
	public MessageQueue getOutgoingMessageQueue()
	{
		return this.outQueue;
	}
	
	public TrapChannel setIncomingMessageBuffer(TrapMessageBuffer buffer)
	{
		this.inBuf = buffer;
		return this;
	}
	
	public TrapMessageBuffer getIncomingMessageBuffer()
	{
		return this.inBuf;
	}
	
	public void assignMessageID(TrapMessage message)
	{
		if (message.getMessageId() == 0)
		{
			synchronized (this)
			{
				// Assign message id (if not already set)
				int messageId = this.messageId++;
				
				if (messageId > this.maxMessageId)
					this.messageId = messageId = 1;
				
				message.setMessageId(messageId);
			}
		}
	}
	
	/**
     * Send a message on the channel. If required, splits up the message in multiple component parts. Note that calling
     * this method guarantees the message will be serialized.
	 * 
	 * @param message
	 *            The message to send.
	 * @throws TrapException
	 *             If an error occurs during sending
	 */
	public void send(TrapMessage message) throws TrapException
	{
		this.send(message, true);
	}
	
	protected void send(TrapMessage message, boolean allowChunk) throws TrapException
	{
		// We need to block this loop – somehow – in order to prevent message corruption from
		// simultaneous frgaments on the same channel. 
	    int chunkSize = this.chunkSize;
		synchronized (this)
		{
			this.assignMessageID(message);
			
			// Perform the estimate computation.
			if (allowChunk)
			{
					byte[] data = message.getCompressedData();
					if (data != null && data.length > chunkSize)
					{
						
						// We need to chunk it up.
						for (int i = 0; i < data.length; i += chunkSize)
						{
							byte[] chunk = Arrays.copyOfRange(data, i, Math.min(i + chunkSize, data.length));
							TrapMessage m = new TrapMessageImpl();
							m.setData(chunk);
							
							if (i == 0)
							{
								m.setOp(Operation.FRAGMENT_START);
								m.setMessageId(message.getMessageId());
							}
							else if (i + chunkSize >= data.length)
								m.setOp(Operation.FRAGMENT_END);
							else
								m.setOp(Operation.MESSAGE);
							
							m.setCompressed(message.isCompressed());
							m.setFormat(message.getFormat());
							this.send(m, false);
						}
						
						return;
						
					}
				}
			
		}
		
		message.setChannel(this.channelID);
		
		synchronized (this.outQueue)
		{
			this.outQueue.put(message);
		}
		
		synchronized (this.availabilityLock)
		{
			if (this.bytesInFlight < this.maxInFlightBytes)
				this.available = true;
		}
		
		this.parentEP.kickSendingThread();
	}
	
	public void messageSent(TrapMessage message)
	{
		synchronized (this.availabilityLock)
		{
			this.bytesInFlight -= message.length();
			
			if (this.bytesInFlight < this.maxInFlightBytes && this.outQueue.peek() != null)
			{
				this.available = true;
			}
		}
		this.parentEP.kickSendingThread();
	}
	
	LinkedList<TrapMessage>	failedMessages	= new LinkedList<TrapMessage>();
	
	public void addFailedMessage(TrapMessage failedMessage)
	{
		synchronized (this.availabilityLock)
		{
			this.failedMessages.add(failedMessage);
		}
	}
	
	public void rebuildMessageQueue()
	{
		LinkedList<TrapMessage> faileds = new LinkedList<TrapMessage>();
		
		synchronized (this.outQueue)
		{
			// Create a new, blank queue
			
			if (this.failedMessages.isEmpty())
				return;
			
			// We should iterate over the failed messages and remove them from the transit messages
			
			synchronized (this.availabilityLock)
			{
				Iterator<TrapMessage> fit = this.failedMessages.iterator();
				
				while (fit.hasNext())
					this.bytesInFlight -= fit.next().length();
				
			}
			
			MessageQueue newMessageQueue;
			try
			{
				newMessageQueue = this.outQueue.getClass().newInstance();
			}
			catch (Exception e)
			{
				newMessageQueue = new LinkedMessageQueue();
			}
			
			long blockingTimeout = -1;
			
			if (newMessageQueue instanceof BlockingMessageQueue)
			{
				blockingTimeout = ((BlockingMessageQueue) this.outQueue).blockingTimeout();
				((BlockingMessageQueue) newMessageQueue).setBlockingTimeout(-1); // Disable blocking to throw
			}
			
			// Rebuild the queue easily.
			LinkedList<TrapMessage> newQueue = new LinkedList<TrapMessage>();
			
			Iterator<TrapMessage> it = this.failedMessages.iterator();
			
			TrapMessage failed = it.next();
			
			while (failed != null && failed.getMessageId() == 0)
			{
				if (it.hasNext())
					failed = it.next();
				else
					failed = null;
			}
			
			TrapMessage queued = this.outQueue.peek();
			
			while ((failed != null) || (queued != null))
			{
				
				if (queued != null)
					this.outQueue.pop();
				
				if ((queued != null) && (failed != null))
				{
					if (queued.getMessageId() < failed.getMessageId())
					{
						newQueue.add(queued);
						queued = null;
					}
					else
					{
						newQueue.add(failed);
						failed = null;
					}
				}
				else if (failed == null)
				{
					newQueue.add(queued);
					queued = null;
				}
				else
				{
					newQueue.add(failed);
					failed = null;
				}
				
				if ((failed == null) && it.hasNext())
					failed = it.next();
				
				if (queued == null)
					queued = this.outQueue.peek();
			}
			
			// We'll need a new loop to eliminate duplicates.
			// This loop will actually defer the messages.
			int lastMessageId = -1;
			
			Iterator<TrapMessage> ni = newQueue.iterator();
			
			while (ni.hasNext())
			{
				TrapMessage m = ni.next();
				
				if (m.getMessageId() != lastMessageId)
				{
					
					lastMessageId = m.getMessageId();
					
					try
					{
						newMessageQueue.put(m);
					}
					catch (Throwable t)
					{
						faileds.add(m);
					}
				}
			}
			
			if (blockingTimeout > -1)
				((BlockingMessageQueue) newMessageQueue).setBlockingTimeout(blockingTimeout); // Disable blocking to throw
				
			this.outQueue = newMessageQueue;
			this.failedMessages.clear();
			
			if (this.bytesInFlight < this.maxInFlightBytes && this.outQueue.peek() != null)
			{
				this.available = true;
			}
		}
		
		if (faileds.size() > 0)
			this.parentEP.failedSendingDelegate.trapFailedSending(faileds, this.parentEP, this.parentEP.delegateContext);
	}
	
	public boolean messagesAvailable()
	{
		synchronized (this.availabilityLock)
		{
			return this.available;
		}
	}
	
	public TrapMessage peek()
	{
		if (this.messagesAvailable())
			return this.outQueue.peek();
		else
			return null;
	}
	
	public TrapMessage pop()
	{
		try
		{
			TrapMessage message = null;
			
			message = this.outQueue.pop();
			synchronized (this.availabilityLock)
			{
				
				if (message != null)
					this.bytesInFlight += message.length();
				
				if (this.outQueue.peek() == null || this.bytesInFlight >= this.maxInFlightBytes)
					this.available = false;
			}
			return message;
		}
		finally
		{
		}
	}
	
	public void receiveMessage(TrapMessage m, TrapTransport t)
	{
		this.inBuf.put(m, t);
		
		synchronized (this)
		{
			if (this.running)
				return;
			
			this.running = true;
			ThreadPool.executeCached(this);
		}
	}
	
	TrapEndpointMessage		tmp					= new TrapEndpointMessage();
	ByteArrayOutputStream	buf					= new ByteArrayOutputStream();
	boolean					receivingFragment	= false;
	private int				priority;
	
	// Performs the receiving task
	public void run()
	{
		for (;;)
		{
			try
			{
				while (this.inBuf.fetch(this.tmp))
				{
					
					if (!this.streamingEnabled)
					{
						if (this.receivingFragment)
						{
							switch (this.tmp.getMessage().getOp().getOp())
							{
								case Operation.Value.FRAGMENT_END:
									this.receivingFragment = false;
									this.tmp.getMessage().setOp(Operation.MESSAGE);
								case Operation.Value.MESSAGE:
									try
									{
										this.buf.write(this.tmp.getMessage().getData());
									}
									catch (IOException e)
									{
										parentEP.logger.error("IOException while defragmenting a message; {}", e, e);
										parentEP.close();
									}
									break;
								
								default:
									break;
							}
							
							if (!this.receivingFragment)
							{
								byte[] mData = this.buf.toByteArray();
								
								if (this.tmp.getMessage().isCompressed())
								{
									ByteArrayOutputStream bos = new ByteArrayOutputStream();
									InflaterOutputStream ios = new InflaterOutputStream(bos);
									
									try
									{
										ios.write(mData, 0, mData.length);
										ios.flush();
										mData = bos.toByteArray();
										
										ios.close();
										bos.close();
									}
									catch (IOException e)
									{
										parentEP.logger.error("IOException while defragmenting a message; {}", e, e);
										parentEP.close();
									}
								}
								
								this.tmp.getMessage().setData(mData);
								this.buf = new ByteArrayOutputStream();
							}
							else
							{
								continue;
							}
						}
						else
						{
							if (this.tmp.getMessage().getOp().equals(Operation.FRAGMENT_START))
							{
								this.receivingFragment = true;
								try
								{
									this.buf.write(this.tmp.getMessage().getData());
								}
								catch (IOException e)
								{
									parentEP.logger.error("IOException while defragmenting a message; {}", e, e);
									parentEP.close();
								}
								continue;
							}
						}
					}
					
					try
					{
						/*if (this.tmp.m instanceof TrapObjectMessage && this.parentEP.delegate instanceof TrapEndpointObjectDelegate)
							((TrapEndpointObjectDelegate) this.parentEP.delegate).trapObject(((TrapObjectMessage) this.tmp.m).getObject(), this.channelID, this.parentEP, this.parentEP.delegateContext);
						else
							this.parentEP.delegate.trapData(this.tmp.m.getData(), this.tmp.m.getChannel(), this.parentEP, this.parentEP.delegateContext);
							*/
						
						this.parentEP.executeMessageReceived(this.tmp.getMessage(), this.tmp.t);
					}
					catch (Exception e)
					{
						parentEP.logger.error("Unhandled exception while receiving message; {}", e, e);
					}
				}
				
			}
			catch (Exception e)
			{
				parentEP.logger.error("Unhandled exception while receiving message; {}", e, e);
			}
			finally
			{
				//System.out.println("Exiting run loop with available: " + this.inBuf.available());
			}
			
			synchronized (this)
			{
				
				if (this.inBuf.available() > 0)
					continue;
				
				this.running = false;
				return;
			}
		}
	}
	
	public TrapChannel setPriority(int newPriority)
	{
		this.priority = newPriority;
		return this;
	}
	
	public int getPriority()
	{
		return this.priority;
	}
	
	public String toString()
	{
		return "(" + this.channelID + "/o:" + this.outQueue.length() + "/i:" + this.inBuf.toString() + ")";
	}
	
}
