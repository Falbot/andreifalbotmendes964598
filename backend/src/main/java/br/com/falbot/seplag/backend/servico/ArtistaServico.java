package br.com.falbot.seplag.backend.servico;

import br.com.falbot.seplag.backend.dominio.Album;
import br.com.falbot.seplag.backend.dominio.Artista;
import br.com.falbot.seplag.backend.dominio.TipoArtista;
import br.com.falbot.seplag.backend.repositorio.AlbumRepositorio;
import br.com.falbot.seplag.backend.repositorio.ArtistaRepositorio;
import br.com.falbot.seplag.backend.repositorio.ArtistaSpecs;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ArtistaServico {

    private final ArtistaRepositorio artistaRepo;
    private final AlbumRepositorio albumRepo;

    public ArtistaServico(ArtistaRepositorio artistaRepo, AlbumRepositorio albumRepo) {
        this.artistaRepo = artistaRepo;
        this.albumRepo = albumRepo;
    }

    public Page<Artista> listar(String nome, TipoArtista tipo, Pageable pageable) {
        Specification<Artista> spec = Specification
                .where(ArtistaSpecs.nomeContem(nome))
                .and(ArtistaSpecs.tipoIgual(tipo));
        return artistaRepo.findAll(spec, pageable);
    }

    public Artista obter(UUID id) {
        return artistaRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Artista não encontrado!"));
    }

    @Transactional
    public Artista criar(String nome, TipoArtista tipo) {
        var a = new Artista();
        a.setNome(nome);
        a.setTipo(tipo);
        return artistaRepo.save(a);
    }

    @Transactional
    public Artista atualizar(UUID id, String nome, TipoArtista tipo) {
        var a = obter(id);
        a.setNome(nome);
        a.setTipo(tipo);
        return a;
    }

    @Transactional
    public void excluir(UUID id) {
        if (!artistaRepo.existsById(id)) throw new EntityNotFoundException("Artista não encontrado!");
        artistaRepo.deleteById(id);
    }

    @Transactional
    public void vincularAlbum(UUID artistaId, UUID albumId) {
        Artista artista = obter(artistaId);
        Album album = albumRepo.findById(albumId)
                .orElseThrow(() -> new EntityNotFoundException("Álbum não encontrado!"));

        if (albumRepo.contarVinculo(artistaId, albumId) == 0) {
            artista.getAlbuns().add(album);
        }
    }

    @Transactional
    public void desvincularAlbum(UUID artistaId, UUID albumId) {
        Artista artista = obter(artistaId);
        artista.getAlbuns().removeIf(a -> a.getId().equals(albumId));
    }

    @Transactional(readOnly = true)
    public List<Album> listarAlbuns(UUID artistaId) {
        this.obter(artistaId); // garante 404 se artista não existir
        return albumRepo.listarPorArtistaId(artistaId);
    }

}
