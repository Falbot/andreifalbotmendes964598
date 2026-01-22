package br.com.falbot.seplag.backend.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import br.com.falbot.seplag.backend.dominio.TipoArtista;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ArtistaRequests {

    @Schema(name = "ArtistaCriarRequest")
    public record Criar(@NotBlank String nome, @NotNull TipoArtista tipo)
    {}

    @Schema(name = "ArtistaAtualizarRequest")
    public record Atualizar(@NotBlank String nome, @NotNull TipoArtista tipo)
    {}
}
