/**
 * Interfaces for extending Trap with new transport, keepalive methods, protocols or additional methods. These APIs
 * provide low-level access into the Trap implementation and are for advanced cases only.
 * <p>
 * The main usage of this package is to add new transports to Trap. The TrapTransport interface provides all the
 * requirements needed to implement a new transport (although it is recommended that if trap-core is used in any event,
 * AbstractTransport in trap-core is extended).
 * <p>
 * Additional extensions possible from this package are:
 * <ul>
 * <li>Implementing a new Message Queue or Message Buffer type. Message Queues heavily affect the performance of Trap,
 * and it may be beneficial to use a different kind of queue in some circumstances.
 * <li>Implementing a custom keepalive logic. Trap has a serviceable built-in keepalive, but it is difficult to make one
 * keepalive algorithm to rule them all. Instead, implementations can add new ones.
 * </ul>
 * 
 * @author Vladimir Katardjiev
 */
package com.ericsson.research.trap.spi;

/*
 * #%L
 * TrAP API
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 Ericsson AB
 * %%
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
 * #L%
 */
