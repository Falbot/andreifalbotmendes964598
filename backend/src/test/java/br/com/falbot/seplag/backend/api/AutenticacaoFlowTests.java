package br.com.falbot.seplag.backend.api;

import br.com.falbot.seplag.backend.support.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class AutenticacaoFlowTests extends IntegrationTestBase {
    
    @Autowired MockMvc mvc;
    private final ObjectMapper om = new ObjectMapper().findAndRegisterModules();
    
    @Test
    void fluxo_registrar_login_renovar_e_protegida_sem_token() throws Exception {
        //Dados únicos pra evitar conflito entre execuções
        //String cpf = gerarCpfValido();
        String senha = "Senha@123";
        String email = "user_" + UUID.randomUUID().toString().replace("-", "") + "@teste.com";
        //String nome = "Usuario Teste" + + UUID.randomUUID().toString().replace("-", "");

        //1) Registrar
        Map<String, Object> registrar = new LinkedHashMap<>();
        //registrar.put("cpf", cpf);
        //registrar.put("nome", nome);
        registrar.put("email", email);
        registrar.put("senha", senha);

        MvcResult r1 = mvc.perform(post("/api/autenticacao/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(registrar)))
                .andReturn();

        assertStatus2xxOuFalhaComBody("registrar", r1);

        //2) Login
        Map<String, Object> login = new LinkedHashMap<>();
        login.put("email", email);
        login.put("senha", senha);

        MvcResult r2 = mvc.perform(post("/api/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(login)))
                .andReturn();

        assertEquals(200, r2.getResponse().getStatus(),
                "login falhou: status=" + r2.getResponse().getStatus() + " body=" + r2.getResponse().getContentAsString());

        JsonNode loginJson = om.readTree(r2.getResponse().getContentAsString());
        String accessToken = textoObrigatorio(loginJson, "accessToken");
        String refreshToken = textoObrigatorio(loginJson, "refreshToken");

        //3) Rota protegida SEM token -> 401 ou 403
        MvcResult r3 = mvc.perform(get("/api/artistas"))
                .andReturn();

        int st3 = r3.getResponse().getStatus();
        assertTrue(st3 == 401 || st3 == 403,
                "protegida sem token deveria ser 401/403, mas foi " + st3 + " body=" + r3.getResponse().getContentAsString());

        //4) Renovar (refresh) -> novo access token
        Map<String, Object> renovar = new LinkedHashMap<>();
        renovar.put("refreshToken", refreshToken);

        MvcResult r4 = mvc.perform(post("/api/autenticacao/renovar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(renovar)))
                .andReturn();

        assertEquals(200, r4.getResponse().getStatus(),
                "renovar falhou: status=" + r4.getResponse().getStatus() + " body=" + r4.getResponse().getContentAsString());

        JsonNode renovarJson = om.readTree(r4.getResponse().getContentAsString());
        String novoAccessToken = textoObrigatorio(renovarJson, "accessToken");

        assertNotEquals(accessToken, novoAccessToken, "accessToken deveria mudar após renovar()");
    }

    private static void assertStatus2xxOuFalhaComBody(String etapa, MvcResult r) throws Exception {
        int st = r.getResponse().getStatus();
        if (st < 200 || st >= 300) {
            fail(etapa + " falhou: status=" + st + " body=" + r.getResponse().getContentAsString());
        }
    }

    private static String textoObrigatorio(JsonNode json, String campo) {
        JsonNode n = json.get(campo);
        assertNotNull(n, "campo '" + campo + "' não veio na resposta: " + json);
        String v = n.asText();
        assertFalse(v == null || v.isBlank(), "campo '" + campo + "' veio vazio");
        return v;
    }

    //Gera CPF válido
    private static String gerarCpfValido() {
        int[] n = new int[9];
        for (int i = 0; i < 9; i++) n[i] = (int) (Math.random() * 10);

        int d1 = calcDigito(n, 10);
        int[] n2 = new int[10];
        System.arraycopy(n, 0, n2, 0, 9);
        n2[9] = d1;
        int d2 = calcDigito(n2, 11);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 9; i++) sb.append(n[i]);
        sb.append(d1).append(d2);
        return sb.toString();
    }

    private static int calcDigito(int[] nums, int pesoInicial) {
        int soma = 0;
        int peso = pesoInicial;
        for (int num : nums) soma += num * (peso--);
        int mod = soma % 11;
        return (mod < 2) ? 0 : 11 - mod;
    }
}
