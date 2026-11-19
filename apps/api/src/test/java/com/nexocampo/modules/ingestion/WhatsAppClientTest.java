package com.nexocampo.modules.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class WhatsAppClientTest {

    private static final String GRAPH = "https://graph.test/v23.0";
    private static final WhatsAppProperties PROPERTIES =
            new WhatsAppProperties(GRAPH, "token-do-app", "106540352242922", "segredo", "verify", Duration.ofSeconds(5));

    private MockRestServiceServer server;

    @Test
    void downloadsMediaThroughTheTemporaryUrlUsingTheToken() {
        WhatsAppClient client = client(PROPERTIES);
        server.expect(requestTo(GRAPH + "/1003383421387256"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token-do-app"))
                .andRespond(withSuccess("""
                        {"url":"https://lookaside.test/whatsapp_business/attachments/?mid=1003383421387256&hash=abc",
                         "mime_type":"audio/ogg; codecs=opus","file_size":3,"id":"1003383421387256"}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://lookaside.test/whatsapp_business/attachments/?mid=1003383421387256&hash=abc"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token-do-app"))
                .andRespond(withSuccess(new byte[] {0x4F, 0x67, 0x67}, MediaType.APPLICATION_OCTET_STREAM));

        IncomingMedia media = client.downloadMedia("1003383421387256");

        server.verify();
        assertThat(media.content()).containsExactly(0x4F, 0x67, 0x67);
        assertThat(media.contentType()).isEqualTo("audio/ogg; codecs=opus");
        assertThat(media.fileName()).isEqualTo("whatsapp-1003383421387256.ogg");
    }

    @Test
    void repliesQuotingTheProducerMessage() {
        WhatsAppClient client = client(PROPERTIES);
        server.expect(requestTo(GRAPH + "/106540352242922/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token-do-app"))
                .andExpect(jsonPath("$.messaging_product").value("whatsapp"))
                .andExpect(jsonPath("$.to").value("5511912345678"))
                .andExpect(jsonPath("$.type").value("text"))
                .andExpect(jsonPath("$.text.body").value("✅ Atividade registrada"))
                .andExpect(jsonPath("$.context.message_id").value("wamid.ABC"))
                .andRespond(withSuccess("{\"messages\":[{\"id\":\"wamid.RESP\"}]}", MediaType.APPLICATION_JSON));

        client.sendText("5511912345678", "✅ Atividade registrada", "wamid.ABC");

        server.verify();
    }

    @Test
    void marksMessagesAsRead() {
        WhatsAppClient client = client(PROPERTIES);
        server.expect(requestTo(GRAPH + "/106540352242922/messages"))
                .andExpect(jsonPath("$.status").value("read"))
                .andExpect(jsonPath("$.message_id").value("wamid.ABC"))
                .andRespond(withSuccess("{\"success\":true}", MediaType.APPLICATION_JSON));

        client.markAsRead("wamid.ABC");

        server.verify();
    }

    @Test
    void explainsMissingConfiguration() {
        WhatsAppClient client = client(new WhatsAppProperties(GRAPH, "", "", "", "", null));

        assertThatThrownBy(() -> client.sendText("5511912345678", "oi", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("WHATSAPP_ACCESS_TOKEN");
    }

    private WhatsAppClient client(WhatsAppProperties properties) {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        return new WhatsAppClient(builder, properties);
    }
}
