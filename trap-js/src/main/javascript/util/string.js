/*
 * Adds more string functions
 */

if (typeof String.prototype.startsWith != 'function') {
  String.prototype.startsWith = function (str){
    return this.indexOf(str) == 0;
  };
}

if (typeof String.prototype.trim != 'function')
{
	String.prototype.trim = function(str)
	{
		var	s = str.replace(/^\s\s*/, ''),
			ws = /\s/,
			i = s.length;
		while (ws.test(s.charAt(--i)));
		return s.slice(0, i + 1);
		
	};
}

if (typeof String.prototype.endsWith != 'function') {
	String.prototype.endsWith = function(suffix) {
	    return this.indexOf(suffix, this.length - suffix.length) !== -1;
	};
}

if (typeof String.prototype.contains != 'function')
{
	String.prototype.contains = function(target)
	{
		return this.indexOf(target) != -1;
	};
};

// Calculates the length of the string in utf-8
if (typeof String.prototype.utf8ByteLength != 'function')
{
	String.prototype.utf8ByteLength = function()
	{
		// Matches only the 10.. bytes that are non-initial characters in a multi-byte sequence.
		var m = encodeURIComponent(this).match(/%[89ABab]/g);
		return this.length + (m ? m.length : 0);
	};
};

if (typeof String.prototype.toUTF8ByteArray != 'function')
String.prototype.toUTF8ByteArray = function() {
	var bytes = [];

	var s = unescape(encodeURIComponent(this));

	for (var i = 0; i < s.length; i++) {
		var c = s.charCodeAt(i);
		bytes.push(c);
	}

	return bytes;
};

String.utf8Encode = function(src)
{
	return unescape( encodeURIComponent( src ) );
};

String.utf8Decode = function(src)
{
	return decodeURIComponent( escape( src ) );
};

if (typeof String.prototype.fromUTF8ByteArray != 'function')
String.fromUTF8ByteArray = function(arr, offset, length)
{
	var str = "";
	if (typeof(offset) == "undefined")
	{
		offset = 0; length = arr.length;
	}

	for (var i=offset; i<length+offset; i++)
		str += String.fromCharCode(arr[i]);
	
	return String.utf8Decode(str);
};