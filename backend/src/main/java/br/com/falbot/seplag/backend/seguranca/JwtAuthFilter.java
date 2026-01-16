package br.com.falbot.seplag.backend.seguranca;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.List;
import java.util.UUID;

public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtServico jwtServico;

    public JwtAuthFilter(JwtServico jwtServico) {
        this.jwtServico = jwtServico;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) {
        try {
            String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (auth != null && auth.startsWith("Bearer ")) {
                String token = auth.substring("Bearer ".length()).trim();
                Claims c = jwtServico.validarEObterClaims(token);

                UUID usuarioId = UUID.fromString(c.getSubject());
                String email = c.get("email", String.class);

                var principal = new UsuarioAutenticado(usuarioId, email);
                var authentication = new UsernamePasswordAuthenticationToken(principal, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            chain.doFilter(request, response);
        } catch (Exception e) {
            response.setStatus(401);
        }
    }

    public record UsuarioAutenticado(UUID id, String email) {}
}
