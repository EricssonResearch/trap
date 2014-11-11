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
var cte;

Trap.useBinary = false;

var httpTransport = "http://localhost:8081";
var wsTransport = "ws://localhost:41234";

var setupFuns = [
                 function()
                 {
                	var cte = new Trap.ClientEndpoint(httpTransport);
                	return cte;
                 },
                 function()
                 {
                	var cte = new Trap.ClientEndpoint(wsTransport);
                	return cte;
                 },
                 function()
                 {
                	 console.log("New TrapEndpoint: HTTP Only");
                	var cte = new Trap.ClientEndpoint(httpTransport);
                	cte.disableTransport("websocket");
                	return cte;
                 },
                 function()
                 {
                	 console.log("New TrapEndpoint: WS Only");
                	var cte = new Trap.ClientEndpoint(wsTransport);
                	cte.disableTransport("http");
                	return cte;
                 }
                 ];

function asyncDone()
{
	if(cte)
		cte.close();

	setTimeout( start, 1000);
}

asyncTest("Wait a bit", function() {
	expect(0);
	setTimeout(start, 1000);
});

for (var i=0; i<setupFuns.length; i++)
{
	
	// Got to wrap this into an anonymous function to properly create
	// enclosing scopes.
	(function(){
		
		var setupFun = setupFuns[i];
		
		asyncTest("Trap Connect (" + i + ")", function() {
			console.log("Testing basic connect");
			expect(1);
			cte = setupFun();
			cte.onopen = function() {
				ok(true, "Connected to Trap!");
				asyncDone();
			};
		
		});
		
		asyncTest("Trap Message Echo (" + i + ")", function() {
			console.log("Testing basic echo");
			expect(1);
			cte = setupFun();
			cte.onopen = function() {
				cte.send("Hello");
			};
		
			cte.onmessage = function(m)
			{
				console.error("### DATA: " + m.string);
				if (m.string == "Welcome")
					this.welcomed = true;
				else if (!this.welcomed)
				{
					ok(false, "First message wasn't welcome...");
					asyncDone();
				}
				else if (m.string == "Hello")
				{
					ok(true, "Test Complete!");
					asyncDone();
				}
				else
				{
					ok(false);
					asyncDone();
				}
			}
		
		});
		
		for (var k=0; k>10; k++)
			asyncTest("Trap 10 messages " + k, function() {
				console.log("Sending message " + k);
				expect(1);
				cte = setupFun();
				cte.onopen = function() {
					for (var i=0; i<10; i++)
						cte.send("Hello");
				};
		
				var i=0;
		
				cte.onmessage = function(m)
				{
					if (m.string == "Welcome")
						this.welcomed = true;
					else if (m.string == "Hello")
					{
						if (++i == 10)
						{
							ok(true, "Test Complete!");
							asyncDone();
						}
					}
				}
		
			});
		
		asyncTest("isAlive check (" + i + ")", function() {
			console.log("Testing isAlive");
			expect(1);
			cte = setupFun();
			cte.onopen = function() {
				cte.isAlive(1000, true, true, 1000, function(val) {
					ok(val, "isAlive check result: " + val);
					asyncDone();
				})
			};
		
		});
		
		/*asyncTest("Sending JSON " + i, function() {
			console.log("Sending JSON test");
			expect(1);
			cte = setupFun();
			cte.onopen = function() {
				
			};
			
			cte.onmessage = function(m) {
				
			};
			
			
		});*/
	
	
	})();

}