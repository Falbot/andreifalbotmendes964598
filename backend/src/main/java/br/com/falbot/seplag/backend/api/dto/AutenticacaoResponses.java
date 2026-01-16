package br.com.falbot.seplag.backend.api.dto;

public class AutenticacaoResponses {
    public record Token(
            String accessToken,
            String refreshToken,
            long expiresInSeconds,
            String tokenType
    ) {}
}
