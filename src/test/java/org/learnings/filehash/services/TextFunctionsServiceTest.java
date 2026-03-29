package org.learnings.filehash.services;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.learnings.filehash.model.Text;
import org.learnings.filehash.services.mostcommonword.MostCommonWordService;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.learnings.filehash.services.mostcommonword.MostCommonWordStrategy.StrategyType.*;
import static org.learnings.testutils.AssertionUtils.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TextFunctionsServiceTest {

    @Mock
    private WordCountService wordCountService;
    @Mock
    private MostCommonWordService mostCommonWordService;
    @Mock
    StringFrequencyPipeline stringFrequencyPipeline;
    private static final ThreadPoolTaskExecutor executor = createExecutor(2);
    private TextFunctionsService service;
    private ListAppender<ILoggingEvent> textFunctionsServiceLogs;

    private static final String TEST_TEXT = """
            No man is an island,
            Entire of itself,
            Every man is a piece of the continent,
            A part of the main.
            If a clod be washed away by the sea,
            Europe is the less.
            As well as if a promontory were.
            As well as if a manor of thy friend’s
            Or of thine own were:
            Any man’s death diminishes me,
            Because I am involved in mankind,
            And therefore never send to know for whom the bell tolls;
            It tolls for thee.""";

    @BeforeEach
    void setUp() {
        textFunctionsServiceLogs = getListAppenderForClass(HashingUtils.class);
        service = new TextFunctionsService(wordCountService, mostCommonWordService, stringFrequencyPipeline, executor);
    }

    @AfterAll
    static void tearDown() {
        executor.shutdown();
    }

    @Test
    void extractSentencesHashes_returnsIndexedSentencesHashes() {
        Map<Integer, String> sentencesHashes = service.extractSentencesHashes(new Text(TEST_TEXT));

        System.out.println("---- results of tests ----");
        System.out.println(sentencesHashes);
        assertThat(sentencesHashes).hasSize(4);
        assertThat(sentencesHashes.get(0)).isEqualTo("1dfcea248091114a437e747de8a21e69a99e060b7768c033a4d5881ac73560da");
        assertThat(sentencesHashes.get(1)).isEqualTo("9cc4c653fdcb6f2eb0a03982675ec9187fc69687823e786fcb4088427dde1e2e");
        assertThat(sentencesHashes.get(2)).isEqualTo("ba3fe787b5f7466de1e0859ff85b70adbd216ab06c517623e6e03fd7a5fc2a64");
        assertThat(sentencesHashes.get(3)).isEqualTo("18088be579f248cd0d336f13a6b2dc0fd15b3b4d835529e1a6b3206e3fee7405");

        assertContainsInLogs(textFunctionsServiceLogs,
                "lets see if MESSAGE_DIGEST is same inside the thread",
                Level.DEBUG);
        // these next lines are a bit unorthodox, but... we can assert that only 2 threads were used
        // and using ThreadLocal only 2 MessageDigest instances were created, by creating a set of all the
        // thread names and MessageDigest hashes we logged in DEBUG
        Set<String> threadsNamesAndMessageDigestInstancesLogs =
                getThreadsNamesAndMessageDigestInstancesLogs(textFunctionsServiceLogs,
                        "lets see if MESSAGE_DIGEST is same inside the thread", Level.DEBUG);
        System.out.println("threadsNamesAndMessageDigestInstancesLogs: " + threadsNamesAndMessageDigestInstancesLogs);
        assertThat(threadsNamesAndMessageDigestInstancesLogs)
                .anyMatch(l -> l.equals("test-thread-pool-1"))
                .anyMatch(l -> l.equals("test-thread-pool-2"));
        assertThat(threadsNamesAndMessageDigestInstancesLogs).hasSize(4);
    }

    @Test
    void extractSentencesHashes_whenDuplicatedSentence_returnsCachedHash() {
        String textWithDuplications = """
                As well as if a promontory were1.
                As well as if a promontory were2.
                As well as if a promontory were.
                As well as if a promontory were.
                As well as if a promontory were3.
                """;
        Map<Integer, String> sentencesHashes = service.extractSentencesHashes(new Text(textWithDuplications));

        System.out.println("---- results of tests ----");
        System.out.println(sentencesHashes);
        assertThat(sentencesHashes).hasSize(5);
        assertThat(sentencesHashes.get(0)).isEqualTo("bebc5978fa1110cf6b869a2c9bb313d9f8c5be38887262f3645b8363736b972b");
        assertThat(sentencesHashes.get(1)).isEqualTo("de546c933dbbe212b8f66fc57981935f46023a96038147ec6cbd64d0bcb797d4");
        assertThat(sentencesHashes.get(2)).isEqualTo("ba3fe787b5f7466de1e0859ff85b70adbd216ab06c517623e6e03fd7a5fc2a64");
        assertThat(sentencesHashes.get(3)).isEqualTo("ba3fe787b5f7466de1e0859ff85b70adbd216ab06c517623e6e03fd7a5fc2a64");
        assertThat(sentencesHashes.get(4)).isEqualTo("e6cb7833d907b9a5f8712e9b12a7dbeda4420300a2bf141746a7e965e07a9266");

        // We should only see the log for hash computation 4 times, cause one sentence will be cached
        assertLogOccurrences(textFunctionsServiceLogs,
                "starting sentence digest computation",
                Level.DEBUG, 4);

        assertContainsInLogs(textFunctionsServiceLogs,
                "lets see if MESSAGE_DIGEST is same inside the thread",
                Level.DEBUG);
        // these next lines are a bit unorthodox, but... we can assert that only 2 threads were used
        // and using ThreadLocal only 2 MessageDigest instances were created, by creating a set of all the
        // thread names and MessageDigest hashes we logged in DEBUG
        // the number of threads is not affected by the number of threads for cache...
        Set<String> threadsNamesAndMessageDigestInstancesLogs =
                getThreadsNamesAndMessageDigestInstancesLogs(textFunctionsServiceLogs,
                        "lets see if MESSAGE_DIGEST is same inside the thread", Level.DEBUG);
        System.out.println("threadsNamesAndMessageDigestInstancesLogs: " + threadsNamesAndMessageDigestInstancesLogs);
        assertThat(threadsNamesAndMessageDigestInstancesLogs)
                .anyMatch(l -> l.equals("test-thread-pool-1"))
                .anyMatch(l -> l.equals("test-thread-pool-2"));
        assertThat(threadsNamesAndMessageDigestInstancesLogs).hasSize(4);
    }

    // For this test we use mockedStatic which only mocks on the local-thread for the test runner. so we need to
    // configure the thread-pool of the cache, so this test works (the HashingUtils will run on the cache threads)
    @Test
    void extractSentencesHashes_whenFutureFails_putsErrorMessageInHashingResults() {
        Executor sameThreadExecutor = Runnable::run;
        TextFunctionsService service2 = new TextFunctionsService(
                wordCountService, mostCommonWordService, stringFrequencyPipeline, sameThreadExecutor);

        try (MockedStatic<HashingUtils> mock = Mockito.mockStatic(HashingUtils.class)) {
            mock.when(() -> HashingUtils.hashSentence(anyString())).thenReturn("hash-hello");
            mock.when(() -> HashingUtils.hashSentence(contains("promontory"))).thenThrow(new RuntimeException("failure"));

            Map<Integer, String> result = service2.extractSentencesHashes(new Text(TEST_TEXT));

            assertThat(result.get(0)).isEqualTo("hash-hello");
            assertThat(result.get(1)).isEqualTo("hash-hello");
            assertThat(result.get(2)).isEqualTo("ERROR DURING HASHING");
            assertThat(result.get(3)).isEqualTo("hash-hello");
        }
    }

    @Test
    public void countWordsPerSentence() {
        Map<Integer, Integer> expectedResult = new HashMap<>();
        Text text = new Text(TEST_TEXT);
        when(wordCountService.countWordsPerSentence(text)).thenReturn(expectedResult);

        Map<Integer, Integer> result = service.countWordsPerSentence(text);

        assertThat(expectedResult).isEqualTo(result);
        verifyNoMoreInteractions(wordCountService, mostCommonWordService, stringFrequencyPipeline);
    }

    @Test
    public void countWords() {
        Text text = new Text(TEST_TEXT);
        when(wordCountService.countWords(text)).thenReturn(81);

        int result = service.countWords(text);

        assertThat(81).isEqualTo(result);
        verifyNoMoreInteractions(wordCountService, mostCommonWordService, stringFrequencyPipeline);
    }

    @Test
    public void countWordsParallelStream() {
        Text text = new Text(TEST_TEXT);
        when(wordCountService.countWordsParallelStream(text)).thenReturn(81);

        int result = service.countWordsParallelStream(text);

        assertThat(81).isEqualTo(result);
        verifyNoMoreInteractions(wordCountService, mostCommonWordService, stringFrequencyPipeline);
    }

    @Test
    public void getMostCommonWord() {
        Text text = new Text(TEST_TEXT);
        when(mostCommonWordService.getMostCommonWord(text, RECURSIVE_TASK)).thenReturn("of");

        String result = service.getMostCommonWord(text);

        assertThat("of").isEqualTo(result);
        verifyNoMoreInteractions(wordCountService, mostCommonWordService, stringFrequencyPipeline);
    }

    @Test
    public void getMostCommonWordImproved() {
        Text text = new Text(TEST_TEXT);
        when(mostCommonWordService.getMostCommonWord(text, RECURSIVE_ACTION)).thenReturn("of");

        String result = service.getMostCommonWordImproved(text);

        assertThat("of").isEqualTo(result);
        verifyNoMoreInteractions(wordCountService, mostCommonWordService, stringFrequencyPipeline);
    }

    @Test
    public void getMostCommonWordParallelStream() {
        Text text = new Text(TEST_TEXT);
        when(mostCommonWordService.getMostCommonWord(text, PARALLEL_STREAM)).thenReturn("of");

        String result = service.getMostCommonWordParallelStream(text);

        assertThat("of").isEqualTo(result);
        verifyNoMoreInteractions(wordCountService, mostCommonWordService, stringFrequencyPipeline);
    }

    @Test
    public void countOccurrences() {
        Text text = new Text(TEST_TEXT);
        String target = "Entire";
        CompletableFuture<Integer> future = new CompletableFuture<>();
        future.complete(1);
        when(stringFrequencyPipeline.execute(text.getSentences(), target)).thenReturn(future);

        CompletableFuture<Integer> frequency = service.countOccurrences(text, target);

        assertThat(frequency).isNotCompletedExceptionally();
        assertThat(frequency.join()).isEqualTo(1);
    }

    @SuppressWarnings("SameParameterValue")
    private static ThreadPoolTaskExecutor createExecutor(int size) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(size);
        executor.setMaxPoolSize(size);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("test-thread-pool-");

        executor.initialize();

        return executor;
    }
}
