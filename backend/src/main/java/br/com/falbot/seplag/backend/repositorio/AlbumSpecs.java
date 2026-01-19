package br.com.falbot.seplag.backend.repositorio;

import br.com.falbot.seplag.backend.dominio.Album;
import br.com.falbot.seplag.backend.dominio.TipoArtista;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.JoinType;
import java.util.Set;

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
    public static Specification<Album> temArtistaDoTipo(Set<TipoArtista> tipos) {
        return (root, query, cb) -> {
            if (tipos == null || tipos.isEmpty()) return cb.conjunction();
            query.distinct(true);
            var j = root.join("artistas", JoinType.INNER);
            return j.get("tipo").in(tipos);
        };
    }

}
