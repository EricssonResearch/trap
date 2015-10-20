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

import java.util.Map;
import java.util.Set;

/**
 * A generic configuration interface, capable of serialisation, deserialisation
 * and some primitive operations. The supplied implementation is capable of
 * running on virtually any JVM, providing rudimentary configuration
 * possibilities.
 * 
 * @author Vladimir Katardjiev
 */
public interface Configuration
{
	/**
	 * Parses the configuration parameters in the given string and replaces just
	 * those entries in this configuration object. No entries will be removed.
	 * 
	 * @param config
	 *            The string to parse
	 */
	public abstract void initFromString(String config);
	
	/**
	 * Convenience method to access {@link #getOptions(String, boolean)} with
	 * the cutPrefixes value to true.
	 * 
	 * @param optionsPrefix
	 *            The prefix containing the options to include in the map
	 * @return The requested subset of options, in the form of a map.
	 */
	public abstract Map<String, String> getOptions(String optionsPrefix);
	
	/**
	 * Fetches a subset of the available options of this configuration as a live
	 * map. The options are sorted based on a certain prefix, which may
	 * optionally be removed from the map.
	 * <p>
	 * Any changes to the returned map will be propagated back to the parent
	 * configuration. Any changes to the parent configuration will <i>not</i> be
	 * reflected in the child.
	 * 
	 * @param optionsPrefix
	 *            The prefix string containing the options to include in the
	 *            map.
	 * @param cutPrefixes
	 *            <i>true</i> to remove <i>optionsPrefix</i> from the keys in
	 *            the returned map, <i>false</i> otherwise.
	 * @return The requested subset of options, in the form of a map.
	 */
	public abstract Map<String, String> getOptions(String optionsPrefix, boolean cutPrefixes);
	
	/**
	 * Fetches a set of configuration prefixes as a Configuration instance. This
	 * instance can be used to iterate, list or modify the configuration as
	 * needed.
	 * 
	 * @param optionsPrefix
	 *            The prefix to extract the config for.
	 * @param cutPrefixes
	 *            Whether to remove the prefixes from the returned configuration
	 * @param updateParent
	 *            If set to <i>true</i>, the configuration returned will update
	 *            the parent configuration if any of the child's options are
	 *            altered. In essence, what this means is that the child
	 *            Configuration will become a view of the parent containing only
	 *            a subset of the parent's keys, but with the values backed by
	 *            the parent.
	 * @return A forked configuration
	 */
	public Configuration getChildOptions(String optionsPrefix, final boolean cutPrefixes, final boolean updateParent);
	
	/**
	 * Fetches the value of a given option.
	 * 
	 * @param option
	 *            The fully qualified option name.
	 * @return The value of the option, or null if undefined.
	 */
	public abstract String getOption(String option);
	
	/**
	 * Get an option with a prefix.
	 * 
	 * @param prefix
	 *            The prefix string
	 * @param option
	 *            The option string
	 * @return The value of the option, or null.
	 */
	public abstract String getOption(String prefix, String option);
	
	/**
	 * Sets an option. The option must be prefixed.
	 * 
	 * @param option
	 *            The option name
	 * @param value
	 *            The option value.
	 */
	public abstract void setOption(String option, String value);
	
	/**
	 * Sets an option with a prefix.
	 * 
	 * @param prefix
	 *            THe prefix string.
	 * @param option
	 *            The option name.
	 * @param value
	 *            The new option value.
	 */
	public abstract void setOption(String prefix, String option, String value);
	
	/**
	 * Serializes the configuration into a string.
	 * 
	 * @return The string representation of the configuration and all keys and
	 *         values.
	 */
	public abstract String toString();
	
	/**
	 * Gets an option as an int.
	 * 
	 * @param string
	 *            The full option name, including prefix.
	 * @param defaultValue
	 *            The value to return if the option is undefined.
	 * @return The value of the option, or defaultValue.
	 */
	public abstract int getIntOption(String string, int defaultValue);
    
    /**
     * Gets an option as a long.
     * 
     * @param option
     *            The full option name, including prefix.
     * @param defaultValue
     *            The value to return if the option is undefined.
     * @return The value of the option, or defaultValue.
     */
    public abstract long getLongOption(String option, long defaultValue);
    
    /**
     * Gets an option as a double.
     * 
     * @param option
     *            The full option name, including prefix.
     * @param defaultValue
     *            The value to return if the option is undefined.
     * @return The value of the option, or defaultValue.
     */
    public abstract double getDoubleOption(String option, double defaultValue);
	
	/**
	 * Gets an option as a boolean.
	 * 
	 * @param option
	 *            The full option name, including prefix.
	 * @param defaultValue
	 *            The value to return if the option is undefined.
	 * @return The value of the option, or defaultValue.
	 */
	public abstract boolean getBooleanOption(String option, boolean defaultValue);
	
	/**
	 * Gets an option as a string.
	 * 
	 * @param option
	 *            The full option name, including prefix.
	 * @param defaultValue
	 *            The value to return if the option is undefined.
	 * @return The value of the option, or defaultValue.
	 */
	public abstract String getStringOption(String option, String defaultValue);
	
	/**
	 * Returns the set of keys that are contained in this configuration at or
	 * after the call to getKeys() is made, but before it returns. The exact set
	 * of keys is an immutable snapshot (and thus not prone to
	 * ConcurrentModificationException), but is a complete clone of the key set
	 * each time this method is called.
	 * 
	 * @return The set of keys in this configuration
	 */
	public abstract Set<String> getKeys();
	
	/**
	 * Get the keys up to a certain depth, as represented by the number of
	 * periods in the key.
	 * 
	 * @param keyDepth
	 *            The depth to get.
	 * @return The set of unique keys at the given depth.
	 */
	public abstract Set<String> getKeys(int keyDepth);
}
