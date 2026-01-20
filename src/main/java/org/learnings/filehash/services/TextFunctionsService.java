package org.learnings.filehash.services;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.learnings.filehash.model.Text;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TextFunctionsService {

    private static final String SECURITY_ALGORITHM = "SHA-256";
    // MessageDigest is probably a heavy-to-construct object, so we won't construct it inside getSentencesHashes().
    // Inside the method, every time we call the method, a new object is created.
    // As a static class field, it won't be thread safe, and we'll have wrong results, when multiple threads try to hash
    // As ThreadLocal, each thread has its own instance. So we create at most as many MessageDigest objects as threads,
    // and at the same time we ensure thread safety!
    private static final ThreadLocal<MessageDigest> MESSAGE_DIGEST = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance(SECURITY_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // Re-throw as unchecked exception
        }
    });

    public Map<Integer, String> extractSentencesHashes(Text text) {
        Map<Integer, Future<byte[]>> response = new HashMap<>();
        String[] sentences = text.getSentences();

        try (ExecutorService executorService = Executors.newFixedThreadPool(2)) {
            for (int i = 0; i < sentences.length; i++) {
                String sentence = sentences[i];
                response.put(i, executorService.submit(getDigest(sentence)));
            }
        }

        return getFutures(response);
    }

    private static @NonNull Callable<byte[]> getDigest(String sentence) {
        return () -> {
            // IMPORTANT if the MESSAGE_DIGEST.get() is called outside the callable, then it will be the same
            // came it will always come from the outside thread that is common, so same threadlocal.
            // It has to be called here, in the scope of each thread of the ExecutorService
            MessageDigest messageDigest = MESSAGE_DIGEST.get();
            log.debug("starting sentence digest computation, for sentence [{}]", sentence);
            log.debug("lets see if MESSAGE_DIGEST is same inside the thread [{}]: [{}]",
                    Thread.currentThread().getName(), messageDigest.hashCode());
            byte[] digest = messageDigest.digest(sentence.getBytes());
            log.debug("finished sentence digest computation");
            return digest;
        };
    }

    private static Map<Integer, String> getFutures(Map<Integer, Future<byte[]>> mapWithFutureValues) {
        return mapWithFutureValues
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        TextFunctionsService::getFuture));
    }

    private static String getFuture(Map.Entry<Integer, Future<byte[]>> e) {
        try {
            return new String(e.getValue().get());
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }
}
