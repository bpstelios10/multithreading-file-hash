package org.learnings.filehash.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.learnings.filehash.model.Text;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WordCountServiceTest {

    private final WordCountService service = new WordCountService();

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

    @Test
    public void countWordsPerSentence() {
        Map<Integer, Integer> wordsPerSentence = service.countWordsPerSentence(new Text(TEST_TEXT));

        assertThat(wordsPerSentence).hasSize(4);
        assertThat(wordsPerSentence.get(0)).isEqualTo(21);
        assertThat(wordsPerSentence.get(1)).isEqualTo(13);
        assertThat(wordsPerSentence.get(2)).isEqualTo(7);
        assertThat(wordsPerSentence.get(3)).isEqualTo(40);
    }

    @Test
    public void countWordsPerSentence_whenEmptyText() {
        Map<Integer, Integer> wordsPerSentence = service.countWordsPerSentence(new Text(""));

        assertThat(wordsPerSentence).hasSize(1);
        assertThat(wordsPerSentence.get(0)).isEqualTo(0);
    }

    @Test
    public void countWords() {
        int worldsInText = service.countWords(new Text(TEST_TEXT));

        assertThat(worldsInText).isEqualTo(81);
    }

    @Test
    public void countWordsParallelStream() {
        int worldsInText = service.countWordsParallelStream(new Text(TEST_TEXT));

        assertThat(worldsInText).isEqualTo(81);
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    void countWordsInSentence_whenEmptyInputs_returnsZero(String sentence) {
        Integer result =
                ReflectionTestUtils.invokeMethod(WordCountService.class, "countWordsInSentence", sentence);

        assertThat(result).isEqualTo(0);
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
                ReflectionTestUtils.invokeMethod(WordCountService.class, "resolveFutures", map))
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
