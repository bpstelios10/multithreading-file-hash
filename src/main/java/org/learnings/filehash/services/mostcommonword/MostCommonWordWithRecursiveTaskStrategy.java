package org.learnings.filehash.services.mostcommonword;

import org.learnings.filehash.model.Text;
import org.learnings.filehash.services.WordFrequencyTask;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ForkJoinPool;

@Component
public class MostCommonWordWithRecursiveTaskStrategy implements MostCommonWordStrategy {

    @Override
    public String findMostCommonWord(Text text) {
        String[] sentences = text.getSentences();
        Map<String, Integer> frequencies;

        try (ForkJoinPool pool = new ForkJoinPool()) {
            WordFrequencyTask task = new WordFrequencyTask(sentences, 0, sentences.length);

            frequencies = pool.invoke(task);
        }

        Map.Entry<String, Integer> mostCommon =
                frequencies.entrySet()
                        .stream()
                        .max(Map.Entry.comparingByValue())
                        .orElse(null);

        return MostCommonWordUtils.logAndGetEntryKey(mostCommon);
    }

    @Override
    public StrategyType getType() {
        return StrategyType.RECURSIVE_TASK;
    }
}
