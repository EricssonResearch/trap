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
var fs = require('fs');
fs.getSrc = function(src)
{
	return fs.readFileSync('../../main/javascript/' + src) + '';
};

// file is included here:

self = this;
navigator = null;
self.console = console;

eval(fs.getSrc('trap.js'));

Trap.useBinary = true;
eval(fs.getSrc('util/bytearrayoutputstream.js'));
eval(fs.getSrc('util/compat.js'));
eval(fs.getSrc('util/eventobject.js'));
eval(fs.getSrc('util/list.js'));
eval(fs.getSrc('util/log.js'));
eval(fs.getSrc('util/map.js'));
eval(fs.getSrc('util/md5.js'));
eval(fs.getSrc('util/set.js'));
eval(fs.getSrc('util/string.js'));
eval(fs.getSrc('util/stringbuffer.js'));
eval(fs.getSrc('util/zlib.js'));
eval(fs.getSrc('impl/auth.js'));
eval(fs.getSrc('impl/channel.js'));
eval(fs.getSrc('impl/channelmessagequeue.js'));
eval(fs.getSrc('impl/configuration.js'));
eval(fs.getSrc('impl/customconfig.js'));
eval(fs.getSrc('impl/keepalive.js'));
eval(fs.getSrc('impl/messagebuffer.js'));
eval(fs.getSrc('impl/endpoint.js'));
eval(fs.getSrc('impl/message.js'));
eval(fs.getSrc('impl/client-endpoint.js'));
eval(fs.getSrc('impl/listener-endpoint.js'));
eval(fs.getSrc('impl/server-endpoint.js'));
eval(fs.getSrc('spi/transport.js'));
eval(fs.getSrc('transports/abstract-transport.js'));
eval(fs.getSrc('transports/websocket.js'));
eval(fs.getSrc('transports/http.js'));

eval(fs.getSrc('node/abstract-listener.js'));
eval(fs.getSrc('node/websocket-listener.js'));
eval(fs.getSrc('node/http-listener.js'));
eval(fs.getSrc('node/http-server.js'));

Trap.Transports.WebSocket.prototype.supportsBinary = true;
Trap.useBinary = true;

lte = new Trap.ListenerEndpoint();
lte.listen({
	incomingTrapConnection: function(endpoint)
	{
		console.log("New connection");
		endpoint.onmessage = function(m)
		{
			endpoint.send(m.data);
		};
	}
});

console.log(lte.getClientConfiguration());