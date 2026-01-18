package br.com.falbot.seplag.backend.servico;

import br.com.falbot.seplag.backend.config.S3Props;
import br.com.falbot.seplag.backend.dominio.CapaAlbum;
import br.com.falbot.seplag.backend.repositorio.CapaAlbumRepositorio;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.UUID;

@Service
public class CapaAlbumServico {

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

    @Transactional
    public LinkPresignadoResponse enviar(UUID albumId, MultipartFile arquivo) {
        //Garante 404 de álbum inexistente
        albumServico.obter(albumId);

        validarArquivoImagem(arquivo);

        String key = chaveObjeto(albumId);

        //Upload no MinIO
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
            throw new IllegalArgumentException("Falha ao enviar a imagem para o MinIO.", e);
        }

        //Grava/atualiza metadados no banco
        try {
            CapaAlbum capa = capaRepo.findByAlbumId(albumId).orElseGet(() -> {
                var c = new CapaAlbum();
                c.setAlbumId(albumId);
                return c;
            });

            capa.setObjetoChave(key);
            capa.setContentType(arquivo.getContentType());
            capa.setTamanhoBytes(arquivo.getSize());

            capaRepo.save(capa);
        } catch (RuntimeException e) {
            //Se o DB falhar, tenta remover o objeto para não deixar órfão
            try {
                s3.deleteObject(DeleteObjectRequest.builder().bucket(props.bucket).key(key).build());
            } catch (Exception ignored) {}
            throw e;
        }

        //Retorna link já pronto
        return gerarLink(albumId);
    }

    @Transactional(readOnly = true)
    public LinkPresignadoResponse gerarLink(UUID albumId) {
        var capa = capaRepo.findByAlbumId(albumId)
                .orElseThrow(() -> new EntityNotFoundException("Capa do álbum não encontrada!"));

        var getReq = GetObjectRequest.builder()
                .bucket(props.bucket)
                .key(capa.getObjetoChave())
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

    private static String chaveObjeto(UUID albumId) {
        //Chave estável: sobrescreve a capa ao reenviar
        return "albuns/" + albumId + "/capa";
    }

    public record LinkPresignadoResponse(String url, Instant expiraEm) {}

    @Transactional
    public void remover(UUID albumId) {
        // garante 404 se álbum não existir
        albumServico.obter(albumId);

        var capa = capaRepo.findByAlbumId(albumId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Capa do álbum não encontrada!"));

        // remove no MinIO
        s3.deleteObject(DeleteObjectRequest.builder()
                .bucket(props.bucket)
                .key(capa.getObjetoChave())
                .build());

        // remove metadados
        capaRepo.delete(capa);
    }
}
