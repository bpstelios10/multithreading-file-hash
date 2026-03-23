package org.learnings.filehash.services;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

import static org.assertj.core.api.Assertions.assertThat;

class WordFrequencyTaskTest {
    private ForkJoinPool pool;

    @BeforeEach
    void setup() {
        // use a small pool so compute() runs
        pool = new ForkJoinPool(2);
    }

    @AfterEach
    void teardown() {
        pool.shutdown();
    }

    @Test
    @DisplayName("should count words directly when below threshold")
    void wordFrequencyTask_whenBelowThresholdAndCaseInsensitive_returnsCorrect() {
        String[] sentences = {
                "Hello world hello",
                "world Hello"
        };
        WordFrequencyTask task = new WordFrequencyTask(sentences, 0, sentences.length);

        Map<String, Integer> result = pool.invoke(task);

        assertThat(result.get("hello")).isEqualTo(3);
        assertThat(result.get("world")).isEqualTo(2);
        assertThat(result.containsKey("missing")).isFalse();
    }

    @Test
    @DisplayName("should correctly merge results when splitting ranges")
    void wordFrequencyTask_whenAboveThresholdAndTaskSplits_returnsCorrect() {
        // range forces a split (end - start > threshold)
        String[] sentences = {
                "foo bar",   // index 0
                "bar baz",   // index 1
                "baz foo",   // index 2
                "foo foo"    // index 3
        };
        WordFrequencyTask task = new WordFrequencyTask(sentences, 0, sentences.length);

        Map<String, Integer> result = pool.invoke(task);

        assertThat(result.get("foo")).isEqualTo(4);
        assertThat(result.get("bar")).isEqualTo(2);
        assertThat(result.get("baz")).isEqualTo(2);
    }

    @Test
    void wordFrequencyTask_whenEmptyText_returnsEmpty() {
        WordFrequencyTask task = new WordFrequencyTask(new String[0], 0, 0);
        Map<String, Integer> result = pool.invoke(task);

        assertThat(result).isEmpty();
    }

    @Test
    void wordFrequencyTask_whenNullText_returnsEmpty() {
        WordFrequencyTask task = new WordFrequencyTask(null, 0, 0);
        Map<String, Integer> result = pool.invoke(task);

        assertThat(result).isEmpty();
    }

    @Test
    void wordFrequencyTask_whenWordsWithPunctuationAndCaseInsensitive_returnsCorrect() {
        String[] sentences = {"Hello, hello! world? wor!d wor!ld wor?ld"};
        WordFrequencyTask task = new WordFrequencyTask(sentences, 0, 1);

        Map<String, Integer> result = pool.invoke(task);

        assertThat(result.get("hello")).isEqualTo(2);
        assertThat(result.get("world")).isEqualTo(1);
    }

    @Test
    void wordFrequencyTask_whenAboveThresholdAndLargeText_returnsCorrect() {
        try (ForkJoinPool bigPool = new ForkJoinPool(8)) {
            // create 40 sentences: "word1 word2 word3"
            List<String> sentences = new ArrayList<>();
            for (int i = 1; i <= 40; i++) {
                sentences.add("word" + (i % 5 + 1)); // generates word1...word5 repeatedly
            }

            String[] sentenceArray = sentences.toArray(new String[0]);
            WordFrequencyTask task = new WordFrequencyTask(sentenceArray, 0, sentenceArray.length);

            Map<String, Integer> result = bigPool.invoke(task);

            assertThat(result).hasSize(5);
            // word1...word5 should each appear 8 times (40 sentences / 5 words)
            for (int i = 1; i <= 5; i++) {
                assertThat(result.get("word" + i)).isEqualTo(8);
            }
        }
    }
}
