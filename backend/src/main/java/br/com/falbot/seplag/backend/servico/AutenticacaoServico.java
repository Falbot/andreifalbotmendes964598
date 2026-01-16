package br.com.falbot.seplag.backend.servico;

import br.com.falbot.seplag.backend.api.dto.AutenticacaoRequests;
import br.com.falbot.seplag.backend.api.dto.AutenticacaoResponses;
import br.com.falbot.seplag.backend.config.JwtProps;
import br.com.falbot.seplag.backend.dominio.RefreshToken;
import br.com.falbot.seplag.backend.dominio.Usuario;
import br.com.falbot.seplag.backend.repositorio.RefreshTokenRepositorio;
import br.com.falbot.seplag.backend.repositorio.UsuarioRepositorio;
import br.com.falbot.seplag.backend.seguranca.JwtServico;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class AutenticacaoServico {

    private final UsuarioRepositorio usuarioRepo;
    private final RefreshTokenRepositorio refreshRepo;
    private final PasswordEncoder encoder;
    private final JwtServico jwtServico;
    private final JwtProps props;

    public AutenticacaoServico(UsuarioRepositorio usuarioRepo, RefreshTokenRepositorio refreshRepo,
                               PasswordEncoder encoder, JwtServico jwtServico, JwtProps props) {
        this.usuarioRepo = usuarioRepo;
        this.refreshRepo = refreshRepo;
        this.encoder = encoder;
        this.jwtServico = jwtServico;
        this.props = props;
    }

    @Transactional
    public void registrar(AutenticacaoRequests.Registrar req) {
        if (usuarioRepo.existsByEmail(req.email())) {
            throw new IllegalArgumentException("E-mail já cadastrado.");
        }

        Usuario u = new Usuario();
        u.setEmail(req.email());
        u.setSenhaHash(encoder.encode(req.senha()));
        usuarioRepo.save(u);
    }

    @Transactional
    public AutenticacaoResponses.Token login(AutenticacaoRequests.Login req) {
        Usuario u = usuarioRepo.findByEmail(req.email())
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));

        if (!encoder.matches(req.senha(), u.getSenhaHash())) {
            throw new IllegalArgumentException("Credenciais inválidas.");
        }

        return emitirTokens(u);
    }

    @Transactional
    public AutenticacaoResponses.Token renovar(AutenticacaoRequests.Renovar req) {
        String hash = sha256Hex(req.refreshToken());
        RefreshToken rt = refreshRepo.findByTokenHash(hash)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token inválido."));

        if (!rt.estaAtivo()) {
            throw new IllegalArgumentException("Refresh token expirado ou revogado.");
        }

        Usuario u = usuarioRepo.findById(rt.getUsuarioId())
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));

        //Revoga o antigo e emite novo token
        rt.setRevogadoEm(Instant.now());

        return emitirTokens(u);
    }

    private AutenticacaoResponses.Token emitirTokens(Usuario u) {
        String access = jwtServico.gerarTokenAcesso(u.getId(), u.getEmail());

        String refreshRaw = gerarRefreshRaw();
        String refreshHash = sha256Hex(refreshRaw);

        RefreshToken rt = new RefreshToken();
        rt.setUsuarioId(u.getId());
        rt.setTokenHash(refreshHash);
        rt.setExpiraEm(Instant.now().plus(props.refreshDays, ChronoUnit.DAYS));
        refreshRepo.save(rt);

        return new AutenticacaoResponses.Token(access, refreshRaw, props.accessMinutes * 60L, "Bearer");
    }

    private static String gerarRefreshRaw() {
        byte[] b = new byte[32];
        new SecureRandom().nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : digest) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
