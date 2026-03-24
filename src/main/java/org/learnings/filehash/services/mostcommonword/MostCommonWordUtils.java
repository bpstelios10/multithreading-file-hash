package org.learnings.filehash.services.mostcommonword;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import java.util.Map;

@Slf4j
public final class MostCommonWordUtils {

    private MostCommonWordUtils() {
    }

    public static @Nullable String logAndGetEntryKey(Map.Entry<String, ?> mostCommon) {
        if (mostCommon != null)
            log.debug("most common word is [{}] with [{}] occurrences", mostCommon.getKey(), mostCommon.getValue());

        return mostCommon == null ? null : mostCommon.getKey();
    }
}
