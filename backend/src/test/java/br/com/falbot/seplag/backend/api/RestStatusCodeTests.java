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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RestStatusCodeTests extends IntegrationTestBase {

    @Autowired MockMvc mvc;
    private final ObjectMapper om = new ObjectMapper().findAndRegisterModules();

    @Test
    void postAlbuns_deveRetornar201_e_Location() throws Exception {
        String token = registrarELoginEObterAccessToken();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("titulo", "Album Teste");
        body.put("anoLancamento", 2024);

        MvcResult r = mvc.perform(post("/api/albuns")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andReturn();

        JsonNode json = om.readTree(r.getResponse().getContentAsString());
        assertNotNull(json.get("id"), "Resposta deveria conter 'id'");
    }

    @Test
    void postArtistas_deveRetornar201_e_Location() throws Exception {
        String token = registrarELoginEObterAccessToken();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nome", "Artista Teste");
        body.put("tipo", "CANTOR"); //Valores aceitos: CANTOR/BANDA

        MvcResult r = mvc.perform(post("/api/artistas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andReturn();

        JsonNode json = om.readTree(r.getResponse().getContentAsString());
        assertNotNull(json.get("id"), "Resposta deveria conter 'id'");
    }

    @Test
    void vincularAlbumAoArtista_deveRetornar204() throws Exception {
        String token = registrarELoginEObterAccessToken();

        UUID albumId = criarAlbum(token);
        UUID artistaId = criarArtista(token);

        mvc.perform(post("/api/artistas/{id}/albuns/{albumId}", artistaId, albumId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteAlbum_deveRetornar204() throws Exception {
        String token = registrarELoginEObterAccessToken();
        UUID albumId = criarAlbum(token);

        mvc.perform(delete("/api/albuns/{id}", albumId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    // ---------------- helpers ----------------

    private String registrarELoginEObterAccessToken() throws Exception {
        String senha = "Senha@123";
        String email = "rest_" + UUID.randomUUID().toString().replace("-", "") + "@teste.com";

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

        MvcResult r = mvc.perform(post("/api/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = om.readTree(r.getResponse().getContentAsString());
        String token = json.get("accessToken").asText();
        assertTrue(token != null && !token.isBlank(), "accessToken não pode ser vazio");
        return token;
    }

    private UUID criarAlbum(String token) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("titulo", "Album Aux");
        body.put("anoLancamento", 2024);

        MvcResult r = mvc.perform(post("/api/albuns")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = om.readTree(r.getResponse().getContentAsString());
        return UUID.fromString(json.get("id").asText());
    }

    private UUID criarArtista(String token) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nome", "Artista Aux");
        body.put("tipo", "BANDA");

        MvcResult r = mvc.perform(post("/api/artistas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = om.readTree(r.getResponse().getContentAsString());
        return UUID.fromString(json.get("id").asText());
    }
}
