package com.ericsson.research.trap.utils.spi;

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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.ericsson.research.trap.utils.Configuration;
import com.ericsson.research.trap.utils.StringUtil;

// I'd prefer to use generics, but this class has to be reusable with older
// versions than 1.5...
/**
 * @hide
 * @author Vladimir Katardjiev
 * @since 1.0
 */
public class ConfigurationImpl implements Configuration
{
    public static final String    CONFIG_HASH_PROPERTY = "trap.confighash";
    protected Map<String, String> config               = Collections.synchronizedMap(new HashMap<String, String>());
    
    public ConfigurationImpl()
    {
    }
    
    public ConfigurationImpl(String config)
    {
        if (config != null)
            this.initFromString(config);
    }
    
    public ConfigurationImpl(Configuration config)
    {
        this.config = ((ConfigurationImpl) config).config;
    }
    
    public ConfigurationImpl(Map<String, String> config)
    {
        this.config = config;
    }
    
    /*
     * Supported formats:
     *
     * x.y.z = value
     *
     * x.y.
     * .z = value (equivalent to x.y.z = value)
     * .b = value (equivalent to x.y.b = value)
     */
    public void initFromString(String config)
    {
        if (config == null)
            return;
        
        String[] strings = StringUtil.split(config, '\n');
        String prefix = "";
        
        for (int i = 0; i < strings.length; i++)
        {
            String c = strings[i].trim();
            
            //if (!c.startsWith("trap.")) continue; // Invalid formatted string.
            
            // trap.config_type.config_element.config_key = config_value
            
            int pos = c.indexOf('=');
            if ((pos < 0) || (pos >= (c.length() - 1)))
            {
                // May still be a valid prefix assignment
                if (c.endsWith("."))
                {
                    prefix = c.substring(0, c.length() - 1);
                }
                continue;
            }
            
            String key = c.substring(0, pos).trim();
            String val = c.substring(pos + 1).trim().replaceAll("\\\\n", "\n");
            
            if (key.startsWith("."))
                key = prefix + key;
            else
                prefix = "";
            
            this.setOption(key, val);
        }
    }
    
    public Map<String, String> getOptions(String optionsPrefix)
    {
        return this.getOptions(optionsPrefix, true);
    }
    
    public Map<String, String> getOptions(final String optionsPrefix, final boolean cutPrefixes)
    {
        PuttableGettableMap m = new PuttableGettableMap(this, optionsPrefix, cutPrefixes);
        return m;
    }
    
    public Configuration getChildOptions(final String optionsPrefix, final boolean cutPrefixes, final boolean updateParent)
    {
        
        if (updateParent)
            return new ConfigurationImpl(new PuttableGettableMap(this, optionsPrefix, cutPrefixes));
        
        int x = (cutPrefixes && !optionsPrefix.endsWith(".")) ? 1 : 0;
        Configuration c = new ConfigurationImpl();
        for (Iterator<String> it = this.getKeys().iterator(); it.hasNext();)
        {
            String key = it.next();
            String value = this.getOption(key);
            if (key.startsWith(optionsPrefix))
            {
                if (cutPrefixes)
                    key = key.substring(optionsPrefix.length() + x);
                c.setOption(key, value);
            }
        }
        return c;
    }
    
    public String getOption(String option)
    {
        return this.config.get(option);
    }
    
    public String getOption(String prefix, String option)
    {
        return this.getOption(prefix + "." + option);
    }
    
    public void setOption(String option, String value)
    {
        if (value == null)
            this.config.remove(option);
        else
            this.config.put(option, value);
    }
    
    public void setOption(String prefix, String option, String value)
    {
        this.setOption(prefix + "." + option, value);
    }
    
    public int getIntOption(String option, int defaultValue)
    {
        try
        {
            String strValue = this.getOption(option);
            
            if (strValue != null)
                return Integer.parseInt(strValue);
            else
                return defaultValue;
        }
        catch (NumberFormatException e)
        {
            return defaultValue;
        }
    }
    
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        Object[] keys = this.getKeys().toArray();
        Arrays.sort(keys);
        for (int i = 0; i < keys.length; i++)
        {
            sb.append(keys[i]);
            sb.append(" = ");
            
            String oval = this.getOption((String) keys[i]);
            
            if (oval != null)
                sb.append(oval.replaceAll("\n", "\\\\n"));
            
            sb.append("\n");
        }
        return sb.toString();
    }
    
    public Map<String, String> getConfig()
    {
        return this.config;
    }
    
    public void setConfig(HashMap<String, String> config)
    {
        this.config = config;
    }
    
    public boolean getBooleanOption(String key, boolean defaultValue)
    {
        try
        {
            String option = this.getOption(key);
            
            if (option == null)
                return defaultValue;
            
            return Boolean.valueOf(option);
        }
        catch (Exception e)
        {
            return defaultValue;
        }
    }
    
    public String getStringOption(String key, String defaultValue)
    {
        
        String rv = this.getOption(key);
        
        if (rv != null)
            return rv;
        else
            return defaultValue;
    }
    
    public Set<String> getKeys()
    {
        
        synchronized (this.config)
        {
            return new TreeSet<String>(this.config.keySet());
        }
    }
    
    public Set<String> getKeys(int keyDepth)
    {
        HashSet<String> keys = new HashSet<String>();
        Set<String> fullKeys = this.getKeys();
        
        Iterator<String> it = fullKeys.iterator();
        
        while (it.hasNext())
        {
            String key = it.next();
            
            // Now for some tricky dixie. We need the n'th period, where n=keyDepth
            int idx = 0;
            
            for (int i = 0; (i < keyDepth) && (i >= 0); i++)
                // And this loop will find it for us!
                idx = key.indexOf('.', idx);
            
            if (idx <= 0)
            {
                keys.add(key);
            }
            else
            {
                keys.add(key.substring(0, idx));
            }
        }
        
        return keys;
    }
    
    protected Map<String, String> createPuttableGettableMap(String optionsPrefix, boolean cutPrefixes)
    {
        return new PuttableGettableMap(this, optionsPrefix, cutPrefixes);
    }
    
    public long getLongOption(String option, long defaultValue)
    {
        try
        {
            return Long.parseLong(this.getOption(option));
        }
        catch (NumberFormatException e)
        {
            return defaultValue;
        }
    }
    
    public double getDoubleOption(String option, double defaultValue)
    {
        try
        {
            return Double.parseDouble(this.getOption(option));
        }
        catch (NumberFormatException e)
        {
            return defaultValue;
        }
    }
    
}

class PuttableGettableMap implements Map<String, String>
{
    
    private final String        prefix;
    private final boolean       cutPrefixes;
    private final Configuration parent;
    
    public PuttableGettableMap(Configuration parent, String prefix, boolean cutPrefixes)
    {
        this.parent = parent;
        this.prefix = prefix;
        this.cutPrefixes = cutPrefixes;
    }
    
    private String prefixKey(Object key)
    {
        StringBuffer sb = new StringBuffer(this.cutPrefixes ? this.prefix : key.toString());
        if (this.cutPrefixes)
        {
            if (!this.prefix.endsWith("."))
                sb.append(".");
            sb.append(key.toString());
        }
        return sb.toString();
    }
    
    public String put(String key, String value)
    {
        if ((key == null) || (value == null))
            throw new IllegalArgumentException();
        
        this.parent.setOption(this.prefixKey(key), value.toString());
        
        return value;
    }
    
    @SuppressWarnings("rawtypes")
    public void putAll(Map<? extends String, ? extends String> m)
    {
        Iterator<?> it = m.entrySet().iterator();
        
        while (it.hasNext())
        {
            Entry e = (java.util.Map.Entry) it.next();
            this.put(e.getKey().toString(), e.getValue().toString());
        }
    }
    
    public boolean containsKey(Object key)
    {
        return this.keySet().contains(key);
    }
    
    public Set<Entry<String, String>> entrySet()
    {
        TreeSet<Entry<String, String>> rv = new TreeSet<Entry<String, String>>();
        
        Set<String> s = this.keySet();
        Iterator<String> it = s.iterator();
        
        while (it.hasNext())
        {
            String key = it.next();
            rv.add(new MEntry<String, String>(this, key, this.get(key)));
        }
        return rv;
    }
    
    public String get(Object key)
    {
        if (!this.keySet().contains(key))
            return null;
        
        return this.parent.getOption(this.prefixKey(key));
    }
    
    public boolean isEmpty()
    {
        return this.keySet().size() == 0;
    }
    
    public Set<String> keySet()
    {
        Set<String> s = this.parent.getKeys();
        TreeSet<String> keySet = new TreeSet<String>();
        Iterator<String> it = s.iterator();
        int x = (this.cutPrefixes && !this.prefix.endsWith(".")) ? 1 : 0;
        
        while (it.hasNext())
        {
            String key = it.next();
            if (key.startsWith(this.prefix))
            {
                if (this.cutPrefixes)
                    key = key.substring(this.prefix.length() + x);
                
                keySet.add(key);
            }
            
        }
        
        return keySet;
    }
    
    public int size()
    {
        return this.keySet().size();
    }
    
    public Collection<String> values()
    {
        LinkedList<String> l = new LinkedList<String>();
        Set<String> s = this.keySet();
        Iterator<String> it = s.iterator();
        
        while (it.hasNext())
        {
            String key = it.next();
            l.add(this.get(key));
        }
        
        return l;
    }
    
    public void clear()
    {
        // Does nothing
        throw new IllegalAccessError("Cannot clear using a PGM");
    }
    
    public boolean containsValue(Object arg0)
    {
        throw new IllegalAccessError("Cannot check for values using a PGM");
    }
    
    public String remove(Object arg0)
    {
        throw new IllegalAccessError("Cannot remove using a PGM");
    };
}

class MEntry<K, V> implements Map.Entry<K, V>, Comparable<MEntry<K, V>>
{
    
    private final Map<K, V> parent;
    private final K         key;
    private final V         value;
    private Integer hash;
    
    public MEntry(Map<K, V> parent, K key, V value)
    {
        this.parent = parent;
        this.key = key;
        this.value = value;
    }
    
    public K getKey()
    {
        return this.key;
    }
    
    public V getValue()
    {
        return this.value;
    }
    
    public V setValue(V value)
    {
        return this.parent.put(this.key, value);
    }
    
    @Override
    public int hashCode()
    {
        if (this.hash == null)
        {
            this.hash = (this.key.hashCode() + this.value.hashCode());
        }
        
        return this.hash;
    }

    @SuppressWarnings("unchecked")
    public boolean equals(Object obj)
    {
        if (!(obj instanceof MEntry))
            return super.equals(obj);
        
        return this.compareTo((MEntry<K, V>) obj) == 0;
    }

    public int compareTo(MEntry<K, V> other)
    {
        if (this.key instanceof String && other.key instanceof String)
            return ((String) this.key).compareTo((String) other.getKey());
        else
            return this.key.hashCode() - other.key.hashCode();
    }
    
}
