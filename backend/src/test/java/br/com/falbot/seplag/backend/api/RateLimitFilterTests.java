package br.com.falbot.seplag.backend.api;

import br.com.falbot.seplag.backend.support.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
class RateLimitFilterTests extends IntegrationTestBase {

    @Autowired MockMvc mvc;
    private final ObjectMapper om = new ObjectMapper().findAndRegisterModules();

    @Test
    void deveRetornar429_na_11a_requisicao_no_mesmo_minuto() throws Exception {
        String token = registrarELoginEObterAccessToken();

        // 10 primeiras: OK
        for (int i = 1; i <= 10; i++) {
            MvcResult r = mvc.perform(get("/api/v1/albuns")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andReturn();

            assertEquals(200, r.getResponse().getStatus(),
                    "Esperado 200 na requisição #" + i + " | body=" + r.getResponse().getContentAsString());
        }

        // 11ª: 429
        MvcResult r11 = mvc.perform(get("/api/v1/albuns")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andReturn();

        assertEquals(429, r11.getResponse().getStatus(),
                "Esperado 429 na 11ª requisição | body=" + r11.getResponse().getContentAsString());
    }

    private String registrarELoginEObterAccessToken() throws Exception {
        String senha = "Senha@123";
        String email = "rl_" + UUID.randomUUID().toString().replace("-", "") + "@teste.com";

        Map<String, Object> registrar = new LinkedHashMap<>();
        registrar.put("email", email);
        registrar.put("senha", senha);

        MvcResult rReg = mvc.perform(post("/api/v1/autenticacao/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(registrar)))
                .andReturn();

        int stReg = rReg.getResponse().getStatus();
        if (stReg < 200 || stReg >= 300) {
            throw new AssertionError("registrar falhou: status=" + stReg + " body=" + rReg.getResponse().getContentAsString());
        }

        Map<String, Object> login = new LinkedHashMap<>();
        login.put("email", email);
        login.put("senha", senha);

        MvcResult rLogin = mvc.perform(post("/api/v1/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(login)))
                .andReturn();

        assertEquals(200, rLogin.getResponse().getStatus(),
                "login falhou: status=" + rLogin.getResponse().getStatus() + " body=" + rLogin.getResponse().getContentAsString());

        JsonNode json = om.readTree(rLogin.getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }
}
