/**
 * Provides interfaces that allow Trap to give feedback to the application when events occur. Implement these interfaces
 * on a delegate in order to receive calls from a Trap Endpoint the delegate is supplied to.
 * <p>
 * Implementing any single delegate method is optional. Any endpoint can have multiple delegates; only one delegate
 * method will be called (that of the most recently added delegate), but if a delegate does not implement a certain
 * interface it will not conflict. See {@link com.ericsson.research.trap.examples.DelegateEchoClient} for an example.
 * <p>
 * Additionally, Trap will emit warnings when critical events are missed. The following events are considered critical:
 * <ul>
 * <li><b>OnAccept</b> for a listener.
 * <li><b>OnData</b> for any endpoint.
 * <li><b>OnFailedSending</b> for any endpoint.
 * </ul>
 * 
 * @author Vladimir Katardjiev
 * @since 1.1
 */
package com.ericsson.research.trap.delegates;