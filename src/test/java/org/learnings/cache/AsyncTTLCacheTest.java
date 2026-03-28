package org.learnings.cache;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static java.lang.Thread.sleep;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AsyncTTLCacheTest {

    private final int ttlMillis = 100;
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);
    private AsyncTTLCache<Integer, Integer> cache;

    @BeforeEach
    void setUp() {
        cache = new AsyncTTLCache<>(ttlMillis, executor);
    }

    @AfterAll
    static void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void get_whenEmptyCache_computesValue() {
        AtomicInteger counter = new AtomicInteger();

        // each different key should be getting the value of previous key, plus 1
        List<Integer> results = IntStream
                .range(0, 5)
                .mapToObj(i -> cache.get(i, counter::incrementAndGet).join())
                .toList();

        // assert the correct values AND that getting the value from cache again, gives the cached value
        for (int i = 0; i < results.size(); i++) {
            Integer result = results.get(i);
            assertThat(i + 1).isEqualTo(result);
            assertThat(i + 1).isEqualTo(cache.get(i, counter::incrementAndGet).join());
        }
        // assert that the counter only changed 5 times. no more computations!
        assertThat(counter.get()).isEqualTo(5);
    }

    @Test
    void get_whenConcurrentRequestsForSameKey_returnsSameFuture() {
        AtomicInteger counter = new AtomicInteger();
        Supplier<Integer> slowSupplier = () -> {
            try {
                sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return counter.incrementAndGet();
        };

        CompletableFuture<Integer> f1 = cache.get(1, slowSupplier);
        CompletableFuture<Integer> f2 = cache.get(1, slowSupplier);

        assertThat(f1).isSameAs(f2);
    }

    @Test
    void get_whenMultipleThreadsRequestSameKey_returnsSameValue() {
        AtomicInteger counter = new AtomicInteger(3);

        // same key should be getting always the same time (assuming TTL not reached)
        try (ExecutorService executor = Executors.newFixedThreadPool(10)) {
            List<CompletableFuture<Integer>> tasks = IntStream
                    .range(0, 5)
                    .mapToObj(i -> CompletableFuture.supplyAsync(() -> cache.get(1, counter::incrementAndGet).join(), executor))
                    .toList();

            for (CompletableFuture<Integer> task : tasks) {
                assertThat(4).isEqualTo(task.join());
            }
        }
        // assert that the counter only changed 2 time. no more computations!
        assertThat(counter.get()).isEqualTo(4);
    }

    @Test
    void get_whenTtlReached_computesAgain() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(5);
        Integer firstValue = cache.get(1, counter::incrementAndGet).join();

        assertThat(6).isEqualTo(firstValue);
        assertThat(6).isEqualTo(cache.get(1, counter::incrementAndGet).join());
        sleep(ttlMillis + 50);
        assertThat(7).isEqualTo(cache.get(1, counter::incrementAndGet).join());
        // assert that the counter only changed 2 times. no more computations!
        assertThat(counter.get()).isEqualTo(7);
    }

    @Test
    void get_whenTtlExpirationWhileComputation_recomputesImmediately() {
        AtomicInteger counter = new AtomicInteger();
        Supplier<Integer> slowSupplier = () -> {
            try {
                sleep(ttlMillis + 50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return counter.incrementAndGet();
        };

        CompletableFuture<Integer> f1 = cache.get(1, slowSupplier);
        // recompute before TTL expires
        CompletableFuture<Integer> f2 = cache.get(1, slowSupplier);

        try {
            Thread.sleep(ttlMillis + 30); // TTL expires while computing
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // recompute after TTL expires
        CompletableFuture<Integer> f3 = cache.get(1, slowSupplier);

        assertThat(f1).isSameAs(f2);
        assertThat(f1).isNotSameAs(f3);
        assertThat(f1.join()).isEqualTo(1);
        assertThat(f2.join()).isEqualTo(1);
        assertThat(f3.join()).isEqualTo(2);
        assertThat(counter.get()).isEqualTo(2);
    }

    @Test
    void get_whenSupplierFails_cacheDoesNotPoisonFuture() {
        // first call to cache 'pollutes' it
        Supplier<Integer> failing = () -> {
            throw new RuntimeException("fail the task");
        };

        CompletableFuture<Integer> failingCompletableFuture = cache.get(1, failing);
//        assertThat(failingCompletableFuture.isDone()).isFalse();
        assertThatThrownBy(failingCompletableFuture::join)
                .isInstanceOf(CompletionException.class)
                .hasMessage("java.lang.RuntimeException: fail the task");

        // but then the future interactions with the same cache are fine!
        AtomicInteger counter = new AtomicInteger();
        Integer result = cache.get(1, counter::incrementAndGet).join();

        assertThat(result).isEqualTo(1);
    }
}