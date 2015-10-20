package com.ericsson.research.trap.utils;

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

/**
 * Utility class to print out hexadecimal and pretty data, primarily for debug purposes. When printed, the data is shown
 * in a reasonable and understandable format, like the one below.
 * 
 * <pre>
 * 54 68 69 73 20 69 73 20 61 6e 20 65 78 61 6d 70 6c 65 20 6f This.is.an.example.o
 * 66 20 68 65 78 20 64 75 6d 70 69 6e 67 20 73 6f 6d 65 20 62 f.hex.dumping.some.b
 * 79 74 65 73 2e 2e 2e                                        ytes...
 * </pre>
 * 
 * @author Vladimir Katardjiev
 * @since 1.1
 */
public class HexDump
{
    
    /**
     * @hide
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException
    {
        byte[] bytes = StringUtil.toUtfBytes("This is an example of hex dumping some bytes...");//read(inputFileName, start, end);
        for (int index = 0; index < bytes.length; index += 20)
        {
            printHex(bytes, index, 20, System.out);
            printAscii(bytes, index, 20, System.out);
        }
    }
    
    /**
     * Creates a new object with a toString method that will lazily perform a HexDump on the supplied data. This can be
     * used when it is not known if the resulting string will be printed, to save on some computation.
     * 
     * @param bytes
     *            The bytes to HexDump
     * @param start
     *            Offset in the bytes to start at.
     * @param width
     *            Width (in bytes) of the hex dump
     * @return An object with a toString() method that returns the hexDump.
     */
    public static Object makeStringifyable(final byte[] bytes, final int start, final int width)
    {
        return new Object() {
            
            String str = null;
            
            public String toString()
            {
                if (this.str == null)
                {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    PrintStream ps;
                    try
                    {
                        ps = new PrintStream(bos, true, "UTF-8");
                        printBytes(bytes, start, width, ps);
                        this.str = bos.toString("UTF-8");
                    }
                    catch (UnsupportedEncodingException e)
                    {
                        e.printStackTrace();
                        this.str = "Unsupported Encoding";
                    }
                }
                return this.str;
            }
            
        };
    }
    
    /**
     * Prints the supplied bytes as ASCII to System.out.
     * 
     * @param bytes
     *            The bytes to print
     * @param start
     *            Index to start printing from
     * @param width
     *            Width in bytes
     * @see #printBytes(byte[], int, int, PrintStream)
     */
    public static void printBytes(byte[] bytes, int start, int width)
    {
        printBytes(bytes, start, width, System.out);
    }
    
    /**
     * Prints a set of bytes to a given stream. The output format is the one specified in the class description. Prints
     * the bytes as hex and ascii until the end of the byte array.
     * 
     * @param bytes
     *            The source bytes to print.
     * @param start
     *            The offset to start printing from.
     * @param width
     *            Width in bytes of the printed output
     * @param ps
     *            The stream to print to.
     */
    public static void printBytes(byte[] bytes, int start, int width, PrintStream ps)
    {
        for (int index = 0; index < bytes.length; index += width)
        {
            printHex(bytes, index, width, ps);
            try
            {
                printAscii(bytes, index, width, ps);
            }
            catch (UnsupportedEncodingException e)
            {
            }
        }
    }
    
    private static void printHex(byte[] bytes, int offset, int width, PrintStream os)
    {
        for (int index = 0; index < width; index++)
        {
            if (index + offset < bytes.length)
            {
                os.printf("%02x ", bytes[index + offset]);
            }
            else
            {
                os.print("   ");
            }
        }
    }
    
    private static void printAscii(byte[] bytes, int index, int width, PrintStream ps) throws UnsupportedEncodingException
    {
        if (index < bytes.length)
        {
            width = Math.min(width, bytes.length - index);
            ps.println(new String(bytes, index, width, "US-ASCII").replaceAll("[^\\x21-\\x7E]", "."));
        }
        else
        {
            ps.println();
        }
    }
    
    private HexDump(){}
}
