package com.ericsson.research.trap.utils;

/*
 * ##_BEGIN_LICENSE_## Transport Abstraction Package (trap) ---------- Copyright (C) 2014 Ericsson AB ---------- Redistribution
 * and use in source and binary forms, with or without modification, are permitted provided that the following conditions are
 * met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Ericsson AB nor the names of its contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. ##_END_LICENSE_##
 */

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Provides a map with a weak binding of its values. Backed by ConcurrentSkipListMap and RefQueues. Thread safe, OK performance.
 * Mainly intended for NIO-style objects where we want the objects to be GC-able unless referenced by the application. Keys are
 * strongly bound (generally assumed to be an identifier belonging to the object anyway).
 * 
 * @author evlakat
 */
public class WeakMap<K, V> implements Map<K, V>
{
    private static final long                             CLEANUP_INTERVAL = 5000;
    ConcurrentSkipListMap<K, EqualMappedWeakReference<V>> values           = new ConcurrentSkipListMap<K, EqualMappedWeakReference<V>>();
    ReferenceQueue<V>                                     refQueue         = new ReferenceQueue<V>();
    
    Runnable                                              cleanupTask      = new Runnable() {
                                                                               
                                                                               public void run()
                                                                               {
                                                                                   try
                                                                                   {
                                                                                       for (;;)
                                                                                       {
                                                                                           Reference<? extends V> ref = refQueue.poll();
                                                                                           
                                                                                           if (ref == null)
                                                                                               return;
                                                                                           
                                                                                           values.remove(((EqualMappedWeakReference<? extends V>) ref).key);
                                                                                       }
                                                                                   }
                                                                                   finally
                                                                                   {
                                                                                       ThreadPool.weakExecuteAfter(this, CLEANUP_INTERVAL);
                                                                                   }
                                                                               }
                                                                           };
    
    public WeakMap()
    {
        cleanupTask.run();
    }
    
    public int size()
    {
        return this.values.size();
    }
    
    public boolean isEmpty()
    {
        return this.values.isEmpty();
    }
    
    public boolean containsKey(Object key)
    {
        EqualMappedWeakReference<V> ref = this.values.get(key);
        
        if (ref != null && ref.get() != null)
            return true;
        
        return false;
    }
    
    public boolean containsValue(Object value)
    {
        return this.values.containsValue(new EqualWeakReference<Object>(value));
    }
    
    public V get(Object key)
    {
        
        EqualWeakReference<V> ref = this.values.get(key);
        
        if (ref != null)
            return ref.get();
        else
            return null;
    }
    
    public V put(K key, V value)
    {
        this.values.put(key, new EqualMappedWeakReference<V>(value, key, this.refQueue));
        return value;
    }
    
    public V remove(Object key)
    {
        EqualWeakReference<V> ref = this.values.remove(key);
        
        if (ref != null)
            return ref.get();
        
        return null;
    }
    
    public void putAll(Map<? extends K, ? extends V> m)
    {
        Set<?> entrySet = m.entrySet();
        
        for (Object o : entrySet)
        {
            @SuppressWarnings("unchecked")
            Entry<? extends K, ? extends V> e = (Entry<? extends K, ? extends V>) o;
            this.put(e.getKey(), e.getValue());
        }
    }
    
    public void clear()
    {
        this.values.clear();
    }
    
    public Set<K> keySet()
    {
        return this.values.keySet();
    }
    
    public Collection<V> values()
    {
        Collection<EqualMappedWeakReference<V>> vs = this.values.values();
        
        HashSet<V> rv = new HashSet<V>();
        
        for (EqualWeakReference<V> v : vs)
        {
            V val = v.get();
            
            if (val != null)
                rv.add(val);
        }
        
        return rv;
    }
    
    public Set<Entry<K, V>> entrySet()
    {
        
        Set<Entry<K, EqualMappedWeakReference<V>>> entries = this.values.entrySet();
        Set<Entry<K, V>> rv = new HashSet<Entry<K, V>>();
        
        for (Entry<K, EqualMappedWeakReference<V>> entry : entries)
        {
            V v = entry.getValue().get();
            if (v != null)
            {
                Entry<K, V> e = new ReturnedEntry<K, V>(entry.getKey(), entry.getValue().get(), this);
                rv.add(e);
            }
        }
        return rv;
    }
    
}

class EqualWeakReference<T> extends WeakReference<T>
{
    
    public EqualWeakReference(T referent, ReferenceQueue<? super T> q)
    {
        super(referent, q);
    }
    
    public EqualWeakReference(T value)
    {
        super(value);
    }
    
    @Override
    public int hashCode()
    {
        T t = this.get();
        return t != null ? t.hashCode() : 0;
    }
    
    @Override
    public boolean equals(Object obj)
    {
        T t = this.get();
        return t != null ? t.equals(obj) : false;
    }
    
    @Override
    public String toString()
    {
        T t = this.get();
        
        if (t != null)
            return t.toString();
        else
            return "WeakReference " + super.toString() + " with null referent.";
    }
    
}

class EqualMappedWeakReference<T> extends EqualWeakReference<T>
{
    
    final Object key;
    
    public EqualMappedWeakReference(T referent, Object key, ReferenceQueue<? super T> q)
    {
        super(referent, q);
        this.key = key;
    }
    
    @Override
    public boolean equals(Object obj)
    {
        return super.equals(obj);
    }
    
    @Override
    public int hashCode()
    {
        return super.hashCode();
    }
    
}

class ReturnedEntry<K, V> implements Entry<K, V>
{
    
    K         key;
    V         value;
    Map<K, V> map;
    
    ReturnedEntry(K key, V value, Map<K, V> map)
    {
        this.key = key;
        this.value = value;
        this.map = map;
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
        this.map.put(this.key, value);
        this.value = value;
        return value;
    }
    
}
