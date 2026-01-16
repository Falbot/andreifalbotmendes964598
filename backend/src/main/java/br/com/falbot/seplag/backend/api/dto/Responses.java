package br.com.falbot.seplag.backend.api.dto;

import br.com.falbot.seplag.backend.dominio.TipoArtista;

import java.time.OffsetDateTime;
import java.util.UUID;

public class Responses {

    public record ArtistaResponse(
            UUID id,
            String nome,
            TipoArtista tipo,
            OffsetDateTime criadoEm,
            OffsetDateTime atualizadoEm
    ) {}

    public record AlbumResponse(
            UUID id,
            String titulo,
            Integer anoLancamento,
            OffsetDateTime criadoEm,
            OffsetDateTime atualizadoEm
    ) {}
}
