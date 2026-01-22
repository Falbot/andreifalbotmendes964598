package br.com.falbot.seplag.backend.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public class AlbumRequests {

    @Schema(name = "AlbumCriarRequest")
    public record Criar(@NotBlank String titulo, Integer anoLancamento)
    {}
    
    @Schema(name = "AlbumAtualizarRequest")
    public record Atualizar(@NotBlank String titulo, Integer anoLancamento)
    {}
}
