package com.nexocampo.modules.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexocampo.modules.identity.JwtAuthenticationFilter;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = FieldMessageController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class FieldMessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FieldMessageService fieldMessageService;

    @Test
    void receivesTextAndFileFromTheApp() throws Exception {
        when(fieldMessageService.receive(any(), any())).thenReturn(new FieldMessageService.FieldMessageResponse(
                UUID.randomUUID(), MessageChannel.APP, FieldMessageStatus.REGISTRADA, MessageKind.OCORRENCIA,
                UUID.randomUUID(), "Talhão 01", null, UUID.randomUUID(), UUID.randomUUID(), "olha isso", null,
                "⚠️ Ocorrência registrada", null, OffsetDateTime.now()));

        mockMvc.perform(multipart("/api/ingestao/mensagens")
                        .file(new MockMultipartFile("arquivo", "foto.jpg", "image/jpeg", new byte[] {1, 2, 3}))
                        .param("texto", "olha isso"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.situacao").value("REGISTRADA"))
                .andExpect(jsonPath("$.nomeTalhao").value("Talhão 01"));

        ArgumentCaptor<IncomingMessage> message = ArgumentCaptor.forClass(IncomingMessage.class);
        verify(fieldMessageService).receive(any(), message.capture());
        assertThat(message.getValue().channel()).isEqualTo(MessageChannel.APP);
        assertThat(message.getValue().text()).isEqualTo("olha isso");
        assertThat(message.getValue().media().contentType()).isEqualTo("image/jpeg");
        assertThat(message.getValue().media().content()).containsExactly(1, 2, 3);
    }

    @Test
    void answersBadRequestForInvalidMessages() throws Exception {
        when(fieldMessageService.receive(any(), any()))
                .thenThrow(new IllegalArgumentException("Envie um texto, um áudio ou uma foto"));

        mockMvc.perform(multipart("/api/ingestao/mensagens"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Envie um texto, um áudio ou uma foto"));
    }
}
