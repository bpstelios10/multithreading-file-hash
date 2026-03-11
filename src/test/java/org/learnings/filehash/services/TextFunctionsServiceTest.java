package org.learnings.filehash.services;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.learnings.filehash.model.Text;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.learnings.filehash.testutils.AssertionUtils.*;

class TextFunctionsServiceTest {

    private final TextFunctionsService service = new TextFunctionsService();
    private ListAppender<ILoggingEvent> textFunctionsServiceLogs;

    @BeforeEach
    void setUp() {
        textFunctionsServiceLogs = getListAppenderForClass(HashingUtils.class);
    }

    @Test
    void extractSentencesHashes_returnsIndexedSentencesHashes() {
        String text = """
                No man is an island,
                Entire of itself,
                Every man is a piece of the continent,
                A part of the main.
                If a clod be washed away by the sea,
                Europe is the less.
                As well as if a promontory were.
                As well as if a manor of thy friend’s
                Or of thine own were:
                Any man’s death diminishes me,
                Because I am involved in mankind,
                And therefore never send to know for whom the bell tolls;
                It tolls for thee.""";
        Map<Integer, String> sentencesHashes = service.extractSentencesHashes(new Text(text));

        System.out.println("---- results of tests ----");
        System.out.println(sentencesHashes);
        assertThat(sentencesHashes).hasSize(4);
        assertThat(sentencesHashes.get(0)).isEqualTo("1dfcea248091114a437e747de8a21e69a99e060b7768c033a4d5881ac73560da");
        assertThat(sentencesHashes.get(1)).isEqualTo("9cc4c653fdcb6f2eb0a03982675ec9187fc69687823e786fcb4088427dde1e2e");
        assertThat(sentencesHashes.get(2)).isEqualTo("ba3fe787b5f7466de1e0859ff85b70adbd216ab06c517623e6e03fd7a5fc2a64");
        assertThat(sentencesHashes.get(3)).isEqualTo("18088be579f248cd0d336f13a6b2dc0fd15b3b4d835529e1a6b3206e3fee7405");

        assertContainsInLogs(textFunctionsServiceLogs,
                "lets see if MESSAGE_DIGEST is same inside the thread",
                Level.DEBUG);
        // these next lines are a bit unorthodox, but... we can assert that only 2 threads were used
        // and using ThreadLocal only 2 MessageDigest instances were created, by creating a set of all the
        // thread names and MessageDigest hashes we logged in DEBUG
        Set<String> threadsNamesAndMessageDigestInstancesLogs =
                getThreadsNamesAndMessageDigestInstancesLogs(textFunctionsServiceLogs,
                "lets see if MESSAGE_DIGEST is same inside the thread", Level.DEBUG);
        System.out.println("threadsNamesAndMessageDigestInstancesLogs: " + threadsNamesAndMessageDigestInstancesLogs);
        assertThat(threadsNamesAndMessageDigestInstancesLogs)
                .anyMatch(l -> l.contains("-thread-1"))
                .anyMatch(l -> l.contains("-thread-2"));
        assertThat(threadsNamesAndMessageDigestInstancesLogs).hasSize(4);
    }
}
