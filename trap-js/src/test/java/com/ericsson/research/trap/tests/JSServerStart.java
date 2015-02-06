package com.ericsson.research.trap.tests;

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

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Level;

import com.ericsson.research.transport.ws.WSAcceptListener;
import com.ericsson.research.transport.ws.WSFactory;
import com.ericsson.research.transport.ws.WSInterface;
import com.ericsson.research.transport.ws.WSListener;
import com.ericsson.research.transport.ws.WSServer;
import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.TrapFactory;
import com.ericsson.research.trap.TrapListener;
import com.ericsson.research.trap.TrapState;
import com.ericsson.research.trap.delegates.OnAccept;
import com.ericsson.research.trap.delegates.TrapEndpointDelegate;
import com.ericsson.research.trap.utils.JDKLoggerConfig;
import com.ericsson.research.trap.utils.ThreadPool;

@SuppressWarnings({"unused", "rawtypes"})
public class JSServerStart implements OnAccept
{

	public static TrapListener	listener;
	public static ConcurrentSkipListSet<TrapEndpoint>	endpoints	= new ConcurrentSkipListSet<TrapEndpoint>();

	static boolean										started		= false;

	public synchronized static void start() throws Exception
	{
		if (started)
			return;

		started = true;

		main(new String[] {});
	}

	private static HashSet<WSInterface>	sockets;
	private static WSServer	server;

	public static void main(String[] args) throws Exception
	{

		/*ConsoleHandler h = new ConsoleHandler();
		h.setLevel(Level.FINEST);
		Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME).setLevel(Level.FINEST);
		Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME).addHandler(h);
		Logger.getLogger("com").setLevel(Level.FINEST);
		Logger.getLogger("com").addHandler(h);
		*/
		JDKLoggerConfig.initForPrefixes(Level.INFO);
		Thread thread = new Thread(new Runnable() {

			@Override
			public void run()
			{

				try
				{
					Thread.sleep(250);
					listener = TrapFactory.createListener(null);
					//listener.setOption("host", "192.168.1.121");
					listener.getTransport("socket").configure("port", "41235");
					//					listener.getTransport("websocket").configure("host", "155.53.234.51");
					listener.getTransport("websocket").configure("port", "41234");
					listener.getTransport("http").configure("port", "8081");
					//listener.getTransport("http").configure("host", "155.53.234.118");
					listener.disableAllTransports();
					listener.enableTransport("http");
					listener.enableTransport("websocket");
					listener.listen(new JSServerStart());
					System.out.println("[");
					System.out.println(listener.getClientConfiguration());
					System.out.println("]");
					
					sockets = new HashSet<WSInterface>();
					
					server = WSFactory.createWebSocketServer("localhost", 41221, new WSAcceptListener() {
						
						@Override
						public void notifyReady(WSServer server)
						{
							
						}
						
						@Override
						public void notifyError(Throwable t)
						{
							
						}
						
						@Override
						public void notifyAccept(final WSInterface socket)
						{
							sockets.add(socket);
							socket.setReadListener(new WSListener() {
								
								@Override
								public void notifyPong(byte[] payload)
								{
									
								}
								
								@Override
								public void notifyOpen(WSInterface socket)
								{
									
								}
								
								@Override
								public void notifyMessage(byte[] data)
								{
									try
									{
										socket.send(data);
									}
									catch (IOException e)
									{
										e.printStackTrace();
									}
								}
								
								@Override
								public void notifyMessage(String utf8String)
								{
									try
									{
										socket.send(utf8String);
									}
									catch (IOException e)
									{
										e.printStackTrace();
									}
								}
								
								@Override
								public void notifyError(Throwable t)
								{
									
								}
								
								@Override
								public void notifyClose()
								{
									
								}
							});
						}
					}, null);
				}
				catch (TrapException e)
				{
					e.printStackTrace();
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}

			}
		});
		
		thread.setDaemon(false);
		thread.start();
		return;

	}

	int	i	= 0;

	@Override
	public void incomingTrapConnection(final TrapEndpoint endpoint, TrapListener listener, Object context)
	{

		System.out.println("Welcoming " + this.i++);
		try
		{
			endpoint.send("Welcome".getBytes());
		}
		catch (TrapException e1)
		{
			e1.printStackTrace();
		}
		//endpoint.setKeepaliveInterval(10);
		endpoints.add(endpoint);
		endpoint.setDelegate(new TrapEndpointDelegate() {

			@Override
			public void trapStateChange(TrapState newState, TrapState oldState, TrapEndpoint endpoint, Object context)
			{
				System.out.println("State change: " + newState);

				if ((newState == TrapState.CLOSED) || (newState == TrapState.ERROR))
					endpoints.remove(endpoint);
			}

			@Override
			public void trapFailedSending(Collection datas, TrapEndpoint endpoint, Object context)
			{
				System.out.println("Failed sending");
			}

			@Override
			public void trapData(byte[] data, int channel, TrapEndpoint endpoint, Object context)
			{
				try
				{
					//System.out.println("Received data: " + new String(data));
						endpoint.send(data, channel, true);
					//endpoint.send("7b2274797065223a224554696d654d657373616765222c2276657273696f6e223a22302e35222c22616374696f6e223a2243414c4c222c2273656e646572223a7b22757365726964223a223234222c22617661746172496d61676555524c223a2275736572732f32342f6176617461722e6a7067222c226e616d65223a2250657242526f77736572222c226465706172746d656e74223a22546865204465706172746d656e74206f66205265616c6c7920436c65766572204675636b757073222c22656d61696c223a2270657262726f77736572406572696373736f6e2e636f6d222c226973416374697665223a2274727565222c22696365557365726e616d65223a2274657374222c2269636550617373776f7264223a2231323334222c2269636553657276657241646472657373223a225455524e203139322e33362e3135372e36303a33343738227d2c227265636569766572223a7b22757365726964223a223233222c22617661746172496d61676555524c223a2275736572732f32332f6176617461722e6a7067222c226e616d65223a225065724269506164222c226465706172746d656e74223a22546865204465706172746d656e74206f662053706563746163756c6172204675636b757073222c22656d61696c223a227065726269706164406572696373736f6e2e636f6d222c226973416374697665223a2274727565222c22696365557365726e616d65223a2274657374222c2269636550617373776f7264223a2231323334222c2269636553657276657241646472657373223a225455524e203139322e33362e3135372e36303a33343738227d2c226368726f6d655369676e616c4d657373616765223a7b226d65737361676554797065223a224f46464552222c226f66666572657253657373696f6e4964223a227255506f74677a6f727675414b663168484b4f4836323153634d2f53494d3967222c22736470223a22763d305c725c6e6f3d2d2030203020494e20495034203132372e302e302e315c725c6e733d5c725c6e743d3020305c725c6e6d3d617564696f203439373239205254502f415650462031303320313034203020382031303620313035203133203132365c725c6e633d494e20495034203231332e3135392e3138352e35335c725c6e613d727463703a353131383020494e20495034203231332e3135392e3138352e35335c725c6e613d63616e6469646174653a312032207564702031203231332e3135392e3138352e35332036323931382074797020686f7374206e616d652072746370206e6574776f726b5f6e616d6520656e3120757365726e616d652053755473353431386977796248366f352070617373776f7264206f6d3644516974305a75537579516d4a6749327244472067656e65726174696f6e20305c725c6e613d63616e6469646174653a312031207564702031203231332e3135392e3138352e35332036303835302074797020686f7374206e616d6520727470206e6574776f726b5f6e616d6520656e3120757365726e616d652047596262304d4b755a486377777045532070617373776f726420362b6f316862676862713737524345784237443467412067656e65726174696f6e20305c725c6e613d63616e6469646174653a3120322075647020302e39203231332e3135392e3138352e353320353131383020747970207372666c78206e616d652072746370206e6574776f726b5f6e616d6520656e3120757365726e616d6520714e2b4c3135544374794272627730542070617373776f7265427265616b6572223a333037313130353834357d7de47dfe0b0908c61ccae9a576b5b7adb6f3ed7ef6bf01a9de05666258a28c87c7cc80f538195391f28c6727000f5f22b54b394ba4134f77ab5a6fbbede9e67dae030c953824927d537bed6f4b68faf9f4470ba8ceb2b120b488ab921bf8b041380307e5247dee41cf4af3549d3828ca4e56bc9b7da4d35aeeadeba79a3eb30749c159a8c5c9adafa27a2bdf4d6daa5bafc785d46f772bcb212a63cefda0ec65f52319c9ddd7fbc78e4035cf3c42dfadfbdffcf5babdb4f4d34fa9c261d43962bed6daea9ed6df6d030000000000000000005086873f00000001205a9a0c00040000560000000000000062706c6973743030d101025f10245f6b434655524c50726f746f636f6c50726f7065727479466f72466f756e646174696f6e09080b32000000000000010100000000000000030000000000000000000000000000003301010101010101010101010101010101010101010101010101010101010101010101010101010101010101010101010101010101010101010101010101ffdb00430101010101010101010101010101010101010101010101010101010101010101010101010101010101010101010101010101010101010101010101010101010101ffc00011080100010003011100021101031101ffc4001f0000010501010101010100000000000000000102030405060708090a0bffc400b5100002010303020403050504040000017d01020300041105122131410613516107227114328191a1082342b1c11552d1f02433627282090a161718191a25262728292a3435363738393a434445464748494a535455565758595a636465666768696a737475767778797a838485868788898a92939495969798999aa2a3a4a5a6a7a8a9aab2b3b4b5b6b7b8b9bac2c3c4c5c6c7c8c9cad2d3d4d5d6d7d8d9dae1e2e3e4e5e6e7e8e9eaf1f2f3f4f5f6f7f8f9faffc4001f0100030101010101010101010000000000000102030405060700000000000000001100020102040403040705040400010277000102031104052131061241510761711322328108144291a1b1c109233352f0156272d10a162434e125f11718191a262728292a35363738393a434445464748494a535455565758595a636465666768696a737475767778797a82838485868788898a92939495969798999aa2a3a4a5a6a7a8a9aab2b3b4b5b6b7b8b9bac2c3c4c5c6c7c8c9cad2d3d4d5d6d7d8d9dae2e3e4e5e6e7e8e9eaf2f3f4f5f6f7f8f9faffda000c03010002110311003f00fe15c6490a31818e3f03e9d3b7b773d38f1ad14eea314fba5afdfb9f517693d74dede9f8f4f3f21cbb881b73ebcf5f4eff0053dfd4d2e48f6d7d5bf9daf62bec2bbdf5b7d97f2ea2658f19e318191c807927a7a7f5a14546ed5f5f376fbb6ebb7a132f7adab56ecffafc2d6157218e307273ff00eb18fa638e3a1ef8afebf515b5bde5ff00813ff87fc7ab1e4b67ef727af7e70300e41e3a63ff00ac4d1f7f5fc5dfe6ff00e18ae669deeeebfaf9ff00c3790b938e4707ae3233c77f7f72474f418a4a1052e6518a7b7c9dff00afc4ae64def6d3ab77dadb27b79ebeba80760719e3a1e0e7b81d739e075f5ee79a39576fbf5fcc5ed127b3fbe4ff0047dc95492792723a11938c6319ed8cf23e9cd4ca9d396ae3ef69addecba24ad6bf52f96eaf76afaadb46f5eabcf6fbc71c8e091c0193d093ebd3af1c72718e076a99c229594536fabbc9fa24db5ebd75b0a4acad7dfbddfe8c19db24038e00c639c73c671efedd7d7a428bb7c36776f4bafd6daf6b6bdae34ad1bf33d13d53ff002b2ff8767af7c0fd32db55f1fe9ab768b2c5636f777e91c88596496da061086e7b338903118ca8ee463e4f8d7153c16458a745b84eb54a542538fc5185470e66b5d2f769dba6cfbfe83e18e028e3f8ab06abc6338e16957c5284f58ba94a0d47993b689b6edbdd69b1f7725a4726772aa371950386c9c15ce785e4138c938c0e95f826914d3b5bdee8acaf7f86cbfe0b96fa9fd694afeedeea".getBytes());
				}
				catch (TrapException e)
				{
					e.printStackTrace();
				}
			}
		}, true);

		if (true)
			return;

		ThreadPool.executeAfter(new Runnable() {

			@Override
			public void run()
			{
				System.out.println("isAlive result: " + endpoint.isAlive(0, true, true, 10000));
			}

		}, 10000);

		if (true)
			return;

		new Thread(new Runnable() {

			@Override
			public void run()
			{
				try
				{
					for (;;)
					{
						Thread.sleep(3000);
						try
						{
							if ((endpoint.getState() == TrapState.CLOSED) || (endpoint.getState() == TrapState.ERROR))
								return;

							//System.out.println("Going to send a foo");
							endpoint.send("supercalifradgalisticexpialydocious".getBytes());

							break;
						}
						catch (TrapException e)
						{
							e.printStackTrace();
						}
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}).start();
	}
}
