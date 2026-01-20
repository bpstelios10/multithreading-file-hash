package org.learnings.filehash.web.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.learnings.filehash.model.Text;
import org.learnings.filehash.services.TextFunctionsService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;

@ExtendWith(MockitoExtension.class)
class TextFunctionsControllerTest {

    @Mock
    private TextFunctionsService service;
    @InjectMocks
    private TextFunctionsController controller;

    @Test
    void extractSentencesHashes() {
        String text = """
                No man is an island,
                Entire of itself,
                Every man is a piece of the continent,
                A part of the main.
                If a clod be washed away by the sea,
                Europe is the less.
                As well as if a promontory were.
                As well as if a manor of thy friend’s
                Or of thine own were:
                Any man’s death diminishes me,
                Because I am involved in mankind,
                And therefore never send to know for whom the bell tolls;
                It tolls for thee.""";
        TextFunctionsController.TextRequest requestBody = new TextFunctionsController.TextRequest(text);
        Map<Integer, String> expectedResult =
                Map.of(1, "sentence1hash", 2, "sentence2hash", 3, "sentence3hash", 4, "sentence4hash");
        when(service.extractSentencesHashes(new Text(text))).thenReturn(expectedResult);

        ResponseEntity<Map<Integer, String>> allResource1 = controller.extractSentencesHashes(requestBody);

        assertThat(allResource1.getStatusCode()).isEqualTo(OK);
        assertThat(allResource1.getBody()).isEqualTo(expectedResult);
    }
}
