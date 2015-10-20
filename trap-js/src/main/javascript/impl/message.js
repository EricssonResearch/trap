	/*
	 * 
	 * 
	 * @author Vladimir Katardjiev
	 * @param {String|Uint8Array} inData A string or bytearray containing a full message.
	 * 
	 * @property {any[]} data
	 * @property 
	 * @constructor
	 */

Trap.Message = function(inData)
{
	
	this._data = [];
	this._authData = null;
	this._format = Trap.Message.Format.SEVEN_BIT_SAFE;
	this._op = Trap.Message.Operation.OK;
	this._compressed = false;
	this._channel = 0;
	
	this._messageId = 0;
	
	if (typeof(inData) != "undefined")
		this.deserialize(inData, 0, inData.length);
};


// Getters/setters
Trap._compat.__defineGetterSetter(Trap.Message.prototype, "messageId");
Trap._compat.__defineGetter(Trap.Message.prototype, "channel", function() {
	return this.format == Trap.Message.Format.REGULAR ? this._channel : 0;
});
Trap._compat.__defineSetter(Trap.Message.prototype, "channel");
Trap._compat.__defineGetterSetter(Trap.Message.prototype, "compressed");

Trap._compat.__defineGetter(Trap.Message.prototype, "data", function() {
	return this._data;
});

Trap._compat.__defineGetter(Trap.Message.prototype, "dataAsString", function() {
	return String.fromUTF8ByteArray(this._data);
});

Trap._compat.__defineGetter(Trap.Message.prototype, "string", function() {
	return String.fromUTF8ByteArray(this._data);
});
Trap._compat.__defineGetter(Trap.Message.prototype, "authData", function() {
	return this._authData;
});
Trap._compat.__defineGetter(Trap.Message.prototype, "format", function() {
	return this._format;
});
Trap._compat.__defineGetter(Trap.Message.prototype, "op", function() {
	return this._op;
});
Trap._compat.__defineGetter(Trap.Message.prototype, "channelID", function() {
	return this._channel;
});

Trap._compat.__defineSetter(Trap.Message.prototype, "data", function(newData) {
	
	if (typeof(newData) == "string")
		this._data = newData.toUTF8ByteArray();
	else if (typeof(newData.length) == "number" || typeof(newData.byteLength) == "number")
		this._data = newData;
	else if (typeof(newData) == "number")
		this._data = [newData];
	else if (typeof(newData) == "object")
		this._data = JSON.stringify(newData).toUTF8ByteArray();
	else
		throw "Invalid data supplied; not an array, not a string, not a number";
	
	return this;
});

Trap._compat.__defineSetter(Trap.Message.prototype, "authData", function(newAuthData){
	
	if (!!newAuthData && newAuthData.length > 65535) 
		throw "authData cannot be more than 65535 bytes";
	
	if (!!newAuthData && newAuthData.length != newAuthData.toUTF8ByteArray().length)
		throw "authData was not a US-ASCII string";
	
	this._authData = newAuthData;
	return this; 
});

Trap._compat.__defineSetter(Trap.Message.prototype, "format", function(newFormat){
	this._format = newFormat;
	return this;
});

Trap._compat.__defineSetter(Trap.Message.prototype, "op", function(newOp){
	this._op = newOp;
	return this;
});

Trap._compat.__defineSetter(Trap.Message.prototype, "channelID", function(newID){
	this._channel= newID;
	return this;
});

Trap.Message.Operation =
{
		
		OPEN: 1,
		OPENED: 2,
		CLOSE: 3,
		END: 4,
		CHALLENGE: 5,
		ERROR: 6,
		MESSAGE: 8,
		ACK: 9,
		FRAGMENT_START: 10,
		FRAGMENT_END: 11,
		OK: 16,
		PING: 17,
		PONG: 18,
		TRANSPORT: 19,
		name: function(op)
		{
			switch (op)
			{
				case 1:
					return "OPEN";
					
				case 2:
					return "OPENED";
					
				case 3:
					return "CLOSE";
					
				case 4:
					return "END";
					
				case 5:
					return "CHALLENGE";
					
				case 6:
					return "ERROR";
					
				case 8:
					return "MESSAGE";
					
				case 9:
					return "ACK";
					
				case 10:
					return "FRAGMENT_START";
					
				case 11:
					return "FRAGMENT_END";
					
				case 16:
					return "OK";
					
				case 17:
					return "PING";
					
				case 18:
					return "PONG";
					
				case 19:
					return "TRANSPORT";
					
				default:
					return "Unknown op type: " + op;
			}
		},
		
		getType: function(t)
		{
			return t;
		}
};

Trap.Message.Format = 
{
	REGULAR: "Trap.Message.Format.Regular",
	SEVEN_BIT_SAFE: "Trap.Message.Format.7bit",
	DEFAULT: "Trap.Message.Format.Regular"
};

Trap.Constants.MESSAGE_FORMAT_DEFAULT = Trap.Message.Format.DEFAULT;

Trap.Message.prototype.getBits = function(src, startBit, endBit)
{
	var mask = (Math.pow(2, endBit - startBit + 1) - 1);
	mask = mask << (32 - endBit);
	var rv = (src & mask) >> (32 - endBit);
	return rv;
};

Trap.Message.prototype.writeInt7 = function(src, bos)
{
	bos.write(this.getBits(src, 5, 11));
	bos.write(this.getBits(src, 12, 18));
	bos.write(this.getBits(src, 19, 25));
	bos.write(this.getBits(src, 26, 32));
};

Trap.Message.prototype.writeInt8 = function(src, bos)
{
	bos.write(this.getBits(src, 1, 8));
	bos.write(this.getBits(src, 9, 16));
	bos.write(this.getBits(src, 17, 24));
	bos.write(this.getBits(src, 25, 32));
};

Trap.Message.prototype.serialize = function(useBinary)
{
	var bos = (useBinary ? new Trap.ByteArrayOutputStream() : new Trap.ByteStringOutputStream());
	
	if (this.format == Trap.Message.Format.SEVEN_BIT_SAFE)
		this.serialize7bit(bos, useBinary);
	else
		this.serialize8bit(bos, useBinary);
	
	if (useBinary)
		return bos.toArray();
	else
		return bos.toString();
};

Trap.Message.prototype.getCompressedData = function()
{
	if (!this.compressed)
		return this.data;
	
	if (!this._compressedData)
		this._compressedData = new Zlib.Deflate(this.data).compress();
	
	return this._compressedData;
};


Trap.Message.prototype.serialize8bit = function(bos, useBinary)
{
	// Make 8-bit assertions
	if (this.data.length >= Math.pow(2, 32))
		throw "Asked to serialize more than 2^32 bytes data into a 8-bit Trap message";
	
	var b = 0;
	
	// First byte: |1|0| MESSAGEOP |
	b |= this.op | 0x80;
	bos.write(b);
	
	var authLen = (this.authData != null ? this.authData.length : 0);
	var mData = this.getCompressedData();
	
	// Second byte: |C|RSV1
	b = 0;
	
	if (this.compressed && useBinary)
		b |= 0x80;
	
	bos.write(b);
	
	// Third byte: Bits 3 - 9 of authLen
	bos.write(this.getBits(authLen, 17, 24));
	
	// Fourth byte: Bits 10 - 16 of authLen
	bos.write(this.getBits(authLen, 25, 32));
	
	// Bytes 5-8: MessageID
	this.writeInt8(this.getMessageId(), bos, true);
		
	// Byte 9: RSV2
	bos.write(0);
		
	// Byte 10: ChannelID
	bos.write(this.channelID);
		
	// Byte 11-12: RSV3
	bos.write(0);
	bos.write(0);
	
	// Byte 13-16: Data length
	this.writeInt8((mData.byteLength ? mData.byteLength : mData.length), bos, false);
	
	if (authLen > 0)
		bos.write(this.authData);
	
	bos.write(useBinary ? mData : String.fromUTF8ByteArray(mData));
};

Trap.Message.prototype.serialize7bit = function(bos, useBinary)
{
	if (this.data.length >= Math.pow(2, 28))
		throw "Asked to serialize more than 2^28 bytes data into a 7-bit Trap message";
	
	var b = 0;
	
	// First byte: |0|0| MESSAGEOP |
	b |= this.op;
	bos.write(b);
	
	var authLen = (this.authData != null ? this.authData.length : 0);
	
	// Second byte: First two bits of authLen
	bos.write(this.getBits(authLen, 17, 18));
	
	// Third byte: Bits 3 - 9 of authLen
	bos.write(this.getBits(authLen, 19, 25));
	
	// Fourth byte: Bits 10 - 16 of authLen
	bos.write(this.getBits(authLen, 26, 32));
	
	// Skip four bytes (RSV2, CHANID, RSV3)
	this.writeInt7(this.getMessageId(), bos, true);
	this.writeInt7(0, bos, true);
	this.writeInt7((this.data.byteLength ? this.data.byteLength : this.data.length), bos, false);
	
	// This will corrupt non-US-ASCII authData. Trap spec forbids it, so we're correct in doing so. 
	if (authLen > 0)
		bos.write(this.authData);

	bos.write(useBinary ? this.data : String.fromUTF8ByteArray(this.data));
	
};
	
	/**
	 * Attempts to deserialize a TrapMessage.
	 * 
	 * @param rawData
	 * @param length
	 * @param offset
	 * @return -1 if it could not parse a message from the data, the number of
	 *         bytes consumed otherwise.
	 * @throws UnsupportedEncodingException
	 *             if the message encoding is not supported
	 */
Trap.Message.prototype.deserialize = function(rawData, offset, length)
{

	
	if ((offset + length) > rawData.length)
		throw "Offset and length specified exceed the buffer";
	
	if (length < 16)
		return -1;
	
	var authLen;
	var contentLen;
	
	if ((rawData[offset + 0] & 0x80) != 0)
	{
		// 8-bit
		this.format = Trap.Message.Format.REGULAR;
		this.op = Trap.Message.Operation.getType(rawData[offset + 0] & 0x3F);
		this.compressed = (rawData[offset+1] & 0x80) != 0;
		this.channel = rawData[offset+9] & 0xFF;
		
		authLen = rawData[offset + 2] << 8 | rawData[offset + 3];
		this.messageId = rawData[offset + 4] << 24 | rawData[offset + 5] << 16 | rawData[offset + 6] << 8 | rawData[offset + 7];
		
		contentLen = rawData[offset + 12] << 24 | rawData[offset + 13] << 16 | rawData[offset + 14] << 8 | rawData[offset + 15];
	}
	else
	{
		// 7-bit
		this.format = Trap.Message.Format.SEVEN_BIT_SAFE;
		this.op = Trap.Message.Operation.getType(rawData[offset + 0] & 0x3F);
		
		authLen = ((rawData[offset + 1] & 0x03) << 14) | ((rawData[offset + 2] & 0x7F) << 7) | ((rawData[offset + 3] & 0x7F) << 0);
		this.messageId = ((rawData[offset + 4] & 0x7F) << 21) | ((rawData[offset + 5] & 0x7F) << 14) | ((rawData[offset + 6] & 0x7F) << 7) | ((rawData[offset + 7] & 0x7F) << 0);
		contentLen = ((rawData[offset + 12] & 0x7F) << 21) | ((rawData[offset + 13] & 0x7F) << 14) | ((rawData[offset + 14] & 0x7F) << 7) | ((rawData[offset + 15] & 0x7F) << 0);
		
		this.compressed = false;
		this.channel = 0;
	}
	
	// Verify that there's enough remaining content to read the message.
	var messageSize = 16 + authLen + contentLen;
	
	if (length < messageSize)
		return -1; // Cannot successfully read the remaining values.
		
	// Range of authHeader = (12, authLen)
	var startByte = offset + 16;
	
	// We have an authentication header!
	if (authLen > 0)
	{
		this.authData = Trap.subarray(rawData, startByte, startByte + authLen);
		
		// AuthData is a string, we should decode it...
		this.authData = String.utf8Decode(this.authData);
		
		startByte += authLen;
	}
	else
	{
		this.authData = null;
	}
	
	// Copy the data
	// We won't UTF-8 decode at this stage. If we do, it'll be harder to construct the .data and .string
	// properties when we dispatch the event. Instead, store data as an array and leave it to higher ups
	// to decide the representation
	this.data = Trap.subarray(rawData, startByte, startByte + contentLen);
	
	if (this.compressed)
		this.data = new Zlib.Inflate(this.data).decompress();
	
	// The number of bytes consumed. This allows multiple messages to be parsed from the same data block.
	return messageSize;
};

Trap.Message.prototype.length = function() {
	var l = 16;
	
	if (this.authData != null) l += this.authData.toUTF8ByteArray().length;
	if (this.getCompressedData() != null) l += this.getCompressedData().byteLength || this.getCompressedData().length;
	
	return l;
};