package br.com.falbot.seplag.backend.repositorio;

import br.com.falbot.seplag.backend.dominio.Artista;
import br.com.falbot.seplag.backend.dominio.TipoArtista;
import org.springframework.data.jpa.domain.Specification;

public class ArtistaSpecs {

    public static Specification<Artista> nomeContem(String nome) {
        return (root, query, cb) ->
                nome == null || nome.isBlank()
                        ? cb.conjunction()
                        : cb.like(cb.lower(root.get("nome")), "%" + nome.toLowerCase() + "%");
    }

    public static Specification<Artista> tipoIgual(TipoArtista tipo) {
        return (root, query, cb) ->
                tipo == null ? cb.conjunction() : cb.equal(root.get("tipo"), tipo);
    }
}
