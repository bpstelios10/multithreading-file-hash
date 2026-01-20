package org.learnings.filehash.web.controller;

import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.learnings.filehash.model.Text;
import org.learnings.filehash.services.TextFunctionsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/text/sentences-hashes")
public class TextFunctionsController {

    private final TextFunctionsService textFunctionsService;

    public TextFunctionsController(TextFunctionsService textFunctionsService) {
        this.textFunctionsService = textFunctionsService;
    }

    @PostMapping
    public ResponseEntity<Map<Integer, String>> extractSentencesHashes(@NotNull @RequestBody TextRequest requestBody) {
        Map<Integer, String> sentencesHashes = textFunctionsService.extractSentencesHashes(requestBody.toText());

        return ResponseEntity.ok(sentencesHashes);
    }

    public record TextRequest(String text) {
        Text toText() {
            return new Text(text);
        }
    }
}
