package br.com.falbot.seplag.backend.api;

import br.com.falbot.seplag.backend.api.dto.AutenticacaoRequests;
import br.com.falbot.seplag.backend.api.dto.AutenticacaoResponses;
import br.com.falbot.seplag.backend.servico.AutenticacaoServico;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/autenticacao", "/api/v1/autenticacao"})
public class AutenticacaoController {

    private final AutenticacaoServico servico;

    public AutenticacaoController(AutenticacaoServico servico) {
        this.servico = servico;
    }

    @PostMapping("/registrar")
    public void registrar(@RequestBody @Valid AutenticacaoRequests.Registrar req) {
        servico.registrar(req);
    }

    @PostMapping("/login")
    public AutenticacaoResponses.Token login(@RequestBody @Valid AutenticacaoRequests.Login req) {
        return servico.login(req);
    }

    @PostMapping("/renovar")
    public AutenticacaoResponses.Token renovar(@RequestBody @Valid AutenticacaoRequests.Renovar req) {
        return servico.renovar(req);
    }
}
