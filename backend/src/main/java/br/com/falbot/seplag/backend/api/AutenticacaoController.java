package br.com.falbot.seplag.backend.api;

import br.com.falbot.seplag.backend.api.dto.AutenticacaoRequests;
import br.com.falbot.seplag.backend.api.dto.AutenticacaoResponses;
import br.com.falbot.seplag.backend.servico.AutenticacaoServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Autenticação")
@RestController
@RequestMapping({"/api/autenticacao", "/api/v1/autenticacao"})
public class AutenticacaoController {

    private final AutenticacaoServico servico;

    public AutenticacaoController(AutenticacaoServico servico) {
        this.servico = servico;
    }

    @Operation(summary = "Registrar usuário")
    @PostMapping("/registrar")
    public void registrar(@RequestBody @Valid AutenticacaoRequests.Registrar req) {
        servico.registrar(req);
    }
    
    @Operation(summary = "Autenticar usuário")
    @PostMapping("/login")
    public AutenticacaoResponses.Token login(@RequestBody @Valid AutenticacaoRequests.Login req) {
        return servico.login(req);
    }

    @Operation(summary = "Renovar token de acesso")
    @PostMapping("/renovar")
    public AutenticacaoResponses.Token renovar(@RequestBody @Valid AutenticacaoRequests.Renovar req) {
        return servico.renovar(req);
    }
}
