package br.com.falbot.seplag.backend.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {

    @GetMapping({"/api/ping", "/api/v1/ping"})
    public String ping() {
        return "ok";
    }
}
