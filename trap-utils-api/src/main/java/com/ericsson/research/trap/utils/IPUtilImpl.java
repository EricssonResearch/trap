package com.ericsson.research.trap.utils;

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

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * @hide
 * @author Vladimir Katardjiev
 * @since 1.0
 */
class IPUtilImpl extends IPUtil
{

	int score(String ifaceName)
	{
		if (ifaceName.startsWith("en"))
			return 1;
		if (ifaceName.startsWith("eth"))
			return 2;
		if (ifaceName.startsWith("ethernet"))
			return 3;
		if (ifaceName.startsWith("wireless"))
			return 4;
		if (ifaceName.startsWith("wi"))
			return 5;
		return 6;
	}

	InetAddress[] performGetPublicAddresses()
	{
		try
		{

			// Priority order:
			// en, eth, ethernet, wireless

			Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
			HashMap<String, HashSet<InetAddress>> ifaceAddresses = new HashMap<String, HashSet<InetAddress>>();
			LinkedList<String> ifaceNames = new LinkedList<String>();

			while (networkInterfaces.hasMoreElements())
			{
				NetworkInterface iface = networkInterfaces.nextElement();
				HashSet<InetAddress> foundAddresses = new HashSet<InetAddress>();
				Enumeration<InetAddress> inetAddresses = iface.getInetAddresses();

				while (inetAddresses.hasMoreElements())
				{
					InetAddress address = inetAddresses.nextElement();

					if (!IPUtil.isLocal(address))
						foundAddresses.add(address);
				}

				ifaceAddresses.put(iface.getName(), foundAddresses);
				ifaceNames.add(iface.getName());
			}

			Collections.sort(ifaceNames, new Comparator<String>() {

				public int compare(String o1, String o2)
				{
					return IPUtilImpl.this.score(o1) - IPUtilImpl.this.score(o2);
				}
			});

			LinkedList<InetAddress> foundAddresses = new LinkedList<InetAddress>();

			for (String name : ifaceNames)
			{
				foundAddresses.addAll(ifaceAddresses.get(name));
			}

			return foundAddresses.toArray(new InetAddress[] {});
		}
		catch (SocketException e)
		{
			return new InetAddress[] {};
		}
	}

	String performGetAddressForURI(InetAddress address)
	{

		String host = address.getHostAddress();

		byte[] bs = address.getAddress();
		boolean isv6 = bs.length > 4;

		if (isv6)
			host = "[" + host + "]";

		return host;
	}

	boolean performIsLocal(InetAddress address)
	{

		boolean disqualified = false;
		// IPv6 tests
		if (address instanceof Inet6Address)
		{
			byte[] addr = address.getAddress();

			// fe80::/10 — Addresses in the link-local prefix are only valid and unique on a single link
			if ((addr[0] == (byte) 0xfe) && ((addr[1] & 0xc) != 0))
			{
				disqualified = true;
			}

			// fc00::/7 — Unique local addresses (ULAs) are intended for local communication.
			if ((addr[0] == (byte) 0xfc) || (addr[0] == (byte) 0xfd))
			{
				disqualified = true;
			}
		}

		return disqualified || address.isMulticastAddress() || address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isSiteLocalAddress();
	} 

}
