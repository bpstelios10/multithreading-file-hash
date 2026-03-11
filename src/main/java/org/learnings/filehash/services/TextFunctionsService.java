package org.learnings.filehash.services;

import lombok.extern.slf4j.Slf4j;
import org.learnings.filehash.model.Text;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

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

    public Map<Integer, Integer> countWords(Text text) {
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
