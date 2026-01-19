package br.com.falbot.seplag.backend.api;

import br.com.falbot.seplag.backend.api.dto.AlbumRequests;
import br.com.falbot.seplag.backend.api.dto.Responses;
import br.com.falbot.seplag.backend.dominio.TipoArtista;
import br.com.falbot.seplag.backend.servico.AlbumServico;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;
import java.util.Set;

@RestController
@RequestMapping("/api/albuns")
public class AlbumController {

    private final AlbumServico servico;
    private final br.com.falbot.seplag.backend.servico.CapaAlbumServico capaServico;

    public AlbumController(AlbumServico servico, br.com.falbot.seplag.backend.servico.CapaAlbumServico capaServico) {
        this.servico = servico;
        this.capaServico = capaServico;
    }

    @GetMapping
    public Page<Responses.AlbumResponse> listar(
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false, name = "tipoArtista") Set<TipoArtista> tiposArtista,
            Pageable pageable
    ) {
        return servico.listar(titulo, ano, tiposArtista, pageable)
                .map(this::toResponse);
    }

    @GetMapping("/{id}")
    public Responses.AlbumResponse obter(@PathVariable UUID id) {
        var a = servico.obter(id);
        return toResponse(a);
    }

    @PostMapping
    public ResponseEntity<Responses.AlbumResponse> criar(
            @RequestBody @Valid AlbumRequests.Criar req,
            UriComponentsBuilder uriBuilder
    ) {
        var a = servico.criar(req.titulo(), req.anoLancamento());

        var resp = toResponse(a);

        var location = uriBuilder
                .path("/api/albuns/{id}")
                .buildAndExpand(resp.id())
                .toUri();

        return ResponseEntity.created(location).body(resp);
    }

    @PutMapping("/{id}")
    public Responses.AlbumResponse atualizar(@PathVariable UUID id, @RequestBody @Valid AlbumRequests.Atualizar req) {
        var a = servico.atualizar(id, req.titulo(), req.anoLancamento());
        return toResponse(a);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        servico.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(path = "/{id}/capa", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public br.com.falbot.seplag.backend.servico.CapaAlbumServico.LinkPresignadoResponse enviarCapa(
            @PathVariable UUID id,
            @RequestPart("arquivo") MultipartFile arquivo
    ) {
        return capaServico.enviar(id, arquivo);
    }

    @GetMapping("/{id}/capa/link")
    public br.com.falbot.seplag.backend.servico.CapaAlbumServico.LinkPresignadoResponse obterLinkCapa(@PathVariable UUID id) {
        return capaServico.gerarLink(id);
    }

    @DeleteMapping("/{id}/capa")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerCapa(@PathVariable UUID id) {
        capaServico.remover(id);
    }


    private Responses.AlbumResponse toResponse(br.com.falbot.seplag.backend.dominio.Album a) {
        boolean temCantor = a.getArtistas().stream().anyMatch(ar -> ar.getTipo() == TipoArtista.CANTOR);
        boolean temBanda = a.getArtistas().stream().anyMatch(ar -> ar.getTipo() == TipoArtista.BANDA);
        return new Responses.AlbumResponse(
                a.getId(),
                a.getTitulo(),
                a.getAnoLancamento(),
                a.getCriadoEm(),
                a.getAtualizadoEm(),
                temCantor,
                temBanda
        );
    }

}