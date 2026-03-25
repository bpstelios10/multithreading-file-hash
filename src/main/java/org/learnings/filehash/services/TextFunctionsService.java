package org.learnings.filehash.services;

import lombok.extern.slf4j.Slf4j;
import org.learnings.filehash.model.Text;
import org.learnings.filehash.services.mostcommonword.MostCommonWordService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

import static org.learnings.filehash.services.mostcommonword.MostCommonWordStrategy.StrategyType.*;

@Slf4j
@Component
public class TextFunctionsService {

    private final WordCountService wordCountService;
    private final MostCommonWordService mostCommonWordService;
    private final StringFrequencyPipeline stringFrequencyPipeline;

    public TextFunctionsService(WordCountService wordCountService,
                                MostCommonWordService mostCommonWordService,
                                StringFrequencyPipeline stringFrequencyPipeline) {
        this.wordCountService = wordCountService;
        this.mostCommonWordService = mostCommonWordService;
        this.stringFrequencyPipeline = stringFrequencyPipeline;
    }

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
}
