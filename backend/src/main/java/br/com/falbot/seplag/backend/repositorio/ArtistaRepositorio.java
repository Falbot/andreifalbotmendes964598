package br.com.falbot.seplag.backend.repositorio;

import br.com.falbot.seplag.backend.dominio.Artista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface ArtistaRepositorio extends JpaRepository<Artista, UUID>, JpaSpecificationExecutor<Artista> {}
