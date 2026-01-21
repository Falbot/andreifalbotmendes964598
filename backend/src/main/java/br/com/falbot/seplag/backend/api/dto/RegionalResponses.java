package br.com.falbot.seplag.backend.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class RegionalResponses {

    public record RegionalResponse(
            UUID idRegional,
            Integer id,
            String nome,
            boolean ativo,
            OffsetDateTime criadoEm
    ) {}

    public record SyncResponse(
            int inseridos,
            int inativados,
            int alterados,
            int totalAtivos,
            int totalRecebidos
    ) {}
}
