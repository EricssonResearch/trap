package com.ericsson.research.trap.utils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public class LRUCache<K, V>
{
    
    public interface LRUDefault<V>
    {
        public V get();
    }
    
    protected LRUCache(long size)
    {
        maxCacheSize = size;
    }
    
    public static <K, V> LRUCache<K, V> createCache()
    {
        return new LRUCache<K, V>(25);
    }
    
    public static <K, V> LRUCache<K, V> createCache(long size)
    {
        return new LRUCache<K, V>(size);
    }
    
    ConcurrentHashMap<K, V>  cache        = new ConcurrentHashMap<K, V>();
    ConcurrentLinkedQueue<K> strongRefs   = new ConcurrentLinkedQueue<K>();
    private AtomicLong       numRefs      = new AtomicLong();
    private long             maxCacheSize = 25;
    
    AtomicLong numHits    = new AtomicLong(0);
    AtomicLong numQueries = new AtomicLong(1);
    
    public void put(K key, V value)
    {
        
        this.cache.put(key, value);
        this.strongRefs.add(key);
        long val = numRefs.incrementAndGet();
        
        while (val > maxCacheSize && numRefs.compareAndSet(val, val - 1))
        {
            K k1 = strongRefs.poll();
            cache.remove(k1);
            val = numRefs.get();
        }
    }
    
    public V get(K key)
    {
        numQueries.incrementAndGet();
        return this.cache.get(key);
    }
    
    long f = 0;
    
    public V get(K key, LRUDefault<V> defaultValue)
    {
        
        numQueries.incrementAndGet();
        V v = this.cache.get(key);
        if (v == null)
        {
            v = defaultValue.get();
            cache.put(key, v);
            this.strongRefs.add(key);
            long val = numRefs.incrementAndGet();
            
            while (val > maxCacheSize && numRefs.compareAndSet(val, val - 1))
            {
                K k1 = strongRefs.poll();
                cache.remove(k1);
                val = numRefs.get();
            }
        }
        else
        {
            numHits.incrementAndGet();
        }
        
        return v;
    }
    
    public String toString()
    {
        return "Cache: " + numRefs + ", " + (numHits.get() * 100 / numQueries.get()) + "%";
    }
    
}
