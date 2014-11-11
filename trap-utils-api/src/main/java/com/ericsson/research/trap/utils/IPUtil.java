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

import java.net.InetAddress;

/**
 * A collection of checks for IP addresses. These utilities provide convenience
 * methods not covered by the Java API.
 *
 * @author vladi
 */
public abstract class IPUtil
{

	static IPUtil	instance;

	static
	{
		try
		{
			Class<?> c = Class.forName(IPUtil.class.getName() + "Impl");
			instance = (IPUtil) c.newInstance();
		}
		catch (Throwable t)
		{
			System.err.println("Could not initialise IPUtil Impl");
		}
	}

	/**
	 * Gets an array of the publicly accesisble IP addresses of the JVM.
	 *
	 * @return an array of the publicly accesisble IP addresses of the JVM.
	 */
	public static InetAddress[] getPublicAddresses()
	{
		return instance.performGetPublicAddresses();
	}

	abstract InetAddress[] performGetPublicAddresses();

	/**
	 * Gets a URI-safe address of an InetAddress. For example, returns
	 * <code>127.0.0.1</code> for localhost in IPv4, and <code>[::1]</code> for
	 * localhost in IPv6.
	 *
	 * @param address
	 *            The address to convert
	 * @return A string representation of the address that is safe to insert
	 *         into a URI.
	 */
	public static String getAddressForURI(InetAddress address)
	{
		return instance.performGetAddressForURI(address);
	}

	abstract String performGetAddressForURI(InetAddress address);

	/**
	 * Checks whether an InetAddress is a local (=virtual address space). For
	 * example, 127.0.0.1 returns true, while 8.8.8.8 returns false.
	 *
	 * @param address
	 *            The address to verify.
	 * @return <i>true</i> if the address is defined in an address space that is
	 *         routable from the Internet, <i>false</i> otherwise.
	 */
	public static boolean isLocal(InetAddress address)
	{
		return instance.performIsLocal(address);
	}

	abstract boolean performIsLocal(InetAddress address);

	IPUtil()
	{
	}
}
