package br.com.falbot.seplag.backend.api;

import br.com.falbot.seplag.backend.api.dto.RegionalResponses;
import br.com.falbot.seplag.backend.servico.RegionalServico;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/regionais", "/api/v1/regionais"})
@SecurityRequirement(name = "bearerAuth")
public class RegionalController {

    private final RegionalServico servico;

    public RegionalController(RegionalServico servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<RegionalResponses.RegionalResponse> listar(
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(required = false) String nome
    ) {
        return servico.listar(ativo, nome);
    }

    @PostMapping("/sincronizar")
    public RegionalResponses.SyncResponse sincronizar() {
        return servico.sincronizar();
    }
}
