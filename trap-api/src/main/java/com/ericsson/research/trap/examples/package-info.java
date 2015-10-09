/**
 * This package contains a set of examples on how to use Trap. These examples are fully executable, and include main()
 * methods, but require a Trap implementation to run. If using Maven, ensure that the appropriate transports are added
 * to your project's dependencies.
 * <p>
 * <b>Note that these examples do not follow proper data isolation practices!</b> Most/all fields are marked public in
 * order to allow easy integration testing. This ensures the examples all run, but does not make proper coding practice.
 * When using these samples in your own code, please ensure that the fields are properly marked as private, protected or
 * default scope.
 * <p>
 * The examples are client/server pairs based on a certain topics. The topics covered are:
 * <ul>
 * <li><b>Echo</b>: A simple Hello World example with a server that echoes back any data it receives, and a client that
 * continuously sends messages.
 * <li><b>Configured</b>: Comprehensive list of the configuration options that may be applied to clients and servers.
 * <li><b>Authenticated</b>: Shows how to attach (mutual) authentication to identify a Trap session.
 * <li><b>Multiplexed</b>: Leveraging Trap channels to allow multiple simultaneous flows.
 * <li><b>Delegate</b>: Highlights how we can attach multiple delegates to handle different tasks.
 * </ul>
 * 
 * @author Vladimir Katardjiev
 * @since 1.1
 */
package com.ericsson.research.trap.examples;