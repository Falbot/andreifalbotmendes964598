package br.com.falbot.seplag.backend.api;

import br.com.falbot.seplag.backend.support.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CapaAlbumFlowTests extends IntegrationTestBase {

    @Autowired MockMvc mvc;
    private final ObjectMapper om = new ObjectMapper().findAndRegisterModules();


    @Test
    void upload_em_lote_gera_link_presignado_e_download_funciona() throws Exception {
        // 1) registra + login
        String email = "user_" + UUID.randomUUID().toString().replace("-", "") + "@teste.com";
        String senha = "Senha@123";

        mvc.perform(post("/api/autenticacao/registrar")
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isOk());

        var loginRes = mvc.perform(post("/api/autenticacao/login")
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = om.readTree(loginRes.getResponse().getContentAsString());
        String token = loginJson.get("accessToken").asText();

        // 2) cria álbum
        var criarAlbumRes = mvc.perform(post("/api/albuns")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"titulo\":\"alb_" + UUID.randomUUID() + "\",\"anoLancamento\":2000}"))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode albumJson = om.readTree(criarAlbumRes.getResponse().getContentAsString());
        String albumId = albumJson.get("id").asText();

        // 3) upload em lote: POST /api/albuns/{id}/capas (multipart, part name = "arquivos")
        byte[] conteudo = "conteudo_teste_capa".getBytes(StandardCharsets.UTF_8);

        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivos",               // <-- TEM que ser "arquivos"
                "capa.png",
                "image/png",
                conteudo
        );

        var uploadRes = mvc.perform(multipart("/api/albuns/{id}/capas", albumId)
                        .file(arquivo)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode uploadJson = om.readTree(uploadRes.getResponse().getContentAsString());
        assertTrue(uploadJson.isArray(), "Resposta do upload deve ser um array");
        assertTrue(uploadJson.size() >= 1, "Upload deveria retornar pelo menos 1 capa");

        String capaId = uploadJson.get(0).get("id").asText();
        String urlUpload = uploadJson.get(0).get("url").asText();
        assertNotBlank(urlUpload);

        // 4) obter link por id: GET /api/albuns/{id}/capas/{capaId}
        var linkRes = mvc.perform(get("/api/albuns/{id}/capas/{capaId}", albumId, capaId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode linkJson = om.readTree(linkRes.getResponse().getContentAsString());
        String presignedUrl = linkJson.get("url").asText();
        assertNotBlank(presignedUrl);

        // 5) download REAL via HTTP (não use MockMvc pra isso)
System.out.println("minioEndpoint() esperado = " + minioEndpoint());
System.out.println("presignedUrl retornado  = " + presignedUrl);

        assertPresignedDownloadOk(presignedUrl);
    }

    private void assertPresignedDownloadOk(String presignedUrl) throws Exception {
        HttpClient http = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(presignedUrl))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();

        HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());

        if (resp.statusCode() != 200) {
            // Debug bem direto: checar se o objeto existe no bucket, e listar prefixo
            String key = extrairKeyDoPresignedUrl(presignedUrl);

            String debug = "";
            try (S3Client s3 = s3Client()) {
                try {
                    s3.headObject(HeadObjectRequest.builder()
                            .bucket(MINIO_BUCKET)
                            .key(key)
                            .build());
                    debug += "HEAD OK (objeto existe no bucket).";
                } catch (S3Exception e) {
                    debug += "HEAD falhou: status=" + e.statusCode() + " msg=" + e.getMessage();

                    // lista o prefixo do álbum pra ver se salvou em outra key
                    String prefixoAlbum = key.contains("/capas/") ? key.substring(0, key.indexOf("/capas/") + 7) : "";
                    var listed = s3.listObjectsV2(ListObjectsV2Request.builder()
                                    .bucket(MINIO_BUCKET)
                                    .prefix(prefixoAlbum)
                                    .build())
                            .contents().stream()
                            .map(o -> o.key())
                            .collect(Collectors.toList());

                    debug += " | Objetos no prefixo '" + prefixoAlbum + "': " + listed;
                }
            }

            fail("Download pelo presigned URL não retornou 200. status=" + resp.statusCode()
                    + " | key=" + key + " | " + debug);
        }

        assertTrue(resp.body().length > 0, "Download retornou 200 mas veio vazio");
    }

    private static String extrairKeyDoPresignedUrl(String url) {
        URI uri = URI.create(url);
        String path = uri.getPath(); // ex: /app-bucket/albuns/<albumId>/capas/<objId>
        String prefix = "/" + MINIO_BUCKET + "/";
        int i = path.indexOf(prefix);
        if (i < 0) {
            // fallback: remove leading "/"
            return path.startsWith("/") ? path.substring(1) : path;
        }
        return path.substring(i + prefix.length());
    }

    private static S3Client s3Client() {
        return S3Client.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .endpointOverride(URI.create(minioEndpoint()))
                .region(Region.of(MINIO_REGION))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(MINIO_ACCESS_KEY, MINIO_SECRET_KEY)
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    private static void assertNotBlank(String s) {
        assertNotNull(s, "String não pode ser null");
        assertFalse(s.isBlank(), "String não pode estar em branco");
    }
}
