package org.learnings.filehash.tests.component.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("component-test")
public class TextFunctionsEndpointTests {

    @Autowired
    private MockMvc mockMvc;

    private final String text = "{\"text\":\"No man is an island,\\n" +
            "Entire of itself,\\n" +
            "Every man is a piece of the continent,\\n" +
            "A part of the main.\\n" +
            "If a clod be washed away by the sea,\\n" +
            "Europe is the less.\\n" +
            "As well as if a promontory were.\\n" +
            "As well as if a manor of thy friend’s\\n" +
            "Or of thine own were:\\n" +
            "Any man’s death diminishes me,\\n" +
            "Because I am involved in mankind,\\n" +
            "And therefore never send to know for whom the bell tolls;\\n" +
            "It tolls for thee.\"}";

    @Test
    void extractSentencesHashes() throws Exception {
        String responseBody = mockMvc.perform(post("/text/sentences-hashes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(text))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // returns a map of 4 sentences:
        assertThat(responseBody)
                .contains("{\"0\":\"1dfcea248091114a437e747de8a21e69a99e060b7768c033a4d5881ac73560da\"," +
                        "\"1\":\"9cc4c653fdcb6f2eb0a03982675ec9187fc69687823e786fcb4088427dde1e2e\"," +
                        "\"2\":\"ba3fe787b5f7466de1e0859ff85b70adbd216ab06c517623e6e03fd7a5fc2a64\"," +
                        "\"3\":\"18088be579f248cd0d336f13a6b2dc0fd15b3b4d835529e1a6b3206e3fee7405\"}");
    }

    @Test
    void countOccurrences() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/text/occurrences/of")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(text))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    void countOccurrences_whenTargetNotInText() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/text/occurrences/nada")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(text))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }
}
