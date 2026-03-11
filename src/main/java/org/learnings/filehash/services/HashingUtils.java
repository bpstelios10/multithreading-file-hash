package org.learnings.filehash.services;

import lombok.extern.slf4j.Slf4j;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
public final class HashingUtils {

    private HashingUtils() {}

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

    public static String hashSentence(String sentence) {
        // IMPORTANT if the MESSAGE_DIGEST.get() is called outside the callable, then it will be the same
        // came it will always come from the outside thread that is common, so same threadlocal.
        // It has to be called here, in the scope of each thread of the ExecutorService
        MessageDigest messageDigest = MESSAGE_DIGEST.get();
        log.debug("starting sentence digest computation, for sentence [{}]", sentence);
        log.debug("lets see if MESSAGE_DIGEST is same inside the thread [{}]: [{}]",
                Thread.currentThread().getName(), messageDigest.hashCode());
        messageDigest.reset(); // defensive but ok

        byte[] hash = messageDigest.digest(sentence.getBytes(UTF_8));
        log.debug("finished sentence digest computation");
        return HexFormat.of().formatHex(hash);
    }
}
