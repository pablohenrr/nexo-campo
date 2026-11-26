package com.nexocampo.modules.weather;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexocampo.modules.identity.JwtAuthenticationFilter;
import com.nexocampo.modules.identity.Role;
import com.nexocampo.modules.identity.User;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = WeatherAlertController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class WeatherAlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WeatherAlertService weatherAlertService;

    private User user;

    @BeforeEach
    void principal() {
        user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        user.setName("Ana");
        user.setEmail("ana@nexocampo.local");
        user.setPasswordHash("hash");
        user.setRole(Role.PRODUTOR);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listsAlertsForTheAuthenticatedOwner() throws Exception {
        when(weatherAlertService.list(user))
                .thenReturn(new WeatherAlertDtos.AlertasClimaticosResponse(1, List.of()));

        mockMvc.perform(get("/api/alertas-climaticos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.naoLidos").value(1));

        verify(weatherAlertService).list(user);
    }

    @Test
    void marksAnAlertAsRead() throws Exception {
        UUID eventId = UUID.randomUUID();

        mockMvc.perform(post("/api/alertas-climaticos/{id}/leitura", eventId)).andExpect(status().isNoContent());

        verify(weatherAlertService).markRead(user, eventId);
    }

    @Test
    void rejectsRadiusAboveTheCap() throws Exception {
        mockMvc.perform(put("/api/fazendas/{fazendaId}/alertas-climaticos", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"raioKm\":200,\"categorias\":[\"CHUVA\"],\"frequencia\":\"POR_EVENTO\"}"))
                .andExpect(status().isBadRequest());

        verify(weatherAlertService, never()).update(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
