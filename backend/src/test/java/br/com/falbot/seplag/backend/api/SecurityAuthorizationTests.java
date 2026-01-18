package br.com.falbot.seplag.backend.api;

import br.com.falbot.seplag.backend.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityAuthorizationTests extends IntegrationTestBase {

    @Autowired MockMvc mvc;

    @Test
    void rotaProtegida_semToken_deveRetornar401_artista() throws Exception {
        mvc.perform(get("/api/artistas"))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void rotaProtegida_semToken_deveRetornar401_album() throws Exception {
        mvc.perform(get("/api/album"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rotaProtegida_comTokenInvalido_deveRetornar401() throws Exception {
        mvc.perform(get("/api/albuns")
                .header(HttpHeaders.AUTHORIZATION, "Bearer abc.def.ghi"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void rotaPublica_ping_deveRetornar200() throws Exception {
        mvc.perform(get("/api/ping"))
                .andExpect(status().isOk());
    }
}
