package com.ericsson.research.trap;

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
 * Interface that should be implemented by objects to be transported using Trap. This allows applications that use Trap
 * and loopback messaging to avoid the serialization/deserialization step of using Trap. If an application's data
 * implements TrapObject, they can send the data object directly into Trap, and, if the loopback interface is enabled,
 * will receive the objects back directly.
 * <p>
 * Applications <b>must not assume</b> they will receive a TrapObject if they send a TrapObject, however. For various
 * reasons (sandboxing, permissions, deployment), Trap may be forced to serialize the message to transfer it;
 * applications must be ready for both contingencies. This also means the TrapObject's {@link #getSerializedData()}
 * method <b>must not</b> return null.
 * 
 * @author Vladimir Katardjiev
 */
public interface TrapObject
{
    /**
     * Fetches a serialized representation of this object's data. This method will be called <i>at most once</i>, but
     * may never be called if the object is sent locally via a loopback (direct function call).
     * <p>
     * When a TrapObject is supplied for sending by Trap, it is serialized as late as possible, with a preference to
     * never. As such, a TrapObject may be transferred to the other endpoint as a ready-to-go object, if that is
     * possible. <b>This behaviour must not be assumed</b>, which is why TrapObjects must implement this method.
     * <p>
     * Once this method is called, any further changes to the object will not be reflected into Trap. Trap will not call
     * this method a second time (for performance reasons). This method will generally execute in a Trap thread, so it
     * must not assume anything about the context in which it executes.
     * 
     * @return a byte array representing this object in serialized form
     */
    public byte[] getSerializedData();
}
