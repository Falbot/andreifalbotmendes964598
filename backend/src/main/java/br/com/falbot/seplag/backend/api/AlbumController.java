package br.com.falbot.seplag.backend.api;

import br.com.falbot.seplag.backend.api.dto.AlbumRequests;
import br.com.falbot.seplag.backend.api.dto.Responses;
import br.com.falbot.seplag.backend.servico.AlbumServico;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/albuns")
public class AlbumController {

    private final AlbumServico servico;

    public AlbumController(AlbumServico servico) {
        this.servico = servico;
    }

    @GetMapping
    public Page<Responses.AlbumResponse> listar(
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) Integer ano,
            Pageable pageable
    ) {
        return servico.listar(titulo, ano, pageable)
                .map(a -> new Responses.AlbumResponse(a.getId(), a.getTitulo(), a.getAnoLancamento(), a.getCriadoEm(), a.getAtualizadoEm()));
    }

    @GetMapping("/{id}")
    public Responses.AlbumResponse obter(@PathVariable UUID id) {
        var a = servico.obter(id);
        return new Responses.AlbumResponse(a.getId(), a.getTitulo(), a.getAnoLancamento(), a.getCriadoEm(), a.getAtualizadoEm());
    }

    @PostMapping
    public Responses.AlbumResponse criar(@RequestBody @Valid AlbumRequests.Criar req) {
        var a = servico.criar(req.titulo(), req.anoLancamento());
        return new Responses.AlbumResponse(a.getId(), a.getTitulo(), a.getAnoLancamento(), a.getCriadoEm(), a.getAtualizadoEm());
    }

    @PutMapping("/{id}")
    public Responses.AlbumResponse atualizar(@PathVariable UUID id, @RequestBody @Valid AlbumRequests.Atualizar req) {
        var a = servico.atualizar(id, req.titulo(), req.anoLancamento());
        return new Responses.AlbumResponse(a.getId(), a.getTitulo(), a.getAnoLancamento(), a.getCriadoEm(), a.getAtualizadoEm());
    }

    @DeleteMapping("/{id}")
    public void excluir(@PathVariable UUID id) {
        servico.excluir(id);
    }
}
