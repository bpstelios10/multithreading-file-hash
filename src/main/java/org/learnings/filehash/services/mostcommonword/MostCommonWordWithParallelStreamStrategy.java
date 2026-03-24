package org.learnings.filehash.services.mostcommonword;

import org.learnings.filehash.model.Text;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

@Component
public class MostCommonWordWithParallelStreamStrategy implements MostCommonWordStrategy {

    @Override
    public String findMostCommonWord(Text text) {
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

        return MostCommonWordUtils.logAndGetEntryKey(mostCommon);
    }

    @Override
    public StrategyType getType() {
        return StrategyType.PARALLEL_STREAM;
    }
}
