package org.learnings.cache;

import org.springframework.beans.factory.annotation.Qualifier;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public class AsyncTTLCache<K, V> {

    private final ConcurrentHashMap<K, Entry<V>> cache;
    private final long ttlMillis;
    private final Executor executor;

    public AsyncTTLCache(long ttlMillis, @Qualifier("cacheExecutor") Executor executor) {
        this.cache = new ConcurrentHashMap<>();
        this.ttlMillis = ttlMillis;
        this.executor = executor;
    }

    public CompletableFuture<V> get(K key, Supplier<V> supplier) {
        long now = System.currentTimeMillis();

        Entry<V> entry = cache.compute(key, (k, existing) -> {
            // cache will re-compute the value, even if there is an active computation happening
            // if we would want to block re-computes during active computations we'd add `&& existing.future.isDone()`
            if (existing == null ||
                    existing.expiresAt < now ||
                    existing.future.isCompletedExceptionally()) {
                CompletableFuture<V> future = CompletableFuture.supplyAsync(supplier, executor);

                return new Entry<>(future, now + ttlMillis);
            }
            return existing;
        });

        return entry.future;
    }

    private record Entry<V>(CompletableFuture<V> future, long expiresAt) { }
}
