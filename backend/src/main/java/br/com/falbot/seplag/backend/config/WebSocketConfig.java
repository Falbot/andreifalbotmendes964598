package br.com.falbot.seplag.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.beans.factory.annotation.Value;
import java.util.Arrays;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    //Endpoint do handshake WebSocket
    registry.addEndpoint("/ws")
      .setAllowedOrigins(originsArray())
      .withSockJS();
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    //Mensagens enviadas para destinos iniciados por /app roteadas para @MessageMapping
    config.setApplicationDestinationPrefixes("/app");

    //Broker simples (in-memory) para publish/subscribe em tópicos
    config.enableSimpleBroker("/topic");
  }

  @Value("${app.seguranca.origins-permitidas}")
  private String originsPermitidas;

  private String[] originsArray() {
    return Arrays.stream(originsPermitidas.split(","))
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .toArray(String[]::new);
  }

}
