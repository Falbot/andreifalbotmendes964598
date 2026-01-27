package br.com.falbot.seplag.backend.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Tag(name = "Health Check")
@RestController
public class PingController {

    @Operation(summary = "Checar se o servidor de API está funcionando")
    @GetMapping({"/api/ping", "/api/v1/ping"})
    public String ping() {
        return "ok";
    }
}
