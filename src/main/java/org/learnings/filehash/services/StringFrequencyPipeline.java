package org.learnings.filehash.services;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * This is a parallel processing pipeline using BlockingQueue
 * 1) the producer populates the sentences into one queue
 * 2) the workers read from this queue, find the number of occurrences of a string
 * and put the result into a results BlockingQueue
 * 3) the aggregator reads from the results queue, sums up and returns the total
 * <br>
 * We also use the return-completable-future pattern to keep the pipeline non-blocking
 */
@Component
public class StringFrequencyPipeline {
    private static final String WORKER_POISON_PILL = "__EOF__";
    private static final int AGGREGATOR_POISON_PILL = -1;

    private final ThreadPoolTaskExecutor executor;
    private final int workers;

    public StringFrequencyPipeline(ThreadPoolTaskExecutor stringFrequencyPipelineExecutor) {
        this.executor = stringFrequencyPipelineExecutor;
        // we need 1 thread for producer and 1 for aggregator. the rest can be workers
        workers = executor.getMaxPoolSize() - 2;
    }

    public CompletableFuture<Integer> execute(String[] sentences, String target) {
        BlockingQueue<String> sentenceQueue = new LinkedBlockingQueue<>(100);
        BlockingQueue<Integer> resultQueue = new LinkedBlockingQueue<>(100);
        CompletableFuture<Integer> resultFuture = new CompletableFuture<>();
        executor.submit(createProducer(sentences, sentenceQueue));

        for (int i = 0; i < workers; i++) {
            executor.submit(createWorker(sentenceQueue, resultQueue, target));
        }

        executor.submit(createAggregator(resultQueue, resultFuture));

        return resultFuture;
    }

    private Runnable createProducer(String[] sentences, BlockingQueue<String> queue) {
        return () -> {
            try {
                for (String sentence : sentences) {
                    queue.put(sentence);
                }

                for (int i = 0; i < workers; i++) {
                    queue.put(WORKER_POISON_PILL);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
    }

    private Runnable createWorker(BlockingQueue<String> sentenceQueue, BlockingQueue<Integer> resultQueue, String target) {
        return () -> {
            try {
                while (true) {
                    String sentence = sentenceQueue.take();

                    if (sentence.equals(WORKER_POISON_PILL)) {
                        resultQueue.put(AGGREGATOR_POISON_PILL);
                        break;
                    }

                    int count = countOccurrences(sentence, target);

                    resultQueue.put(count);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
    }

    private Runnable createAggregator(BlockingQueue<Integer> queue, CompletableFuture<Integer> resultFuture) {
        return () -> {
            int total = 0;
            int finishedWorkers = 0;

            try {
                while (finishedWorkers < workers) {
                    int value = queue.take();

                    if (value == AGGREGATOR_POISON_PILL) {
                        finishedWorkers++;
                        continue;
                    }

                    total += value;
                }

                resultFuture.complete(total);
            } catch (InterruptedException e) {
                resultFuture.completeExceptionally(e);
                Thread.currentThread().interrupt();
            }
        };
    }

    private static int countOccurrences(String text, String target) {
        int count = 0;
        int index = 0;

        while ((index = text.indexOf(target, index)) != -1) {
            count++;
            index += target.length();
        }

        return count;
    }
}
