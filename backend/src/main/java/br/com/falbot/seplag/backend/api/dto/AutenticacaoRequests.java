package br.com.falbot.seplag.backend.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AutenticacaoRequests {

    public record Registrar(
            @Email @NotBlank String email,
            @NotBlank String senha
    ) {}

    public record Login(
            @Email @NotBlank String email,
            @NotBlank String senha
    ) {}

    public record Renovar(
            @NotBlank String refreshToken
    ) {}
}
