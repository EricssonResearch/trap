package com.ericsson.research.transport.ws.spi;

import java.io.UnsupportedEncodingException;

import junit.framework.TestCase;


import com.ericsson.research.transport.ws.WSException;
import com.ericsson.research.transport.ws.WSFactory;
import com.ericsson.research.transport.ws.spi.hybi10.WSHybi10ClientHandshake;

public class WebSocketVersionParserTest extends TestCase {

	public void testParseVersion_Hixie_75() throws UnsupportedEncodingException, WSException {
		String handshake = WSConstants.GET+" "+ "/resource" +" HTTP/1.1\r\nUpgrade: WebSocket\r\nConnection: Upgrade\r\nHost: "+ "2.2.2.2:80" + "\r\n";
		handshake += "Origin: " + "1.1.1.1" +"\r\n\r\n";
		
		assertEquals(WSFactory.VERSION_HIXIE_75, WebSocketVersionParser.parseVersion(handshake.getBytes("UTF-8")));
	}

	public void testParseVersion_Hixie_76() throws UnsupportedEncodingException, WSException {
		String handshake = WSConstants.GET+" "+ "/resource" +" HTTP/1.1\r\nUpgrade: WebSocket\r\nConnection: Upgrade\r\nHost: "+ "2.2.2.2:80" + "\r\n";
		handshake += "\r\nOrigin: "+ "1.1.1.1" +"\r\n";
		
		int spaces1 = (int) (Math.random()*12+1);
		int spaces2 = (int) (Math.random()*12+1);
		int max1 = Integer.MAX_VALUE / spaces1;
		int max2 = Integer.MAX_VALUE / spaces2;
		int num1 = (int) (Math.random() * max1 + 1);
		int num2 = (int) (Math.random() * max2 + 1);
		String spc1 = generateString(spaces1, 0x20, 0x20);
		String spc2 = generateString(spaces2, 0x20, 0x20);
		String gbg1 = generateString((int) (Math.random()*12+1), 0x3A, 0x7E);
		String gbg2 = generateString((int) (Math.random()*12+1), 0x3A, 0x7E);
		String key1 = splice(splice(String.valueOf(num1*spaces1), spc1), gbg1);
		String key2 = splice(splice(String.valueOf(num2*spaces2), spc2), gbg2);
		String key3 = generateString(8, 0x00, 0xFF);
		handshake += WSConstants.SEC_WEBSOCKET_KEY1_HEADER+": " + key1 + "\r\n";
		handshake += WSConstants.SEC_WEBSOCKET_KEY2_HEADER+": " + key2 + "\r\n";
		handshake += "\r\n";
		handshake += key3;
		
		assertEquals(WSFactory.VERSION_HIXIE_76, WebSocketVersionParser.parseVersion(handshake.getBytes("UTF-8")));
	}
	

	public void testParseVersion_HyBi_10() throws UnsupportedEncodingException, WSException {
		String handshake = new WSHybi10ClientHandshake(null, "2.2.2.2", 80, "/resource", "1.1.1.1", false).createHandshake();
		
		assertEquals(WSFactory.VERSION_HYBI_10, WebSocketVersionParser.parseVersion(handshake.getBytes("UTF-8")));
	}

	private String generateString(int length, int startChar, int endChar) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < length; i++) {
			int j = (int) (Math.floor(Math.random() * (endChar - startChar)) + startChar);
			sb.append((char)j);
		}
		return sb.toString();
	}
	
	private String splice(String src1, String src2) {
		StringBuffer sb = new StringBuffer(src1);
		for (int i = 0; i < src2.length(); i++) {
			int pos = (int) Math.round(Math.random() * sb.length());
			sb.insert(pos, src2.charAt(i));
		}
		return sb.toString();
	}


}
