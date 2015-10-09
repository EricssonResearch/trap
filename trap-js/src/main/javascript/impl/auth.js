Trap.Authentication = function()
{
	
	/**
	 * Fetches a collection of keys (strings) that this TrapAuthentication
	 * instance wants from the TrapTransport. The TrapAuthentication instance
	 * must not change this collection after this call, and must not assume the
	 * transport will call this function more times than one. It can assume the
	 * transport calls this function at least once.
	 * <p>
	 * As an argument, the transport supplies the context keys available from
	 * this transport. The returned collection may not contain a key that does
	 * not exist in the <i>availableKeys</i> collection. If the
	 * TrapAuthentication's implementation requires a context value whose key
	 * not exist in <i>availableKeys</i>, it may generate a value, as long as it
	 * does not significantly compromise the integrity of the authentication.
	 * <p>
	 * If there is not enough context information for this TrapAuthentication
	 * instance to successfully work, it may throw a TrapException.
	 * 
	 * @param availableKeys
	 *            A collection containing all keys that the TrapTransport can
	 *            fill in with meaningful values.
	 * @throws TrapException
	 *             If there is not enough context information for this
	 *             TrapAuthentication instance to successfully work
	 * @return A collection of keys that the TrapAuthentication instance wants
	 *         the TrapTransport to provide on every call.
	 */
	this.getContextKeys = function(availableKeys) { return []; };
	
	/**
	 * Verifies the authentication of a message. Checks the authentication
	 * header against Trapauthentication's internal state, and checks if it is
	 * correct. Additional data is provided by the transport in the form of
	 * other message headers (if any), as well as the message body (if
	 * available) and, finally, the additional context keys requested.
	 * 
	 * @param authenticationString
	 *            The authentication string provided by the other side. This
	 *            does not include beginning or trailing whitespaces, newlines,
	 *            etc, and does not include an eventual header name this was
	 *            sent in, nor the authentication type (e.g. DIGEST). If the
	 *            authentication string was transferred as part of a message
	 *            header, that header may be present in the <i>headers</I> map
	 *            if and only if it is called exactly "Authorization".
	 * @param headers
	 *            A map (String, String) of eventual other message headers. May
	 *            contain any number of headers (including zero). May not be
	 *            null. May not be modified by verifyAuthentication.
	 * @param body
	 *            A message body, if present. May be null. May not be modified
	 *            by verifyAuthentication.
	 * @param context
	 *            A non-null map of the context values requested by this
	 *            TrapAuthentication in {@link #getContextKeys(Collection)}.
	 *            Every key that was returned by getContextKeys MUST be filled
	 *            in.
	 * @return <i>true</i> if the authentication string is correct, <i>false</i>
	 *         otherwise (incorrect, could not be verified, etc).
	 */
	this.verifyAuthentication = function(authenticationString, headers, body, context) {return true;};
	
	/**
	 * Creates an authentication challenge.
	 * 
	 * @param context
	 *            A map of key/value pairs deduced from the transport and
	 *            environment
	 * @return A finished authentication challenge, to be inserted into the
	 *         message to the remote end.
	 */
	this.createAuthenticationChallenge = function(context) { return "";};
	
	/**
	 * Creates an authentication string to answer an authentication challenge,
	 * or sign a message. The TrapTransport provides the challenge
	 * authentication header of the last message(if any). If there is no
	 * authentication header, the TrapAuthentication instance should attempt to
	 * generate an authentication string from the current context and state. If
	 * that fails, it may throw a TrapException.
	 * <p>
	 * The call additionally includes the message header(s) and body (if any) to
	 * be signed in the authentication header, as well as the TrapTransport's
	 * context, as requested by this TrapAuthentication instance.
	 * 
	 * @param challengeString
	 *            A challenge string received by the TrapTransport, or null if
	 *            there was no new challenge.
	 * @param headers
	 *            Eventual message headers
	 * @param body
	 *            Eventual body
	 * @param context
	 *            A non-null map of the context values requested by this
	 *            TrapAuthentication in {@link #getContextKeys(Collection)}.
	 *            Every key that was returned by getContextKeys MUST be filled
	 *            in.
	 * @return An authentication response corresponding to the challenge, to be
	 *         inserted into a message with no further modifications
	 */
	this.createAuthenticationResponse = function(challengeString, headers, body, context) {return "";};
};