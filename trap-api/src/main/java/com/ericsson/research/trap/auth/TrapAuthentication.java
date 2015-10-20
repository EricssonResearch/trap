package com.ericsson.research.trap.auth;

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



import java.util.Collection;
import java.util.Map;

import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.spi.TrapMessage;
import com.ericsson.research.trap.spi.TrapTransport;

/**
 * Provides an interface by which applications can authenticate Trap sessions and messages. Trap ships with no built-in
 * implementation of TrapAuthentication; it must be entirely user-supplied. Authentication can be set per-endpoint using
 * {@link TrapEndpoint#setAuthentication(TrapAuthentication)}, or per-transport using
 * {@link TrapTransport#setAuthentication(TrapAuthentication)}.
 * <p>
 * When set, the authentication instance will be called by the transport or endpoint whenever an unauthenticated
 * incoming message is received, and before any potentially unauthenticated message is sent. Essentially, if the remote
 * party can trust a message (e.g. because it's transmitted over a stateful TLS connection), this authentication will
 * not be invoked. On the other hand, if an unsecured transport is used (e.g. plain HTTP), authentication will be
 * invoked for every message. Most transports will lie somewhere in-between, most likely invoking authentication on
 * session start.
 * <p>
 * The basic steps when Trap receives a message is to do the following:
 * <ul>
 * <li>Parse the incoming message, separating auth payload from data payload. Verify the message type.
 * <li>If message, do
 * <ul>
 * <li>Ask the assigned TrapAuthentication instance to verify the authentication. If successful, return.
 * <li>If unsuccessful, ask the assigned TrapAuthentication instance to
 * {@link #createAuthenticationChallenge(TrapMessage, Map)}.
 * <li>Send the new challenge to the remote side..
 * </ul>
 * <li>If challenge, do
 * <ul>
 * <li>Ask TrapAuthentication to {@link #createAuthenticationResponse(TrapMessage, TrapMessage, Map, Map). 
 * <li>Send the challenge response back to the other side.
 * </ul>
 * </ul>
 * 
 * @author Vladimir Katardjiev
 */
public interface TrapAuthentication
{
    
    /**
     * Fetches a collection of keys (strings) that this TrapAuthentication instance wants from the TrapTransport. The
     * TrapAuthentication instance must not change this collection after this call, and must not assume the transport
     * will call this function more times than one. It can assume the transport calls this function at least once.
     * <p>
     * As an argument, the transport supplies the context keys available from this transport. The returned collection
     * may not contain a key that does not exist in the <i>availableKeys</i> collection. If the TrapAuthentication's
     * implementation requires a context value whose key not exist in <i>availableKeys</i>, it may generate a value, as
     * long as it does not significantly compromise the integrity of the authentication.
     * <p>
     * If there is not enough context information for this TrapAuthentication instance to successfully work, it may
     * throw a TrapException.
     * 
     * @param availableKeys
     *            A collection containing all keys that the TrapTransport can fill in with meaningful values.
     * @throws TrapException
     *             If there is not enough context information for this TrapAuthentication instance to successfully work
     * @return A collection of keys that the TrapAuthentication instance wants the TrapTransport to provide on every
     *         call.
     */
    public Collection<String> getContextKeys(Collection<String> availableKeys) throws TrapException;
    
    /**
     * Verifies the authentication of a message. Implementations should check that {@link TrapMessage#getAuthData()} is
     * a valid authentication string/data. Implementations <b>must</b> treat all arguments as read-only, and may not
     * assume any specific ordering on the received messages.
     * 
     * @param message
     *            The message for which to verify the authentication for. The contents of the message MUST NOT be
     *            modified.
     * @param headers
     *            A map (String, String) of eventual other transport headers. The presence and/or absence of these
     *            headers is dependant on the transport; for example, socket will have zero headers whereas HTTP may
     *            have plenty.. May contain any number of headers (including zero). May not be null. May not be modified
     *            by verifyAuthentication.
     * @param context
     *            A non-null map of the context values requested by this TrapAuthentication in
     *            {@link #getContextKeys(Collection)}. Every key that was returned by getContextKeys MUST be filled in.
     * @return <i>true</i> if the authentication string is correct, <i>false</i> otherwise (incorrect, could not be
     *         verified, etc).
     * @throws TrapAuthenticationException
     *             If the authentication failure is so severe that the transport should be disconnected. This exception
     *             can be thrown to signify that the transport is behaving improperly (potential DDoS/hack) and should
     *             be terminated.
     */
    public boolean verifyAuthentication(TrapMessage message, Map<String, String> headers, Map<String, Object> context) throws TrapAuthenticationException;
    
    /**
     * Creates an authentication challenge. The challenge may contain any UTF-16-representable characters. It is
     * otherwise entirely application-specified. Trap will not parse the challenge's contents.
     * 
     * @param src
     *            The message that arrived with invalid authentication and needs to be challenged.
     * @param context
     *            A map of key/value pairs deduced from the transport and environment
     * @return A finished authentication challenge, to be inserted into the message to the remote end.
     */
    public String createAuthenticationChallenge(TrapMessage src, Map<String, Object> context);
    
    /**
     * Creates an authentication signature for an outgoing message. If a challenge was received, it will be included.
     * 
     * @param challenge
     *            Any new challenge received; <i>null</i> if no challenge was received since the last call to this
     *            method.
     * @param outgoing
     *            The outgoing message (the one to be signed). All fields should be considered immutable.
     * @param headers
     *            Eventual message headers
     * @param context
     *            A non-null map of the context values requested by this TrapAuthentication in
     *            {@link #getContextKeys(Collection)}. Every key that was returned by getContextKeys MUST be filled in.
     * @return An authentication response corresponding to the challenge, to be inserted into a message with no further
     *         modifications. May be null/empty string.
     * @throws TrapException
     *             if no authentication response could be generated.
     */
    public String createAuthenticationResponse(TrapMessage challenge, TrapMessage outgoing, Map<String, String> headers, Map<String, Object> context);
}
