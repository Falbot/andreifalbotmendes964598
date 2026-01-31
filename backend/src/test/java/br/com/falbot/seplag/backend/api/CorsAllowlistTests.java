package br.com.falbot.seplag.backend.api;

import br.com.falbot.seplag.backend.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
class CorsAllowlistTests extends IntegrationTestBase {

    @Autowired MockMvc mvc;

    @Test
    void preflight_options_deveResponder200_e_allow_origin_para_origem_permitida() throws Exception {
        mvc.perform(options("/api/v1/artistas")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void requisicao_com_origem_nao_permitida_deveBloquear_com_403() throws Exception {
        mvc.perform(get("/api/v1/ping")
                        .header("Origin", "http://evil.com"))
                .andExpect(status().isForbidden());
    }
}
