package br.com.falbot.seplag.backend.servico;

import br.com.falbot.seplag.backend.api.dto.AlbumCriadoWsDTO;
import br.com.falbot.seplag.backend.dominio.Album;
import br.com.falbot.seplag.backend.repositorio.AlbumRepositorio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlbumServicoWsTests {

  @Mock AlbumRepositorio albumRepo;
  @Mock AlbumWsPublisher wsPublisher;

  @InjectMocks AlbumServico servico;

  @Test
  void criar_devePublicarEventoWs() {
    UUID id = UUID.randomUUID();

    Album salvo = new Album();
    salvo.setTitulo("Hybrid Theory");
    salvo.setAnoLancamento(2000);
    ReflectionTestUtils.setField(salvo, "id", id); //Album não tem setId()

    when(albumRepo.save(any(Album.class))).thenReturn(salvo);

    servico.criar("Hybrid Theory", 2000);

    verify(wsPublisher).notificarNovoAlbum(new AlbumCriadoWsDTO(id, "Hybrid Theory", 2000));
  }
}
