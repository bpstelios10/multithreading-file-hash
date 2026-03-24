package org.learnings.filehash.services.mostcommonword;

import org.junit.jupiter.api.Test;
import org.learnings.filehash.model.Text;

import static org.assertj.core.api.Assertions.assertThat;

class MostCommonWordWithRecursiveTaskStrategyTest {

    private final MostCommonWordWithRecursiveTaskStrategy strategy = new MostCommonWordWithRecursiveTaskStrategy();

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
    public void getMostCommonWord() {
        String mostCommonWord = strategy.findMostCommonWord(new Text(TEST_TEXT));

        assertThat(mostCommonWord).isEqualTo("of");
    }

    @Test
    public void getMostCommonWord_whenEmptyText() {
        String mostCommonWord = strategy.findMostCommonWord(new Text(""));

        assertThat(mostCommonWord).isNull();
    }
}
