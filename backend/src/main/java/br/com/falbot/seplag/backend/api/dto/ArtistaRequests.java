package br.com.falbot.seplag.backend.api.dto;

import br.com.falbot.seplag.backend.dominio.TipoArtista;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ArtistaRequests {

    public record Criar(
            @NotBlank String nome,
            @NotNull TipoArtista tipo
    ) {}

    public record Atualizar(
            @NotBlank String nome,
            @NotNull TipoArtista tipo
    ) {}
}
