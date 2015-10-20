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



import com.ericsson.research.trap.TrapKeepalivePolicy;

/**
 * A TrapKeepalivePredictor allows TrapTransports to determine on an automatic basis the next keepalive. Implementations
 * of this class can attempt various predictions on how long keepalives to use
 * 
 * @author Vladimir Katardjiev
 */
public interface TrapKeepalivePredictor
{
    
    /**
     * Sets the minimum keepalive timer supported by this transport.
     * 
     * @param min
     *            The minimum keepalive interval, in seconds.
     */
    void setMinKeepalive(int min);
    
    /**
     * Sets the maximum keepalive timer supported by the transport. Called by the transport when a new
     * KeepalivePredictor is assigned to it. In effect, this predictor must then ignore any setKeepaliveInterval calls
     * with a value larger than max.
     * 
     * @param max
     *            The maximum keepalive interval, in seconds.
     */
    void setMaxKeepalive(int max);
    
    /**
     * Sets the minimum keepalive interval automatic keepalive time adjustments are allowed to use.
     * 
     * @param min
     *            The new minimum, in seconds
     */
    void setMinAutoKeepalive(int min);
    
    /**
     * Sets the maximum keepalive interval automatic keepalive time adjustments are allowed to use.
     * 
     * @param max
     *            The new maximum, in seconds
     */
    void setMaxAutoKeepalive(int max);
    
    /**
     * Sets the keepalive interval, in seconds. Some special values are reserved from {@link TrapKeepalivePolicy}.
     * 
     * @param interval
     *            The new keepalive interval.
     */
    void setKeepaliveInterval(int interval);
    
    /**
     * Fetches the keepalive interval.
     * 
     * @return The current keepalive interval, in seconds.
     */
    int getKeepaliveInterval();
    
    /**
     * Fetches the number of msec until the next keepalive should be sent. This differs from the receive timer, and is
     * used to determine when to dispatch a new keepalive to the remote side.
     * 
     * @return the number of msec until the next keepalive.
     */
    long getNextKeepaliveSend();
    
    /**
     * Starts the predictor's timeout facilities, enabling the predictor to do timeout notifications.
     */
    void start();
    
    /**
     * Stops the predictor's timeout notifications/timers.
     */
    void stop();
    
    /**
     * Called when a keepalive (PING or PONG) is received.
     * 
     * @param isPing
     *            Whether the sent message is a ping or a pong
     * @param pingType
     *            The type of ping that is received.
     * @param remoteTimer
     *            The remote side's requested time until next keepalive
     * @param data
     *            The payload of the ping, as sent from this side.
     */
    void keepaliveReceived(boolean isPing, char pingType, int remoteTimer, byte[] data);
    
    /**
     * Called by the transport when it has received some data – any data.
     */
    void dataReceived();
    
    /**
     * Called by the transport when it has sent some data – any data.
     */
    void dataSent();
    
    /**
     * Determines when the predictor expects the next keepalive to be received. This can be a positive number of the
     * next keepalive is in the future, or a negative number if the keepalive was supposed to happen but didn't.
     * <p>
     * This method can be used to inspect whether the keepalive mechanism is working (on the local side) or whether the
     * remote side is connected.
     * 
     * @return The number of milliseconds until/since the keepalive was expected.
     */
    long nextKeepaliveReceivedDelta();
    
    /**
     * Sets a delegate for timeout notifications. This delegate MUST be <b>weakly</b> referenced, to prevent the
     * predictor from stopping garbage collection with its timers.
     * 
     * @param delegate
     *            The delegate that will receive the predictor's, well, predictions!
     */
    void setDelegate(TrapKeepaliveDelegate delegate);
    
    /**
     * Sets the keepalive expiry. This controls how long time after a missed keepalive should be permitted to pass
     * before the predictor notifies the delegate of the fact. The expiry value should be long enough to not produce too
     * many false positives due to network latency.
     * 
     * @param newExpiry
     *            A new expiry value, in milliseconds. Must be positive.
     */
    public void setKeepaliveExpiry(long newExpiry);
    
    /**
     * Accessor for the current keepalive expiry.
     * 
     * @return The keepalive expiry, in msec.
     */
    public long getKeepaliveExpiry();
}
