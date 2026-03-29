package org.learnings.filehash.services;

import lombok.extern.slf4j.Slf4j;
import org.learnings.cache.AsyncTTLCache;
import org.learnings.filehash.model.Text;
import org.learnings.filehash.services.mostcommonword.MostCommonWordService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static org.learnings.filehash.services.mostcommonword.MostCommonWordStrategy.StrategyType.*;

@Slf4j
@Component
public class TextFunctionsService {

    private final WordCountService wordCountService;
    private final MostCommonWordService mostCommonWordService;
    private final StringFrequencyPipeline stringFrequencyPipeline;
    private final AsyncTTLCache<String, String> cache;

    public TextFunctionsService(WordCountService wordCountService,
                                MostCommonWordService mostCommonWordService,
                                StringFrequencyPipeline stringFrequencyPipeline,
                                Executor ttlCacheExecutor) {
        this.wordCountService = wordCountService;
        this.mostCommonWordService = mostCommonWordService;
        this.stringFrequencyPipeline = stringFrequencyPipeline;
        cache = new AsyncTTLCache<>(10_000_000, ttlCacheExecutor);
    }

    public Map<Integer, String> extractSentencesHashes(Text text) {
        String[] sentences = text.getSentences();
        Map<Integer, CompletableFuture<String>> hashFutures = new HashMap<>(sentences.length);

        for (int i = 0; i < sentences.length; i++) {
            String sentence = sentences[i];
            hashFutures.put(i, cache.get(sentence, () -> HashingUtils.hashSentence(sentence)));
        }

        return resolveFutures(hashFutures);
    }

    public Map<Integer, Integer> countWordsPerSentence(Text text) {
        return wordCountService.countWordsPerSentence(text);
    }

    public int countWords(Text text) {
        return wordCountService.countWords(text);
    }

    public int countWordsParallelStream(Text text) {
        return wordCountService.countWordsParallelStream(text);
    }

    public String getMostCommonWord(Text text) {
        return mostCommonWordService.getMostCommonWord(text, RECURSIVE_TASK);
    }

    public String getMostCommonWordImproved(Text text) {
        return mostCommonWordService.getMostCommonWord(text, RECURSIVE_ACTION);
    }

    public String getMostCommonWordParallelStream(Text text) {
        return mostCommonWordService.getMostCommonWord(text, PARALLEL_STREAM);
    }

    public CompletableFuture<Integer> countOccurrences(Text text, String target) {
        String[] sentences = text.getSentences();

        return stringFrequencyPipeline.execute(sentences, target);
    }

    /*
     * Wait for all Futures, so we block once, not N times (CompletableFuture.allOf method).
     *
     * In order to keep the initial futures untouched (with their exceptions, etc.), we copy all of them to
     * `safeFutures`, but if any of them fails, we mute it, so that we don't fail fast (default approach).
     *
     * Then, in the next steps we use again the original hashFutures with all of their states & metadata.
     */
    private static Map<Integer, String> resolveFutures(Map<Integer, CompletableFuture<String>> hashFutures) {
        CompletableFuture<?>[] safeFutures =
                hashFutures.values().stream()
                        .map(f -> f.exceptionally(ex -> null))
                        .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(safeFutures).join();

        return hashFutures.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue()
                                .exceptionally(ex -> {
                                    log.error("Hash failed: [{}]", ex.getMessage(), ex);
                                    return "ERROR DURING HASHING";
                                })
                                .join()
                ));
    }
}
