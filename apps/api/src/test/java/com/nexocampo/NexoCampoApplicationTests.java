package com.nexocampo;

import com.nexocampo.modules.identity.JwtAuthenticationFilter;
import com.nexocampo.shared.health.HealthController;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// O filtro JWT entraria na fatia web sem o JwtService; com addFilters = false ele não roda mesmo.
@WebMvcTest(
        controllers = HealthController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class NexoCampoApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointReportsApplicationAsUp() throws Exception {
        mockMvc.perform(get("/api/saude"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("ATIVO"))
                .andExpect(jsonPath("$.dataHora").isNotEmpty());
    }
}
