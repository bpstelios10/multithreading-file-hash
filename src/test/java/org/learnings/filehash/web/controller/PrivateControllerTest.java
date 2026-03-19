package org.learnings.filehash.web.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.learnings.filehash.web.filter.RequestsCounterFilter;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrivateControllerTest {

    @Mock
    private RequestsCounterFilter requestsCounterFilter;
    @InjectMocks
    private PrivateController controller;

    @BeforeEach
    void setup() {
        when(requestsCounterFilter.getTotalRequestsReceived()).thenReturn(new AtomicLong());
    }

    @Test
    void status() {
        ResponseEntity<String> response = controller.status();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("\"status\": \"OK\",")
                .contains("\"total requests\": \"");
    }
}
