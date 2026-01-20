package org.learnings.filehash.model;

import lombok.Data;

@Data
public final class Text {

    private final String content;
    private final String[] sentences;
    private final int numberOfSentences;

    public Text(String content) {
        this.content = content;
        sentences = content.split("\\.\\s*");
        numberOfSentences = sentences.length;
    }
}
