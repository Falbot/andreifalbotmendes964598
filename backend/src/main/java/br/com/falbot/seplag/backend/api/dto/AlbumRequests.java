package br.com.falbot.seplag.backend.api.dto;

import jakarta.validation.constraints.NotBlank;

public class AlbumRequests {

    public record Criar(
            @NotBlank String titulo,
            Integer anoLancamento
    ) {}

    public record Atualizar(
            @NotBlank String titulo,
            Integer anoLancamento
    ) {}
}
