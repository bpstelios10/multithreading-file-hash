package org.learnings.filehash.web.controller;

import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.learnings.filehash.model.Text;
import org.learnings.filehash.services.TextFunctionsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/text")
public class TextFunctionsController {

    private final TextFunctionsService textFunctionsService;

    public TextFunctionsController(TextFunctionsService textFunctionsService) {
        this.textFunctionsService = textFunctionsService;
    }

    @PostMapping("/sentences-hashes")
    public ResponseEntity<Map<Integer, String>> extractSentencesHashes(@NotNull @RequestBody TextRequest requestBody) {
        Map<Integer, String> sentencesHashes = textFunctionsService.extractSentencesHashes(requestBody.toText());

        return ResponseEntity.ok(sentencesHashes);
    }

    @PostMapping("/occurrences/{target}")
    public CompletableFuture<ResponseEntity<Integer>> countOccurrences(
            @NotNull @RequestBody TextRequest requestBody,
            @NotNull @PathVariable String target) {
        return textFunctionsService.countOccurrences(requestBody.toText(), target)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error processing occurrences: [{}]", ex.getMessage(), ex);
                    return ResponseEntity.internalServerError().build();
                });
    }

    public record TextRequest(String text) {
        Text toText() {
            return new Text(text);
        }
    }
}
