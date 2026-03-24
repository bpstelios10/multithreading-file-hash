package org.learnings.filehash.services.mostcommonword;

import org.learnings.filehash.model.Text;
import org.learnings.filehash.services.WordFrequencyImprovedTask;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.LongAdder;

@Component
public class MostCommonWordWithRecursiveActionStrategy implements MostCommonWordStrategy {

    @Override
    public String findMostCommonWord(Text text) {
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

        return MostCommonWordUtils.logAndGetEntryKey(mostCommon);
    }

    @Override
    public StrategyType getType() {
        return StrategyType.RECURSIVE_ACTION;
    }
}
