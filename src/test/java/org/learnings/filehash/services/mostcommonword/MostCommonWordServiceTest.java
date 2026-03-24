package org.learnings.filehash.services.mostcommonword;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.learnings.filehash.model.Text;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.learnings.filehash.services.mostcommonword.MostCommonWordStrategy.StrategyType.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MostCommonWordServiceTest {

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

    @Mock
    private MostCommonWordWithRecursiveActionStrategy mostCommonWordWithRecursiveActionStrategy;
    @Mock
    private MostCommonWordWithRecursiveTaskStrategy mostCommonWordWithRecursiveTaskStrategy;
    @Mock
    private MostCommonWordWithParallelStreamStrategy mostCommonWordWithParallelStreamStrategy;
    private MostCommonWordService service;

    @BeforeEach
    void setUp() {
        when(mostCommonWordWithRecursiveActionStrategy.getType()).thenReturn(RECURSIVE_ACTION);
        when(mostCommonWordWithRecursiveTaskStrategy.getType()).thenReturn(RECURSIVE_TASK);
        when(mostCommonWordWithParallelStreamStrategy.getType()).thenReturn(PARALLEL_STREAM);

        service = new MostCommonWordService(List.of(mostCommonWordWithRecursiveActionStrategy,
                mostCommonWordWithRecursiveTaskStrategy, mostCommonWordWithParallelStreamStrategy));
    }

    @Test
    public void getMostCommonWord_whenWrongStrategyName_throwsException() {
        when(mostCommonWordWithRecursiveActionStrategy.getType()).thenReturn(null);
        MostCommonWordService faultyStrategy =
                new MostCommonWordService(List.of(mostCommonWordWithRecursiveActionStrategy));

        assertThatThrownBy(() -> faultyStrategy.getMostCommonWord(new Text(""), RECURSIVE_ACTION))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown strategy: [%s]", RECURSIVE_ACTION);
    }

    @Test
    public void getMostCommonWord() {
        Text text = new Text(TEST_TEXT);
        when(mostCommonWordWithRecursiveActionStrategy.findMostCommonWord(text)).thenReturn("of");

        String mostCommonWord = service.getMostCommonWord(text, RECURSIVE_ACTION);

        assertThat(mostCommonWord).isEqualTo("of");
    }

    @Test
    public void getMostCommonWordImproved() {
        Text text = new Text(TEST_TEXT);
        when(mostCommonWordWithRecursiveTaskStrategy.findMostCommonWord(text)).thenReturn("of");

        String mostCommonWord = service.getMostCommonWord(text, RECURSIVE_TASK);

        assertThat(mostCommonWord).isEqualTo("of");
    }

    @Test
    public void getMostCommonWordParallelStream() {
        Text text = new Text(TEST_TEXT);
        when(mostCommonWordWithParallelStreamStrategy.findMostCommonWord(text)).thenReturn("of");

        String mostCommonWord = service.getMostCommonWord(text, PARALLEL_STREAM);

        assertThat(mostCommonWord).isEqualTo("of");
    }
}
