package org.learnings.filehash.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StringFrequencyPipelineTest {

    private final StringFrequencyPipeline pipeline = new StringFrequencyPipeline(createExecutor(4));

    @ParameterizedTest
    @ValueSource(strings = {"Two words", "Two"})
    void countOccurrences_succeeds(String target) {
        String[] input = {
                "Two words hello world",
                "Two words Two words test",
                "nothing here two words",
                "Two words appears again Two words"
        };

        int result = pipeline.execute(input, target);

        assertThat(result).isEqualTo(5);
    }

    @Test
    void countOccurrences_whenEmptySource_returnsZero() {
        String[] input = {};

        int result = pipeline.execute(input, "anything");

        assertThat(result).isEqualTo(0);
    }

    @Test
    void countOccurrences_whenNoMatch_returnsZero() {
        String[] input = {
                "hello world",
                "nothing here",
                "still nothing"
        };

        int result = pipeline.execute(input, "anything");

        assertThat(result).isEqualTo(0);
    }

    @Test
    void countOccurrences_whenSingleWorker_succeeds() {
        String[] input = {
                "X X X",
                "X"
        };

        int result = pipeline.execute(input, "X");

        assertThat(result).isEqualTo(4);
    }

    @Test
    void countOccurrences_whenLargeInput_succeeds() {
        StringFrequencyPipeline bigPipeline = new StringFrequencyPipeline(createExecutor(8));
        String[] input = new String[10_000];
        Arrays.fill(input, "X test X test");

        int result = bigPipeline.execute(input, "X");

        assertThat(result).isEqualTo(20_000);
    }

    // Test race conditions
    @Test
    void countOccurrences_whenMultipleExecutions_producesSameResults() {
        String[] input = {
                "X X",
                "X",
                "nothing",
                "X again X"
        };

        for (int i = 0; i < 20; i++) {
            int result = pipeline.execute(input, "X");

            assertThat(result).isEqualTo(5);
        }
    }

    @Test
    void countOccurrences_whenSubstrings_succeeds() {
        String[] input = {
                "abc test abc",
                "abcabc",
                "nothing"
        };

        int result = pipeline.execute(input, "abc");

        assertThat(result).isEqualTo(4);
    }

    @Test
    void countOccurrences_whenParallelExecutions_shouldBeThreadSafe() throws Exception {
        try (ExecutorService executor = Executors.newFixedThreadPool(10)) {
            List<Callable<Integer>> tasks =
                    IntStream
                            .range(0, 20)
                            .<Callable<Integer>>mapToObj(i -> () -> pipeline.execute(new String[]{"X X", "X"}, "X"))
                            .collect(Collectors.toList());

            List<Future<Integer>> results = executor.invokeAll(tasks);

            for (Future<Integer> result : results) {
                assertThat(result.get()).isEqualTo(3);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("exceptionProvider")
    @SuppressWarnings({"unchecked", "ResultOfMethodCallIgnored"})
    void stringFrequencyPipeline_whenThreadInterrupted_throwsRuntimeException(Throwable cause) throws Exception {
        ThreadPoolTaskExecutor faultyExecutor = mock(ThreadPoolTaskExecutor.class);
        StringFrequencyPipeline faultyPipeline = new StringFrequencyPipeline(faultyExecutor);
        Future<String> mockFuture = mock(Future.class);
        when(faultyExecutor.submit(any(Callable.class))).thenReturn(mockFuture);
        when(mockFuture.get()).thenThrow(cause);

        assertThatThrownBy(() -> faultyPipeline.execute(new String[0], "nada"))
                .isInstanceOf(RuntimeException.class)
                .hasCause(cause)
                .hasMessage(cause.toString());
        if (cause instanceof InterruptedException) {
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            Thread.interrupted();
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

    private ThreadPoolTaskExecutor createExecutor(int size) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(size);
        executor.setMaxPoolSize(size);
        executor.setQueueCapacity(100);

        executor.initialize();

        return executor;
    }

    static Stream<Exception> exceptionProvider() {
        return Stream.of(
                new InterruptedException("Custom interruption of thread"),
                new ExecutionException(new RuntimeException("fail task with execution exception"))
        );
    }
}
