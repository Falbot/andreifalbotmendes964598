package br.com.falbot.seplag.backend.seguranca;

import br.com.falbot.seplag.backend.config.JwtProps;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtServico {

    private final JwtProps props;
    private final SecretKey key;

    public JwtServico(JwtProps props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(props.secret));
    }

    public String gerarTokenAcesso(UUID usuarioId, String email) {
        Instant agora = Instant.now();
        Instant exp = agora.plusSeconds(props.accessMinutes * 60L);

        return Jwts.builder()
                .issuer(props.issuer)
                .subject(usuarioId.toString())
                .claim("email", email)
                .issuedAt(Date.from(agora))
                .expiration(Date.from(exp))
                .signWith(key)
                .id(UUID.randomUUID().toString())
                .compact();
    }

    public Claims validarEObterClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(props.issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
