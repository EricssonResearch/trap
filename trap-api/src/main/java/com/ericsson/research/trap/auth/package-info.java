/**
 * Contains interfaces used for authenticating Trap sessions. By default, Trap sessions are unauthenticated, and only
 * protected by a unique session ID token. If a remote attacker correctly guesses the session, they can attach to an
 * existing Trap session and add more transports.
 * <p>
 * That is where authentication comes in. The application using Trap can use the interfaces in this package to
 * authenticate Trap sessions. Authenticators will be invoked whenever Trap has a potentially untrusted message
 * incoming. This varies on a per-transport basis; it can be as seldom as once on setup (for TCP/SSL) to once every
 * message (HTTP/port80)
 * 
 * @author Vladimir Katardjiev
 * @since 1.0
 */
package com.ericsson.research.trap.auth;