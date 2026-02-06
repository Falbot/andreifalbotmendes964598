package br.com.falbot.seplag.backend.api;

import br.com.falbot.seplag.backend.api.dto.AlbumRequests;
import br.com.falbot.seplag.backend.api.dto.Responses;
import br.com.falbot.seplag.backend.dominio.TipoArtista;
import br.com.falbot.seplag.backend.servico.AlbumServico;
import jakarta.validation.Valid;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.UUID;
import java.util.Set;
import java.util.List;


@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping({"/api/albuns", "/api/v1/albuns"})
public class AlbumController {

    private final AlbumServico servico;
    private final br.com.falbot.seplag.backend.servico.CapaAlbumServico capaServico;

    public AlbumController(AlbumServico servico, br.com.falbot.seplag.backend.servico.CapaAlbumServico capaServico) {
        this.servico = servico;
        this.capaServico = capaServico;
    }
    @Tag(name = "Álbuns")
    @Operation(summary = "Listar álbuns")
    @Transactional(readOnly = true)
    @GetMapping
    public Page<Responses.AlbumResponse> listarAlbuns(
            @Parameter(description = "Filtro por título (contém, case-insensitive)", example = "Bem Sertanejo")
            @RequestParam(required = false) String titulo,

            @Parameter(description = "Ano de lançamento (exato)", example = "2018")
            @RequestParam(required = false) Integer ano,

            @Parameter(description = "Filtrar por tipo de artista associado (pode repetir o parâmetro)", example = "&tipoArtista=CANTOR&tipoArtista=BANDA")
            @RequestParam(required = false, name = "tipoArtista") Set<TipoArtista> tiposArtista,

            @ParameterObject
            @PageableDefault(size = 20)
            Pageable pageable
    ) {
        return servico.listar(titulo, ano, tiposArtista, pageable)
                .map(this::toResponse);
    }

    @Tag(name = "Álbuns")
    @Operation(summary = "Listar álbum por ID")
    @Transactional(readOnly = true)
    @GetMapping("/{id}")
    public Responses.AlbumResponse obterAlbum(@PathVariable UUID id) {
        var a = servico.obter(id);
        return toResponse(a);
    }

    @Tag(name = "Álbuns")
    @Operation(summary = "Criar álbum")
    @PostMapping
    public ResponseEntity<Responses.AlbumResponse> criarAlbum(@RequestBody @Valid AlbumRequests.Criar req, UriComponentsBuilder uriBuilder)
    {
        var a = servico.criar(req.titulo(), req.anoLancamento());

        var resp = toResponse(a);

        var location = uriBuilder
                .path("/api/albuns/{id}")
                .buildAndExpand(resp.id())
                .toUri();

        return ResponseEntity.created(location).body(resp);
    }

    @Tag(name = "Álbuns")
    @Operation(summary = "Atualizar álbum")
    @PutMapping("/{id}")
    public Responses.AlbumResponse atualizarAlbum(@PathVariable UUID id, @RequestBody @Valid AlbumRequests.Atualizar req) {
        var a = servico.atualizar(id, req.titulo(), req.anoLancamento());
        return toResponse(a);
    }

    @Tag(name = "Álbuns")
    @Operation(summary = "Deletar álbum")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirAlbum(@PathVariable UUID id) {
        servico.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @Tag(name = "Álbuns")
    @Operation(summary = "Enviar capa do álbum")
    @PostMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public br.com.falbot.seplag.backend.servico.CapaAlbumServico.LinkPresignadoResponse enviarCapa(
            @PathVariable UUID id,
            @RequestPart("arquivo") MultipartFile arquivo
    ) {
        return capaServico.enviar(id, arquivo);
    }

    @Tag(name = "Capas")
    @Operation(summary = "Enviar capas de álbum")
    @PostMapping(path = "/{id}/capas", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<br.com.falbot.seplag.backend.servico.CapaAlbumServico.CapaCriadaResponse> adicionarCapasLote(
            @PathVariable UUID id,
            @RequestPart("arquivos") List<MultipartFile> arquivos
    ) {
        return capaServico.adicionarLote(id, arquivos);
    }

    @Tag(name = "Capas")
    @Operation(summary = "Listar capas de álbum")
    @GetMapping("/{id}/capas")
    public List<br.com.falbot.seplag.backend.servico.CapaAlbumServico.CapaItemComLinkResponse> listarCapasComLinks(
            @PathVariable UUID id
    ) {
        return capaServico.listarComLinks(id);
    }

    @Tag(name = "Capas")
    @Operation(summary = "Listar capa de álbum por ID")
    @GetMapping("/{id}/capas/{capaId}")
    public br.com.falbot.seplag.backend.servico.CapaAlbumServico.LinkPresignadoResponse obterLinkCapaPorId(
            @PathVariable UUID id,
            @PathVariable UUID capaId
    ) {
        return capaServico.gerarLink(id, capaId);
    }

    @Tag(name = "Capas")
    @Operation(summary = "Deletar capa de álbum")
    @DeleteMapping("/{id}/capas/{capaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerCapaPorId(
            @PathVariable UUID id,
            @PathVariable UUID capaId
    ) {
        capaServico.remover(id, capaId);
    }

    @Tag(name = "Capas")
    @Operation(summary = "Definir capa principal de álbum")
    @PutMapping("/{id}/capas/{capaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void definirCapaPrincipal(@PathVariable UUID id, @PathVariable UUID capaId) {
        capaServico.definirPrincipal(id, capaId);
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