package br.com.falbot.seplag.backend.repositorio;

import br.com.falbot.seplag.backend.dominio.Regional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RegionalRepositorio extends JpaRepository<Regional, UUID> {

    List<Regional> findByAtivoTrueOrderByNomeAsc();
    List<Regional> findByAtivoFalseOrderByNomeAsc();

    List<Regional> findByAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc(String nome);
    List<Regional> findByAtivoFalseAndNomeContainingIgnoreCaseOrderByNomeAsc(String nome);

    Optional<Regional> findFirstByIdExternoAndAtivoTrue(Integer idExterno);

    List<Regional> findByAtivoTrueAndIdExternoIsNotNull();
}
