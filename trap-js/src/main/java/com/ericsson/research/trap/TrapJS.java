package com.ericsson.research.trap;

import java.io.InputStream;

/**
 * Convenience class to load the Trap JS files from within Java.
 * 
 * @author Vladimir Katardjiev
 * @since 1.4.1
 */
public class TrapJS
{
    /**
     * Trap-full is the unminified, debug-friendly version of the Trap JS library.
     * @return An InputStream to trap-full, or <i>null</i> if it was not found on the classpath.
     */
    public static InputStream getFull()
    {
        return TrapJS.class.getClassLoader().getResourceAsStream("trap-full.js");
    }

    /**
     * Trap-min is the minified, deployment-friendly version of the Trap JS library.
     * @return An InputStream to trap-min, or <i>null</i> if it was not found on the classpath.
     */
    public static InputStream getMin()
    {
        return TrapJS.class.getClassLoader().getResourceAsStream("trap-min.js");
    }
}
