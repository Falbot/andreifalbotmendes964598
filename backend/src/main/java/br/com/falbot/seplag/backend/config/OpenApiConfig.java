package br.com.falbot.seplag.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.tags.Tag;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Seletivo SEPLAG 2026 - Backend",
                version = "v1",
                description = "API Back-End do desafio técnico (catálogo musical com capas via MinIO, autenticação JWT e rate limit)."
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)

public class OpenApiConfig {
        @Bean
        public OpenAPI openAPI() {
                return new OpenAPI()
                .tags(List.of(
                new Tag().name("Autenticação").description("Operações para autenticação e gerenciamento de tokens."),
                new Tag().name("Artistas").description("Operações de artistas"),
                new Tag().name("Álbuns").description("Operações de álbum"),
                new Tag().name("Capas").description("Operações de capas de álbum"),
                new Tag().name("Regionais").description("Operações de Regionais"),
                new Tag().name("Health Check").description("Verificação de saúde")
                ));
        }
}
