package com.nexocampo.modules.identity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = UserProfileController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserProfileService userProfileService;

    @Test
    void returnsTheProfileWithPortugueseFields() throws Exception {
        when(userProfileService.get(any())).thenReturn(new UserProfileService.ProfileResponse(
                UUID.randomUUID(), "Maria", "maria@example.com", Role.PRODUTOR, "+5511987654321", true, false,
                OffsetDateTime.parse("2026-11-20T10:00:00Z")));

        mockMvc.perform(get("/api/usuarios/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria"))
                .andExpect(jsonPath("$.telefone").value("+5511987654321"))
                .andExpect(jsonPath("$.notificacoesSmsAtivas").value(true))
                .andExpect(jsonPath("$.contaGoogle").value(false))
                .andExpect(jsonPath("$.criadoEm").exists());
    }

    @Test
    void requiresAName() throws Exception {
        mockMvc.perform(put("/api/usuarios/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"  \",\"telefone\":null,\"notificacoesSmsAtivas\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes[0]").value(org.hamcrest.Matchers.startsWith("nome:")));
        verify(userProfileService, never()).update(any(), any());
    }

    @Test
    void changesThePasswordWithoutReturningABody() throws Exception {
        mockMvc.perform(put("/api/usuarios/me/senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"senha-antiga-123\",\"novaSenha\":\"senha-nova-456\"}"))
                .andExpect(status().isNoContent());
        verify(userProfileService).changePassword(any(), any());
    }

    @Test
    void rejectsShortNewPasswords() throws Exception {
        mockMvc.perform(put("/api/usuarios/me/senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"senha-antiga-123\",\"novaSenha\":\"curta\"}"))
                .andExpect(status().isBadRequest());
        verify(userProfileService, never()).changePassword(any(), any());
    }
}
