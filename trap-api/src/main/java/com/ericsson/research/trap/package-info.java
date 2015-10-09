/**
 * <p>
 * The main API package for Trap. This is the primary entry point for writing applications that use Trap. Most of the
 * entries herein are interfaces to objects that should be instantiated using the factory.
 * <p>
 * {@link com.ericsson.research.trap.TrapFactory} is the main factory class and is used to create new instances. To
 * create a client, use {@link com.ericsson.research.trap.TrapFactory#createClient(String, boolean)}. To create a server
 * (=listener), use {@link com.ericsson.research.trap.TrapFactory#createListener(String)}.
 * <p>
 * In some containers, automatic transport detection will not work. For them, before instantiating a server or client,
 * the available transports should be added using
 * {@link com.ericsson.research.trap.TrapTransports#addTransportClass(Class)}. This limitation is present everywhere
 * that packages cannot be scanned by reflection.
 * <h2>Version History</h2>
 * For version history and API changes, please reference <a href="https://github.com/EricssonResearch/trap/wiki/Version-History">the Trap Wiki at Github</a>.
 * 
 * @author Vladimir Katardjiev
 * @since 1.0
 */
package com.ericsson.research.trap;