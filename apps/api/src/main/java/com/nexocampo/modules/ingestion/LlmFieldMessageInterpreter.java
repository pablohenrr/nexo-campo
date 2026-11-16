package com.nexocampo.modules.ingestion;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.nexocampo.shared.ai.AiProviderException;
import com.nexocampo.shared.ai.OpenAiCompatibleClient;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Interpreta mensagens com um modelo de linguagem, pedindo a resposta no JSON Schema do contrato. */
@Component
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "openai-compatible")
public class LlmFieldMessageInterpreter implements FieldMessageInterpreter {

    static final String PROMPT_PATH = "ai/field-message-interpreter.prompt.md";
    static final String SCHEMA_PATH = "ai/field-message-interpretation.schema.json";
    private static final String SCHEMA_NAME = "field_message_interpretation";

    private final OpenAiCompatibleClient client;
    private final String promptTemplate;
    private final JsonNode schema;
    private final ObjectReader interpretationReader;

    public LlmFieldMessageInterpreter(OpenAiCompatibleClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.promptTemplate = readResource(PROMPT_PATH);
        try {
            this.schema = objectMapper.readTree(readResource(SCHEMA_PATH));
        } catch (IOException exception) {
            throw new UncheckedIOException("Schema de interpretação inválido: " + SCHEMA_PATH, exception);
        }
        // Um valor fora da lista vira OUTRO (ou null) em vez de derrubar a mensagem inteira.
        this.interpretationReader = objectMapper.readerFor(Interpretation.class)
                .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
    }

    @Override
    public Interpretation interpret(InterpretationRequest request) {
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(OpenAiCompatibleClient.textPart(userText(request)));
        if (request.image() != null) {
            content.add(OpenAiCompatibleClient.imagePart(request.image().content(), request.image().contentType()));
        }
        String json = client.chatJson(systemPrompt(request), content, SCHEMA_NAME, schema);
        try {
            return interpretationReader.readValue(json);
        } catch (IOException exception) {
            throw new AiProviderException("A IA respondeu fora do formato esperado", exception);
        }
    }

    private String systemPrompt(InterpretationRequest request) {
        String plots = request.plotNames().isEmpty()
                ? "(nenhum talhão cadastrado)"
                : request.plotNames().stream().map(name -> "- " + name).collect(Collectors.joining("\n"));
        return promptTemplate
                .replace("{{today}}", request.today().toString())
                .replace("{{plots}}", plots);
    }

    private static String userText(InterpretationRequest request) {
        String text = request.text() == null || request.text().isBlank()
                ? "(sem texto; interprete a foto)"
                : request.text();
        return "Mensagem recebida:\n" + text;
    }

    private static String readResource(String path) {
        try (InputStream input = new ClassPathResource(path).getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException("Recurso de IA não encontrado: " + path, exception);
        }
    }
}
