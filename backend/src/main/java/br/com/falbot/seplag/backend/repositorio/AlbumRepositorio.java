package br.com.falbot.seplag.backend.repositorio;

import br.com.falbot.seplag.backend.dominio.Album;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AlbumRepositorio extends JpaRepository<Album, UUID>, JpaSpecificationExecutor<Album> {

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
