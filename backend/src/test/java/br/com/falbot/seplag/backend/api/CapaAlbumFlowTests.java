package br.com.falbot.seplag.backend.api;

import br.com.falbot.seplag.backend.support.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CapaAlbumFlowTests extends IntegrationTestBase {

    @Autowired MockMvc mvc;
    private final ObjectMapper om = new ObjectMapper().findAndRegisterModules();

    @Test
    void upload_gera_link_presignado_30min_e_download_funciona() throws Exception {
        Tokens t = registrarELogin();
        UUID albumId = criarAlbum(t.accessToken);

        byte[] conteudo = "fake-jpeg-bytes".getBytes(StandardCharsets.UTF_8);
        var file = new MockMultipartFile("arquivo", "capa.jpg", "image/jpeg", conteudo);

        MvcResult r = mvc.perform(
                        multipart("/api/albuns/{id}/capa", albumId)
                                .file(file)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + t.accessToken)
                )
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = om.readTree(r.getResponse().getContentAsString());
        String url = textoObrigatorio(json, "url");
        String expiraEm = textoObrigatorio(json, "expiraEm");

        assertTrue(url.contains("X-Amz-Expires=1800"), "URL deveria expirar em 1800s (30min): " + url);
        assertNotNull(Instant.parse(expiraEm), "expiraEm deve ser Instant ISO-8601: " + expiraEm);

        //Baixa imagem do MinIO via link presignado
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());

        assertEquals(200, resp.statusCode(), "download do presigned deveria ser 200");
        assertArrayEquals(conteudo, resp.body(), "conteúdo baixado deve ser igual ao enviado");
    }

    @Test
    void delete_capa_depois_link_deve_ser_404() throws Exception {
        Tokens t = registrarELogin();
        UUID albumId = criarAlbum(t.accessToken);

        byte[] conteudo = "img".getBytes(StandardCharsets.UTF_8);
        var file = new MockMultipartFile("arquivo", "capa.jpg", "image/jpeg", conteudo);

        mvc.perform(
                multipart("/api/albuns/{id}/capa", albumId)
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + t.accessToken)
        ).andExpect(status().isOk());

        mvc.perform(
                delete("/api/albuns/{id}/capa", albumId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + t.accessToken)
        ).andExpect(status().isNoContent());

        mvc.perform(
                get("/api/albuns/{id}/capa/link", albumId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + t.accessToken)
        ).andExpect(status().isNotFound());
    }

    @Test
    void upload_com_content_type_invalido_deve_ser_400() throws Exception {
        Tokens t = registrarELogin();
        UUID albumId = criarAlbum(t.accessToken);

        var file = new MockMultipartFile("arquivo", "capa.txt", "text/plain", "x".getBytes(StandardCharsets.UTF_8));

        mvc.perform(
                multipart("/api/albuns/{id}/capa", albumId)
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + t.accessToken)
        ).andExpect(status().isBadRequest());
    }

    private Tokens registrarELogin() throws Exception {
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

        MvcResult r = mvc.perform(post("/api/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = om.readTree(r.getResponse().getContentAsString());
        return new Tokens(textoObrigatorio(json, "accessToken"), textoObrigatorio(json, "refreshToken"));
    }

    private UUID criarAlbum(String accessToken) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("titulo", "Album Teste");
        body.put("anoLancamento", 2024);

        MvcResult r = mvc.perform(post("/api/albuns")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = om.readTree(r.getResponse().getContentAsString());
        String id = textoObrigatorio(json, "id");
        return UUID.fromString(id);
    }

    private static String textoObrigatorio(JsonNode json, String campo) {
        JsonNode n = json.get(campo);
        assertNotNull(n, "campo '" + campo + "' não veio na resposta: " + json);
        String v = n.asText();
        assertFalse(v == null || v.isBlank(), "campo '" + campo + "' veio vazio");
        return v;
    }

    private record Tokens(String accessToken, String refreshToken) {}
}
