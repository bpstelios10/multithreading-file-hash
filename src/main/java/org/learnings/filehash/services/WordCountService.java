package org.learnings.filehash.services;

import lombok.extern.slf4j.Slf4j;
import org.learnings.filehash.model.Text;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/*
 * most probably the parallelStream is the best performance option
 * it uses ForkJoinPool and work-stealing. Optimized for divide-and-conquer workloads
 *
 * second best is ExecutorService + Future. low overhead and Good for CPU tasks or blocking IO
 *
 * last is CompletableFuture. slightly more overhead. built for async composition, not raw speed
 */
@Slf4j
@Component
public class WordCountService {

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

            CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

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
                .mapToInt(WordCountService::countWordsInSentence)
                .sum();
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
