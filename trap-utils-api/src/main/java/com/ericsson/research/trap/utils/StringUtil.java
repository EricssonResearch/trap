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

import java.io.UnsupportedEncodingException;

/**
 * StringUtil contains a number of Java 1.4 and Android-safe String operations that should be preferred over the Java
 * native one. It will use the JSE one where available, but take care of the compatibility with Android and earlier Java
 * versions.
 * 
 * @author Vladimir Katardjiev
 */
public abstract class StringUtil
{
    
    private static StringUtil instance;
    
    static
    {
        try
        {
            String name = StringUtil.class.getName() + "Impl";
            Class<?> c = Class.forName(name);
            instance = (StringUtil) c.newInstance();
        }
        catch (Throwable t)
        {
            System.err.println("Could not initialise StringUtilImpl");
        }
    }
    
    /**
     * Splits the string <i>s</i> using the character <i>c</i> as a delimiter
     * 
     * @param s
     *            The string to split
     * @param c
     *            The delimiter
     * @return An array of the string parts.
     */
    public static String[] split(String s, char c)
    {
        return instance.doSplit(s, c);
    }
    
    abstract String[] doSplit(String s, char c);
    
    /**
     * Converts the supplied string into its UTF-8 representation.
     * 
     * @param s
     *            The string to convert.
     * @return The UTF-8 representation of <i>s</i>
     * @throws RuntimeException
     *             in the rare event that the JVM does not support UTF-8. It should always support it, so this function
     *             wraps away the exception
     */
    public static byte[] toUtfBytes(final String s)
    {
        try
        {
            byte[] b = s.getBytes("UTF-8");
            return (b);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException("This java machine is stupid. The code will die. Cake.");
        }
    }
    
    /**
     * Convert a UTF-8 byte array to a string.
     * 
     * @param buffer
     *            The array to convert
     * @return A string representing the array.
     * @throws RuntimeException
     *             in the rare event that the JVM does not support UTF-8. It should always support it, so this function
     *             wraps away the exception
     */
    public static String toUtfString(final byte[] buffer)
    {
        try
        {
            String s = new String(buffer, "UTF-8");
            return (s);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException("This java machine is stupid. The code will die. Cake.");
        }
    }
    
    /**
     * Convert a char array to UTF string
     * 
     * @param encoded
     *            The UTF-16 char array
     * @return A string with the array
     */
    public static String toUtfString(char[] encoded)
    {
        String s = new String(encoded);
        return s;
    }
    
    /**
     * Formats the object's name and full class name to be logger-safe
     * 
     * @param o
     *            The object to create the logger name for.
     * @return The logger name for the object.
     */
    public static String getLoggerComponent(Object o)
    {
        String component = o.getClass().getName().substring(o.getClass().getPackage().getName().length());
        if (!component.startsWith("."))
            component = "." + component;
        
        if (component.startsWith(".."))
            component = component.substring(1);
        
        return component;
    }
    
    StringUtil()
    {
    }
    
    /**
     * Convert a UTF-8 byte array to a string.
     * 
     * @param buffer
     *            The array to convert
     * @param i
     *            The index to start at
     * @param length
     *            The number of bytes to convert
     * @return A string representing the array.
     * @throws RuntimeException
     *             in the rare event that the JVM does not support UTF-8. It should always support it, so this function
     *             wraps away the exception
     */
    public static String toUtfString(byte[] bytes, int i, int length)
    {
        try
        {
            String s = new String(bytes, i, length, "UTF-8");
            return (s);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException("This java machine is stupid. The code will die. Cake.");
        }
    }
    
}
