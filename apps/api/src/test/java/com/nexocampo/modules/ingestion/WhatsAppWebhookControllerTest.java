package com.nexocampo.modules.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexocampo.modules.identity.JwtAuthenticationFilter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(OutputCaptureExtension.class)
@WebMvcTest(
        controllers = WhatsAppWebhookController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(WhatsAppWebhookControllerTest.Properties.class)
@TestPropertySource(properties = {
        "app.whatsapp.app-secret=" + WhatsAppWebhookControllerTest.APP_SECRET,
        "app.whatsapp.verify-token=token-combinado"})
class WhatsAppWebhookControllerTest {

    static final String APP_SECRET = "segredo-do-app";

    // Formato real de um webhook da Meta (valores trocados).
    private static final String MESSAGE_WEBHOOK = """
            {"object":"whatsapp_business_account","entry":[{"id":"102290129340398","changes":[{"field":"messages",
            "value":{"messaging_product":"whatsapp","metadata":{"display_phone_number":"15550783881",
            "phone_number_id":"106540352242922"},"contacts":[{"profile":{"name":"Zé"},"wa_id":"5511912345678"}],
            "messages":[{"from":"5511912345678","id":"wamid.HBgLMTU1","timestamp":"1763400000","type":"image",
            "image":{"caption":"ferrugem no talhão 4","mime_type":"image/jpeg","sha256":"abc","id":"1003383421387256"}}]}}]}]}
            """;
    // Falha real recebida da Meta com o portfólio ainda sem verificação (valores trocados).
    private static final String STATUS_WEBHOOK = """
            {"object":"whatsapp_business_account","entry":[{"id":"1","changes":[{"field":"messages",
            "value":{"messaging_product":"whatsapp","statuses":[
            {"id":"wamid.ENTREGUE","status":"delivered","recipient_id":"5511912345678"},
            {"id":"wamid.FALHOU","status":"failed","recipient_id":"5511912345678","errors":[{"code":131031,
            "title":"Business Account locked","message":"Business Account locked",
            "error_data":{"details":"Business account has been locked."}}]}]}}]}]}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WhatsAppMessageProcessor processor;

    @Test
    void answersTheVerificationChallengeOnlyWithTheAgreedToken() throws Exception {
        mockMvc.perform(get("/api/webhooks/whatsapp")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "token-combinado")
                        .param("hub.challenge", "1158201444"))
                .andExpect(status().isOk())
                .andExpect(content().string("1158201444"));

        mockMvc.perform(get("/api/webhooks/whatsapp")
                        .param("hub.mode", "subscribe")
                        .param("hub.verify_token", "errado")
                        .param("hub.challenge", "1158201444"))
                .andExpect(status().isForbidden());
    }

    @Test
    @SuppressWarnings("unchecked")
    void acceptsSignedMessagesAndHandsThemToTheProcessor() throws Exception {
        byte[] body = MESSAGE_WEBHOOK.getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(post("/api/webhooks/whatsapp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(WhatsAppSignature.HEADER, "sha256=" + WhatsAppSignature.sign(APP_SECRET, body))
                        .content(body))
                .andExpect(status().isOk());

        ArgumentCaptor<List<WhatsAppWebhookPayload.Message>> messages = ArgumentCaptor.forClass(List.class);
        verify(processor).enqueue(messages.capture());
        WhatsAppWebhookPayload.Message message = messages.getValue().get(0);
        assertThat(message.from()).isEqualTo("5511912345678");
        assertThat(message.id()).isEqualTo("wamid.HBgLMTU1");
        assertThat(message.type()).isEqualTo("image");
        assertThat(message.image().id()).isEqualTo("1003383421387256");
        assertThat(message.image().mimeType()).isEqualTo("image/jpeg");
        assertThat(message.image().caption()).isEqualTo("ferrugem no talhão 4");
    }

    @Test
    void rejectsUnsignedOrTamperedCalls() throws Exception {
        byte[] body = MESSAGE_WEBHOOK.getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(post("/api/webhooks/whatsapp").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/webhooks/whatsapp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(WhatsAppSignature.HEADER, "sha256=" + WhatsAppSignature.sign("outro-segredo", body))
                        .content(body))
                .andExpect(status().isUnauthorized());

        verify(processor, never()).enqueue(any());
    }

    @Test
    void logsFailedDeliveriesWithoutProcessingThem(CapturedOutput output) throws Exception {
        byte[] body = STATUS_WEBHOOK.getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(post("/api/webhooks/whatsapp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(WhatsAppSignature.HEADER, "sha256=" + WhatsAppSignature.sign(APP_SECRET, body))
                        .content(body))
                .andExpect(status().isOk());

        verify(processor).enqueue(List.of());
        assertThat(output).contains(
                "A Meta não entregou a mensagem wamid.FALHOU do WhatsApp: "
                        + "131031 Business Account locked (Business account has been locked.)");
        assertThat(output).doesNotContain("wamid.ENTREGUE").doesNotContain("5511912345678");
    }

    @TestConfiguration
    @EnableConfigurationProperties(WhatsAppProperties.class)
    static class Properties {
    }
}
