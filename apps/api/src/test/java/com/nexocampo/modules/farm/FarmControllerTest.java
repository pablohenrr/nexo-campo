package com.nexocampo.modules.farm;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexocampo.modules.identity.JwtAuthenticationFilter;
import java.math.BigDecimal;
import java.util.List;
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
        controllers = FarmController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class FarmControllerTest {

    private static final String BODY = """
            {"nome":"Fazenda Boa Vista","municipio":"Londrina","estado":"PR",
             "contorno":[{"latitude":-23.300,"longitude":-51.170},{"latitude":-23.300,"longitude":-51.160},
                         {"latitude":-23.310,"longitude":-51.160},{"latitude":-23.310,"longitude":-51.170}]}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FarmService farmService;

    @Test
    void createReturnsOutlineAndCalculatedCenter() throws Exception {
        when(farmService.create(any(), any())).thenReturn(response());

        mockMvc.perform(post("/api/fazendas").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.latitude").value(-23.305))
                .andExpect(jsonPath("$.contorno.length()").value(4))
                .andExpect(jsonPath("$.contorno[0].longitude").value(-51.17));
    }

    @Test
    void updateUsesPathId() throws Exception {
        UUID id = UUID.randomUUID();
        when(farmService.update(eq(id), any(), any())).thenReturn(response());

        mockMvc.perform(put("/api/fazendas/{id}", id).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Fazenda Boa Vista"));
    }

    @Test
    void outlineIsRequired() throws Exception {
        mockMvc.perform(post("/api/fazendas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Sem contorno\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(farmService);
    }

    @Test
    void outlineNeedsAtLeastThreePins() throws Exception {
        mockMvc.perform(post("/api/fazendas").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nome":"Curta","contorno":[{"latitude":-23.3,"longitude":-51.1},{"latitude":-23.4,"longitude":-51.2}]}
                        """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(farmService);
    }

    @Test
    void invalidGeometryFromServiceIsBadRequest() throws Exception {
        when(farmService.create(any(), any()))
                .thenThrow(new IllegalArgumentException("contorno não pode ter linhas que se cruzam"));

        mockMvc.perform(post("/api/fazendas").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("contorno não pode ter linhas que se cruzam"));
    }

    private static FarmService.FarmResponse response() {
        return new FarmService.FarmResponse(UUID.randomUUID(), "Fazenda Boa Vista", null, "Londrina", "PR",
                new BigDecimal("113.560"), new BigDecimal("-23.3050000"), new BigDecimal("-51.1650000"),
                List.of(new FarmService.Coordenada(new BigDecimal("-23.300"), new BigDecimal("-51.170")),
                        new FarmService.Coordenada(new BigDecimal("-23.300"), new BigDecimal("-51.160")),
                        new FarmService.Coordenada(new BigDecimal("-23.310"), new BigDecimal("-51.160")),
                        new FarmService.Coordenada(new BigDecimal("-23.310"), new BigDecimal("-51.170"))));
    }
}
