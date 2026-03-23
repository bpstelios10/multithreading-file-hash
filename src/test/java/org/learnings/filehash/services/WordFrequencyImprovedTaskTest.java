package org.learnings.filehash.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.LongAdder;

import static org.assertj.core.api.Assertions.assertThat;

class WordFrequencyImprovedTaskTest {

    private ForkJoinPool pool;

    @BeforeEach
    void setup() {
        pool = new ForkJoinPool(2);
    }

    @AfterEach
    void teardown() {
        pool.shutdown();
    }

    @Test
    void wordFrequencyImprovedTask_whenBelowThresholdAndCaseInsensitive_returnsCorrect() {
        String[] sentences = {
                "Hello world hello",
                "world Hello"
        };
        ConcurrentHashMap<String, LongAdder> freq = new ConcurrentHashMap<>();
        WordFrequencyImprovedTask task =
                new WordFrequencyImprovedTask(sentences, 0, sentences.length, freq);

        pool.invoke(task);

        assertThat(freq.get("hello").intValue()).isEqualTo(3);
        assertThat(freq.get("world").intValue()).isEqualTo(2);
    }

    @Test
    void wordFrequencyImprovedTask_whenEmptyText_returnsEmpty() {
        String[] sentences = new String[0];
        ConcurrentHashMap<String, LongAdder> freq = new ConcurrentHashMap<>();
        WordFrequencyImprovedTask task =
                new WordFrequencyImprovedTask(sentences, 0, sentences.length, freq);

        pool.invoke(task);

        assertThat(freq).isEmpty();
    }

    @Test
    void wordFrequencyImprovedTask_whenNullText_returnsEmpty() {
        String[] sentences = new String[0];
        ConcurrentHashMap<String, LongAdder> freq = new ConcurrentHashMap<>();
        WordFrequencyImprovedTask task =
                new WordFrequencyImprovedTask(sentences, 0, sentences.length, freq);

        pool.invoke(task);

        assertThat(freq).isEmpty();
    }

    @Test
    void wordFrequencyImprovedTask_whenWordsWithPunctuationAndCaseInsensitive_returnsCorrect() {
        String[] sentences = {"Hello, hello! world? he!lo he??o"};
        ConcurrentHashMap<String, LongAdder> freq = new ConcurrentHashMap<>();
        WordFrequencyImprovedTask task =
                new WordFrequencyImprovedTask(sentences, 0, 1, freq);

        pool.invoke(task);

        assertThat(freq.get("hello").intValue()).isEqualTo(2);
        assertThat(freq.get("world").intValue()).isEqualTo(1);
    }

    @Test
    void wordFrequencyImprovedTask_whenAboveThresholdAndLargeText_returnsCorrect() {
        try (ForkJoinPool bigPool = new ForkJoinPool(8)) {
            List<String> sentences = new ArrayList<>();
            for (int i = 1; i <= 40; i++) {
                // cycle through 5 different words
                sentences.add("word" + (i % 5 + 1));
            }
            String[] sentenceArray = sentences.toArray(new String[0]);

            ConcurrentHashMap<String, LongAdder> freq = new ConcurrentHashMap<>();
            WordFrequencyImprovedTask task =
                    new WordFrequencyImprovedTask(sentenceArray, 0, sentenceArray.length, freq);

            bigPool.invoke(task);

            assertThat(freq).hasSize(5);
            // each word "word1"..."word5" should appear 8 times
            for (int i = 1; i <= 5; i++) {
                assertThat(freq.get("word" + i).intValue()).isEqualTo(8);
            }
        }
    }
}
