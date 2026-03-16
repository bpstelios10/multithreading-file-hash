package org.learnings.filehash.services;

import lombok.extern.slf4j.Slf4j;
import org.learnings.filehash.model.Text;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;

/*
 * most probably the parallelStream is the best performance option
 * it uses ForkJoinPool and work-stealing. Optimized for divide-and-conquer workloads
 * second best is ExecutorService + Future. low overhead and Good for CPU tasks or blocking IO
 * last is CompletableFuture. slightly more overhead. built for async composition, not raw speed
 *
 * If you have many tasks you should be using parallel (ForkJoin). uses work stealing and tasks are lighter than futures
 * if not that many, then maybe the overhead is big and should better use Futures.
 * ConcurrentHashMap can be used so that all threads safely use the same hashmap, to avoid different ones and then merge
 * LongAdder is used cause it uses one long per thread, to avoid the overhead of taking care of race conditions. in the
 * end we sum up all the values of each thread to get to total.
 */
@Slf4j
@Component
public class TextFunctionsService {

    public Map<Integer, String> extractSentencesHashes(Text text) {
        String[] sentences = text.getSentences();
        Map<Integer, Future<String>> hashFutures = new HashMap<>(sentences.length);

        try (ExecutorService executorService = Executors.newFixedThreadPool(2)) {
            for (int i = 0; i < sentences.length; i++) {
                String sentence = sentences[i];
                hashFutures.put(i, executorService.submit(() -> HashingUtils.hashSentence(sentence)));
            }
        }

        return resolveFutures(hashFutures);
    }

    public Map<Integer, Integer> countWordsPerSentence(Text text) {
        String[] sentences = text.getSentences();
        Map<Integer, CompletableFuture<Integer>> futures = new HashMap<>(sentences.length);

        for (int i = 0; i < sentences.length; i++) {
            String sentence = sentences[i];
            futures.put(i,
                    CompletableFuture.supplyAsync(() -> countWordsInSentence(sentence))
            );
        }

        return resolveFutures(futures);
    }

    public int countWords(Text text) {
        // using the existing method
//        Map<Integer, Integer> wordsPerSentence = countWordsPerSentence(text);
//        return wordsPerSentence.values().stream().mapToInt(Integer::intValue).sum();

        CompletableFuture<Integer> totalFuture;
        try (ExecutorService executor = Executors.newFixedThreadPool(8)) {
            String[] sentences = text.getSentences();
            List<CompletableFuture<Integer>> futures =
                    Arrays.stream(sentences)
                            .map(sentence ->
                                    CompletableFuture.supplyAsync(
                                            () -> countWordsInSentence(sentence),
                                            executor
                                    )
                            )
                            .toList();

            CompletableFuture<Void> all =
                    CompletableFuture.allOf(
                            futures.toArray(new CompletableFuture[0])
                    );

            totalFuture =
                    all.thenApply(v ->
                            futures.stream()
                                    .map(CompletableFuture::join)
                                    .reduce(0, Integer::sum)
                    );
        }

        return totalFuture.join();
    }

    public int countWordsParallelStream(Text text) {
        String[] sentences = text.getSentences();

        return Arrays.stream(sentences).parallel()
                .mapToInt(TextFunctionsService::countWordsInSentence)
                .sum();
    }

    public String getMostCommonWord(Text text) {
        String[] sentences = text.getSentences();
        Map.Entry<String, Integer> mostCommon;

        try (ForkJoinPool pool = new ForkJoinPool()) {
            WordFrequencyTask task =
                    new WordFrequencyTask(sentences, 0, sentences.length);

            Map<String, Integer> frequencies = pool.invoke(task);

            mostCommon =
                    frequencies.entrySet()
                            .stream()
                            .max(Map.Entry.comparingByValue())
                            .orElse(null);
            if (mostCommon != null)
                log.debug("most common word is [{}] with [{}] occurrences", mostCommon.getKey(), mostCommon.getValue());
        }

        return mostCommon == null ? null : mostCommon.getKey();
    }

    public String getMostCommonWordImproved(Text text) {
        String[] sentences = text.getSentences();
        ConcurrentHashMap<String, LongAdder> freq = new ConcurrentHashMap<>();

        try (ForkJoinPool pool = new ForkJoinPool()) {
            WordFrequencyImprovedTask task = new WordFrequencyImprovedTask(sentences, 0, sentences.length, freq);

            pool.invoke(task);
        }

        Map.Entry<String, LongAdder> mostCommon =
                freq.entrySet()
                        .stream()
                        .max(Comparator.comparingLong(e -> e.getValue().sum()))
                        .orElse(null);

        if (mostCommon != null)
            log.debug("most common word is [{}] with [{}] occurrences", mostCommon.getKey(), mostCommon.getValue());

        return mostCommon == null ? null : mostCommon.getKey();
    }

    public String getMostCommonWordParallelStream(Text text) {
        String[] sentences = text.getSentences();
        ConcurrentHashMap<String, LongAdder> freq = new ConcurrentHashMap<>();

        Arrays.stream(sentences).parallel().forEach(sentence -> {
            String[] words = sentence.toLowerCase().split("\\W+");

            for (String w : words) {
                if (w.isBlank()) continue;
                freq.computeIfAbsent(w, k -> new LongAdder()).increment();
            }
        });

        Map.Entry<String, LongAdder> mostCommon =
                freq.entrySet()
                        .stream()
                        .max(Comparator.comparingLong(e -> e.getValue().sum()))
                        .orElse(null);

        if (mostCommon != null)
            log.debug("most common word is [{}] with [{}] occurrences", mostCommon.getKey(), mostCommon.getValue());

        return mostCommon == null ? null : mostCommon.getKey();
    }

    private static <K, V> Map<K, V> resolveFutures(Map<K, ? extends Future<V>> futures) {
        Map<K, V> result = new HashMap<>(futures.size());

        for (Map.Entry<K, ? extends Future<V>> entry : futures.entrySet()) {
            try {
                result.put(entry.getKey(), entry.getValue().get());
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        return result;
    }

    private static int countWordsInSentence(String sentence) {
        if (sentence == null || sentence.isBlank()) {
            return 0;
        }

        return sentence.trim().split("\\s+").length;
    }
}
