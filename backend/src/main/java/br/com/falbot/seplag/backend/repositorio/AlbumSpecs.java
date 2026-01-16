package br.com.falbot.seplag.backend.repositorio;

import br.com.falbot.seplag.backend.dominio.Album;
import org.springframework.data.jpa.domain.Specification;

public class AlbumSpecs {

    public static Specification<Album> tituloContem(String titulo) {
        return (root, query, cb) ->
                titulo == null || titulo.isBlank()
                        ? cb.conjunction()
                        : cb.like(cb.lower(root.get("titulo")), "%" + titulo.toLowerCase() + "%");
    }

    public static Specification<Album> anoIgual(Integer ano) {
        return (root, query, cb) ->
                ano == null ? cb.conjunction() : cb.equal(root.get("anoLancamento"), ano);
    }
}
