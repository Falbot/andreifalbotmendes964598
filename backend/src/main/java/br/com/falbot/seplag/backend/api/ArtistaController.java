package br.com.falbot.seplag.backend.api;

import br.com.falbot.seplag.backend.api.dto.ArtistaRequests;
import br.com.falbot.seplag.backend.api.dto.Responses;
import br.com.falbot.seplag.backend.dominio.TipoArtista;
import br.com.falbot.seplag.backend.servico.ArtistaServico;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.List;
import java.util.UUID;

@Tag(name = "Artistas")
@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping({"/api/artistas", "/api/v1/artistas"})
public class ArtistaController {

    private final ArtistaServico servico;

    public ArtistaController(ArtistaServico servico) {
        this.servico = servico;
    }
    @Operation(summary = "Listar artistas (paginado)")
    @GetMapping
    public Page<Responses.ArtistaResponse> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) TipoArtista tipo,
            @RequestParam(required = false, defaultValue = "asc") String ordem,
            Pageable pageable
    ) {
        Pageable efetivo = pageable;
        if (pageable.getSort().isUnsorted()) {
            Sort s = "desc".equalsIgnoreCase(ordem) ? Sort.by("nome").descending() : Sort.by("nome").ascending();
            efetivo = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), s);
        }

        return servico.listar(nome, tipo, efetivo)
                .map(a -> new Responses.ArtistaResponse(a.getId(), a.getNome(), a.getTipo(), a.getCriadoEm(), a.getAtualizadoEm()));
    }

    @Operation(summary = "Obter artista por ID")
    @GetMapping("/{id}")
    public Responses.ArtistaResponse obter(@PathVariable UUID id) {
        var a = servico.obter(id);
        return new Responses.ArtistaResponse(a.getId(), a.getNome(), a.getTipo(), a.getCriadoEm(), a.getAtualizadoEm());
    }

    @Operation(summary = "Criar artista")
    @PostMapping
    public ResponseEntity<Responses.ArtistaResponse> criar(
            @RequestBody @Valid ArtistaRequests.Criar req,
            UriComponentsBuilder uriBuilder
    ) {
        var a = servico.criar(req.nome(), req.tipo());

        var resp = new Responses.ArtistaResponse(
                a.getId(), a.getNome(), a.getTipo(), a.getCriadoEm(), a.getAtualizadoEm()
        );

        var location = uriBuilder
                .path("/api/artistas/{id}")
                .buildAndExpand(resp.id())
                .toUri();
        return ResponseEntity.created(location).body(resp);
    }

    @Operation(summary = "Atualizar artista")
    @PutMapping("/{id}")
    public Responses.ArtistaResponse atualizar(@PathVariable UUID id, @RequestBody @Valid ArtistaRequests.Atualizar req) {
        var a = servico.atualizar(id, req.nome(), req.tipo());
        return new Responses.ArtistaResponse(a.getId(), a.getNome(), a.getTipo(), a.getCriadoEm(), a.getAtualizadoEm());
    }

    @Operation(summary = "Remover artista")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        servico.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Listar álbuns de um artista (paginado)")
    @GetMapping("/{id}/albuns")
    public List<Responses.AlbumResponse> listarAlbuns(@PathVariable UUID id) {
        return servico.listarAlbuns(id).stream()
                .map(al -> new Responses.AlbumResponse(
                        al.getId(), al.getTitulo(), al.getAnoLancamento(),
                        al.getCriadoEm(), al.getAtualizadoEm(),
                        al.getArtistas().stream().anyMatch(ar -> ar.getTipo() == TipoArtista.CANTOR),
                        al.getArtistas().stream().anyMatch(ar -> ar.getTipo() == TipoArtista.BANDA)
                ))
                .toList();
    }

    @Operation(summary = "Associar artista a um álbum")
    @PostMapping("/{id}/albuns/{albumId}")
    public ResponseEntity<Void> vincular(@PathVariable UUID id, @PathVariable UUID albumId) {
        servico.vincularAlbum(id, albumId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Desassociar artista de um álbum")
    @DeleteMapping("/{id}/albuns/{albumId}")
    public ResponseEntity<Void> desvincular(@PathVariable UUID id, @PathVariable UUID albumId) {
        servico.desvincularAlbum(id, albumId);
        return ResponseEntity.noContent().build();
    }
}
