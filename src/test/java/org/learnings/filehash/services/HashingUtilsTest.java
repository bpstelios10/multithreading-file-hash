package org.learnings.filehash.services;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

class HashingUtilsTest {

    @Test
    void coverStaticInitializerFailure() {
        try (MockedStatic<MessageDigest> mockedDigest = mockStatic(MessageDigest.class)) {
            mockedDigest.when(() -> MessageDigest.getInstance(anyString()))
                    .thenThrow(new NoSuchAlgorithmException("Fake Error"));

            // This will trigger the ExceptionInInitializerError because the static block fails
            assertThatThrownBy(() ->
                // We use reflection to force class loading
                ReflectionTestUtils.invokeMethod(HashingUtils.class, "hashSentence", "some sentence"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("java.security.NoSuchAlgorithmException: Fake Error");
        }
    }
}
