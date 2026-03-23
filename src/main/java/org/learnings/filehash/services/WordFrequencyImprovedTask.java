package org.learnings.filehash.services;

import org.springframework.util.ObjectUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.LongAdder;

/*
 * This is a good example of using ForkJoin for map-reduce. but it comes with some cons for this problem:
 * 1 Creating many HashMaps - SOLVED with ConcurrentHashMap
 * 2 Merging maps repeatedly (very expensive) - SOLVED with ConcurrentHashMap
 * 3 Regex splitting (split("\\W+")) – surprisingly slow
 * 4 Lots of temporary objects
 */
public class WordFrequencyImprovedTask extends RecursiveAction {
    private static final int THRESHOLD = 10;

    private final String[] sentences;
    private final int start;
    private final int end;
    private final ConcurrentHashMap<String, LongAdder> freq;

    public WordFrequencyImprovedTask(String[] sentences, int start, int end, ConcurrentHashMap<String, LongAdder> freq) {
        this.sentences = sentences;
        this.start = start;
        this.end = end;
        this.freq = freq;
    }

    @Override
    protected void compute() {
        if (ObjectUtils.isEmpty(sentences)) return;
        if (end - start <= THRESHOLD) {
            computeDirectly();
            return;
        }

        int mid = (start + end) / 2;
        WordFrequencyImprovedTask left = new WordFrequencyImprovedTask(sentences, start, mid, freq);
        WordFrequencyImprovedTask right = new WordFrequencyImprovedTask(sentences, mid, end, freq);

        invokeAll(left, right);
    }

    private void computeDirectly() {
        for (int i = start; i < end; i++) {
            String[] words = sentences[i]
                    .toLowerCase()
                    .split("\\W+");

            for (String w : words) {
                if (w.isBlank()) continue;

                freq.computeIfAbsent(w, k -> new LongAdder())
                        .increment();
            }
        }
    }
}
