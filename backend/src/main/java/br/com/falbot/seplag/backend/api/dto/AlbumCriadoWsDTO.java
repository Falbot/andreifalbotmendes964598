package br.com.falbot.seplag.backend.api.dto;
import java.util.UUID;
public record AlbumCriadoWsDTO(
  UUID id,
  String titulo,
  Integer anoLancamento
) {}
