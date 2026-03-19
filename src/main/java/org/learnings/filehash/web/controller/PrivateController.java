package org.learnings.filehash.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.learnings.filehash.web.filter.RequestsCounterFilter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/private")
public class PrivateController {

    private final RequestsCounterFilter requestsCounterFilter;

    public PrivateController(RequestsCounterFilter requestsCounterFilter) {
        this.requestsCounterFilter = requestsCounterFilter;
    }

    @GetMapping(path = "/status")
    public ResponseEntity<String> status() {
        log.info("test logs");

        String responseText = """
                {
                  "status": "OK",
                  "total requests": "%d"
                }""";

        return ResponseEntity.ok(responseText.formatted(requestsCounterFilter.getTotalRequestsReceived().get()));
    }
}
