package org.learnings.filehash.services;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.learnings.filehash.model.Text;
import org.learnings.filehash.services.mostcommonword.MostCommonWordService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.learnings.filehash.services.mostcommonword.MostCommonWordStrategy.StrategyType.*;
import static org.learnings.filehash.testutils.AssertionUtils.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TextFunctionsServiceTest {

    @Mock
    private WordCountService wordCountService;
    @Mock
    private MostCommonWordService mostCommonWordService;
    @Mock
    StringFrequencyPipeline stringFrequencyPipeline;
    @InjectMocks
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
                .anyMatch(l -> l.contains("-thread-1"))
                .anyMatch(l -> l.contains("-thread-2"));
        assertThat(threadsNamesAndMessageDigestInstancesLogs).hasSize(4);
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
    public void getFrequencyOf() {
        Text text = new Text(TEST_TEXT);
        String target = "Entire";
        when(stringFrequencyPipeline.execute(text.getSentences(), target)).thenReturn(1);

        int frequency = service.getFrequencyOf(text, target);

        assertThat(frequency).isEqualTo(1);
    }

    @ParameterizedTest
    @MethodSource("exceptionProvider")
    @SuppressWarnings("unchecked")
    void resolveFutures_whenThreadInterrupted_throwsRuntimeException(Throwable cause) throws Exception {
        Future<String> mockFuture = mock(Future.class);
        Map<String, Future<String>> map = new HashMap<>(1);
        map.put("should-interrupt-thread", mockFuture);
        when(mockFuture.get()).thenThrow(cause);

        assertThatThrownBy(() ->
                ReflectionTestUtils.invokeMethod(TextFunctionsService.class, "resolveFutures", map))
                .isInstanceOf(RuntimeException.class)
                .hasCause(cause)
                .hasMessage(cause.toString());
    }

    static Stream<Exception> exceptionProvider() {
        return Stream.of(
                new InterruptedException("Custom interruption of thread"),
                new ExecutionException(new RuntimeException("fail task with execution exception"))
        );
    }
}
