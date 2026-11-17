package com.nexocampo.modules.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexocampo.modules.investigation.OccurrenceCategory;
import com.nexocampo.shared.ai.AiProperties;
import com.nexocampo.shared.ai.AiProviderException;
import com.nexocampo.shared.ai.OpenAiCompatibleClient;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class LlmFieldMessageInterpreterTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 16);
    private static final List<String> PLOTS = List.of("Talhão 01", "Talhão 04 - Milho Safrinha");

    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
    private MockRestServiceServer server;

    @Test
    void sendsPromptPlotsAndPhotoAndReadsTheStructuredAnswer() throws Exception {
        LlmFieldMessageInterpreter interpreter = interpreter(true);
        server.expect(requestTo("https://chat.test/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer chat-key"))
                .andExpect(jsonPath("$.model").value("chat-model"))
                .andExpect(jsonPath("$.response_format.type").value("json_schema"))
                .andExpect(jsonPath("$.response_format.json_schema.schema.properties.tipo.enum[1]").value("OCORRENCIA"))
                .andExpect(jsonPath("$.messages[0].content", containsString("- Talhão 04 - Milho Safrinha")))
                .andExpect(jsonPath("$.messages[0].content", containsString("Data de hoje: 2026-11-16")))
                .andExpect(jsonPath("$.messages[1].content[0].text", containsString("olha o milho")))
                .andExpect(jsonPath("$.messages[1].content[1].image_url.url", startsWith("data:image/jpeg;base64,AQID")))
                .andRespond(withSuccess(chatResponse("""
                        {"tipo":"OCORRENCIA","nomeTalhao":"Talhão 04 - Milho Safrinha",
                         "resumo":"Manchas nas folhas do milho",
                         "ocorrencia":{"categoria":"DOENCA","severidade":"MEDIA","descricao":"Possível ferrugem"},
                         "camposFaltantes":[],"confianca":0.8}
                        """), MediaType.APPLICATION_JSON));

        Interpretation result = interpreter.interpret(new InterpretationRequest("olha o milho",
                new IncomingMedia(new byte[] {1, 2, 3}, "image/jpeg", "foto.jpg"), PLOTS, TODAY));

        server.verify();
        assertThat(result.tipo()).isEqualTo(MessageKind.OCORRENCIA);
        assertThat(result.nomeTalhao()).isEqualTo("Talhão 04 - Milho Safrinha");
        assertThat(result.ocorrencia().categoria()).isEqualTo(OccurrenceCategory.DOENCA);
    }

    @Test
    void turnsUnknownEnumValuesIntoOutroAndAcceptsMarkdownFences() throws Exception {
        LlmFieldMessageInterpreter interpreter = interpreter(true);
        server.expect(requestTo("https://chat.test/v1/chat/completions"))
                .andRespond(withSuccess(chatResponse("""
                        ```json
                        {"tipo":"ATIVIDADE","resumo":"Roçagem","atividade":{"tipo":"ROCAGEM","data":"2026-11-15",
                         "descricao":"Roçagem da beira"},"camposFaltantes":["custo"],"confianca":0.6}
                        ```"""), MediaType.APPLICATION_JSON));

        Interpretation result = interpreter.interpret(new InterpretationRequest("rocei ontem", null, PLOTS, TODAY));

        assertThat(result.atividade().tipo()).isEqualTo(ReportedActivityType.OUTRO);
        assertThat(result.atividade().data()).isEqualTo(LocalDate.of(2026, 11, 15));
        assertThat(result.camposFaltantes()).containsExactly("custo");
    }

    @Test
    void putsTheSchemaInThePromptWhenProviderOnlySupportsJsonObject() throws Exception {
        LlmFieldMessageInterpreter interpreter = interpreter(false);
        server.expect(requestTo("https://chat.test/v1/chat/completions"))
                .andExpect(jsonPath("$.response_format.type").value("json_object"))
                .andExpect(jsonPath("$.messages[0].content", containsString("JSON Schema")))
                .andRespond(withSuccess(chatResponse("{\"tipo\":\"OUTRO\",\"resumo\":\"oi\",\"camposFaltantes\":[],\"confianca\":1}"),
                        MediaType.APPLICATION_JSON));

        assertThat(interpreter.interpret(new InterpretationRequest("oi", null, PLOTS, TODAY)).tipo())
                .isEqualTo(MessageKind.OUTRO);
    }

    @Test
    void reportsProviderErrors() {
        LlmFieldMessageInterpreter interpreter = interpreter(true);
        server.expect(requestTo("https://chat.test/v1/chat/completions"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("{\"error\":\"quota\"}"));

        assertThatThrownBy(() -> interpreter.interpret(new InterpretationRequest("oi", null, PLOTS, TODAY)))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("429");
    }

    @Test
    void retriesWhenProviderIsTemporarilyUnavailable() throws Exception {
        LlmFieldMessageInterpreter interpreter = interpreter(true);
        server.expect(requestTo("https://chat.test/v1/chat/completions"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE).body("{\"error\":\"high demand\"}"));
        server.expect(requestTo("https://chat.test/v1/chat/completions"))
                .andRespond(withSuccess(chatResponse("{\"tipo\":\"OUTRO\",\"resumo\":\"oi\",\"camposFaltantes\":[],\"confianca\":1}"),
                        MediaType.APPLICATION_JSON));

        Interpretation result = interpreter.interpret(new InterpretationRequest("oi", null, PLOTS, TODAY));

        server.verify();
        assertThat(result.tipo()).isEqualTo(MessageKind.OUTRO);
    }

    @Test
    void givesUpAfterLastAttempt() {
        LlmFieldMessageInterpreter interpreter = interpreter(true);
        for (int attempt = 0; attempt < 3; attempt++) {
            server.expect(requestTo("https://chat.test/v1/chat/completions"))
                    .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        }

        assertThatThrownBy(() -> interpreter.interpret(new InterpretationRequest("oi", null, PLOTS, TODAY)))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("503");
        server.verify();
    }

    @Test
    void reportsAnswersOutsideTheContract() throws Exception {
        LlmFieldMessageInterpreter interpreter = interpreter(true);
        server.expect(requestTo("https://chat.test/v1/chat/completions"))
                .andRespond(withSuccess(chatResponse("não sei responder em JSON"), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> interpreter.interpret(new InterpretationRequest("oi", null, PLOTS, TODAY)))
                .isInstanceOf(AiProviderException.class);
    }

    @Test
    void transcribesAudioWithFileNameAndLanguage() {
        interpreter(true);
        LlmAudioTranscriber transcriber = new LlmAudioTranscriber(client);
        server.expect(requestTo("https://audio.test/v1/audio/transcriptions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer audio-key"))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, startsWith("multipart/form-data")))
                .andExpect(content().string(containsString("filename=\"audio.ogg\"")))
                .andExpect(content().string(containsString("audio-model")))
                .andRespond(withSuccess("{\"text\":\" Apliquei glifosato no talhão 4. \"}", MediaType.APPLICATION_JSON));

        String transcript = transcriber.transcribe(new IncomingMedia(new byte[] {1, 2}, "audio/ogg; codecs=opus", null));

        server.verify();
        assertThat(transcript).isEqualTo("Apliquei glifosato no talhão 4.");
    }

    private OpenAiCompatibleClient client;

    private LlmFieldMessageInterpreter interpreter(boolean jsonSchema) {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        AiProperties properties = new AiProperties("openai-compatible", Duration.ofSeconds(5), 3, Duration.ofMillis(1),
                new AiProperties.Chat("https://chat.test/v1/", "chat-key", "chat-model", jsonSchema),
                new AiProperties.Transcription("https://audio.test/v1", "audio-key", "audio-model"));
        client = new OpenAiCompatibleClient(builder, properties, objectMapper);
        return new LlmFieldMessageInterpreter(client, objectMapper);
    }

    private String chatResponse(String content) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "choices", List.of(Map.of("message", Map.of("role", "assistant", "content", content)))));
    }
}
