package br.com.falbot.seplag.backend.api;

import br.com.falbot.seplag.backend.dominio.TipoArtista;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@SpringBootTest(properties = "app.rate-limit.enabled=false")
class CatalogoConsultasTests extends IntegrationTestBase {

    @Autowired MockMvc mvc;
    private final ObjectMapper om = new ObjectMapper().findAndRegisterModules();

    @Test
    void albuns_filtrar_por_tipo_artista_e_expor_flags_temCantor_temBanda() throws Exception {
        String accessToken = registrarELoginEObterAccessToken();

        String prefix = "q_" + UUID.randomUUID().toString().replace("-", "");

        UUID albumCantor = criarAlbum(accessToken, prefix + "_cantor");
        UUID albumBanda = criarAlbum(accessToken, prefix + "_banda");
        UUID albumAmbos = criarAlbum(accessToken, prefix + "_ambos");

        UUID cantor = criarArtista(accessToken, prefix + "_art_cantor", TipoArtista.CANTOR);
        UUID banda = criarArtista(accessToken, prefix + "_art_banda", TipoArtista.BANDA);

        vincular(accessToken, cantor, albumCantor);
        vincular(accessToken, cantor, albumAmbos);
        vincular(accessToken, banda, albumBanda);
        vincular(accessToken, banda, albumAmbos);

        //Filtra por CANTOR
        JsonNode pageCantor = listarAlbuns(accessToken, Map.of(
                "titulo", prefix,
                "tipoArtista", "CANTOR",
                "size", "50"
        ));
        Set<String> titulosCantor = titulos(pageCantor);
        assertTrue(titulosCantor.contains(prefix + "_cantor"));
        assertTrue(titulosCantor.contains(prefix + "_ambos"));
        assertFalse(titulosCantor.contains(prefix + "_banda"));

        //Confere flags
        assertFlagsAlbum(pageCantor, prefix + "_cantor", true, false);
        assertFlagsAlbum(pageCantor, prefix + "_ambos", true, true);

        //Filtra por BANDA
        JsonNode pageBanda = listarAlbuns(accessToken, Map.of(
                "titulo", prefix,
                "tipoArtista", "BANDA",
                "size", "50"
        ));
        Set<String> titulosBanda = titulos(pageBanda);
        assertTrue(titulosBanda.contains(prefix + "_banda"));
        assertTrue(titulosBanda.contains(prefix + "_ambos"));
        assertFalse(titulosBanda.contains(prefix + "_cantor"));

        assertFlagsAlbum(pageBanda, prefix + "_banda", false, true);
        assertFlagsAlbum(pageBanda, prefix + "_ambos", true, true);

        //Filtra por ambos (OR)
        JsonNode pageAmbos = listarAlbuns(accessToken, Map.of(
                "titulo", prefix,
                "tipoArtista", "CANTOR,BANDA",
                "size", "50"
        ));
        Set<String> titulosAmbos = titulos(pageAmbos);
        assertTrue(titulosAmbos.containsAll(Set.of(prefix + "_cantor", prefix + "_banda", prefix + "_ambos")));
    }

    @Test
    void artistas_consulta_por_nome_com_ordenacao_asc_desc() throws Exception {
        String accessToken = registrarELoginEObterAccessToken();

        String prefix = "ord_" + UUID.randomUUID().toString().replace("-", "");
        criarArtista(accessToken, prefix + "_aaa", TipoArtista.CANTOR);
        criarArtista(accessToken, prefix + "_bbb", TipoArtista.CANTOR);
        criarArtista(accessToken, prefix + "_ccc", TipoArtista.CANTOR);

        JsonNode asc = listarArtistas(accessToken, Map.of(
                "nome", prefix,
                "ordem", "asc",
                "size", "50"
        ));
        var ascNomes = nomes(asc);
        assertEquals(prefix + "_aaa", ascNomes.get(0));
        assertEquals(prefix + "_ccc", ascNomes.get(ascNomes.size() - 1));

        JsonNode desc = listarArtistas(accessToken, Map.of(
                "nome", prefix,
                "ordem", "desc",
                "size", "50"
        ));
        var descNomes = nomes(desc);
        assertEquals(prefix + "_ccc", descNomes.get(0));
        assertEquals(prefix + "_aaa", descNomes.get(descNomes.size() - 1));
    }

    // -------------------- helpers --------------------

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

        MvcResult r = mvc.perform(post("/api/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = om.readTree(r.getResponse().getContentAsString());
        return textoObrigatorio(json, "accessToken");
    }

    private UUID criarAlbum(String accessToken, String titulo) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("titulo", titulo);
        body.put("anoLancamento", 2024);

        MvcResult r = mvc.perform(post("/api/albuns")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = om.readTree(r.getResponse().getContentAsString());
        return UUID.fromString(textoObrigatorio(json, "id"));
    }

    private UUID criarArtista(String accessToken, String nome, TipoArtista tipo) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nome", nome);
        body.put("tipo", tipo.name());

        MvcResult r = mvc.perform(post("/api/artistas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = om.readTree(r.getResponse().getContentAsString());
        return UUID.fromString(textoObrigatorio(json, "id"));
    }

    private void vincular(String accessToken, UUID artistaId, UUID albumId) throws Exception {
        mvc.perform(post("/api/artistas/{id}/albuns/{albumId}", artistaId, albumId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNoContent());
    }

    private JsonNode listarAlbuns(String accessToken, Map<String, String> params) throws Exception {
        String query = params.entrySet().stream()
                .flatMap(e -> java.util.Arrays.stream(e.getValue().split(","))
                        .map(v -> e.getKey() + "=" + encode(v.trim())))
                .collect(Collectors.joining("&"));

        MvcResult r = mvc.perform(get("/api/albuns?" + query)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();
        return om.readTree(r.getResponse().getContentAsString());
    }

    private JsonNode listarArtistas(String accessToken, Map<String, String> params) throws Exception {
        String query = params.entrySet().stream()
                .flatMap(e -> java.util.Arrays.stream(e.getValue().split(","))
                        .map(v -> e.getKey() + "=" + encode(v.trim())))
                .collect(Collectors.joining("&"));

        MvcResult r = mvc.perform(get("/api/artistas?" + query)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();
        return om.readTree(r.getResponse().getContentAsString());
    }

    private static String encode(String v) {
        return v.replace(" ", "%20");
    }

    private static Set<String> titulos(JsonNode page) {
        return stream(page, "content").map(n -> n.get("titulo").asText()).collect(Collectors.toSet());
    }

    private static java.util.List<String> nomes(JsonNode page) {
        return stream(page, "content").map(n -> n.get("nome").asText()).toList();
    }

    private static java.util.stream.Stream<JsonNode> stream(JsonNode root, String arrayField) {
        JsonNode arr = root.get(arrayField);
        assertNotNull(arr, "campo '" + arrayField + "' não veio: " + root);
        assertTrue(arr.isArray(), "campo '" + arrayField + "' deveria ser array: " + arr);
        java.util.List<JsonNode> list = new java.util.ArrayList<>();
        arr.forEach(list::add);
        return list.stream();
    }

    private void assertFlagsAlbum(JsonNode page, String titulo, boolean temCantor, boolean temBanda) {
        JsonNode alvo = null;
        for (JsonNode n : page.get("content")) {
            if (titulo.equals(n.get("titulo").asText())) {
                alvo = n;
                break;
            }
        }
        assertNotNull(alvo, "album '" + titulo + "' não encontrado na resposta: " + page);
        assertEquals(temCantor, alvo.get("temCantor").asBoolean(), "temCantor incorreto: " + alvo);
        assertEquals(temBanda, alvo.get("temBanda").asBoolean(), "temBanda incorreto: " + alvo);
    }

    private static String textoObrigatorio(JsonNode json, String campo) {
        JsonNode n = json.get(campo);
        assertNotNull(n, "campo '" + campo + "' não veio na resposta: " + json);
        String v = n.asText();
        assertFalse(v == null || v.isBlank(), "campo '" + campo + "' veio vazio");
        return v;
    }
}
