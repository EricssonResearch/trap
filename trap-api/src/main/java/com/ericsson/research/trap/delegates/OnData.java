package com.ericsson.research.trap.delegates;

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



import com.ericsson.research.trap.TrapEndpoint;

/**
 * Implement this interface to handle data callbacks from Trap Endpoints.
 * 
 * @author Vladimir Katardjiev
 * @since 1.1
 */
public interface OnData extends TrapDelegate
{
    /**
     * Called when the Trap endpoint has received byte data from the other end. This method executes in a Trap thread,
     * so it should only perform minimal operations before returning, in order to allow for maximum throughput. <h3>
     * Threading Considerations</h3>
     * <p>
     * In the default -- and recommended -- asynchronous mode (see {@link TrapEndpoint#setAsync(boolean)} for a
     * discussion about that), trapData <b>may be called concurrently</b> when multiple channels are used. This occurs
     * when the client uses different channel IDs to send data. Within a single channel ID, however, trapData will only
     * be called by one thread at a time.
     * <p>
     * For example, suppose the client sends messages on both channels 1 and 2. This means trapData can be called from
     * two threads concurrently. The first thread will handle the data for channel 1, and the second will receive the
     * data for channel 2. It is therefore important to synchronise any data structures shared between the two channels.
     * In the best-case scenario, the two channels can work completely independent, and thus achieve full parallelism.
     * <p>
     * If the client only ever sends data on a single channel, this method will never be called by multiple threads, and
     * does thus not need to be synchronised.
     * 
     * @param data
     *            The data received. The ownership of this array is transferred to the receiver. It will not be
     *            overwritten by Trap, and Trap will not read any changes off of it.
     * @param endpoint
     *            The TrapEndpoint that received the data.
     * @param context
     *            The original context object supplied to the TrapEndpoint for this delegate. Will be <i>null</i> if no
     *            context was supplied.
     * @param channel
     *            The Channel ID on which this data was received.
     */
    public void trapData(byte[] data, int channel, TrapEndpoint endpoint, Object context);
}
