package org.learnings.filehash.services.mostcommonword;

import lombok.extern.slf4j.Slf4j;
import org.learnings.filehash.model.Text;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
 * If you have many tasks you should be using parallel (ForkJoin). uses work stealing and tasks are lighter than futures
 * if not that many, then maybe the overhead is big and should better use Futures.
 * ConcurrentHashMap can be used so that all threads safely use the same hashmap, to avoid different ones and then merge
 * LongAdder is used cause it uses one long per thread, to avoid the overhead of taking care of race conditions. in the
 * end we sum up all the values of each thread to get to total.
 */
@Slf4j
@Component
public class MostCommonWordService {

    private final Map<MostCommonWordStrategy.StrategyType, MostCommonWordStrategy> strategies;

    public MostCommonWordService(List<MostCommonWordStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(MostCommonWordStrategy::getType, s -> s));
    }

    public String getMostCommonWord(Text text, MostCommonWordStrategy.StrategyType strategyType) {
        MostCommonWordStrategy strategy = strategies.get(strategyType);

        if (strategy == null) {
            throw new IllegalArgumentException("Unknown strategy: [" + strategyType + "]");
        }

        return strategy.findMostCommonWord(text);
    }
}
