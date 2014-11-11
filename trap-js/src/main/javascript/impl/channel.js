/**
 * @param {Trap.Endpoint}
 *            endpoint
 * @param {Number}
 *            channelID
 * @return void
 */
Trap.Channel = function(endpoint, channelID)
{
	this._parentEP = endpoint;
	this._channelID = channelID;
	this._streamingEnabled = false;
	this._chunkSize = 16*1024;
	this._maxInFlightBytes = this._chunkSize * 8;
	this._bytesInFlight = 0;
	this._available = false;

	this._messageId = 1;
	this._maxMessageId = 0x8000000;
	this._priority = 0;

	this._outQueue = new Trap.List();
	this._inBuf = new Trap.MessageBuffer(50, 1000, 1, 1, this.maxMessageId);

	this.failedMessages = new Trap.List();

	this.tmp = {};
	this.buf = new Trap.ByteArrayOutputStream();
	this.receivingFragment = false;

};

Trap._compat.__defineGetterSetter(Trap.Channel.prototype, "parentEP");
Trap._compat.__defineGetterSetter(Trap.Channel.prototype, "channelID");
Trap._compat.__defineGetterSetter(Trap.Channel.prototype, "streamingEnabled");
Trap._compat.__defineGetter(Trap.Channel.prototype, "chunkSize");
Trap._compat.__defineGetterSetter(Trap.Channel.prototype, "maxInFlightBytes");
Trap._compat.__defineGetterSetter(Trap.Channel.prototype, "bytesInFlight");
Trap._compat.__defineGetterSetter(Trap.Channel.prototype, "available");
Trap._compat.__defineGetterSetter(Trap.Channel.prototype, "messageId");
Trap._compat.__defineGetterSetter(Trap.Channel.prototype, "maxMessageId");
Trap._compat.__defineGetterSetter(Trap.Channel.prototype, "outQueue");
Trap._compat.__defineGetterSetter(Trap.Channel.prototype, "inBuf");
Trap._compat.__defineGetterSetter(Trap.Channel.prototype, "priority");

Trap._compat.__defineSetter(Trap.Channel.prototype, "chunkSize", function(numBytes)
{
	var newSize = numBytes;

	if (newSize > 16) newSize -= 16;

	if (newSize > this.parentEP.getMaxChunkSize()) newSize = this.parentEP.getMaxChunkSize();

	if (newSize <= 0) newSize = Integer.MAX_VALUE;

	this._chunkSize = newSize;
	return this;
});

/**
 * @param {Trap.Message}
 *            message
 * @return null
 */
Trap.Channel.prototype.assignMessageID = function(message)
{
	if (message.getMessageId() == 0)
	{
		// Assign message id (if not already set)
		var messageId = this.messageId++;

		if (messageId > this.maxMessageId) this.messageId = messageId = 1;

		message.setMessageId(messageId);
	}
};

/**
 * Send a message on the channel. If required, splits up the message in multiple
 * component parts. Note that calling this method guarantees the message will be
 * serialized.
 * 
 * @param {Trap.Message}
 *            message The message to send.
 * @throws TrapException
 *             If an error occurs during sending
 * @return void
 */
Trap.Channel.prototype.send = function(message, disableChunking)
{

	this.assignMessageID(message);

	// Perform the estimate computation.
	if (!disableChunking)
	{
		var data = message.getCompressedData();
		if (data != null && data.length > this.chunkSize)
		{

			// We need to chunk it up.
			for ( var i = 0; i < data.length; i += this.chunkSize)
			{
				var chunk = Trap.subarray(data, i, Math.min(i + this.chunkSize, data.length));
				var m = new Trap.Message();
				m.setData(chunk);

				if (i == 0)
				{
					m.setOp(Trap.Message.Operation.FRAGMENT_START);
					m.setMessageId(message.getMessageId());
				}
				else if (i + this.chunkSize >= data.length)
					m.setOp(Trap.Message.Operation.FRAGMENT_END);
				else
					m.setOp(Trap.Message.Operation.MESSAGE);

				m.setCompressed(message.getCompressed());
				m.setFormat(message.getFormat());
				this.send(m, true);
			}

			return;

		}
	}

	message.setChannel(this.channelID);
	this.outQueue.addLast(message);

	if (this.bytesInFlight < this.maxInFlightBytes) this.available = true;
};

/**
 * @param {Trap.Message}
 *            message
 * @return void
 */
Trap.Channel.prototype.messageSent = function(message)
{
	this.bytesInFlight -= message.length();

	if (this.bytesInFlight < this.maxInFlightBytes && this.outQueue.peek() != null) this.available = true;

	this.parentEP.kickSendingThread();
};

/**
 * @param {Trap.Message}
 *            failedMessage
 * @return null;
 */
Trap.Channel.prototype.addFailedMessage = function(failedMessage)
{
	this.failedMessages.add(failedMessage);
};

Trap.Channel.prototype.rebuildMessageQueue = function()
{

	if (this.failedMessages.isEmpty()) return;
	
	// We should iterate over the failed messages and remove them from the transit messages

	var fit = this.failedMessages.iterator();
	while (fit.hasNext())
		this.bytesInFlight -= fit.next().length();

	var newMessageQueue = new Trap.List();

	// Rebuild the queue easily.
	var newQueue = new LinkedList();

	var it = this.failedMessages.iterator();

	var failed = it.next();

	while (failed != null && failed.getMessageId() == 0)
	{
		if (it.hasNext())
			failed = it.next();
		else
			failed = null;
	}

	var queued = this.outQueue.peek();

	while ((failed != null) || (queued != null))
	{

		if (queued != null) this.outQueue.pop();

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

		if ((failed == null) && it.hasNext()) failed = it.next();

		if (queued == null) queued = this.outQueue.peek();
	}

	// We'll need a new loop to eliminate duplicates.
	// This loop will actually defer the messages.
	var lastMessageId = -1;

	var ni = newQueue.iterator();

	while (ni.hasNext())
	{
		var m = ni.next();

		if (m.getMessageId() != lastMessageId)
		{

			lastMessageId = m.getMessageId();
			newMessageQueue.put(m);
		}
	}

	this.outQueue = newMessageQueue;
	this.failedMessages.clear();

	if (this.bytesInFlight < this.maxInFlightBytes && this.outQueue.peek() != null)
	{
		this.available = true;
	}

};

/**
 * 
 * @returns {Boolean}
 */
Trap.Channel.prototype.messagesAvailable = function()
{
	return this.available;
};

/**
 * 
 * @returns {Trap.Message}
 */
Trap.Channel.prototype.peek = function()
{
	if (this.messagesAvailable()) return this.outQueue.peek();

	return null;
};

/**
 * @returns {Trap.Message}
 */
Trap.Channel.prototype.pop = function()
{
	var message = null;

	message = this.outQueue.pop();

	if (message != null) this.bytesInFlight += message.length();

	if (this.outQueue.peek() == null || this.bytesInFlight >= this.maxInFlightBytes) this.available = false;

	return message;
};

/**
 * @param {Trap.Message}
 *            m
 * @param {Trap.Transport}
 *            t
 * @returns void
 */
Trap.Channel.prototype.receiveMessage = function(m, t)
{
	this.inBuf.put(m, t);

	for (;;)
	{
		try
		{
			while (this.inBuf.fetch(this.tmp, false))
			{

				if (!this.streamingEnabled)
				{
					if (this.receivingFragment)
					{
						switch (this.tmp.m.getOp())
						{
							case Trap.Message.Operation.FRAGMENT_END:
								this.receivingFragment = false;
								this.tmp.m.setOp(Trap.Message.Operation.MESSAGE);
							case Trap.Message.Operation.MESSAGE:
								this.buf.write(this.tmp.m.getData());
								break;
	
							default:
								break;
						}

						if (!this.receivingFragment)
						{
							this.tmp.m.setData(this.buf.toArray());
							
							if (this.tmp.m.getCompressed())
								this.tmp.m.setData(new Zlib.Inflate(this.tmp.m.getData()).decompress());
							
							this.buf = new Trap.ByteArrayOutputStream();
						}
						else
						{
							continue;
						}
					}
					else
					{
						if (this.tmp.m.getOp() == Trap.Message.Operation.FRAGMENT_START)
						{
							this.receivingFragment = true;
							this.buf.write(this.tmp.m.getData());
							continue;
						}
					}
				}
				
				this.parentEP.executeMessageReceived(this.tmp.m, this.tmp.t);
			}

		}
		catch (e)
		{
			console.log(e.stack);
		}
		finally
		{
			// System.out.println("Exiting run loop with available: " +
			// this.inBuf.available());
		}

		if (this.inBuf.available() > 0) continue;

		return;
	}
};

Trap.Channel.prototype.toString = function()
{
	return "(" + this.channelID + "/o:" + this.outQueue.length() + "/i:" + this.inBuf.toString() + ")";
};
