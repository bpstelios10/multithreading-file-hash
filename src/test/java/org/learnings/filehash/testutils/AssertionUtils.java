package org.learnings.filehash.testutils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class AssertionUtils {

    private static final Pattern pattern = Pattern.compile("\\[(.*?)]");

    private AssertionUtils() {
    }

    public static ListAppender<ILoggingEvent> getListAppenderForClass(Class<?> clazz) {
        Logger logger = (Logger) LoggerFactory.getLogger(clazz);
        ListAppender<ILoggingEvent> loggingEventListAppender = new ListAppender<>();
        loggingEventListAppender.start();
        logger.addAppender(loggingEventListAppender);
        return loggingEventListAppender;
    }

    public static void assertContainsInLogs(ListAppender<ILoggingEvent> appender, String message, Level level) {
        assertThat(appender.list)
                .filteredOn(e -> e.getLevel() == level)
                .extracting(ILoggingEvent::getFormattedMessage)
                .filteredOn(e -> e.startsWith(message))
                .isNotEmpty();
    }

    public static Set<String> getThreadsNamesAndMessageDigestInstancesLogs(
            ListAppender<ILoggingEvent> appender, String message, Level level) {
        return appender.list
                .stream()
                .filter(e -> e.getLevel() == level)
                .map(ILoggingEvent::getFormattedMessage)
                .filter(logMessage -> logMessage.contains(message))
                .flatMap(str -> {
                    Matcher matcher = pattern.matcher(str);
                    Set<String> matches = new HashSet<>();
                    while (matcher.find()) {
                        matches.add(matcher.group(1));
                    }
                    return matches.stream();
                })
                .collect(Collectors.toSet());
    }
}