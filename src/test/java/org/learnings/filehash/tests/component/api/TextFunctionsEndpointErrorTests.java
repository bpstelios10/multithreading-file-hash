package org.learnings.filehash.tests.component.api;

import org.junit.jupiter.api.Test;
import org.learnings.filehash.services.TextFunctionsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.CompletableFuture;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("component-test")
public class TextFunctionsEndpointErrorTests {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private TextFunctionsService textFunctionsService;

    @Test
    void extractSentencesHashes_shouldFail_forNullBody() throws Exception {
        mockMvc.perform(post("/text/sentences-hashes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("")
        ).andExpect(status().isBadRequest());
    }

    @Test
    void extractSentencesHashes_shouldFail_forMissingBody() throws Exception {
        mockMvc.perform(post("/text/sentences-hashes")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isBadRequest());
    }

    @Test
    void countOccurrences_shouldFail_forNullBody() throws Exception {
        mockMvc.perform(post("/text/occurrences/anything")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Failed to read request")));
    }

    @Test
    void countOccurrences_shouldFail_forMissingBody() throws Exception {
        mockMvc.perform(post("/text/occurrences/anything")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Failed to read request")));
    }

    @Test
    void countOccurrences_shouldFail_forPipelineFailure() throws Exception {
        when(textFunctionsService.countOccurrences(any(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Simulated failure")));

        MvcResult mvcResult = mockMvc.perform(post("/text/occurrences/anything")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"No man is an island\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        // Assert: async dispatch completes with 500
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(""));
    }
}
