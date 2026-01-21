package br.com.falbot.seplag.backend.servico;

import br.com.falbot.seplag.backend.api.dto.AlbumCriadoWsDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class AlbumWsPublisher {

  private final SimpMessagingTemplate messaging;

  public AlbumWsPublisher(SimpMessagingTemplate messaging) {
    this.messaging = messaging;
  }

  public void notificarNovoAlbum(AlbumCriadoWsDTO dto) {
    messaging.convertAndSend("/topic/albuns", dto);
  }
}
