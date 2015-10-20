package com.ericsson.research.trap.spi;

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



/**
 * Interface to be implemented by objects that want to be notified about the state of a keepalive predictor/validator.
 * 
 * @author Vladimir Kataradjiev
 */
public interface TrapKeepaliveDelegate
{
    /**
     * Called when a {@link TrapKeepalivePredictor} notices that a keepalive it expected to receive a keepaliveSuccess
     * call for did not occur within a certain number of milliseconds (configurable, default 5000).
     * <p>
     * This strongly implies the link is either dead or overloaded. The KeepalivePredictor will not, on its own, close
     * the connection. This method is called at most one time <i>for each call to
     * {@link TrapKeepalivePredictor#keepaliveReceived(boolean, char, int, byte[])} </i>. If keepaliveReceived() is
     * called after receiving a notification from this method, this method may be called again.
     * <p>
     * Additionally, this method may be called up to once without keepaliveSuccess being called, meaning no keepalive
     * was successful ever.
     * <p>
     * To summarise, what this means is:
     * <ul>
     * <li>This method is called every time the Predictor notices a keepalive has expired, though the exact timing is
     * not guaranteed.
     * <li>This method does not take into account latency of the connection/keepalive
     * </ul>
     * 
     * @param predictor
     *            The KeepalivePredictor that noticed the expiration
     * @param msec
     *            The number of milliseconds since the keepalive was expected. This value is the absolute time since the
     *            keepalive was expected, and only concerns connection timeout; not load or latency.
     */
    public void predictedKeepaliveExpired(TrapKeepalivePredictor predictor, long msec);
    
    /**
     * Callback function asking the delegate to send a keepalive message with the specified properties. The keepalive
     * predictor itself does not directly affect the transport, but does ask for these things to happen.
     * 
     * @param isPing
     *            Whether the sent message is a ping or a pong
     * @param type
     *            The type of keepalive message. One of '1' or '2', depending on the specific configuration.
     * @param timer
     *            The timer requested
     * @param data
     *            Keepalive predictor internal data. This should be returned unchanged.
     */
    public void shouldSendKeepalive(boolean isPing, char type, int timer, byte[] data);
}
