package org.learnings.filehash.services.mostcommonword;

import org.learnings.filehash.model.Text;

public interface MostCommonWordStrategy {

    String findMostCommonWord(Text text);

    enum StrategyType {
        RECURSIVE_ACTION,
        RECURSIVE_TASK,
        PARALLEL_STREAM
    }

    StrategyType getType();
}
