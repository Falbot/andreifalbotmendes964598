package br.com.falbot.seplag.backend.repositorio;

import br.com.falbot.seplag.backend.dominio.CapaAlbum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CapaAlbumRepositorio extends JpaRepository<CapaAlbum, UUID> {

    Optional<CapaAlbum> findByAlbumIdAndPrincipalTrue(UUID albumId);

    List<CapaAlbum> findAllByAlbumIdOrderByCriadoEmDesc(UUID albumId);

    Optional<CapaAlbum> findByIdAndAlbumId(UUID id, UUID albumId);

    Optional<CapaAlbum> findFirstByAlbumIdOrderByCriadoEmDesc(UUID albumId);

    @Modifying
    @Query("update CapaAlbum c set c.principal = false " +
           "where c.albumId = :albumId and c.principal = true")
    int desmarcarPrincipal(@Param("albumId") UUID albumId);

    @Modifying
    @Query("update CapaAlbum c set c.principal = true " +
           "where c.id = :capaId and c.albumId = :albumId")
    int marcarComoPrincipal(@Param("albumId") UUID albumId, @Param("capaId") UUID capaId);

    boolean existsByAlbumId(UUID albumId);
}
