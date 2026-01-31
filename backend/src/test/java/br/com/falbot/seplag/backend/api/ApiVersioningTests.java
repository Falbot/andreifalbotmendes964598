package br.com.falbot.seplag.backend.api;

import br.com.falbot.seplag.backend.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
class ApiVersioningTests extends IntegrationTestBase {

    @Autowired MockMvc mvc;

    @Test
    void ping_deveFuncionarEmV1_semAutenticacao() throws Exception {
        mvc.perform(get("/api/v1/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    void albuns_emV1_deveExigirAutenticacao() throws Exception {
        mvc.perform(get("/api/v1/albuns"))
                .andExpect(status().isUnauthorized());
    }
}
