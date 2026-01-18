package br.com.falbot.seplag.backend.api;
import br.com.falbot.seplag.backend.config.JwtProps;
import br.com.falbot.seplag.backend.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.http.HttpHeaders;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Date;
import java.util.UUID;

class SecurityAuthorizationTests extends IntegrationTestBase {

    @Autowired MockMvc mvc;
    @Autowired JwtProps jwtProps;
    private final ObjectMapper om = new ObjectMapper().findAndRegisterModules();

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

    @Test
    void rotaProtegida_comTokenExpirado_deveRetornar401() throws Exception {
        String tokenExpirado = gerarTokenExpirado();

        mvc.perform(get("/api/albuns")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenExpirado))
            .andExpect(status().isUnauthorized());
    }

    private String gerarTokenExpirado() {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProps.secret));

        Instant agora = Instant.now();
        Instant exp = agora.minusSeconds(60); // já expirado

        return Jwts.builder()
                .issuer(jwtProps.issuer)
                .subject(UUID.randomUUID().toString())
                .claim("email", "expirado@teste.com")
                .issuedAt(Date.from(agora.minusSeconds(120)))
                .expiration(Date.from(exp))
                .signWith(key)
                .id(UUID.randomUUID().toString())
                .compact();
    }

    @Test
    void rotaProtegida_comTokenValido_deveRetornar200() throws Exception {
        String accessToken = registrarELoginEObterAccessToken();

        mvc.perform(get("/api/albuns")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
            .andExpect(status().isOk());
    }

    @Test
    void registrar_semToken_deveSerPermitido() throws Exception {
        Map<String, Object> registrar = new LinkedHashMap<>();
        registrar.put("email", "pub_" + UUID.randomUUID().toString().replace("-", "") + "@teste.com");
        registrar.put("senha", "Senha@123");

        mvc.perform(post("/api/autenticacao/registrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(registrar)))
            .andExpect(status().is2xxSuccessful());
    }

    @Test
    void login_semToken_deveSerPermitido() throws Exception {
        String senha = "Senha@123";
        String email = "pub_login_" + UUID.randomUUID().toString().replace("-", "") + "@teste.com";

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

        mvc.perform(post("/api/autenticacao/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(login)))
            .andExpect(status().isOk());
    }

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
        return json.get("accessToken").asText();
    }

}
