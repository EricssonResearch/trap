//< needs(trap)

Trap.ByteArrayOutputStream = function(initialLength) {
	// Set an initial length of the array, unless otherwise specified
	if (typeof (initialLength) != "number" || initialLength < 0)
		initialLength = 512;

	this.buf = new Uint8Array(initialLength);
	this.off = 0;
	this.size = initialLength;
	this.growthSize = 512;
};

Trap.ByteArrayOutputStream.prototype._remaining = function() {
	return this.size - this.off;
};

Trap.ByteArrayOutputStream.prototype._resize = function(newSize) {
	var copySize = Math.min(newSize, this.off);
	var newBuf = new Uint8Array(newSize);

	var src = (copySize < this.buf.length ? this.buf.subarray(0, copySize)
			: this.buf);
	newBuf.set(src, 0);
	this.buf = newBuf;
	this.size = this.buf.length;
};

Trap.ByteArrayOutputStream.prototype._checkAndResize = function(neededBytes) {
	if (this._remaining() < neededBytes)
		this._resize(this.size + Math.max(neededBytes, this.growthSize));
};

Trap.ByteArrayOutputStream.prototype.write = function(src, off, len) {
	if (typeof (src) == "number") {
		this._checkAndResize(1);
		this.buf[this.off++] = src;
		return;
	}

	if (typeof (off) != "number")
		off = 0;

	if (typeof (len) != "number")
		len = (src.byteLength ? src.byteLength : src.length);

	if (typeof (src) == "string") {

		var result = src.toUTF8ByteArray();
		this._checkAndResize(result.length);
		for ( var i = 0; i < result.length; i++)
			this.buf[this.off++] = result[i];
		return;
	}

	this._checkAndResize(len - off);

	if (typeof (src.length) == "number" && src.slice) {
		for ( var i = off; i < off + len; i++)
			this.buf[this.off++] = src[i];

		return;
	}

	if (typeof (src.byteLength) == "number") {

		if (src.byteLength == 0)
			return;
		
		if (src.byteOffset > 0)
			off += src.byteOffset;

		var buf = (src.buffer ? src.buffer : src);
		var view = new Uint8Array(buf, off, len);
		this.buf.set(view, this.off);
		this.off += len;
		return;
	}

	throw "Cannot serialise: " + typeof (src);
};

Trap.ByteArrayOutputStream.prototype.toString = function() {
	var str = "";
	for ( var i = 0; i < this.buf.length; i++)
		str += this.buf[i];
	return String.utf8Decode(str);
};

Trap.ByteArrayOutputStream.prototype.toArray = function() {
	return new Uint8Array(this.buf.buffer.slice(0, this.off));
};

Trap.ByteArrayOutputStream.prototype.clear = function() {
	this.buf = new Uint8Array(512);
	this.off = 0;
	this.size = 512;
};

Trap.ByteStringOutputStream = function() {
	this.buf = "";
};

// Append the contents of the write operation in compact mode.
// Assume the input is byte-equivalent
Trap.ByteStringOutputStream.prototype.write = function(str, off, len) {

	if (typeof (str) == "number") {
		this.buf += String.fromCharCode(str);
		return;
	}

	if (typeof (off) != "number")
		off = 0;

	if (typeof (len) != "number")
		len = str.length;

	if (typeof (str) == "string")
		this.buf += str.substr(0, len);
	else if (typeof (str.length) == "number" && str.slice)
		for ( var i = off; i < off + len; i++)
			this.buf += String.fromCharCode(str[i]);
	else
		throw "Cannot serialise: " + typeof (str);
};

Trap.ByteStringOutputStream.prototype.toString = function() {
	return this.buf;
};

Trap.ByteStringOutputStream.prototype.toArray = function() {
	var arr = [];
	for ( var i = 0; i < this.buf.length; i++)
		arr[i] = this.buf[i].charCodeAt(0);
	return arr;
};

Trap.ByteStringOutputStream.prototype.clear = function() {
	this.buf = "";
};

