package br.com.falbot.seplag.backend.api;

import br.com.falbot.seplag.backend.servico.IntegradorArgusClient;
import br.com.falbot.seplag.backend.support.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RegionaisSyncFlowTests extends IntegrationTestBase {

    @Autowired MockMvc mvc;
    @MockitoBean IntegradorArgusClient integrador;

    private final ObjectMapper om = new ObjectMapper().findAndRegisterModules();

    @Test
    void sincronizar_deve_inserir_inativar_e_versionar_quando_nome_muda() throws Exception {
        String token = registrarELoginEObterAccessToken();

        //1ª sync: cria 2 ativas
        when(integrador.listarRegionais()).thenReturn(List.of(
                new IntegradorArgusClient.RegionalExternaDTO(1, "Cuiabá"),
                new IntegradorArgusClient.RegionalExternaDTO(2, "Várzea Grande")
        ));

        mvc.perform(post("/api/v1/regionais/sincronizar")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        JsonNode ativas1 = om.readTree(mvc.perform(get("/api/v1/regionais?ativo=true")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        assertEquals(2, ativas1.size());

        //2ª sync: id=2 some (inativa) / id=1 muda nome (inativa + cria novo) / id=3 novo
        when(integrador.listarRegionais()).thenReturn(List.of(
                new IntegradorArgusClient.RegionalExternaDTO(1, "Cuiabá Centro"),
                new IntegradorArgusClient.RegionalExternaDTO(3, "Sinop")
        ));

        mvc.perform(post("/api/v1/regionais/sincronizar")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        JsonNode ativas2 = om.readTree(mvc.perform(get("/api/v1/regionais?ativo=true")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        assertEquals(2, ativas2.size());
        assertTrue(contemNome(ativas2, "Cuiabá Centro"));
        assertTrue(contemNome(ativas2, "Sinop"));

        JsonNode inativas = om.readTree(mvc.perform(get("/api/v1/regionais?ativo=false")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        assertTrue(contemNome(inativas, "Cuiabá"));
        assertTrue(contemNome(inativas, "Várzea Grande"));
    }

    //helpers
    private String registrarELoginEObterAccessToken() throws Exception {
        String senha = "Senha@123";
        String email = "user_" + UUID.randomUUID().toString().replace("-", "") + "@teste.com";

        Map<String, Object> registrar = new LinkedHashMap<>();
        registrar.put("email", email);
        registrar.put("senha", senha);

        mvc.perform(post("/api/autenticacao/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(registrar)))
                .andExpect(status().is2xxSuccessful());

        Map<String, Object> login = new LinkedHashMap<>();
        login.put("email", email);
        login.put("senha", senha);

        var r = mvc.perform(post("/api/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = om.readTree(r.getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }

    private static boolean contemNome(JsonNode arr, String nome) {
        for (JsonNode n : arr) {
            if (nome.equals(n.get("nome").asText())) return true;
        }
        return false;
    }
}
