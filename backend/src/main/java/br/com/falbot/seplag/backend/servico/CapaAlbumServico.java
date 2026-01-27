package br.com.falbot.seplag.backend.servico;

import br.com.falbot.seplag.backend.config.S3Props;
import br.com.falbot.seplag.backend.dominio.CapaAlbum;
import br.com.falbot.seplag.backend.repositorio.CapaAlbumRepositorio;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CapaAlbumServico {
    private static final Logger log = LoggerFactory.getLogger(CapaAlbumServico.class);
    private static final Duration EXPIRACAO_LINK = Duration.ofMinutes(30);

    private final AlbumServico albumServico;
    private final CapaAlbumRepositorio capaRepo;
    private final S3Client s3;
    private final S3Presigner presigner;
    private final S3Props props;

    public CapaAlbumServico(
            AlbumServico albumServico,
            CapaAlbumRepositorio capaRepo,
            S3Client s3,
            S3Presigner presigner,
            S3Props props
    ) {
        this.albumServico = albumServico;
        this.capaRepo = capaRepo;
        this.s3 = s3;
        this.presigner = presigner;
        this.props = props;
    }

    /**
     * Endpoint legado: /{albumId}/capa
     * Agora significa "enviar capa PRINCIPAL".
     * Mantém histórico: a antiga principal vira principal=false.
     */
    @Transactional
    public LinkPresignadoResponse enviar(UUID albumId, MultipartFile arquivo) {
        albumServico.obter(albumId); // garante 404 se álbum não existir
        validarArquivoImagem(arquivo);

        UUID capaId = UUID.randomUUID();
        String key = chaveObjeto(albumId, capaId);

        // Upload no MinIO
        try {
            var putReq = PutObjectRequest.builder()
                    .bucket(props.bucket)
                    .key(key)
                    .contentType(arquivo.getContentType())
                    .build();

            s3.putObject(putReq, RequestBody.fromInputStream(arquivo.getInputStream(), arquivo.getSize()));
        } catch (IOException e) {
            throw new IllegalArgumentException("Falha ao ler o arquivo enviado.", e);
        } catch (RuntimeException e) {
            log.error("Falha ao enviar para MinIO. bucket={}, key={}, size={}, contentType={}, endpoint={}",
                    props.bucket, key, arquivo.getSize(), arquivo.getContentType(), props.endpoint, e);
            throw new IllegalArgumentException("Falha ao enviar a imagem para o MinIO.", e);
        }

        // Atualiza principal atual (se existir) e cria nova principal
        try {
            capaRepo.findByAlbumIdAndPrincipalTrue(albumId)
                    .ifPresent(atual -> atual.setPrincipal(false));
            capaRepo.flush(); // evita violar o índice único parcial na hora de inserir a nova principal
            var nova = new CapaAlbum();

            nova.setAlbumId(albumId);
            nova.setPrincipal(true);
            nova.setObjetoChave(key);
            nova.setContentType(arquivo.getContentType());
            nova.setTamanhoBytes(arquivo.getSize());
            capaRepo.save(nova);

        } catch (RuntimeException e) {
            // Se o DB falhar, tenta remover o objeto para não deixar órfão
            try { s3.deleteObject(DeleteObjectRequest.builder().bucket(props.bucket).key(key).build()); } catch (Exception ignored) {}
            throw e;
        }

        // Retorna link já pronto (30min)
        return presign(key);
    }

    /**
     * Novo: link de uma capa específica (/capas/{capaId}/link)
     */
    @Transactional(readOnly = true)
    public LinkPresignadoResponse gerarLink(UUID albumId, UUID capaId) {
        albumServico.obter(albumId);

        var capa = capaRepo.findByIdAndAlbumId(capaId, albumId)
                .orElseThrow(() -> new EntityNotFoundException("Capa do álbum não encontrada!"));

        return presign(capa.getObjetoChave());
    }

    @Transactional(readOnly = true)
    public List<CapaItemComLinkResponse> listarComLinks(UUID albumId) {
        albumServico.obter(albumId);

        return capaRepo.findAllByAlbumIdOrderByCriadoEmDesc(albumId).stream()
                .map(c -> {
                    var link = presign(c.getObjetoChave());
                    return new CapaItemComLinkResponse(
                            c.getId(),
                            c.isPrincipal(),
                            link.url(),
                            link.expiraEm(),
                            c.getContentType(),
                            c.getTamanhoBytes(),
                            c.getCriadoEm()
                    );
                })
                .toList();
    }

    @Transactional
    public List<CapaCriadaResponse> adicionarLote(UUID albumId, List<MultipartFile> arquivos) {
        albumServico.obter(albumId);

        if (arquivos == null || arquivos.isEmpty()) {
            throw new IllegalArgumentException("Lista de arquivos é obrigatória.");
        }

        var chavesEnviadas = new java.util.ArrayList<String>();
        var respostas = new java.util.ArrayList<CapaCriadaResponse>();

        try {
            for (var arquivo : arquivos) {
                validarArquivoImagem(arquivo);

                UUID objetoId = UUID.randomUUID();
                String key = chaveObjeto(albumId, objetoId);

                // upload
                var putReq = PutObjectRequest.builder()
                        .bucket(props.bucket)
                        .key(key)
                        .contentType(arquivo.getContentType())
                        .build();

                s3.putObject(putReq, RequestBody.fromInputStream(arquivo.getInputStream(), arquivo.getSize()));
                chavesEnviadas.add(key);

                // save
                var c = new CapaAlbum();
                c.setAlbumId(albumId);
                c.setPrincipal(false);
                c.setObjetoChave(key);
                c.setContentType(arquivo.getContentType());
                c.setTamanhoBytes(arquivo.getSize());

                var salva = capaRepo.save(c);

                var link = presign(key);
                respostas.add(new CapaCriadaResponse(salva.getId(), false, link.url(), link.expiraEm()));
            }

            return respostas;

        } catch (IOException e) {
            for (var key : chavesEnviadas) {
                try { s3.deleteObject(DeleteObjectRequest.builder().bucket(props.bucket).key(key).build()); } catch (Exception ignored) {}
            }
            throw new IllegalArgumentException("Falha ao ler algum arquivo do lote.", e);

        } catch (RuntimeException e) {
            for (var key : chavesEnviadas) {
                try { s3.deleteObject(DeleteObjectRequest.builder().bucket(props.bucket).key(key).build()); } catch (Exception ignored) {}
            }
            throw e;
        }
    }

    @Transactional
    public void remover(UUID albumId, UUID capaId) {
        albumServico.obter(albumId);

        var capa = capaRepo.findByIdAndAlbumId(capaId, albumId)
                .orElseThrow(() -> new EntityNotFoundException("Capa não encontrada para este álbum."));

        boolean eraPrincipal = capa.isPrincipal();
        String key = capa.getObjetoChave();

        //1)Remove do banco
        capaRepo.delete(capa);
        capaRepo.flush();

        //2)Se removeu a principal, promove outra (a mais recente)
        if (eraPrincipal) {
            capaRepo.findFirstByAlbumIdOrderByCriadoEmDesc(albumId)
                    .ifPresent(nova -> capaRepo.marcarComoPrincipal(albumId, nova.getId()));
        }

        //3)Remove do MinIO somente após commit (evita apagar arquivo se a transação falhar)
        Runnable apagarMinio = () -> {
            try {
                s3.deleteObject(DeleteObjectRequest.builder()
                        .bucket(props.bucket)
                        .key(key)
                        .build());
            } catch (Exception e) {
                log.warn("Falha ao remover objeto no MinIO. bucket={}, key={}", props.bucket, key, e);
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { apagarMinio.run(); }
            });
        } else {
            apagarMinio.run();
        }
    }

    /**
     * Novo: remove uma capa específica (/capas/{capaId})
     */
    @Transactional
    public void definirPrincipal(UUID albumId, UUID capaId) {
        albumServico.obter(albumId);

        capaRepo.desmarcarPrincipal(albumId);

        int afetadas = capaRepo.marcarComoPrincipal(albumId, capaId);
        if (afetadas == 0) {
            throw new EntityNotFoundException("Capa não encontrada para este álbum.");
        }
    }

    private LinkPresignadoResponse presign(String key) {
        var getReq = GetObjectRequest.builder()
                .bucket(props.bucket)
                .key(key)
                .build();

        var presignReq = GetObjectPresignRequest.builder()
                .signatureDuration(EXPIRACAO_LINK)
                .getObjectRequest(getReq)
                .build();

        var presigned = presigner.presignGetObject(presignReq);
        Instant expiraEm = Instant.now().plus(EXPIRACAO_LINK);

        return new LinkPresignadoResponse(presigned.url().toString(), expiraEm);
    }

    private static void validarArquivoImagem(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Arquivo de imagem é obrigatório.");
        }
        String ct = arquivo.getContentType();
        if (ct == null || !ct.startsWith("image/")) {
            throw new IllegalArgumentException("O arquivo enviado deve ser uma imagem (content-type image/*).");
        }
    }

    private static String chaveObjeto(UUID albumId, UUID capaId) {
        return "albuns/" + albumId + "/capas/" + capaId;
    }

    public record LinkPresignadoResponse(String url, Instant expiraEm) {}
    public record CapaCriadaResponse(UUID id, boolean principal, String url, Instant expiraEm) {}
    public record CapaItemResponse(
        UUID id, boolean principal, String contentType, long tamanhoBytes, java.time.OffsetDateTime criadoEm) {}
    public record CapaItemComLinkResponse(
            UUID id,
            boolean principal,
            String url,
            Instant expiraEm,
            String contentType,
            long tamanhoBytes,
            java.time.OffsetDateTime criadoEm
    ) {}

}
