Trap._compat = {};

Trap._compat.capitalise = function(str)
{
	return str.substr(0,1).toUpperCase() + str.substr(1);
};

Trap._compat.__defineSetter = function(object, setterName, cb)
{
	
	var newName = "set" + Trap._compat.capitalise(setterName);
	
	if (!cb)
	{
		var privateName = "_" + setterName;
		cb = function(val) {
			this[privateName] = val;
			return this;
		};
	}
	
	if (object.__defineSetter__)
	{
		try
		{
			object.__defineSetter__(setterName, cb);
		} catch(e){}
	}

	// Also create the getter function as a property of the object
	object[newName] = cb;
};

Trap._compat.__defineGetter = function(object, getterName, cb)
{
	
	var newName = "get" + Trap._compat.capitalise(getterName);
	
	if (!cb)
	{
		var privateName = "_" + getterName;
		
		cb = function() {
			return this[privateName];
		};
	}
	
	if (object.__defineGetter__)
	{
		try
		{
			object.__defineGetter__(getterName, cb);
		} catch(e){}
	}
	
	// Also create the getter function as a property of the object
	object[newName] = cb;
};

Trap._compat.__defineGetterSetter = function(object, publicName, privateName, getter, setter)
{
	if (!privateName)
		privateName = "_" + publicName;
	
	if (!getter)
	{
		getter = function() {
			return this[privateName];
		};
	}
	
	if (!setter)
	{
		setter = function(val) {
			this[privateName] = val;
			return this;
		};
	}

	Trap._compat.__defineSetter(object, publicName, setter);
	Trap._compat.__defineGetter(object, publicName, getter);
};

Trap._compat.__addEventListener = function(object, event, listener)
{
	
	function ie() { object.attachEvent("on"+event, listener); }
	
	if (object.addEventListener)
		try
		{
			object.addEventListener(event, listener, false);
		} catch(e) { ie(); } // Yes, Internet Explorer supports AddEventListener... YET STILL THROWS. What's the logic? Really?
	else if (object.attachEvent)
		ie();
	else
		throw "Could not add listener for " + event + " to object " + object;
};

Trap._uuidCounter = 0;
Trap._uuid = function()
{
	return Math.random().toString(16).substring(2) + (Trap._uuidCounter++).toString(16);
};

// Choosing not to define a common function, in case someone wants to feature detect object type
Trap.subarray = function(src, start, end)
{
	if (src.subarray)
		return src.subarray(start,end);
	else
		return src.slice(start,end);
};

// Some ArrayBuffers don't have slice
if (typeof(ArrayBuffer) != "undefined" && typeof(ArrayBuffer.prototype.slice) == "undefined") {
	ArrayBuffer.prototype.slice = function(begin,end) {
		if (typeof(begin) == "undefined") return new ArrayBuffer(0);
		if (begin < 0) begin = this.byteLength + begin;
		if (begin < 0) begin = 0;
		if (begin > this.byteLength-1) begin = this.byteLength-1;
		if (typeof(end) != "undefined") {
			if (end < 0) end = this.byteLength + end;
			if (end < 0) end = 0;
			if (end > this.byteLength) end = this.byteLength;
		} else {
			end = this.byteLength;
		}
		if (end-begin <= 0) return new ArrayBuffer(0);
		var src = new Uint8Array(this,begin,end-begin);
		var dst = new Uint8Array(end-begin);
		dst.set(src);
		return dst.buffer;		
	};
}

// Flag detects if the browser supports getters (optimises access)
Trap._useGetters = false;
try { eval('var f = {get test() { return true; }}; Trap._useGetters = f.test;'); } catch(e){}

Trap.ByteConverter = {};

Trap.ByteConverter.toBigEndian = function(i, arr, j)
{
	
	if (!arr)
		arr = [];

	if (!j)
		j = 0;
	
	arr[j + 0] = (i >> 24);
	arr[j + 1] = ((i >> 16) & 0xFF);
	arr[j + 2] = ((i >> 8) & 0xFF);
	arr[j + 3] = ((i >> 0) & 0xFF);
	return arr;
};

Trap.ByteConverter.fromBigEndian = function(arr, offset)
{
	var rv = 0;

	rv |= (arr[offset + 0] & 0xFF) << 24;
	rv |= ((arr[offset + 1] & 0xFF) << 16);
	rv |= ((arr[offset + 2] & 0xFF) << 8);
	rv |= ((arr[offset + 3] & 0xFF) << 0);

	return rv;
};

/**
 * Converts an integer to a 7-bit representation of an integer by only
 * taking the 28 lowest-order values.
 *
 * @param src
 * @return
 */
Trap.ByteConverter.toBigEndian7 = function(src)
{

	var rv = [];

	rv[0] = this.getBits(src, 4, 11);
	rv[1] = this.getBits(src, 11, 18);
	rv[2] = this.getBits(src, 18, 25);
	rv[3] = this.getBits(src, 25, 32);

	return rv;
};

Trap.ByteConverter.fromBigEndian7 = function(arr, offset)
{
	
	if (!offset)
		offset = 0;
	
	var rv = 0;

	rv |= (arr[offset + 0] & 0xFF) << 21;
	rv |= ((arr[offset + 1] & 0xFF) << 14);
	rv |= ((arr[offset + 2] & 0xFF) << 7);
	rv |= ((arr[offset + 3] & 0xFF) << 0);

	if (this.getBits(rv, 4, 5) == 1)
		rv |= 0xF0000000;

	return rv;
};

/**
 * Fetch a number of bits from an integer.
 *
 * @param src
 *            The source to take from
 * @param startBit
 *            The first bit, with starting index 0
 * @param endBit
 *            The end index. This index will NOT be included in the return
 *            value.
 * @return The endBit-startBit number of bits from index startBit will be in
 *         the lowest order bits of the returned value.
 */
Trap.ByteConverter.getBits = function(src, startBit, endBit)
{
	var mask = (Math.pow(2, endBit - startBit) - 1);
	mask = mask << (32 - endBit);
	var rv = (src & mask) >> (32 - endBit);
	return rv;
};