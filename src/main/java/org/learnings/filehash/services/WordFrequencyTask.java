package org.learnings.filehash.services;

import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.RecursiveTask;

/*
 * This is a good example of using ForkJoin for map-reduce. but it comes with some cons for this problem:
 * 1 Creating many HashMaps
 * 2 Merging maps repeatedly (very expensive)
 * 3 Regex splitting (split("\\W+")) – surprisingly slow
 * 4 Lots of temporary objects
 */
public class WordFrequencyTask extends RecursiveTask<Map<String, Integer>> {

    private static final int THRESHOLD = 10;

    private final String[] sentences;
    private final int start;
    private final int end;

    public WordFrequencyTask(String[] sentences, int start, int end) {
        this.sentences = sentences;
        this.start = start;
        this.end = end;
    }

    @Override
    protected Map<String, Integer> compute() {
        if (ObjectUtils.isEmpty(sentences)) return new HashMap<>();
        if (end - start <= THRESHOLD) {
            return computeDirectly();
        }

        int mid = (start + end) / 2;
        WordFrequencyTask left = new WordFrequencyTask(sentences, start, mid);
        WordFrequencyTask right = new WordFrequencyTask(sentences, mid, end);

        left.fork();

        Map<String, Integer> rightResult = right.compute();
        Map<String, Integer> leftResult = left.join();

        return merge(leftResult, rightResult);
    }

    private Map<String, Integer> computeDirectly() {
        Map<String, Integer> freq = new HashMap<>();

        for (int i = start; i < end; i++) {
            String[] words = sentences[i]
                    .toLowerCase()
                    .split("\\W+");

            for (String w : words) {
                if (w.isBlank()) continue;

                freq.merge(w, 1, Integer::sum);
            }
        }

        return freq;
    }

    private static Map<String, Integer> merge(Map<String, Integer> a, Map<String, Integer> b) {
        Map<String, Integer> result = new HashMap<>(a);

        b.forEach((word, count) ->
                result.merge(word, count, Integer::sum));

        return result;
    }
}
