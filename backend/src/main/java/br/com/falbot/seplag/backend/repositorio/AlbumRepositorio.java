package br.com.falbot.seplag.backend.repositorio;

import br.com.falbot.seplag.backend.dominio.Album;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface AlbumRepositorio extends JpaRepository<Album, UUID>, JpaSpecificationExecutor<Album> {

    @EntityGraph(attributePaths = "artistas")
    Page<Album> findAll(Specification<Album> spec, Pageable pageable);
    @EntityGraph(attributePaths = "artistas")
    Optional<Album> findById(UUID id);


    @EntityGraph(attributePaths = "artistas")
    @Query("""
        select al from Artista ar
        join ar.albuns al
        where ar.id = :artistaId
        order by al.titulo
    """)
    List<Album> listarPorArtistaId(@Param("artistaId") UUID artistaId);

    @Query("""
        select count(al) from Artista ar
        join ar.albuns al
        where ar.id = :artistaId and al.id = :albumId
    """)
    long contarVinculo(@Param("artistaId") UUID artistaId, @Param("albumId") UUID albumId);
}
