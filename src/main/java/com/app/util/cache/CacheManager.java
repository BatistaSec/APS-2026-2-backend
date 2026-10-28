package com.app.util.cache;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;


public class CacheManager {

    private static final CacheManager INSTANCE = new CacheManager();

    private final Map<String, CacheEntry> store = new ConcurrentHashMap<>();
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);

    public static CacheManager getInstance() {
        return INSTANCE;
    }

    public <T> void put(String key, T value, Duration ttl) {
        if (key == null || value == null) return;
        long expireAt = System.currentTimeMillis() + ttl.toMillis();
        store.put(key, new CacheEntry(value, expireAt));
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key) {
        if (key == null) return Optional.empty();

        CacheEntry entry = store.get(key);
        if (entry == null) {
            misses.incrementAndGet();
            return Optional.empty();
        }

        if (entry.isExpired()) {
            store.remove(key);
            misses.incrementAndGet();
            return Optional.empty();
        }

        hits.incrementAndGet();
        return Optional.of((T) entry.value());
    }

    public void invalidatePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) return;
        store.keySet().removeIf(k -> k.startsWith(prefix));
    }

    public void clear() {
        store.clear();
    }

    public long getHits() {
        return hits.get();
    }

    public long getMisses() {
        return misses.get();
    }

    public double getHitRate() {
        long total = hits.get() + misses.get();
        return total == 0 ? 0.0 : ((double) hits.get() / total) * 100.0;
    }

    public int size() {
        return store.size();
    }

    private record CacheEntry(Object value, long expireAt) {
        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
    }
}
