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



import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.ericsson.research.trap.TrapChannel;

/**
 * TrapMessage is a message that can be transported via Trap. This class defines the functionality of TrapMessages. A
 * TrapMessage encapsulates the data to be sent in a structured format, and allows for easy serialization.
 * <p>
 * The messages have two representation formats: 8-bit and 7-bit. The 8-bit is the recommended format, while 7-bit is
 * reserved for legacy use cases. The diagram below represents the 8-bit message format.
 * 
 * <pre>
 *       0               1               2               3
 *       0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *      +-+-+-----------+---------------+-------------------------------+
 *      |1|0| MESSAGEOP |C|  Reserved1  |             AUTHLEN           | ; R = Reserved, C = Compressed Flag, CHANID = Channel ID
 * 32   +-+-+-----------+---------------+-------------------------------+
 *      |                          Message ID                           |
 * 64   +---------------+---------------+-------------------------------+
 *      |   Reserved2   |   ChannelID   |           Reserved3           |
 * 96   +---------------+---------------+-------------------------------+
 *      |                        Content Length                         | ; Length of the payload that begins *after* AUTHLEN
 * 128  +---------------------------------------------------------------+
 *      |        ... Authentication Data (0...65535 bytes) ...          | ; MUST be US-ASCII (0x20 - 0x7F)
 *      +---------------------------------------------------------------+
 *      |                         Payload Data                          | ; Optional content, specified by Content Length
 *      + - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - + ; Payload begins at offset (in bytes) 12+AUTHLEN
 *      |                     Payload Data continued ...                |
 *      +---------------------------------------------------------------+
 * </pre>
 * 
 * @author Vladimir Katardjiev
 */
public interface TrapMessage
{
    /**
     * This subclass defines the possible message operations of a TrapMessage. For Trap, this is the complete subset of
     * operations supported, for this version, and may not be extended without requiring a clean break. That is, Trap
     * endpoints should never silently ignore invalid operations, so remote Trap endpoints MUST ensure they do not send
     * commands that are invalid.
     * 
     * @author Vladimir Katardjiev
     */
    public static class Operation
    {
        /**
         * Contains numeric values for the operations, to be used in switch statements. See {@link Operation} for a
         * description of the values.
         * 
         * @since 1.1
         */
        public static class Value
        {
            public static final int OPEN           = 1;
            public static final int OPENED         = 2;
            public static final int CLOSE          = 3;
            public static final int END            = 4;
            public static final int CHALLENGE      = 5;
            public static final int ERROR          = 6;
            public static final int MESSAGE        = 8;
            public static final int ACK            = 9;
            public static final int FRAGMENT_START = 10;
            public static final int FRAGMENT_END   = 11;
            public static final int OK             = 16;
            public static final int PING           = 17;
            public static final int PONG           = 18;
            public static final int TRANSPORT      = 19;
        }
        
        /**
         * The OPEN operation is sent from an opening transport to a remote side.
         */
        public static final Operation OPEN           = new Operation(Value.OPEN);
        
        /**
         * The OPENED message is used to respond to an OPEN query.
         */
        public static final Operation OPENED         = new Operation(Value.OPENED);
        
        /**
         * CLOSE is sent from both sides to acknowledge a closing transport.
         */
        public static final Operation CLOSE          = new Operation(Value.CLOSE);
        
        /**
         * END is sent to terminate a TrapEndpoint connection, including all transports.
         */
        public static final Operation END            = new Operation(Value.END);
        
        /**
         * Sent to send some authentication challenge data.
         */
        public static final Operation CHALLENGE      = new Operation(Value.CHALLENGE);
        
        /**
         * Denotes an error has occurred. The connection should be terminated.
         */
        public static final Operation ERROR          = new Operation(Value.ERROR);
        
        /**
         * Regular message. Send to application.
         */
        public static final Operation MESSAGE        = new Operation(Value.MESSAGE);
        /**
         * Used by transports to acknowledge receipt of a message. Transports that fail sending messages MUST trigger
         * ttMessagesFailedSending. The ACK operation is reserved for transport usage for that purpose.
         */
        public static final Operation ACK            = new Operation(Value.ACK);
        
        /**
         * Marks the beginning of a fragmented message. The data of this message, including all {@link #MESSAGE} objects
         * that follow, and terminated by (and including the data of) the {@link #FRAGMENT_END} message are actually a
         * single message, and should be represented as such to the application, unless streaming mode has been enabled
         * in {@link TrapChannel#setStreamingMode(boolean)}.
         */
        public static final Operation FRAGMENT_START = new Operation(Value.FRAGMENT_START);
        
        /**
         * Marks the end of a fragmented message. If the channel is in regular mode, this is when the reconstructed
         * message should be dispatched.
         */
        public static final Operation FRAGMENT_END   = new Operation(Value.FRAGMENT_END);
        
        /**
         * No-op response.
         */
        public static final Operation OK             = new Operation(Value.OK);
        
        /**
         * PING the remote side.
         */
        public static final Operation PING           = new Operation(Value.PING);
        
        /**
         * Respond to a PING.
         */
        public static final Operation PONG           = new Operation(Value.PONG);
        
        /**
         * Reserved operation for transport-specific operations. The messages will not be seen by the endpoint.
         */
        public static final Operation TRANSPORT      = new Operation(Value.TRANSPORT);
        
        int                           op;
        
        private Operation(int op)
        {
            this.op = op;
        }
        
        /**
         * Gets the numeric representation of an operation. These numbers represent the protocol values for the
         * operations.
         * 
         * @return A number representing the operation.
         */
        public int getOp()
        {
            return this.op;
        }
        
        /**
         * Gets an Operation object from a transport-level operation.
         * 
         * @param op
         *            The operation ID to parse.
         * @return The corresponding Operation object.
         */
        public static Operation getType(int op)
        {
            switch (op)
            {
                case Operation.Value.OPEN:
                    return OPEN;
                    
                case Operation.Value.OPENED:
                    return OPENED;
                    
                case Operation.Value.CLOSE:
                    return CLOSE;
                    
                case Operation.Value.END:
                    return END;
                    
                case Operation.Value.CHALLENGE:
                    return CHALLENGE;
                    
                case Operation.Value.ERROR:
                    return ERROR;
                    
                case Operation.Value.MESSAGE:
                    return MESSAGE;
                    
                case Operation.Value.ACK:
                    return ACK;
                    
                case Operation.Value.FRAGMENT_START:
                    return FRAGMENT_START;
                    
                case Operation.Value.FRAGMENT_END:
                    return FRAGMENT_END;
                    
                case Operation.Value.OK:
                    return OK;
                    
                case Operation.Value.PING:
                    return PING;
                    
                case Operation.Value.PONG:
                    return PONG;
                    
                case Operation.Value.TRANSPORT:
                    return TRANSPORT;
                    
                default:
                    throw new UnsupportedOperationException("Unknown op type: " + op);
            }
        }
        
        /**
         * Provides a human readable representation of the message operation. There is no functional property to this
         * value.
         * 
         * @return The operation name
         */
        public String toString()
        {
            switch (this.op)
            {
                case Operation.Value.OPEN:
                    return "OPEN";
                    
                case Operation.Value.OPENED:
                    return "OPENED";
                    
                case Operation.Value.CLOSE:
                    return "CLOSE";
                    
                case Operation.Value.END:
                    return "END";
                    
                case Operation.Value.CHALLENGE:
                    return "CHALLENGE";
                    
                case Operation.Value.ERROR:
                    return "ERROR";
                    
                case Operation.Value.MESSAGE:
                    return "MESSAGE";
                    
                case Operation.Value.ACK:
                    return "ACK";
                    
                case Operation.Value.FRAGMENT_START:
                    return "FRAGMENT_START";
                    
                case Operation.Value.FRAGMENT_END:
                    return "FRAGMENT_END";
                    
                case Operation.Value.OK:
                    return "OK";
                    
                case Operation.Value.PING:
                    return "PING";
                    
                case Operation.Value.PONG:
                    return "PONG";
                    
                case Operation.Value.TRANSPORT:
                    return "TRANSPORT";
                    
                default:
                    return "Unknown op type: " + this.op;
            }
        }
        
    }
    
    /**
     * The Format class specifies the available Trap message formats. These are specified in [REF], and should be
     * implemented where supported. The most efficient format is the "regular" one, when it comes to CPU cycles,
     * although there is no bandwidth difference between the two formats.
     * <p>
     * Format switching is allowed only in response to an OPEN message; an established session MUST NOT change formats.
     * This allows implementations to optimise away format checking logic beyond the OPEN handshake.
     * 
     * @author Vladimir Katardjiev
     */
    public static class Format
    {
        private final String string;
        
        private Format(String string)
        {
            this.string = string;
        }
        
        /**
         * Denotes the regular, 8-bit message format for Trap.
         */
        public static final Format REGULAR        = new Format("REGULAR");
        
        /**
         * Denotes a JavaScript-safe 7-bit message format.
         */
        public static final Format SEVEN_BIT_SAFE = new Format("7-BIT");
        
        /**
         * Provides a human readable string for the format.
         * 
         * @return The format name
         */
        public String toString()
        {
            return this.string;
        }
    }
    
    /**
     * Serializes this TrapMessage into a byte array. This includes the headers and data. The format used is the one
     * specified in the message settings. This operation is not cached.
     * 
     * @return A serialized representation of the message.
     * @throws IOException
     *             If serialization fails.
     */
    public abstract byte[] serialize() throws IOException;
    
    /**
     * Attempts to deserialize a TrapMessage.
     * 
     * @param rawData
     *            The data to read from
     * @param length
     *            The maximum number of bytes that can be read
     * @param offset
     *            The start of the buffer, where to read from.
     * @return -1 if it could not parse a message from the data, the number of bytes consumed otherwise.
     * @throws UnsupportedEncodingException
     *             if the message encoding is not supported
     */
    public abstract int deserialize(byte[] rawData, int offset, int length) throws UnsupportedEncodingException;
    
    /**
     * Gets the message's <i>data</i> payload, as set by the user. If data is not serialized at this point, will
     * serialize it. If serialized, the resulting data is cached, so serialization occurs at most once.
     * 
     * @return The serialized payload.
     */
    public abstract byte[] getData();
    
    /**
     * Sets the message's data (payload)
     * 
     * @param data
     *            The data to set
     * @return The TrapMessage object. Useful for chaining multiple .setX calls.
     */
    public abstract TrapMessage setData(byte[] data);
    
    /**
     * Accessor for the authentication payload.
     * 
     * @return The authentication payload, as a UTF string.
     */
    public abstract String getAuthData();
    
    /**
     * Sets the message's authentication string/header
     * 
     * @param authData
     *            The string to set
     * @return The TrapMessage object. Useful for chaining multiple .setX calls.
     */
    public abstract TrapMessage setAuthData(String authData);
    
    /**
     * Accessor for the message format.
     * 
     * @return The current message format.
     */
    public abstract Format getFormat();
    
    /**
     * Sets a new message format.
     * 
     * @param format
     *            The new format to use for serialzation.
     * @return The TrapMessage object. Useful for chaining multiple .setX calls.
     */
    public abstract TrapMessage setFormat(Format format);
    
    /**
     * Fetches the message operation.
     * 
     * @return The current message operation.
     */
    public abstract Operation getOp();
    
    /**
     * Sets the message operation.
     * 
     * @param op
     *            The new operation.
     * @return The TrapMessage object. Useful for chaining multiple .setX calls.
     */
    public abstract TrapMessage setOp(Operation op);
    
    /**
     * Sets the message id. Messages must have a unique ID (except for certain transport messages). This method should
     * only be called by Trap.
     * 
     * @param newMessageId
     *            The new message ID.
     * @return keepaliveReceived
     */
    public abstract TrapMessage setMessageId(int newMessageId);
    
    /**
     * Fetches the message ID.
     * 
     * @return The current message id.
     */
    public abstract int getMessageId();
    
    /**
     * The length in bytes of this message, as it would appear when serialized using the current settings. There is no
     * guarantee this will be the number of bytes sent on the wire, but it is an estimate (if nothing else changes).
     * <p>
     * This method performs an estimate computation, so may be off in case of compressible encodings.
     * 
     * @return The serialized length, in bytes
     */
    public abstract long length();
    
    /**
     * Sets the <i>compressed</i> flag of this message. If true, Trap will attempt to automatically compress (and
     * decompress on the remote side)
     * 
     * @param isCompressed
     *            <i>true</i> if Trap should compress this message, <i>false</i> otherwise.
     * @return An instance of this message, for chaining.
     * @since 1.1
     */
    public abstract TrapMessage setCompressed(boolean isCompressed);
    
    /**
     * Checks whether this message will be or was transmitted compressed.
     * 
     * @return <i>true</i> if compression was enabled for this message, <i>false</i> otherwise.
     * @since 1.1
     */
    public abstract boolean isCompressed();
    
    /**
     * Sets the channel this message should be transmitted on. Trap supports channels with IDs 0-63, where 0 is the
     * default channel. Each channel has a separate transmission queue, enabling multiplexing onto a single Trap logical
     * connection.
     * <p>
     * Note that ID 0 is the control channel on which trap signalling goes. While it is possible to send a message on
     * this channel, it is <b>strongly discouraged</b>. Sending application messages on channel ID 0 will be deprecated
     * in the future.
     * 
     * @param channelID
     *            The Channel ID to set. Allowed values are [0,63]. This setting should not be changed after the message
     *            is enqueued.
     * @return This TrapMessage instance, for chaining.
     * @throws IllegalArgumentException
     *             If <i>channelId</i> is outside the permitted range of [0,63].
     * @since 1.1
     */
    public abstract TrapMessage setChannel(int channelID);
    
    /**
     * Retrieves the channel this message was sent/received on.
     * 
     * @return The channel ID, in the range [0,63].
     * @since 1.1
     */
    public abstract int getChannel();
    
    /**
     * Retrieves the compressed data, if compression is enabled. Else returns the message data.
     * 
     * @return The message data, compressed if enabled.
     * @since 1.1
     */
    byte[] getCompressedData();
    
    /**
     * Generates a human-readable string describing this message. The string representation will contain the message
     * operation, channel, id, and the data length (if present). For example:
     * 
     * <pre>
     * OK/C0/0 - Plain OK message with no body, on the control channel.
     * MESSAGE/C5/42/6 - Message with op MESSAGE, on channel 5, with ID 42, and 6 bytes payload.
     * </pre>
     * 
     * It is not possible to use toString() to serialize the message. The {@link #serialize()} method exists for that
     * (but is not human readable).
     * 
     * @return A string with concise information about the message.
     */
    public String toString();
    
}
