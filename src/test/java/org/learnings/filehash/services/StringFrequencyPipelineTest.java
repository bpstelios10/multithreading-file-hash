package org.learnings.filehash.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StringFrequencyPipelineTest {

    private final StringFrequencyPipeline pipeline = new StringFrequencyPipeline(createExecutor(4));

    @ParameterizedTest
    @ValueSource(strings = {"Two words", "Two"})
    void countOccurrences_succeeds(String target) {
        String[] input = {"Two words hello world", "Two words Two words test",
                "nothing here two words", "Two words appears again Two words"};

        CompletableFuture<Integer> result = pipeline.execute(input, target);

        assertThat(result).isNotCompletedExceptionally();
        assertThat(result.join()).isEqualTo(5);
    }

    @Test
    void countOccurrences_whenEmptySource_returnsZero() {
        String[] input = {};

        CompletableFuture<Integer> result = pipeline.execute(input, "anything");

        assertThat(result).isNotCompletedExceptionally();
        assertThat(result.join()).isEqualTo(0);
    }

    @Test
    void countOccurrences_whenNoMatch_returnsZero() {
        String[] input = {"hello world", "nothing here", "still nothing"};

        CompletableFuture<Integer> result = pipeline.execute(input, "anything");

        assertThat(result).isNotCompletedExceptionally();
        assertThat(result.join()).isEqualTo(0);
    }

    @Test
    void countOccurrences_whenSingleWorker_succeeds() {
        String[] input = {"X X X", "X"};

        CompletableFuture<Integer> result = pipeline.execute(input, "X");

        assertThat(result).isNotCompletedExceptionally();
        assertThat(result.join()).isEqualTo(4);
    }

    @Test
    void countOccurrences_whenLargeInput_succeeds() {
        StringFrequencyPipeline bigPipeline = new StringFrequencyPipeline(createExecutor(8));
        String[] input = new String[10_000];
        Arrays.fill(input, "X test X test");

        CompletableFuture<Integer> result = bigPipeline.execute(input, "X");

        assertThat(result).isNotCompletedExceptionally();
        assertThat(result.join()).isEqualTo(20_000);
    }

    // Test race conditions
    @Test
    void countOccurrences_whenMultipleExecutions_producesSameResults() {
        String[] input = {"X X", "X", "nothing", "X again X"};

        for (int i = 0; i < 20; i++) {
            CompletableFuture<Integer> result = pipeline.execute(input, "X");

            assertThat(result).isNotCompletedExceptionally();
            assertThat(result.join()).isEqualTo(5);
        }
    }

    @Test
    void countOccurrences_whenSubstrings_succeeds() {
        String[] input = {"abc test abc", "abcabc", "nothing"};

        CompletableFuture<Integer> result = pipeline.execute(input, "abc");

        assertThat(result).isNotCompletedExceptionally();
        assertThat(result.join()).isEqualTo(4);
    }

    @Test
    void countOccurrences_whenParallelExecutions_shouldBeThreadSafe() throws Exception {
        try (ExecutorService executor = Executors.newFixedThreadPool(10)) {
            List<Callable<Integer>> tasks =
                    IntStream
                            .range(0, 20)
                            .<Callable<Integer>>mapToObj(i -> () -> pipeline.execute(new String[]{"X X", "X"}, "X").join())
                            .toList();

            List<Future<Integer>> results = executor.invokeAll(tasks);

            for (Future<Integer> result : results) {
                assertThat(result.get()).isEqualTo(3);
            }
        }
    }

    @Test
    @SuppressWarnings({"unchecked", "ResultOfMethodCallIgnored"})
    void createProducer_whenThreadInterrupted_throwsRuntimeException() throws Exception {
        BlockingQueue<String> faultyQueue = mock(BlockingQueue.class);
        Throwable cause = new InterruptedException("Custom interruption of thread");
        String[] s = {"fail!"};
        doThrow(cause).when(faultyQueue).put(s[0]);

        Runnable producer = ReflectionTestUtils.invokeMethod(pipeline, "createProducer", s, faultyQueue);
        assertThat(producer).isNotNull();
        producer.run();

        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        Thread.interrupted();
    }

    @Test
    @SuppressWarnings({"unchecked", "ResultOfMethodCallIgnored"})
    void createWorker_whenThreadInterrupted_throwsRuntimeException() throws Exception {
        BlockingQueue<String> faultyQueue = mock(BlockingQueue.class);
        Throwable cause = new InterruptedException("Custom interruption of thread");
        doThrow(cause).when(faultyQueue).take();

        Runnable worker = ReflectionTestUtils.invokeMethod(
                pipeline, "createWorker",
                faultyQueue, new LinkedBlockingQueue<Integer>(1), "target");
        assertThat(worker).isNotNull();
        worker.run();

        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        Thread.interrupted();
    }

    @Test
    @SuppressWarnings({"unchecked", "ResultOfMethodCallIgnored"})
    void createAggregator_whenThreadInterrupted_throwsRuntimeException() throws Exception {
        CompletableFuture<Integer> mockFuture = mock(CompletableFuture.class);
        BlockingQueue<String> faultyQueue = mock(BlockingQueue.class);
        Throwable cause = new InterruptedException("Custom interruption of thread");
        doThrow(cause).when(faultyQueue).take();

        Runnable aggregatorResult = ReflectionTestUtils.invokeMethod(
                pipeline, "createAggregator", faultyQueue, mockFuture);
        assertThat(aggregatorResult).isNotNull();
        aggregatorResult.run();

        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        Thread.interrupted();
        verify(mockFuture, times(0)).complete(any());
        verify(mockFuture).completeExceptionally(cause);
        verify(faultyQueue).take();
        verifyNoMoreInteractions(mockFuture, faultyQueue);
    }

    private ThreadPoolTaskExecutor createExecutor(int size) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(size);
        executor.setMaxPoolSize(size);
        executor.setQueueCapacity(100);

        executor.initialize();

        return executor;
    }
}
