package org.learnings.filehash.tests.component.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("component-test")
public class TextFunctionsEndpointTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void extractSentencesHashes_shouldFail_forNullBody() throws Exception {
        mockMvc.perform(post("/text/sentences-hashes")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isBadRequest());
    }

    @Test
    void extractSentencesHashes() throws Exception {
        String text = "{\"text\":\"No man is an island,\\n" +
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

        String responseBody = mockMvc.perform(post("/text/sentences-hashes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(text))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // returns a map of 4 sentences:
        assertThat(responseBody).contains("\"0\":\"").contains("\"1\":\"").contains("\"2\":\"").contains("\"3\":\"");
    }
}
