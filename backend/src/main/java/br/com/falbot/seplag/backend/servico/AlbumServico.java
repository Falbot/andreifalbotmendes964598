package br.com.falbot.seplag.backend.servico;

import br.com.falbot.seplag.backend.dominio.Album;
import br.com.falbot.seplag.backend.repositorio.AlbumRepositorio;
import br.com.falbot.seplag.backend.repositorio.AlbumSpecs;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AlbumServico {

    private final AlbumRepositorio albumRepo;

    public AlbumServico(AlbumRepositorio albumRepo) {
        this.albumRepo = albumRepo;
    }

    public Page<Album> listar(String titulo, Integer ano, Pageable pageable) {
        Specification<Album> spec = Specification
                .where(AlbumSpecs.tituloContem(titulo))
                .and(AlbumSpecs.anoIgual(ano));
        return albumRepo.findAll(spec, pageable);
    }

    public Album obter(UUID id) {
        return albumRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Álbum não encontrado!"));
    }

    @Transactional
    public Album criar(String titulo, Integer anoLancamento) {
        var a = new Album();
        a.setTitulo(titulo);
        a.setAnoLancamento(anoLancamento);
        return albumRepo.save(a);
    }

    @Transactional
    public Album atualizar(UUID id, String titulo, Integer anoLancamento) {
        var a = obter(id);
        a.setTitulo(titulo);
        a.setAnoLancamento(anoLancamento);
        return a;
    }

    @Transactional
    public void excluir(UUID id) {
        if (!albumRepo.existsById(id)) throw new EntityNotFoundException("Álbum não encontrado!");
        albumRepo.deleteById(id);
    }
}
