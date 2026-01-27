package br.com.falbot.seplag.backend.servico;

import tools.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

@Component
public class IntegradorArgusClient {

    public record RegionalExternaDTO(Integer id, String nome) {}

    private final RestClient rc;

    public IntegradorArgusClient(
            RestClient.Builder builder,
            @Value("${app.integrador-argus.base-url:https://integrador-argus-api.geia.vip}") String baseUrl
    ) {
        this.rc = builder.baseUrl(baseUrl).build();
    }

    public List<RegionalExternaDTO> listarRegionais() {
        JsonNode root = rc.get()
                .uri("/v1/regionais")
                .retrieve()
                .body(JsonNode.class);

        JsonNode arr = extrairArray(root);

        // Dedup por id (se vier repetido, fica o último)
        Map<Integer, RegionalExternaDTO> map = new LinkedHashMap<>();
        for (JsonNode n : arr) {
            Integer id = lerInt(n, "id", "codigo", "regionalId");
            String nome = lerText(n, "nome", "descricao", "name", "regionalNome");

            if (id == null || nome == null || nome.isBlank()) continue;
            map.put(id, new RegionalExternaDTO(id, nome.trim()));
        }
        return new ArrayList<>(map.values());
    }

    private static JsonNode extrairArray(JsonNode root) {
        if (root == null) throw new IllegalStateException("Resposta vazia do integrador");
        if (root.isArray()) return root;
        if (root.has("content") && root.get("content").isArray()) return root.get("content");
        if (root.has("data") && root.get("data").isArray()) return root.get("data");
        throw new IllegalStateException("Formato inesperado no JSON do integrador");
    }

    private static Integer lerInt(JsonNode n, String... campos) {
        for (String c : campos) {
            JsonNode v = n.get(c);
            if (v == null || v.isNull()) continue;
            if (v.isInt()) return v.asInt();
            if (v.isTextual()) {
                try { return Integer.parseInt(v.asText().trim()); } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private static String lerText(JsonNode n, String... campos) {
        for (String c : campos) {
            JsonNode v = n.get(c);
            if (v == null || v.isNull()) continue;
            if (v.isTextual()) return v.asText();
        }
        return null;
    }
}
