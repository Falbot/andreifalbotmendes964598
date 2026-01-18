package br.com.falbot.seplag.backend.repositorio;

import br.com.falbot.seplag.backend.dominio.CapaAlbum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CapaAlbumRepositorio extends JpaRepository<CapaAlbum, UUID> {
    Optional<CapaAlbum> findByAlbumId(UUID albumId);
}
